---
icon: tv/book
hide:
  - toc
---

# All events

An **event** is a situation in which an entity says something. A voice has a list of
lines for each event.

When a voice has no line for an event, the event under “Otherwise” is tried, and so on.
A pack with only `ambient`, `hurt` and `death` already works for every entity.

Each voice only reacts to events of its own entities: a witch voice never says `trade`,
even if the pack contains such lines.

[[events]]

“Illagers” are the pillager, vindicator, evoker and illusioner. The pages of the
[villagers](../entities/villagers.md), [wandering trader](../entities/wandering-trader.md),
[witch](../entities/witch.md) and [illagers](../entities/illagers.md) explain the events
in context.

!!! info "Renamed in version 2"
    `nope` is now `no`, `level` is `level_up`, `herodrop` is `hero_gift` and
    `tradesuccess` is `trade_success`.
