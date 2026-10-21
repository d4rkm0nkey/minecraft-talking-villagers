---
icon: tv/filled_map
---

# Configuration

Most settings can be changed in game, in the *Settings* of the
[voicepack screen](playing/voicepack-screen.md). They are stored in two files in the
`config` folder. After editing a file by hand, run `/tv reload` or restart.

## Client: `config/talkingvillagers-client.json`

Settings for your own game only.

| Key | Default | |
| --- | --- | --- |
| `volume` | `1.0` | Volume of all voices, 0 to 1. Older values above 1 count as 1. |
| `subtitles` | `transcript` | `transcript` (the spoken text), `generic` (“Villager speaks”) or `off`. |
| `trustedServers` | `[]` | Servers whose packs are downloaded without asking. |

## Server: `config/talkingvillagers-server.json`

Settings that decide which voices entities get. They apply on the server, or in
singleplayer in your own game.

### Voices

| Key | Default | |
| --- | --- | --- |
| `femaleChance` | `0.5` | Share of female voices, 0 to 1. |
| `witchFemaleChance` | `1.0` | Share of female witch voices. |
| `childFallback` | `vanilla` | Babies without child voice: `vanilla` sounds or `pitch_up_adult`. |
| `disabledEntities` | `[]` | Entities that do not talk, e.g. `["witch"]`. |
| `disabledPacks` | `[]` | Pack ids that are not used. |
| `disabledVoices` | `[]` | Single voices as `pack:voice`. |
| `maxConcurrentSpeakers` | `3` | How many entities close to each other may talk at the same time, 1 to 50. |

### Languages

| Key | Default | |
| --- | --- | --- |
| `languageMode` | `any` | `any`, `preferred` or `per_village`. |
| `preferredLanguages` | `[]` | e.g. `["de", "en"]`, for `preferred`. |
| `allowedLanguages` | `[]` | Only these languages; empty means all. |
| `strictLanguage` | `false` | Rather vanilla sounds than a voice in another language. |
| `villageRadius` | `64` | Blocks around a bell that belong to the same village, 8 to 512. |
| `traderLanguage` | `random` | `random` or `nearest_village`. |
| `raidLanguage` | `random` | `random` or `opposite_of_village`. |

See [Languages and villages](talking/languages.md).

### Pack download

| Key | Default | |
| --- | --- | --- |
| `packDownload.enabled` | `true` | Offer the server's packs to players. |
| `packDownload.url` | `""` | Base URL; clients download `<url>/<file name>` over HTTP(S) instead. |
| `packDownload.kbpsPerPlayer` | `2048` | Bandwidth per player when sending packs through the game. |
| `packDownload.maxPackSizeMb` | `100` | Larger packs are not offered. |

See [Servers and multiplayer](playing/servers.md).

## Example

```json
{
  "femaleChance": 0.5,
  "languageMode": "per_village",
  "allowedLanguages": ["de", "en"],
  "raidLanguage": "opposite_of_village",
  "disabledEntities": ["illusioner"],
  "packDownload": {
    "url": "https://example.com/voicepacks"
  }
}
```

Keys you leave out keep their default.

## Folders

| Folder | |
| --- | --- |
| `voicepacks/` | Your voicepacks (client and server). |
| `voicepacks/.server-cache/` | Packs downloaded from servers. |
| `voicepacks/.trash/` | Deleted packs. |
