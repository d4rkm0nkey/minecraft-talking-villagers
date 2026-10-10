# Phase 6 – Commands

## Status: erledigt (Server getestet)

Umgesetzt auf Branch `v2/phase-6`. Alle Commands wurden auf Dedicated Servern mit Fabric und NeoForge über RCON getestet, inklusive der Fehlerfälle: falsche Entity-Art, falsches Alter, Zombie, unbekanntes Event, Zeilennummer außerhalb, ungültiger Sprachcode, Sprache ohne installierte Stimmen. Tab-Vervollständigung lässt sich über RCON nicht prüfen.

Umgesetzter Befehlsumfang:
```
/tv reload                                       Config und Packs neu laden
/tv packs list | info <pack> | reload
/tv voice get <entity>                           mit [▶ abspielen]-Link
/tv voice set <entities> <pack:voice> [force]
/tv voice randomize <entities> [newGender]
/tv voice clear <entities>                       stumm, behält Vanilla-Sounds
/tv voice say <entity> <event> [line]
/tv gender get <entity>
/tv gender set <entities> male|female
/tv language village get [pos]
/tv language village set <language> [pos] [reassign]
/tv language entity set <entities> <language>
/tv debug stats                                  welche Stimme wie oft
/tv debug say <entities> <pack:voice> <event>    beliebige Stimme testen
```

Abweichungen vom Plan:
- **Syntax:** `force`, `newGender` und `reassign` sind einfache Wörter statt `force:true` bzw. `--reassign`. `voice randomize` behält das Geschlecht standardmäßig, `newGender` würfelt es neu.
- **`/tv debug info` und `/tv debug language`** aus Phase 3 und 5 sind durch `voice get` und `language village get` ersetzt.
- **`VoiceIdentity.muted`** ist neu: Stumm geschaltete Entities bekommen nie automatisch eine Stimme, bis `voice set` oder `voice randomize` sie wieder freigibt.
- **Entities, die noch auf ihr Dorf warten**, bekommen bei Commands sofort eine Grundidentität. So lassen sich Geschlecht und Sprache auch in den ersten 30 Sekunden ändern.
- **`gender set` und `language … set`** weisen sofort eine passende neue Stimme zu. Eine manuell gesetzte Stimme wird dabei ersetzt.
- **Sprachen ohne installierte Stimmen** sind erlaubt, z. B. bevor das Pack installiert ist. Es erscheint nur eine Warnung.
- **Ausgaben** sind übersetzt (Deutsch und Englisch), auch Geschlecht, Alter und Status.

## Ziel
Admins und Packersteller können Stimmen ansehen, ändern und testen. Wurzel-Command: `/talkingvillagers`, Alias `/tv`. Registriert über `CommandRegistrationCallback` (Fabric API).

## Command-Baum

```
/tv voice get <entity>
/tv voice set <entities> <voice>                 # voice = <pack>:<voiceId>, Tab-Completion gefiltert auf passende Entity-Arten
/tv voice randomize <entities> [keepGender]      # neu würfeln nach allen Regeln (Kind, Sprache …)
/tv voice clear <entities>                       # Stimme entfernen → Vanilla (bleibt leer, Flag "muted")
/tv voice say <entity> <event> [lineIndex]       # Zeile abspielen (Test für Pack-Ersteller)

/tv gender get <entity>
/tv gender set <entities> male|female

/tv language village get [<pos>]                 # Sprache des Dorfs an Position (Default: eigene Position)
/tv language village set <language> [<pos>]     # Dorfsprache ändern
        [--reassign]                             # optional: alle Villager im Dorf neu einkleiden
/tv language entity set <entities> <language>    # Sprache einzelner Entities ändern (+ Neuzuweisung)

/tv packs list                                   # geladene Packs: id, Version, Sprache, #Voices, Status
/tv packs info <pack>                            # Voices mit Entity/Gender/Child/#Lines
/tv packs reload                                 # Server-PackRepository neu laden

/tv debug stats                                  # Zuweisungsverteilung (welche Voice wie oft im geladenen Bereich)
```

## Details
- **Berechtigungen:** `voice`, `gender`, `language` und `packs reload` brauchen Permission-Level 2. `voice get`, `packs list` und `packs info` sind für alle frei.
- **Argument-Typen:**
  - `<entities>`: `EntityArgument.entities()`. Nicht unterstützte Typen werden übersprungen, mit Meldung „3 von 5 Entities geändert, 2 nicht unterstützt“.
  - `<voice>`: eigener `SuggestionProvider`, der nur Stimmen vorschlägt, die zum Typ (und Kind-Status) der gewählten Entities passen. Erzwingen mit `force:true`, z. B. eine Erwachsenenstimme für ein Kind. Dann wird die Stimme beim Erwachsenwerden **nicht** automatisch gewechselt (`manual=true` in `VoiceIdentity`).
  - `<event>`: Vorschläge aus dem Event-Katalog für den Entity-Typ.
  - `<language>`: Vorschläge aus den Sprachen der installierten Packs.
- **Manuell gesetzte Stimmen** werden im Attachment mit `manual=true` markiert. Sie überstehen einen Pack-Reload, solange die Stimme existiert, und auch das Erwachsenwerden.
- **Feedback:** Übersetzbare Texte (`Component.translatable`), mit Klick-Aktionen in `get`-Ausgaben, z. B. „[▶ abspielen]“ führt `/tv voice say` aus.
- Die Logik steckt in einem `VoiceCommandService`, das Command-Gerüst ist nur Parsing. So ist die Logik auch von der GUI aus nutzbar (Phase 7, „Stimme des angeschauten Villagers ändern“).

## Tests
- Brigadier-Parsing per GameTest mit `CommandSourceStack` auf einem Test-Server.
- `voice set` bei einem Baby mit einer Erwachsenenstimme ohne `force` → Fehlermeldung.

## Ergebnis
Alle Commands funktionieren auf Dedicated Server und im Singleplayer (mit Cheats), mit Tab-Completion.
