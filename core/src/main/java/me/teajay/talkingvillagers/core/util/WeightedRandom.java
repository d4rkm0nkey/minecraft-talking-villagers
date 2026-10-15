package me.teajay.talkingvillagers.core.util;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

public final class WeightedRandom {
    private WeightedRandom() {
    }

    /**
     * Picks an element with a probability proportional to its weight. Elements with a weight below 1 count as 1.
     */
    public static <T> Optional<T> pick(List<T> elements, ToIntFunction<T> weight, RandomGenerator random) {
        if (elements.isEmpty()) {
            return Optional.empty();
        }
        int total = 0;
        for (T element : elements) {
            total += Math.max(1, weight.applyAsInt(element));
        }
        int roll = random.nextInt(total);
        for (T element : elements) {
            roll -= Math.max(1, weight.applyAsInt(element));
            if (roll < 0) {
                return Optional.of(element);
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
