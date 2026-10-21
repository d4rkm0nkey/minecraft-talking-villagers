package me.teajay.talkingvillagers.client.gui;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoicePack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Texts shared by the screens.
 */
final class GuiTexts {
    private GuiTexts() {
    }

    static Component key(String key, Object... args) {
        return Component.translatable("talkingvillagers.gui." + key, args);
    }

    /** The entities a voice is for, e.g. "Villager" or "Illager". */
    static Component entities(Voice voice) {
        if (voice.entities().containsAll(EntityKind.ILLAGERS) && voice.entities().size() == EntityKind.ILLAGERS.size()) {
            return key("illagers");
        }
        MutableComponent text = Component.empty();
        List<EntityKind> kinds = voice.entities().stream().sorted().toList();
        for (int i = 0; i < kinds.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(Component.translatable("entity.minecraft." + kinds.get(i).id()));
        }
        return text;
    }

    static Component gender(Gender gender) {
        return key("gender." + gender.id());
    }

    /** E.g. "Villager ♂2 ♀2, children 2 · Witch ♀1". */
    static Component summary(VoicePack pack) {
        Map<String, int[]> groups = new LinkedHashMap<>();
        Map<String, Component> names = new LinkedHashMap<>();
        for (Voice voice : pack.voices()) {
            Component name = entities(voice);
            String key = name.getString();
            names.putIfAbsent(key, name);
            int[] counts = groups.computeIfAbsent(key, k -> new int[4]);
            if (voice.child()) {
                counts[3]++;
            } else {
                counts[voice.gender().ordinal()]++;
            }
        }
        MutableComponent text = Component.empty();
        boolean first = true;
        for (Map.Entry<String, int[]> group : groups.entrySet()) {
            if (!first) {
                text.append(" · ");
            }
            first = false;
            int[] counts = group.getValue();
            text.append(names.get(group.getKey())).append(": ");
            List<Component> parts = new ArrayList<>();
            for (Gender gender : Gender.values()) {
                if (counts[gender.ordinal()] > 0) {
                    parts.add(key("count." + gender.id(), counts[gender.ordinal()]));
                }
            }
            if (counts[3] > 0) {
                parts.add(key("children", counts[3]));
            }
            for (int i = 0; i < parts.size(); i++) {
                text.append(i == 0 ? Component.empty() : Component.literal(", ")).append(parts.get(i));
            }
        }
        return text;
    }

    /** Server settings can only be changed in singleplayer or in the main menu. */
    static boolean canEditServerSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null || minecraft.hasSingleplayerServer();
    }

    /** A file size with a translated unit, e.g. "1.5 MB". */
    static Component size(long bytes) {
        return bytes >= 1024 * 1024
                ? Component.translatable("talkingvillagers.unit.mb", String.format(Locale.ROOT, "%.1f", bytes / 1024.0 / 1024.0))
                : Component.translatable("talkingvillagers.unit.kb", bytes / 1024);
    }
}
