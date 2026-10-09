# Phase 0 – Projekt-Setup für Minecraft 26.x

## Ziel
Ein sauberes, kompilierendes Fabric-Projekt für die aktuelle Minecraft-Version mit CI und Testinfrastruktur. Fachlogik kommt erst ab Phase 1.

## Hintergrund
- Mojang nummeriert seit 2026 nach Jahren (26.1, 26.2, …). 26.x verlangt **Java 25**.
- Ab 26.1 ist Minecraft **nicht mehr obfuskiert**. Fabric arbeitet mit den offiziellen Mojang-Namen, Yarn entfällt. Klassen heißen daher z. B. `Villager` statt `VillagerEntity` und `ResourceLocation` bzw. `Identifier` statt Yarn-`Identifier`. Alte Mixins lassen sich also nicht 1:1 übernehmen.
- Vor dem Start sind die genauen Versionen auf <https://fabricmc.net/develop> zu prüfen: Minecraft, Loader, Fabric API, Loom, Mod Menu.

## Aufgaben

### Repo bereinigen
- [ ] Branch `v1` vom aktuellen `master` anlegen, damit der alte Stand erhalten bleibt.
- [ ] Löschen: `remappedSrc/`, `logs/`, `config/teajay.json`, `scripts/` (ersetzt durch den Web-Editor in Phase 8).
- [ ] Alte Audio-Dateien des Default-Packs **nicht** löschen. Sie werden nach `packs-src/teajay-default/` verschoben und in Phase 9 zu einem `.voicepack` gebaut.
- [ ] `.gitignore` ergänzen: `run/`, `logs/`, `remappedSrc/`, `web-editor/node_modules`, `web-editor/dist`.

### Build
- [ ] Gradle-Wrapper auf die aktuelle Version bringen. Loom-Plugin auf die aktuelle Version für 26.x (ohne Yarn-Mappings).
- [ ] Java-Toolchain 25.
- [ ] Modulstruktur als **Multi-Loader-Projekt** (Fabric zuerst, NeoForge vorbereitet):
  ```
  core/            Pure Java, keine MC-Abhängigkeit (Format, Parser, Auswahl-Logik, Tests)
  common/          Minecraft-Code ohne Loader-API (Mixins, Controller, Screens, Commands-Logik)
                   → kompiliert gegen „vanilla“ Minecraft (Loom im common-Modus bzw. NeoForm/ModDevGradle „vanilla“)
  fabric/          Fabric-Einstieg + Implementierung der Plattform-Services (Phase 0–9)
  neoforge/        NeoForge-Einstieg + Plattform-Services (Phase 10, zunächst nur leeres Gerüst)
  schema/          voicepack.schema.json (geteilt mit Web-Editor)
  web-editor/      Phase 8
  packs-src/       Quell-Audio für das Default-Pack
  docs/
  ```
  Vorlage: das MultiLoader-Template (jaredlll08) bzw. dessen aktueller Stand für 26.x. Seit 26.x nutzen Fabric **und** NeoForge die Mojang-Namen. Das gemeinsame Modul funktioniert damit ohne Remapping-Tricks, und genau deshalb ist NeoForge jetzt „billig“.
- [ ] **Plattform-Abstraktion** in `common` als Java-`ServiceLoader`-Interfaces, z. B. `PlatformNetworking`, `PlatformAttachments`, `PlatformResources`, `PlatformPaths`, `PlatformEvents` (Command-Registrierung, Server-Start/-Stop, Join). Regel: In `common` steht **kein** Import aus `net.fabricmc.*` oder `net.neoforged.*`.
- [ ] Client-Code in `common` liegt in einem eigenen Paket `…client`. In `fabric` wird `splitEnvironmentSourceSets()` genutzt, damit Server-Crashes durch Client-Klassen auffallen. Der Commit „Fix server crash“ zeigt, dass das schon einmal passiert ist.
- [ ] Abhängigkeiten: `fabric-api`, `modmenu` (nur `modCompileOnly` und `modLocalRuntime`, optional zur Laufzeit), JUnit 5 und AssertJ für `core`.
- [ ] Fabric-GameTests (`fabric-gametest-api`) für Integrationstests ab Phase 3 vorbereiten.

### Mod-Grundgerüst
- [ ] `fabric.mod.json`: Version `2.0.0-alpha.1`, `depends.minecraft` auf die konkrete 26.x-Range, `java >= 25`, `suggests.modmenu`, Kontakt- und Issue-Links, Autor.
- [ ] Entrypoints: `main` (`TalkingVillagers`), `client` (`TalkingVillagersClient`), `modmenu` (`TalkingVillagersModMenu`), `fabric-gametest`.
- [ ] Logger korrekt benennen (aktuell `LogManager.getLogger("Mod Menu")`). SLF4J verwenden.
- [ ] Paket umbenennen auf `me.teajay.talkingvillagers` (bleibt), mit Unterpaketen gemäß Phase 1.

### CI
- [ ] `.github/workflows/build.yml` aktualisieren: `actions/checkout@v4`, `actions/setup-java@v4` (Temurin 25), Gradle-Cache, `./gradlew build test`, Artefakt-Upload mit `upload-artifact@v4`. Die Matrix kann auf Linux reduziert werden, Windows nur bei Release.
- [ ] Zweiter Job für den Web-Editor (Lint, Test, Build), aktiv ab Phase 8.

## Ergebnis
- `./gradlew build` erzeugt ein Fabric-JAR, das in einem 26.x-Client **und** einem Dedicated Server startet (leere Mod, Log-Zeile „Talking Villagers loaded“).
- `neoforge/` existiert als Gerüst und baut (leere Mod), damit Plattform-Grenzen von Anfang an eingehalten werden.
- CI ist grün.

## Risiken
- Mod Menu ist kurz nach einem Minecraft-Release eventuell noch nicht verfügbar. Gegenmaßnahme: als optionale Abhängigkeit einbinden (`compileOnly`), damit die Mod auch ohne Mod Menu lädt.
