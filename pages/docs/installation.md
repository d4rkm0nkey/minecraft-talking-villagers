---
icon: tv/map
---

# Installation

## Requirements

| | |
| --- | --- |
| Minecraft | **26.3** |
| Java | **25** (the Minecraft launcher brings the right version) |
| Mod loader | **Fabric** (with Fabric API) **or NeoForge** |
| Recommended | [Mod Menu](https://modrinth.com/mod/modmenu) on Fabric, for the voicepack screen |

In singleplayer you only need the mod in your game. On a server, the mod has to be on
the **server** for anyone to hear voices, see [Servers and multiplayer](playing/servers.md).

## Fabric

1. Install the [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3
   (version 0.19.5 or later).
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3
   (version 0.162.0+26.3 or later) and, recommended, [Mod Menu](https://modrinth.com/mod/modmenu).
3. Download the Talking Villagers jar for Fabric.
4. Put all jars into the `mods` folder of your Minecraft directory:
    - Windows: `%appdata%\.minecraft\mods`
    - macOS: `~/Library/Application Support/minecraft/mods`
    - Linux: `~/.minecraft/mods`
5. Start Minecraft with the *Fabric Loader* profile.

## NeoForge

1. Install [NeoForge](https://neoforged.net/) for Minecraft 26.3 (version 26.3.0.58-beta
   or later).
2. Download the Talking Villagers jar for NeoForge.
3. Put it into the `mods` folder (see above).
4. Start Minecraft with the *NeoForge* profile.

!!! tip "Launchers"
    With Prism Launcher, the Modrinth App or the CurseForge app, create an instance for
    Minecraft 26.3 with Fabric or NeoForge and add the mod there. Remember Fabric API on
    Fabric.

## A voicepack

The mod itself has no voices. Without a voicepack, everything sounds like vanilla.
Get the default pack *TeaJay Default* from the
[releases](https://github.com/d4rkm0nkey/minecraft-talking-villagers/releases), or
[record your own](making/web-editor.md). How to install it is on the next page,
[Your first voices](getting-started.md).

## Dedicated server

Install the same loader on the server, put the jar (and Fabric API on Fabric) into the
server's `mods` folder and put the voicepacks into `<server>/voicepacks/`. Then restart,
or run `/tv reload`. See [Servers and multiplayer](playing/servers.md).

## Updating

Replace the old jar with the new one. Voicepacks and the voices your villagers have are
kept. During the beta, **back up your world first**.

Coming from version 1 of the mod? Read [Coming from version 1](playing/version-1.md).

## Uninstalling

Remove the jar. Villagers simply sound like vanilla again; the voices they had are stored
with them and come back if you install the mod again.
