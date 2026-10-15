---
icon: tv/command_block_minecart
---

# Commands

All commands start with `/talkingvillagers`, or short `/tv`. Commands that change
something need operator rights. Entity arguments are the usual selectors, e.g.
`@e[type=villager,limit=1,sort=nearest]`; tab completion suggests voices and events.

## Voicepacks

| Command | |
| --- | --- |
| `/tv packs list` | Installed packs, with inactive and broken ones. |
| `/tv packs info <pack>` | The voices of a pack: entities, gender, age, language, number of lines. |
| `/tv packs reload` | Reload the voicepacks. |
| `/tv reload` | Reload the config and the voicepacks. |

## Voices

| Command | |
| --- | --- |
| `/tv voice get <entity>` | Which voice an entity has, with gender, pitch and language. Click *[▶ play]* to hear it. |
| `/tv voice set <entities> <pack:voice> [force]` | Give a voice. `force` ignores entity type, gender and age. |
| `/tv voice randomize <entities> [newGender]` | Pick a new random voice; `newGender` may also change the gender. |
| `/tv voice clear <entities>` | Mute: back to vanilla sounds. |
| `/tv voice say <entity> <event> [line]` | Let an entity say a line of an event; `line` picks a certain one, counting from 0. |

## Gender and language

| Command | |
| --- | --- |
| `/tv gender get <entity>` | Male, female or neutral. |
| `/tv gender set <entities> <male\|female\|neutral>` | Set the gender; the entity gets a fitting voice. |
| `/tv language village get [pos]` | The language of the village at a position. |
| `/tv language village set <language> [pos] [reassign]` | Set it. `reassign` gives the village's villagers new voices in that language. |
| `/tv language entity set <entities> <language>` | Set the language of single entities. |

Languages are tags like `en`, `de` or `en-GB`. See
[Languages and villages](talking/languages.md).

## Debugging

| Command | |
| --- | --- |
| `/tv debug watch` | Show every spoken line in chat (or in the server log). Run again to stop. |
| `/tv debug stats` | Which voices the entities in this dimension use. |
| `/tv debug say <entities> <pack:voice> <event>` | Play a line of any voice from these entities, without changing their voice. |

## Examples

```
# what does the villager next to me sound like?
/tv voice get @e[type=villager,limit=1,sort=nearest]

# let all witches in a radius of 32 blocks laugh
/tv voice say @e[type=witch,distance=..32,limit=1] laugh

# a German village
/tv language village set de ~ ~ ~ reassign
```
