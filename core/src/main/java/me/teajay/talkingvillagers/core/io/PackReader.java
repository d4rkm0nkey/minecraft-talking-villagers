package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads, validates and completes a {@code .voicepack} file.
 */
public final class PackReader {
    public static final long MAX_MANIFEST_BYTES = 1024 * 1024;

    private PackReader() {
    }

    public static LoadedPack read(Path file) {
        String sha256;
        try {
            sha256 = sha256(file);
        } catch (IOException e) {
            return failed(file, "", ProblemCode.UNREADABLE_ARCHIVE, "Cannot read file: " + e.getMessage());
        }
        try (ZipPackContents contents = new ZipPackContents(file)) {
            return read(file, sha256, contents);
        } catch (IOException e) {
            return failed(file, sha256, ProblemCode.UNREADABLE_ARCHIVE, "Not a valid ZIP archive: " + e.getMessage());
        }
    }

    static LoadedPack read(Path file, String sha256, PackContents contents) throws IOException {
        boolean hasIcon = contents.exists(VoicePack.ICON);
        if (!contents.exists(VoicePack.MANIFEST)) {
            return failed(file, sha256, ProblemCode.MISSING_MANIFEST, VoicePack.MANIFEST + " is missing at the root of the archive");
        }
        if (contents.size(VoicePack.MANIFEST) > MAX_MANIFEST_BYTES) {
            return failed(file, sha256, ProblemCode.MANIFEST_TOO_LARGE, VoicePack.MANIFEST + " is larger than 1 MiB");
        }
        String json;
        try (InputStream in = contents.open(VoicePack.MANIFEST)) {
            json = new String(in.readNBytes((int) MAX_MANIFEST_BYTES), StandardCharsets.UTF_8);
        }

        ManifestCodec.Result parsed = ManifestCodec.parse(json);
        List<Problem> problems = new ArrayList<>(parsed.problems());
        if (parsed.pack().isEmpty()) {
            return new LoadedPack(file, sha256, Optional.empty(), problems, hasIcon);
        }
        PackValidator.Result validation = PackValidator.validate(parsed.pack().get(), contents);
        problems.addAll(validation.problems());
        if (problems.stream().anyMatch(Problem::isError)) {
            return new LoadedPack(file, sha256, Optional.empty(), problems, hasIcon);
        }
        VoicePack pack = withDurations(parsed.pack().get(), validation.audio());
        return new LoadedPack(file, sha256, Optional.of(pack), problems, hasIcon);
    }

    /**
     * Fills in missing line durations from the audio headers, so the server knows how long every line takes.
     */
    static VoicePack withDurations(VoicePack pack, Map<String, OggInfo> audio) {
        List<Voice> voices = pack.voices().stream().map(voice -> {
            Map<String, List<VoiceLine>> lines = new LinkedHashMap<>();
            voice.lines().forEach((event, eventLines) -> lines.put(event, eventLines.stream().map(line -> {
                if (line.duration().isPresent()) {
                    return line;
                }
                Optional<Float> duration = Optional.ofNullable(audio.get(line.file())).flatMap(OggInfo::duration);
                return new VoiceLine(line.file(), line.text(), duration, line.conditions());
            }).toList()));
            return new Voice(voice.id(), voice.name(), voice.entities(), voice.gender(), voice.child(), voice.language(),
                    voice.pitch(), voice.weight(), lines, voice.legacyIds());
        }).toList();
        return new VoicePack(pack.format(), pack.id(), pack.name(), pack.version(), pack.description(), pack.authors(),
                pack.language(), voices);
    }

    private static LoadedPack failed(Path file, String sha256, ProblemCode code, String message) {
        return new LoadedPack(file, sha256, Optional.empty(), List.of(new Problem(code, "", message)), false);
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
