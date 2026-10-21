package me.teajay.talkingvillagers.pack;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.Problem;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.pack.PackRepository;
import me.teajay.talkingvillagers.platform.Services;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The voicepacks in {@code <game dir>/voicepacks}. Client and integrated server share this instance.
 */
public final class Voicepacks {
    public static final String DIRECTORY = "voicepacks";
    private static final PackRepository REPOSITORY = new PackRepository(Services.PLATFORM.getGameDir().resolve(DIRECTORY));
    private static final List<Runnable> RELOAD_LISTENERS = new ArrayList<>();
    private static final ExecutorService RELOADER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Talking Villagers reload");
        thread.setDaemon(true);
        return thread;
    });
    /** A reload that was asked for but has not started yet; later requests join it. Guarded by the class. */
    private static @Nullable CompletableFuture<PackIndex> queued;

    private Voicepacks() {
    }

    public static PackRepository repository() {
        return REPOSITORY;
    }

    public static PackIndex index() {
        return REPOSITORY.index();
    }

    /**
     * Called after every reload, e.g. to tell connected players about the new packs.
     */
    public static synchronized void onReload(Runnable listener) {
        RELOAD_LISTENERS.add(listener);
    }

    /**
     * Reloads the packs on a background thread, so neither the game nor the server waits for reading and hashing.
     * Requests while a reload is waiting to start share it; a reload that already runs is followed by a new one, so
     * the result always includes changes made before the call.
     */
    public static synchronized CompletableFuture<PackIndex> reloadAsync() {
        if (queued != null) {
            return queued;
        }
        CompletableFuture<PackIndex> future = new CompletableFuture<>();
        queued = future;
        RELOADER.execute(() -> {
            synchronized (Voicepacks.class) {
                queued = null;
            }
            try {
                future.complete(reload());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Reloads the packs on the calling thread. Unchanged files are not read again.
     */
    public static PackIndex reload() {
        PackIndex index;
        try {
            index = REPOSITORY.reload();
        } catch (IOException e) {
            TalkingVillagers.LOG.error("Cannot read voicepacks from {}", REPOSITORY.directory(), e);
            return REPOSITORY.index();
        }
        log(index);
        List<Runnable> listeners;
        synchronized (Voicepacks.class) {
            listeners = List.copyOf(RELOAD_LISTENERS);
        }
        listeners.forEach(Runnable::run);
        return index;
    }

    private static void log(PackIndex index) {
        for (LoadedPack pack : index.all()) {
            String name = pack.source().getFileName().toString();
            if (!pack.isUsable()) {
                TalkingVillagers.LOG.error("Voicepack {} cannot be loaded:", name);
                pack.errors().forEach(problem -> TalkingVillagers.LOG.error("  {}", problem));
            } else {
                pack.warnings().stream().map(Problem::toString).forEach(warning -> TalkingVillagers.LOG.warn("Voicepack {}: {}", name, warning));
            }
        }
        if (index.cached().isEmpty()) {
            TalkingVillagers.LOG.info("Loaded {} voicepack(s) with {} voice(s) from {}", index.active().size(), index.voices().size(), REPOSITORY.directory());
        } else {
            TalkingVillagers.LOG.info("Loaded {} voicepack(s) with {} voice(s) from {}, plus {} downloaded from servers",
                    index.active().size(), index.voices().size(), REPOSITORY.directory(), index.cached().size());
        }
    }
}
