# Phase 3b – Zustände und Reaktionen für Villager

## Status: erledigt (Server getestet)

Umgesetzt auf Branch `v2/phase-3b` (abgeleitet von `v2/phase-4b`). Getestet auf Dedicated Servern mit Fabric und NeoForge über RCON und `/tv debug watch`:

| Szene | Gesagtes Event |
|---|---|
| Husk nähert sich Villagern | `spot_zombie` beim Start jeder Panik |
| Husk tötet einen Villager | Nachbar sagt `villager_died` |
| Glocke wird per Redstone geläutet | `hide` (geflüstert), nach einer Weile erneut |
| Villager mit Glocke in der Nähe | erkennen sie als Treffpunkt (bestätigt auch die Dorf-Erkennung aus Phase 5) |

**Nicht getestet** (braucht einen Spieler, einen Raid oder ein Dorf mit Tagesablauf): `attacked_by_player`, `greeting_friendly`/`greeting_unfriendly`, `trade_unfriendly`, `cured`, `pre_raid`, `spot_raider`, `work`, `play`, `meet`, `love`. Die Entscheidungslogik ist unit-getestet.

## Ziel
Villager reagieren auf die Lage im Dorf: Sie rufen um Hilfe, solange sie in Panik sind, warnen bei einem Raid, flüstern im Versteck, reden passend zu ihrer Arbeit und begrüßen Spieler je nach Ruf freundlich oder abweisend. Smalltalk über das Wetter, während ein Zombie sie jagt, gibt es nicht mehr.

## Zustände
Sie werden aus der aktiven Aktivität des Villager-Gehirns gelesen.

| Aktivität | Verhalten | Event | Fallback |
|---|---|---|---|
| Panik | erste Zeile nach Ursache, dann alle 5–8 s; kein Smalltalk | `spot_zombie` / `spot_raider` / `panic`, danach `panic_loop` | `panic` |
| Vor-Raid (Glocke wegen Raid) | Warnruf, dann alle 6–10 s | `pre_raid` | `panic` |
| Verstecken / Raid | Flüstern mit halber Lautstärke alle 15–25 s | `hide` | – |
| Arbeit | Smalltalk wird zur Arbeitszeile | `work` | `random` |
| Spielen (Kinder) | Smalltalk wird zur Spielzeile | `play` | `random` |
| Treffen an der Glocke | Smalltalk wird zum Tratsch | `meet` | `gossip` |

In Panik, Vor-Raid und Versteck ersetzt das Zustands-Event auch den Ambient-Sound.

## Reaktionen

| Event | Auslöser | Fallback |
|---|---|---|
| `attacked_by_player` | ein Spieler schlägt zu (höchstens alle 3 s, sonst `hurt`) | `hurt` |
| `villager_died` | ein Villager im Umkreis von 16 Blöcken stirbt; der nächste reagiert | – |
| `cured` | ein Zombie-Villager wurde geheilt (Reputations-Event `zombie_villager_cured`) | `celebrate` |
| `love` | Villager bekommen ein Kind; ein Elternteil sagt etwas | – |
| `greeting_friendly` | Begrüßung bei Reputation ≥ 30 | `greeting` |
| `greeting_unfriendly` | Begrüßung bei Reputation ≤ −30 (Golems greifen ab −100 an) | `greeting` |
| `trade_unfriendly` | Handelsbeginn bei Reputation ≤ −30 | `trade` |
| `cheer` | ein Spieler schlägt ein Monster (`Enemy`), das der Villager sieht; der nächste im Umkreis von 16 Blöcken, höchstens alle 5 s pro Spieler (nachträglich ergänzt) | – |
| `monster_killed` | ein Spieler tötet ein Monster, das der Villager sieht (nachträglich ergänzt) | `cheer` |

## Umsetzung
- **Katalog und Schema:** 15 neue Villager-Events mit Fallbacks, bestehende Packs funktionieren weiter.
- **core:**
  - `VillagerActivity` und `Threat` beschreiben Aktivität und Panik-Ursache.
  - `Chatter` kennt Aktivität, Bedrohung und Reputation: Alarm-Zustände mit eigenem Takt, Begrüßung nach Reputation, Smalltalk passend zur Aktivität. `Chatter.ambientEvent` ersetzt den Ambient-Sound in Alarm-Zuständen.
  - Der alte Panik-Schalter in `SpeakerState` ist durch die Aktivität ersetzt.
- **Minecraft-Seite (`EntityVoices`):**
  - Aktivität aus `Brain.isActive`.
  - Bedrohung aus `NEAREST_HOSTILE` oder `HURT_BY_ENTITY`: Zombie-Klassen → Zombie, Raider und Vex → Raider.
  - Reputation über `Villager.getPlayerReputation`.
  - Spieler-Treffer über `getLastHurtByMob` im selben Tick.
- **Hooks:**
  - `Villager.onReputationEventFrom` für `cured`
  - Tod-Hook für `villager_died`
  - `getBreedOffspring` für `love`
  - `AbstractVillager.setTradingPlayer` für `trade_unfriendly`
