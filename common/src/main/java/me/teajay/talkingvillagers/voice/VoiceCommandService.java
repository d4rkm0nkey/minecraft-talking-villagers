package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.voice.VoiceAssigner;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.language.LanguageData;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import me.teajay.talkingvillagers.server.SpeechService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SplittableRandom;

/**
 * What the {@code /tv} commands do, without the command parsing, so the GUI can use the same operations.
 */
public final class VoiceCommandService {
    private static final SplittableRandom RANDOM = new SplittableRandom();
    /** Events tried in this order when a voice is played as a sample. */
    private static final List<VoiceEvent> SAMPLE_EVENTS = List.of(EventCatalog.GREETING, EventCatalog.AMBIENT, EventCatalog.RANDOM,
            EventCatalog.HURT);

    private VoiceCommandService() {
    }

    public enum SetResult {
        OK,
        /** The entity cannot have a voice at all. */
        UNSUPPORTED,
        /** The voice is made for other entity types. */
        WRONG_KIND,
        /** A child voice for an adult or the other way round. */
        WRONG_AGE
    }

    /**
     * The entity's identity, giving it a voice first if it has none yet. Entities that are still waiting for their
     * village get an identity without voice right away, so commands can change their gender or language.
     *
     * @return empty only for entities that cannot have a voice
     */
    public static Optional<VoiceIdentity> identity(LivingEntity entity) {
        Optional<EntityKind> kind = EntityVoices.kindOf(entity);
        if (kind.isEmpty()) {
            // e.g. a zombie villager, which keeps the voice data of the villager it was
            return Optional.empty();
        }
        EntityVoices.voice(entity);
        Optional<VoiceIdentity> identity = Services.ATTACHMENTS.getIdentity(entity);
        if (identity.isEmpty()) {
            Gender gender = VoiceAssigner.rollGender(Configs.server().assignment(), kind.get(), RANDOM);
            identity = Optional.of(VoiceIdentity.withoutVoice(gender, entity.isBaby(), Optional.empty()));
            Services.ATTACHMENTS.setIdentity(entity, identity.get());
        }
        return identity;
    }

    /**
     * @param force also set voices made for other entity types or ages; such voices are marked manual and kept
     */
    public static SetResult setVoice(LivingEntity entity, ResolvedVoice voice, boolean force) {
        Optional<EntityKind> kind = EntityVoices.kindOf(entity);
        if (kind.isEmpty()) {
            return SetResult.UNSUPPORTED;
        }
        if (!force && !voice.voice().entities().contains(kind.get())) {
            return SetResult.WRONG_KIND;
        }
        if (!force && voice.voice().child() != entity.isBaby()) {
            return SetResult.WRONG_AGE;
        }
        VoiceIdentity current = identity(entity).orElseThrow();
        Services.ATTACHMENTS.setIdentity(entity, current.withVoice(voice.ref(), voice.voice().pitch().sample(RANDOM), entity.isBaby(), true)
                .adoptingGenderOf(voice.voice().gender()));
        return SetResult.OK;
    }

    /**
     * Gives the entity a different voice by the usual rules.
     *
     * @param keepGender if false, the entity's gender is rolled again as well
     */
    public static Optional<ResolvedVoice> randomize(LivingEntity entity, boolean keepGender) {
        Optional<VoiceIdentity> current = identity(entity);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        VoiceIdentity identity = current.get();
        Gender gender = keepGender ? identity.gender() : EntityVoices.kindOf(entity)
                .map(kind -> VoiceAssigner.rollGender(Configs.server().assignment(), kind, RANDOM))
                .orElse(identity.gender());
        // keep the current voice in the identity, so the assigner avoids picking it again
        VoiceIdentity unlocked = new VoiceIdentity(gender, gender == identity.gender() ? identity.voice() : Optional.empty(),
                identity.pitch(), entity.isBaby(), identity.language(), false, false);
        Services.ATTACHMENTS.setIdentity(entity, unlocked);
        return EntityVoices.reassign(entity);
    }

    /**
     * The entity keeps its vanilla sounds until it gets a voice by command again.
     */
    public static boolean mute(LivingEntity entity) {
        Optional<VoiceIdentity> current = identity(entity);
        current.ifPresent(identity -> Services.ATTACHMENTS.setIdentity(entity, identity.mute()));
        return current.isPresent();
    }

    public static Optional<ResolvedVoice> setGender(LivingEntity entity, Gender gender) {
        Optional<VoiceIdentity> current = identity(entity);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        Services.ATTACHMENTS.setIdentity(entity, current.get().withGender(gender));
        return EntityVoices.reassign(entity);
    }

    public static Optional<ResolvedVoice> setLanguage(LivingEntity entity, String language) {
        Optional<VoiceIdentity> current = identity(entity);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        Services.ATTACHMENTS.setIdentity(entity, current.get().withLanguage(language));
        return EntityVoices.reassign(entity);
    }

    public static Optional<String> villageLanguage(ServerLevel level, BlockPos pos) {
        return LanguageData.get(level).villages().find(pos.getX(), pos.getY(), pos.getZ(), Configs.server().villageRadius())
                .map(anchor -> anchor.language());
    }

    /**
     * @param reassign also give all villagers of the village a voice in the new language
     * @return number of villagers that got a new voice
     */
    public static int setVillageLanguage(ServerLevel level, BlockPos pos, String language, boolean reassign) {
        int radius = Configs.server().villageRadius();
        LanguageData data = LanguageData.get(level);
        data.villages().setLanguage(pos.getX(), pos.getY(), pos.getZ(), radius, language);
        data.setDirty();
        if (!reassign) {
            return 0;
        }
        int changed = 0;
        for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(radius))) {
            boolean manual = Services.ATTACHMENTS.getIdentity(villager).map(VoiceIdentity::manual).orElse(false);
            if (!manual) {
                setLanguage(villager, language);
                changed++;
            }
        }
        return changed;
    }

    /**
     * @param lineIndex a specific line, or empty for a random one
     * @return false if the entity has no voice or the voice has no such line
     */
    public static boolean say(LivingEntity entity, VoiceEvent event, OptionalInt lineIndex) {
        Optional<ResolvedVoice> voice = EntityVoices.voice(entity);
        if (voice.isEmpty()) {
            return false;
        }
        float pitch = Services.ATTACHMENTS.getIdentity(entity).map(VoiceIdentity::pitch).orElse(1.0f);
        if (lineIndex.isPresent()) {
            return SpeechService.speakLine(entity, EntityVoices.state(entity), voice.get(), event, lineIndex.getAsInt(), pitch, 1.0f);
        }
        return SpeechService.speak(entity, EntityVoices.state(entity), voice.get(), event, EntityVoices.context(entity), pitch, 1.0f, true, null);
    }

    /**
     * An event the voice has lines for, to play it as a sample.
     */
    public static Optional<VoiceEvent> sampleEvent(ResolvedVoice voice) {
        return SAMPLE_EVENTS.stream().filter(voice.voice()::hasLines).findFirst()
                .or(() -> EventCatalog.all().stream().filter(voice.voice()::hasLines).findFirst());
    }

    /**
     * How many loaded entities use each voice.
     */
    public static Map<VoiceRef, Integer> usage(ServerLevel level) {
        Map<VoiceRef, Integer> usage = new HashMap<>();
        for (Entity entity : level.getAllEntities()) {
            Services.ATTACHMENTS.getIdentity(entity).flatMap(VoiceIdentity::voice).ifPresent(ref -> usage.merge(ref, 1, Integer::sum));
        }
        return usage;
    }

    public static Optional<ResolvedVoice> resolve(VoiceRef ref) {
        return Voicepacks.index().voice(ref);
    }
}
