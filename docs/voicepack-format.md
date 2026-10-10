# Voicepack format (version 2)

A voicepack is a single file with the extension `.voicepack`. It is a ZIP archive with this layout:

```
my-pack.voicepack
├── voicepack.json      the manifest (required, at most 1 MiB)
├── icon.png            optional, square, shown in the voicepack screen
└── sounds/
    └── anna/greeting/hello.ogg
```

The [web editor](https://d4rkm0nkey.github.io/minecraft-talking-villagers/) writes this format for you, see [creating-voicepacks.md](creating-voicepacks.md). This page is for people who build packs by hand or with their own tools. The JSON schema in [`schema/voicepack.schema.json`](../schema/voicepack.schema.json) is the reference; editors like VS Code validate the manifest when it starts with `"$schema"`.

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

A complete example is [`schema/examples/voicepack.json`](../schema/examples/voicepack.json).

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

When a voice has no line for an event, the event in “Falls back to” is tried, and so on. A pack with only `ambient`, `hurt` and `death` already works for every entity.

| Event | Entities | Falls back to | When |
|---|---|---|---|
| `ambient` | all | – | Replaces the vanilla ambient sound |
| `hurt` | all | – | Takes damage |
| `death` | all | – | Dies |
| `celebrate` | villager, witch, pillager, vindicator, evoker, illusioner | – | Vanilla celebrate sound (raid won or lost) |
| `greeting` | villager, wandering_trader | – | A player comes close (at most every few minutes per villager) |
| `random` | villager | – | Small talk |
| `gossip` | villager | – | Two villagers meet and chat |
| `yes` | villager, wandering_trader | – | Vanilla “yes” sound, e.g. selecting a trade |
| `no` | villager, wandering_trader | – | Vanilla “no” sound, e.g. clicking a villager without trades |
| `trade` | villager, wandering_trader | – | Trade screen opens |
| `trade_success` | villager, wandering_trader | – | A trade was made |
| `level_up` | villager | – | Profession level up |
| `hero` | villager | – | A Hero of the Village is nearby |
| `hero_gift` | villager | – | Throws a gift to a Hero of the Village |
| `good_weather` | villager | – | Small talk in clear weather |
| `bad_weather` | villager | – | Small talk in rain or thunder |
| `sleep` | villager | – | Goes to bed |
| `wake_up` | villager | – | Wakes up |
| `panic` | villager | – | Starts panicking |
| `grow_up` | villager | – | A baby grows up (said with the new adult voice) |
| `drink_potion` | witch, wandering_trader | – | Drinks a potion |
| `throw_potion` | witch | – | Throws a potion |
| `laugh` | witch | – | Witch kills something that is not a player |
| `disappear` | wandering_trader | – | Wandering trader drinks the invisibility potion |
| `spot_player` | illagers | – | Spots a player |
| `attack` | illagers | – | Attacks |
| `charge` | pillager | – | Pillager charges its crossbow |
| `raid_horn` | illagers | – | The raid horn sounds |
| `cast_spell` | evoker, illusioner | – | Casts a spell |
| `summon_vex` | evoker | `cast_spell` | Evoker summons vexes |
| `fangs` | evoker | `cast_spell` | Evoker summons fangs |
| `mirror` | illusioner | `cast_spell` | Illusioner creates mirror images |
| `idle` | witch, illagers | `ambient` | Ambient line without target, raid or patrol |
| `combat` | witch, illagers | `ambient` | Fighting a player |
| `raid_idle` | witch, illagers | `idle` | Ambient line during a raid without target |
| `lost_target` | witch, illagers | – | Combat ends without the target being killed |
| `low_health` | witch, illagers | – | Below 30 % health (once until healed above 60 %) |
| `victory` | witch, illagers | – | Killed a player |
| `ally_died` | witch, illagers | – | A witch or illager within 16 blocks died |
| `patrol` | illagers | `idle` | Ambient line on patrol |
| `patrol_leader` | illagers | `patrol` | Ambient line as patrol captain |
| `charge_at` | vindicator | `spot_player` | Vindicator spots a player |
| `johnny` | vindicator | `combat` | Combat lines of a vindicator named “Johnny” |
| `retreat` | evoker | `combat` | Evoker in combat with a player closer than 8 blocks |
| `wololo` | evoker | – | Evoker turns a blue sheep red |
| `blindness` | illusioner | `cast_spell` | Illusioner casts blindness |
| `brew` | witch | `idle` | Witch idle line |
| `heal_self` | witch | `drink_potion` | Witch drinks a healing potion |
| `drink_other` | witch | `drink_potion` | Witch drinks fire resistance, water breathing or swiftness |
| `heal_ally` | witch | `throw_potion` | Witch throws a healing potion to a raider |
| `panic_loop` | villager | `panic` | Repeated while panicking |
| `pre_raid` | villager | `panic` | Warning when the bell rings for a raid |
| `hide` | villager | – | Whispered while hiding |
| `work` | villager | `random` | Small talk at the workstation |
| `play` | villager | `random` | Small talk of playing children |
| `meet` | villager | `gossip` | Small talk at the meeting point |
| `attacked_by_player` | villager | `hurt` | A player hits it |
| `spot_zombie` | villager | `panic` | Panics because of a zombie |
| `spot_raider` | villager | `panic` | Panics because of a raider |
| `villager_died` | villager | – | A villager within 16 blocks died |
| `cured` | villager | `celebrate` | Cured from being a zombie villager |
| `love` | villager | – | Villagers had a child |
| `greeting_friendly` | villager | `greeting` | Greeting at reputation ≥ 30 |
| `greeting_unfriendly` | villager | `greeting` | Greeting at reputation ≤ −30 |
| `trade_unfriendly` | villager | `trade` | Trade screen opens at reputation ≤ −30 |
| `cheer` | villager | – | A player hits a monster the villager can see (nearest villager within 16 blocks, at most every 5 s per player) |
| `monster_killed` | villager | `cheer` | A player killed a monster the villager can see |

Besides these, each voice reacts to its own entity only: a witch voice never says `trade`, even if the pack contains such lines.

## Loading rules

- Packs live in `.minecraft/voicepacks/` (client and server). Packs downloaded from servers live in `voicepacks/.server-cache/` and are only used while playing on that server.
- On a server, the server decides which voices entities get and which line they say. Clients only play the line, and only if they have exactly the same pack file (same SHA-256). Otherwise they hear the vanilla sound.
- Broken packs are listed in the voicepack screen with the reason; they never stop the game from loading.
