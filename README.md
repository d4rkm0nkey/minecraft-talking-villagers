![Talking Villagers](https://raw.githubusercontent.com/d4rkm0nkey/minecraft-talking-villagers/master/docs/banner.png)

Villagers, wandering traders, witches and illagers talk – with real recorded voices instead of “hmm”. They greet you, gossip, complain about the rain, scream for help when zombies come, whisper while hiding from a raid, and illagers taunt you in battle.

Voices come in **voicepacks**: single `.voicepack` files you drop into the game. Anyone can record their own in the browser with the [web editor](https://d4rkm0nkey.github.io/minecraft-talking-villagers/editor/).

**Player wiki:** <https://d4rkm0nkey.github.io/minecraft-talking-villagers/>

**Minecraft 26.3 · Fabric and NeoForge · Java 25**

## Features

- **Voices for villagers, wandering traders, witches, pillagers, vindicators, evokers and illusioners**, 67 situations from `greeting` and `trade` to `panic`, `hide`, `cheer`, `combat`, `patrol` and `wololo`.
- **Male, female and child voices.** Babies only get child voices and a new adult voice when they grow up. Every entity keeps its voice and a personal pitch.
- **Reacts to the situation:** reputation (friendly or cold greetings), raids, zombies, profession, weather, time of day.
- **Languages:** each pack has a language. Every village can speak its own language, children inherit the language of a parent, villagers keep their language when they move.
- **Voicepack manager in game:** install by drag and drop, enable/disable packs, listen to every line, settings.
- **Multiplayer:** the server decides who says what. Players without the server's packs get them offered for download when they join. Players without the mod hear vanilla sounds.
- **Commands** to change voices, genders and languages.
- **Web editor** with recording studio, noise removal and export.

## Installation

1. Install the mod (Fabric: also [Fabric API](https://modrinth.com/mod/fabric-api); [Mod Menu](https://modrinth.com/mod/modmenu) is recommended for the settings screen).
2. Get a voicepack, e.g. the default pack *TeaJay Default* from the releases.
3. Start Minecraft, open *Mods → Talking Villagers → Configure* and drag the `.voicepack` file into the window. Or copy it into `.minecraft/voicepacks/`.

Without any voicepack, everything sounds like vanilla.

### Servers

The mod **and** the voicepacks go on the server (into `<server>/voicepacks/`, then `/tv reload` or restart).

- Players with the mod get the server's packs offered when they join (*Download*, *Not now*, *Always for this server*). Downloaded packs are stored in `voicepacks/.server-cache/` and only used on servers that use them; *Install permanently* in the voicepack screen copies them over.
- Players without the mod can join and hear vanilla sounds.
- If the mod is only on the client, villagers stay vanilla – the voicepack screen explains this.

For large packs you can let clients download from a web server instead of through the game connection, see `packDownload.url` below.

### Coming from version 1

- Old voice resource packs can be converted: drag the ZIP into the voicepack screen. You choose language and gender; voices, professions and subtitles are taken over.
- Villagers in old worlds keep their voice if the new pack lists the old name in `legacyIds` (the converted and default packs do).
- The old files in `config/talkingvillagers/` and the resource pack are no longer needed.

## Configuration

In game: *Voicepacks → Settings*. Files:

**`config/talkingvillagers-client.json`**

| Key | Default | |
|---|---|---|
| `volume` | `1.0` | Volume of all voices. |
| `subtitles` | `transcript` | `transcript` (the spoken text), `generic` (“Villager speaks”) or `off`. |
| `trustedServers` | `[]` | Servers whose packs are downloaded without asking. |

**`config/talkingvillagers-server.json`** (on the server, or in singleplayer)

| Key | Default | |
|---|---|---|
| `femaleChance` | `0.5` | Share of female voices. |
| `witchFemaleChance` | `1.0` | Share of female witch voices. |
| `childFallback` | `vanilla` | Babies without child voice: `vanilla` sounds or `pitch_up_adult`. |
| `languageMode` | `any` | `any`, `preferred` (use `preferredLanguages` where possible) or `per_village`. |
| `preferredLanguages` | `[]` | e.g. `["de", "en"]`. |
| `allowedLanguages` | `[]` | Only these languages; empty means all. |
| `strictLanguage` | `false` | Rather vanilla sounds than a voice in another language. |
| `villageRadius` | `64` | Distance in blocks within which bells and villagers belong to the same village. |
| `traderLanguage` | `random` | `random` or `nearest_village`. |
| `raidLanguage` | `random` | `random` or `opposite_of_village`. |
| `disabledEntities` | `[]` | e.g. `["witch"]`. |
| `disabledPacks`, `disabledVoices` | `[]` | Pack ids or `pack:voice`. |
| `maxConcurrentSpeakers` | `3` | How many entities close to each other may talk at the same time. |
| `packDownload.enabled` | `true` | Offer the server's packs to players. |
| `packDownload.url` | `""` | Base URL; clients download `<url>/<file name>` over HTTP(S) instead. |
| `packDownload.kbpsPerPlayer` | `2048` | Bandwidth per player when sending packs through the game. |
| `packDownload.maxPackSizeMb` | `100` | Larger packs are not offered. |

## Commands

`/talkingvillagers` or short `/tv`. Commands that change something need operator rights.

| Command | |
|---|---|
| `/tv packs list`, `/tv packs info <pack>` | Installed packs and their voices. |
| `/tv reload` | Reload config and voicepacks. |
| `/tv voice get <entity>` | Which voice an entity has. |
| `/tv voice set <entities> <pack:voice> [force]` | Give a voice; `force` ignores entity type, gender and age. |
| `/tv voice randomize <entities> [newGender]` | Pick a new random voice. |
| `/tv voice clear <entities>` | Back to vanilla sounds. |
| `/tv voice say <entity> <event> [line]` | Let an entity say a line. |
| `/tv gender get <entity>`, `/tv gender set <entities> <gender>` | |
| `/tv language village get [pos]`, `/tv language village set <language> [pos] [reassign]` | Language of a village; `reassign` gives its villagers new voices. |
| `/tv language entity set <entities> <language>` | |
| `/tv debug watch` | Show every spoken line in chat (or the server log). |
| `/tv debug stats` | Which voices the entities in this dimension use. |

## Making voicepacks

- [Creating voicepacks with the web editor](docs/creating-voicepacks.md)
- [The voicepack format](docs/voicepack-format.md) and its [JSON schema](schema/voicepack.schema.json)

## Building

```
./gradlew build                 # mod jars in fabric/build/libs and neoforge/build/libs
./gradlew :core:buildDefaultPack  # default pack from packs-src/ into core/build/voicepacks
cd web-editor && npm ci && npm run dev
python3 -m venv .venv && .venv/bin/pip install -r pages/requirements.txt   # player wiki
.venv/bin/mkdocs serve -f pages/mkdocs.yml
```

The wiki pictures come from `python3 tools/generate_wiki_assets.py <minecraft client jar>`. The workflow `pages.yml` publishes the wiki at the root of GitHub Pages and the web editor under `/editor/`.

Layout: `core` (plain Java: pack format, selection logic, tests), `common` (Minecraft code shared by both loaders), `fabric`, `neoforge`, `web-editor`, `schema`, `pages` (player wiki).

## License

MIT
