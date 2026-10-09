# Phase 2 – Pack-Loading, Audio-Pipeline und Networking

## Status: erledigt

Umgesetzt auf Branch `v2/phase-2`. Getestet: Dedicated Server mit Fabric und NeoForge über RCON (Pack laden, `/tv`-Commands, Zeile auswählen und senden). Für den Client gibt es einen automatisierten Fabric-Client-GameTest (`./gradlew :fabric:runClientGameTest`), der eine Welt anlegt, einen Villager sprechen lässt und prüft, dass die Zeile wirklich läuft. **Offen:** Der Test lief bisher nicht bis zum Ende durch; der Prozess wurde jedes Mal von außen per SIGTERM beendet.

Abweichungen vom Plan:
- **Kein virtuelles Resourcepack.** Ein Mixin auf `SoundBufferLibrary.getCompleteBuffer` liefert die Ogg-Daten für `talkingvillagers:sounds/voice/<sha256>/…` direkt aus der `.voicepack`-Datei. `VoiceSoundInstance` baut ihren `Sound` selbst, eine `sounds.json` entfällt. Vorteile: Ein neues Pack wird ohne Ressourcen-Neuladen sofort hörbar, Packs tauchen nicht in der Resourcepack-Liste auf, und die Lösung ist für Fabric und NeoForge identisch.
- **Pack-Identität über den Hash.** `SpeakPayload` enthält statt der Pack-ID den SHA-256 der Datei. Der Client spielt damit garantiert aus derselben Datei wie der Server, auch bei unterschiedlichen Pack-Versionen.
- **Vanilla-Fallback auf dem Server.** Spieler ohne Mod oder ohne das Pack bekommen vom Server direkt den Vanilla-Sound (`ClientboundSoundEntityPacket`). Der Server kennt dafür über `client_packs` die Packs jedes Clients.
- **`PackRepository`, `PackIndex` und der Versionsvergleich** liegen in `core` und sind dort getestet.
- **Fabric ohne getrennte Client-/Server-Source-Sets:** `common` enthält Client-Code, das verträgt sich nicht mit `splitEnvironmentSourceSets`. Client-Klassen werden nur über Client-Einstiegspunkte und Netzwerk-Handler erreicht.
- **Debug-Command:** `/tv debug say <entities> <pack:voice> <event>`, dazu `/tv packs list` und `/tv packs reload`.

## Ziel
`.voicepack`-Dateien aus `.minecraft/voicepacks/` werden geladen. Ihre Audio-Dateien sind ohne manuelles Resourcepack hörbar, und der Server kann einem Client sagen: „Entity X spricht Zeile Y“. Ein Neuladen funktioniert ohne Neustart.

## Architektur

```
            Server (oder integrierter Server)                     Client
┌──────────────────────────────────────────┐    ┌───────────────────────────────────────┐
│ PackRepository (Manifeste, kein Audio)   │    │ PackRepository (Manifeste + ZIP-Handles)│
│ VoiceController ─► entscheidet Zeile     │    │ VoicepackResourceSource                │
│        │                                 │    │  └─ virtuelles PackResources           │
│        └─► S2C SpeakPayload ─────────────┼───►│      talkingvillagers:voice/<pack>/... │
│            (entityId, packId, voiceId,   │    │ VoicePlayer                            │
│             event, lineIndex, pitch)     │    │  └─ EntityBoundSoundInstance           │
└──────────────────────────────────────────┘    └───────────────────────────────────────┘
```

### Warum so?
- **Keine SoundEvent-Registrierung mehr.** V1 registrierte pro Zeile ein `SoundEvent` in der Registry. Das erforderte identische Configs auf Server und Client und einen Neustart. V2 spielt Sounds auf dem Client über eine `SoundInstance` mit direkter `ResourceLocation`. Der Server muss keine Sound-IDs kennen.
- **Eigenes Payload statt Vanilla-Soundpaket.** Der Client entscheidet selbst, ob er das Pack hat. Falls nicht, spielt er den Vanilla-Sound (Fallback). Er kann außerdem Untertitel aus dem Transkript anzeigen und Lautstärke und Kategorie selbst steuern.

## Aufgaben

### PackRepository (common)
- [ ] Verzeichnis `gameDir/voicepacks/` anlegen. Auf dem Dedicated Server liegt es im Server-Root.
- [ ] Alle `*.voicepack` scannen, mit `core.PackReader` laden und validieren. Fehlerhafte Packs werden geloggt und als „defekt“ markiert (für die GUI), aber nicht geladen.
- [ ] Zustand pro Pack: `enabled` (aus der Config, Phase 7), `sha256` (für den Multiplayer-Abgleich), `problems`.
- [ ] Doppelte Pack-IDs: das neuere `version` gewinnt, Warnung ins Log.
- [ ] `reload()` ist thread-safe: neuer Snapshot, dann atomarer Tausch (`volatile ImmutablePackIndex`).
- [ ] Index: `Map<VoiceRef, Voice>`, `Map<EntityKind, List<Voice>>`, Liste der Sprachen.

### Client: virtuelles Resourcepack
- [ ] Eigene `PackResources`-Implementierung, die Dateien direkt aus den `.voicepack`-ZIPs liefert:
  - `assets/talkingvillagers/sounds/voice/<pack>/<voice>/<event>/<file>.ogg`
- [ ] Wird als **immer aktives, verstecktes** Pack eingehängt, über einen `RepositorySource` im Client-`PackRepository` (Mixin bzw. Fabric-Resource-Loader-API, je nachdem was in 26.x verfügbar ist).
- [ ] Eine `sounds.json` wird nicht benötigt, wenn `SoundInstance`s direkt mit `Sound`-Objekten gebaut werden. Alternative: `sounds.json` generieren. Im Spike entscheiden, was in 26.x robuster ist.
- [ ] Nach `PackRepository.reload()` → `Minecraft.reloadResourcePacks()`.

### Client: VoicePlayer
- [ ] `play(Entity, VoiceLine, pitch, volume)` erzeugt eine an die Entity gebundene Sound-Instanz. Kategorie: neue `SoundSource`? Nein, das ist nicht erweiterbar. Stattdessen `SoundSource.NEUTRAL` bzw. `HOSTILE` je Entity, plus eigener Lautstärke-Multiplikator in der Config.
- [ ] **Pro Entity höchstens eine laufende Zeile.** Eine neue Zeile stoppt die alte oder wird verworfen, abhängig von der Event-Priorität (`hurt`/`death` unterbrechen, `random` nicht).
- [ ] Untertitel: wenn vorhanden, `text` der Zeile, sonst ein generischer Text („Dorfbewohner spricht“). Konfigurierbar.

### Networking
- [ ] Payloads (Fabric Networking API, `CustomPacketPayload` + `StreamCodec`):
  - `S2C speak(entityId, VoiceRef, eventId, lineIndex, pitch)`
  - `S2C server_packs(List<PackId, version, sha256>)`, beim Join (für Phase 9)
  - `C2S client_packs(List<sha256>)`, welche Packs der Client hat. Grundlage für den Pack-Download (Phase 9)
- [ ] Senden an alle Spieler in Hörweite (`PlayerLookup.tracking(entity)`).
- [ ] Hat der Client die Mod nicht (Vanilla-Client auf modded Server): Fabric sendet nur an Clients mit registriertem Kanal (`ServerPlayNetworking.canSend`). Vanilla-Clients hören normale Sounds.

### Server: Sprech-Sperre
- [ ] `SpeechTracker` pro Entity: `busyUntilTick = now + duration*20`. Vor einer neuen Zeile prüft der Server, ob die Entity frei ist. Damit braucht der Server keine Audio-Daten.

## Ergebnis
- Ein Test-`.voicepack` in `voicepacks/` legen, `/reload` bzw. „Packs neu laden“ ausführen, und ein Debug-Command (`/tv debug say @e[type=villager,limit=1] greeting`) spielt die Zeile auf dem Client ab.
- Dedicated Server mit Mod und Client mit Mod und Pack: Die Zeile ist hörbar, mit korrekter Position.
- Client ohne das Pack: kein Crash, Vanilla-Sound.

## Risiken
- Die Resource-API in 26.x muss geprüft werden. Deshalb steht am Anfang der Phase ein **Spike (1–2 Tage)**, der nur „eine `.ogg` aus einem ZIP abspielen“ beweist.
