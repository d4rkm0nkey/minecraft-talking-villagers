# Release-Plan nach dem Review vom 19.10.2026

Grundlage ist ein Code-Review von Core, Server-Code, Client/GUI und Web-Editor auf dem Stand `2.0.0-beta.1`. Die Funde sind auf mehrere Updates verteilt. Jedes Update bekommt einen eigenen Branch (`fix/beta-N`), einen CHANGELOG-Eintrag und zu jedem Bug einen Regressionstest, wo das ohne Spiel möglich ist.

| Update | Inhalt | Branch |
|---|---|---|
| **2.0.0-beta.2** | Kritisch: Sicherheit, Kicks, Sleep-Line, Export abgelehnter Packs | `fix/beta-2` |
| **2.0.0-beta.3** | Verhalten im Spiel: Lines und Stimmen, die fehlen oder falsch sind | `fix/beta-3` |
| **2.0.0-beta.4** | Robustheit bei Downloads und Packs | `fix/beta-4` |
| **2.0.0-beta.5** | Performance und Bedienung | `fix/beta-5` |
| **Web-Editor (parallel)** | Datensicherheit, danach Audioqualität | `fix/editor-data`, `fix/editor-audio` |
| **2.0.0** | Testlücken schließen, Release | `release/2.0.0` |

Der Web-Editor wird über GitHub Pages ausgeliefert und hängt nicht an der Mod-Version. Nur die Format-Änderungen aus beta.2 müssen gleichzeitig in Mod und Editor landen.

---

## 2.0.0-beta.2: kritische Fehler

**Status:** umgesetzt in `fix/beta-2`. Abweichungen vom Plan:
- K1 nur am Bytecode geprüft (`TAIL` = `return false` bei Offset 122). Die Dev-Packs haben keine `sleep`-Lines, und im Test hat kein Dorfbewohner ein Bett beansprucht.
- K2: Die Hash-Prüfung (`LoadedPack.isSha256`) steht in `PackDownloads.canDownload` und vor dem Anlegen der `.part`-Datei, nicht in den Codecs. So führt ein ungültiger Hash nicht zum Verbindungsabbruch.
- K4: `ServerVoices` schickt höchstens `MAX_PACKS` Packs und lässt eine zu lange Download-URL weg.
- K5: Lines merken sich ihre Länge (`Line.length`). Für ältere Projekte prüft zusätzlich der Export und bricht mit einer übersetzten Meldung ab.

### K1 Sleep-Line spielt nie
- `common/.../mixin/LivingEntityMixin.java:17`: `@At("TAIL")` trifft in `startSleeping` nur das letzte `return false`. Auf `@At("RETURN")` umstellen.
- Prüfen: Dorfbewohner nachts per RCON/`/tv debug watch` beobachten, `sleep` muss im Log erscheinen.

### K2 Pfadangriff über `sha256` vom Server
- `client/PackDownloads.java:151` baut den `.part`-Pfad aus dem vom Server gesendeten Hash.
- `PackInfo` (`network/ServerPacksPayload.java`) und `RequestPackPayload`/`PackChunkPayload`: Hash nur akzeptieren, wenn er `^[0-9a-f]{64}$` entspricht. Packs mit ungültigem Hash in `onServerPacks` verwerfen und loggen, keine Exception im Handler.
- Zusätzlich in `PackDownloads`: `part.normalize().startsWith(cache)` prüfen.
- Test: Unit-Test für die Hash-Prüfung (Hilfsmethode, z. B. in `core/util`).

### K3 Pfadangriff über `version` beim Installieren
- `core/pack/PackRepository.java:81` (`installCached`) baut den Dateinamen aus `id` und `version`.
- Schema und `ManifestCodec`: `version` bekommt ein Pattern, z. B. `^[0-9A-Za-z.+-]{1,32}$`.
- `installCached`: Ziel normalisieren und `startsWith(directory)` prüfen.
- **Web-Editor gleichzeitig anpassen**: das gleiche Pattern prüfen, sonst exportiert er Packs, die die Mod jetzt ablehnt. Default pack und `schema/examples` gegen das Pattern prüfen.
- Test: `PackRepositoryTest` mit `version = "x/../../evil"`, `ManifestCodecTest` mit ungültigen Versionen.

### K4 Lange Namen kicken alle Spieler mit Mod
- `ServerPacksPayload` begrenzt name (256), version (64) und url (2048). Der `ManifestCodec` prüft keine Länge, also gibt es beim Senden eine EncoderException.
- `ManifestCodec`: die `maxLength`-Werte aus dem Schema durchsetzen (Pack- und Voice-`name` 64, `version` 32, `description` 1024, Line-`text` 256). Dazu `minLength 1` für `authors[]` und `legacyIds[]`.
- Absicherung in `ServerVoices.info()`: Werte, die das Paket nicht fassen würde (auch eine konfigurierte URL), kürzen bzw. weglassen und warnen, statt die Verbindung zu riskieren.
- Web-Editor: dieselben Längen prüfen und leere Namen aus Leerzeichen ablehnen (Java nutzt `isBlank()`).
- Test: `ManifestCodecTest` für jede Grenze. Ein Test, dass ein `PackInfo` aus einem gültigen Manifest immer kodierbar ist.

### K5 Web-Editor exportiert Packs, die die Mod ablehnt (Dauer)
- Die Mod verlangt `0 < duration ≤ 60`, sonst ist das ganze Pack ungültig.
- Web-Editor: die Länge nach dem Zuschneiden am `Line` speichern (Aufnahme und Import). `check()` meldet Lines über 60 s als Fehler. `buildManifest` lässt Dauern unter 0,01 s weg.
- Test: `manifest.test.ts`/`export.test.ts` mit 75-s- und 3-ms-Clips.

### Abschluss beta.2
- `./gradlew build` sowie `npm test` und `svelte-check` im Web-Editor.
- Fabric- und NeoForge-Server starten. Per RCON prüfen, dass die Packs laden und Sprechen sowie Download funktionieren.
- `gradle.properties` auf `2.0.0-beta.2`, CHANGELOG-Abschnitt „Fixed“ und „Security“.

---

## 2.0.0-beta.3: Verhalten im Spiel

**Status:** umgesetzt in `fix/beta-3`. Abweichungen vom Plan:
- NeoForge: Das Problem ist im Quelltext bestätigt (`RegistryManager`: Einträge, die der Client nicht kennt, führen „garantiert“ zum Disconnect). Das Attachment ist nicht mehr synchronisiert. Stattdessen gibt es `VoiceIdentityPayload` beim Setzen und bei `PlayerEvent.StartTracking`. Getestet mit einem NeoForge-Client ohne Mod (eine vorübergehende Run-Konfiguration mit `loadedMods = []`): Auf `master` wird er getrennt („The server sent registries with unknown keys: neoforge:synced_attachment_types / talkingvillagers:voice“), mit beta.3 kann er joinen. Ein Client mit Mod bekommt die Stimmen beim Tracking-Start und nach `/tv voice set`.
- Umwandlung: Von Hand gesetzte Stimmen (`manual`) bleiben auch dann, wenn sie nicht zur neuen Entity-Art passen, weil `/tv voice set … force` das absichtlich erlaubt. Getestet per RCON auf Fabric und NeoForge (Blitz → Hexe, Zombie → Heilung über `ConversionTime`).
- CURED: wird nach 20 Ticks gesagt, sobald der Dorfbewohner eine Stimme hat (höchstens 35 s Wartezeit für die Dorfsprache). Ohne Spieler nicht testbar, weil Vanilla das Event nur mit einem heilenden Spieler auslöst.
- `/tv voice get` bleibt unverändert: Die ausgelöste Zuweisung passiert ohnehin, sobald ein Spieler in der Nähe ist. Neu ist nur, dass Entities ohne Stimme (Zombie-Dorfbewohner) keine geerbten Stimmdaten mehr zeigen.
- Neu gefunden (für beta.4): `/tv voice set` übernimmt nicht das Geschlecht der Stimme, die Anzeige lautet dann z. B. „Man – female“.

- **CURED wird nicht gehört** (`VillagerMixin.java:70`): Das Event kommt, bevor der Dorfbewohner in der Welt ist. Vormerken und beim ersten Tick bzw. einige Ticks später sprechen.
- **Stimme geht bei der Zombie-Umwandlung verloren**: `copyOnDeath()` auf Fabric und NeoForge setzen. Die Stimme neu zuweisen, wenn sie die neue Entity-Art nicht abdeckt (Hexe), Geschlecht und Sprache bleiben. `ClientVoices.isVoiced` und der Server ignorieren Stimmdaten auf Entities ohne Stimme (Zombie-Dorfbewohner).
- **NeoForge: Mod auf dem Client Pflicht?** Erst mit einem NeoForge-Client ohne Mod testen. Falls bestätigt: das Attachment nicht per `.sync` synchronisieren, die Felder stattdessen über ein eigenes optionales Payload senden.
- **Wiederholungssperre bei Eltern-Events** (`LineSelector.pick`, `SpeechService`): den Avoid-Index pro probiertem Event nachschlagen (`Function<String, OptionalInt>`). Test in `LineSelectorTest`.
- **HostileChatter** (`HostileChatter.java:360-378`): erst den Kampfbeginn und das Kampfende verarbeiten, dann Low-Health mit Vorrang zurückgeben. Test in `HostileChatterTest`.
- **Spawn-Ei löst LOVE aus** (`VillagerMixin.java:46`): bei `partner == this` überspringen.
- **Doppelte Todesreaktionen** (`LivingEntityMixin`, `die` HEAD): abbrechen, wenn die Entity schon tot oder entfernt ist.
- **`/tv voice get` / `/tv gender get` ohne Rechte weisen Stimmen zu**: den Lesepfad ohne Seiteneffekte machen.
- **`SpeechWatchers`** beim Logout und beim Server-Stopp aufräumen.
- **Wetter**: Entscheiden, ob `rain` auch bei Gewitter gilt, und das in `LineContext` und in der Doku festhalten.

---

## 2.0.0-beta.4: Robustheit bei Downloads und Packs

**Status:** umgesetzt in `fix/beta-4`. Abweichungen vom Plan:
- Priorisiert nach dem, was Spieler merken. Die Punkte unter **Core** sind nach 2.0.0 verschoben (siehe dort). Aus beta.5 vorgezogen: `NeoForgePlatformNetworking` ohne Client-Import (Setter wie bei Fabric). Neu: `/tv voice set` übernimmt das Geschlecht der Stimme (bei beta.3 gefunden).
- Ablehnung durch den Server: kein neues Payload, sondern ein leerer `PackChunkPayload` mit `total = -1`. Clients von beta.3 sehen eine falsche Größe und brechen ebenfalls ab, das Protokoll bleibt kompatibel.
- Stillstand: 30 s ohne Daten. Über die Spielverbindung zählt jeder Chunk für alle wartenden Downloads, weil der Server die Packs nacheinander schickt. Danach fragt der Download-Dialog erneut, auch bei vertrauten Servern, damit ein kaputter Server keine Endlosschleife auslöst. Der Server beantwortet höchstens 3 Anfragen pro Pack und Spieler.
- HTTP: ein gemeinsamer `HttpClient`, die Antwort wird auf eigenen Threads gelesen und gezählt. Abbruch per Interrupt. HTTP-Downloads zeigen jetzt auch Prozent.
- Icons: Fehlschläge bleiben bis zum nächsten Reload gecacht, sonst würde das Icon jeden Frame neu gelesen und geloggt. Beim Reload werden Fehlschläge vergessen und Texturen entfernter Packs freigegeben.
- Sound-Cache: Fehlgeschlagene Einträge werden beim nächsten Abspielen ersetzt statt per Callback entfernt (der Cache ist eine `HashMap` des Client-Threads).
- Fremde Dateien gleichen Namens bekommen nicht den Vorrang: die neue Datei erhält eine Nummer (`SafeFiles.freeName`).
- Getestet: `/tv voice set` per RCON auf NeoForge (Geschlecht wird übernommen), Dedicated Server auf NeoForge startet. `DownloadClientGameTest` gegen den Fabric-Dev-Server: abgebrochener Download hinterlässt nichts, vollständiger Download landet im Cache, Accessoren und Sound-Cache-Mixin laden. Stillstand-Timeout und HTTP-Download nicht im Spiel getestet.

**Client-Downloads** (`PackDownloads`):
- URL in `canDownload` validieren, `startHttp` mit try/catch absichern, im Fehlerfall `fail()`.
- Timeout bei fehlendem Fortschritt (z. B. 30 s ohne Chunk) statt endlosem „Downloading 0 %“. Danach erneut anbieten.
- Der Server antwortet ausdrücklich mit einer Ablehnung, wenn er ein Pack nicht senden kann.
- HTTP: Bytes zählen und bei mehr als `min(size, MAX_PACK_BYTES)` abbrechen. Ein zu großes `Content-Length` ablehnen. Timeout bei fehlendem Fortschritt statt fester 10 Minuten.
- Pro Download einen `HttpClient` vermeiden bzw. schließen. Übrig gebliebene `.part`-Dateien beim Start löschen.

**Cache:**
- `pruneCache` lässt Packs des aktuellen Servers und `.part`-Dateien in Ruhe. Die mtime bei Benutzung aktualisieren. Fehler pro Datei behandeln.

**Größenlimits:**
- `PackRepository.readFile`, `Version1Converter` und `PackIcons` lesen höchstens N MiB (`readNBytes(limit + 1)`).
- Icons: Größe und Abmessungen aus dem PNG-Header prüfen, bevor dekodiert wird.
- Texturen gelöschter Packs freigeben, Fehlschläge nicht cachen.

**Sound-Cache** (`SoundBufferLibraryMixin`):
- Fehlgeschlagene Futures entfernen. Nach einem Reload Einträge löschen, deren Hash nicht mehr im Index ist.

**Server-Transfers** (`PackTransfers`):
- Logs bei ungültigen Anfragen drosseln. Wiederholte Anfragen pro Spieler und Session ablehnen.
- Transfers inaktiver Packs bei einem Reload abbrechen.

**Überschreiben beim Konvertieren und Installieren:**
- In eine Temp-Datei schreiben und atomar verschieben.
- Fremde Dateien gleichen Namens nicht stillschweigend ersetzen.

**Core:**
- `Versions.compare` wird transitiv: Zahlen stehen vor Text wie bei semver, ein führendes `v` wird ignoriert. Test für die Konsistenz der Ordnung.
- `ManifestCodec.integer()` prüft auf Überlauf.
- `PackIndex` behält die Reihenfolge (`LinkedHashMap` statt `Map.copyOf`).
- `PackReader` meldet `MANIFEST_TOO_LARGE` statt `INVALID_JSON`.
- `Version1Converter`: Kollisionen bei Ordner- und Dateinamen auflösen (`_2`), auf 64 Zeichen kürzen, Beruf klein schreiben. Fehlerhafte `en_us.json` tolerieren.
- `OggInfo`: Page-Header beim Ermitteln der Dauer genauer prüfen.

---

## 2.0.0-beta.5: Performance, Bedienung und Feedback-Link

Ziel: Die Mod ruckelt nicht mehr bei Reloads und beim Installieren, die Einstellungen sind verständlich, und Spieler finden die Umfrage. Mit beta.5 startet die Spielerumfrage (`docs/player-survey.md` auf `docs/player-survey`): 3 Wochen ab Release, danach Auswertung vor 2.0.0.

### 1. Feedback-Link (Umfrage)

Adresse: `https://tally.so/r/zExyRM`. Nur ein Link, der nach Bestätigung den Browser öffnet; die Mod sendet nichts.

- **Voicepack-Screen:** Die Fußleiste ist mit 5 Buttons voll (5 × 76 px). Stattdessen eine Zeile ganz oben in der Liste: „Talking Villagers 2 is a beta – tell us what you think!“ mit Button *Feedback* (`ConfirmLinkScreen.confirmLink`, wie beim Web-Editor). Nur solange die Mod-Version `-beta` enthält, damit der Link mit 2.0.0 von selbst verschwindet.
- **Einstellungen:** dieselbe Zeile unten im `SettingsScreen` (für Leute, die über Mod Menu direkt dort landen).
- Konstante `FEEDBACK_URL` neben `WEB_EDITOR`. Übersetzungen `talkingvillagers.gui.packs.feedback*` in `en_us.json` und `de_de.json`.
- **Außerhalb der Mod:** Changelog von beta.5 (erste Zeile), README unter „Download“, Wiki `pages/docs/beta.md` (neuer Abschnitt „Feedback“ neben „Reporting bugs“). Modrinth- und CurseForge-Beschreibung und die GitHub Discussion macht der Autor von Hand.
- Test: Screenshot im `GuiClientGameTest` (Zeile sichtbar, Button öffnet den Bestätigungsdialog, nicht den Browser).

### 2. Reloads und Installieren ohne Ruckler

Heute liest `Voicepacks.reload()` jede Datei komplett und berechnet SHA-256, auf dem Thread, der es aufruft (Render-Thread in der GUI, Server-Thread bei `/tv reload`). Bei großen Packs friert das Spiel ein.

- **Cache in `PackRepository`:** `LoadedPack` nach Pfad, Größe und mtime merken. Unveränderte Dateien werden nicht neu gelesen und gehasht. Neue Tests: unveränderte Datei → dasselbe Objekt, geänderte mtime oder Größe → neu gelesen, gelöschte Datei fällt aus dem Cache.
- **`Voicepacks.reloadAsync()`:** lädt auf einem eigenen Worker und gibt ein `CompletableFuture<PackIndex>` zurück. Gleichzeitige Aufrufe teilen sich einen laufenden Reload. Die Reload-Listener springen schon heute selbst in den richtigen Thread (`execute`), das bleibt so.
- **GUI:** *Reload*, Löschen, *Install permanently*, Installieren und Konvertieren nutzen `reloadAsync()`. Während des Ladens zeigt der Screen „Loading voicepacks…“ in der Kopfzeile und sperrt die Buttons, die Packs ändern. `PackInstaller.install` prüft die abgelegten Dateien (`PackReader.read`) ebenfalls auf dem Worker und öffnet den Bestätigungsdialog danach.
- **`/tv reload`:** lädt asynchron, die Meldung kommt per `server.execute`, wenn der Reload fertig ist.
- **Download:** `onChunk` schreibt über den Download-Worker statt auf dem Render-Thread. Der Worker ist ein einzelner Thread, deshalb bleibt die Reihenfolge der Chunks erhalten und `finish` läuft automatisch nach dem letzten Chunk.

### 3. Bedienung

- **Liste springt nicht mehr nach oben:** `VoicepacksScreen.refresh()` merkt sich die Scroll-Position wie `ListScreen.refresh()`. Während Downloads laufen, nur die Fortschrittszeilen aktualisieren statt alle Zeilen neu zu bauen.
- **Lautstärke:** Der Regler geht heute bis 200 %. Minecraft spielt nie lauter als 100 %, höhere Werte vergrößern nur die Hörweite, das versteht niemand. Regler auf 0–100 % begrenzen, `volume` im Codec auf 0–1, ältere Werte über 1 werden beim Laden auf 1 gesetzt (statt die Datei als ungültig zu verwerfen). Wiki `configuration.md` anpassen.
- **Vertraute Server in den Einstellungen:** Abschnitt „Trusted servers“ im `SettingsScreen` mit einer Zeile pro Server und Button *Remove*. Leer: Hinweis, wie ein Server dazukommt („Always download from this server“).
- **Übersetzungen:** Die Gründe für Download-Fehler (`fail(…)` in `PackDownloads`, heute englischer Text) werden Übersetzungsschlüssel mit Parametern. Größenangaben über eine kleine Hilfsfunktion mit übersetzbaren Einheiten (`talkingvillagers.unit.kib/mib`). Deutsche Texte in `de_de.json`.

### 4. Server-Performance

- **`EntityVoices.crowded()`:** Heute laufen alle `SpeakerState`s der Welt durch, bei jedem Satz. Stattdessen `level.getEntitiesOfClass(LivingEntity.class, box um CROWD_RADIUS, …)` und nur deren Zustand prüfen. Vorher und nachher mit JFR und `/tick query` in einem Dorf mit Raid vergleichen (siehe Notizen zur Performance, kein spark).

### Reihenfolge und Tests

1. Feedback-Link (klein, damit er sicher drin ist).
2. Cache und `reloadAsync()` in Core und `Voicepacks`, mit Unit-Tests.
3. GUI, `/tv reload` und `onChunk` auf den neuen Reload umstellen.
4. Bedienung (Scroll, Lautstärke, vertraute Server, Übersetzungen).
5. `crowded()` mit Messung.
6. Test: `./gradlew build`, RCON (`/tv reload` mit Meldung, Sprechen im Raid), Fabric-Client-Gametests inklusive `DownloadClientGameTest`, Screenshots der Screens prüfen. NeoForge-Server starten.

---

## Web-Editor (parallel zu beta.3–beta.5)

### Editor-Update A: Datensicherheit
- **Mehrere Tabs**: Den Projektindex atomar aktualisieren (`update()`). Andere Tabs per `BroadcastChannel` warnen oder Bearbeitung nur in einem Tab erlauben (`navigator.locks`).
- **`addFiles`**: nach jeder Datei speichern, Fehler pro Datei anzeigen.
- **Stimme löschen** löscht auch deren Audio. Beim Start verwaiste `audio:*`-Einträge aufräumen.
- **Import**: Manifest gegen das Schema validieren, übersetzte Fehlermeldungen anzeigen, fehlende Dateien melden.
- **`weight` und `legacyIds`** im Modell, beim Import und beim Export mitnehmen. Der v1-Import setzt `legacyIds: [voiceName]`.
- **Unbekannte Events** beim v1-Import als Warnung pro Line statt eines allgemeinen Exportfehlers.
- **Export-Worker**: `onerror` behandeln, beim Verlassen beenden.

### Editor-Update B: Audioqualität und Stabilität
- **Resampler**: Windowed-Sinc statt Box-Filter, bei Verhältnis ≈ 1 ohne Filter.
- **Rauschprofil** auf die Abtastrate der Line umrechnen.
- **Long Take**: laufender Sample-Zähler statt alle 100 ms den ganzen Puffer zu kopieren.
- **Countdown** stoppen, wenn die Phase von Hand gewechselt wird.
- **Mikrofon** schließen, wenn das Studio während `getUserMedia` verlassen wird. Worklet-Nodes beenden.
- **Speicher und CPU**: Audio-Cache begrenzen, Vorbis-WASM einmal pro Export laden, Pegel-Updates drosseln.
- **Eingangskanäle**: alle zu Mono mischen oder den Kanal wählen lassen.

---

## 2.0.0: Testlücken und Release

- Aus beta.4 verschoben (Core): `Versions.compare` transitiv, Überlauf in `ManifestCodec.integer()`, Reihenfolge in `PackIndex`, `MANIFEST_TOO_LARGE`, Namenskollisionen und fehlerhafte `en_us.json` im `Version1Converter`, Page-Header in `OggInfo`.
- Neue Tests für `PackIndex`, `PackValidator`, `WeightedRandom`, `LineContext`, `ZipPackContents`, `EventPolicies` und `Selection.samplePitch`.
- Der Fabric-Client-Gametest deckt Download, GUI und Sprechen nach allen Fixes ab.
- In der Doku festhalten, dass unbekannte Events und Felder in der Mod nur Warnungen sind, während das Schema sie ablehnt.
