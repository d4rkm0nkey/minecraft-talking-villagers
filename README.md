# Talking Villagers
## Description
Villagers are talking different voice lines, like greetings, small-talk and similar.
The mod lets you easily add different voices with resource packs. Every villager gets a random voice from the installed voices.

## Install voices
### Step 1: Client
Add resource pack for the voice into the "resourcepack"-folder in .minecraft.
### Step 2: Client or Server
Add [voicename].json file into `../talkingvillagers/voices/` in the config folder of your server (or client, if you want to play SinglePlayer).
## Add your own voice lines
**[ToDo]** *Add YouTube tutorial here!*

## Voice Events
- **death:** Sounds played when the villager dies.
- **hurt:** Sounds played when the villager is hurt.
- **random:** Voice lines a villager occasinaly says.
- **gossip:** Sounds played when a villager gossips with another villager.
- **eat:** Voice lines played when a villager eats.
- **yes:** Voice lines for affirmation.
- **no:** Voice lines for refusal.
- **celebrate** Voice lines if Villager is happy.
- **level:** Voice lines played when a villager levels up.
- **hero:** Voice lines played when a villager passes a player with the "Hero of the village" effect.
- **herodrop:** Voice lines played when a villager drops a gift for a player with the "Hero of the village" effect.
- **ambient:** Ambient sounds villagers produce.
- **greeting:** Voice lines played when a villager is close to a player and hasn't seen the player for a while.
- **trade** Voice lines played when starting to trade with a villager.
- **tradesuccess** Voice lines played after trading with a villager.

## Professions
The _all_ voice lines in `config/voices/[voicename].json` can be overwritten if a villager has a specific profession.

Possible professions:
- Nitwit (nitwit)
- Armorer (armorer)
- Butcher (butcher)
- Cartographer (cartographer)
- Cleric (cleric)
- Farmer (farmer)
- Fisherman (fischerman)
- Fletcher (fletcher)
- Leatherworker (leatherworker)
- Librarian (librarian)
- Mason (mason)
- Shepherd (shepard)
- Toolsmith (toolsmith)
- Weaponsmith (weaponsmith)
