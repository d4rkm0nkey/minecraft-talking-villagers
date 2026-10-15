---
title: Talking Villagers
hide:
  - navigation
  - toc
---

<div class="tv-hero">
<span class="tv-hero__sun" aria-hidden="true"></span>
<span class="tv-hero__cloud" style="top: 3.4rem; left: 7%; width: 4.8rem" aria-hidden="true"></span>
<span class="tv-hero__cloud" style="top: 6.6rem; left: 70%; width: 3.6rem" aria-hidden="true"></span>
<img class="tv-hero__logo" src="assets/img/icon.png" alt="">
<h1 class="tv-hero__title">Talking Villagers</h1>
<p class="tv-hero__tagline">Villagers, witches, wandering traders and illagers talk – with real recorded voices instead of “hmm”.</p>
<div class="tv-hero__actions">
<a class="md-button md-button--primary" href="installation/">Download</a>
<a class="md-button" href="making/web-editor/">Record your own voice</a>
</div>
<div class="tv-hero__village" aria-hidden="true">
<img class="tv-lantern" src="assets/mc/items/lantern.png" alt="">
<span class="tv-talker" style="--delay: 0s; --bob: 0s"><span class="tv-bubble">It's a pleasure to see you!</span><img src="assets/mc/heads/farmer.png" alt=""></span>
<span class="tv-talker" style="--delay: 3s; --bob: .4s"><span class="tv-bubble">I really like you!</span><img src="assets/mc/heads/librarian.png" alt=""></span>
<span class="tv-talker" style="--delay: 6s; --bob: .9s"><span class="tv-bubble">Who will I torture today?</span><img class="tv-head--witch" src="assets/mc/heads/witch.png" alt=""></span>
<span class="tv-talker" style="--delay: 9s; --bob: .2s"><span class="tv-bubble">Do you want to buy something?</span><img src="assets/mc/heads/wandering_trader.png" alt=""></span>
<div class="tv-bell"><img src="assets/mc/items/bell.png" alt=""></div>
<span class="tv-talker" style="--delay: 12s; --bob: .7s"><span class="tv-bubble">There! Get them!</span><img src="assets/mc/heads/pillager.png" alt=""></span>
<span class="tv-talker" style="--delay: 15s; --bob: 1.1s"><span class="tv-bubble">You look funny!</span><img src="assets/mc/heads/cleric.png" alt=""></span>
<span class="tv-talker" style="--delay: 18s; --bob: .5s"><span class="tv-bubble">Wololo!</span><img src="assets/mc/heads/evoker.png" alt=""></span>
<span class="tv-talker" style="--delay: 21s; --bob: .3s"><span class="tv-bubble">See you!</span><img src="assets/mc/heads/fisherman.png" alt=""></span>
<img class="tv-lantern" src="assets/mc/items/lantern.png" alt="">
</div>
</div>

Talking Villagers gives the mobs of a village real voices. They greet you, gossip,
complain about the rain, scream for help when zombies come, whisper while hiding from a
raid – and illagers taunt you in battle. With subtitles of what they say, if you like.

The voices come in **voicepacks**: single `.voicepack` files you drop into the game.
Anyone can record their own in the browser with the [web editor](making/web-editor.md).

!!! warning "Beta"
    Version 2 is a beta for testing. Back up your worlds before trying it. See
    [Beta notes](beta.md).

## Who talks

[[entities]]

The lines on the cards are examples: what an entity says is up to the voicepack.

## How it works in short

<ol class="tv-steps">
<li><span class="mc-slot">:mc-book:</span><strong>Install</strong> Put the mod into your <code>mods</code> folder, on Fabric or NeoForge.</li>
<li><span class="mc-slot">:mc-written_book:</span><strong>Voicepack</strong> Drag a <code>.voicepack</code> file into the voicepack screen.</li>
<li><span class="mc-slot">:mc-name_tag:</span><strong>Voices</strong> Every villager gets its own voice and pitch, and keeps it.</li>
<li><span class="mc-slot">:mc-bell:</span><strong>Listen</strong> Walk through a village: they greet you, chat and react to what happens.</li>
<li><span class="mc-slot">:mc-goat_horn:</span><strong>Record</strong> Make your own pack in the web editor and share it.</li>
</ol>

New here? Start with [Installation](installation.md) and then
[Your first voices](getting-started.md).

## Features at a glance

- **Seven entities:** villagers, wandering traders, witches, pillagers, vindicators,
  evokers and illusioners, in [67 situations](making/events.md) from `greeting` and
  `trade` to `panic`, `hide`, `combat`, `patrol` and `wololo`.
- **Male, female and child voices.** Babies only get child voices and a new adult voice
  when they grow up. Every entity keeps its voice and a personal pitch.
- **Reacts to the situation:** your reputation (warm or cold greetings), raids, zombies,
  profession, weather and time of day.
- **[Languages](talking/languages.md):** each pack has a language. Every village can
  speak its own one, children inherit their parents' language.
- **[Voicepack screen](playing/voicepack-screen.md) in game:** install by drag and drop,
  turn packs on and off, listen to every line, change the settings.
- **[Multiplayer](playing/servers.md):** the server decides who says what. Players who
  lack the server's packs are offered a download when they join; players without the mod
  hear vanilla sounds.
- **[Commands](commands.md)** to change voices, genders and languages and to let entities
  say a line.
- **[Web editor](making/web-editor.md)** with recording studio, noise removal and export,
  right in the browser.
- Works on **Fabric and NeoForge**, Minecraft 26.3, in singleplayer and on servers.
