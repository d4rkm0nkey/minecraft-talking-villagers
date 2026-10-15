import { describe, expect, it } from "vitest";
import { eventsFor } from "../src/lib/model/catalog";

describe("eventsFor", () => {
  it("offers a witch voice only witch events", () => {
    const events = eventsFor(["witch"]).map((event) => event.id);
    expect(events).toEqual(expect.arrayContaining(["ambient", "hurt", "death", "brew", "heal_self", "combat"]));
    for (const villagerOnly of ["play", "gossip", "work", "trade", "greeting", "sleep"]) {
      expect(events).not.toContain(villagerOnly);
    }
  });

  it("expands the illager group", () => {
    const events = eventsFor(["illager"]).map((event) => event.id);
    expect(events).toEqual(expect.arrayContaining(["patrol", "summon_vex", "mirror", "johnny"]));
    expect(events).not.toContain("brew");
  });
});
