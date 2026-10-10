# Phase 5 – Sprachen und Sprache pro Dorf

## Status: erledigt (Server getestet)

Umgesetzt auf Branch `v2/phase-5`. Getestet auf Dedicated Servern mit Fabric und NeoForge über RCON mit zwei Packs gleicher Stimmen in Deutsch und Englisch und `languageMode: per_village`:
- Vier Villager-Gruppen ohne Glocke, je 300 Blöcke auseinander, warten bis zu 30 Sekunden auf ein Dorf und bekommen dann je einen gemeinsamen Anker. Jede Gruppe spricht einheitlich, die Gruppen bekamen unterschiedliche Sprachen.
- Zwei benachbarte Hexen teilen sich eine Sprache.
- Die Sprachen überstehen einen Neustart.

**Nicht getestet:** Dörfer mit echter Glocke (Villager mit KI finden ihren Treffpunkt erst nach einiger Zeit), Raid-Sprache und die Modi `preferred` und `nearest_village` im Spiel. Die Logik dahinter ist unit-getestet bzw. sehr klein.

Umsetzung und Abweichungen:
- **Server-Config** `config/talkingvillagers-server.json` gibt es schon jetzt (eigentlich Phase 7): Geschlechterverteilung, Kinder-Fallback und alle Spracheinstellungen. Fehlende Werte werden mit Standardwerten ergänzt. `/tv reload` lädt Config und Packs neu.
- **Sprachwahl in `core`** (`me.teajay.talkingvillagers.core.language`): `LanguageMode`, `LanguageSettings`, `LanguageAnchors` (nächster Anker im Radius) und `LanguageChooser` (Kandidaten nur aus Sprachen mit Erwachsenenstimmen für die Entity-Art, `allowedLanguages` als Filter).
- **`LanguageData`** speichert pro Dimension die Dorf-Anker, die Anker für Hexenhütten und Außenposten und die Sprache jedes laufenden Raids. Beendete Raids werden beim nächsten Eintrag entfernt.
- **Wartezeit:** Ein Villager ohne Glocke und ohne Anker in der Nähe wartet bis zu 600 Ticks. Erst dann wird an seiner Position ein Anker angelegt. Danach ist seine Sprache in der `VoiceIdentity` gespeichert und bleibt, auch wenn er umzieht oder erwachsen wird.
- **Kinder erben die Sprache:** Ein neugeborener Villager bekommt die Sprache eines zufällig gewählten Elternteils, egal in welchem Dorf er geboren wird und in jedem Sprachmodus. Seine Stimme wird beim ersten Gebrauch in dieser Sprache gewählt. Hook: `Villager.getBreedOffspring`. Im echten Spiel noch nicht getestet, weil sich Villager per Konsole nicht verpaaren lassen.
- **Umzug getestet:** Ein englischer Villager, der ins deutsche Dorf teleportiert wurde, spricht dort weiter Englisch.
- **`/tv debug language [pos]`** zeigt Modus, Dorfsprache und Außenposten-Sprache an einer Position. Die Commands zum Ändern kommen in Phase 6.

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

### Kinder
- Bei der Geburt erbt das Kind die Sprache eines zufälligen Elternteils (nicht die Sprache des Geburtsdorfs).
- Beim Erwachsenwerden wird die neue Stimme in der **in der Identity gespeicherten** Sprache gewählt. Das Kind behält seine Sprache.

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
