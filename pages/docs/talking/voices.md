---
icon: tv/name_tag
---

# Voices and personalities

A voicepack contains one or more **voices**: a farmer couple, their child, the local
witch. Each voice is made for certain entities and has a gender. The mod hands the voices
out to the entities in your world.

## Who gets which voice

The first time an entity speaks, it gets a voice from the installed packs:

- Only voices made for its kind: a witch never gets a villager voice.
- **Gender:** a share of the villagers get female voices, the rest male ones
  (`femaleChance`, default half). Witches are female by default (`witchFemaleChance`).
  **Neutral** voices fit everyone.
- **Neighbours sound different:** the mod counts the voices within 48 blocks and prefers
  one that is used less there, so a village with several voices gets a mix.
- **Weight:** packs can make a voice more or less common than the others.
- **Language:** if you set a language mode, voices in the right language win, see
  [Languages and villages](languages.md).

The voice is stored with the entity. It keeps it through restarts, reloads and pack
updates. If its pack is removed, the entity sounds like vanilla until the pack comes back.

## Pitch

Every entity also gets a **personal pitch** from the voice's pitch range. Two villagers
with the same voice still sound a little different, and each of them always sounds the
same.

## Children

Baby villagers only get **child voices**. When a baby grows up, it gets a new adult voice
and says a `grow_up` line with it.

[[say: villager | Look, I'm all grown up now!]]

If no child voice is installed, babies make vanilla sounds. The setting *Children without
child voice* can instead play an adult voice with a higher pitch (`childFallback`).

## Who talks how often

- Villagers greet a player who comes within 5 blocks, at most every five minutes each.
- Small talk, gossip and work lines come now and then while villagers go about their day.
- When many entities stand together, only a few talk at once: by default 3 within 32
  blocks (`maxConcurrentSpeakers`). Everybody else waits or keeps quiet.
- An event without lines in the voice falls back to a more general one, e.g.
  `greeting_friendly` to `greeting`. See [All events](../making/events.md).

## Lines that fit

Packs can give lines **conditions**: only for farmers, only in the rain. When matching
conditional lines exist, they are said half of the time, the general lines the other half.
So a librarian talks about books now and then, but not all the time.

## Changing voices by hand

Operators can give an entity another voice, gender or language with
[commands](../commands.md):

```
/tv voice get @e[type=villager,limit=1,sort=nearest]
/tv voice set @e[type=villager,limit=1,sort=nearest] teajay-default:teajay
/tv voice randomize @e[type=villager,distance=..32]
/tv voice clear @e[type=witch,limit=1,sort=nearest]
```

`clear` mutes an entity: it keeps its vanilla sounds, and the mod does not give it a new
voice by itself.
