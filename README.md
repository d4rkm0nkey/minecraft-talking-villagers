![Talking Villagers](https://raw.githubusercontent.com/d4rkm0nkey/minecraft-talking-villagers/master/docs/banner.png)
## Description
Villagers are talking different voice lines, like greetings, small-talk and similar.
The mod lets you easily add different voices with resource packs. Every villager gets a random voice from the installed voices.

## Install voices
### Step 1: Client
Add resource pack for the voice into the "resourcepack"-folder in .minecraft.
### Step 2: Client and Server
Add [voicename].json file into `../talkingvillagers/voices/` in the config folder of your server and client.
## Add your own voice lines
### Making a voicepack
Download the python scripts [create_folder_structure.py](https://github.com/d4rkm0nkey/minecraft-talking-villagers/blob/master/scripts/create_folder_structure.py) 
and [generate_json_files.py](https://github.com/d4rkm0nkey/minecraft-talking-villagers/blob/master/scripts/generate_json_files.py).
Execute `python3 create_folder_structure.py [directory]` to create a resourcepack folder structure in directory and type in the name you want to give to your voice. This name is an identifier and should be without spaces and lowercase.
In the `assets/talkingvillagers/sounds/voices/[voicename]` directory that was created, there should be a folder for every profession. To add sounds to all villagers go into the `all` directory.
Inside should be directories for each voice event. Put your sound files into those directories. The sound files need to be in the OGG Vorbis format (.ogg). 

When you have all your sound files in the folders, execute `python3 generate_json_files.py [directory]`. Now there should be automatically three files created. 
`assets/talkingvillagers/sounds.json`, `assets/talkingvillagers/lang/en_us.json` and `[voicename].json`.
Add the transcript of your voice files into the `en_us.json` file and save it. Now compress the `assets`directory together with `pack.mcmeta` into a zip file.

To test the pack put the zip file into your `resourcepacks` folder and the `[voicename].json` file into your `config/talkingvillagers/voices` directory.
Start Minecraft and select the  Resource Pack in the options.
### Voicepack guidelines
Here are some guidelines for making a good voicepack. 
- Make 3-5 variants of each voice lines
- Voice lines that are used very often e.g. death or hurt should have more variants
- Be creative with the random voice lines
- Give your voice a personality with certain attributes that influence all your voice recordings (e.g. happy, positive, depressed, funny, naughty, cheeky, self-confident, selfish, insecure, friendly, shy, overworked, eager, ...)
- Record the voice lines with best possible quality (no reverb, no clipping)
- Use a noise reduction to remove background noise
- Keep the recordings natural and don't add any sound effects like compressor, pitch
### Send in voicepacks
If you made a voicepack you can upload it as a resourcepack and send me the link with this [google form](https://forms.gle/jvBomR4Y7BXMYSiL8).
Good voicepacks will be featured on the CurseForge mod page and I might make a big voicepack with all the voices I like the most.
## Voice Events
- **death:** Sounds played when the villager dies.
- **hurt:** Sounds played when the villager is hurt.
- **random:** Voice lines a villager occasinaly says.
- **gossip:** Sounds played when a villager gossips with another villager.
- **yes:** Voice lines for affirmation.
- **nope:** Voice lines for refusal.
- **celebrate** Voice lines if Villager is happy.
- **level:** Voice lines played when a villager levels up.
- **hero:** Voice lines played when a villager passes a player with the "Hero of the village" effect.
- **herodrop:** Voice lines played when a villager drops a gift for a player with the "Hero of the village" effect.
- **ambient:** Ambient sounds villagers produce.
- **greeting:** Voice lines played when a villager is close to a player and hasn't seen the player for a while.
- **trade** Voice lines played when starting to trade with a villager.
- **trade_success** Voice lines played after trading with a villager.
- **good_weather** Voice lines played occasionally instead of random voiceline, when the weather is sunny.
- **bad_weather** Voice lines played occasionally instead of random voiceline, when the weather is rainy.

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
