---
icon: tv/head-witch
entities: [witch]
---

# Witch

Witches mutter to themselves, talk to you while they throw potions at you and laugh when
something falls to them.

## On her own

When no player is around, a witch talks to herself (`idle`) or over her brew (`brew`).
During a raid she has her own lines while she has no target (`raid_idle`).

[[say: witch | What a beautiful day... makes me sick!]]

## In combat

- **Combat taunts** every 8 to 15 seconds while she fights a player (`combat`).
- **Potions:** `throw_potion`, `drink_potion`, and more specific ones: healing herself
  (`heal_self`), fire resistance, water breathing or speed (`drink_other`), and healing
  an allied raider (`heal_ally`).
- **Low health** below 30 % (`low_health`, once until she is healed above 60 %).
- **Victory** when she kills a player, `laugh` when she kills anything else, `lost_target`
  when you escape.
- When a witch or illager within 16 blocks dies: `ally_died`.

## Female by default

Witches get female voices unless you change `witchFemaleChance`. Neutral voices fit them
too.

## All witch events

[[events: witch]]
