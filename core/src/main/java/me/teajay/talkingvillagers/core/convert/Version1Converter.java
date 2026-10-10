package me.teajay.talkingvillagers.core.convert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.Professions;
import me.teajay.talkingvillagers.core.io.ManifestCodec;
import me.teajay.talkingvillagers.core.io.PackContents;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts voices of version 1 (a resource pack with {@code <voice>/<profession or all>/<event>/<sound>.ogg} folders
 * and transcripts in {@code lang/en_us.json}) into a version 2 voicepack. Profession folders become line conditions,
 * renamed events are mapped, and the folder name is kept as legacy id so villagers in old worlds keep their voice.
 */
public final class Version1Converter {
    /** Location of the voices inside a version 1 resource pack. */
    public static final String RESOURCE_PACK_VOICES = "assets/talkingvillagers/sounds/voices/";
    public static final String RESOURCE_PACK_LANG = "assets/talkingvillagers/lang/en_us.json";
    private static final Map<String, String> EVENTS = Map.of("herodrop", "hero_gift", "level", "level_up", "nope", "no", "tradesuccess", "trade_success");
    private static final Map<String, String> PROFESSIONS = Map.of("cartograph", "cartographer");
    private static final String ALL_PROFESSIONS = "all";

    private Version1Converter() {
    }

    /**
     * @param gender gender of all voices; version 1 did not record it
     */
    public record Settings(String packId, String name, String version, String language, Gender gender, List<String> authors) {
    }

    /**
     * @param files    audio files by their path in the new pack
     * @param skipped  files that could not be mapped, e.g. unknown events
     */
    public record Result(VoicePack pack, Map<String, byte[]> files, List<String> skipped) {
    }

    public static boolean isVersion1(PackContents contents) {
        return contents.files().stream().anyMatch(path -> path.startsWith(RESOURCE_PACK_VOICES) && path.endsWith(".ogg"));
    }

    public static Result convertResourcePack(PackContents contents, Settings settings) throws IOException {
        return convert(contents, RESOURCE_PACK_VOICES, RESOURCE_PACK_LANG, settings);
    }

    /**
     * @param voicesRoot folder that contains one folder per voice, e.g. {@value #RESOURCE_PACK_VOICES}
     * @param langFile   transcripts, may be missing
     */
    public static Result convert(PackContents contents, String voicesRoot, String langFile, Settings settings) throws IOException {
        JsonObject lang = contents.exists(langFile) ? readJson(contents, langFile) : new JsonObject();
        Map<String, Map<String, List<VoiceLine>>> linesByVoice = new TreeMap<>();
        Map<String, byte[]> files = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();

        for (String path : contents.files()) {
            if (!path.startsWith(voicesRoot) || !path.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                continue;
            }
            String[] parts = path.substring(voicesRoot.length()).split("/");
            if (parts.length != 4) {
                skipped.add(path);
                continue;
            }
            String voiceFolder = parts[0];
            String profession = PROFESSIONS.getOrDefault(parts[1], parts[1]);
            String event = EVENTS.getOrDefault(parts[2], parts[2]);
            if (EventCatalog.byId(event).isEmpty()) {
                skipped.add(path);
                continue;
            }
            String voiceId = sanitize(voiceFolder);
            String sound = parts[3].substring(0, parts[3].length() - 4);
            String prefix = profession.equals(ALL_PROFESSIONS) ? "" : sanitize(profession) + "-";
            String target = "sounds/" + voiceId + "/" + event + "/" + prefix + sanitize(sound) + ".ogg";
            try (InputStream in = contents.open(path)) {
                files.put(target, in.readAllBytes());
            }
            // version 1 numbered variants of the same line (hello1, hello2) and shared one subtitle
            String subtitleKey = "subtitles.talkingvillagers." + voiceFolder + "_" + sound.replaceAll("\\d+$", "");
            Optional<String> text = lang.has(subtitleKey) && !lang.get(subtitleKey).getAsString().isBlank()
                    ? Optional.of(lang.get(subtitleKey).getAsString()) : Optional.empty();
            LineConditions conditions = profession.equals(ALL_PROFESSIONS)
                    ? LineConditions.NONE
                    : new LineConditions(Set.of(Professions.normalize(profession)), Optional.empty());
            linesByVoice.computeIfAbsent(voiceFolder, v -> new LinkedHashMap<>())
                    .computeIfAbsent(event, e -> new ArrayList<>())
                    .add(new VoiceLine(target, text, Optional.empty(), conditions));
        }

        List<Voice> voices = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, List<VoiceLine>>> voice : linesByVoice.entrySet()) {
            String id = sanitize(voice.getKey());
            ids.add(id);
            voices.add(new Voice(id, capitalize(voice.getKey()), Set.of(EntityKind.VILLAGER), settings.gender(), false, Optional.empty(),
                    PitchRange.DEFAULT, 1, voice.getValue(), List.of(voice.getKey())));
        }
        VoicePack pack = new VoicePack(VoicePack.FORMAT, settings.packId(), settings.name(), settings.version(),
                Optional.of("Converted from version 1"), settings.authors(), settings.language(), voices);
        return new Result(pack, files, skipped);
    }

    /**
     * Writes the converted pack as a {@code .voicepack} file.
     */
    public static void write(Result result, Map<String, byte[]> extraFiles, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (OutputStream out = Files.newOutputStream(target); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(VoicePack.MANIFEST));
            zip.write(ManifestCodec.write(result.pack()).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            Map<String, byte[]> all = new LinkedHashMap<>(result.files());
            all.putAll(extraFiles);
            for (Map.Entry<String, byte[]> file : all.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
    }

    private static JsonObject readJson(PackContents contents, String path) throws IOException {
        try (InputStream in = contents.open(path)) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Lower case and only characters that Minecraft resource paths and voice ids allow. */
    static String sanitize(String name) {
        String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        return clean.isEmpty() ? "voice" : clean;
    }

    private static String capitalize(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
