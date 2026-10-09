package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.util.WeightedRandom;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Picks a voice for an entity.
 *
 * <p>Criteria are relaxed step by step until a voice is found:
 * <ol>
 *     <li>all criteria</li>
 *     <li>any language, unless the language is strict</li>
 *     <li>any gender</li>
 *     <li>for children only, and only with {@link VoiceCriteria.ChildFallback#PITCH_UP_ADULT}: steps 1-3 with adult voices</li>
 * </ol>
 * A child never gets an adult voice otherwise, and an adult never gets a child voice.
 */
public final class VoiceSelector {
    private VoiceSelector() {
    }

    public static Optional<Selection> select(VoiceCriteria criteria, Collection<ResolvedVoice> pool, RandomGenerator random) {
        Optional<Selection> selection = selectAge(criteria, criteria.child(), pool, random);
        if (selection.isEmpty() && criteria.child() && criteria.childFallback() == VoiceCriteria.ChildFallback.PITCH_UP_ADULT) {
            selection = selectAge(criteria, false, pool, random)
                    .map(found -> new Selection(found.voice(), found.languageRelaxed(), found.genderRelaxed(), true));
        }
        return selection;
    }

    private static Optional<Selection> selectAge(VoiceCriteria criteria, boolean child, Collection<ResolvedVoice> pool, RandomGenerator random) {
        List<ResolvedVoice> candidates = pool.stream()
                .filter(voice -> voice.voice().entities().contains(criteria.kind()))
                .filter(voice -> voice.voice().child() == child)
                .toList();

        Optional<ResolvedVoice> found = pick(criteria, candidates, true, true, random);
        if (found.isPresent()) {
            return found.map(voice -> new Selection(voice, false, false, false));
        }
        if (!criteria.strictLanguage()) {
            found = pick(criteria, candidates, false, true, random);
            if (found.isPresent()) {
                return found.map(voice -> new Selection(voice, true, false, false));
            }
        }
        found = pick(criteria, candidates, true, false, random);
        if (found.isPresent()) {
            return found.map(voice -> new Selection(voice, false, true, false));
        }
        if (!criteria.strictLanguage()) {
            found = pick(criteria, candidates, false, false, random);
            if (found.isPresent()) {
                return found.map(voice -> new Selection(voice, true, true, false));
            }
        }
        return Optional.empty();
    }

    private static Optional<ResolvedVoice> pick(VoiceCriteria criteria, List<ResolvedVoice> candidates,
                                                boolean matchLanguage, boolean matchGender, RandomGenerator random) {
        List<ResolvedVoice> matching = candidates.stream()
                .filter(voice -> !matchLanguage || criteria.language().map(wanted -> Languages.matches(voice.language(), wanted)).orElse(true))
                .filter(voice -> !matchGender || criteria.gender().map(wanted -> voice.voice().gender().fits(wanted)).orElse(true))
                .toList();
        if (matching.isEmpty()) {
            return Optional.empty();
        }

        // avoided voices are only skipped if something else fits
        List<ResolvedVoice> preferred = matching.stream().filter(voice -> !criteria.avoid().contains(voice.ref())).toList();
        if (preferred.isEmpty()) {
            preferred = matching;
        }

        if (!criteria.usage().isEmpty()) {
            int leastUsed = preferred.stream().mapToInt(voice -> usage(criteria, voice)).min().orElse(0);
            preferred = preferred.stream().filter(voice -> usage(criteria, voice) == leastUsed).toList();
        }
        return WeightedRandom.pick(preferred, voice -> voice.voice().weight(), random);
    }

    private static int usage(VoiceCriteria criteria, ResolvedVoice voice) {
        return criteria.usage().getOrDefault(voice.ref(), 0);
    }
}
