/** Common language tags offered in the language fields; any valid BCP 47 tag can be typed. */
export const COMMON_LANGUAGES = ["de", "en", "en-GB", "en-US", "fr", "es", "it", "nl", "pl", "pt", "pt-BR", "ru", "sv", "da", "no", "fi",
  "cs", "tr", "uk", "ja", "ko", "zh"];

export const LANGUAGE_PATTERN = /^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$/;

export function languageName(tag: string, uiLanguage: string): string {
  try {
    return new Intl.DisplayNames([uiLanguage], { type: "language" }).of(tag) ?? tag;
  } catch {
    return tag;
  }
}
