package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.select.VoiceCriteria;

/**
 * Rules for giving entities a voice.
 *
 * @param femaleChance      chance that a new entity is female
 * @param witchFemaleChance the same for witches, which are female in vanilla lore
 * @param strictLanguage    never fall back to a voice in another language
 */
public record AssignmentSettings(double femaleChance, double witchFemaleChance, VoiceCriteria.ChildFallback childFallback, boolean strictLanguage) {
    public static final AssignmentSettings DEFAULT = new AssignmentSettings(0.5, 1.0, VoiceCriteria.ChildFallback.VANILLA, false);

    public double femaleChance(EntityKind kind) {
        return kind == EntityKind.WITCH ? witchFemaleChance : femaleChance;
    }
}
