import { describe, expect, it } from "vitest";
import { de } from "../src/lib/i18n/de";
import { en } from "../src/lib/i18n/en";
import { EVENTS } from "../src/lib/model/catalog";

describe("translations", () => {
  it("have the same keys in German and English", () => {
    expect(Object.keys(de).sort()).toEqual(Object.keys(en).sort());
  });

  it("describe every event", () => {
    for (const event of EVENTS) {
      expect(en[`event.${event.id}`], event.id).toBeTruthy();
      expect(de[`event.${event.id}`], event.id).toBeTruthy();
    }
  });
});
