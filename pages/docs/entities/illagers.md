---
icon: tv/head-pillager
entities: [illagers]
---

# Illagers

Pillagers, vindicators, evokers and illusioners share most of their events. A voice can
be made for all four (`illager` in the pack) or for single ones.

## Patrols and raids

- On **patrol**, illagers talk among themselves (`patrol`), and the captain with the
  banner gives orders (`patrol_leader`).
- During a **raid** they have their own lines while they look for a target
  (`raid_idle`), and every wave starts with the raid horn (`raid_horn`).
- Without a target, raid or patrol they talk to themselves (`idle`).
- Won or lost raids end with `celebrate`.

## Combat

- **Spotting** you (`spot_player`), **combat taunts** every 8 to 15 seconds (`combat`) and
  a line on every **attack** (`attack`).
- **Low health** below 30 % (`low_health`), **victory** when they kill a player,
  `lost_target` when you escape, `ally_died` when a witch or illager within 16 blocks dies.

## Each illager

**Pillager** :mc-crossbow:
: Threatens you while it loads its crossbow (`charge`).

**Vindicator** :mc-iron_axe:
: Storms towards you (`charge_at`). A vindicator named **Johnny** with a name tag
  :mc-name_tag: attacks everything and has its own combat lines (`johnny`).

**Evoker** :mc-totem_of_undying:
: Casts spells (`cast_spell`), summons vexes (`summon_vex`) and fangs (`fangs`), backs
  away from players closer than 8 blocks (`retreat`) – and says `wololo` when it turns a
  blue sheep red.

**Illusioner** :mc-bow:
: Creates mirror images (`mirror`) and blinds you (`blindness`).

[[say: evoker | Wololo!]]

## Their language

By default raiders speak a random language. With `raidLanguage: opposite_of_village`,
they speak a different language than the village they attack. See
[Languages and villages](../talking/languages.md).

## All illager events

[[events: evoker]]

This is the evoker's list. The other illagers have the same events, without the
evoker's spells; the pillager has `charge`, the vindicator `charge_at` and `johnny`, the
illusioner `mirror` and `blindness`. The full list is on [All events](../making/events.md).
