package me.teajay.talkingvillagers.core.util;

import java.util.Comparator;

/**
 * Compares pack versions like {@code 1.2.0}, {@code 1.10} or {@code 2.0.0-beta.1}.
 * Numeric parts compare as numbers, other parts as text, and a pre-release ({@code -beta}) is older than the release.
 */
public final class Versions {
    public static final Comparator<String> COMPARATOR = Versions::compare;

    private Versions() {
    }

    public static int compare(String a, String b) {
        String[] mainA = splitPreRelease(a);
        String[] mainB = splitPreRelease(b);
        int result = compareParts(mainA[0], mainB[0]);
        if (result != 0) {
            return result;
        }
        if (mainA[1] == null || mainB[1] == null) {
            // the release is newer than any of its pre-releases
            return mainA[1] == null ? (mainB[1] == null ? 0 : 1) : -1;
        }
        return compareParts(mainA[1], mainB[1]);
    }

    private static String[] splitPreRelease(String version) {
        String withoutBuild = version.split("\\+", 2)[0];
        int dash = withoutBuild.indexOf('-');
        return dash < 0 ? new String[]{withoutBuild, null} : new String[]{withoutBuild.substring(0, dash), withoutBuild.substring(dash + 1)};
    }

    private static int compareParts(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        for (int i = 0; i < Math.max(partsA.length, partsB.length); i++) {
            String partA = i < partsA.length ? partsA[i] : "0";
            String partB = i < partsB.length ? partsB[i] : "0";
            int result = isNumber(partA) && isNumber(partB)
                    ? Long.compare(Long.parseLong(partA), Long.parseLong(partB))
                    : partA.compareTo(partB);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static boolean isNumber(String part) {
        return !part.isEmpty() && part.length() < 19 && part.chars().allMatch(Character::isDigit);
    }
}
