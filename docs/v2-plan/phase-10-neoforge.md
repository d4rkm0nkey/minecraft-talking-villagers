# Phase 10 – NeoForge-Port

## Status: erledigt (Server und Client getestet)

NeoForge läuft seit Phase 0 parallel mit: Jede Phase wurde auf Fabric **und** NeoForge gebaut und auf dem NeoForge-Server per RCON getestet. Diese Phase schließt den Port nur noch ab. Branch `v2/phase-10` (abgeleitet von `v2/phase-9`).

**Abweichungen vom Plan**
- Es gibt keine eigenen Services für Ressourcen, Events und Pfade. Die Sounds kommen über einen Mixin auf `SoundBufferLibrary` direkt aus der ZIP-Datei. Das funktioniert auf beiden Loadern ohne Loader-Code. Events werden in den Einstiegsklassen der Loader registriert, Pfade laufen über `PlatformHelper.getGameDir()`.
  - Fabric: `TalkingVillagersFabric`, `TalkingVillagersFabricClient`
  - NeoForge: `TalkingVillagersNeoForge`, `TalkingVillagersNeoForgeClient`
- Payloads sind auf NeoForge `optional()` registriert. Spieler ohne den Mod können also joinen und hören Vanilla-Sounds, wie auf Fabric.
- Statt eines ArchUnit-Tests prüft der Gradle-Task `checkNoLoaderImports` in `common` und `core`, dass weder Imports noch voll qualifizierte Namen aus `net.fabricmc`, `net.neoforged` oder `com.terraformersmc` vorkommen. Kommentare sind ausgenommen. Der Task hängt an `check` und läuft damit bei jedem `build` und in CI.
- Für NeoForge gibt es keine Client-Gametests, die Fabric-Gametests decken den Client-Code in `common` ab. Für Handtests verbindet `./gradlew :neoforge:runClient -PquickPlay=localhost:25565` den Dev-Client direkt mit einem Server.

**Getestet auf NeoForge (10.10.2026)**

| Test | Ergebnis |
|---|---|
| Server startet mit 2 Packs (14 Stimmen), `/tv packs list` und `info` | ok |
| Alle Phasen 2–6, 3b, 4b: Stimmen, Sprachen, Commands, Zustände | ok (jeweils per RCON, siehe die Phasen-Dokumente) |
| Client verbindet sich, Server meldet fehlende Packs | ok |
| Pack-Download über die Spielverbindung (vertrauter Server, ohne Dialog), 2 × 4,2 MB in etwa 4 s, Prüfung und Ablage in `.server-cache` | ok |
| Villager neben dem Spieler spricht per `/tv voice say` (`greeting`, `random`) und von selbst (`ambient`) | Server sendet, im Client-Log keine Fehler |
| `cheer` ohne passende Zeilen im Pack | bleibt still, wie erwartet |

**Offen** (Testmatrix aus Phase 9): Kompatibilität mit Sodium/Embeddium, Iris/Oculus und MCA. Außerdem die Prüfung im Spiel, ob die Zeilen hörbar sind und der Config-Screen in der NeoForge-Mod-Liste funktioniert; beides geht nur von Hand.

## Ziel
Dieselbe Mod mit denselben Features und demselben `.voicepack`-Format läuft auf NeoForge. Der Aufwand bleibt klein, weil Phase 0 die Multi-Loader-Struktur und die Plattform-Services schon angelegt hat.

## Warum das jetzt günstig ist
- Seit Minecraft 26.x nutzen Fabric und NeoForge beide die offiziellen Mojang-Namen. Mixins und der Großteil des Codes in `common` laufen auf beiden Loadern unverändert.
- `core` (Format, Auswahl, Validierung) ist reines Java.
- Pro Loader bleibt nur die Implementierung der Plattform-Services übrig.

## Zuordnung der Plattform-Services

| Service (common) | Fabric | NeoForge |
|---|---|---|
| `PlatformNetworking` | `PayloadTypeRegistry` + `ServerPlayNetworking` | `RegisterPayloadHandlersEvent` |
| `PlatformAttachments` | Fabric Data Attachment API | `NeoForgeRegistries.ATTACHMENT_TYPES` (`AttachmentType` mit Codec und Sync) |
| `PlatformResources` (virtuelles Resourcepack) | Resource-Loader-API bzw. `RepositorySource`-Mixin | `AddPackFindersEvent` |
| `PlatformEvents` (Commands, Server-Start/-Stop, Join) | `CommandRegistrationCallback`, `ServerLifecycleEvents`, `ServerPlayConnectionEvents` | `RegisterCommandsEvent`, `ServerStartingEvent`, `PlayerLoggedInEvent` |
| `PlatformPaths` | `FabricLoader.getGameDir()` | `FMLPaths.GAMEDIR` |
| Config-Screen-Einstieg | Mod Menu (`ModMenuApi`) | `IConfigScreenFactory` als Extension Point (eingebaute Mod-Liste) |
| GameTests | `fabric-gametest-api` | NeoForge GameTest-Integration |

## Aufgaben
- [x] `neoforge/`-Modul mit ModDevGradle auf aktuelle NeoForge-Version für 26.x
- [x] `neoforge.mods.toml`, Mixin-Config aus `common` einbinden
- [x] Services implementieren (Tabelle oben)
- [x] Netzwerk-Kompatibilität: gleiche Payload-IDs und Codecs. Ein Fabric-Client auf einem NeoForge-Server ist nicht Ziel, aber das Datenformat bleibt gleich.
- [x] CI: zweites Artefakt `talkingvillagers-neoforge-<version>.jar`
- [x] Publishing: `mc-publish` lädt beide Loader-Varianten in dasselbe Modrinth- und CurseForge-Projekt hoch
- [ ] Testmatrix aus Phase 9 auf NeoForge wiederholen

## Ergebnis
Ein Release mit zwei JARs (Fabric, NeoForge). Ein `.voicepack` funktioniert identisch auf beiden.

## Aufwandsschätzung
Etwa 15–25 % des Fabric-Aufwands, wenn in Phase 0–9 die Regel „kein Loader-Import in `common`“ eingehalten wurde. Ohne diese Disziplin wäre es ein halber Neubau. Daher empfiehlt sich ein CI-Check (z. B. ArchUnit-Test), der Loader-Imports in `common` verbietet.
