package me.teajay.talkingvillagers.core.io;

import java.util.Locale;

/**
 * Stable identifiers for problems, usable as translation keys in the GUI and the web editor.
 */
public enum ProblemCode {
    UNREADABLE_ARCHIVE(Severity.ERROR),
    MISSING_MANIFEST(Severity.ERROR),
    MANIFEST_TOO_LARGE(Severity.ERROR),
    INVALID_JSON(Severity.ERROR),
    UNSUPPORTED_FORMAT(Severity.ERROR),
    MISSING_FIELD(Severity.ERROR),
    INVALID_VALUE(Severity.ERROR),
    DUPLICATE_VOICE_ID(Severity.ERROR),
    UNKNOWN_ENTITY(Severity.ERROR),
    INVALID_FILE_PATH(Severity.ERROR),
    MISSING_FILE(Severity.ERROR),
    NOT_OGG(Severity.ERROR),
    NOT_VORBIS(Severity.ERROR),

    UNKNOWN_PROPERTY(Severity.WARNING),
    UNKNOWN_EVENT(Severity.WARNING),
    UNKNOWN_PROFESSION(Severity.WARNING),
    EVENT_NOT_USED(Severity.WARNING),
    NO_LINES(Severity.WARNING),
    MISSING_BASIC_EVENT(Severity.WARNING),
    CHILD_VOICE_NOT_VILLAGER(Severity.WARNING),
    STEREO_FILE(Severity.WARNING),
    LARGE_FILE(Severity.WARNING),
    UNUSED_FILE(Severity.WARNING);

    private final Severity severity;

    ProblemCode(Severity severity) {
        this.severity = severity;
    }

    public Severity severity() {
        return severity;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
