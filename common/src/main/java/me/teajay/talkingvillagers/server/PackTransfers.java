package me.teajay.talkingvillagers.server;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.config.ServerConfig;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.network.PackChunkPayload;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends voicepacks to players that asked for them, in small pieces and limited to the configured bandwidth per player,
 * so joining players do not slow down the server.
 */
public final class PackTransfers {
    private static final Map<UUID, Deque<Transfer>> QUEUES = new HashMap<>();
    private static final Map<UUID, Double> CREDIT = new HashMap<>();

    private PackTransfers() {
    }

    private static final class Transfer {
        final String sha256;
        final FileChannel channel;
        final long size;
        long offset;

        Transfer(String sha256, FileChannel channel, long size) {
            this.sha256 = sha256;
            this.channel = channel;
            this.size = size;
        }
    }

    /** Server thread. */
    public static void onRequest(ServerPlayer player, String sha256) {
        if (!isDownloadable(sha256)) {
            TalkingVillagers.LOG.warn("{} asked for voicepack {} that cannot be downloaded", player.getName().getString(), sha256);
            return;
        }
        Deque<Transfer> queue = QUEUES.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        if (queue.stream().anyMatch(transfer -> transfer.sha256.equals(sha256))) {
            return;
        }
        LoadedPack pack = activePack(sha256).orElseThrow();
        try {
            queue.add(new Transfer(sha256, FileChannel.open(pack.source(), StandardOpenOption.READ), Files.size(pack.source())));
        } catch (IOException e) {
            TalkingVillagers.LOG.error("Cannot send {}", pack.source(), e);
        }
    }

    /** Whether the server offers this pack for download. */
    public static boolean isDownloadable(String sha256) {
        ServerConfig.PackDownload config = Configs.server().packDownload();
        if (!config.enabled()) {
            return false;
        }
        return activePack(sha256).filter(pack -> size(pack) <= config.maxPackSizeMb() * 1024L * 1024L).isPresent();
    }

    private static Optional<LoadedPack> activePack(String sha256) {
        return Voicepacks.index().activePacks().stream().filter(pack -> pack.sha256().equals(sha256)).findFirst();
    }

    static long size(LoadedPack pack) {
        try {
            return Files.size(pack.source());
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    /** Called every server tick. */
    public static void tick(MinecraftServer server) {
        if (QUEUES.isEmpty()) {
            return;
        }
        double perTick = Configs.server().packDownload().kbpsPerPlayer() * 1024.0 / 20.0;
        QUEUES.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Deque<Transfer> queue = entry.getValue();
            if (player == null) {
                queue.forEach(PackTransfers::close);
                CREDIT.remove(entry.getKey());
                return true;
            }
            double credit = Math.min(CREDIT.getOrDefault(entry.getKey(), 0.0) + perTick, perTick * 20);
            while (!queue.isEmpty() && credit >= Math.min(PackChunkPayload.MAX_CHUNK, queue.peek().size - queue.peek().offset)) {
                Transfer transfer = queue.peek();
                credit -= send(player, transfer);
                if (transfer.offset >= transfer.size) {
                    close(queue.poll());
                }
            }
            CREDIT.put(entry.getKey(), credit);
            return queue.isEmpty();
        });
    }

    private static int send(ServerPlayer player, Transfer transfer) {
        int length = (int) Math.min(PackChunkPayload.MAX_CHUNK, transfer.size - transfer.offset);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        try {
            while (buffer.hasRemaining() && transfer.channel.read(buffer, transfer.offset + buffer.position()) > 0) {
                // read until the chunk is full
            }
        } catch (IOException e) {
            TalkingVillagers.LOG.error("Cannot read voicepack {}", transfer.sha256, e);
            transfer.offset = transfer.size;
            return length;
        }
        Services.NETWORK.sendToPlayer(player, new PackChunkPayload(transfer.sha256, transfer.offset, transfer.size, buffer.array()));
        transfer.offset += length;
        return length;
    }

    private static void close(Transfer transfer) {
        try {
            transfer.channel.close();
        } catch (IOException ignored) {
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        Deque<Transfer> queue = QUEUES.remove(player.getUUID());
        if (queue != null) {
            queue.forEach(PackTransfers::close);
        }
        CREDIT.remove(player.getUUID());
    }
}
