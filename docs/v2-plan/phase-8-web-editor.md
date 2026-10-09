# Phase 8 – Web-Editor zum Erstellen von Voicepacks

## Ziel
Eine einfache, rein clientseitige Web-App (kein Backend), mit der jeder ohne Python-Skripte und ohne Ordnerstruktur ein `.voicepack` erstellen oder bearbeiten kann. Gehostet auf GitHub Pages.

## Tech-Stack (Empfehlung)
| Thema | Wahl | Grund |
|---|---|---|
| Framework | **Svelte 5 + Vite + TypeScript** | klein, schnell, wenig Boilerplate. Alternativ React. |
| ZIP | `fflate` | schnell, kein Worker-Zwang |
| Audio-Konvertierung → Ogg Vorbis | `wasm-media-encoders` (Vorbis-Encoder als WASM). Fallback ist `ffmpeg.wasm`, das aber größer ist. | Minecraft verlangt Ogg **Vorbis**. Browser-Aufnahmen liefern Opus/WebM. |
| Dekodieren / Trimmen | Web Audio API (`decodeAudioData`, `OfflineAudioContext`) | nativ |
| Waveform | `wavesurfer.js` | Trim-UI |
| Rauschentfernung | RNNoise als WASM (`@shiguredo/rnnoise-wasm`) + eigene Spectral-Gating-Implementierung im Worker | Sprachoptimiert, läuft offline im Browser |
| Aufnahme | `getUserMedia` + `AudioWorklet` (rohes PCM statt `MediaRecorder`) | verlustfrei, kein Opus-Zwischenschritt, Echtzeit-Vorschau möglich |
| Validierung | `ajv` mit `schema/voicepack.schema.json` | dasselbe Schema wie die Mod |
| Speicher | IndexedDB (`idb-keyval`) | Projekt überlebt einen Tab-Reload |

Ordner: `web-editor/` im selben Repo. Das Schema wird per Build-Skript aus `schema/` importiert.

## Funktionen

### Projekt
- Neues Pack: ID (wird aus dem Namen erzeugt), Name, Version, Beschreibung, Autoren, **Sprache** (Dropdown mit Suche, BCP-47), Icon-Upload (wird auf 128×128 skaliert).
- Bestehendes `.voicepack` öffnen und weiterbearbeiten.
- **V1-Import:** altes Resourcepack-ZIP plus optional V1-`<voice>.json` → V2-Projekt. Berufs-Ordner werden zu `conditions.professions`, Transkripte kommen aus `lang/en_us.json`. Das ist der zweite Migrationsweg neben der Mod (Phase 9).
- Automatisches Speichern in IndexedDB. Die Projektliste steht auf der Startseite.

### Stimmen
- Stimme anlegen: Name, **Entity-Typen** (Checkboxen: Villager, Wandering Trader, Witch, Pillager, Vindicator, Evoker, Illusioner, „alle Illager“), **Geschlecht** (♂/♀/neutral), **Kinderstimme** (Toggle), optional abweichende Sprache, Pitch-Bereich.
- Die Event-Liste passt sich den gewählten Entities an (aus dem Event-Katalog im Schema). Jedes Event zeigt einen Kurztext, was es auslöst, und eine Empfehlung für die Anzahl der Varianten (Guidelines aus der V1-README).
- Fortschrittsanzeige pro Stimme, z. B. „14/18 Events haben Zeilen, `hurt` hat nur 1 Variante (empfohlen ≥ 3)“.

### Aufnahme-Studio (Schwerpunkt: schnell viele Zeilen aufnehmen)

Ziel: Viele Zeilen hintereinander aufnehmen, ohne Maus und ohne Nachbearbeitung in Audacity.

**Skript-Modus (Teleprompter)**
- Vor der Session wird eine Liste der aufzunehmenden Zeilen erstellt: Event plus Text, z. B. „greeting: Hallo, Reisender!“. Eingabe per Tippen oder Einfügen (eine Zeile pro Zeile, Präfix `event:`). Vorlagen pro Event aus den V1-Guidelines stehen als Vorschläge bereit.
- Für Events ohne Text (`hurt`, `death`, `ambient`) gibt es Platzhalter wie „Schmerzlaut 1/5“.
- Großer Teleprompter-Text der aktuellen Zeile, darunter die nächste Zeile in klein.

**Tastatur-Flow**
| Taste | Aktion |
|---|---|
| `Leertaste` | Aufnahme starten/stoppen |
| `Enter` | Take übernehmen → nächste Zeile, Aufnahme startet automatisch nach kurzem Countdown (abschaltbar) |
| `R` | Take verwerfen und neu aufnehmen |
| `P` | Letzten Take abspielen |
| `←`/`→` | Zeile zurück/vor |
| `+` | Weitere Variante derselben Zeile aufnehmen |

- Optionaler **Auto-Stopp per Stille-Erkennung (VAD)**: Nach ~0,8 s Stille stoppt die Aufnahme und springt zur nächsten Zeile. So läuft eine Session freihändig.
- **Langer Take mit Auto-Split:** alternativ eine einzige lange Aufnahme mit Pausen zwischen den Zeilen. Der Editor schneidet an den Stillen in einzelne Zeilen und ordnet sie der Skript-Liste der Reihe nach zu. Die Zuordnung lässt sich danach korrigieren.
- Auswahl des Mikrofons (`enumerateDevices`) und Pegelanzeige mit Clipping-Warnung (rot bei > −1 dBFS). Die Zeile wird markiert, wenn ein Take übersteuert ist.

**Rauschentfernung**
- Die Aufnahme erfolgt **roh**: `getUserMedia` mit `noiseSuppression`, `echoCancellation` und `autoGainControl` = `false`. Die Browser-Filter sind je nach Browser unterschiedlich gut und verfälschen die Stimme. Rauschentfernung ist ein eigener, **nicht-destruktiver** Bearbeitungsschritt, das Original bleibt gespeichert.
- **Stufe 1 – RNNoise (WASM, z. B. `@shiguredo/rnnoise-wasm`):** neuronale Rauschunterdrückung für Sprache, läuft in Echtzeit im `AudioWorklet`. Damit ist sie schon während der Aufnahme als Vorschau hörbar. Default: an.
- **Stufe 2 – Rauschprofil / Spectral Gating:** Zu Beginn der Session nimmt man „2 Sekunden Raumstille“ auf. Daraus entsteht ein Rauschprofil, das per Spectral Subtraction (FFT im Worker) aus allen Takes entfernt wird. Gut gegen gleichmäßiges Brummen und Lüfter. Stärke einstellbar (Regler 0–100 %).
- **Noise Gate** zwischen den Wörtern (Schwelle aus dem Rauschprofil abgeleitet).
- Vorher/Nachher-Umschalter pro Take. Die Einstellungen gelten für die ganze Session und lassen sich nachträglich auf alle Takes neu anwenden (Batch).
- Zusätzlich optional: Hochpass 80 Hz (Trittschall/Brummen) und leichte Kompression, damit die Lautstärke zwischen den Zeilen gleich bleibt.

**Pipeline pro Take** (automatisch, im Worker):
`Roh-PCM → Hochpass → Rauschentfernung → Trim (Stille vorn/hinten) → Normalisieren (−1 dBFS Peak bzw. Loudness-Ziel) → Mono → Vorschau`
Kodiert wird erst beim Export. Bis dahin liegt alles verlustfrei als PCM/FLAC in IndexedDB.

### Zeilen (Audio)
- **Upload** per Drag & Drop: WAV, MP3, OGG, FLAC, M4A, mehrere Dateien gleichzeitig. Hochgeladene Dateien durchlaufen auf Wunsch dieselbe Pipeline (Rauschentfernung usw.).
- Pro Zeile:
  - Waveform mit **Trim** (Stille am Anfang und Ende automatisch erkennen und vorschlagen)
  - **Normalisieren** (Peak −1 dBFS), **Mono-Downmix**. Mono ist Pflicht, sonst ist der Sound in Minecraft nicht positionsabhängig.
  - Transkript-Feld (wird zum Untertitel)
  - Bedingungen: Berufe (nur bei Villager), Wetter
  - Abspielen; Pitch-Vorschau (Kind/Erwachsen)
- Der Export kodiert alles nach Ogg Vorbis (Qualität ~0.4, 44.1 kHz oder 22.05 kHz wählbar), in einem Web Worker mit Fortschrittsbalken. `duration` wird automatisch ins Manifest geschrieben.

### Validierung und Export
- Live-Validierung über das gemeinsame Schema plus dieselben Warnregeln wie `PackValidator` (Regeln als JSON in `schema/lint-rules.json`, damit Mod und Editor identisch prüfen).
- Export → `<pack-id>-<version>.voicepack`, Download.
- Hinweistext nach dem Export: „Ziehe die Datei in Minecraft → Mod Menu → Talking Villagers“.

### Nicht-Ziele (vorerst)
- Kein Hosting oder Teilen von Packs (keine Server-Kosten). Ein Link zu Modrinth/CurseForge zum Hochladen reicht.
- Keine Sprachsynthese/TTS.

## Struktur
```
web-editor/
├── src/
│   ├── lib/model/        TS-Typen (aus JSON-Schema generiert: json-schema-to-typescript)
│   ├── lib/audio/        decode, trim, normalize, denoise (RNNoise, Spectral Gate), vad/split, encodeVorbis (Worker)
│   ├── lib/recorder/     AudioWorklet-Recorder, Pegel, Teleprompter-Session, Hotkeys
│   ├── lib/pack/         import (v2 + v1), export, validate
│   ├── lib/store/        IndexedDB-Projekt-Store
│   └── routes/           Start, Pack, Voice, Event-Editor, Export
├── tests/                Vitest (Unit) + Playwright (E2E: Pack erstellen → Export → Schema-valide)
└── vite.config.ts
```

## Aufgaben
- [ ] Spike: Ogg-Vorbis-Encoding im Browser (Bundle-Größe, Qualität, Firefox/Chrome/Safari)
- [ ] Datenmodell und Store
- [ ] UI: Projektliste, Pack-Editor, Voice-Editor, Line-Editor
- [ ] Spike: RNNoise-WASM im AudioWorklet (Latenz, Qualität mit Headset vs. USB-Mikro)
- [ ] Aufnahme-Studio: Skript-Modus, Hotkeys, VAD-Auto-Stopp, Auto-Split
- [ ] Rauschentfernung: RNNoise, Rauschprofil/Spectral Gating, Noise Gate, Batch-Neuanwendung
- [ ] Import (V2, V1), Export
- [ ] Cross-Check-Test: Ein im Editor exportiertes Pack wird im CI vom Java-`PackValidator` geprüft (gemeinsame Fixtures).
- [ ] GitHub Action: Build → GitHub Pages
- [ ] Link aus der Mod-GUI („Web-Editor öffnen“) und aus der README

## Ergebnis
Eine Person ohne technische Kenntnisse kann in < 30 Minuten eine Stimme mit 20 Zeilen aufnehmen. Sie kann sie rauschfrei als `.voicepack` exportieren und im Spiel installieren. Mit Skript-Modus und Auto-Stopp schafft man 50+ Zeilen in einer Session, ohne die Maus anzufassen.
