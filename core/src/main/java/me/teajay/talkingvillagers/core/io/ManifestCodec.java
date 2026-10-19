package me.teajay.talkingvillagers.core.io;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.teajay.talkingvillagers.core.catalog.EntityKind;
import me.teajay.talkingvillagers.core.catalog.EventCatalog;
import me.teajay.talkingvillagers.core.catalog.Professions;
import me.teajay.talkingvillagers.core.model.Gender;
import me.teajay.talkingvillagers.core.model.Languages;
import me.teajay.talkingvillagers.core.model.LineConditions;
import me.teajay.talkingvillagers.core.model.PitchRange;
import me.teajay.talkingvillagers.core.model.Voice;
import me.teajay.talkingvillagers.core.model.VoiceLine;
import me.teajay.talkingvillagers.core.model.VoicePack;
import me.teajay.talkingvillagers.core.model.Weather;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Reads and writes {@code voicepack.json}. Mirrors {@code schema/voicepack.schema.json}, but reports every problem
 * with a readable path instead of stopping at the first one.
 */
public final class ManifestCodec {
    private static final Pattern ID = Pattern.compile("^[a-z0-9_-]{1,64}$");
    /** Minecraft resource paths only allow lower case; segments must not start with a dot to rule out {@code ..}. */
    /** Also used in file names of installed packs, so no path separators. */
    private static final Pattern VERSION = Pattern.compile("^[0-9A-Za-z][0-9A-Za-z._+-]{0,31}$");
    // length limits of the schema; the server sends name and version to clients, which must fit into the packet
    private static final int MAX_NAME = 64;
    private static final int MAX_DESCRIPTION = 1024;
    private static final int MAX_TEXT = 256;
    private static final Pattern FILE = Pattern.compile("^sounds/([a-z0-9_-][a-z0-9_.-]*/)*[a-z0-9_-][a-z0-9_.-]*\\.ogg$");
    private static final Set<String> PACK_KEYS = Set.of("$schema", "format", "id", "name", "version", "description", "authors", "language", "voices");
    private static final Set<String> VOICE_KEYS = Set.of("id", "name", "entities", "gender", "child", "language", "pitch", "weight", "lines", "legacyIds");
    private static final Set<String> LINE_KEYS = Set.of("file", "text", "duration", "conditions");
    private static final Set<String> CONDITION_KEYS = Set.of("professions", "weather");

    private ManifestCodec() {
    }

    public record Result(Optional<VoicePack> pack, List<Problem> problems) {
    }

    public static Result parse(String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            return new Result(Optional.empty(), List.of(new Problem(ProblemCode.INVALID_JSON, "", e.getMessage())));
        }
        Parser parser = new Parser();
        Optional<VoicePack> pack = parser.pack(root);
        boolean failed = parser.problems.stream().anyMatch(Problem::isError);
        return new Result(failed ? Optional.empty() : pack, List.copyOf(parser.problems));
    }

    private static final class Parser {
        private final List<Problem> problems = new ArrayList<>();

        private void report(ProblemCode code, String path, String message) {
            problems.add(new Problem(code, path, message));
        }

        Optional<VoicePack> pack(JsonElement root) {
            if (!root.isJsonObject()) {
                report(ProblemCode.INVALID_JSON, "", "The manifest must be a JSON object");
                return Optional.empty();
            }
            JsonObject json = root.getAsJsonObject();
            unknownKeys(json, PACK_KEYS, "");

            Optional<Integer> format = integer(json, "format", "", true);
            if (format.isPresent() && format.get() != VoicePack.FORMAT) {
                String hint = format.get() > VoicePack.FORMAT ? " Update Talking Villagers to use this pack." : "";
                report(ProblemCode.UNSUPPORTED_FORMAT, "format", "Format " + format.get() + " is not supported, expected " + VoicePack.FORMAT + "." + hint);
                return Optional.empty();
            }
            Optional<String> id = id(json, "id", "");
            Optional<String> name = nonEmptyString(json, "name", "", MAX_NAME);
            Optional<String> version = version(json, "version", "");
            Optional<String> description = string(json, "description", "", false, MAX_DESCRIPTION);
            List<String> authors = nonEmptyStrings(json, "authors", "");
            Optional<String> language = language(json, "language", "", true);
            List<Voice> voices = voices(json);

            if (format.isEmpty() || id.isEmpty() || name.isEmpty() || version.isEmpty() || language.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new VoicePack(format.get(), id.get(), name.get(), version.get(), description, authors, language.get(), voices));
        }

        private List<Voice> voices(JsonObject json) {
            List<Voice> voices = new ArrayList<>();
            Optional<JsonArray> array = array(json, "voices", "", true);
            if (array.isEmpty()) {
                return voices;
            }
            if (array.get().isEmpty()) {
                report(ProblemCode.INVALID_VALUE, "voices", "A pack needs at least one voice");
            }
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < array.get().size(); i++) {
                String path = "voices[" + i + "]";
                JsonElement element = array.get().get(i);
                if (!element.isJsonObject()) {
                    report(ProblemCode.INVALID_VALUE, path, "Expected an object");
                    continue;
                }
                Optional<Voice> voice = voice(element.getAsJsonObject(), path);
                if (voice.isPresent()) {
                    if (!ids.add(voice.get().id())) {
                        report(ProblemCode.DUPLICATE_VOICE_ID, path + ".id", "Voice id '" + voice.get().id() + "' is used more than once");
                    }
                    voices.add(voice.get());
                }
            }
            return voices;
        }

        private Optional<Voice> voice(JsonObject json, String path) {
            unknownKeys(json, VOICE_KEYS, path);
            Optional<String> id = id(json, "id", path);
            Optional<String> name = nonEmptyString(json, "name", path, MAX_NAME);
            Set<EntityKind> entities = entities(json, path);
            Optional<Gender> gender = enumValue(json, "gender", path, true, Gender::byId);
            boolean child = bool(json, "child", path).orElse(false);
            Optional<String> language = json.has("language") && json.get("language").isJsonNull()
                    ? Optional.empty()
                    : language(json, "language", path, false);
            PitchRange pitch = pitch(json, path);
            int weight = integer(json, "weight", path, false).orElse(1);
            if (weight < 1 || weight > 100) {
                report(ProblemCode.INVALID_VALUE, join(path, "weight"), "Weight must be between 1 and 100");
            }
            Map<String, List<VoiceLine>> lines = lines(json, path);
            List<String> legacyIds = nonEmptyStrings(json, "legacyIds", path);

            if (id.isEmpty() || name.isEmpty() || entities.isEmpty() || gender.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Voice(id.get(), name.get(), entities, gender.get(), child, language, pitch, weight, lines, legacyIds));
        }

        private Set<EntityKind> entities(JsonObject json, String path) {
            Set<EntityKind> kinds = EnumSet.noneOf(EntityKind.class);
            List<String> values = stringList(json, "entities", path, true, Function.identity());
            if (json.has("entities") && values.isEmpty()) {
                report(ProblemCode.INVALID_VALUE, join(path, "entities"), "At least one entity is required");
            }
            for (int i = 0; i < values.size(); i++) {
                Set<EntityKind> resolved = EntityKind.resolve(values.get(i));
                if (resolved.isEmpty()) {
                    report(ProblemCode.UNKNOWN_ENTITY, join(path, "entities") + "[" + i + "]", "Unknown entity '" + values.get(i) + "'");
                }
                kinds.addAll(resolved);
            }
            return kinds;
        }

        private PitchRange pitch(JsonObject json, String path) {
            Optional<JsonObject> object = object(json, "pitch", path, false);
            if (object.isEmpty()) {
                return PitchRange.DEFAULT;
            }
            String pitchPath = join(path, "pitch");
            unknownKeys(object.get(), Set.of("min", "max"), pitchPath);
            Optional<Float> min = number(object.get(), "min", pitchPath, true);
            Optional<Float> max = number(object.get(), "max", pitchPath, true);
            if (min.isEmpty() || max.isEmpty()) {
                return PitchRange.DEFAULT;
            }
            if (outsidePitch(min.get()) || outsidePitch(max.get())) {
                report(ProblemCode.INVALID_VALUE, pitchPath, "Pitch must be between " + PitchRange.LOWEST + " and " + PitchRange.HIGHEST);
                return PitchRange.DEFAULT;
            }
            if (min.get() > max.get()) {
                report(ProblemCode.INVALID_VALUE, pitchPath, "min must not be greater than max");
                return PitchRange.DEFAULT;
            }
            return new PitchRange(min.get(), max.get());
        }

        private static boolean outsidePitch(float value) {
            return value < PitchRange.LOWEST || value > PitchRange.HIGHEST;
        }

        private Map<String, List<VoiceLine>> lines(JsonObject json, String path) {
            Map<String, List<VoiceLine>> lines = new LinkedHashMap<>();
            Optional<JsonObject> object = object(json, "lines", path, true);
            if (object.isEmpty()) {
                return lines;
            }
            for (Map.Entry<String, JsonElement> entry : object.get().entrySet()) {
                String eventPath = join(path, "lines") + "." + entry.getKey();
                if (EventCatalog.byId(entry.getKey()).isEmpty()) {
                    report(ProblemCode.UNKNOWN_EVENT, eventPath, "Unknown event '" + entry.getKey() + "', its lines are ignored");
                    continue;
                }
                if (!entry.getValue().isJsonArray()) {
                    report(ProblemCode.INVALID_VALUE, eventPath, "Expected an array of lines");
                    continue;
                }
                JsonArray array = entry.getValue().getAsJsonArray();
                List<VoiceLine> eventLines = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    String linePath = eventPath + "[" + i + "]";
                    if (!array.get(i).isJsonObject()) {
                        report(ProblemCode.INVALID_VALUE, linePath, "Expected an object");
                        continue;
                    }
                    line(array.get(i).getAsJsonObject(), linePath).ifPresent(eventLines::add);
                }
                lines.put(entry.getKey(), List.copyOf(eventLines));
            }
            return lines;
        }

        private Optional<VoiceLine> line(JsonObject json, String path) {
            unknownKeys(json, LINE_KEYS, path);
            Optional<String> file = string(json, "file", path, true);
            if (file.isPresent() && !FILE.matcher(file.get()).matches()) {
                report(ProblemCode.INVALID_FILE_PATH, join(path, "file"),
                        "'" + file.get() + "' must be a lower case .ogg path inside the sounds/ folder");
                file = Optional.empty();
            }
            Optional<String> text = string(json, "text", path, false, MAX_TEXT);
            Optional<Float> duration = number(json, "duration", path, false);
            if (duration.isPresent() && (duration.get() <= 0 || duration.get() > 60)) {
                report(ProblemCode.INVALID_VALUE, join(path, "duration"), "Duration must be between 0 and 60 seconds");
            }
            LineConditions conditions = conditions(json, path);
            return file.map(f -> new VoiceLine(f, text, duration, conditions));
        }

        private LineConditions conditions(JsonObject json, String path) {
            Optional<JsonObject> object = object(json, "conditions", path, false);
            if (object.isEmpty()) {
                return LineConditions.NONE;
            }
            String conditionPath = join(path, "conditions");
            unknownKeys(object.get(), CONDITION_KEYS, conditionPath);
            List<String> professions = stringList(object.get(), "professions", conditionPath, false, Professions::normalize);
            for (int i = 0; i < professions.size(); i++) {
                if (!Professions.isKnown(professions.get(i))) {
                    report(ProblemCode.UNKNOWN_PROFESSION, join(conditionPath, "professions") + "[" + i + "]",
                            "Unknown profession '" + professions.get(i) + "'. Use a namespaced id for professions of other mods.");
                }
            }
            Optional<Weather> weather = enumValue(object.get(), "weather", conditionPath, false, Weather::byId);
            return new LineConditions(new LinkedHashSet<>(professions), weather);
        }

        // --- primitive readers -------------------------------------------------------------------------------------

        private void unknownKeys(JsonObject json, Set<String> known, String path) {
            for (String key : json.keySet()) {
                if (!known.contains(key)) {
                    report(ProblemCode.UNKNOWN_PROPERTY, join(path, key), "Unknown property '" + key + "' is ignored");
                }
            }
        }

        private Optional<JsonElement> value(JsonObject json, String key, String path, boolean required) {
            JsonElement element = json.get(key);
            if (element == null || element.isJsonNull()) {
                if (required) {
                    report(ProblemCode.MISSING_FIELD, join(path, key), "Required property '" + key + "' is missing");
                }
                return Optional.empty();
            }
            return Optional.of(element);
        }

        private Optional<String> string(JsonObject json, String key, String path, boolean required) {
            Optional<JsonElement> element = value(json, key, path, required);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isString()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected a string");
                return Optional.empty();
            }
            return Optional.of(primitive.getAsString());
        }

        /** Like the schema's {@code maxLength}, counted in code points. */
        private Optional<String> string(JsonObject json, String key, String path, boolean required, int maxLength) {
            Optional<String> value = string(json, key, path, required);
            if (value.isPresent() && value.get().codePointCount(0, value.get().length()) > maxLength) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Must not be longer than " + maxLength + " characters");
                return Optional.empty();
            }
            return value;
        }

        private Optional<String> nonEmptyString(JsonObject json, String key, String path, int maxLength) {
            Optional<String> value = string(json, key, path, true, maxLength);
            if (value.isPresent() && value.get().isBlank()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Must not be empty");
                return Optional.empty();
            }
            return value;
        }

        private Optional<String> version(JsonObject json, String key, String path) {
            Optional<String> value = string(json, key, path, true);
            if (value.isPresent() && !VERSION.matcher(value.get()).matches()) {
                report(ProblemCode.INVALID_VALUE, join(path, key),
                        "'" + value.get() + "' must be at most 32 characters of A-Z, a-z, 0-9, '.', '_', '+' and '-', like '1.2.0'");
                return Optional.empty();
            }
            return value;
        }

        private List<String> nonEmptyStrings(JsonObject json, String key, String path) {
            List<String> values = stringList(json, key, path, false, Function.identity());
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i).isEmpty()) {
                    report(ProblemCode.INVALID_VALUE, join(path, key) + "[" + i + "]", "Must not be empty");
                }
            }
            return values.stream().filter(value -> !value.isEmpty()).toList();
        }

        private Optional<String> id(JsonObject json, String key, String path) {
            Optional<String> value = string(json, key, path, true);
            if (value.isPresent() && !ID.matcher(value.get()).matches()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "'" + value.get() + "' may only contain a-z, 0-9, _ and -");
                return Optional.empty();
            }
            return value;
        }

        private Optional<String> language(JsonObject json, String key, String path, boolean required) {
            Optional<String> value = string(json, key, path, required);
            if (value.isPresent() && !Languages.isValid(value.get())) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "'" + value.get() + "' is not a language tag like 'de' or 'en-GB'");
                return Optional.empty();
            }
            return value.map(Languages::normalize);
        }

        private Optional<Boolean> bool(JsonObject json, String key, String path) {
            Optional<JsonElement> element = value(json, key, path, false);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected true or false");
                return Optional.empty();
            }
            return Optional.of(primitive.getAsBoolean());
        }

        private Optional<Float> number(JsonObject json, String key, String path, boolean required) {
            Optional<JsonElement> element = value(json, key, path, required);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected a number");
                return Optional.empty();
            }
            return Optional.of(primitive.getAsFloat());
        }

        private Optional<Integer> integer(JsonObject json, String key, String path, boolean required) {
            Optional<JsonElement> element = value(json, key, path, required);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isNumber()
                    || primitive.getAsDouble() != Math.rint(primitive.getAsDouble())) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected a whole number");
                return Optional.empty();
            }
            return Optional.of(primitive.getAsInt());
        }

        private <T> Optional<T> enumValue(JsonObject json, String key, String path, boolean required, Function<String, Optional<T>> lookup) {
            Optional<String> value = string(json, key, path, required);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            Optional<T> result = lookup.apply(value.get());
            if (result.isEmpty()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Unknown value '" + value.get() + "'");
            }
            return result;
        }

        private Optional<JsonObject> object(JsonObject json, String key, String path, boolean required) {
            Optional<JsonElement> element = value(json, key, path, required);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!element.get().isJsonObject()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected an object");
                return Optional.empty();
            }
            return Optional.of(element.get().getAsJsonObject());
        }

        private Optional<JsonArray> array(JsonObject json, String key, String path, boolean required) {
            Optional<JsonElement> element = value(json, key, path, required);
            if (element.isEmpty()) {
                return Optional.empty();
            }
            if (!element.get().isJsonArray()) {
                report(ProblemCode.INVALID_VALUE, join(path, key), "Expected an array");
                return Optional.empty();
            }
            return Optional.of(element.get().getAsJsonArray());
        }

        private List<String> stringList(JsonObject json, String key, String path, boolean required, Function<String, String> mapper) {
            Optional<JsonArray> array = array(json, key, path, required);
            List<String> values = new ArrayList<>();
            if (array.isEmpty()) {
                return values;
            }
            for (int i = 0; i < array.get().size(); i++) {
                JsonElement element = array.get().get(i);
                if (element instanceof JsonPrimitive primitive && primitive.isString()) {
                    values.add(mapper.apply(primitive.getAsString()));
                } else {
                    report(ProblemCode.INVALID_VALUE, join(path, key) + "[" + i + "]", "Expected a string");
                }
            }
            return values;
        }

        private static String join(String path, String key) {
            return path.isEmpty() ? key : path + "." + key;
        }
    }

    // --- writing -----------------------------------------------------------------------------------------------------

    public static String write(VoicePack pack) {
        JsonObject json = new JsonObject();
        json.addProperty("format", pack.format());
        json.addProperty("id", pack.id());
        json.addProperty("name", pack.name());
        json.addProperty("version", pack.version());
        pack.description().ifPresent(description -> json.addProperty("description", description));
        if (!pack.authors().isEmpty()) {
            json.add("authors", stringArray(pack.authors()));
        }
        json.addProperty("language", pack.language());
        JsonArray voices = new JsonArray();
        pack.voices().forEach(voice -> voices.add(writeVoice(voice)));
        json.add("voices", voices);
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json);
    }

    private static JsonObject writeVoice(Voice voice) {
        JsonObject json = new JsonObject();
        json.addProperty("id", voice.id());
        json.addProperty("name", voice.name());
        json.add("entities", stringArray(voice.entities().stream().sorted().map(EntityKind::id).toList()));
        json.addProperty("gender", voice.gender().id());
        if (voice.child()) {
            json.addProperty("child", true);
        }
        voice.language().ifPresent(language -> json.addProperty("language", language));
        if (!voice.pitch().equals(PitchRange.DEFAULT)) {
            JsonObject pitch = new JsonObject();
            pitch.addProperty("min", voice.pitch().min());
            pitch.addProperty("max", voice.pitch().max());
            json.add("pitch", pitch);
        }
        if (voice.weight() != 1) {
            json.addProperty("weight", voice.weight());
        }
        JsonObject lines = new JsonObject();
        // catalog order keeps written manifests stable
        EventCatalog.all().forEach(event -> {
            List<VoiceLine> eventLines = voice.lines(event);
            if (!eventLines.isEmpty()) {
                JsonArray array = new JsonArray();
                eventLines.forEach(line -> array.add(writeLine(line)));
                lines.add(event.id(), array);
            }
        });
        json.add("lines", lines);
        if (!voice.legacyIds().isEmpty()) {
            json.add("legacyIds", stringArray(voice.legacyIds()));
        }
        return json;
    }

    private static JsonObject writeLine(VoiceLine line) {
        JsonObject json = new JsonObject();
        json.addProperty("file", line.file());
        line.text().ifPresent(text -> json.addProperty("text", text));
        line.duration().ifPresent(duration -> json.addProperty("duration", duration));
        LineConditions conditions = line.conditions();
        if (!conditions.isEmpty()) {
            JsonObject object = new JsonObject();
            if (!conditions.professions().isEmpty()) {
                object.add("professions", stringArray(conditions.professions().stream().sorted().toList()));
            }
            conditions.weather().ifPresent(weather -> object.addProperty("weather", weather.id()));
            json.add("conditions", object);
        }
        return json;
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
