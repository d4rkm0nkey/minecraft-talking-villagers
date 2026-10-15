---
icon: tv/compass
---

# Languages and villages

Every voicepack has a **language**, e.g. `en`, `de` or `en-GB`. A single voice can speak
another language than its pack. With packs in several languages installed, you decide
how they are mixed.

## Language modes

Set in *Settings → Language (world)* or as `languageMode` in the
[server config](../configuration.md).

| Mode | What happens |
| --- | --- |
| **Any** (`any`) | Every voice can be used, whatever its language. The default. |
| **Preferred** (`preferred`) | Voices in the *preferred languages* are used where possible, others only when no preferred voice fits. |
| **Per village** (`per_village`) | Every village gets a random language that all its villagers speak. |

Two more settings narrow it down:

- **Allowed languages** (`allowedLanguages`): only these languages are used at all. Empty
  means all.
- **Only voices in that language** (`strictLanguage`): rather vanilla sounds than a voice
  in another language, e.g. when no child voice in the village's language exists.

## Villages

In *per village* mode, a village is everything within `villageRadius` blocks (default 64)
of its meeting point, the bell. The first villager there gets a random language, and that
becomes the language of the village: everyone else who belongs to the same bell speaks it
too.

- Villagers keep their language when they **move** to another village.
- **Children** speak the language of one of their parents, wherever they are born.
- Show or change a village's language with commands:

```
/tv language village get
/tv language village set de
/tv language village set de ~ ~ ~ reassign
```

`reassign` also gives all villagers of that village new voices in the new language.
A single entity's language can be set with `/tv language entity set <entities> <language>`.

## Strangers

| Setting | Options |
| --- | --- |
| *Wandering trader* (`traderLanguage`) | `random` language, or the language of the `nearest_village`. |
| *Raids* (`raidLanguage`) | `random`, or `opposite_of_village`: raiders speak another language than the village they attack. |

In *per village* mode, everybody near the same witch hut, outpost or mansion shares a
language, and all raiders of one raid speak the same language.

## Making packs in several languages

Make one pack per language. The [web editor](../making/web-editor.md) asks for the
language when you create a pack; a voice can override it with *Different language*.
