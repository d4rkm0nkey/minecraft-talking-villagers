/** Pictures of the wiki (pages/docs/assets/mc), bundled by Vite. */

function byName(files: Record<string, string>): Record<string, string> {
  return Object.fromEntries(Object.entries(files).map(([path, url]) => [path.split("/").pop()!.replace(/\.png$/, ""), url]));
}

const HEADS = byName(import.meta.glob("$mc/heads/*.png", { eager: true, query: "?url", import: "default" }));
const ITEMS = byName(import.meta.glob("$mc/items/*.png", { eager: true, query: "?url", import: "default" }));

/** Head of an entity or a villager profession; professions without a picture get the plain villager. */
export function head(name: string): string {
  if (name === "illager") return HEADS.pillager;
  return HEADS[name] ?? HEADS.villager;
}

/** Head for a voice: its first entity, or the profession it is filtered to. */
export function voiceHead(entities: string[], profession?: string): string {
  const entity = entities[0] ?? "villager";
  return head(entity === "villager" && profession ? profession : entity);
}

export function item(name: string): string {
  return ITEMS[name] ?? ITEMS.paper;
}

/** Item shown next to each group of events. */
export const GROUP_ITEMS: Record<string, string> = {
  basic: "bell",
  talk: "writable_book",
  trade: "emerald",
  life: "bread",
  danger: "totem_of_undying",
  hostile_idle: "campfire",
  combat: "crossbow",
  magic: "splash_potion",
};
