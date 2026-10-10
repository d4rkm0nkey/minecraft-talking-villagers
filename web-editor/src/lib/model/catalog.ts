import eventsJson from "$schema/events.json";

/**
 * Events and entities, generated from the mod's EventCatalog (schema/events.json).
 */

export interface CatalogEvent {
  id: string;
  entities: string[];
  parent?: string;
}

export const EVENTS: CatalogEvent[] = (eventsJson as { events: CatalogEvent[] }).events;

export const ENTITIES = ["villager", "wandering_trader", "witch", "pillager", "vindicator", "evoker", "illusioner"] as const;
export const ILLAGERS = ["pillager", "vindicator", "evoker", "illusioner"];
export const PROFESSIONS = ["armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker",
  "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith", "none"];

/** Groups for the editor; every event belongs to the first group that lists it. */
export const GROUPS: { id: string; events: string[] }[] = [
  { id: "basic", events: ["hurt", "death", "ambient", "celebrate"] },
  { id: "talk", events: ["greeting", "greeting_friendly", "greeting_unfriendly", "random", "gossip", "meet", "work", "play", "good_weather", "bad_weather"] },
  { id: "trade", events: ["trade", "trade_unfriendly", "trade_success", "yes", "no", "level_up"] },
  { id: "life", events: ["sleep", "wake_up", "grow_up", "love", "hero", "hero_gift", "cured"] },
  { id: "danger", events: ["panic", "panic_loop", "spot_zombie", "spot_raider", "attacked_by_player", "villager_died", "pre_raid", "hide"] },
  { id: "hostile_idle", events: ["idle", "brew", "patrol", "patrol_leader", "raid_idle"] },
  { id: "combat", events: ["spot_player", "charge_at", "combat", "johnny", "attack", "charge", "retreat", "lost_target", "low_health", "victory", "ally_died", "raid_horn", "laugh"] },
  { id: "magic", events: ["cast_spell", "summon_vex", "fangs", "wololo", "mirror", "blindness", "drink_potion", "heal_self", "drink_other", "throw_potion", "heal_ally", "disappear"] },
];

export function expandEntities(entities: string[]): string[] {
  return [...new Set(entities.flatMap((entity) => (entity === "illager" ? ILLAGERS : [entity])))];
}

/** Choices when creating a voice; a voice can be extended to more entities later. */
export const VOICE_TYPES: { id: string; entities: string[] }[] = [
  { id: "villager", entities: ["villager"] },
  { id: "wandering_trader", entities: ["wandering_trader"] },
  { id: "witch", entities: ["witch"] },
  { id: "illager", entities: ["illager"] },
  { id: "pillager", entities: ["pillager"] },
  { id: "vindicator", entities: ["vindicator"] },
  { id: "evoker", entities: ["evoker"] },
  { id: "illusioner", entities: ["illusioner"] },
];

/** Events triggered by at least one of the entities. */
export function eventsFor(entities: string[]): CatalogEvent[] {
  const kinds = expandEntities(entities);
  return EVENTS.filter((event) => event.entities.some((entity) => kinds.includes(entity)));
}

export function groupOf(eventId: string): string {
  return GROUPS.find((group) => group.events.includes(eventId))?.id ?? "other";
}

/** Recommended number of variants: frequent events need more. */
export function recommendedVariants(eventId: string): number {
  if (["hurt", "ambient", "random", "idle", "combat", "panic_loop"].includes(eventId)) return 5;
  if (["greeting", "yes", "no", "trade", "attack", "brew", "patrol"].includes(eventId)) return 3;
  return 1;
}
