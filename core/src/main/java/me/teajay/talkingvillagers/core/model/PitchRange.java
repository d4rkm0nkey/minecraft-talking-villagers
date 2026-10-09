package me.teajay.talkingvillagers.core.model;

import java.util.random.RandomGenerator;

public record PitchRange(float min, float max) {
    public static final float LOWEST = 0.5f;
    public static final float HIGHEST = 2.0f;
    public static final PitchRange DEFAULT = new PitchRange(0.95f, 1.05f);

    public PitchRange {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
    }

    public float sample(RandomGenerator random) {
        return min == max ? min : random.nextFloat(min, max);
    }
}
