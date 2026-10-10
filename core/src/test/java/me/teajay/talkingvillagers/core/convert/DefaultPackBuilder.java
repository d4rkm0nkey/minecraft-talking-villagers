package me.teajay.talkingvillagers.core.convert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.io.DirectoryPackContents;
import me.teajay.talkingvillagers.core.io.LoadedPack;
import me.teajay.talkingvillagers.core.io.PackReader;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the default pack from {@code packs-src/teajay-default}: the version 1 recordings of TeaJay as villager voice
 * plus the newer witch recordings. Run with {@code ./gradlew :core:buildDefaultPack}.
 */
public final class DefaultPackBuilder {
    private DefaultPackBuilder() {
    }

    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]);
        Path target = Path.of(args[1]);
        String version = args[2];

        Version1Converter.Result converted = Version1Converter.convert(new DirectoryPackContents(source), "", "v1-en_us.json",
                new Version1Converter.Settings("teajay-default", "TeaJay Default", version, "en", Gender.MALE, List.of("TeaJay")));

        // the witch folder has one file per line and the transcripts in transcripts.json
        Path witchDir = source.resolve("witch");
        JsonObject transcripts = JsonParser.parseString(Files.readString(witchDir.resolve("transcripts.json"))).getAsJsonObject();
        Map<String, byte[]> witchFiles = new LinkedHashMap<>();
        List<VoiceLine> ambient = new ArrayList<>();
        for (String file : transcripts.keySet()) {
            String path = "sounds/teajay-witch/ambient/" + file;
            witchFiles.put(path, Files.readAllBytes(witchDir.resolve(file)));
            ambient.add(new VoiceLine(path, Optional.of(transcripts.get(file).getAsString()), Optional.empty(), LineConditions.NONE));
        }
        Voice witch = new Voice("teajay-witch", "TeaJay (Witch)", Set.of(EntityKind.WITCH), Gender.NEUTRAL, false, Optional.empty(),
                PitchRange.DEFAULT, 1, Map.of("ambient", ambient, "brew", ambient));

        List<Voice> voices = new ArrayList<>(converted.pack().voices());
        voices.add(witch);
        VoicePack pack = new VoicePack(VoicePack.FORMAT, converted.pack().id(), converted.pack().name(), version,
                Optional.of("The original voice of Talking Villagers"), converted.pack().authors(), converted.pack().language(), voices);
        Version1Converter.write(new Version1Converter.Result(pack, converted.files(), converted.skipped()), witchFiles, target);

        LoadedPack check = PackReader.read(target);
        if (!check.isUsable()) {
            throw new IllegalStateException("Default pack is broken: " + check.errors());
        }
        check.warnings().forEach(warning -> System.out.println("warning: " + warning));
        converted.skipped().forEach(file -> System.out.println("skipped: " + file));
        System.out.println("Wrote " + target + " (" + Files.size(target) / 1024 + " KiB)");
    }
}
