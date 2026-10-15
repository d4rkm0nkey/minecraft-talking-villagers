package me.teajay.talkingvillagers.core.select;

import me.teajay.talkingvillagers.core.model.ResolvedVoice;

import java.util.random.RandomGenerator;

/**
 * A chosen voice and which criteria had to be relaxed to find it.
 *
 * @param adultForChild an adult voice was chosen for a child because no child voice exists
 */
public record Selection(ResolvedVoice voice, boolean languageRelaxed, boolean genderRelaxed, boolean adultForChild) {
    /** Pitch added when a child speaks with an adult voice. */
    public static final float CHILD_PITCH_BOOST = 0.4f;

    public float samplePitch(RandomGenerator random) {
        float pitch = voice.voice().pitch().sample(random);
        return adultForChild ? Math.min(2.0f, pitch + CHILD_PITCH_BOOST) : pitch;
    }
}
