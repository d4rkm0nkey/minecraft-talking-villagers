package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.voice.VoiceAssigner;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Takes over the voice of villagers saved by version 1 ({@code VoiceName} and {@code VoicePitch}), if a pack has a voice
 * that lists that name in {@code legacyIds} or has it as id. Otherwise the villager gets a new voice the usual way.
 */
public final class LegacyVoices {
    private static final String VOICE_NAME = "VoiceName";
    private static final String VOICE_PITCH = "VoicePitch";
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private LegacyVoices() {
    }

    public static void migrate(LivingEntity entity, ValueInput input) {
        Optional<String> name = input.getString(VOICE_NAME);
        if (name.isEmpty() || Services.ATTACHMENTS.getIdentity(entity).isPresent()) {
            return;
        }
        Optional<ResolvedVoice> voice = Voicepacks.index().voices().values().stream()
                .filter(candidate -> candidate.voice().child() == entity.isBaby())
                .filter(candidate -> candidate.voice().legacyIds().contains(name.get()) || candidate.ref().voiceId().equals(name.get()))
                .sorted((a, b) -> Boolean.compare(!a.voice().legacyIds().contains(name.get()), !b.voice().legacyIds().contains(name.get())))
                .findFirst();
        if (voice.isEmpty()) {
            return;
        }
        float pitch = input.getFloatOr(VOICE_PITCH, 1.0f);
        Gender gender = voice.get().voice().gender() == Gender.NEUTRAL
                ? VoiceAssigner.rollGender(Configs.server().assignment(), EntityKind.VILLAGER, RANDOM)
                : voice.get().voice().gender();
        Services.ATTACHMENTS.setIdentity(entity, new VoiceIdentity(gender, Optional.of(voice.get().ref()),
                pitch < 0.1f ? 1.0f : pitch, entity.isBaby(), Optional.of(voice.get().language()), false, false));
    }
}
