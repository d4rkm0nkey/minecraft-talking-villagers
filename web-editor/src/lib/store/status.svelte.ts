/**
 * Whether saving in the browser works. Projects live only in IndexedDB, so a failed write must not go unnoticed.
 */
export const storage = $state<{ error: string; persisted: boolean | undefined }>({ error: "", persisted: undefined });

/** What an import could not take over, shown above the opened project until closed. Keys are "import.<code>". */
export const importNotes = $state<{ warnings: { code: string; params?: Record<string, string | number> }[] }>({ warnings: [] });

/** Shows a failed write on the page and passes the error on. */
export function reportError(error: unknown): never {
  storage.error = error instanceof DOMException && error.name === "QuotaExceededError" ? "quota" : String(error);
  throw error;
}

/**
 * Asks the browser not to clear our storage when space runs low. Browsers decide on their own:
 * Firefox asks the user, Chrome grants it to sites the user visits often, Safari ignores it.
 */
export async function requestPersistence(): Promise<void> {
  if (storage.persisted || typeof navigator === "undefined" || !navigator.storage?.persist) return;
  try {
    storage.persisted = (await navigator.storage.persisted()) || (await navigator.storage.persist());
  } catch {
    storage.persisted = false;
  }
}
