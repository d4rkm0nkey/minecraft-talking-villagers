package me.teajay.talkingvillagers.core.catalog;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Entity types that can have a voice.
 */
public enum EntityKind {
    VILLAGER,
    WANDERING_TRADER,
    WITCH,
    PILLAGER,
    VINDICATOR,
    EVOKER,
    ILLUSIONER;

    /** Manifest shorthand for all illagers. */
    public static final String ILLAGER_GROUP = "illager";
    public static final Set<EntityKind> ILLAGERS = EnumSet.of(PILLAGER, VINDICATOR, EVOKER, ILLUSIONER);

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<EntityKind> byId(String id) {
        for (EntityKind kind : values()) {
            if (kind.id().equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves a manifest entity value, expanding the {@value #ILLAGER_GROUP} group.
     *
     * @return the matching kinds, or an empty set if the value is unknown
     */
    public static Set<EntityKind> resolve(String value) {
        if (ILLAGER_GROUP.equals(value)) {
            return EnumSet.copyOf(ILLAGERS);
        }
        return byId(value).map(EnumSet::of).orElseGet(() -> EnumSet.noneOf(EntityKind.class));
    }
}
