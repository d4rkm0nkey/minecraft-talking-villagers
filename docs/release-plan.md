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

## 2.0.0-beta.5: Performance und Bedienung

- **Reloads und Hashing nicht im Spiel-Thread**: in der GUI (`VoicepacksScreen`, `PackInstaller`, `ConvertScreen`) und bei `/tv reload` auf einem Worker laden, das Ergebnis per `execute` übernehmen. Hashes nach Pfad, Größe und mtime cachen.
- **Voicepack-Liste springt nach oben**: Scroll-Position bei `refresh()` behalten oder nur den Fortschrittstext aktualisieren.
- **Lautstärke über 100 %**: auf 0–1 begrenzen oder in der UI als Hörweite bezeichnen.
- **Vertraute Server** in den Einstellungen anzeigen und entfernen.
- **Übersetzungen**: Download-Fehler und die Einheiten MB/KB übersetzbar machen.
- **`EntityVoices.crowded()`**: lokal mit `getEntitiesOfClass` suchen statt alle `SpeakerState`s zu durchlaufen.
- **Chunk-Schreiben** in `onChunk` auf den Worker-Thread verlegen.
- **`NeoForgePlatformNetworking`**: keinen Import der Client-Klasse in Code, den der Server lädt (Setter wie bei Fabric).

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

- Neue Tests für `PackIndex`, `PackValidator`, `WeightedRandom`, `LineContext`, `ZipPackContents`, `EventPolicies` und `Selection.samplePitch`.
- Der Fabric-Client-Gametest deckt Download, GUI und Sprechen nach allen Fixes ab.
- In der Doku festhalten, dass unbekannte Events und Felder in der Mod nur Warnungen sind, während das Schema sie ablehnt.
