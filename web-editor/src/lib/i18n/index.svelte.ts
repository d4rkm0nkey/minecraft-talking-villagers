import { de } from "./de";
import { en } from "./en";

const dictionaries: Record<string, Record<string, string>> = { de, en };

function initial(): string {
  try {
    const stored = localStorage.getItem("language");
    if (stored && dictionaries[stored]) return stored;
  } catch {
    // storage blocked
  }
  return navigator.language.toLowerCase().startsWith("de") ? "de" : "en";
}

export const locale = $state({ language: typeof navigator === "undefined" ? "en" : initial() });

export function setLanguage(language: string): void {
  locale.language = language;
  try {
    localStorage.setItem("language", language);
  } catch {
    // storage blocked
  }
}

/** Translated text; {name} placeholders are replaced by params. */
export function t(key: string, params: Record<string, string | number> = {}): string {
  const text = dictionaries[locale.language]?.[key] ?? en[key] ?? key;
  return text.replace(/\{(\w+)\}/g, (_, name) => String(params[name] ?? `{${name}}`));
}
