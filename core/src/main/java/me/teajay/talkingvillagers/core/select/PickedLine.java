package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.VoiceLine;

/**
 * @param event the event the line belongs to; differs from the requested one when a parent event was used
 * @param index position in the voice's line list of {@code event}, sent over the network to identify the line
 */
public record PickedLine(VoiceEvent event, int index, VoiceLine line) {
}
