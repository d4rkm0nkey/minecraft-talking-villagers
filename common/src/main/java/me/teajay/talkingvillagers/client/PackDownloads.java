package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.client.gui.DownloadPromptScreen;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.network.PackChunkPayload;
import me.teajay.talkingvillagers.network.RequestPackPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload.PackInfo;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads the voicepacks a server uses into {@code voicepacks/.server-cache}, through the game connection or over
 * HTTP(S) if the server names an address. Cached packs are only used while playing, they never become active packs.
 */
public final class PackDownloads {
    /** Larger packs are never downloaded, whatever the server says. */
    public static final long MAX_PACK_BYTES = 200L * 1024 * 1024;
    private static final long CACHE_LIMIT = 500L * 1024 * 1024;
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(4000L);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Talking Villagers downloads");
        thread.setDaemon(true);
        return thread;
    });

    /** Running downloads by SHA-256. Client thread. */
    private static final Map<String, Download> ACTIVE = new HashMap<>();
    private static List<PackInfo> offered = List.of();
    private static boolean promptPending;
    /** Increased on disconnect, so late results of an old connection are ignored. */
    private static int session;

    private PackDownloads() {
    }

    public static final class Download {
        final PackInfo info;
        final Path part;
        @Nullable FileChannel channel;
        @Nullable CompletableFuture<?> http;
        long received;

        Download(PackInfo info, Path part) {
            this.info = info;
            this.part = part;
        }

        public PackInfo info() {
            return info;
        }

        /** 0..1, or -1 for HTTP downloads without progress. */
        public float progress() {
            return http != null ? -1 : info.size() == 0 ? 0 : (float) received / info.size();
        }
    }

    /**
     * Called when the server told us its packs; offers the missing ones that can be downloaded.
     */
    static void onServerPacks(List<PackInfo> missing) {
        offered = missing.stream()
                .filter(PackDownloads::canDownload)
                .filter(pack -> !ACTIVE.containsKey(pack.sha256()))
                .toList();
        if (offered.isEmpty()) {
            return;
        }
        if (Configs.client().trustedServers().contains(serverKey())) {
            start(offered);
        } else {
            promptPending = true;
        }
    }

    public static boolean canDownload(PackInfo pack) {
        return (pack.downloadable() || isHttp(pack.url())) && pack.size() <= MAX_PACK_BYTES;
    }

    private static boolean isHttp(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    /** Client tick: shows the question once the player is in the world and no other screen is open. */
    public static void tick(Minecraft minecraft) {
        if (promptPending && minecraft.player != null && minecraft.gui.screen() == null) {
            promptPending = false;
            if (!offered.isEmpty()) {
                minecraft.gui.setScreen(new DownloadPromptScreen(offered));
            }
        }
    }

    /** The address of the current server, as the player entered it; empty in singleplayer. */
    public static String serverKey() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? "" : server.ip;
    }

    /** Answer of the prompt. */
    public static void accept(List<PackInfo> packs, boolean always) {
        if (always && !serverKey().isEmpty()) {
            Configs.setClient(Configs.client().trusting(serverKey()));
        }
        start(packs);
    }

    public static Map<String, Download> active() {
        return Map.copyOf(ACTIVE);
    }

    private static void start(List<PackInfo> packs) {
        Path cache = Voicepacks.repository().cacheDirectory();
        try {
            Files.createDirectories(cache);
        } catch (IOException e) {
            TalkingVillagers.LOG.error("Cannot create {}", cache, e);
            return;
        }
        for (PackInfo pack : packs) {
            if (ACTIVE.containsKey(pack.sha256()) || Voicepacks.index().byHash(pack.sha256()).isPresent()) {
                continue;
            }
            Download download = new Download(pack, cache.resolve(pack.sha256() + ".part"));
            ACTIVE.put(pack.sha256(), download);
            if (isHttp(pack.url())) {
                startHttp(download);
            } else {
                Services.NETWORK.sendToServer(new RequestPackPayload(pack.sha256()));
            }
        }
        toast(Component.translatable("talkingvillagers.download.started", packs.size()), null);
    }

    private static void startHttp(Download download) {
        int current = session;
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(download.info.url())).timeout(Duration.ofMinutes(10)).build();
        download.http = client.sendAsync(request, HttpResponse.BodyHandlers.ofFile(download.part))
                .whenComplete((response, error) -> Minecraft.getInstance().execute(() -> {
                    if (current != session) {
                        return;
                    }
                    if (error != null || response.statusCode() != 200) {
                        fail(download, error != null ? String.valueOf(error.getMessage()) : "HTTP " + response.statusCode());
                    } else {
                        finish(download);
                    }
                }));
    }

    /** Client thread. */
    public static void onChunk(PackChunkPayload chunk) {
        Download download = ACTIVE.get(chunk.sha256());
        if (download == null || download.http != null) {
            return;
        }
        if (chunk.total() != download.info.size() || chunk.offset() != download.received
                || chunk.offset() + chunk.data().length > chunk.total()) {
            fail(download, "unexpected data from the server");
            return;
        }
        try {
            if (download.channel == null) {
                download.channel = FileChannel.open(download.part, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            ByteBuffer buffer = ByteBuffer.wrap(chunk.data());
            while (buffer.hasRemaining()) {
                download.channel.write(buffer);
            }
            download.received += chunk.data().length;
            if (download.received == chunk.total()) {
                download.channel.close();
                download.channel = null;
                finish(download);
            }
        } catch (IOException e) {
            fail(download, e.getMessage());
        }
    }

    /** Checks the file on a worker thread, moves it into the cache and reloads the packs. */
    private static void finish(Download download) {
        int current = session;
        WORKER.execute(() -> {
            String error = null;
            try {
                LoadedPack pack = PackReader.read(download.part);
                if (!pack.sha256().equals(download.info.sha256())) {
                    error = "checksum does not match";
                } else if (!pack.isUsable()) {
                    error = pack.errors().getFirst().message();
                } else {
                    Path target = download.part.resolveSibling(download.info.sha256() + ".voicepack");
                    Files.move(download.part, target, StandardCopyOption.REPLACE_EXISTING);
                    Voicepacks.repository().pruneCache(CACHE_LIMIT);
                    Voicepacks.reload();
                }
            } catch (IOException e) {
                error = e.getMessage();
            }
            String result = error;
            Minecraft.getInstance().execute(() -> {
                if (current != session) {
                    return;
                }
                if (result != null) {
                    fail(download, result);
                    return;
                }
                ACTIVE.remove(download.info.sha256());
                toast(Component.translatable("talkingvillagers.download.done"), Component.literal(download.info.name()));
            });
        });
    }

    private static void fail(Download download, @Nullable String reason) {
        TalkingVillagers.LOG.warn("Download of voicepack {} failed: {}", download.info.name(), reason);
        ACTIVE.remove(download.info.sha256());
        close(download);
        toast(Component.translatable("talkingvillagers.download.failed"), Component.literal(download.info.name() + ": " + reason));
    }

    private static void close(Download download) {
        try {
            if (download.channel != null) {
                download.channel.close();
            }
            if (download.http != null) {
                download.http.cancel(true);
            }
            Files.deleteIfExists(download.part);
        } catch (IOException ignored) {
        }
    }

    public static void onDisconnect() {
        session++;
        ACTIVE.values().forEach(PackDownloads::close);
        ACTIVE.clear();
        offered = List.of();
        promptPending = false;
    }

    private static void toast(Component title, @Nullable Component message) {
        SystemToast.addOrUpdate(Minecraft.getInstance().gui.toastManager(), TOAST, title, message);
    }
}
