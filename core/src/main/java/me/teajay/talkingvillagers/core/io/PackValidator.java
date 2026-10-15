package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.VoiceEvent;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Checks a parsed manifest against the files in the archive and for likely mistakes.
 */
public final class PackValidator {
    /** Files above this size are reported, voice lines are usually far smaller. */
    public static final long LARGE_FILE_BYTES = 1024 * 1024;
    private static final String SOUNDS_DIR = "sounds/";

    private PackValidator() {
    }

    /**
     * @param audio header information of every readable referenced file
     */
    public record Result(List<Problem> problems, Map<String, OggInfo> audio) {
    }

    public static Result validate(VoicePack pack, PackContents contents) {
        List<Problem> problems = new ArrayList<>();
        Map<String, OggInfo> audio = new HashMap<>();
        Set<String> checked = new HashSet<>();
        Set<String> referenced = new HashSet<>();

        for (int v = 0; v < pack.voices().size(); v++) {
            Voice voice = pack.voices().get(v);
            String voicePath = "voices[" + v + "]";
            checkVoice(voice, voicePath, problems);

            for (Map.Entry<String, List<VoiceLine>> entry : voice.lines().entrySet()) {
                for (int i = 0; i < entry.getValue().size(); i++) {
                    String file = entry.getValue().get(i).file();
                    String path = voicePath + ".lines." + entry.getKey() + "[" + i + "].file";
                    referenced.add(file);
                    if (!contents.exists(file)) {
                        problems.add(new Problem(ProblemCode.MISSING_FILE, path, "File '" + file + "' is not in the pack"));
                    } else if (checked.add(file)) {
                        checkAudio(file, path, contents, problems).ifPresent(info -> audio.put(file, info));
                    }
                }
            }
        }

        for (String file : contents.files()) {
            if (file.startsWith(SOUNDS_DIR) && !referenced.contains(file)) {
                problems.add(new Problem(ProblemCode.UNUSED_FILE, file, "File is not used by any line"));
            }
        }
        return new Result(List.copyOf(problems), Map.copyOf(audio));
    }

    private static void checkVoice(Voice voice, String path, List<Problem> problems) {
        if (voice.lines().values().stream().allMatch(List::isEmpty)) {
            problems.add(new Problem(ProblemCode.NO_LINES, path, "Voice '" + voice.id() + "' has no lines"));
            return;
        }
        if (!voice.hasLines(EventCatalog.HURT) || !voice.hasLines(EventCatalog.DEATH)) {
            problems.add(new Problem(ProblemCode.MISSING_BASIC_EVENT, path,
                    "Voice '" + voice.id() + "' should have lines for 'hurt' and 'death', otherwise vanilla sounds are used"));
        }
        if (voice.child() && voice.entities().stream().anyMatch(kind -> kind != EntityKind.VILLAGER)) {
            problems.add(new Problem(ProblemCode.CHILD_VOICE_NOT_VILLAGER, path + ".child",
                    "Only villagers have children, the child voice is never used for other entities"));
        }
        for (String eventId : voice.lines().keySet()) {
            VoiceEvent event = EventCatalog.byId(eventId).orElseThrow();
            if (voice.entities().stream().noneMatch(event::appliesTo)) {
                problems.add(new Problem(ProblemCode.EVENT_NOT_USED, path + ".lines." + eventId,
                        "None of the voice's entities trigger '" + eventId + "'"));
            }
        }
    }

    private static Optional<OggInfo> checkAudio(String file, String path, PackContents contents, List<Problem> problems) {
        long size = contents.size(file);
        if (size > LARGE_FILE_BYTES) {
            problems.add(new Problem(ProblemCode.LARGE_FILE, path, "'" + file + "' is " + size / 1024 + " KiB, keep lines short"));
        }
        Optional<OggInfo> info;
        try (InputStream in = contents.open(file)) {
            info = OggInfo.probe(in);
        } catch (IOException e) {
            problems.add(new Problem(ProblemCode.NOT_OGG, path, "'" + file + "' cannot be read: " + e.getMessage()));
            return Optional.empty();
        }
        if (info.isEmpty()) {
            problems.add(new Problem(ProblemCode.NOT_OGG, path, "'" + file + "' is not an Ogg file"));
            return Optional.empty();
        }
        if (info.get().codec() != OggInfo.Codec.VORBIS) {
            problems.add(new Problem(ProblemCode.NOT_VORBIS, path,
                    "'" + file + "' uses " + info.get().codec().name().toLowerCase(Locale.ROOT) + ", Minecraft can only play Ogg Vorbis"));
            return Optional.empty();
        }
        if (info.get().channels() > 1) {
            problems.add(new Problem(ProblemCode.STEREO_FILE, path,
                    "'" + file + "' is stereo; Minecraft only plays mono sounds positionally"));
        }
        return info;
    }
}
