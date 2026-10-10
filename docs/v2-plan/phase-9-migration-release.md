# Phase 9 – Multiplayer-Feinschliff, Migration von V1, Default-Pack und Release

## Status: technisch erledigt, Release offen

Umgesetzt auf Branch `v2/phase-9` (abgeleitet von `v2/phase-8`).

**Multiplayer**
- Pack-Download wie geplant: Der Server schickt in `server_packs` pro Pack zusätzlich `downloadable` und eine optionale `url`. Der Client fragt mit einem Dialog (*Herunterladen*, *Jetzt nicht*, *Von diesem Server immer herunterladen*). Vertraute Server stehen in der Client-Config unter `trustedServers`.
- Übertragung über neue Payloads `request_pack` (C2S) und `pack_chunk` (S2C), 64 KB pro Paket. `PackTransfers` sendet pro Tick nur so viel, wie `packDownload.kbpsPerPlayer` erlaubt (Default 2048 KB/s).
- Abweichung: Es gibt kein Gesamtlimit `maxConcurrent`. Pro Spieler läuft immer nur ein Pack gleichzeitig, die übrigen warten in einer Warteschlange.
- Der Client schreibt nach `.server-cache/<sha256>.part`. Nach dem Download prüft `PackReader` den Inhalt: sha256, Manifest und Validierung. Erst dann wird die Datei in `<sha256>.voicepack` umbenannt, der Cache auf 500 MB gekürzt (älteste Dateien zuerst) und neu geladen. Bei einem Verbindungsabbruch werden `.part`-Dateien gelöscht.
- Packs aus dem Cache landen im `PackIndex` nur in `byHash`, werden also nie aktiv. Im Voicepack-Screen stehen sie im Abschnitt „Von Servern“ mit dem Knopf „Dauerhaft installieren“. Fehlende Server-Packs haben einen Knopf „Herunterladen“ und zeigen den Fortschritt.
- `packDownload.url`: Der Client lädt `<url>/<Dateiname>` per HTTP(S), mit derselben Prüfung.
- Größenlimits: Der Server bietet Packs über `packDownload.maxPackSizeMb` (Default 100) nicht an, der Client lädt nichts über 200 MB.
- Server ohne Mod: Kommt 5 s nach dem Join kein `server_packs`, zeigt der Voicepack-Screen einen gelben Hinweis.
- Performance: Smalltalk- und Hostile-Ticks laufen nur, wenn ein Spieler im Umkreis von 48 Blöcken ist. Folge für Tests über RCON ohne Spieler: Smalltalk und Kampfsprüche kommen dort nicht mehr. Messung am 2026-10-10 (Fabric-Dev-Server, 200 Villager in einem 40×40-Gehege, Spieler daneben, je 60 s JFR auf dem Server-Thread):

  | Szenario | Ø ms/Tick (P95) | Anteil Talking Villagers |
  |---|---|---|
  | Spieler im Gehege, Smalltalk aktiv | 27,1 (41,0) | 0,44 % ≈ 0,12 ms/Tick |
  | Spieler 180 Blöcke weg (Mod im Leerlauf) | 28,3 (43,6) | 0,17 % |
  | Kampf: 50 Illager, 5 Hexen, 5 Zombies im Gehege, 625 Zeilen/min | 24,6 (35,5) | 0,64 % ≈ 0,16 ms/Tick |

  Die Tickzeit kommt fast ganz aus der Vanilla-KI der Villager. Der Unterschied zwischen den ersten beiden Zeilen ist Rauschen. Auf dem Client lag der Anteil am Render-Thread bei 0,08 %.

**Migration**
- `Version1Converter` (core, getestet) wandelt V1-Resourcepacks um, im Spiel per Drag & Drop mit Dialog (Name, Sprache, Geschlecht). Event- und Berufs-Umbenennungen sind enthalten, die Untertitel werden zu `text`.
- `voices[].legacyIds` ist neu im Manifest, im Schema und im Web-Editor-Import. `LegacyVoices` prüft zuerst die `legacyIds` und dann die Voice-ID.
- Abweichung: Statt eines Toasts beim Start zeigt der Voicepack-Screen den Hinweis „V1-Stimmen gefunden“.

**Default-Pack**
- `./gradlew :core:buildDefaultPack` baut `build/voicepacks/teajay-default-2.0.0.voicepack` (ca. 13 MB). Es enthält die TeaJay-Villager-Stimme (männlich, `legacyIds: ["teajay"]`) und die Hexen-Testaufnahmen.
- Das Pack kommt nicht ins JAR. Stimmen für Frauen, Kinder, Händler und Illager fehlen noch, dafür braucht es neue Aufnahmen.

**Doku und Release**
- README ist neu geschrieben. Neu sind `docs/voicepack-format.md` (Event-Tabelle aus dem `EventCatalog` erzeugt), `docs/creating-voicepacks.md` und `CHANGELOG.md`.
- Die Version ist jetzt `2.0.0-beta.1`. Der Build hängt `+mc26.3` an, die JARs heißen `talkingvillagers-<loader>-<version>.jar`.
- `.github/workflows/publish.yml` (mc-publish für Modrinth, CurseForge und GitHub) läuft bei Tags `v*`. Die Projekt-IDs sind Platzhalter, die Secrets `MODRINTH_TOKEN` und `CURSEFORGE_TOKEN` müssen noch angelegt werden.
- `DownloadClientGameTest` läuft nur mit `TV_TEST_SERVER=localhost:25565` gegen einen laufenden Dev-Server. Er bricht einen Download per Verbindungsabbruch ab und prüft, dass der Cache leer bleibt. Danach lädt er vollständig herunter und prüft, dass das Pack nicht aktiv wird.

**Offen:** Beta auf Modrinth, Testmatrix unten, Aufnahmen für das Default-Pack.

## Ziel
V2 ist veröffentlichungsreif: Es läuft robust auf Servern, alte Packs und Welten lassen sich übernehmen, ein Default-Pack wird mitgeliefert, die Doku ist aktuell, und die Mod ist auf Modrinth und CurseForge veröffentlicht.

## 1. Multiplayer
- [ ] **Pack-Abgleich beim Join:** Der Server schickt `server_packs` (ID, Version, sha256). Der Client vergleicht und stößt bei fehlenden Packs den Download-Dialog an (siehe nächster Punkt).
- [ ] **Pack-Download vom Server** (fester Bestandteil von V2, wegen Option A: Ohne Download hören Spieler ohne die Server-Packs nichts):
  - Ablauf beim Join: Der Server schickt `server_packs`. Der Client meldet per `client_packs`, welche Packs (sha256) ihm fehlen. Der Server bietet den Download an.
  - **Zustimmung des Spielers:** Ein Dialog wie beim Server-Resourcepack: „Dieser Server nutzt 2 Voicepacks (14 MB). Herunterladen?“ mit [Ja] [Nein] [Immer für diesen Server]. Die Entscheidung wird pro Server-Adresse gespeichert.
  - Übertragung in Chunks per Custom Payload (≤ 1 MB pro Paket wegen des Payload-Limits), sha256-Prüfung nach Abschluss. Der Download läuft im Hintergrund, Stimmen werden aktiv, sobald ein Pack fertig ist (Reload der Client-Ressourcen).
  - Bandbreitenlimit pro Spieler und gesamt (Server-Config `packDownload.maxKbpsPerPlayer`, `packDownload.maxConcurrent`), damit der Join anderer Spieler nicht leidet.
  - Ablage in `voicepacks/.server-cache/<sha256>.voicepack`: Die Packs sind nur auf diesem Server aktiv und erscheinen im Voicepack-Screen in einem Abschnitt „Vom Server“. Der Button „Dauerhaft installieren“ kopiert sie nach `voicepacks/`. Cache-Größe begrenzt (LRU, Default 500 MB).
  - Server-Config `packDownload.enabled` (Default **an**). Optional `packDownload.url`: Statt der Übertragung über die Spielverbindung lädt der Client von einer HTTP(S)-URL (z. B. Modrinth/CDN), ebenfalls mit sha256-Prüfung. Das ist schneller und schont den Server.
  - Sicherheit: Nur `.voicepack`-Inhalte (Manifest und Ogg-Dateien) werden akzeptiert. Der `PackValidator` läuft vor der Aktivierung, Zip-Slip-Schutz, Größenlimit pro Pack (Default 100 MB).
  - Tests: Join mit fehlendem Pack → Dialog → Download → Stimmen hörbar. Abbruch und Verbindungsabbruch mitten im Download → kein kaputtes Pack im Cache.
- [ ] **Client ohne Mod** auf Server mit Mod: Vanilla-Sounds, kein Fehler. Testen.
- [ ] **Server ohne Mod** mit Client mit Mod: keine Stimmen, Vanilla-Sounds, keine Fehlermeldung. Ein einmaliger Hinweis im Voicepack-Screen erklärt: „Dieser Server hat Talking Villagers nicht installiert“. Das ist gewollt (Entscheidung: Option A, Server ist maßgeblich).
- [ ] Doku/FAQ für Server-Admins: Mod **und** Voicepacks gehören auf den Server. Clients ohne diese Packs bekommen sie beim Join automatisch zum Download angeboten.
- [ ] Performance: Tick-Hooks laufen nur, wenn Spieler im Umkreis sind (`level.hasNearbyAlivePlayer`). Messung mit Spark bei 200 Villagern.

## 2. Migration von V1
- [ ] **V1-Packs konvertieren** (in der Mod, aus Phase 7 aufgerufen): V1-Resourcepack-ZIP (+ `voicepack.json` mit `voicepack_id`) einlesen:
  - `assets/talkingvillagers/sounds/voices/<voice>/<profession|all>/<event>/*.ogg` → V2-Struktur
  - Profession-Ordner → `conditions.professions`
  - Event-Umbenennungen: `nope` → `no`, `level` → `level_up`, `herodrop` → `hero_gift`, `tradesuccess` → `trade_success`
  - `lang/*.json` → `text`
  - Sprache, Geschlecht und Kind sind unbekannt. Ein Dialog fragt sie ab (Defaults: `en`, `neutral`, `false`).
  - `duration`: Die Mod kann sie auf dem Client mit dem Vanilla-Ogg-Decoder ermitteln.
- [ ] **Welten:** Legacy-NBT `VoiceName`/`VoicePitch` → `VoiceIdentity` (siehe Phase 3). Zuordnung V1-`VoiceName` → V2-`VoiceRef` über eine `legacyIds`-Liste im Manifest (optionales Feld `voices[].legacyIds`).
- [ ] Die alte Config `config/talkingvillagers/voices/*.json` wird beim ersten Start erkannt. Es gibt einen Log-Hinweis und einen einmaligen Toast „V1-Stimmen gefunden, im Voicepack-Screen konvertieren“.

## 3. Default-Pack
- [ ] Die vorhandenen Aufnahmen (`packs-src/teajay-default/`) werden mit dem Web-Editor oder per Gradle-Task (`buildDefaultPack`) zu `teajay-default.voicepack` gebaut. Metadaten: Sprache, Geschlecht, Kind ergänzen.
- [ ] Ausliefern: **nicht** ins Mod-JAR (Größe!). Separater Download auf Modrinth als „Voicepack“-Projekt. Die Mod enthält einen Button „Standard-Stimmen herunterladen“, der auf die Modrinth-Seite verlinkt, oder optional einen direkten Download mit Zustimmung.
  - Alternative: ein kleines Mini-Pack (1 Stimme, wenige Zeilen) im JAR als Demo, damit nach der Installation sofort etwas passiert. Diese Entscheidung ist offen.
- [ ] Mindestens Stimmen für: Villager ♂/♀, Kind, Witch, Wandering Trader, Illager. Für die neuen Entities müssen ggf. neue Aufnahmen gemacht werden. Das ist der größte nicht-technische Aufwand.

## 4. Doku
- [ ] README neu: Installation (Mod + Pack per Drag & Drop), Features, Config, Commands, FAQ (Multiplayer!)
- [ ] `docs/voicepack-format.md` (aus dem Schema erzeugt) und `docs/creating-voicepacks.md` (Web-Editor-Anleitung mit Screenshots, Aufnahme-Guidelines aus V1 übernommen und erweitert: Mono, Kinderstimmen, Geschlecht, Sprache)
- [ ] CHANGELOG.md
- [ ] Übersetzungen: `en_us.json`, `de_de.json` für alle UI-Texte und Command-Meldungen

## 5. Release
- [ ] Versionsschema `2.0.0+mc26.x`, Pre-Releases `2.0.0-beta.N`
- [ ] Beta-Phase: Pre-Release auf Modrinth, 2–3 Wochen Feedback, Discord/GitHub-Issues
- [ ] Publishing-Automatisierung: `mc-publish` GitHub Action (Modrinth + CurseForge + GitHub Release) auf Tag `v*`
- [ ] Testmatrix vor dem Release (manuell, Checkliste):
  - Singleplayer neu / V1-Welt
  - Dedicated Server + 2 Clients (einer ohne Packs, einer ohne Mod)
  - mit/ohne Mod Menu
  - Kompatibilität mit Sodium, Iris, MCA-Reborn (Konflikt mit Villager-Mixins?), Guard Villagers

## Backlog (nach 2.0)
- Reputation-basierte Zeilen (TODO aus V1: `// ToDo Reputation based voice lines`)
- Weitere Mobs: Piglin, Iron Golem (Brummen?), Allay
- Gespräche: Zwei Villager antworten sich (Event-Paare `gossip_question`/`gossip_answer`)
- NeoForge-Port über das `core`-Modul
- Pack-Galerie im Web-Editor
