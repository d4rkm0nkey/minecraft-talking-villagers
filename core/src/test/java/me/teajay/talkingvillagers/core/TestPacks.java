package me.teajay.talkingvillagers.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds {@code .voicepack} archives for tests.
 */
public final class TestPacks {
    private final Map<String, byte[]> files = new LinkedHashMap<>();

    public static TestPacks create() {
        return new TestPacks();
    }

    public static byte[] audio(String name) {
        try (InputStream in = TestPacks.class.getResourceAsStream("/audio/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("No test audio " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String resource(String path) {
        try (InputStream in = TestPacks.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No test resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public TestPacks manifest(String json) {
        return file("voicepack.json", json.getBytes(StandardCharsets.UTF_8));
    }

    public TestPacks file(String path, byte[] content) {
        files.put(path, content);
        return this;
    }

    /** Adds the mono test sound under each path. */
    public TestPacks sounds(String... paths) {
        for (String path : paths) {
            file(path, audio("mono.ogg"));
        }
        return this;
    }

    public Path write(Path dir, String name) throws IOException {
        Path file = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return file;
    }

    /**
     * A minimal valid manifest with one villager voice. {@code lines} is the JSON content of the {@code lines} object.
     */
    public static String manifest(String voiceExtras, String lines) {
        return """
                {
                  "format": 2,
                  "id": "test",
                  "name": "Test",
                  "version": "1.0.0",
                  "language": "de",
                  "voices": [
                    {
                      "id": "anna",
                      "name": "Anna",
                      "entities": ["villager"],
                      "gender": "female",
                      %s
                      "lines": { %s }
                    }
                  ]
                }
                """.formatted(voiceExtras, lines);
    }

    public static final String BASIC_LINES = """
            "hurt": [{ "file": "sounds/anna/hurt/au.ogg" }],
            "death": [{ "file": "sounds/anna/death/oh.ogg" }]
            """;
    public static final String[] BASIC_FILES = {"sounds/anna/hurt/au.ogg", "sounds/anna/death/oh.ogg"};
}
