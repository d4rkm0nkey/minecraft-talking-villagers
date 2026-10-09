# Phase 3 – Villager neu (Attachments, Controller, Kind/Erwachsen, Geschlecht)

## Ziel
Villager haben wieder alle V1-Funktionen, jetzt mit sauberer Architektur. Zusätzlich gibt es Kinderstimmen (mit Stimmwechsel beim Erwachsenwerden) und Geschlecht.

## Architektur

```
Mixin (dünn)  ──►  VoiceHooks.onEvent(entity, VoiceEvent, ctx)
                          │
                          ▼
                 VoiceController (pro EntityKind konfiguriert)
                  ├─ ensureAssigned()      → VoiceAssigner (nutzt core.VoiceSelector)
                  ├─ Cooldowns / Chancen   → EventPolicy (aus Config)
                  ├─ SpeechTracker         → nicht überlappen
                  └─ LineSelector          → Zeile wählen → SpeakPayload senden
```

- Die gesamte Logik liegt **außerhalb** der Mixins und ist per GameTest prüfbar.
- `EventPolicy`: pro Event `cooldownTicks`, `chance`, `priority`, `interrupts`. Die Werte von V1 (Greeting 6000, Hero 2000, Random 300–5000 …) werden Defaults.

## Datenhaltung: Fabric Data Attachments
Statt eigener NBT-Felder im Mixin (`VoicePitch`, `VoiceName`) wird die Fabric Data Attachment API verwendet:

```java
record VoiceIdentity(Gender gender, Optional<VoiceRef> voice, float pitch, boolean assignedAsChild, Optional<String> language)
AttachmentType<VoiceIdentity> VOICE = AttachmentRegistry.create(id("voice"), b -> b.persistent(CODEC).syncWith(STREAM_CODEC, AttachmentSyncPredicate.all()));
```

- Persistiert automatisch, und der Sync zum Client ist für die Anzeige im Debug-Overlay/GUI nützlich.
- Laufzeit-Zustand (Cooldowns) ist **nicht** persistent, sondern liegt in einem transienten Attachment oder einer `WeakHashMap` im Controller.

## Regeln

### Zuweisung (lazy)
- Die Stimme wird **beim ersten Bedarf** zugewiesen (erstes Event oder erster Server-Tick nach dem Laden), nicht im Konstruktor. Begründung: Im Konstruktor sind Baby-Status, Beruf, Dorf (Meeting Point, für Phase 5) und Welt noch nicht bekannt. V1 hat im Konstruktor zugewiesen und deshalb Babys falsch behandelt.
- Auch nachträglich installierte Packs werden so berücksichtigt: Villager ohne Stimme bekommen beim nächsten Event eine.
- Wird ein Pack entfernt und die Stimme der Entity existiert nicht mehr, wird neu zugewiesen (gleiches Geschlecht, gleiche Sprache).

### Geschlecht
- Wird **einmal** bei der ersten Zuweisung gewürfelt (50/50, per Config einstellbar) und bleibt fürs ganze Leben, auch über das Erwachsenwerden.
- Voices mit `gender: "neutral"` passen auf beide.
- Bietet keine Stimme das gewürfelte Geschlecht an, greift die Fallback-Kaskade aus Phase 1. Das Geschlecht der Entity bleibt dabei erhalten. So passt es wieder, sobald später ein Pack mit passender Stimme installiert wird.

### Kinder
- `isBaby()` → Kriterium `child=true`.
- **Grow-up:** Hook auf den Übergang Baby→Erwachsen (`ageBoundaryReached` bzw. Äquivalent in 26.x). Dann:
  1. optional Event `grow_up` mit der **alten** Kinderstimme
  2. neue Erwachsenenstimme mit gleichem Geschlecht und gleicher Sprache zuweisen
  3. Pitch neu würfeln
- Fehlen Kinderstimmen, entscheidet die Config `childFallback`: `vanilla` (Default, Kinder klingen wie Vanilla-Babys) oder `pitch_up_adult`.
- Bei Babys, die per Command erzeugt oder per Spawn-Egg zu Erwachsenen werden, gilt derselbe Pfad.

### Pitch
- Pitch pro Entity aus `voice.pitch` (oder Default 0.95–1.05). Die künstliche Pitch-Erhöhung für Babys aus V1 entfällt, wenn eine echte Kinderstimme zugewiesen ist.

## Mixins (Villager)
Nur Hooks, keine Logik:

| Hook | Event |
|---|---|
| `aiStep`/`tick` (Server) | Tick → Controller prüft `greeting`, `hero`, `random`, Wetter-Events |
| `getAmbientSound` | `ambient` (Rückgabe `null` und eigener Sound, wenn die Stimme die Zeile hat) |
| `getHurtSound`, `getDeathSound` | `hurt`, `death` |
| `playCelebrateSound` | `celebrate` |
| `setUnhappy` / „Nein“-Kopfschütteln | `no` |
| `getNotifyTradeSound`/`getTradeUpdatedSound` | `yes` / `no` |
| `startTrading` | `trade` |
| `rewardTradeXp` / `notifyTrade` | `trade_success` |
| `increaseMerchantCareer` | `level_up` |
| `gossip` | `gossip` (nur einer der beiden Gesprächspartner spricht) |
| `GiveGiftToHero` (Behavior) | `hero_gift` |
| `startSleeping` / `stopSleeping` | `sleep` / `wake_up` |
| Panic-Activity | `panic` |

Die genauen Methodennamen werden im Code von 26.x (Mojang-Namen) nachgeschlagen. Vanilla-Sounds werden nur unterdrückt, wenn tatsächlich eine Zeile gespielt wurde.

## Migration aus V1-Welten
- Beim Laden: Wenn Legacy-NBT `VoiceName`/`VoicePitch` vorhanden ist, wird es in `VoiceIdentity` übernommen. Gefunden wird die Stimme über die Legacy-ID-Zuordnung in Phase 9, sonst wird neu zugewiesen. Danach werden die Legacy-Keys entfernt.

## Tests
- Unit (core): Auswahlregeln Kind/Geschlecht.
- GameTests:
  - Ein Baby-Villager bekommt eine Kinderstimme. Nach `setAge(0)` hat er eine Erwachsenenstimme mit gleichem Geschlecht.
  - Speichern und Laden behält Stimme, Geschlecht und Pitch.
  - Pack entfernt: Neuzuweisung ohne Crash.
  - Ohne Packs: Vanilla-Verhalten unverändert.

## Ergebnis
Alle V1-Events funktionieren für Villager, dazu Kinderstimmen, Stimmwechsel beim Erwachsenwerden und Geschlecht. Es gibt keine Logik mehr in Mixins.
