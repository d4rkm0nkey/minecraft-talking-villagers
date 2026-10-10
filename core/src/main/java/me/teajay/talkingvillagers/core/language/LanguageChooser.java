package me.teajay.talkingvillagers.core.language;

import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.random.RandomGenerator;

/**
 * Picks languages from the installed voices.
 */
public final class LanguageChooser {
    private LanguageChooser() {
    }

    /**
     * Languages of the given voices, restricted to {@code allowed} unless it is empty. Child voices are ignored, every
     * language a village can get must have adult voices.
     */
    public static List<String> candidates(Collection<ResolvedVoice> voices, List<String> allowed) {
        TreeSet<String> languages = new TreeSet<>();
        for (ResolvedVoice voice : voices) {
            if (!voice.voice().child() && (allowed.isEmpty() || allowed.stream().anyMatch(tag -> Languages.matches(voice.language(), tag)))) {
                languages.add(voice.language());
            }
        }
        return List.copyOf(languages);
    }

    public static Optional<String> random(List<String> candidates, RandomGenerator random) {
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    /**
     * A random language other than {@code avoid}, or any language if there is no other.
     */
    public static Optional<String> randomExcept(List<String> candidates, String avoid, RandomGenerator random) {
        List<String> others = candidates.stream().filter(language -> !Languages.matches(language, avoid)).toList();
        return random(others.isEmpty() ? candidates : others, random);
    }

    /**
     * The first preferred language that has voices.
     */
    public static Optional<String> preferred(List<String> candidates, List<String> preferred) {
        for (String wanted : preferred) {
            for (String candidate : candidates) {
                if (Languages.matches(candidate, wanted)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }
}
