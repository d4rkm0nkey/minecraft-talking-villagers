import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";
import { fileURLToPath } from "node:url";

// relative base, so the build works on GitHub Pages under /<repo>/
export default defineConfig({
  base: "./",
  plugins: [svelte()],
  resolve: {
    alias: {
      $schema: fileURLToPath(new URL("../schema", import.meta.url)),
    },
  },
  server: {
    fs: { allow: [".."] },
  },
  worker: { format: "es" },
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts"],
  },
});
