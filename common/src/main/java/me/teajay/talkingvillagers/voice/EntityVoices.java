package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.model.Weather;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.select.LineContext;
import me.teajay.talkingvillagers.core.voice.AssignmentSettings;
import me.teajay.talkingvillagers.core.voice.EventPolicies;
import me.teajay.talkingvillagers.core.voice.SpeakerState;
import me.teajay.talkingvillagers.core.voice.VillagerChatter;
import me.teajay.talkingvillagers.core.voice.VoiceAssigner;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.pack.Voicepacks;
import me.teajay.talkingvillagers.platform.Services;
import me.teajay.talkingvillagers.server.SpeechService;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

/**
 * Server side voice logic for all entities: gives them a voice and turns game events into spoken lines.
 * Mixins only report what happened and call into this class.
 */
public final class EntityVoices {
    /** Radius in blocks in which voices are counted to give neighbours different voices. */
    private static final double NEIGHBOURHOOD = 48.0;
    private static final Map<Entity, SpeakerState> STATES = new WeakHashMap<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private EntityVoices() {
    }

    public static Optional<EntityKind> kindOf(Entity entity) {
        if (entity instanceof Villager) {
            return Optional.of(EntityKind.VILLAGER);
        }
        return Optional.empty();
    }

    public static SpeakerState state(Entity entity) {
        return STATES.computeIfAbsent(entity, e -> new SpeakerState());
    }

    // --- assignment --------------------------------------------------------------------------------------------------

    /**
     * The entity's voice, assigning one first if it has none or needs a new one (e.g. after growing up).
     */
    public static Optional<ResolvedVoice> voice(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Optional<EntityKind> kind = kindOf(entity);
        if (kind.isEmpty()) {
            return Optional.empty();
        }
        PackIndex index = Voicepacks.index();
        Optional<VoiceIdentity> current = Services.ATTACHMENTS.getIdentity(entity);
        boolean child = entity.isBaby();
        if (!VoiceAssigner.needsAssignment(current, child, index::voice)) {
            return current.flatMap(VoiceIdentity::voice).flatMap(index::voice);
        }
        SpeakerState state = state(entity);
        boolean sameSituation = current.isPresent() && current.get().voice().isEmpty() && current.get().child() == child;
        if (sameSituation && state.alreadyFailedFor(index)) {
            // nothing fits and the packs did not change since the last try
            return Optional.empty();
        }

        return assign(entity, kind.get(), current, level, index);
    }

    private static Optional<ResolvedVoice> assign(LivingEntity entity, EntityKind kind, Optional<VoiceIdentity> current, ServerLevel level, PackIndex index) {
        VoiceIdentity identity = VoiceAssigner.assign(current, kind, entity.isBaby(), index.voicesFor(kind),
                neighbourVoices(entity, level), AssignmentSettings.DEFAULT, RANDOM);
        Services.ATTACHMENTS.setIdentity(entity, identity);
        if (identity.voice().isEmpty()) {
            state(entity).assignmentFailedFor(index);
        }
        return identity.voice().flatMap(index::voice);
    }

    private static Map<VoiceRef, Integer> neighbourVoices(LivingEntity entity, ServerLevel level) {
        Map<VoiceRef, Integer> usage = new HashMap<>();
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(NEIGHBOURHOOD), e -> e != entity)) {
            Services.ATTACHMENTS.getIdentity(other).flatMap(VoiceIdentity::voice).ifPresent(ref -> usage.merge(ref, 1, Integer::sum));
        }
        return usage;
    }

    /**
     * Gives the entity a new voice by the usual rules, e.g. for {@code /tv voice randomize}.
     */
    public static Optional<ResolvedVoice> reassign(LivingEntity entity) {
        Optional<EntityKind> kind = kindOf(entity);
        if (kind.isEmpty() || !(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return assign(entity, kind.get(), Services.ATTACHMENTS.getIdentity(entity), level, Voicepacks.index());
    }

    /**
     * Whether the entity speaks with a voice, without assigning one. Cheap enough to call every tick.
     */
    public static boolean hasVoice(Entity entity) {
        return Services.ATTACHMENTS.getIdentity(entity).flatMap(VoiceIdentity::voice).isPresent();
    }

    // --- events ------------------------------------------------------------------------------------------------------

    /**
     * Called for every sound an entity makes on the server.
     *
     * @return true if the voice takes over and the vanilla sound must not play
     */
    public static boolean onVanillaSound(LivingEntity entity, @Nullable SoundEvent sound) {
        if (sound == null) {
            return false;
        }
        Optional<VoiceEvent> event = kindOf(entity).flatMap(kind -> VanillaSounds.event(kind, sound));
        return event.isPresent() && onEvent(entity, event.get(), sound);
    }

    /**
     * Voices an event if its cooldown and chance allow it.
     *
     * @param vanillaSound sound played for players without the pack, or null
     * @return true if the entity's voice has lines for this event; the vanilla sound is then replaced, even when the
     * cooldown keeps the entity quiet this time
     */
    public static boolean onEvent(LivingEntity entity, VoiceEvent event, @Nullable SoundEvent vanillaSound) {
        Optional<ResolvedVoice> voice = voice(entity);
        if (voice.isEmpty() || !hasLines(voice.get(), event)) {
            return false;
        }
        long now = entity.level().getGameTime();
        SpeakerState state = state(entity);
        if (state.mayVoice(event, EventPolicies.of(event), now, RANDOM)) {
            speak(entity, voice.get(), event, vanillaSound);
        }
        return true;
    }

    public static void onEvent(LivingEntity entity, VoiceEvent event) {
        onEvent(entity, event, null);
    }

    private static boolean hasLines(ResolvedVoice voice, VoiceEvent event) {
        for (Optional<VoiceEvent> current = Optional.of(event); current.isPresent(); current = current.get().parent()) {
            if (voice.voice().hasLines(current.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean speak(LivingEntity entity, ResolvedVoice voice, VoiceEvent event, @Nullable SoundEvent vanillaSound) {
        Holder<SoundEvent> fallback = vanillaSound == null ? null : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(vanillaSound);
        float pitch = Services.ATTACHMENTS.getIdentity(entity).map(VoiceIdentity::pitch).orElse(1.0f);
        return SpeechService.speak(entity, state(entity), voice, event, context(entity), pitch, 1.0f, EventPolicies.of(event).interrupts(), fallback);
    }

    /**
     * A villager grew up: it says goodbye to its child voice and gets an adult voice.
     */
    public static void onGrowUp(LivingEntity entity) {
        Services.ATTACHMENTS.getIdentity(entity).flatMap(VoiceIdentity::voice).flatMap(Voicepacks.index()::voice)
                .ifPresent(childVoice -> speak(entity, childVoice, EventCatalog.GROW_UP, null));
        voice(entity);
    }

    /**
     * Ambient sounds of entities with a voice are rarer than vanilla's, because they are full voice lines.
     */
    public static int ambientInterval(Entity entity, int vanillaInterval) {
        return hasVoice(entity) ? Math.max(vanillaInterval, EventPolicies.AMBIENT_INTERVAL) : vanillaInterval;
    }

    /**
     * Called every server tick of a villager; does real work once per {@link VillagerChatter#INTERVAL} ticks.
     */
    public static void tickVillager(Villager villager, ServerLevel level) {
        if ((level.getGameTime() + villager.getId()) % VillagerChatter.INTERVAL != 0 || !villager.isAlive()) {
            return;
        }
        Optional<ResolvedVoice> voice = voice(villager);
        if (voice.isEmpty() || villager.isSleeping()) {
            return;
        }
        SpeakerState state = state(villager);
        if (state.updatePanic(villager.getBrain().isActive(Activity.PANIC))) {
            onEvent(villager, EventCatalog.PANIC);
            return;
        }
        if (villager.isTrading()) {
            return;
        }

        Optional<Player> player = villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        VillagerChatter.Situation situation = new VillagerChatter.Situation(
                level.getGameTime(),
                player.map(p -> OptionalDouble.of(p.distanceTo(villager))).orElse(OptionalDouble.empty()),
                player.filter(p -> p.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)).isPresent(),
                weather(level),
                level.isBrightOutside());
        VillagerChatter.decide(state, situation, RANDOM).ifPresent(event -> speak(villager, voice.get(), event, null));
    }

    // --- context -----------------------------------------------------------------------------------------------------

    public static LineContext context(Entity entity) {
        Optional<String> profession = Optional.empty();
        if (entity instanceof Villager villager) {
            profession = villager.getVillagerData().profession().unwrapKey().map(key -> {
                var id = key.identifier();
                return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
            });
        }
        return new LineContext(profession, entity.level() instanceof ServerLevel level ? Optional.of(weather(level)) : Optional.empty());
    }

    private static Weather weather(ServerLevel level) {
        if (level.isThundering()) {
            return Weather.THUNDER;
        }
        return level.isRaining() ? Weather.RAIN : Weather.CLEAR;
    }
}
