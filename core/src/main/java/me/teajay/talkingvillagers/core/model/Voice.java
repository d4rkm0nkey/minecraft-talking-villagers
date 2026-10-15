package me.teajay.talkingvillagers.core.model;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A voice as declared in the manifest.
 *
 * @param language  overrides the pack language when present
 * @param lines     lines per event id; only known events are kept
 * @param legacyIds ids this voice had in version 1, so villagers from old worlds keep it
 */
public record Voice(
        String id,
        String name,
        Set<EntityKind> entities,
        Gender gender,
        boolean child,
        Optional<String> language,
        PitchRange pitch,
        int weight,
        Map<String, List<VoiceLine>> lines,
        List<String> legacyIds
) {
    public Voice {
        entities = Set.copyOf(entities);
        lines = Map.copyOf(lines);
        legacyIds = List.copyOf(legacyIds);
    }

    public Voice(String id, String name, Set<EntityKind> entities, Gender gender, boolean child, Optional<String> language,
                 PitchRange pitch, int weight, Map<String, List<VoiceLine>> lines) {
        this(id, name, entities, gender, child, language, pitch, weight, lines, List.of());
    }

    public List<VoiceLine> lines(VoiceEvent event) {
        return lines.getOrDefault(event.id(), List.of());
    }

    public boolean hasLines(VoiceEvent event) {
        return !lines(event).isEmpty();
    }
}
