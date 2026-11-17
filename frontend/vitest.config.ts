import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

// Solo se prueba la lógica sin interfaz (ADR-0031): por eso el entorno es
// `node` y no hay jsdom. El alias repite el `paths` de tsconfig.json.
export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
