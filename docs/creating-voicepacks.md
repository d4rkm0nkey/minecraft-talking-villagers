# Creating voicepacks

The easiest way is the **web editor**: <https://d4rkm0nkey.github.io/minecraft-talking-villagers/>. It runs completely in your browser – recordings never leave your computer – and works in current Firefox, Chrome and Edge.

## 1. Create a pack

On the start page enter a name and the language you will speak, then click **Create**. A pack can contain several voices, e.g. a farmer couple, their child and the local witch.

You can also:
- **Open .voicepack** to edit an existing pack. Lines you do not touch are exported unchanged. Use *Increase version* before exporting an update, so players and servers recognize it as newer.
- **Import a version 1 pack** (the old resource pack ZIP). Voices, professions and subtitles are taken over; you only choose the gender.

## 2. Add voices

**Add voice**, then set:
- **For:** which entities may use the voice (villager, wandering trader, witch, illagers).
- **Gender:** male, female or neutral. Servers choose voices so villages get a mix; neutral voices fit everyone.
- **Child voice:** only baby villagers get child voices. When they grow up, they get a new adult voice and say a `grow_up` line with it.
- **Different language:** only if this voice speaks another language than the pack.
- **Pitch:** every entity gets a random pitch from this range, so the same voice sounds slightly different on each villager.

The voice page lists the events of the chosen entities. Each event shows how many variants are recommended and which event is used instead when it has no lines. You can start with `ambient`, `hurt`, `death` and `greeting` and add more later.

## 3. Record

Open the **Recording studio** of a voice.

1. Choose your microphone and click **Record 2 s of room tone** once while being quiet. The editor uses this as noise profile.
2. Write the **script**: one line per recording, optionally with the event in front:
   ```
   greeting: Hello, traveller!
   greeting: Oh, you again.
   hurt: Ouch!
   trade: Let's see what you have.
   ```
3. Click **Start** and read line by line. Keyboard: <kbd>Space</kbd> start/stop · <kbd>Enter</kbd> accept · <kbd>R</kbd> retry · <kbd>P</kbd> listen · <kbd>←</kbd>/<kbd>→</kbd> change line · <kbd>+</kbd> another variant.

Useful options:
- **Stop automatically on silence** and **Start the next recording right after accepting** let you record dozens of lines without touching the mouse.
- **Long take:** read all lines in one go with short pauses; the editor cuts the recording at the pauses and assigns the pieces to the script lines.

You can also add existing audio files (WAV, MP3, Ogg, …) with **Files**.

## 4. Clean up the audio

In the pack settings under **Audio processing**. The settings are applied when listening and exporting; your original recordings stay untouched, so you can change them at any time.

| Setting | Recommended | What it does |
|---|---|---|
| High-pass | on | Removes rumble and hum below 80 Hz. |
| Noise suppression (RNNoise) | on | Neural network that removes fans, keyboard and street noise. |
| Subtract noise profile | on, strength 0.5–0.8 | Removes the constant noise of your room. Needs the room tone. Too strong sounds “underwater”. |
| Noise gate | off | Silences the pauses between words. Useful in noisy rooms. |
| Compressor | off | Evens out loud and quiet lines. |
| Normalize | on | Every line peaks at −1 dB. |
| Remove silence | on | Cuts silence at the start and end. |
| Sample rate / quality | 44.1 kHz / 5 | Lower values make smaller packs. |

## 5. Check and export

**Check** lists problems, e.g. voices without lines or without `hurt` and `death`, or events no entity of the voice ever triggers. **Export as .voicepack** creates the file.

Install it by dragging the file into *Mods → Talking Villagers → Configure* (with Mod Menu on Fabric, or the mod list on NeoForge), or by copying it into `.minecraft/voicepacks/`. On a server, put it into the server's `voicepacks/` folder and run `/tv reload`; players are offered the download when they join.

Test single lines in game with `/tv voice say @e[type=villager,limit=1,sort=nearest] greeting`, or watch every line that is said with `/tv debug watch`.

## Tips for good voices

- **Variety:** 3–5 variants per event; events that happen often (`hurt`, `ambient`, `random`) need more.
- **Personality:** give each voice a character – grumpy, cheerful, shy, greedy, overworked – and let it show in every line.
- **Short lines:** most lines should be 1–3 seconds. Small talk can be longer.
- **Quality:** record close to the microphone in a quiet room with soft furniture. Avoid clipping (the studio warns you) and reverb.
- **No effects:** do not add pitch shifting or reverb; the mod varies the pitch itself.
- **Mono:** the editor exports mono, so the voice comes from the villager and not from inside your head.
- **Children:** child voices are best recorded by children or with a naturally higher voice. If a pack has no child voices, servers can play adult voices with a higher pitch (`childFallback`).
- **Languages:** one pack per language. A server with packs in several languages can give every village its own language.
- **Illagers and witches:** they have their own events like `combat`, `patrol`, `spot_player`, `cast_spell` or `brew`. See the full list in [voicepack-format.md](voicepack-format.md#events).

## Building packs without the editor

A `.voicepack` is a ZIP file with a `voicepack.json` and Ogg Vorbis files. See [voicepack-format.md](voicepack-format.md).
