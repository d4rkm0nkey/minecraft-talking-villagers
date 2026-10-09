package me.teajay.talkingvillagers.core.voice;

import me.teajay.talkingvillagers.core.select.VoiceCriteria;

/**
 * Rules for giving entities a voice.
 *
 * @param femaleChance   chance that a new entity is female
 * @param strictLanguage never fall back to a voice in another language
 */
public record AssignmentSettings(double femaleChance, VoiceCriteria.ChildFallback childFallback, boolean strictLanguage) {
    public static final AssignmentSettings DEFAULT = new AssignmentSettings(0.5, VoiceCriteria.ChildFallback.VANILLA, false);
}
