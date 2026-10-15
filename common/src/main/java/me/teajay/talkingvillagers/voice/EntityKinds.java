package me.teajay.talkingvillagers.voice;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

import java.util.Map;
import java.util.Optional;

/**
 * Which entity types can have a voice.
 */
public final class EntityKinds {
    private static final Map<EntityType<?>, EntityKind> KINDS = Map.of(
            EntityTypes.VILLAGER, EntityKind.VILLAGER,
            EntityTypes.WANDERING_TRADER, EntityKind.WANDERING_TRADER,
            EntityTypes.WITCH, EntityKind.WITCH,
            EntityTypes.PILLAGER, EntityKind.PILLAGER,
            EntityTypes.VINDICATOR, EntityKind.VINDICATOR,
            EntityTypes.EVOKER, EntityKind.EVOKER,
            EntityTypes.ILLUSIONER, EntityKind.ILLUSIONER);

    private EntityKinds() {
    }

    public static Optional<EntityKind> of(Entity entity) {
        return Optional.ofNullable(KINDS.get(entity.getType()));
    }
}
