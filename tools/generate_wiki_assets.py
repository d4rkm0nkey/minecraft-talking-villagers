#!/usr/bin/env python3
"""Generates the pictures of the wiki (needs Pillow and NumPy).

Usage: python3 tools/generate_wiki_assets.py <minecraft client or merged jar>

Writes into pages/docs/assets/mc/:

- items/<name>.png   16x16 item textures
- blocks/<name>.png  blocks rendered like in the inventory, from their block models
- heads/<name>.png   heads of the entities that talk: villagers with their profession
                     hats, the wandering trader, the witch and the illagers
- ui/<name>.png      pieces of the Minecraft GUI used by the wiki's style sheet

and into pages/overrides/.icons/tv/<name>.svg pixel art icons for the navigation.

The renderer is the one of the Working Villagers wiki: a small orthographic projection of
block model elements and entity boxes, with a depth buffer.
"""
import json
import math
import pathlib
import sys
import zipfile
from io import BytesIO

import numpy
from PIL import Image, ImageDraw

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "pages/docs/assets/mc"
ICONS = ROOT / "pages/overrides/.icons/tv"

# Vanilla item textures copied as they are; "name:block" takes a block texture.
VANILLA_ITEMS = [
    "emerald", "bell", "book", "writable_book", "written_book", "name_tag", "paper", "map",
    "filled_map", "spyglass", "barrier", "clock_00", "compass_16", "goat_horn", "totem_of_undying",
    "crossbow_standby", "iron_axe", "iron_sword", "bow", "glass_bottle", "experience_bottle",
    "cake", "bread", "wheat", "sugar", "redstone", "glowstone_dust", "spider_eye", "egg",
    "music_disc_cat", "ominous_bottle", "torch:block", "poppy:block",
    "command_block_minecart", "knowledge_book", "lantern", "campfire",
]

# Block icons: output name -> model.
BLOCKS = {
    "bookshelf": "minecraft:block/bookshelf",
    "lectern": "minecraft:block/lectern",
    "composter": "minecraft:block/composter",
    "note_block": "minecraft:block/note_block",
    "jukebox": "minecraft:block/jukebox",
    "command_block": "minecraft:block/command_block",
    "crafting_table": "minecraft:block/crafting_table",
    "grass_block": "minecraft:block/grass_block",
    "cauldron": "minecraft:block/cauldron",
    "barrel": "minecraft:block/barrel",
    "cartography_table": "minecraft:block/cartography_table",
    "smithing_table": "minecraft:block/smithing_table",
    "loom": "minecraft:block/loom",
    "brewing_stand": "minecraft:block/brewing_stand",
    "hay_block": "minecraft:block/hay_block",
    "oak_planks": "minecraft:block/oak_planks",
}

# Plains villagers with these vanilla professions; None is an unemployed one.
PROFESSIONS = ["farmer", "librarian", "cleric", "armorer", "fisherman", "cartographer", "shepherd", "butcher", "nitwit"]

# Navigation icons (pixel SVGs) made from item textures.
NAV_ICONS = ["emerald", "bell", "book", "writable_book", "name_tag", "map", "filled_map", "spyglass",
             "barrier", "clock", "compass", "goat_horn", "paper", "knowledge_book", "command_block_minecart"]

GRASS_TINT = (145, 189, 89)
BLOCK_SIZE = 128
SUPERSAMPLE = 4


class Assets:
    def __init__(self, jar_path):
        self.jar = zipfile.ZipFile(jar_path)
        self.names = set(self.jar.namelist())
        self.textures = {}
        self.models = {}

    def read(self, namespace, path):
        name = f"assets/{namespace}/{path}"
        if name in self.names:
            return self.jar.read(name)
        raise FileNotFoundError(name)

    def texture(self, ref):
        """A texture by resource location; animated textures give their first frame."""
        namespace, path = split(ref)
        key = f"{namespace}:{path}"
        if key not in self.textures:
            image = Image.open(BytesIO(self.read(namespace, f"textures/{path}.png"))).convert("RGBA")
            if image.height > image.width:
                image = image.crop((0, 0, image.width, image.width))
            self.textures[key] = image
        return self.textures[key]

    def model(self, ref):
        """A block model with its parents merged: (textures, elements)."""
        namespace, path = split(ref)
        key = f"{namespace}:{path}"
        if key not in self.models:
            data = json.loads(self.read(namespace, f"models/{path}.json"))
            textures, elements = {}, None
            if "parent" in data and not data["parent"].startswith("builtin/"):
                textures, elements = self.model(data["parent"])
                textures = dict(textures)
            textures.update(data.get("textures", {}))
            if "elements" in data:
                elements = data["elements"]
            self.models[key] = (textures, elements)
        return self.models[key]


def split(ref):
    namespace, _, path = ref.rpartition(":")
    return (namespace or "minecraft"), path


def resolve(textures, name):
    seen = set()
    while name.startswith("#"):
        if name in seen:
            return None
        seen.add(name)
        name = textures.get(name[1:])
        if name is None:
            return None
    return name


# --- the renderer -------------------------------------------------------------------

# Camera: looking from the north-west and from above, like the inventory does.
PITCH, YAW = math.radians(30), math.radians(45)
VIEW = (math.cos(PITCH) * math.sin(YAW), -math.sin(PITCH), math.cos(PITCH) * math.cos(YAW))
def _norm(v):
    length = math.sqrt(sum(c * c for c in v))
    return tuple(c / length for c in v)


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return sum(x * y for x, y in zip(a, b))


RIGHT = _norm(_cross(VIEW, (0, 1, 0)))
UP = _cross(RIGHT, VIEW)


def project(p):
    return (_dot(p, RIGHT), -_dot(p, UP))


class Face:
    """A textured parallelogram: corners TL, TR, BL in the world and in texture pixels."""

    def __init__(self, corners, image, tex_points, tint=None):
        self.corners = corners
        self.image = image
        self.tex_points = tex_points
        self.tint = tint
        a, b, c = corners
        self.normal = _norm(_cross(tuple(y - x for x, y in zip(a, c)), tuple(y - x for x, y in zip(a, b))))
        far = tuple(b[i] + c[i] - a[i] for i in range(3))
        self.center = tuple((a[i] + far[i]) / 2 for i in range(3))

    def outline(self):
        a, b, c = self.corners
        return [a, b, tuple(b[i] + c[i] - a[i] for i in range(3)), c]

    def visible(self):
        return _dot(self.normal, VIEW) < -1e-6

    def shade(self):
        nx, ny, nz = self.normal
        return nx * nx * 0.6 + ny * ny * (1.0 if ny > 0 else 0.5) + nz * nz * 0.8


def solve_affine(src, dst):
    """Coefficients (a, b, c, d, e, f) with dst = (a*x + b*y + c, d*x + e*y + f)."""
    (x0, y0), (x1, y1), (x2, y2) = src
    det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    coeffs = []
    for k in range(2):
        v0, v1, v2 = dst[0][k], dst[1][k], dst[2][k]
        a = ((v1 - v0) * (y2 - y0) - (v2 - v0) * (y1 - y0)) / det
        b = ((x1 - x0) * (v2 - v0) - (x2 - x0) * (v1 - v0)) / det
        coeffs += [a, b, v0 - a * x0 - b * y0]
    return coeffs


def render(faces, size=BLOCK_SIZE, bounds=None):
    """Draws the visible faces into a size x size picture, scaled to fit `bounds`
    (world points) or the faces themselves."""
    faces = [f for f in faces if f.visible()]
    points = bounds or [p for f in faces for p in f.outline()]
    projected = [project(p) for p in points]
    min_x, max_x = min(p[0] for p in projected), max(p[0] for p in projected)
    min_y, max_y = min(p[1] for p in projected), max(p[1] for p in projected)
    big = size * SUPERSAMPLE
    margin = big * 0.04
    scale = (big - 2 * margin) / max(max_x - min_x, max_y - min_y)
    offset_x = big / 2 - (min_x + max_x) / 2 * scale
    offset_y = big / 2 - (min_y + max_y) / 2 * scale

    def screen(p):
        x, y = project(p)
        return (x * scale + offset_x, y * scale + offset_y)

    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    # A depth buffer decides what is in front, pixel by pixel: sorting whole faces fails
    # when a small face (a nose, a hat rim) sits in front of a big one. Far faces come
    # first, so of two faces in the same plane (overlays) the later one wins.
    depth = numpy.full((big, big), numpy.inf)
    pixel_x, pixel_y = numpy.meshgrid(numpy.arange(big) + 0.5, numpy.arange(big) + 0.5)
    faces.sort(key=lambda f: -_dot(f.center, VIEW))
    for face in faces:
        tl, tr, bl = (screen(p) for p in face.corners)
        br = (tr[0] + bl[0] - tl[0], tr[1] + bl[1] - tl[1])
        image = face.image
        if face.tint:
            image = multiply(image, face.tint)
        image = multiply(image, (round(255 * face.shade()),) * 3)
        coeffs = solve_affine([tl, tr, bl], face.tex_points)
        layer = image.transform((big, big), Image.AFFINE, coeffs, resample=Image.NEAREST)
        mask = Image.new("L", (big, big), 0)
        ImageDraw.Draw(mask).polygon([tl, tr, br, bl], fill=255)
        alpha = numpy.minimum(numpy.asarray(layer.getchannel("A")), numpy.asarray(mask))
        a, b, c = solve_affine([tl, tr, bl], [(_dot(p, VIEW), 0) for p in face.corners])[:3]
        face_depth = a * pixel_x + b * pixel_y + c
        drawn = (alpha > 0) & (face_depth <= depth + 1e-3)
        depth[drawn] = face_depth[drawn]
        layer.putalpha(Image.fromarray(numpy.where(drawn, alpha, 0).astype(numpy.uint8)))
        canvas.alpha_composite(layer)
    return canvas.resize((size, size), Image.LANCZOS)


def multiply(image, color):
    r, g, b, a = image.split()
    r = r.point(lambda v: v * color[0] // 255)
    g = g.point(lambda v: v * color[1] // 255)
    b = b.point(lambda v: v * color[2] // 255)
    return Image.merge("RGBA", (r, g, b, a))


# --- block models ---------------------------------------------------------------

def face_corners(direction, f, t):
    fx, fy, fz = f
    tx, ty, tz = t
    return {
        "north": ((tx, ty, fz), (fx, ty, fz), (tx, fy, fz)),
        "south": ((fx, ty, tz), (tx, ty, tz), (fx, fy, tz)),
        "west": ((fx, ty, fz), (fx, ty, tz), (fx, fy, fz)),
        "east": ((tx, ty, tz), (tx, ty, fz), (tx, fy, tz)),
        "up": ((fx, ty, fz), (tx, ty, fz), (fx, ty, tz)),
        "down": ((fx, fy, tz), (tx, fy, tz), (fx, fy, fz)),
    }[direction]


def default_uv(direction, f, t):
    fx, fy, fz = f
    tx, ty, tz = t
    return {
        "north": (16 - tx, 16 - ty, 16 - fx, 16 - fy),
        "south": (fx, 16 - ty, tx, 16 - fy),
        "west": (fz, 16 - ty, tz, 16 - fy),
        "east": (16 - tz, 16 - ty, 16 - fz, 16 - fy),
        "up": (fx, fz, tx, tz),
        "down": (fx, 16 - tz, tx, 16 - fz),
    }[direction]


def rotate_point(p, rotation):
    if not rotation:
        return p
    origin = rotation.get("origin", (8, 8, 8))
    angle = math.radians(rotation.get("angle", 0))
    axis = rotation.get("axis", "y")
    x, y, z = (p[i] - origin[i] for i in range(3))
    c, s = math.cos(angle), math.sin(angle)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


def model_faces(assets, ref, tint=None):
    textures, elements = assets.model(ref)
    faces = []
    for element in elements or []:
        f, t = element["from"], element["to"]
        for direction, spec in element.get("faces", {}).items():
            texture_ref = resolve(textures, spec["texture"])
            if texture_ref is None:
                continue
            image = assets.texture(texture_ref)
            scale = image.width / 16
            u1, v1, u2, v2 = spec.get("uv") or default_uv(direction, f, t)
            uv = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
            k = (spec.get("rotation", 0) // 90) % 4
            uv = [uv[(i - k) % 4] for i in range(4)]
            tex_points = [(u * scale, v * scale) for u, v in (uv[0], uv[1], uv[3])]
            corners = tuple(rotate_point(p, element.get("rotation")) for p in face_corners(direction, f, t))
            faces.append(Face(corners, image, tex_points, tint if "tintindex" in spec else None))
    return faces


CUBE = [(x, y, z) for x in (0, 16) for y in (0, 16) for z in (0, 16)]


# --- boxes with entity texture layout ----------------------------------------------

def entity_box(image, u, v, size, origin, inflate=0.0, sides=("north", "west", "up")):
    """A box with the usual entity texture layout at (u, v), its front facing north.
    `origin` is the north-west-bottom corner in world units (y up)."""
    w, h, d = size
    ox, oy, oz = origin
    f = (ox - inflate, oy - inflate, oz - inflate)
    t = (ox + w + inflate, oy + h + inflate, oz + d + inflate)
    regions = {
        # direction: texture points of the face's TL, TR, BL corners (see face_corners)
        "north": ((u + d, v + d), (u + d + w, v + d), (u + d, v + d + h)),
        "west": ((u + d + w, v + d), (u + d + w + d, v + d), (u + d + w, v + d + h)),
        # The top region touches the front region along its lower edge, and its left
        # column lies above the front's left column (east), so it is mirrored here.
        "up": ((u + d + w, v + d), (u + d, v + d), (u + d + w, v)),
    }
    return [Face(face_corners(direction, f, t), image, list(regions[direction])) for direction in sides]


def box(image, origin, size, north, west, up):
    """A box (y up) whose north, west and up faces show the given texture rectangles."""
    (ox, oy, oz), (w, h, d) = origin, size
    f, t = (ox, oy, oz), (ox + w, oy + h, oz + d)
    faces = []
    for direction, (x1, y1, x2, y2) in (("north", north), ("west", west), ("up", up)):
        faces.append(Face(face_corners(direction, f, t), image, [(x1, y1), (x2, y1), (x1, y2)]))
    return faces


def chest(assets):
    """The chest is a block entity, so it is put together from its entity texture."""
    image = assets.texture("minecraft:entity/chest/normal")
    faces = box(image, (1, 0, 1), (14, 10, 14), (42, 33, 56, 43), (28, 33, 42, 43), (14, 19, 28, 33))
    faces += box(image, (1, 9, 1), (14, 5, 14), (42, 14, 56, 19), (28, 14, 42, 19), (28, 0, 42, 14))
    faces += box(image, (7, 7, 0), (2, 4, 1), (1, 1, 3, 5), (0, 1, 1, 5), (1, 0, 3, 1))
    return render(faces, bounds=CUBE)


# --- GUI pieces ----------------------------------------------------------------------

def gui(assets, path):
    return Image.open(BytesIO(assets.read("minecraft", f"textures/gui/{path}.png"))).convert("RGBA")


def grass_strip(assets):
    """The upper part of a grass block's side, tiled under the header."""
    side = assets.texture("minecraft:block/dirt").copy()
    overlay = multiply(assets.texture("minecraft:block/grass_block_side_overlay"), GRASS_TINT)
    side.alpha_composite(overlay)
    return side.crop((0, 0, 16, 6))


def pixel_svg(image):
    """A 16x16 texture as an SVG made of one rectangle per run of equal pixels."""
    w, h = image.size
    pixels = image.load()
    rects = []
    for y in range(h):
        x = 0
        while x < w:
            r, g, b, a = pixels[x, y]
            if a == 0:
                x += 1
                continue
            start = x
            while x < w and pixels[x, y] == (r, g, b, a):
                x += 1
            opacity = "" if a == 255 else f' fill-opacity="{a / 255:.2f}"'
            rects.append(f'<rect x="{start}" y="{y}" width="{x - start}" height="1" fill="#{r:02x}{g:02x}{b:02x}"{opacity}/>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" shape-rendering="crispEdges">'
            + "".join(rects) + "</svg>\n")


# --- entity heads ------------------------------------------------------------------

def layered(assets, *refs):
    skin = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for ref in refs:
        if ref:
            layer = assets.texture(ref)
            skin.alpha_composite(layer.resize((64, layer.height * 64 // layer.width), Image.NEAREST).crop((0, 0, 64, 64)))
    return skin


def head_faces(skin, hat=True):
    """The head of the villager and illager models: 8x10x8 with an outer layer and a nose."""
    faces = entity_box(skin, 0, 0, (8, 10, 8), (0, 0, 0))
    if hat:
        faces += entity_box(skin, 32, 0, (8, 10, 8), (0, 0, 0), inflate=0.51)
    faces += entity_box(skin, 24, 0, (2, 4, 2), (3, -1, -2))
    return faces


def hat_rim(skin, rim_source):
    """The villager hat rim: a 16x16 plate, one pixel thick, laid flat around the head."""
    if not rim_source.crop((30, 47, 64, 64)).getbbox():
        return []
    y = 5
    corners = ((-4, y + 1, -4), (12, y + 1, -4), (-4, y + 1, 12))
    return [
        Face(corners, skin, [(31, 48), (47, 48), (31, 64)]),
        # Its edges: the front one is the plate's bottom row, the side one a column of its side.
        Face(((12, y + 1, -4), (-4, y + 1, -4), (12, y, -4)), skin, [(47, 47), (63, 47), (47, 48)]),
        Face(((-4, y + 1, -4), (-4, y + 1, 12), (-4, y, -4)), skin, [(30, 64), (30, 48), (31, 64)]),
    ]


def villager_skin(assets, profession):
    profession_ref = f"minecraft:entity/villager/profession/{profession}" if profession else None
    skin = layered(assets, "minecraft:entity/villager/villager", "minecraft:entity/villager/type/plains", profession_ref)
    rim = layered(assets, profession_ref)
    return skin, rim


def villager_head(assets, profession):
    skin, rim = villager_skin(assets, profession)
    return render(head_faces(skin) + hat_rim(skin, rim), bounds=HEAD_BOUNDS)


def trader_head(assets):
    skin = layered(assets, "minecraft:entity/wandering_trader/wandering_trader")
    return render(head_faces(skin) + hat_rim(skin, skin), bounds=HEAD_BOUNDS)


def illager_head(assets, name):
    skin = layered(assets, f"minecraft:entity/illager/{name}")
    # Only the illusioner shows the outer layer of the head (its hood).
    return render(head_faces(skin, hat=name == "illusioner"), bounds=HEAD_BOUNDS)


def witch_head(assets):
    """The witch: a villager head with a nose and the four parts of the pointed hat
    (texture rows 64 and below of the 64x128 texture)."""
    # Not assets.texture(): that would take the 64x128 texture for an animation and crop it.
    image = Image.open(BytesIO(assets.read("minecraft", "textures/entity/witch/witch.png"))).convert("RGBA")
    faces = entity_box(image, 0, 0, (8, 10, 8), (0, 0, 0))
    faces += entity_box(image, 24, 0, (2, 4, 2), (3, -1, -2))
    faces += entity_box(image, 0, 64, (10, 2, 10), (-1, 8.05, -1))
    faces += entity_box(image, 0, 76, (7, 4, 7), (0.75, 10.05, 1))
    faces += entity_box(image, 0, 87, (4, 4, 4), (2.5, 14.05, 2.5))
    faces += entity_box(image, 0, 95, (1, 2, 1), (4, 18.05, 4.5), inflate=0.25)
    return render(faces, bounds=WITCH_BOUNDS)


def flat_face(skin, hat=True):
    """The front of a head with nose, flat, for the navigation."""
    face = Image.new("RGBA", (8, 11), (0, 0, 0, 0))
    face.alpha_composite(skin.crop((8, 8, 16, 18)))
    if hat:
        face.alpha_composite(skin.crop((40, 8, 48, 18)))
    face.alpha_composite(skin.crop((26, 2, 28, 6)), (3, 7))
    return face


# All heads are scaled the same, so a row of them lines up; the hat rim is the widest part.
HEAD_BOUNDS = [(x, y, z) for x in (-4, 12) for y in (-1, 10.6) for z in (-4, 12)]
WITCH_BOUNDS = [(x, y, z) for x in (-4, 12) for y in (-1, 20.5) for z in (-4, 12)]


def potion(assets, color):
    """A potion bottle item with its liquid tinted."""
    bottle = assets.texture("minecraft:item/potion").copy()
    overlay = multiply(assets.texture("minecraft:item/potion_overlay"), color)
    bottle.alpha_composite(overlay)
    return bottle


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    assets = Assets(sys.argv[1])
    for folder in ("items", "blocks", "heads", "ui"):
        (OUT / folder).mkdir(parents=True, exist_ok=True)
    ICONS.mkdir(parents=True, exist_ok=True)

    items = {}
    for entry in VANILLA_ITEMS:
        name, _, kind = entry.partition(":")
        image = assets.texture(f"minecraft:{kind or 'item'}/{name}")
        items[name.removesuffix("_00").removesuffix("_16").removesuffix("_standby")] = image
    items["potion"] = potion(assets, (248, 36, 35))
    items["splash_potion"] = potion(assets, (60, 160, 70))
    for name, image in items.items():
        image.save(OUT / "items" / f"{name}.png")

    for name, ref in BLOCKS.items():
        tint = GRASS_TINT if name == "grass_block" else None
        render(model_faces(assets, ref, tint), bounds=CUBE).save(OUT / "blocks" / f"{name}.png", optimize=True)
    chest(assets).save(OUT / "blocks" / "chest.png", optimize=True)

    heads = OUT / "heads"
    for profession in PROFESSIONS:
        villager_head(assets, profession).save(heads / f"{profession}.png", optimize=True)
    villager_head(assets, None).save(heads / "villager.png", optimize=True)
    trader_head(assets).save(heads / "wandering_trader.png", optimize=True)
    witch_head(assets).save(heads / "witch.png", optimize=True)
    for name in ("pillager", "vindicator", "evoker", "illusioner"):
        illager_head(assets, name).save(heads / f"{name}.png", optimize=True)

    skin, _ = villager_skin(assets, None)
    (ICONS / "head-villager.svg").write_text(pixel_svg(flat_face(skin)))
    skin, _ = villager_skin(assets, "librarian")
    (ICONS / "head-librarian.svg").write_text(pixel_svg(flat_face(skin)))
    (ICONS / "head-wandering_trader.svg").write_text(pixel_svg(flat_face(layered(assets, "minecraft:entity/wandering_trader/wandering_trader"))))
    (ICONS / "head-witch.svg").write_text(pixel_svg(flat_face(layered(assets, "minecraft:entity/witch/witch"), hat=False)))
    (ICONS / "head-pillager.svg").write_text(pixel_svg(flat_face(layered(assets, "minecraft:entity/illager/pillager"), hat=False)))

    gui(assets, "container/crafting_table").crop((89, 34, 113, 51)).save(OUT / "ui" / "arrow.png")
    for name in ("button", "button_highlighted"):
        button = gui(assets, f"sprites/widget/{name}")
        button.save(OUT / "ui" / f"{name}.png")
        # An emerald green variant for the main buttons; the outline stays black.
        multiply(button, (90, 230, 120)).save(OUT / "ui" / f"{name}_green.png")
    grass_strip(assets).save(OUT / "ui" / "grass_strip.png")
    for name in ("dirt", "stone", "oak_planks", "spruce_planks", "dark_oak_planks", "deepslate", "bookshelf"):
        assets.texture(f"minecraft:block/{name}").save(OUT / "ui" / f"{name}.png")

    for name in NAV_ICONS:
        (ICONS / f"{name}.svg").write_text(pixel_svg(items[name]))


if __name__ == "__main__":
    main()
