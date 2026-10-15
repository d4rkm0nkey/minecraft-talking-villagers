# Talking Villagers – Voicepack Editor

A browser app to record and build `.voicepack` files for the Talking Villagers mod. Everything runs locally in the browser
(recordings are kept in IndexedDB), nothing is uploaded.

```bash
npm install
npm run dev      # http://localhost:5173, add #demo-pack, #demo-voice or #demo-studio for a sample project
npm test         # unit tests; also writes build/test-export.voicepack for the Java cross-check
npm run check    # type check
npm run build    # static site in dist/, deployed to GitHub Pages
```

The event list comes from `../schema/events.json`, generated from the mod's `EventCatalog`
(`./gradlew :core:writeEventCatalog`); manifests are validated against `../schema/voicepack.schema.json`.
