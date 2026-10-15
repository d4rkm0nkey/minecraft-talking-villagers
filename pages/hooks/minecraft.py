"""Minecraft markup for the wiki pages.

- ``:mc-emerald:`` an item, block or head icon with a Minecraft tooltip. Heads are called
  ``head_<name>``, e.g. ``:mc-head_witch:``.
- ``[[entities]]`` the cards of the entities that talk, for the home page.
- ``[[events: witch]]`` a table of the events of an entity, with what triggers them and
  their fallbacks; ``[[events]]`` all events with their entities.
- ``[[say: witch | Who will I torture today?]]`` a head with a speech bubble.
- ``entities: [witch]`` in a page's front matter puts the infobox of that entity under the
  page title.

The pictures come from tools/generate_wiki_assets.py, the entities from data/entities.yml,
the events from schema/events.json and their descriptions from the mod's en_us.json, so
the tables always match the code.
"""
import html
import json
import pathlib
import re

import yaml
from mkdocs.exceptions import PluginError
from mkdocs.utils import get_relative_url

DOCS_ASSETS = "assets/mc"

NAMES = {
    "crossbow": "Crossbow",
    "totem_of_undying": "Totem of Undying",
    "head_wandering_trader": "Wandering Trader",
    "command_block_minecart": "Minecart with Command Block",
}

ENTITY_NAMES = {
    "villager": "Villager",
    "wandering_trader": "Wandering trader",
    "witch": "Witch",
    "pillager": "Pillager",
    "vindicator": "Vindicator",
    "evoker": "Evoker",
    "illusioner": "Illusioner",
}
ILLAGERS = ["pillager", "vindicator", "evoker", "illusioner"]

ICON = re.compile(r":mc-([a-z0-9_]+):")
ENTITIES = re.compile(r"^\[\[entities\]\]$", re.M)
EVENTS = re.compile(r"^\[\[events(?::\s*([a-z_]+))?\]\]$", re.M)
SAY = re.compile(r"\[\[say:\s*([a-z_]+)\s*\|\s*(.+?)\s*\]\]")
FENCE = re.compile(r"^(```|~~~).*?^\1", re.M | re.S)

_state = {}


def on_config(config):
    root = pathlib.Path(config.config_file_path).resolve().parent
    repo = root.parent
    docs = pathlib.Path(config.docs_dir)
    # Heads are named head_<name>, the profession names clash with items and blocks.
    _state["pictures"] = {
        (f"head_{path.stem}" if folder == "heads" else path.stem): (folder, path.name)
        for folder in ("items", "blocks", "heads")
        for path in (docs / DOCS_ASSETS / folder).glob("*.png")
    }
    _state["entities"] = yaml.safe_load((root / "data/entities.yml").read_text())
    _state["events"] = json.loads((repo / "schema/events.json").read_text())["events"]
    lang = json.loads((repo / "common/src/main/resources/assets/talkingvillagers/lang/en_us.json").read_text())
    _state["descriptions"] = {
        key.removeprefix("talkingvillagers.event."): value
        for key, value in lang.items() if key.startswith("talkingvillagers.event.")
    }
    return config


def on_page_markdown(markdown, page, config, files):
    ctx = Context(page, files)
    parts, last = [], 0
    for fence in FENCE.finditer(markdown):
        parts.append(ctx.convert(markdown[last:fence.start()]))
        parts.append(fence.group(0))
        last = fence.end()
    parts.append(ctx.convert(markdown[last:]))
    markdown = "".join(parts)

    names = page.meta.get("entities") or []
    if names:
        boxes = "".join(ctx.infobox(name) for name in names)
        markdown = re.sub(r"^(# .*\n)", lambda m: m.group(1) + "\n" + boxes + "\n\n", markdown, count=1, flags=re.M)
    return markdown


def name_of(item):
    if item in NAMES:
        return NAMES[item]
    if item.startswith("head_"):
        return item[5:].replace("_", " ").title()
    return item.replace("_", " ").title()


def entity_list(entities):
    """'villager, witch, illagers' instead of seven names."""
    entities = list(entities)
    if len(entities) == len(ENTITY_NAMES):
        return "all"
    if all(i in entities for i in ILLAGERS):
        entities = [e for e in entities if e not in ILLAGERS] + ["illagers"]
    return ", ".join(entities)


class Context:
    def __init__(self, page, files):
        self.page = page
        self.files = files

    def url(self, path):
        return get_relative_url(path, self.page.url)

    def page_url(self, src):
        file = self.files.get_file_from_path(src)
        if file is None:
            raise PluginError(f"{self.page.file.src_path}: no page {src}")
        return get_relative_url(file.url, self.page.url)

    def image(self, item, css=""):
        picture = _state["pictures"].get(item)
        if picture is None:
            raise PluginError(f"{self.page.file.src_path}: no picture for '{item}'")
        folder, file_name = picture
        cls = f"mc-{folder[:-1]} {css}".strip()
        src = self.url(f"{DOCS_ASSETS}/{folder}/{file_name}")
        return f'<img class="{cls}" src="{src}" alt="{html.escape(name_of(item))}" loading="lazy">'

    def icon(self, item):
        return f'<span class="mc-icon" data-tip="{html.escape(name_of(item))}">{self.image(item)}</span>'

    def text(self, value):
        """Plain text with :mc-...: icons."""
        parts, last = [], 0
        for m in ICON.finditer(str(value)):
            parts.append(html.escape(str(value)[last:m.start()]))
            parts.append(self.icon(m.group(1)))
            last = m.end()
        parts.append(html.escape(str(value)[last:]))
        return "".join(parts)

    def entity(self, name):
        entity = _state["entities"].get(name)
        if entity is None:
            raise PluginError(f"{self.page.file.src_path}: no entity '{name}' in data/entities.yml")
        return entity

    def events_of(self, name):
        return [e for e in _state["events"] if name in e["entities"]]

    def infobox(self, name):
        """An entity, or a group of entities with `heads` and `kinds` instead of `head` and `station`."""
        entity = self.entity(name)
        facts = dict(entity["facts"])
        kinds = entity.get("kinds", [name])
        facts["Events"] = str(len([e for e in _state["events"] if any(k in e["entities"] for k in kinds)]))
        rows = "".join(f"<dt>{html.escape(k)}</dt><dd>{self.text(v)}</dd>" for k, v in facts.items())
        if "heads" in entity:
            art = "".join(self.image("head_" + head, "tv-infobox__head tv-infobox__head--small") for head in entity["heads"])
        else:
            art = (self.image("head_" + entity["head"], "tv-infobox__head tv-head--" + entity["head"])
                   + self.image(entity["station"], "tv-infobox__station"))
        return (
            f'<aside class="tv-infobox" aria-label="{html.escape(entity["name"])} at a glance">'
            f'<div class="tv-infobox__title">{html.escape(entity["name"])}</div>'
            f'<div class="tv-infobox__art">{self.bubble(entity["quote"])}{art}</div>'
            f'<dl>{rows}</dl></aside>'
        )

    def bubble(self, text):
        return f'<span class="tv-bubble">{html.escape(text)}</span>'

    def cards(self, _match):
        cards = []
        for entity in _state["entities"].values():
            if not entity.get("card", True):
                continue
            cards.append(
                f'<a class="tv-card" href="{self.page_url(entity["page"])}">'
                f'<span class="tv-card__art">{self.bubble(entity["quote"])}'
                f'{self.image("head_" + entity["head"], "tv-card__head tv-head--" + entity["head"])}'
                f'{self.image(entity["station"], "tv-card__station")}</span>'
                f'<span class="tv-card__name">{html.escape(entity["name"])}</span>'
                f'<span class="tv-card__text">{html.escape(entity["summary"])}</span></a>'
            )
        return f'<div class="tv-cards">{"".join(cards)}</div>'

    def say(self, match):
        head, text = match.groups()
        return (
            f'<span class="tv-say">{self.image("head_" + head, "tv-say__head tv-head--" + head)}'
            f'<span class="tv-say__bubble">{html.escape(text)}</span></span>'
        )

    def events_table(self, match):
        name = match.group(1)
        if name and name not in ENTITY_NAMES:
            raise PluginError(f"{self.page.file.src_path}: no entity '{name}'")
        events = self.events_of(name) if name else _state["events"]
        rows = []
        for event in events:
            description = _state["descriptions"].get(event["id"], "")
            parent = f'<code>{event["parent"]}</code>' if event.get("parent") else "–"
            who = "" if name else f'<td>{html.escape(entity_list(event["entities"]))}</td>'
            rows.append(
                f'<tr><td><code>{event["id"]}</code></td>{who}'
                f'<td>{html.escape(description)}</td><td>{parent}</td></tr>'
            )
        who_head = "" if name else "<th>Entities</th>"
        return (
            f'<table><thead><tr><th>Event</th>{who_head}<th>When</th><th>Otherwise</th></tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table>'
        )

    def convert(self, text):
        text = ENTITIES.sub(self.cards, text)
        text = EVENTS.sub(self.events_table, text)
        text = SAY.sub(self.say, text)
        return ICON.sub(lambda m: self.icon(m.group(1)), text)
