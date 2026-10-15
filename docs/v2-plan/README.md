# Talking Villagers V2 – Gesamtplan

Ziel: Talking Villagers wird für die aktuelle Minecraft-Version (26.x, Fabric) neu aufgebaut, mit sauberer Architektur, Voicepacks als einzelner Datei, Verwaltung im Spiel, Web-Editor, mehr Entities (Witch, Wandering Trader, Illager), Kinder-, Geschlechts- und Sprach-Stimmen sowie Commands.

## Ist-Zustand (Analyse des aktuellen Codes)

| Bereich | Befund |
|---|---|
| Build | Fabric 1.19.3, Yarn-Mappings, Loom 1.1, Java 17. `fabric.mod.json` verlangt noch `1.18.x`. |
| Kompilierbarkeit | `master` kompiliert nicht. Die Umstellung von `VillagerVoiceManager` auf `VoiceManager` ist nicht fertig: `VillagerEntityMixin` greift noch auf `VillagerVoiceManager.Reason` und `voiceManager` zu. |
| Repo-Hygiene | `remappedSrc/` (zwei alte Paketstände), `logs/` und `config/teajay.json` sind eingecheckt. |
| Voicepack-Modell | Zwei Teile: Resourcepack-ZIP (Audio und `sounds.json`) plus JSON-Datei in `config/talkingvillagers/voices/`. Der Spieler muss beides installieren, auf Client **und** Server. |
| Sound-Registrierung | `SoundEvent`s werden beim Start aus Config-Dateien in `Registries.SOUND_EVENT` registriert. Folgen: Neustart nötig, Registry-Desync, wenn Client- und Server-Configs abweichen. |
| Serialisierung | JSON wird von Hand per String-Verkettung gebaut. Fehler darin: `",/n"` statt `"\n"`, fehlende Anführungszeichen in `SimpleVoiceConfig`, Vergleich von Strings mit `==` in `Voicepack.getVoice`. |
| Datenmodell | `VoiceDataContainer` hat ein Feld pro Beruf und ein Feld pro Event, dazu große `switch`-Blöcke. Ein neues Event oder ein neuer Beruf erfordert Änderungen an vier Stellen. |
| Entities | Nur Villager funktioniert. `WitchEntityMixin` zielt auf einen nicht existierenden Konstruktor und ist nicht in `mixins.json` eingetragen. Pillager und Trader existieren nur als leere Klassen. |
| Logik in Mixins | Cooldowns, Zufall und Event-Auswahl stecken direkt im `VillagerEntityMixin`. Dadurch ist die Logik nicht testbar und nicht wiederverwendbar. |
| GUI | Basiert auf SpruceUI (seit Jahren kaum gepflegt) und einem TitleScreen-Mixin. |
| Tests | Keine. |

**Fazit:** Ein Port lohnt sich nicht. V2 wird neu geschrieben. Übernommen werden die Event-Liste, die Ideen zum Verhalten (Cooldowns, Greeting, Hero, Gossip), die Audio-Dateien des Default-Packs und die Voicepack-Guidelines aus der README.

## Leitprinzipien der Architektur

1. **Pure-Java-Kern:** Datenmodell, Parser, Validierung und Stimmenauswahl kommen ohne Minecraft-Klassen aus und sind per JUnit testbar.
2. **Dünne Mixins:** Mixins melden nur Ereignisse („Villager wurde verletzt“). Was gesagt wird, entscheidet ein `VoiceController`.
3. **Daten statt Code:** Neue Events, Entities und Berufe erfordern nur einen Registry-Eintrag, keine neuen Felder oder `switch`-Fälle.
4. **Keine dynamische Registry-Manipulation:** Sounds werden nicht mehr in `BuiltInRegistries.SOUND_EVENT` registriert. Sie kommen über ein virtuelles Resourcepack auf dem Client. Der Server verschickt nur IDs.
5. **Ein Format, eine Quelle der Wahrheit:** Ein JSON-Schema (`schema/voicepack.schema.json`) beschreibt das Format. Mod und Web-Editor nutzen es beide.
6. **Multi-Loader-fähig:** `core` und `common` enthalten keinen Loader-Code. Fabric kommt zuerst, NeoForge später über eine dünne Service-Schicht (Phase 10).
7. **Wenige Abhängigkeiten:** Nur Fabric API und Mod Menu (optional). Kein SpruceUI und kein Cloth Config, die Screens werden mit Vanilla-Widgets gebaut.

## Phasen

| # | Datei | Inhalt | Voraussetzung |
|---|---|---|---|
| 0 | [phase-0-setup.md](phase-0-setup.md) | Neues Projekt-Setup für 26.x, Repo-Bereinigung, CI, Modulstruktur | – |
| 1 | [phase-1-core-format.md](phase-1-core-format.md) | Voicepack-Format v2 (`.voicepack`), Kern-Domain, Stimmenauswahl, Tests | 0 |
| 2 | [phase-2-pack-loading-audio.md](phase-2-pack-loading-audio.md) | Packs laden, virtuelles Resourcepack, Abspielen, Networking | 1 |
| 3 | [phase-3-villager.md](phase-3-villager.md) | Villager neu: Attachments, Controller, Kinder/Erwachsen, Geschlecht | 2 |
| 3b | [phase-3b-villager-states.md](phase-3b-villager-states.md) | Villager-Zustände (Panik, Raid, Verstecken, Arbeit) und Reaktionen (Spieler-Angriff, Heilung, Reputation) | 3, 4b |
| 4 | [phase-4-more-entities.md](phase-4-more-entities.md) | Witch, Wandering Trader, Pillager, Vindicator, Evoker, Illusioner | 3 |
| 4b | [phase-4b-hostile-states.md](phase-4b-hostile-states.md) | Zustände für Witches und Illager: Leerlauf, Patrouille, Raid, Kampf, Reaktionen | 4 |
| 5 | [phase-5-languages-villages.md](phase-5-languages-villages.md) | Sprachen, zufällige Sprache pro Dorf | 3 |
| 6 | [phase-6-commands.md](phase-6-commands.md) | `/talkingvillagers`-Commands | 3 (5 für Sprach-Commands) |
| 7 | [phase-7-ingame-gui.md](phase-7-ingame-gui.md) | Voicepack-Manager und Einstellungen in Mod Menu | 2 |
| 8 | [phase-8-web-editor.md](phase-8-web-editor.md) | Web-GUI zum Erstellen von Voicepacks | 1 (Schema) |
| 9 | [phase-9-migration-release.md](phase-9-migration-release.md) | Multiplayer-Feinschliff, Migration von V1, Default-Pack, Release | alle |
| 10 | [phase-10-neoforge.md](phase-10-neoforge.md) | NeoForge-Port über die Plattform-Services | 9 (Struktur aus 0) |

```
0 ──► 1 ──► 2 ──► 3 ──► 4 ──► 4b
       │     │     ├──► 5 ──► 6
       │     │     └──────────► 6
       │     └──► 7
       └──► 8   (parallel ab Phase 1 möglich)
                                 alle ──► 9 ──► 10
```

Phase 8 (Web-Editor) hängt nur vom Format aus Phase 1 ab und kann parallel zu den Phasen 2–7 laufen.

## Grundsatzentscheidungen

| Thema | Entscheidung |
|---|---|
| Loader | **Fabric zuerst.** Die Struktur ist ab Phase 0 Multi-Loader (`core`/`common`/`fabric`/`neoforge`), der NeoForge-Port folgt in Phase 10. |
| Ablageort der Packs | **`.minecraft/voicepacks/`** (auf dem Server: `<server>/voicepacks/`) |
| Dateiendung | **`.voicepack`** (intern ein ZIP) |
| Mod auf dem Server nötig? | **Ja (Option A).** Der Server ist maßgeblich: Er weist Stimmen zu, speichert sie und löst Zeilen aus. Clients spielen nur ab. Der Server braucht Mod und Voicepacks. Auf Servern ohne die Mod gibt es keine Stimmen, dort laufen die Vanilla-Sounds. Ein Client-only-Modus ist bewusst nicht geplant. |
