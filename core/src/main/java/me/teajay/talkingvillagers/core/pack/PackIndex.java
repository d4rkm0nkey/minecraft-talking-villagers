package me.teajay.talkingvillagers.core.pack;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.util.Versions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Immutable snapshot of the installed voicepacks.
 *
 * @param all     every pack file found, including broken, disabled and shadowed ones
 * @param active  the packs in use, one per pack id
 * @param byHash  every usable pack by its SHA-256, so a line can be played from exactly the file the server meant
 */
public record PackIndex(
        List<LoadedPack> all,
        Map<String, LoadedPack> active,
        Map<String, LoadedPack> byHash,
        Map<VoiceRef, ResolvedVoice> voices,
        Map<EntityKind, List<ResolvedVoice>> voicesByKind
) {
    public static final PackIndex EMPTY = build(List.of(), id -> true);

    /**
     * @param enabled whether a pack id is enabled by the user
     */
    public static PackIndex build(List<LoadedPack> packs, Predicate<String> enabled) {
        Map<String, LoadedPack> active = new LinkedHashMap<>();
        Map<String, LoadedPack> byHash = new LinkedHashMap<>();
        // newest version first, so it wins when two files contain the same pack id
        Comparator<LoadedPack> newestFirst = Comparator.comparing((LoadedPack loaded) -> loaded.pack().orElseThrow().version(), Versions.COMPARATOR).reversed();
        packs.stream()
                .filter(LoadedPack::isUsable)
                .sorted(newestFirst.thenComparing(loaded -> loaded.source().getFileName().toString()))
                .forEach(loaded -> {
                    byHash.putIfAbsent(loaded.sha256(), loaded);
                    String id = loaded.pack().orElseThrow().id();
                    if (enabled.test(id)) {
                        active.putIfAbsent(id, loaded);
                    }
                });

        Map<VoiceRef, ResolvedVoice> voices = new LinkedHashMap<>();
        Map<EntityKind, List<ResolvedVoice>> byKind = new EnumMap<>(EntityKind.class);
        active.values().stream()
                .flatMap(loaded -> loaded.pack().orElseThrow().resolvedVoices().stream())
                .forEach(voice -> {
                    voices.put(voice.ref(), voice);
                    voice.voice().entities().forEach(kind -> byKind.computeIfAbsent(kind, k -> new ArrayList<>()).add(voice));
                });
        byKind.replaceAll((kind, list) -> List.copyOf(list));
        return new PackIndex(List.copyOf(packs), Map.copyOf(active), Map.copyOf(byHash), Map.copyOf(voices), Map.copyOf(byKind));
    }

    public Collection<LoadedPack> activePacks() {
        return active.values();
    }

    public Optional<VoicePack> pack(String id) {
        return Optional.ofNullable(active.get(id)).flatMap(LoadedPack::pack);
    }

    public Optional<ResolvedVoice> voice(VoiceRef ref) {
        return Optional.ofNullable(voices.get(ref));
    }

    public List<ResolvedVoice> voicesFor(EntityKind kind) {
        return voicesByKind.getOrDefault(kind, List.of());
    }

    public Optional<LoadedPack> byHash(String sha256) {
        return Optional.ofNullable(byHash.get(sha256));
    }

    /**
     * The pack a voice belongs to, as file, e.g. to tell clients which file holds the line.
     */
    public Optional<LoadedPack> packOf(VoiceRef ref) {
        return Optional.ofNullable(active.get(ref.packId()));
    }

    public Set<String> languages() {
        Set<String> languages = new TreeSet<>();
        voices.values().forEach(voice -> languages.add(voice.language()));
        return languages;
    }
}
