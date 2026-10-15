---
icon: tv/barrier
---

# No voices?

Go through these points in order.

## Is a voicepack installed?

The mod has no voices of its own. Open the [voicepack screen](voicepack-screen.md): is
your pack listed and *On*? Without a pack, everything sounds like vanilla.

## Is the pack broken?

Broken packs are listed with the reason, e.g. a missing sound file or an invalid
manifest. `/tv packs list` shows them too. Export the pack again from the web editor, or
check the [format](../making/format.md).

## Does the voice fit the entity?

A pack with only villager voices leaves witches and illagers vanilla. *Details* shows
which entities each voice is made for. Also check:

- **Babies** only get child voices. Without one, they stay vanilla unless *Children
  without child voice* is set to *Adult voice, higher*.
- **Gender:** with *Share of women* at 100 %, only female and neutral voices are used.
- **Language:** with *Only voices in that language* on, entities without a voice in their
  language stay vanilla.
- **Who talks:** single entities may be turned off.

## On a server

- The mod must be on the **server**. With the mod only on your side, villagers stay
  vanilla.
- You need the **same pack file** as the server. Download it when you are asked, or from
  the voicepack screen.
- Ask an operator to run `/tv packs list` and `/tv debug watch`: do lines get said at all?

## Some lines are never said

- The entity has no line for that event and the fallback chain ends without one. See
  [All events](../making/events.md).
- Only a few entities talk at once (*Speaking at once*, 3 within 32 blocks by default).
- Greetings come at most every five minutes per villager.

Try a line directly: `/tv voice say @e[type=villager,limit=1,sort=nearest] greeting`.

## The voice sounds like it is in my head

The sound file is stereo. Minecraft plays stereo sounds without distance. Export mono
files; the web editor always does.

## Still stuck?

Open an [issue](https://github.com/d4rkm0nkey/minecraft-talking-villagers/issues) with
your Minecraft version, loader, mod version and the log (`logs/latest.log`).
