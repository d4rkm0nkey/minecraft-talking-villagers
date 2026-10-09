package me.teajay.talkingvillagers.core.io;

/**
 * Something wrong with a voicepack.
 *
 * @param path location inside the pack, e.g. {@code voices[0].lines.greeting[2].file}, or empty for the whole pack
 */
public record Problem(ProblemCode code, String path, String message) {
    public Severity severity() {
        return code.severity();
    }

    public boolean isError() {
        return severity() == Severity.ERROR;
    }

    @Override
    public String toString() {
        return severity() + " " + code.id() + (path.isEmpty() ? "" : " at " + path) + ": " + message;
    }
}
