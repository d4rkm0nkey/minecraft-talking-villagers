---
icon: tv/knowledge_book
---

# Recording good voices

[[say: cleric | Speak clearly, my child, and close to the microphone.]]

## Variety

- **3–5 variants per event.** Events that happen often – `hurt`, `ambient`, `random` –
  need more, or players hear the same line again and again.
- Start with `ambient`, `hurt`, `death` and `greeting`. Thanks to the
  [fallbacks](events.md), a pack with only `ambient`, `hurt` and `death` already works for
  every entity.
- Record `yes` and `no` for villagers and traders (and `drink_potion` and `disappear` for
  traders). These events replace vanilla sounds; without lines, the vanilla “hmm” comes
  through between your voice's lines.
- Give lines **professions**: the farmer talks about crops, the librarian about books.
  Profession lines are said half of the time, general lines the other half.

## Personality

Give each voice a character – grumpy, cheerful, shy, greedy, overworked – and let it show
in every line. A grumpy villager greets you differently from a cheerful one, and an
unfriendly greeting (`greeting_unfriendly`) can be really rude.

## Short lines

Most lines should be **1–3 seconds**. Small talk can be longer, but long lines get
interrupted more often. Combat taunts and `hurt` should be very short.

## Sound quality

- Record **close to the microphone** in a quiet room with soft furniture.
- Avoid **clipping** – the studio warns you – and reverb.
- **No effects:** do not add pitch shifting or reverb; the mod varies the pitch itself.
- **Mono:** the editor exports mono, so the voice comes from the villager and not from
  inside your head.

## Children

Child voices are best recorded by children or with a naturally higher voice. If a pack
has no child voices, servers can play adult voices with a higher pitch
(`childFallback`).

## Witches and illagers

They have their own events like `combat`, `patrol`, `spot_player`, `cast_spell` or
`brew`. Illagers on patrol talk among themselves, the captain gives orders, and the evoker
of course needs a `wololo`. See [Illagers](../entities/illagers.md) and
[Witch](../entities/witch.md).

## Languages

One pack per language. A server with packs in several languages can give every village
its own language, see [Languages and villages](../talking/languages.md).

## Subtitles

Write the **script** in the studio: the text of each line becomes its subtitle. Players
with subtitles on read what your villagers say.
