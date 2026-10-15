---
icon: tv/head-wandering_trader
entities: [wandering_trader]
---

# Wandering trader

The wandering trader greets you like a villager and talks while trading, but makes no
small talk of its own.

- **Greeting** when you come close.
- **Trading:** `trade` when the screen opens, `yes` and `no`, and `trade_success`.
- **Potions:** `drink_potion` when it drinks milk or a potion, and `disappear` when it
  drinks the invisibility potion at night.

[[say: wandering_trader | Do you want to buy something?]]

## Its language

Wandering traders come from far away. By default they speak a random language of your
packs; with `traderLanguage: nearest_village` they speak the language of the nearest
village instead. See [Languages and villages](../talking/languages.md).

## All events of the wandering trader

[[events: wandering_trader]]
