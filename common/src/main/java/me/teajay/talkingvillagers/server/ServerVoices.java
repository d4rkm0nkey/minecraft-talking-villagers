package me.teajay.talkingvillagers.server;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.network.ClientPacksPayload;
import me.teajay.talkingvillagers.network.RequestPackPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side bookkeeping: which voicepacks each connected client has.
 */
public final class ServerVoices {
    private static final Map<UUID, Set<String>> CLIENT_PACKS = new ConcurrentHashMap<>();
    private static volatile MinecraftServer server;

    private ServerVoices() {
    }

    public static void onServerStarting(MinecraftServer minecraftServer) {
        server = minecraftServer;
        CLIENT_PACKS.clear();
    }

    public static void onServerStopped() {
        server = null;
        CLIENT_PACKS.clear();
    }

    /**
     * Tell all connected players about the new packs after a reload.
     */
    public static void onPacksReloaded() {
        MinecraftServer current = server;
        if (current != null) {
            current.execute(() -> current.getPlayerList().getPlayers().forEach(ServerVoices::sendPackList));
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        sendPackList(player);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        CLIENT_PACKS.remove(player.getUUID());
        PackTransfers.onPlayerLeave(player);
    }

    public static void onServerTick(MinecraftServer minecraftServer) {
        PackTransfers.tick(minecraftServer);
    }

    public static void onPackRequest(ServerPlayer player, RequestPackPayload payload) {
        PackTransfers.onRequest(player, payload.sha256());
    }

    public static void onClientPacks(ServerPlayer player, ClientPacksPayload payload) {
        CLIENT_PACKS.put(player.getUUID(), payload.packHashes());
        TalkingVillagers.LOG.debug("{} has {} of the server's voicepacks", player.getName().getString(), payload.packHashes().size());
    }

    /**
     * Whether the player's client can play lines from the pack file with this hash.
     */
    public static boolean hasPack(ServerPlayer player, String packHash) {
        return CLIENT_PACKS.getOrDefault(player.getUUID(), Set.of()).contains(packHash);
    }

    private static void sendPackList(ServerPlayer player) {
        if (!Services.NETWORK.canSend(player, ServerPacksPayload.TYPE)) {
            return;
        }
        var packs = Voicepacks.index().activePacks().stream().map(ServerVoices::info).toList();
        Services.NETWORK.sendToPlayer(player, new ServerPacksPayload(packs));
    }

    private static ServerPacksPayload.PackInfo info(LoadedPack loaded) {
        VoicePack pack = loaded.pack().orElseThrow();
        long size = PackTransfers.size(loaded);
        String base = Configs.server().packDownload().url().trim();
        String url = base.isEmpty() ? "" : (base.endsWith("/") ? base : base + "/") + loaded.source().getFileName();
        return new ServerPacksPayload.PackInfo(pack.id(), pack.name(), pack.version(), loaded.sha256(), size,
                PackTransfers.isDownloadable(loaded.sha256()), url);
    }
}
