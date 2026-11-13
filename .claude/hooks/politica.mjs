// Política de permisos de los agentes de LatinosMC.
//
// El campo `tools` de un agente decide QUÉ herramientas tiene, pero no SOBRE QUÉ
// carpetas las usa ni QUÉ comandos ejecuta. Eso se decide aquí, y lo aplica el
// hook PreToolUse `aplicar-politica.mjs`. Es una restricción real: el hook rechaza la llamada aunque el modelo decida ignorar su prompt.
//
// Todo en este archivo son funciones puras para poder probarlas
// (`node --test .claude/hooks/`).

/** Migración Flyway versionada, con la ruta relativa al repositorio. */
const MIGRACION = /^backend\/app\/src\/main\/resources\/db\/migration\/V\d+__[^/]+\.sql$/;

/**
 * Qué puede editar cada rol. `permite` y `niega` son expresiones sobre la ruta
 * relativa al repositorio, con `/` como separador. Un rol que no aparece aquí
 * no puede editar nada.
 */
export const RUTAS = {
  // El orquestador no escribe código: solo los documentos de la spec (incluida
  // la descripción del pull request, pr.md) y los ADR.
  orquestador: {
    permite: [/^specs\/\d{3}-[a-z0-9-]+\/(spec|plan|tareas|pr)\.md$/, /^docs\/decisiones\/\d{4}-[a-z0-9-]+\.md$/, /^docs\/DECISIONES\.md$/],
    niega: [],
  },
  "pruebas-backend": {
    permite: [/^backend\/[a-z]+\/src\/test\//],
    niega: [],
  },
  "dev-backend": {
    permite: [/^backend\/[a-z]+\/src\/main\//],
    // La configuración de producción no se toca desde un agente.
    niega: [/^backend\/app\/src\/main\/resources\/application[^/]*\.(yml|yaml|properties)$/],
  },
  "pruebas-frontend": {
    permite: [/^frontend\/src\/.+\.test\.ts$/],
    niega: [],
  },
  "dev-frontend": {
    permite: [/^frontend\/src\//, /^frontend\/public\//],
    niega: [/\.test\.ts$/],
  },
};

/**
 * Decide si `rol` puede escribir en `ruta`.
 *
 * @param {string} rol
 * @param {string} ruta relativa al repositorio; se aceptan `\` y `/`
 * @param {(ruta: string) => boolean} yaEstaEnMain dice si el archivo existe en `main`
 * @returns {{ok: true} | {ok: false, motivo: string}}
 */
export function puedeEditar(rol, ruta, yaEstaEnMain = () => false) {
  const reglas = RUTAS[rol];
  if (!reglas) {
    return { ok: false, motivo: `el rol '${rol}' no puede editar archivos` };
  }

  const limpia = ruta.replaceAll("\\", "/").replace(/^\.\//, "");
  if (limpia === "" || limpia.startsWith("/") || /^[a-zA-Z]:/.test(limpia) || limpia.split("/").includes("..")) {
    return { ok: false, motivo: `'${ruta}' está fuera del repositorio` };
  }

  if (!reglas.permite.some((patron) => patron.test(limpia))) {
    return { ok: false, motivo: `'${limpia}' está fuera de las carpetas del rol '${rol}'` };
  }
  if (reglas.niega.some((patron) => patron.test(limpia))) {
    return { ok: false, motivo: `el rol '${rol}' no puede tocar '${limpia}'` };
  }
  if (MIGRACION.test(limpia) && yaEstaEnMain(limpia)) {
    return {
      ok: false,
      motivo: `'${limpia}' ya está en main: una migración aplicada no se edita. Crea una migración nueva.`,
    };
  }
  return { ok: true };
}

// ---------------------------------------------------------------------------
// Comandos
// ---------------------------------------------------------------------------

const RAMA = "(?!main$|master$)[A-Za-z0-9][A-Za-z0-9._/-]*";

/** Lectura del repositorio: no cambia nada, así que la tiene todo rol con Bash. */
const GIT_LECTURA = [/^git (status|diff|log|show|branch --show-current|rev-parse|merge-base|ls-files)\b/];

const CD = /^cd (\.\.|(\.\/)?(backend|frontend)(\/[A-Za-z0-9._/-]*)?)$/;

/** Metas de Maven que puede ejecutar cada rol. */
const METAS_MAVEN = {
  "pruebas-backend": ["test-compile", "test", "verify"],
  "dev-backend": ["compile", "test-compile", "test", "verify"],
  revisor: ["test", "verify", "org.pitest:pitest-maven:mutationCoverage", "jacoco:report"],
};

/** Opciones de Maven que llevan un valor en el argumento siguiente. */
const MAVEN_CON_VALOR = new Set(["-pl", "--projects", "-f", "--file", "-P", "--activate-profiles", "-rf", "-T"]);

const COMANDOS = {
  orquestador: [
    /^git fetch( origin)?$/,
    /^git pull --ff-only( origin main)?$/,
    new RegExp(`^git switch( -c)? ${RAMA}$`),
    /^git switch main$/,
    /^git add (?!.*(^| )(-f|--force)( |$)).+$/,
    /^git commit (?!.*(--no-verify|--amend|(^| )-n( |$))).+$/,
    new RegExp(`^git push( -u| --set-upstream)? origin ${RAMA}$`),
    /^gh pr (create|view|list|checks|status|diff)\b(?!.*--admin)/,
    /^gh run (list|view|watch)\b/,
  ],
  "pruebas-frontend": [/^npm (run )?test( -- [A-Za-z0-9 ._/=-]+)?$/],
  "dev-frontend": [/^npm ci$/, /^npm run (lint|build)$/, /^npm (run )?test( -- [A-Za-z0-9 ._/=-]+)?$/],
  "verificador-ui": [/^npm ci$/, /^npm run (build|start|dev)( -- [A-Za-z0-9 ._/=-]+)?$/],
  revisor: [/^npm run lint$/, /^npm (run )?test$/],
};

/**
 * Parte una línea de comandos en sus segmentos (`a && b; c | d`), respetando
 * las comillas. Devuelve `null` si usa algo que no se puede analizar con
 * seguridad: sustitución de comandos, redirecciones o comillas sin cerrar.
 *
 * @param {string} linea
 * @returns {string[] | null}
 */
export function segmentos(linea) {
  const partes = [];
  let actual = "";
  let comilla = null;

  for (let i = 0; i < linea.length; i++) {
    const c = linea[i];
    const dos = linea.slice(i, i + 2);

    if (comilla === "'") {
      if (c === "'") comilla = null;
      actual += c;
      continue;
    }
    // Dentro de comillas dobles el shell sigue expandiendo `$(...)` y `` ` ``.
    if (c === "`" || dos === "$(" || dos === "${") return null;
    if (comilla === '"') {
      if (c === "\\") {
        actual += linea.slice(i, i + 2);
        i++;
        continue;
      }
      if (c === '"') comilla = null;
      actual += c;
      continue;
    }

    if (c === "'" || c === '"') {
      comilla = c;
      actual += c;
    } else if (dos === "&&" || dos === "||") {
      partes.push(actual);
      actual = "";
      i++;
    } else if (c === ";" || c === "|" || c === "\n") {
      partes.push(actual);
      actual = "";
    } else if (c === ">" || c === "<" || c === "&") {
      return null;
    } else {
      actual += c;
    }
  }

  if (comilla) return null;
  partes.push(actual);
  return partes.map((p) => p.trim().replace(/\s+/g, " ")).filter((p) => p !== "");
}

function mavenPermitido(rol, segmento) {
  const metas = METAS_MAVEN[rol];
  if (!metas) return false;
  const piezas = segmento.split(" ").slice(1);
  if (piezas.length === 0) return false;

  let hayMeta = false;
  for (let i = 0; i < piezas.length; i++) {
    const p = piezas[i];
    if (MAVEN_CON_VALOR.has(p)) {
      i++;
    } else if (p.startsWith("-")) {
      // Saltarse las pruebas convertiría `verify` en una comprobación vacía.
      if (/^-D(skipTests|maven\.test\.skip|skipITs)\b/.test(p)) return false;
    } else if (metas.includes(p)) {
      hayMeta = true;
    } else {
      return false;
    }
  }
  return hayMeta;
}

/**
 * Decide si `rol` puede ejecutar `linea`. Todos los segmentos tienen que estar
 * permitidos: `mvn test && rm -rf x` se rechaza entero.
 *
 * @returns {{ok: true} | {ok: false, motivo: string}}
 */
export function puedeEjecutar(rol, linea) {
  const partes = segmentos(linea);
  if (partes === null) {
    return {
      ok: false,
      motivo: "el comando usa sustitución ($(...), `...`), redirecciones o comillas sin cerrar; escríbelo sin ellas",
    };
  }
  if (partes.length === 0) {
    return { ok: false, motivo: "comando vacío" };
  }

  const propios = COMANDOS[rol] ?? [];
  for (const segmento of partes) {
    const permitido =
      CD.test(segmento) ||
      GIT_LECTURA.some((p) => p.test(segmento)) ||
      propios.some((p) => p.test(segmento)) ||
      (/^mvn(\.cmd)? /.test(segmento) && mavenPermitido(rol, segmento));
    if (!permitido) {
      return { ok: false, motivo: `el rol '${rol}' no puede ejecutar: ${segmento}` };
    }
  }
  return { ok: true };
}

// ---------------------------------------------------------------------------
// Entrada única del hook
// ---------------------------------------------------------------------------

/** Los roles del proyecto. Un `agent_type` que no esté aquí no se restringe. */
export const ROLES = new Set([
  "orquestador",
  "analista",
  "pruebas-backend",
  "dev-backend",
  "pruebas-frontend",
  "dev-frontend",
  "verificador-ui",
  "revisor",
]);

const HERRAMIENTAS_DE_EDICION = new Set(["Edit", "Write", "MultiEdit", "NotebookEdit"]);
const HERRAMIENTAS_DE_COMANDOS = new Set(["Bash", "PowerShell"]);

/**
 * Decide una llamada PreToolUse completa, tal como la manda Claude Code.
 *
 * El rol sale de `agent_type`: lo trae la llamada cuando la hace un subagente o
 * cuando la sesión arrancó con `--agent`. Una sesión normal del desarrollador no
 * lo trae y no se restringe aquí (para ella están `permissions.deny`, la CI y el
 * ruleset de `main`).
 *
 * @param {object} llamada entrada del hook
 * @param {{relativaAlRepositorio: (ruta: string) => string | null, yaEstaEnMain: (ruta: string) => boolean}} entorno
 * @returns {{ok: true} | {ok: false, motivo: string}}
 */
export function decidirLlamada(llamada, entorno) {
  const rol = llamada?.agent_type;
  if (typeof rol !== "string" || !ROLES.has(rol)) return { ok: true };

  const herramienta = llamada?.tool_name;

  if (HERRAMIENTAS_DE_EDICION.has(herramienta)) {
    const ruta = llamada?.tool_input?.file_path ?? llamada?.tool_input?.notebook_path;
    if (typeof ruta !== "string" || ruta === "") {
      return { ok: false, motivo: `la herramienta ${herramienta} no trae una ruta que se pueda comprobar` };
    }
    const relativa = entorno.relativaAlRepositorio(ruta);
    if (relativa === null) return { ok: false, motivo: `'${ruta}' está fuera del repositorio` };
    return puedeEditar(rol, relativa, entorno.yaEstaEnMain);
  }

  if (HERRAMIENTAS_DE_COMANDOS.has(herramienta)) {
    const comando = llamada?.tool_input?.command;
    if (typeof comando !== "string") {
      return { ok: false, motivo: `la herramienta ${herramienta} no trae un comando que se pueda comprobar` };
    }
    return puedeEjecutar(rol, comando);
  }

  return { ok: true };
}
