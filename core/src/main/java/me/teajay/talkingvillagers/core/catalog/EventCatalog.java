package me.teajay.talkingvillagers.core.catalog;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static me.teajay.talkingvillagers.core.catalog.EntityKind.*;

/**
 * All events known to the mod. The JSON schema lists the same ids, which a test keeps in sync.
 */
public final class EventCatalog {
    private static final Map<String, VoiceEvent> EVENTS = new LinkedHashMap<>();

    private static final Set<EntityKind> ALL = EnumSet.allOf(EntityKind.class);
    private static final Set<EntityKind> MERCHANTS = EnumSet.of(VILLAGER, WANDERING_TRADER);
    private static final Set<EntityKind> SPELLCASTERS = EnumSet.of(EVOKER, ILLUSIONER);
    /** Witches and illagers: hostile entities that fight players. */
    public static final Set<EntityKind> HOSTILE = EnumSet.of(WITCH, PILLAGER, VINDICATOR, EVOKER, ILLUSIONER);

    public static final VoiceEvent AMBIENT = register("ambient", ALL);
    public static final VoiceEvent HURT = register("hurt", ALL);
    public static final VoiceEvent DEATH = register("death", ALL);
    public static final VoiceEvent CELEBRATE = register("celebrate", EnumSet.of(VILLAGER, WITCH, PILLAGER, VINDICATOR, EVOKER, ILLUSIONER));

    public static final VoiceEvent GREETING = register("greeting", MERCHANTS);
    public static final VoiceEvent RANDOM = register("random", EnumSet.of(VILLAGER));
    public static final VoiceEvent GOSSIP = register("gossip", EnumSet.of(VILLAGER));
    public static final VoiceEvent YES = register("yes", MERCHANTS);
    public static final VoiceEvent NO = register("no", MERCHANTS);
    public static final VoiceEvent TRADE = register("trade", MERCHANTS);
    public static final VoiceEvent TRADE_SUCCESS = register("trade_success", MERCHANTS);
    public static final VoiceEvent LEVEL_UP = register("level_up", EnumSet.of(VILLAGER));
    public static final VoiceEvent HERO = register("hero", EnumSet.of(VILLAGER));
    public static final VoiceEvent HERO_GIFT = register("hero_gift", EnumSet.of(VILLAGER));
    public static final VoiceEvent GOOD_WEATHER = register("good_weather", EnumSet.of(VILLAGER));
    public static final VoiceEvent BAD_WEATHER = register("bad_weather", EnumSet.of(VILLAGER));
    public static final VoiceEvent SLEEP = register("sleep", EnumSet.of(VILLAGER));
    public static final VoiceEvent WAKE_UP = register("wake_up", EnumSet.of(VILLAGER));
    public static final VoiceEvent PANIC = register("panic", EnumSet.of(VILLAGER));
    public static final VoiceEvent GROW_UP = register("grow_up", EnumSet.of(VILLAGER));

    public static final VoiceEvent DRINK_POTION = register("drink_potion", EnumSet.of(WITCH, WANDERING_TRADER));
    public static final VoiceEvent THROW_POTION = register("throw_potion", EnumSet.of(WITCH));
    public static final VoiceEvent LAUGH = register("laugh", EnumSet.of(WITCH));
    public static final VoiceEvent DISAPPEAR = register("disappear", EnumSet.of(WANDERING_TRADER));

    public static final VoiceEvent SPOT_PLAYER = register("spot_player", ILLAGERS);
    public static final VoiceEvent ATTACK = register("attack", ILLAGERS);
    public static final VoiceEvent CHARGE = register("charge", EnumSet.of(PILLAGER));
    public static final VoiceEvent RAID_HORN = register("raid_horn", ILLAGERS);
    public static final VoiceEvent CAST_SPELL = register("cast_spell", SPELLCASTERS);
    public static final VoiceEvent SUMMON_VEX = register("summon_vex", EnumSet.of(EVOKER), CAST_SPELL);
    public static final VoiceEvent FANGS = register("fangs", EnumSet.of(EVOKER), CAST_SPELL);
    public static final VoiceEvent MIRROR = register("mirror", EnumSet.of(ILLUSIONER), CAST_SPELL);

    // hostile states and transitions; the state events fall back to ambient, so older packs keep working
    public static final VoiceEvent IDLE = register("idle", HOSTILE, AMBIENT);
    public static final VoiceEvent COMBAT = register("combat", HOSTILE, AMBIENT);
    public static final VoiceEvent RAID_IDLE = register("raid_idle", HOSTILE, IDLE);
    public static final VoiceEvent LOST_TARGET = register("lost_target", HOSTILE);
    public static final VoiceEvent LOW_HEALTH = register("low_health", HOSTILE);
    public static final VoiceEvent VICTORY = register("victory", HOSTILE);
    public static final VoiceEvent ALLY_DIED = register("ally_died", HOSTILE);
    public static final VoiceEvent PATROL = register("patrol", ILLAGERS, IDLE);
    public static final VoiceEvent PATROL_LEADER = register("patrol_leader", ILLAGERS, PATROL);
    public static final VoiceEvent CHARGE_AT = register("charge_at", EnumSet.of(VINDICATOR), SPOT_PLAYER);
    public static final VoiceEvent JOHNNY = register("johnny", EnumSet.of(VINDICATOR), COMBAT);
    public static final VoiceEvent RETREAT = register("retreat", EnumSet.of(EVOKER), COMBAT);
    /** No fallback: without own lines the evoker keeps its famous vanilla "wololo". */
    public static final VoiceEvent WOLOLO = register("wololo", EnumSet.of(EVOKER));
    public static final VoiceEvent BLINDNESS = register("blindness", EnumSet.of(ILLUSIONER), CAST_SPELL);
    public static final VoiceEvent BREW = register("brew", EnumSet.of(WITCH), IDLE);
    public static final VoiceEvent HEAL_SELF = register("heal_self", EnumSet.of(WITCH), DRINK_POTION);
    public static final VoiceEvent DRINK_OTHER = register("drink_other", EnumSet.of(WITCH), DRINK_POTION);
    public static final VoiceEvent HEAL_ALLY = register("heal_ally", EnumSet.of(WITCH), THROW_POTION);

    // villager states and reactions; most fall back to an existing event
    public static final VoiceEvent PANIC_LOOP = register("panic_loop", EnumSet.of(VILLAGER), PANIC);
    public static final VoiceEvent PRE_RAID = register("pre_raid", EnumSet.of(VILLAGER), PANIC);
    public static final VoiceEvent HIDE = register("hide", EnumSet.of(VILLAGER));
    public static final VoiceEvent WORK = register("work", EnumSet.of(VILLAGER), RANDOM);
    public static final VoiceEvent PLAY = register("play", EnumSet.of(VILLAGER), RANDOM);
    public static final VoiceEvent MEET = register("meet", EnumSet.of(VILLAGER), GOSSIP);
    public static final VoiceEvent ATTACKED_BY_PLAYER = register("attacked_by_player", EnumSet.of(VILLAGER), HURT);
    public static final VoiceEvent SPOT_ZOMBIE = register("spot_zombie", EnumSet.of(VILLAGER), PANIC);
    public static final VoiceEvent SPOT_RAIDER = register("spot_raider", EnumSet.of(VILLAGER), PANIC);
    public static final VoiceEvent VILLAGER_DIED = register("villager_died", EnumSet.of(VILLAGER));
    public static final VoiceEvent CURED = register("cured", EnumSet.of(VILLAGER), CELEBRATE);
    public static final VoiceEvent LOVE = register("love", EnumSet.of(VILLAGER));
    public static final VoiceEvent GREETING_FRIENDLY = register("greeting_friendly", EnumSet.of(VILLAGER), GREETING);
    public static final VoiceEvent GREETING_UNFRIENDLY = register("greeting_unfriendly", EnumSet.of(VILLAGER), GREETING);
    public static final VoiceEvent TRADE_UNFRIENDLY = register("trade_unfriendly", EnumSet.of(VILLAGER), TRADE);
    /** A player fights a monster close by. */
    public static final VoiceEvent CHEER = register("cheer", EnumSet.of(VILLAGER));
    /** A player killed a monster close by. */
    public static final VoiceEvent MONSTER_KILLED = register("monster_killed", EnumSet.of(VILLAGER), CHEER);

    private EventCatalog() {
    }

    private static VoiceEvent register(String id, Set<EntityKind> kinds) {
        return register(id, kinds, null);
    }

    private static VoiceEvent register(String id, Set<EntityKind> kinds, VoiceEvent parent) {
        VoiceEvent event = new VoiceEvent(id, kinds, Optional.ofNullable(parent));
        EVENTS.put(id, event);
        return event;
    }

    public static Optional<VoiceEvent> byId(String id) {
        return Optional.ofNullable(EVENTS.get(id));
    }

    public static Collection<VoiceEvent> all() {
        return Collections.unmodifiableCollection(EVENTS.values());
    }

    public static List<VoiceEvent> forKind(EntityKind kind) {
        return EVENTS.values().stream().filter(event -> event.appliesTo(kind)).toList();
    }
}
