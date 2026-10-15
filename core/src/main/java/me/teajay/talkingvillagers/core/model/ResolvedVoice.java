package me.teajay.talkingvillagers.core.model;

/**
 * A voice together with the pack it belongs to, ready for selection.
 *
 * @param language effective language, the voice override or else the pack language
 */
public record ResolvedVoice(VoiceRef ref, Voice voice, String language) {
}
