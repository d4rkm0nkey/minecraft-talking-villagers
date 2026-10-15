---
icon: tv/spyglass
---

# Your first voices

This page walks you from a fresh install to a village full of voices.

## 1. Open the voicepack screen

=== "Fabric"

    Open *Mods* in the title screen (needs [Mod Menu](https://modrinth.com/mod/modmenu)),
    select *Talking Villagers* and click the configure button.

=== "NeoForge"

    Open *Mods* in the title screen, select *Talking Villagers* and click *Config*.

The screen is empty for now: *No voicepacks installed yet.*

## 2. Install a voicepack

Drag the `.voicepack` file from your file manager **into the Minecraft window**. The
game asks *Install these voicepacks?* – confirm, and the pack appears in the list.

You can also copy the file into `.minecraft/voicepacks/` yourself and click *Reload*.
*Open folder* opens that folder for you.

!!! tip "Listen first"
    Click *Details* on a pack to see its voices. Every event and every line can be played
    right there, without starting a world.

## 3. Meet the villagers

Load a world and walk into a village. When you come within 5 blocks of a villager, it
greets you – at most every five minutes per villager. Stay a while and you hear small
talk, gossip between two villagers, lines about the weather and about their work.

[[say: farmer | It's a pleasure to see you!]]

Every villager gets a voice from your packs the first time it speaks. It keeps that voice
and a slightly different pitch for the rest of its life, so the village gets a mix of
voices even from a single pack. More about this in
[Voices and personalities](talking/voices.md).

## 4. Turn on subtitles

With Minecraft's subtitles on (*Options → Accessibility → Show Subtitles*), you see what
is said: the spoken text, if the pack has transcripts. The voicepack *Settings* switch
between the spoken text, a generic “Villager speaks” and off.

## 5. Try a few situations

- **Trade** with a villager: it comments on the trade screen and on successful trades.
- **Ring the bell** or wait for a raid: villagers warn each other, run home and whisper
  while they hide.
- **Fight a zombie** near the village: villagers panic, call for help and cheer you on.
- **Visit a witch hut** or meet a pillager patrol: they mutter on their own, taunt you in
  combat and react when they are low on health.

Want to hear one line on demand? Look at a villager and run
`/tv voice say @e[type=villager,limit=1,sort=nearest] greeting`. See [Commands](commands.md).

## Next steps

- [Who talks](entities/villagers.md): what each entity says when.
- [Languages and villages](talking/languages.md): packs in several languages.
- [Servers and multiplayer](playing/servers.md): playing with friends.
- [The web editor](making/web-editor.md): record your own voice.
