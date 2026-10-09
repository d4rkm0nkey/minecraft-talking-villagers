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
 * Which vanilla sounds a voice replaces. Every sound an entity makes through {@code makeSound} is looked up here.
 */
public final class VanillaSounds {
    private static final Map<EntityKind, Map<SoundEvent, VoiceEvent>> SOUNDS = new EnumMap<>(EntityKind.class);

    static {
        Map<SoundEvent, VoiceEvent> villager = new HashMap<>();
        villager.put(SoundEvents.VILLAGER_AMBIENT, EventCatalog.AMBIENT);
        villager.put(SoundEvents.VILLAGER_TRADE, EventCatalog.AMBIENT);
        villager.put(SoundEvents.VILLAGER_HURT, EventCatalog.HURT);
        villager.put(SoundEvents.VILLAGER_DEATH, EventCatalog.DEATH);
        villager.put(SoundEvents.VILLAGER_YES, EventCatalog.YES);
        villager.put(SoundEvents.VILLAGER_NO, EventCatalog.NO);
        villager.put(SoundEvents.VILLAGER_CELEBRATE, EventCatalog.CELEBRATE);
        SOUNDS.put(EntityKind.VILLAGER, villager);
    }

    private VanillaSounds() {
    }

    public static Optional<VoiceEvent> event(EntityKind kind, SoundEvent sound) {
        return Optional.ofNullable(SOUNDS.getOrDefault(kind, Map.of()).get(sound));
    }
}
