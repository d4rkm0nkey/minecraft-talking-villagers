package me.teajay.talkingvillagers.core.language;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Places that have a language, e.g. village bells. An entity takes the language of the nearest anchor in range,
 * so a village with several bells speaks one language.
 */
public final class LanguageAnchors {
    public record Anchor(int x, int y, int z, String language) {
        long distanceSqr(int px, int py, int pz) {
            long dx = x - px;
            long dy = y - py;
            long dz = z - pz;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private final List<Anchor> anchors = new ArrayList<>();

    public LanguageAnchors() {
    }

    public LanguageAnchors(List<Anchor> anchors) {
        this.anchors.addAll(anchors);
    }

    public List<Anchor> anchors() {
        return List.copyOf(anchors);
    }

    /**
     * The nearest anchor within {@code radius} blocks.
     */
    public Optional<Anchor> find(int x, int y, int z, int radius) {
        long radiusSqr = (long) radius * radius;
        Anchor best = null;
        long bestDistance = Long.MAX_VALUE;
        for (Anchor anchor : anchors) {
            long distance = anchor.distanceSqr(x, y, z);
            if (distance <= radiusSqr && distance < bestDistance) {
                best = anchor;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public Anchor add(int x, int y, int z, String language) {
        Anchor anchor = new Anchor(x, y, z, language);
        anchors.add(anchor);
        return anchor;
    }

    /**
     * Changes the language of all anchors within the radius, e.g. by command.
     *
     * @return how many anchors changed; if none existed, a new anchor is created and 1 is returned
     */
    public int setLanguage(int x, int y, int z, int radius, String language) {
        long radiusSqr = (long) radius * radius;
        int changed = 0;
        for (int i = 0; i < anchors.size(); i++) {
            Anchor anchor = anchors.get(i);
            if (anchor.distanceSqr(x, y, z) <= radiusSqr) {
                anchors.set(i, new Anchor(anchor.x(), anchor.y(), anchor.z(), language));
                changed++;
            }
        }
        if (changed == 0) {
            add(x, y, z, language);
            changed = 1;
        }
        return changed;
    }
}
