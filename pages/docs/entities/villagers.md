---
icon: tv/head-villager
entities: [villager]
---

# Villagers

Villagers have the most to say. They react to you, to each other and to what happens in
the village.

## A day in the village

- **Greetings** when you come close: warm at a good reputation (`greeting_friendly`,
  30 or more), cold at a bad one (`greeting_unfriendly`, −30 or less).
- **Small talk** (`random`) now and then, about the **weather** in clear weather or rain
  (`good_weather`, `bad_weather`), at the **workstation** (`work`), at the **meeting
  point** by the bell (`meet`), and children while they **play** (`play`).
- **Gossip** when two villagers meet and chat.
- **Sleep** and **wake up** lines in the evening and morning.

[[say: librarian | Did you know that books can be enchanted?]]

## Trading

The trade screen opens with a `trade` line (or `trade_unfriendly` at a bad reputation).
Selecting a trade says `yes`, a villager without trades says `no`, and every finished
trade says `trade_success`. A villager that levels up in its profession says `level_up`.

Lines can be bound to **professions**, so the farmer talks about crops and the cleric
about faith. Modded professions work too.

## Danger

- Villagers **panic** when a zombie or a raider shows up (`spot_zombie`, `spot_raider`)
  and call for help while they run (`panic_loop`).
- When the bell rings for a raid, they warn each other (`pre_raid`) and **whisper** while
  they hide (`hide`), at half volume.
- A villager hit by a player complains (`attacked_by_player`).
- When a neighbour within 16 blocks dies, the others react (`villager_died`).

## Cheering

When you fight a monster that a villager can see, the nearest villager within 16 blocks
**cheers you on** (`cheer`, at most every 5 seconds per player) and celebrates when you
kill it (`monster_killed`).

## Life events

- A **Hero of the Village** is greeted specially (`hero`), and villagers throw gifts with
  a line (`hero_gift`).
- Villagers that had a **child** say `love`. The child speaks the language of a parent.
- A villager **cured** from being a zombie thanks you (`cured`).
- A baby that **grows up** gets an adult voice and says `grow_up`, see
  [Children](../talking/voices.md#children).

## All villager events

[[events: villager]]

“Otherwise” is the event used when the voice has no line for this one.
