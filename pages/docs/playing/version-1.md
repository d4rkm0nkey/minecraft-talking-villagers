---
icon: tv/clock
---

# Coming from version 1

Version 2 is a complete rewrite. Voices are no longer resource packs with config files,
but single `.voicepack` files.

## Old voice packs

Version 1 voices came as a resource pack ZIP. Drag that ZIP into the
[voicepack screen](voicepack-screen.md): it offers to **convert** it. You choose the name
of the new pack, its language and the gender of the voices; voices, professions and
subtitles are taken over.

The [web editor](../making/web-editor.md) can import version 1 packs too, if you want to
add lines or fix them before you convert.

!!! note
    Version 1 did not store transcripts for most lines, so converted packs often have
    no subtitle text. You can add it in the web editor.

## Old worlds

Villagers in old worlds keep their voice if the new pack lists the old voice name in
`legacyIds`. The converted packs and the default pack do.

## What you can delete

- The old files in `config/talkingvillagers/`.
- The old voice resource pack, once it is converted.
- The Python scripts of version 1.

## Renamed events

| Version 1 | Version 2 |
| --- | --- |
| `nope` | `no` |
| `level` | `level_up` |
| `herodrop` | `hero_gift` |
| `tradesuccess` | `trade_success` |

Version 1 stays available on the
[v1 branch](https://github.com/d4rkm0nkey/minecraft-talking-villagers/tree/v1).
