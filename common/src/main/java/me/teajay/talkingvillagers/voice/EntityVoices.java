package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.language.LanguageChooser;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.ResolvedVoice;
import me.teajay.talkingvillagers.core.model.VoiceRef;
import me.teajay.talkingvillagers.core.model.Weather;
import me.teajay.talkingvillagers.core.pack.PackIndex;
import me.teajay.talkingvillagers.core.select.LineContext;
import me.teajay.talkingvillagers.core.voice.EventPolicies;
import me.teajay.talkingvillagers.core.voice.SpeakerState;
import me.teajay.talkingvillagers.core.voice.Threat;
import me.teajay.talkingvillagers.core.voice.VillagerActivity;
import me.teajay.talkingvillagers.core.voice.Chatter;
import me.teajay.talkingvillagers.core.voice.HostileChatter;
import me.teajay.talkingvillagers.core.voice.VoiceAssigner;
import me.teajay.talkingvillagers.core.voice.VoiceIdentity;
import me.teajay.talkingvillagers.config.Configs;
import me.teajay.talkingvillagers.config.ServerConfig;
import me.teajay.talkingvillagers.language.LanguageResolver;
import me.teajay.talkingvillagers.mixin.PatrollingMonsterAccessor;
import me.teajay.talkingvillagers.mixin.VindicatorAccessor;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import java.util.WeakHashMap;

/**
 * Server side voice logic for all entities: gives them a voice and turns game events into spoken lines.
 * Mixins only report what happened and call into this class.
 */
public final class EntityVoices {
    /** Radius in blocks in which voices are counted to give neighbours different voices. */
    private static final double NEIGHBOURHOOD = 48.0;
    private static final float HIDE_VOLUME = 0.5f;
    /** Allies within this radius notice when a witch or illager dies. */
    private static final double ALLY_RADIUS = 16.0;
    private static final Map<Entity, SpeakerState> STATES = new WeakHashMap<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();

    private EntityVoices() {
    }

    public static Optional<EntityKind> kindOf(Entity entity) {
        return EntityKinds.of(entity);
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
        ServerConfig config = Configs.server();
        Optional<String> wantedLanguage = Optional.empty();
        if (current.flatMap(VoiceIdentity::language).isEmpty()) {
            LanguageResolver.Resolution resolution = LanguageResolver.resolve(entity, kind, level, index, config.language());
            if (resolution.pending()) {
                // e.g. a new villager that does not know its village yet
                return Optional.empty();
            }
            wantedLanguage = resolution.language();
        }
        VoiceIdentity identity = VoiceAssigner.assign(current, kind, entity.isBaby(), index.voicesFor(kind),
                neighbourVoices(entity, level), config.assignment(), wantedLanguage, RANDOM);
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
        Optional<EntityKind> kind = kindOf(entity);
        Optional<VoiceEvent> event = kind.flatMap(k -> VanillaSounds.event(k, sound));
        if (event.isPresent() && event.get() == EventCatalog.AMBIENT && entity instanceof Mob mob && EventCatalog.HOSTILE.contains(kind.get())) {
            // witches and illagers talk according to their state: idle, on patrol, in a raid or fighting
            event = Optional.of(HostileChatter.ambientEvent(kind.get(), hostileSituation(mob)));
        } else if (event.isPresent() && entity instanceof Villager villager) {
            event = Optional.of(villagerSoundEvent(villager, event.get()));
        }
        return event.isPresent() && onEvent(entity, event.get(), sound);
    }

    /**
     * Villagers call for help instead of humming while they panic, and react to being hit by a player.
     */
    private static VoiceEvent villagerSoundEvent(Villager villager, VoiceEvent event) {
        if (event == EventCatalog.AMBIENT) {
            return Chatter.ambientEvent(activity(villager));
        }
        boolean hitByPlayerNow = villager.getLastHurtByMob() instanceof Player && villager.getLastHurtByMobTimestamp() == villager.tickCount;
        if (event == EventCatalog.HURT && hitByPlayerNow
                && state(villager).isReady(EventCatalog.ATTACKED_BY_PLAYER, EventPolicies.of(EventCatalog.ATTACKED_BY_PLAYER), villager.level().getGameTime())) {
            return EventCatalog.ATTACKED_BY_PLAYER;
        }
        return event;
    }

    /**
     * Voices an event if its cooldown and chance allow it.
     *
     * @param vanillaSound sound played for players without the pack, or null
     * @return true if the entity's voice has lines for this event; the vanilla sound is then replaced, even when the
     * cooldown keeps the entity quiet this time
     */
    public static boolean onEvent(LivingEntity entity, VoiceEvent event, @Nullable SoundEvent vanillaSound) {
        if (kindOf(entity).filter(event::appliesTo).isEmpty()) {
            return false;
        }
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
        boolean interrupts = EventPolicies.of(event).interrupts();
        if (!interrupts && crowded(entity)) {
            // e.g. a raid: only a few entities talk at once, the rest stay quiet
            return false;
        }
        Holder<SoundEvent> fallback = vanillaSound == null ? null : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(vanillaSound);
        float pitch = Services.ATTACHMENTS.getIdentity(entity).map(VoiceIdentity::pitch).orElse(1.0f);
        // villagers hiding from raiders whisper
        float volume = event == EventCatalog.HIDE ? HIDE_VOLUME : 1.0f;
        return SpeechService.speak(entity, state(entity), voice, event, context(entity), pitch, volume, interrupts, fallback);
    }

    private static boolean crowded(LivingEntity entity) {
        long now = entity.level().getGameTime();
        double radiusSqr = EventPolicies.CROWD_RADIUS * EventPolicies.CROWD_RADIUS;
        int speaking = 0;
        for (Map.Entry<Entity, SpeakerState> entry : STATES.entrySet()) {
            Entity other = entry.getKey();
            if (other != entity && other.level() == entity.level() && entry.getValue().isSpeaking(now)
                    && other.distanceToSqr(entity) <= radiusSqr && ++speaking >= EventPolicies.MAX_CONCURRENT_SPEAKERS) {
                return true;
            }
        }
        return false;
    }

    /**
     * A villager child was born: it speaks the language of one of its parents, wherever it was born.
     */
    public static void onBirth(LivingEntity child, LivingEntity parent, LivingEntity otherParent) {
        Optional<String> language = LanguageChooser.inherit(
                Services.ATTACHMENTS.getIdentity(parent).flatMap(VoiceIdentity::language),
                Services.ATTACHMENTS.getIdentity(otherParent).flatMap(VoiceIdentity::language),
                RANDOM);
        if (language.isPresent() && Services.ATTACHMENTS.getIdentity(child).isEmpty()) {
            EntityKind kind = kindOf(child).orElse(EntityKind.VILLAGER);
            Gender gender = VoiceAssigner.rollGender(Configs.server().assignment(), kind, RANDOM);
            // no voice yet: it is chosen on first use, in the inherited language
            Services.ATTACHMENTS.setIdentity(child, VoiceIdentity.withoutVoice(gender, true, language));
        }
        onEvent(RANDOM.nextBoolean() ? parent : otherParent, EventCatalog.LOVE);
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
     * Called every server tick of villagers and wandering traders; does real work once per {@link Chatter#INTERVAL} ticks.
     */
    public static void tickChatter(LivingEntity entity, ServerLevel level) {
        if ((level.getGameTime() + entity.getId()) % Chatter.INTERVAL != 0 || !entity.isAlive()) {
            return;
        }
        Optional<ResolvedVoice> voice = voice(entity);
        if (voice.isEmpty() || entity.isSleeping()) {
            return;
        }
        SpeakerState state = state(entity);
        if (entity instanceof AbstractVillager merchant && merchant.isTrading()) {
            return;
        }
        Chatter.Situation situation;
        if (entity instanceof Villager villager) {
            Optional<Player> player = villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
            situation = new Chatter.Situation(
                    level.getGameTime(),
                    player.map(p -> OptionalDouble.of(p.distanceTo(villager))).orElse(OptionalDouble.empty()),
                    player.filter(p -> p.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)).isPresent(),
                    weather(level),
                    level.isBrightOutside(),
                    true,
                    activity(villager),
                    threat(villager),
                    player.map(p -> OptionalInt.of(villager.getPlayerReputation(p))).orElse(OptionalInt.empty()));
        } else {
            Optional<Player> player = Optional.ofNullable(level.getNearestPlayer(entity, EventPolicies.GREETING_DISTANCE)).filter(entity::hasLineOfSight);
            situation = Chatter.Situation.calm(
                    level.getGameTime(),
                    player.map(p -> OptionalDouble.of(p.distanceTo(entity))).orElse(OptionalDouble.empty()),
                    false,
                    weather(level),
                    level.isBrightOutside(),
                    false);
        }
        Chatter.decide(state, situation, RANDOM).ifPresent(event -> speak(entity, voice.get(), event, null));
    }

    private static VillagerActivity activity(Villager villager) {
        var brain = villager.getBrain();
        if (brain.isActive(Activity.PANIC)) {
            return VillagerActivity.PANIC;
        }
        if (brain.isActive(Activity.PRE_RAID)) {
            return VillagerActivity.PRE_RAID;
        }
        if (brain.isActive(Activity.HIDE) || brain.isActive(Activity.RAID)) {
            return VillagerActivity.HIDE;
        }
        if (brain.isActive(Activity.CELEBRATE)) {
            return VillagerActivity.CELEBRATE;
        }
        if (brain.isActive(Activity.REST)) {
            return VillagerActivity.REST;
        }
        if (brain.isActive(Activity.WORK)) {
            return VillagerActivity.WORK;
        }
        if (brain.isActive(Activity.PLAY)) {
            return VillagerActivity.PLAY;
        }
        if (brain.isActive(Activity.MEET)) {
            return VillagerActivity.MEET;
        }
        return VillagerActivity.IDLE;
    }

    private static Threat threat(Villager villager) {
        var brain = villager.getBrain();
        Optional<LivingEntity> source = brain.getMemory(MemoryModuleType.NEAREST_HOSTILE)
                .or(() -> brain.getMemory(MemoryModuleType.HURT_BY_ENTITY));
        if (source.isEmpty()) {
            return Threat.NONE;
        }
        LivingEntity threat = source.get();
        if (threat instanceof Player) {
            return Threat.PLAYER;
        }
        if (threat instanceof Zombie) {
            return Threat.ZOMBIE;
        }
        if (threat instanceof Raider || threat instanceof Vex) {
            return Threat.RAIDER;
        }
        return Threat.OTHER;
    }

    /**
     * Event for a villager that starts trading: unfriendly if the player has a bad reputation with it.
     */
    public static VoiceEvent tradeEvent(LivingEntity merchant, Player player) {
        if (merchant instanceof Villager villager && villager.getPlayerReputation(player) <= EventPolicies.UNFRIENDLY_REPUTATION) {
            return EventCatalog.TRADE_UNFRIENDLY;
        }
        return EventCatalog.TRADE;
    }

    /**
     * A new raid wave sounds the horn; the wave's captain (or another raider) shouts.
     */
    public static void onRaidHorn(Raid raid) {
        Raider speaker = raid.getLeader(raid.getGroupsSpawned());
        if (speaker == null || !speaker.isAlive()) {
            speaker = raid.getAllRaiders().stream().filter(Raider::isAlive).filter(EntityVoices::hasVoice).findFirst().orElse(null);
        }
        if (speaker != null) {
            onEvent(speaker, EventCatalog.RAID_HORN);
        }
    }

    /**
     * Called every server tick of a mob; witches and illagers check their fight state once per second.
     */
    public static void tickHostile(Mob mob, ServerLevel level) {
        Optional<EntityKind> kind = kindOf(mob);
        if (kind.isEmpty() || !EventCatalog.HOSTILE.contains(kind.get())
                || (level.getGameTime() + mob.getId()) % Chatter.INTERVAL != 0 || !mob.isAlive()) {
            return;
        }
        Optional<ResolvedVoice> voice = voice(mob);
        if (voice.isEmpty()) {
            return;
        }
        HostileChatter.tick(kind.get(), state(mob), hostileSituation(mob), RANDOM)
                .ifPresent(event -> onEvent(mob, event, null));
    }

    private static HostileChatter.Situation hostileSituation(Mob mob) {
        boolean johnny = mob instanceof VindicatorAccessor vindicator && vindicator.talkingvillagers$isJohnny();
        LivingEntity target = mob.getTarget();
        // witches also target raiders to heal them; that is not a fight
        boolean fighting = target != null && target.isAlive() && (target instanceof Player || johnny);
        boolean inRaid = mob instanceof Raider raider && raider.hasActiveRaid();
        boolean patrolling = mob instanceof PatrollingMonsterAccessor patrol && patrol.talkingvillagers$isPatrolling();
        boolean leader = mob instanceof PatrollingMonster patrol && patrol.isPatrolLeader() && patrolling;
        return new HostileChatter.Situation(
                mob.level().getGameTime(),
                fighting,
                fighting ? OptionalDouble.of(mob.distanceTo(target)) : OptionalDouble.empty(),
                inRaid,
                patrolling,
                leader,
                mob.getHealth() / mob.getMaxHealth(),
                johnny);
    }

    /**
     * Reactions to a death: the killer gloats or laughs, and an ally nearby mourns a fallen witch or illager.
     */
    public static void onDeath(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide()) {
            return;
        }
        if (source.getEntity() instanceof LivingEntity killer && killer != victim) {
            // any kill ends the fight successfully, so the killer does not complain about a lost target
            state(killer).setLastVictory(killer.level().getGameTime());
            if (victim instanceof Player) {
                onEvent(killer, EventCatalog.VICTORY);
            } else {
                onEvent(killer, EventCatalog.LAUGH);
            }
        }
        if (victim instanceof Villager) {
            victim.level().getEntitiesOfClass(Villager.class, victim.getBoundingBox().inflate(ALLY_RADIUS),
                            villager -> villager != victim && villager.isAlive() && hasVoice(villager))
                    .stream()
                    .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(victim)))
                    .ifPresent(villager -> onEvent(villager, EventCatalog.VILLAGER_DIED));
        }
        if (kindOf(victim).filter(EventCatalog.HOSTILE::contains).isPresent()) {
            victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(ALLY_RADIUS),
                            ally -> ally != victim && ally.isAlive() && kindOf(ally).filter(EventCatalog.HOSTILE::contains).isPresent() && hasVoice(ally))
                    .stream()
                    .min(Comparator.comparingDouble(ally -> ally.distanceToSqr(victim)))
                    .ifPresent(ally -> onEvent(ally, EventCatalog.ALLY_DIED));
        }
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
