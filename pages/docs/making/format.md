---
icon: tv/paper
---

# Voicepack format

This page is for people who build packs by hand or with their own tools. The
[web editor](web-editor.md) writes this format for you.

A voicepack is a single file with the extension `.voicepack`. It is a ZIP archive with
this layout:

```
my-pack.voicepack
├── voicepack.json      the manifest (required, at most 1 MiB)
├── icon.png            optional, square, shown in the voicepack screen
└── sounds/
    └── anna/greeting/hello.ogg
```

The JSON schema [`schema/voicepack.schema.json`](https://github.com/d4rkm0nkey/minecraft-talking-villagers/blob/master/schema/voicepack.schema.json) is the
reference. Editors like VS Code validate the manifest when it starts with `"$schema"`.

## Manifest

```json
{
  "format": 2,
  "id": "example-pack",
  "name": "Example Pack",
  "version": "1.0.0",
  "description": "Optional, shown in the voicepack screen.",
  "authors": ["TeaJay"],
  "language": "de",
  "voices": [
    {
      "id": "anna",
      "name": "Anna",
      "entities": ["villager", "wandering_trader"],
      "gender": "female",
      "pitch": { "min": 0.9, "max": 1.1 },
      "lines": {
        "hurt": [{ "file": "sounds/anna/hurt/au.ogg" }],
        "greeting": [
          { "file": "sounds/anna/greeting/hallo.ogg", "text": "Hallo, Reisender!" },
          {
            "file": "sounds/anna/greeting/felder.ogg",
            "text": "Schön, dass du die Felder besuchst.",
            "conditions": { "professions": ["farmer"], "weather": "clear" }
          }
        ]
      }
    }
  ]
}
```

A complete example is [`schema/examples/voicepack.json`](https://github.com/d4rkm0nkey/minecraft-talking-villagers/blob/master/schema/examples/voicepack.json).

### Pack

| Field | Required | Meaning |
|---|---|---|
| `format` | yes | Always `2`. |
| `id` | yes | Unique pack id, `a-z`, `0-9`, `_`, `-`, at most 64 characters. Two files with the same id are versions of the same pack; the newest version wins. |
| `name` | yes | Display name, at most 64 characters. |
| `version` | yes | e.g. `1.2.0`. Compared like semantic versions. |
| `description` | no | At most 1024 characters. |
| `authors` | no | List of names. |
| `language` | yes | Language of the voices as a BCP 47 tag: `en`, `de`, `en-GB`, … |
| `voices` | yes | At least one voice. |

### Voice

| Field | Required | Meaning |
|---|---|---|
| `id` | yes | Unique within the pack. Entities remember their voice as `<pack id>:<voice id>`, so do not rename voices in updates. |
| `name` | yes | Display name. |
| `entities` | yes | Any of `villager`, `wandering_trader`, `witch`, `pillager`, `vindicator`, `evoker`, `illusioner`. `illager` is short for the four illagers. |
| `gender` | yes | `male`, `female` or `neutral`. Neutral voices fit everyone. |
| `child` | no | `true` for child voices. Baby villagers only get child voices and get a new adult voice when they grow up. Default `false`. |
| `language` | no | Overrides the pack language for this voice. |
| `pitch` | no | `{ "min": 0.9, "max": 1.1 }`. Each entity gets a fixed random pitch from this range (0.5–2.0), so two villagers with the same voice still sound a little different. |
| `weight` | no | 1–100, default 1. Relative chance of being picked. |
| `lines` | yes | Map from event to a list of lines. Unknown events are a warning, not an error. |
| `legacyIds` | no | Voice names from version 1 of the mod, so villagers in old worlds keep their voice. |

### Line

| Field | Required | Meaning |
|---|---|---|
| `file` | yes | Path inside the archive, below `sounds/`, lower case, ending in `.ogg`. |
| `text` | no | Transcript, shown as subtitle. At most 256 characters. |
| `duration` | no | Length in seconds. Filled in automatically when the pack is loaded. |
| `conditions.professions` | no | Only for villagers with one of these professions, e.g. `["farmer", "librarian"]`. Vanilla professions without namespace, modded ones as `modid:profession`; `none` and `nitwit` work too. |
| `conditions.weather` | no | `clear`, `rain` or `thunder`. |

Lines with conditions are only said when all conditions match. If both matching conditional lines and lines without conditions exist, each group is picked half of the time.

## Audio

- Ogg Vorbis, mono. Stereo files play without distance attenuation in Minecraft, so they sound as if the villager was inside your head.
- 44.1 or 48 kHz; 22.05 kHz is fine for small packs.
- Normalize to about −1 dBFS peak and cut silence at the start and end. The web editor does both.
- Lines longer than a few seconds work but get interrupted more often. Files larger than 1 MiB produce a warning.

## Events

The events and their fallbacks are listed on [All events](events.md). Unknown events are
a warning, not an error, so packs made for newer versions still load.

## Loading rules

- Packs live in `.minecraft/voicepacks/` (client and server). Packs downloaded from servers live in `voicepacks/.server-cache/` and are only used while playing on that server.
- On a server, the server decides which voices entities get and which line they say. Clients only play the line, and only if they have exactly the same pack file (same SHA-256). Otherwise they hear the vanilla sound.
- Broken packs are listed in the voicepack screen with the reason; they never stop the game from loading.
