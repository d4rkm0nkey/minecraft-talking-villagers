package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Which vanilla sounds a voice replaces. Every sound an entity plays is looked up here; sounds that are not listed,
 * like work sounds or the evoker's "wololo", always stay vanilla.
 */
public final class VanillaSounds {
    private static final Map<EntityKind, Map<SoundEvent, VoiceEvent>> SOUNDS = new EnumMap<>(EntityKind.class);

    static {
        map(EntityKind.VILLAGER, Map.of(
                SoundEvents.VILLAGER_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.VILLAGER_TRADE, EventCatalog.AMBIENT,
                SoundEvents.VILLAGER_HURT, EventCatalog.HURT,
                SoundEvents.VILLAGER_DEATH, EventCatalog.DEATH,
                SoundEvents.VILLAGER_YES, EventCatalog.YES,
                SoundEvents.VILLAGER_NO, EventCatalog.NO,
                SoundEvents.VILLAGER_CELEBRATE, EventCatalog.CELEBRATE));
        map(EntityKind.WANDERING_TRADER, Map.of(
                SoundEvents.WANDERING_TRADER_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.WANDERING_TRADER_TRADE, EventCatalog.AMBIENT,
                SoundEvents.WANDERING_TRADER_HURT, EventCatalog.HURT,
                SoundEvents.WANDERING_TRADER_DEATH, EventCatalog.DEATH,
                SoundEvents.WANDERING_TRADER_YES, EventCatalog.YES,
                SoundEvents.WANDERING_TRADER_NO, EventCatalog.NO,
                SoundEvents.WANDERING_TRADER_DRINK_POTION, EventCatalog.DRINK_POTION,
                SoundEvents.WANDERING_TRADER_DRINK_MILK, EventCatalog.DRINK_POTION,
                SoundEvents.WANDERING_TRADER_DISAPPEARED, EventCatalog.DISAPPEAR));
        map(EntityKind.WITCH, Map.of(
                SoundEvents.WITCH_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.WITCH_HURT, EventCatalog.HURT,
                SoundEvents.WITCH_DEATH, EventCatalog.DEATH,
                SoundEvents.WITCH_CELEBRATE, EventCatalog.CELEBRATE,
                SoundEvents.WITCH_DRINK, EventCatalog.DRINK_POTION,
                SoundEvents.WITCH_THROW, EventCatalog.THROW_POTION));
        map(EntityKind.PILLAGER, Map.of(
                SoundEvents.PILLAGER_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.PILLAGER_HURT, EventCatalog.HURT,
                SoundEvents.PILLAGER_DEATH, EventCatalog.DEATH,
                SoundEvents.PILLAGER_CELEBRATE, EventCatalog.CELEBRATE));
        map(EntityKind.VINDICATOR, Map.of(
                SoundEvents.VINDICATOR_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.VINDICATOR_HURT, EventCatalog.HURT,
                SoundEvents.VINDICATOR_DEATH, EventCatalog.DEATH,
                SoundEvents.VINDICATOR_CELEBRATE, EventCatalog.CELEBRATE));
        map(EntityKind.EVOKER, Map.of(
                SoundEvents.EVOKER_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.EVOKER_HURT, EventCatalog.HURT,
                SoundEvents.EVOKER_DEATH, EventCatalog.DEATH,
                SoundEvents.EVOKER_CELEBRATE, EventCatalog.CELEBRATE,
                SoundEvents.EVOKER_CAST_SPELL, EventCatalog.CAST_SPELL,
                SoundEvents.EVOKER_PREPARE_ATTACK, EventCatalog.FANGS,
                SoundEvents.EVOKER_PREPARE_SUMMON, EventCatalog.SUMMON_VEX));
        // the illusioner uses its ambient sound to celebrate
        map(EntityKind.ILLUSIONER, Map.of(
                SoundEvents.ILLUSIONER_AMBIENT, EventCatalog.AMBIENT,
                SoundEvents.ILLUSIONER_HURT, EventCatalog.HURT,
                SoundEvents.ILLUSIONER_DEATH, EventCatalog.DEATH,
                SoundEvents.ILLUSIONER_CAST_SPELL, EventCatalog.CAST_SPELL,
                SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, EventCatalog.CAST_SPELL,
                SoundEvents.ILLUSIONER_PREPARE_MIRROR, EventCatalog.MIRROR));
    }

    private VanillaSounds() {
    }

    private static void map(EntityKind kind, Map<SoundEvent, VoiceEvent> sounds) {
        SOUNDS.put(kind, new HashMap<>(sounds));
    }

    public static Optional<VoiceEvent> event(EntityKind kind, SoundEvent sound) {
        return Optional.ofNullable(SOUNDS.getOrDefault(kind, Map.of()).get(sound));
    }
}
