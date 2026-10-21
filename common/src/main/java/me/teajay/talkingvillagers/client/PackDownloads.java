package me.teajay.talkingvillagers.client;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.client.gui.DownloadPromptScreen;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.util.SafeFiles;
import me.teajay.talkingvillagers.network.PackChunkPayload;
import me.teajay.talkingvillagers.network.RequestPackPayload;
import me.teajay.talkingvillagers.network.ServerPacksPayload.PackInfo;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    /** A download without new data for this long is given up and offered again. */
    private static final long STALL_TIMEOUT_MS = 30_000;
    private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(4000L);
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Talking Villagers downloads");
        thread.setDaemon(true);
        return thread;
    });
    private static final ExecutorService HTTP_WORKERS = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "Talking Villagers HTTP download");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

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
        /** Set for downloads over HTTP(S), otherwise the pack comes through the game connection. */
        final @Nullable URI uri;
        @Nullable FileChannel channel;
        @Nullable CompletableFuture<?> http;
        /** Written by the HTTP worker while it reads the response. */
        volatile @Nullable Thread httpThread;
        volatile boolean cancelled;
        /** Bytes written to {@link #part}. */
        volatile long received;
        /** Bytes accepted from the server and queued for writing. Client thread. */
        long accepted;
        /** {@link Util#getMillis()} of the last data. */
        volatile long lastProgress = Util.getMillis();
        /** Set once all data is there and the file is checked; such downloads cannot stall any more. */
        volatile boolean verifying;

        Download(PackInfo info, Path part, @Nullable URI uri) {
            this.info = info;
            this.part = part;
            this.uri = uri;
        }

        public PackInfo info() {
            return info;
        }

        /** 0..1, or -1 if the size is unknown. */
        public float progress() {
            return info.size() <= 0 ? -1 : Math.min(1, (float) received / info.size());
        }

        /** The most bytes we accept for this pack. */
        long limit() {
            return info.size() > 0 ? Math.min(info.size(), MAX_PACK_BYTES) : MAX_PACK_BYTES;
        }
    }

    /** Why a download failed, as translatable text. */
    private static final class DownloadError extends IOException {
        final Component reason;

        DownloadError(String key, Object... args) {
            this(Component.translatable("talkingvillagers.download.error." + key, args));
        }

        DownloadError(Component reason) {
            super(reason.getString());
            this.reason = reason;
        }

        static Component reason(Throwable error) {
            return error instanceof DownloadError download ? download.reason
                    : Component.translatable("talkingvillagers.download.error.io", String.valueOf(error.getMessage()));
        }
    }

    /** Client setup: removes downloads that were interrupted when the game closed. */
    public static void init() {
        WORKER.execute(() -> {
            try {
                Voicepacks.repository().deletePartFiles();
            } catch (IOException e) {
                TalkingVillagers.LOG.warn("Cannot delete unfinished downloads", e);
            }
        });
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
        // the hash names the file in the cache, anything else could point outside of it
        return LoadedPack.isSha256(pack.sha256()) && (pack.downloadable() || httpUri(pack.url()).isPresent()) && pack.size() <= MAX_PACK_BYTES;
    }

    /** The address to download from, if the server named a valid HTTP(S) address. */
    static Optional<URI> httpUri(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(url);
            return uri.getHost() == null ? Optional.empty() : Optional.of(uri);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Downloaded packs of the current server are used again, so they are pruned last.
     */
    static void markUsed(Set<String> sha256s) {
        WORKER.execute(() -> Voicepacks.repository().markCachedUsed(sha256s));
    }

    /**
     * Client tick: gives up downloads without progress and shows the question once the player is in the world and no
     * other screen is open.
     */
    public static void tick(Minecraft minecraft) {
        long now = Util.getMillis();
        List<Download> stalled = ACTIVE.values().stream()
                .filter(download -> !download.verifying && now - download.lastProgress > STALL_TIMEOUT_MS)
                .toList();
        if (!stalled.isEmpty()) {
            List<PackInfo> again = new ArrayList<>(offered);
            for (Download download : stalled) {
                fail(download, new DownloadError("stalled", STALL_TIMEOUT_MS / 1000));
                if (!again.contains(download.info)) {
                    again.add(download.info);
                }
            }
            // ask again instead of retrying on our own, even for trusted servers, so a broken server can't loop
            offered = List.copyOf(again);
            promptPending = true;
        }
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
            Path part = cache.resolve(pack.sha256() + ".part").normalize();
            if (!LoadedPack.isSha256(pack.sha256()) || !cache.normalize().equals(part.getParent())) {
                TalkingVillagers.LOG.warn("Not downloading {}, invalid checksum from the server", pack.name());
                continue;
            }
            Download download = new Download(pack, part, httpUri(pack.url()).orElse(null));
            ACTIVE.put(pack.sha256(), download);
            if (download.uri != null) {
                startHttp(download);
            } else {
                // the server sends the packs one after another, so the waiting ones count from now as well
                ACTIVE.values().stream().filter(other -> other.uri == null).forEach(other -> other.lastProgress = Util.getMillis());
                Services.NETWORK.sendToServer(new RequestPackPayload(pack.sha256()));
            }
        }
        toast(Component.translatable("talkingvillagers.download.started", packs.size()), null);
    }

    private static void startHttp(Download download) {
        int current = session;
        try {
            download.http = CompletableFuture.runAsync(() -> downloadHttp(download), HTTP_WORKERS)
                    .whenComplete((ignored, error) -> Minecraft.getInstance().execute(() -> {
                        if (current != session || ACTIVE.get(download.info.sha256()) != download) {
                            return;
                        }
                        if (error != null) {
                            fail(download, error.getCause() != null ? error.getCause() : error);
                        } else {
                            verifyLater(download);
                        }
                    }));
        } catch (RuntimeException e) {
            fail(download, e);
        }
    }

    /** HTTP worker: reads the response into the {@code .part} file, at most as many bytes as the server announced. */
    private static void downloadHttp(Download download) {
        // a pool thread may still carry the interrupt meant for an earlier download
        Thread.interrupted();
        download.httpThread = Thread.currentThread();
        try {
            if (download.cancelled) {
                throw new DownloadError("cancelled");
            }
            HttpRequest request = HttpRequest.newBuilder(download.uri).timeout(Duration.ofSeconds(30)).build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new DownloadError("http", response.statusCode());
                }
                OptionalLong length = response.headers().firstValueAsLong("Content-Length");
                if (length.isPresent() && length.getAsLong() > download.limit()) {
                    throw new DownloadError("too_large");
                }
                try (OutputStream out = Files.newOutputStream(download.part)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = body.read(buffer)) >= 0) {
                        if (download.cancelled) {
                            throw new DownloadError("cancelled");
                        }
                        if (download.received + read > download.limit()) {
                            throw new DownloadError("too_large");
                        }
                        out.write(buffer, 0, read);
                        download.received += read;
                        download.lastProgress = Util.getMillis();
                    }
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            deletePart(download);
            throw new CompletionException(download.cancelled ? new DownloadError("cancelled") : e);
        } finally {
            download.httpThread = null;
        }
    }

    /** Client thread. Checks the chunk here and writes it on the worker, so the game doesn't wait for the disk. */
    public static void onChunk(PackChunkPayload chunk) {
        Download download = ACTIVE.get(chunk.sha256());
        if (download == null || download.uri != null) {
            return;
        }
        if (chunk.total() == PackChunkPayload.REFUSED) {
            fail(download, new DownloadError("refused"));
            return;
        }
        if (chunk.total() != download.info.size() || chunk.offset() != download.accepted
                || chunk.offset() + chunk.data().length > chunk.total()) {
            fail(download, new DownloadError("unexpected"));
            return;
        }
        download.accepted += chunk.data().length;
        ACTIVE.values().stream().filter(other -> other.uri == null).forEach(other -> other.lastProgress = Util.getMillis());
        boolean last = download.accepted == chunk.total();
        if (last) {
            download.verifying = true;
        }
        int current = session;
        Set<String> keep = last ? ClientVoices.serverPackHashes() : Set.of();
        WORKER.execute(() -> {
            if (download.cancelled) {
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
                if (last) {
                    download.channel.close();
                    download.channel = null;
                    verify(download, current, keep);
                }
            } catch (IOException e) {
                closeChannel(download);
                deletePart(download);
                Minecraft.getInstance().execute(() -> {
                    if (current == session && ACTIVE.get(download.info.sha256()) == download) {
                        fail(download, e);
                    }
                });
            }
        });
    }

    /** Client thread: all data of an HTTP download is there. */
    private static void verifyLater(Download download) {
        download.verifying = true;
        int current = session;
        Set<String> keep = ClientVoices.serverPackHashes();
        WORKER.execute(() -> verify(download, current, keep));
    }

    /**
     * Worker: checks the file, moves it into the cache and reloads the packs.
     *
     * @param keep packs of the current server, which pruning the cache must not delete
     */
    private static void verify(Download download, int current, Set<String> keep) {
        DownloadError error = null;
        try {
            LoadedPack pack = PackReader.read(download.part);
            if (!pack.sha256().equals(download.info.sha256())) {
                error = new DownloadError("checksum");
            } else if (!pack.isUsable()) {
                error = new DownloadError("invalid", pack.errors().getFirst().message());
            } else {
                Path target = download.part.resolveSibling(download.info.sha256() + ".voicepack");
                SafeFiles.move(download.part, target);
                Voicepacks.repository().pruneCache(CACHE_LIMIT, keep);
                Voicepacks.reload();
            }
        } catch (IOException e) {
            error = new DownloadError(DownloadError.reason(e));
        }
        if (error != null) {
            deletePart(download);
        }
        DownloadError result = error;
        Minecraft.getInstance().execute(() -> {
            if (current != session || ACTIVE.get(download.info.sha256()) != download) {
                return;
            }
            if (result != null) {
                fail(download, result);
                return;
            }
            ACTIVE.remove(download.info.sha256());
            toast(Component.translatable("talkingvillagers.download.done"), Component.literal(download.info.name()));
        });
    }

    private static void fail(Download download, Throwable error) {
        Component reason = DownloadError.reason(error);
        TalkingVillagers.LOG.warn("Download of voicepack {} failed: {}", download.info.name(), error.getMessage());
        ACTIVE.remove(download.info.sha256());
        close(download);
        toast(Component.translatable("talkingvillagers.download.failed"), Component.literal(download.info.name() + ": ").append(reason));
    }

    private static void close(Download download) {
        download.cancelled = true;
        Thread httpThread = download.httpThread;
        if (httpThread != null) {
            // unblocks the read; the HTTP worker deletes its file itself
            httpThread.interrupt();
        }
        // after the writes already queued; a file that was checked meanwhile has been moved and stays
        WORKER.execute(() -> {
            closeChannel(download);
            if (download.httpThread == null) {
                deletePart(download);
            }
        });
    }

    /** Worker. */
    private static void closeChannel(Download download) {
        try {
            if (download.channel != null) {
                download.channel.close();
                download.channel = null;
            }
        } catch (IOException ignored) {
        }
    }

    private static void deletePart(Download download) {
        try {
            Files.deleteIfExists(download.part);
        } catch (IOException e) {
            TalkingVillagers.LOG.warn("Cannot delete {}", download.part, e);
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
