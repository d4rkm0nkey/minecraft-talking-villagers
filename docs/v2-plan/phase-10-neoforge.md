# Phase 10 – NeoForge-Port

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
- [ ] `neoforge/`-Modul mit ModDevGradle auf aktuelle NeoForge-Version für 26.x
- [ ] `neoforge.mods.toml`, Mixin-Config aus `common` einbinden
- [ ] Services implementieren (Tabelle oben)
- [ ] Netzwerk-Kompatibilität: gleiche Payload-IDs und Codecs. Ein Fabric-Client auf einem NeoForge-Server ist nicht Ziel, aber das Datenformat bleibt gleich.
- [ ] CI: zweites Artefakt `talkingvillagers-neoforge-<version>.jar`
- [ ] Publishing: `mc-publish` lädt beide Loader-Varianten in dasselbe Modrinth- und CurseForge-Projekt hoch
- [ ] Testmatrix aus Phase 9 auf NeoForge wiederholen

## Ergebnis
Ein Release mit zwei JARs (Fabric, NeoForge). Ein `.voicepack` funktioniert identisch auf beiden.

## Aufwandsschätzung
Etwa 15–25 % des Fabric-Aufwands, wenn in Phase 0–9 die Regel „kein Loader-Import in `common`“ eingehalten wurde. Ohne diese Disziplin wäre es ein halber Neubau. Daher empfiehlt sich ein CI-Check (z. B. ArchUnit-Test), der Loader-Imports in `common` verbietet.
