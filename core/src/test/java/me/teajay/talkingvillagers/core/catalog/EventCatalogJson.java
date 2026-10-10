package me.teajay.talkingvillagers.core.catalog;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes {@code schema/events.json}, the event catalog for the web editor: {@code ./gradlew :core:writeEventCatalog}.
 * {@link EventCatalogJsonTest} fails when the file is out of date.
 */
public final class EventCatalogJson {
    private EventCatalogJson() {
    }

    static String render() {
        JsonArray events = new JsonArray();
        for (VoiceEvent event : EventCatalog.all()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", event.id());
            JsonArray entities = new JsonArray();
            event.kinds().stream().sorted().map(EntityKind::id).forEach(entities::add);
            json.add("entities", entities);
            event.parent().ifPresent(parent -> json.addProperty("parent", parent.id()));
            events.add(json);
        }
        JsonObject root = new JsonObject();
        root.addProperty("$comment", "Generated from EventCatalog by ./gradlew :core:writeEventCatalog – do not edit");
        root.add("events", events);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        Files.writeString(file, render());
        System.out.println("Wrote " + file);
    }
}
