// Lo común de los dos hooks: leer la llamada que Claude Code manda por la
// entrada estándar y responder.
//
// Un hook PreToolUse bloquea la herramienta SOLO con el código de salida 2;
// cualquier otro código distinto de 0 se toma como un fallo del hook y la
// llamada sigue adelante. Por eso todo error inesperado termina también en 2:
// si el control no puede decidir, no deja pasar.

import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

/** Raíz del repositorio: este archivo vive en `.claude/hooks/`. */
export const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

export async function leerLlamada() {
  let texto = "";
  for await (const trozo of process.stdin) texto += trozo;
  return JSON.parse(texto);
}

export function bloquear(motivo) {
  process.stderr.write(`Bloqueado por el arnés de LatinosMC: ${motivo}\n`);
  process.exit(2);
}

/** Ejecuta `decidir` y convierte cualquier excepción en un bloqueo. */
export async function conCierreSeguro(decidir) {
  try {
    await decidir();
    process.exit(0);
  } catch (error) {
    bloquear(`el hook falló (${error?.message ?? error}); por seguridad no se ejecuta la herramienta`);
  }
}

/** Ruta relativa al repositorio con `/`, o `null` si queda fuera de él. */
export function relativaAlRepositorio(rutaAbsoluta) {
  const relativa = path.relative(RAIZ, path.resolve(RAIZ, rutaAbsoluta));
  if (relativa === "" || relativa.startsWith("..") || path.isAbsolute(relativa)) return null;
  return relativa.split(path.sep).join("/");
}

/** Si el archivo ya existe en `main` (local o, si no hay, el de origin). */
export function yaEstaEnMain(rutaRelativa) {
  for (const rama of ["main", "origin/main"]) {
    const r = spawnSync("git", ["cat-file", "-e", `${rama}:${rutaRelativa}`], { cwd: RAIZ });
    if (r.status === 0) return true;
  }
  return false;
}
