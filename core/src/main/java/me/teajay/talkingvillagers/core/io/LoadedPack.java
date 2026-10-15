package me.teajay.talkingvillagers.core.io;

import me.teajay.talkingvillagers.core.model.VoicePack;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Result of reading a {@code .voicepack} file.
 *
 * @param pack   the manifest, present only if the pack has no errors
 * @param sha256 hex digest of the whole file, used to compare packs between server and client
 */
public record LoadedPack(Path source, String sha256, Optional<VoicePack> pack, List<Problem> problems, boolean hasIcon) {
    public LoadedPack {
        problems = List.copyOf(problems);
    }

    public boolean isUsable() {
        return pack.isPresent();
    }

    public List<Problem> errors() {
        return problems.stream().filter(Problem::isError).toList();
    }

    public List<Problem> warnings() {
        return problems.stream().filter(problem -> !problem.isError()).toList();
    }
}
