package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;

/**
 * Picks the line a voice says for an event.
 */
public final class LineSelector {
    /** Chance to prefer a line whose conditions match (e.g. a farmer line) over a general line. */
    public static final double SPECIFIC_LINE_CHANCE = 0.5;

    private LineSelector() {
    }

    /**
     * @param avoidIndex index of the line said last time for this event, not repeated if there is another choice
     * @return empty if the voice has nothing to say, also not via the event's parents
     */
    public static Optional<PickedLine> pick(Voice voice, VoiceEvent event, LineContext context, OptionalInt avoidIndex, RandomGenerator random) {
        for (Optional<VoiceEvent> current = Optional.of(event); current.isPresent(); current = current.get().parent()) {
            Optional<PickedLine> picked = pickFrom(voice, current.get(), context, current.get() == event ? avoidIndex : OptionalInt.empty(), random);
            if (picked.isPresent()) {
                return picked;
            }
        }
        return Optional.empty();
    }

    private static Optional<PickedLine> pickFrom(Voice voice, VoiceEvent event, LineContext context, OptionalInt avoidIndex, RandomGenerator random) {
        List<VoiceLine> lines = voice.lines(event);
        List<Integer> specific = new ArrayList<>();
        List<Integer> general = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            VoiceLine line = lines.get(i);
            if (line.conditions().isEmpty()) {
                general.add(i);
            } else if (context.matches(line.conditions())) {
                specific.add(i);
            }
        }

        List<Integer> candidates;
        if (specific.isEmpty()) {
            candidates = general;
        } else if (general.isEmpty() || random.nextDouble() < SPECIFIC_LINE_CHANCE) {
            candidates = specific;
        } else {
            candidates = general;
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() > 1 && avoidIndex.isPresent()) {
            int avoid = avoidIndex.getAsInt();
            candidates = candidates.stream().filter(index -> index != avoid).toList();
        }
        int index = candidates.get(random.nextInt(candidates.size()));
        return Optional.of(new PickedLine(event, index, lines.get(index)));
    }
}
