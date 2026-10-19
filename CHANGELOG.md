# Changelog

## 2.0.0-beta.2 – unreleased

### Security
- The client only accepts SHA-256 checksums from a server. Before, a server could make the client write and delete `.part` files outside the download cache.
- Pack versions may only contain `A-Z`, `a-z`, `0-9`, `.`, `_`, `+` and `-`. Keeping a pack downloaded from a server can no longer write outside the voicepacks folder.

### Fixed
- Villagers say their `sleep` line when they go to bed. It never played before.
- A pack with a very long name or version no longer disconnects every player with the mod. Packs now follow the length limits of the schema (name 64, version 32, description 1024, subtitle 256 characters); longer values are an error.
- Web editor: lines longer than 60 seconds are reported and can't be exported, and very short lines no longer get a duration of 0. Both made the mod reject the whole pack.
- Web editor: checks pack versions and blank names like the mod.

## 2.0.0-beta.1 – 2026-10-15

Complete rewrite for Minecraft 26.3 on Fabric and NeoForge. First beta: please report bugs on GitHub.

The default voicepack *TeaJay Default* 0.1.0 is a separate download, attached to this release.

### Added
- **Voicepacks** as single `.voicepack` files in `.minecraft/voicepacks/`, with a JSON schema. No resource pack or config file per voice any more.
- **Voicepack manager** (Mod Menu / NeoForge mod list): install by drag and drop, enable, disable and delete packs, browse voices, events and lines with search, preview every line, settings screen.
- **Web editor** for creating and editing packs in the browser, with recording studio, long takes cut at pauses, noise suppression (RNNoise), noise profile, compressor, normalization and export.
- **New entities:** wandering trader, witch, pillager, vindicator, evoker, illusioner.
- **Situations:** villagers panic, warn of raids, whisper while hiding, talk about their work, greet depending on reputation, react to deaths, cures and babies, and cheer on players fighting monsters. Witches and illagers talk differently when idle, on patrol, in a raid or in combat, and react to low health, lost targets, victories and fallen allies. 67 events with fallbacks, so small packs still work everywhere.
- **Male, female and neutral voices**, with a configurable share.
- **Child voices:** babies only get child voices and a new adult voice (with a `grow_up` line) when they grow up.
- **Languages:** one language per pack or voice; optional language per village; children inherit a parent's language, villagers keep theirs when they move; settings for traders and raids.
- **Commands** `/talkingvillagers` (`/tv`): show and change voices, genders and languages, let entities speak, watch spoken lines.
- **Multiplayer:** the server is authoritative. Players without the server's packs are offered a download (through the game connection with a bandwidth limit, or from a web server). Players without the mod hear vanilla sounds.
- **Migration:** version 1 resource packs can be converted in game and in the web editor; villagers in old worlds keep their voice through `legacyIds`. With the new default pack they get a new voice.
- Subtitles with the spoken text, client volume setting, limit for entities talking at the same time.

### Changed
- Voices are stored per entity with pitch, gender, age and language and survive reloads; packs can be updated without villagers losing their voice.
- Event names: `nope` → `no`, `level` → `level_up`, `herodrop` → `hero_gift`, `tradesuccess` → `trade_success`.

### Removed
- Python scripts and the `config/talkingvillagers/voices/*.json` files of version 1.

## 1.x

See the [version 1 branch](https://github.com/d4rkm0nkey/minecraft-talking-villagers/tree/v1).
