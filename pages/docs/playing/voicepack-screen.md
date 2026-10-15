---
icon: tv/writable_book
---

# The voicepack screen

All voicepacks are managed in game. Open it with *Mods → Talking Villagers → Configure*
(Fabric with [Mod Menu](https://modrinth.com/mod/modmenu)) or *Mods → Talking Villagers →
Config* (NeoForge).

## The list

Every installed pack is listed with its icon, name, version, language and number of
voices.

- **On / Off** turns a pack on or off without deleting it.
- **Details** shows the voices of a pack and their lines.
- **Delete** moves the file to `voicepacks/.trash`, from where you can restore it.
- **Search…** filters the list. The line lists of a voice have their own search, by text
  and file name.
- **Open folder** opens `.minecraft/voicepacks/`, **Reload** loads the folder again.
- **Web editor** opens the [web editor](../making/web-editor.md) in your browser.
- **Settings** opens the [settings](#settings).

Broken packs stay in the list with the reason, e.g. a missing file. They never stop the
game from starting. If two files contain the same pack, the newer version wins; the older
one is marked *(a newer version is installed)*.

## Installing

Drag one or more `.voicepack` files into the window. The screen lists what it will
install and which old versions it replaces, and asks *Install these voicepacks?*.

A ZIP from version 1 of the mod can be dragged in too: it is converted into a new pack,
see [Coming from version 1](version-1.md).

## Listening

*Details* lists each voice with its entities, gender and language. Open a voice to see
all its events, with a short explanation of when each one happens and which event is used
otherwise. Click a line to hear it – no world needed.

## Settings

*For you* – only on your computer:

| Setting | |
| --- | --- |
| Voice volume | Volume of all voices. |
| Subtitles | *Spoken text*, *"Villager speaks"* or *Off*. Needs Minecraft's subtitles on. |

*Voices (world)* and *Language (world)* – stored in the server config. In singleplayer
you can change them here; on a server they are *Decided by the server you are playing on*.

| Setting | |
| --- | --- |
| Share of women | Share of female voices. |
| Share of female witches | The same for witches. |
| Children without child voice | *Vanilla sounds* or *Adult voice, higher*. |
| Speaking at once (32 blocks) | How many entities close to each other may talk at the same time. |
| Who talks | Turn single entities off, e.g. witches. |
| Language, preferred and allowed languages, … | See [Languages and villages](../talking/languages.md). |

All settings are also in the [config files](../configuration.md).

## On a server

When you play on a server, the screen also shows:

- *Used by this server, but missing here*: packs the server uses that you do not have.
  **Download** gets them.
- *From servers*: packs downloaded while playing. They are only used on the server that
  uses them; **Install permanently** copies one into your own folder.
- *This server does not have Talking Villagers*: voices only work when the mod is on the
  server.
