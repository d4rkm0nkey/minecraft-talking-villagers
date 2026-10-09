# Phase 5 – Sprachen und Sprache pro Dorf

## Ziel
Jedes Voicepack (oder jede Voice) hat eine Sprache. Wenn Packs mehrerer Sprachen installiert sind, kann jedes Dorf zufällig eine Sprache bekommen, und alle Bewohner sprechen sie.

## Config: `languageMode`
| Modus | Verhalten |
|---|---|
| `any` (Default) | Sprache wird ignoriert, jede Stimme ist erlaubt. |
| `preferred` | Nur Stimmen aus `preferredLanguages` (Liste, Reihenfolge = Priorität), sonst Fallback auf `any`. |
| `per_village` | Jedes Dorf würfelt einmal eine Sprache aus den verfügbaren. Alle Villager des Dorfs bekommen Stimmen dieser Sprache. |

Dazu:
- `allowedLanguages`: optionaler Filter, z. B. nur `de` und `en` würfeln, obwohl auch `fr` installiert ist.
- `strictLanguage`: Wenn `true`, bekommt ein Villager **keine** Stimme, falls es in der Dorfsprache keine passende gibt (Kind/Geschlecht). Wenn `false` (Default), greift der Fallback auf eine andere Sprache.

## Was ist ein „Dorf“?
Minecraft hat kein Dorf-Objekt. Vorgehen:

1. **Primär:** Die Memory `MEETING_POINT` des Villagers (die Glocke, als `GlobalPos`). Alle Villager mit derselben Glocke gehören zum selben Dorf.
2. **Clustering:** Eine `VillageLanguageData extends SavedData` pro Dimension speichert `Map<BlockPos, String> anchors` (Ankerposition → Sprache). Neue Glocken innerhalb von 64 Blöcken (Config) um einen bestehenden Anker übernehmen dessen Sprache. So sprechen Dörfer mit mehreren Glocken einheitlich.
3. **Ohne Glocke** (Villager wurde gerade erst generiert, oder Nomade): Sprache des nächsten Ankers im Radius. Gibt es keinen, wird die Zuweisung **verzögert**. Ist nach N Ticks immer noch keine Glocke bekannt, wird ein neuer Anker an der Position des Villagers erzeugt.
   - Deshalb ist die Zuweisung in Phase 3 lazy: Frisch generierte Dorfbewohner haben in den ersten Ticks noch keine Glocke.
4. Die gewählte Sprache wird zusätzlich in der `VoiceIdentity` des Villagers gespeichert. Zieht ein Villager in ein anderes Dorf, **behält** er seine Stimme und Sprache (ein Zugezogener). Kinder, die im neuen Dorf geboren werden, bekommen die Dorfsprache.

### Kinder beim Erwachsenwerden
Die neue Stimme wird in der **in der Identity gespeicherten** Sprache gewählt, nicht neu ermittelt. Das Kind behält seine Sprache.

## Andere Entities
| Entity | Sprache |
|---|---|
| Wandering Trader | zufällig aus `allowedLanguages`, oder Config `traderLanguage` = `random` / `nearest_village` |
| Witch | Witch-Hütte: Anker an der Spawn-Position. Sonst `random`. |
| Illager im Raid | eine Sprache **pro Raid**, gespeichert über die Raid-ID in `SavedData`. Option: `opposite_of_village` (eine andere Sprache als das angegriffene Dorf, falls verfügbar) |
| Illager-Patrouille / Mansion / Outpost | Anker-Logik wie beim Dorf, mit eigener Ankerliste `illager_anchors` |

## Sprachcodes
- BCP-47-Basis (`de`, `en`, `fr`, `en-GB` …). Der Vergleich erfolgt zuerst exakt, dann über die Primärsprache (`en-GB` passt auf `en`).
- Anzeige in der GUI über `Locale.forLanguageTag(code).getDisplayLanguage(clientLocale)`.

## Aufgaben
- [ ] `LanguageResolver`-Interface mit Implementierungen pro Modus. Liegt im core, testbar mit Fake-Welt.
- [ ] `VillageLanguageData` (SavedData, Codec)
- [ ] Hook: Villager bekommt bzw. ändert `MEETING_POINT` → ggf. ausstehende Zuweisung auslösen
- [ ] Raid-Sprache
- [ ] Config-Felder
- [ ] GameTests: zwei Glocken mit 200 Blöcken Abstand und `per_village` → beide Gruppen sprechen jeweils einheitlich. Packs mit `de` und `en` installiert.

## Ergebnis
Mit zwei installierten Packs unterschiedlicher Sprache und `languageMode=per_village` spricht jedes Dorf einheitlich. Die Zuordnung bleibt über Neustarts stabil.
