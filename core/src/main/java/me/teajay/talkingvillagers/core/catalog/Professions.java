package me.teajay.talkingvillagers.core.catalog;

import java.util.Set;

/**
 * Villager profession ids. Lines may also name professions added by other mods using a namespaced id.
 */
public final class Professions {
    public static final Set<String> VANILLA = Set.of(
            "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith", "none"
    );
    private static final String MINECRAFT_NAMESPACE = "minecraft:";

    private Professions() {
    }

    /**
     * Strips the {@code minecraft:} namespace, so {@code minecraft:farmer} and {@code farmer} are the same.
     */
    public static String normalize(String id) {
        return id.startsWith(MINECRAFT_NAMESPACE) ? id.substring(MINECRAFT_NAMESPACE.length()) : id;
    }

    public static boolean isKnown(String id) {
        String normalized = normalize(id);
        return VANILLA.contains(normalized) || normalized.contains(":");
    }
}
