# Phase 4b – Zustände für Witches und Illager

## Status: erledigt (Server getestet)

Umgesetzt auf Branch `v2/phase-4b` (abgeleitet von `v2/phase-6`). Getestet auf Dedicated Servern mit Fabric und NeoForge über RCON mit KI-gesteuerten Mobs und `/tv debug watch`:

| Situation | Gesagtes Event |
|---|---|
| Vindicator „Johnny“ kämpft gegen Schweine | `johnny` |
| Johnny nach seinen Kills | `idle` (kein `lost_target`) |
| Patrouillen-Hauptmann ohne Ziel | `patrol_leader` |
| Pillager unter 30 % Leben | `low_health` |
| verletzte Hexe trinkt Heiltrank | `heal_self` |
| Hexe im Leerlauf | `brew` |
| Pillager stirbt neben anderen | `ally_died` beim nächsten Verbündeten |

**Nicht getestet** (braucht einen echten Spieler oder Raid): `combat`-Sprüche gegen Spieler, `lost_target`, `victory`, `charge_at`, `retreat`, `raid_idle`, `heal_ally`, `wololo`, `blindness`.

## Ziel
Witches und Illager reden passend zur Situation statt immer gleich: vor sich hin, auf Patrouille, im Raid oder im Kampf. Dazu kommen Reaktionen, wenn ein Kampf endet, wenn sie schwer verletzt sind oder ein Verbündeter stirbt.

## Zustände und Übergänge

Alle Witches und Illager:

| Event | Auslöser | Fallback |
|---|---|---|
| `idle` | Ambient-Timer, kein Ziel, kein Raid, keine Patrouille | `ambient` |
| `combat` | Spieler ist Ziel; Ambient-Timer und zusätzlich alle 8–15 s | `ambient` |
| `raid_idle` | Ambient-Timer im laufenden Raid ohne Ziel | `idle` |
| `lost_target` | Kampf endet, ohne dass das Ziel getötet wurde | – |
| `low_health` | unter 30 % Leben, einmal bis zur Erholung über 60 % | – |
| `victory` | hat einen Spieler getötet | – |
| `ally_died` | Witch oder Illager im Umkreis von 16 Blöcken stirbt; der nächste Verbündete reagiert | – |

Pro Entity-Art:

| Entity | Event | Auslöser | Fallback |
|---|---|---|---|
| Illager | `patrol` | Ambient-Timer auf Patrouille | `idle` |
| Illager | `patrol_leader` | Ambient-Timer als Patrouillen-Hauptmann | `patrol` |
| Vindicator | `charge_at` | erblickt einen Spieler (statt `spot_player`) | `spot_player` |
| Vindicator | `johnny` | Kampfsprüche, wenn er „Johnny“ heißt; er greift dann alles an | `combat` |
| Evoker | `retreat` | im Kampf, Spieler näher als 8 Blöcke | `combat` |
| Evoker | `wololo` | färbt ein blaues Schaf rot | – (ohne Zeilen bleibt das Vanilla-„Wololo“) |
| Illusioner | `blindness` | Blindheitszauber | `cast_spell` |
| Witch | `brew` | Leerlauf (statt `idle`) | `idle` |
| Witch | `heal_self` | trinkt einen Heiltrank | `drink_potion` |
| Witch | `drink_other` | trinkt Feuerresistenz, Wasseratmung oder Tempo | `drink_potion` |
| Witch | `heal_ally` | wirft einem Raider einen Heil- oder Regenerationstrank zu | `throw_potion` |

Durch die Fallbacks funktionieren ältere Packs weiter: Ein Pack nur mit `ambient` klingt wie bisher. Ein Pack mit `idle` und `combat` klingt im Kampf anders als im Leerlauf.

## Umsetzung
- **Katalog:** Die neuen Events stehen in `EventCatalog` und im JSON-Schema. `EventCatalog.HOSTILE` fasst Witch und Illager zusammen.
- **`HostileChatter`** (core, getestet):
  - `ambientEvent(kind, situation)` wählt anhand der Situation das Event, das den Vanilla-Ambient-Sound ersetzt.
  - `tick(...)` verfolgt den Kampfzustand und liefert Kampfsprüche, `lost_target`, `low_health` und den Rückzug des Evokers.
- **Situation:** Sie wird in `EntityVoices.hostileSituation` aus dem Spiel gelesen:
  - Ziel: Spieler, bei Johnny jedes Ziel
  - Abstand zum Ziel und Anteil der Lebenspunkte
  - Raid (`Raider.hasActiveRaid`)
  - Patrouille und Hauptmann: über Accessor-Mixins, weil es keine öffentlichen Getter gibt
- **Tick:** Einstieg über `Mob.serverAiStep`, einmal pro Sekunde und Entity.
- **Tod:** Ein Hook in `LivingEntity.die` setzt den letzten Sieg des Killers (jeder Kill, damit Johnny nicht „Ziel verloren“ sagt). Er löst außerdem `victory` (Spieler-Kill), `laugh` (Hexe tötet Nicht-Spieler) und `ally_died` aus.
- **Hexen-Tränke:** Der Wrap auf `Level.playSound` liest den Trank in der Hand (Heilung → `heal_self`) bzw. das Ziel des Wurfs (Raider → `heal_ally`).
- **`/tv debug watch`:** Neu. Zeigt jede gesprochene Zeile im Chat, bzw. im Server-Log, wenn der Befehl von der Konsole kommt. Gedacht für Pack-Ersteller und Tests.
