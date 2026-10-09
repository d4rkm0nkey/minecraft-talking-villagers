# Phase 7 – Voicepack-Manager und Einstellungen in Mod Menu

## Ziel
Voicepacks werden komplett im Spiel verwaltet: installieren (per Drag & Drop), aktivieren/deaktivieren, Stimmen anhören, löschen. Der Einstieg erfolgt über Mod Menu (Config-Button) und optional über einen Button im Optionen-Menü. SpruceUI und Cloth Config werden durch Vanilla-Widgets ersetzt.

## Screens

### 1. `VoicepackScreen` (Hauptscreen, öffnet sich aus Mod Menu)
```
┌───────────────────────── Talking Villagers – Voicepacks ─────────────────────────┐
│ [Suche…]   Sprache: [Alle ▾]   Entity: [Alle ▾]                                  │
│ ┌──────────────────────────────────────────────────────────────────────────────┐ │
│ │ [icon] TeaJay Default         v2.0.0   🇩🇪 de   12 Stimmen   [✔ aktiv]  [⋯]  │ │
│ │        Villager ♂6 ♀4  Kinder 2 · Witch 1 · Illager 2                       │ │
│ │ [icon] English Village        v1.1.0   🇬🇧 en    8 Stimmen   [  aus ]   [⋯]  │ │
│ │ [!]    broken-pack.voicepack     Fehler: sounds/x.ogg fehlt          [Details] │ │
│ └──────────────────────────────────────────────────────────────────────────────┘ │
│  Dateien hierher ziehen zum Installieren                                         │
│ [Ordner öffnen] [Neu laden] [Web-Editor öffnen] [Einstellungen]        [Fertig]  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
- **Installation per Drag & Drop:** `Screen.onFilesDrop(List<Path>)`, so wie im Vanilla-Resourcepack-Screen. Ablauf: `.voicepack` validieren, Bestätigungsdialog („Pack X v1.0 installieren? 12 Stimmen, Sprache de“), Datei nach `voicepacks/` kopieren, Packs neu laden. Bei gleicher Pack-ID: „Aktualisieren von v1.0 auf v1.1?“.
- **Legacy-Erkennung:** Wird ein altes V1-Resourcepack-ZIP gedroppt, erscheint der Hinweis „Dies ist ein V1-Pack. Konvertieren?“. Die Konvertierung kommt aus Phase 9.
- **Aktiv/Inaktiv** wird in der Config gespeichert (`disabledPacks`). Die Datei bleibt liegen.
- **Löschen** (über das `⋯`-Menü) nach Bestätigung. Die Datei wandert in `voicepacks/.trash/`, damit sie wiederherstellbar ist.
- Fehlerhafte Packs werden mit Problemen aus dem `PackValidator` angezeigt.

### 2. `VoicepackDetailScreen`
- Metadaten (Autoren, Beschreibung, Version, Sprache)
- Liste der Stimmen: Name, Entity-Icons, ♂/♀/⚲, Kind-Badge, Anzahl Zeilen pro Event
- **▶ Probehören:** pro Stimme eine zufällige Zeile, pro Event aufklappbar mit allen Zeilen und Transkript. Abgespielt wird lokal über `VoicePlayer` (ohne Entity, nicht positional).
- Einzelne Stimmen deaktivieren (`disabledVoices`).

### 3. `SettingsScreen` (eigener Screen, Vanilla `OptionsList` bzw. Tabs)
Tabs:
- **Allgemein:** Lautstärke-Multiplikator, Untertitel (Transkript / generisch / aus), Pitch-Varianz, max. gleichzeitige Sprecher
- **Entities:** pro Typ an/aus, Häufigkeit (`random`-Chance), Witch-Geschlecht
- **Stimmen:** Geschlechterverteilung, Kinder-Fallback, Vielfalt im Dorf (`preferLeastUsed`)
- **Sprache:** `languageMode`, bevorzugte/erlaubte Sprachen (Mehrfachauswahl aus installierten), `strictLanguage`
- **Erweitert:** Cooldowns pro Event (für Power-User)

Hinweis im Multiplayer: Zuweisungsregeln wie Sprache und Geschlecht gelten **serverseitig**. Auf einem fremden Server sind diese Optionen ausgegraut („Wird vom Server bestimmt“). Lautstärke und Untertitel bleiben clientseitig.

## Config-System
- [ ] Eigene, kleine Config-Klasse mit `Codec` und JSON unter `config/talkingvillagers.json`, inkl. Versionsfeld und Migration. Keine Cloth/AutoConfig-Abhängigkeit mehr.
- [ ] Getrennt in `ClientConfig` (Lautstärke, Untertitel, UI) und `ServerConfig` (Zuweisungsregeln, Cooldowns). Beim Dedicated Server liegt nur die Server-Datei vor.
- [ ] Änderungen werden sofort übernommen. Bei Zuweisungsregeln bietet die GUI optional „Alle geladenen Entities neu einkleiden“ an.

## Mod-Menu-Integration
- [ ] `ModMenuApi#getModConfigScreenFactory` → `VoicepackScreen`.
- [ ] Ohne Mod Menu: Button „Voices…“ im `SoundOptionsScreen` (Musik & Geräusche) per Mixin oder Fabric Screen API. Das ersetzt den TitleScreen-Button aus V1, der mit anderen Mods kollidieren kann.
- [ ] Optional: Keybinding (ungebunden per Default) „Stimme des anvisierten Villagers anzeigen/ändern“ → kleines Overlay, nutzt bei OP-Rechten den `VoiceCommandService`.

## Ergebnis
Ein Spieler lädt ein `.voicepack` herunter, öffnet Mod Menu → Talking Villagers, zieht die Datei ins Fenster, bestätigt, hört Probe. Fertig, ohne Neustart.

## Risiken
- `onFilesDrop` wird unter Wayland/Linux von GLFW nicht immer unterstützt. Fallback ist der Button „Ordner öffnen“ (`Util.getPlatform().openPath`, statt `java.awt.Desktop` wie in V1, das headless-Probleme hatte).
