package me.teajay.talkingvillagers.language;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.teajay.talkingvillagers.TalkingVillagers;
import me.teajay.talkingvillagers.core.language.LanguageAnchors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Languages stored per dimension: village anchors (bells), anchors for witch huts and illager outposts, and the
 * language of each running raid.
 */
public final class LanguageData extends SavedData {
    private static final Codec<LanguageAnchors.Anchor> ANCHOR = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(LanguageAnchors.Anchor::x),
            Codec.INT.fieldOf("y").forGetter(LanguageAnchors.Anchor::y),
            Codec.INT.fieldOf("z").forGetter(LanguageAnchors.Anchor::z),
            Codec.STRING.fieldOf("language").forGetter(LanguageAnchors.Anchor::language)
    ).apply(instance, LanguageAnchors.Anchor::new));

    private static final Codec<LanguageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ANCHOR.listOf().optionalFieldOf("villages", List.of()).forGetter(data -> data.villages.anchors()),
            ANCHOR.listOf().optionalFieldOf("outsiders", List.of()).forGetter(data -> data.outsiders.anchors()),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("raids", Map.of()).forGetter(data -> data.raids)
    ).apply(instance, LanguageData::new));

    // both loaders handle saved data without a data fixer
    public static final SavedDataType<LanguageData> TYPE = new SavedDataType<>(TalkingVillagers.id("languages"), LanguageData::new, CODEC, null);

    private final LanguageAnchors villages;
    private final LanguageAnchors outsiders;
    /** Raid id as text, because map keys must be strings. */
    private final Map<String, String> raids;

    public LanguageData() {
        this(List.of(), List.of(), Map.of());
    }

    private LanguageData(List<LanguageAnchors.Anchor> villages, List<LanguageAnchors.Anchor> outsiders, Map<String, String> raids) {
        this.villages = new LanguageAnchors(villages);
        this.outsiders = new LanguageAnchors(outsiders);
        this.raids = new HashMap<>(raids);
    }

    public static LanguageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Callers that change anchors must call {@link #setDirty()}. */
    public LanguageAnchors villages() {
        return villages;
    }

    public LanguageAnchors outsiders() {
        return outsiders;
    }

    public Optional<String> raidLanguage(int raidId) {
        return Optional.ofNullable(raids.get(Integer.toString(raidId)));
    }

    public void setRaidLanguage(int raidId, String language, IntPredicate raidExists) {
        raids.keySet().removeIf(id -> !raidExists.test(Integer.parseInt(id)));
        raids.put(Integer.toString(raidId), language);
        setDirty();
    }
}
