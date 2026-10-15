# Phase 7 – Voicepack-Manager und Einstellungen in Mod Menu

## Status: erledigt

Umgesetzt auf Branch `v2/phase-7` (abgeleitet von `v2/phase-3b`). Geprüft mit einem automatisierten Fabric-Client-Test (`GuiClientGameTest`), der alle Screens öffnet und Screenshots macht. Der Abspiel-Test aus Phase 2 lief dabei ebenfalls erfolgreich durch.

Umgesetzt:
- **`VoicepacksScreen`** (Einstieg über Mod Menu bzw. die NeoForge-Mod-Liste):
  - Suche, Pack-Zeilen mit Icon, Version, Sprache und Stimmen-Zusammenfassung
  - An/Aus, Löschen nach `voicepacks/.trash`, „Details“
  - fehlerhafte Packs in Rot mit erstem Fehler
  - Abschnitt „Vom Server verwendet, fehlen hier“
  - Buttons: Ordner öffnen, Neu laden, Web-Editor, Einstellungen
- **Drag & Drop:** Prüfung jeder Datei, Bestätigungsdialog mit Name, Version, Stimmenanzahl und Sprache, „ersetzt vX“ bei Updates (die alte Datei wandert in den Papierkorb), Toast danach. V1-Packs und andere Dateien werden mit Erklärung abgelehnt.
- **`PackDetailsScreen`:** Infos, pro Stimme Probehören (▶), Zeilenliste und An/Aus; bei Problemen alle Fehler und Warnungen mit Fundstelle.
- **Zeilen in zwei Ebenen:** `VoiceEventsScreen` listet die Events einer Stimme mit Beschreibung, Anzahl, ▶ (zufällige Probe) und „Zeilen ›“. `EventLinesScreen` zeigt die Zeilen eines Events mit Text, Datei, Länge, Bedingungen und ▶.
- **Suchfelder:** in der Event-Liste (findet Events auch über den Text ihrer Zeilen, die Suche wird in die Zeilenliste übernommen), in der Zeilenliste ab 6 Zeilen und in den Pack-Details ab 4 Stimmen.
- **Event-Beschreibungen** stammen aus dem Web-Editor und stehen als `talkingvillagers.event.<id>` in den Sprachdateien.
- **`SettingsScreen`:**
  - Für dich: Lautstärke, Untertitel (Text / „Dorfbewohner spricht“ / aus)
  - Stimmen: Anteil Frauen, Anteil weiblicher Hexen, Kinder-Fallback, gleichzeitige Sprecher
  - Wer spricht: an/aus pro Entity-Art
  - Sprache: Modus, bevorzugte und erlaubte Sprachen, strikt, Händler, Raids

  Änderungen werden beim Schließen gespeichert. Server-Einstellungen sind auf fremden Servern ausgegraut.
- **Config:** `config/talkingvillagers-client.json` ist neu. Die Server-Config bekommt `disabledEntities`, `disabledPacks`, `disabledVoices` und `maxConcurrentSpeakers`. Deaktivierte Packs und Stimmen werden nicht mehr zugewiesen, betroffene Entities bekommen eine neue Stimme.
- **Probehören** im Menü (`PreviewSounds`): nicht positionsabhängig, Kategorie „Stimme/Sprache“.
- **Texte:** Deutsch und Englisch.

Abweichungen vom Plan:
- **Kein Button im Sound-Menü und keine Taste:** Der Einstieg ist Mod Menu (Fabric) bzw. die eingebaute Mod-Liste (NeoForge). Ein zusätzlicher Einstieg kann später kommen.
- **Keine Einstellung für Smalltalk-Häufigkeit und Cooldowns pro Event** („Erweitert“). Die Werte aus `EventPolicies` gelten weiterhin.
- **Web-Editor-Button:** Er verlinkt auf die geplante GitHub-Pages-Adresse. Die Seite gibt es erst mit Phase 8.
- **V1-Packs** werden erkannt, aber noch nicht konvertiert (Phase 9).
- **Fehlende Server-Packs** werden nur angezeigt, der Download kommt mit Phase 9.

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
