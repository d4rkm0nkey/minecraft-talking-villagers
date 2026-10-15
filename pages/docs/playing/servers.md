---
icon: tv/bell
---

# Servers and multiplayer

On a server, **the server decides** which voices entities get and which line they say.
Players only play the line.

## Setting up a server

1. Install the mod on the server (and Fabric API on Fabric).
2. Put the voicepacks into `<server>/voicepacks/`.
3. Restart, or run `/tv reload`.

The settings are in `config/talkingvillagers-server.json` on the server, see
[Configuration](../configuration.md).

## Who hears what

| Player has | Hears |
| --- | --- |
| The mod and the same pack | The voices. |
| The mod, but not the pack | Vanilla sounds, and gets the pack offered for download. |
| No mod | Vanilla sounds. Players without the mod can still join. |

A player needs exactly the same pack file as the server (same SHA-256). A different
version of the pack does not count.

If the mod is only on the client, villagers stay vanilla on that server. The voicepack
screen says so.

## Downloading packs from the server

Players with the mod who lack some of the server's packs are asked when they join:

- **Download** – downloads them now.
- **Not now** – asks again next time.
- **Always download from this server** – remembers the server (`trustedServers`) and
  downloads without asking from now on.

Downloaded packs are stored in `voicepacks/.server-cache/` and only used on servers that
use them. *Install permanently* in the voicepack screen copies them into your own folder.

### Settings for the download

| Key in `packDownload` | Default | |
| --- | --- | --- |
| `enabled` | `true` | Offer the server's packs to players. |
| `url` | `""` | A web server with the pack files. Clients then download `<url>/<file name>` over HTTP(S) instead of through the game connection. |
| `kbpsPerPlayer` | `2048` | Bandwidth per player when sending packs through the game. |
| `maxPackSizeMb` | `100` | Larger packs are not offered. |

!!! tip "Large packs"
    Packs with many lines can be tens of megabytes. Put them on a web server and set
    `packDownload.url`, so downloads do not slow down the game connection.

## Operators

All [commands](../commands.md) that change something need operator rights. Useful on a
server:

- `/tv packs list` – which packs the server has loaded.
- `/tv debug watch` – shows every spoken line in chat (or in the server log).
- `/tv debug stats` – which voices the entities in this dimension use.
