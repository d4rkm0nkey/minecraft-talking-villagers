package me.teajay.talkingvillagers.pack;

import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.Problem;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.pack.PackRepository;
import me.teajay.talkingvillagers.platform.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The voicepacks in {@code <game dir>/voicepacks}. Client and integrated server share this instance.
 */
public final class Voicepacks {
    public static final String DIRECTORY = "voicepacks";
    private static final PackRepository REPOSITORY = new PackRepository(Services.PLATFORM.getGameDir().resolve(DIRECTORY));
    private static final List<Runnable> RELOAD_LISTENERS = new ArrayList<>();

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
