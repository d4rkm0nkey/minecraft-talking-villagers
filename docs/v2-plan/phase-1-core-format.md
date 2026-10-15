# Phase 1 – Voicepack-Format v2 und Kern-Domain

## Status: erledigt

Umgesetzt im Modul `core` (Paket `me.teajay.talkingvillagers.core`), 53 Tests, 92 % Zeilenabdeckung. Schema: `schema/voicepack.schema.json`, Beispiel: `schema/examples/voicepack.json`.

Abweichungen und Ergänzungen:
- **`duration` ist optional.** `OggInfo` liest Kanalzahl, Abtastrate und Länge aus den Ogg-Headern, ohne zu dekodieren, und `PackReader` ergänzt fehlende Längen. Die Warnung „fehlende `duration`“ entfällt.
- **Problem-Codes statt `lint-rules.json`:** Jedes Problem hat einen stabilen Code (`ProblemCode`, z. B. `missing_file`), der in GUI und Web-Editor als Übersetzungsschlüssel dient.
- **Berufe:** Neben den Vanilla-IDs (inkl. `none`) sind Berufe anderer Mods als Namespace-ID erlaubt (`mymod:baker`). `minecraft:farmer` wird zu `farmer` normalisiert.
- **Dateipfade** müssen klein geschrieben unter `sounds/` liegen und auf `.ogg` enden (Vorgabe der Minecraft-Ressourcenpfade). `..`-Segmente sind ausgeschlossen.
- **Reihenfolge der Fallback-Kaskade:** zuerst Sprache lockern, dann Geschlecht (wie geplant), wobei beim Lockern des Geschlechts die Sprache noch einmal bevorzugt wird.
- Ein Test (`SchemaTest`) hält Schema und Java-Parser synchron: gleiche Events, Entities, Berufe und Formatversion, und beide lehnen dieselben fehlerhaften Manifeste ab.

## Ziel
Ein einziges, selbstbeschreibendes Dateiformat für Voicepacks und ein Minecraft-unabhängiger Kern, der dieses Format liest, validiert und daraus Stimmen auswählt. Alle Regeln (Kind/Erwachsen, Geschlecht, Sprache, Entity-Typ, Beruf) stecken hier und sind vollständig unit-getestet.

## 1. Dateiformat `.voicepack`

Ein `.voicepack` ist ein ZIP-Archiv:

```
my-pack.voicepack
├── voicepack.json          Manifest (Pflicht)
├── icon.png                optional, 64×64 oder 128×128
└── sounds/
    └── <voice-id>/
        └── <event>/
            └── <datei>.ogg  Ogg Vorbis, mono empfohlen
```

### Manifest `voicepack.json`

```jsonc
{
  "format": 2,
  "id": "teajay-default",              // [a-z0-9_-], global eindeutig
  "name": "TeaJay Default",
  "version": "2.0.0",
  "description": "Die Original-Stimmen",
  "authors": ["TeaJay"],
  "language": "de",                     // BCP-47-Sprachcode, Pflicht
  "voices": [
    {
      "id": "anna",                     // eindeutig innerhalb des Packs
      "name": "Anna",
      "entities": ["villager"],         // mind. 1, siehe Entity-Katalog
      "gender": "female",               // "male" | "female" | "neutral"
      "child": false,                   // true = nur für Baby-Villager
      "language": null,                 // optional: überschreibt Pack-Sprache
      "pitch": { "min": 0.95, "max": 1.05 }, // optional
      "weight": 1,                      // optional: Gewichtung bei der Auswahl
      "lines": {
        "greeting": [
          {
            "file": "sounds/anna/greeting/hello_1.ogg",
            "text": "Hallo, Reisender!",            // Transkript, für Untertitel
            "duration": 1.42,                       // optional, Sekunden; fehlt sie, wird sie aus der Datei gelesen
            "conditions": { "professions": ["farmer"], "weather": "clear" } // optional
          }
        ],
        "hurt": [ { "file": "sounds/anna/hurt/au.ogg", "duration": 0.4 } ]
      }
    }
  ]
}
```

**Designentscheidungen**
- **Ein Manifest statt mehrerer Dateien:** Es gibt keine eigene `sounds.json`, keine `lang`-Datei und keine Config-Datei mehr. Die Mod erzeugt die Resourcepack-Struktur zur Laufzeit selbst (Phase 2).
- **Bedingungen statt Beruf-Ordner:** V1 hatte pro Beruf einen eigenen Datenblock. In V2 hängt der Beruf als optionale `conditions` an der Zeile. Das gilt genauso für Wetter, Tageszeit und später weitere Bedingungen.
- **`duration`:** Der Server muss wissen, wie lange eine Zeile dauert, damit sich Stimmen nicht überlappen. Die Länge steht in den Ogg-Headern und wird beim Laden gelesen, ein Audio-Decoder ist dafür nicht nötig.
- **`format`-Feld:** Erlaubt spätere Formatänderungen mit Migrationspfad.
- **`entities` als Liste:** Eine Illager-Stimme kann z. B. für Pillager, Vindicator und Evoker gleichzeitig gelten.

### JSON-Schema
- [ ] `schema/voicepack.schema.json` (JSON Schema 2020-12) schreiben. Es ist die einzige Quelle der Wahrheit für Mod und Web-Editor.
- [ ] Event-Namen und Entity-Typen stehen als `enum` im Schema und werden aus dem Java-Katalog generiert oder per Test abgeglichen.

## 2. Kern-Domain (`core`-Modul, Pure Java)

```
me.teajay.talkingvillagers.core
├── model/        VoicePack, Voice, VoiceLine, LineConditions, Gender, PackId, VoiceRef
├── catalog/      EntityKind (enum), VoiceEvent (Registry), EventCatalog
├── io/           PackReader (ZIP → VoicePack), ManifestCodec (Gson), PackValidator
├── select/       VoiceSelector, VoiceCriteria, LineSelector, SelectionContext
└── util/         WeightedRandom, Result<T, List<Problem>>
```

### Event-Katalog
Ein `VoiceEvent` hat eine ID, gilt für bestimmte Entity-Arten, und optional gibt es ein Vanilla-Fallback-Flag. Die Liste wird in Phase 3 und 4 befüllt, bereits jetzt angelegt werden:

| Gruppe | Events |
|---|---|
| Allgemein (alle) | `ambient`, `hurt`, `death`, `celebrate` |
| Villager | `greeting`, `random`, `gossip`, `yes`, `no`, `trade`, `trade_success`, `level_up`, `hero`, `hero_gift`, `good_weather`, `bad_weather`, `sleep`, `wake_up`, `panic`, `grow_up` |
| Witch | `drink_potion`, `throw_potion`, `laugh` |
| Wandering Trader | `trade`, `trade_success`, `yes`, `no`, `drink_potion`, `disappear`, `greeting` |
| Illager | `spot_player`, `attack`, `charge`, `raid_horn`, `cast_spell` (Evoker/Illusioner), `summon_vex`, `fangs`, `mirror` (Illusioner) |

### Stimmenauswahl
`VoiceSelector.select(VoiceCriteria criteria, Collection<Voice> pool, RandomSource rnd)`

`VoiceCriteria`: `entityKind`, `child`, `gender` (optional), `language` (optional), `excludeVoices` / `preferLeastUsed` (für Vielfalt im Dorf).

Fallback-Kaskade, falls kein Treffer:
1. Alle Kriterien
2. ohne `language` (wenn Config `strictLanguage=false`)
3. ohne `gender`
4. `child` lockern: Ein Kind bekommt **nie** eine Erwachsenenstimme, falls die Config `childFallback=pitch_up_adult` nicht gesetzt ist. Bei gesetzter Option bekommt das Kind eine Erwachsenenstimme mit Pitch +0.4.
5. keine Stimme, also Vanilla-Sounds

`LineSelector.pick(Voice, VoiceEvent, LineContext)`: filtert Zeilen nach `conditions` (Beruf, Wetter, …). Spezifische Zeilen (mit passender Bedingung) werden mit Wahrscheinlichkeit 50 % bevorzugt, wie in V1. Die zuletzt gespielte Zeile wird nicht direkt wiederholt.

### Validierung
`PackValidator` liefert eine Liste von `Problem(severity, path, message)`:
- Fehler: Manifest fehlt oder ist ungültig, `format` wird nicht unterstützt, doppelte Voice-IDs, referenzierte Datei fehlt, Datei ist kein Ogg Vorbis (Magic Bytes `OggS` + Vorbis-Header), unbekannte Entity.
- Warnungen: unbekanntes Event (wird ignoriert), Voice ohne `hurt`/`death`, Datei > 1 MB, Stereo-Datei (in Minecraft nicht positionsabhängig!), unbenutzte Dateien, Events, die keine der Entities der Voice auslöst, Kinderstimme für Nicht-Villager.

## Aufgaben
- [ ] Schema schreiben
- [ ] Modell als Java-`record`s
- [ ] `ManifestCodec` mit Gson (in Minecraft enthalten, im `core` als `compileOnly`)
- [ ] `PackReader` liest ZIP über `java.util.zip.ZipFile` mit try-with-resources. Zip-Slip-Schutz: Pfade normalisieren und `..` ablehnen.
- [ ] `PackValidator`
- [ ] `VoiceSelector`, `LineSelector`
- [ ] Unit-Tests: Fixtures unter `core/src/test/resources/packs/` (gültig, kaputt, nur Kinderstimmen, mehrsprachig …)

## Ergebnis
- `core` ist eigenständig gebaut und getestet, Testabdeckung > 80 %.
- Das Schema ist veröffentlicht und wird in Phase 8 vom Web-Editor genutzt.
