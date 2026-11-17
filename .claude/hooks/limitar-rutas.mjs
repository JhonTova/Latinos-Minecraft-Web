// Hook PreToolUse para Edit y Write: rechaza una ruta fuera de las carpetas del
// rol. Uso, en el frontmatter del agente:
//
//   command: node
//   args: ["${CLAUDE_PROJECT_DIR}/.claude/hooks/limitar-rutas.mjs", "<rol>"]

import { bloquear, conCierreSeguro, leerLlamada, relativaAlRepositorio, yaEstaEnMain } from "./entrada.mjs";
import { puedeEditar } from "./politica.mjs";

await conCierreSeguro(async () => {
  const rol = process.argv[2];
  if (!rol) bloquear("al hook limitar-rutas le falta el rol");

  const llamada = await leerLlamada();
  const ruta = llamada?.tool_input?.file_path ?? llamada?.tool_input?.notebook_path;
  if (typeof ruta !== "string" || ruta === "") {
    bloquear(`la herramienta ${llamada?.tool_name} no trae una ruta que se pueda comprobar`);
  }

  const relativa = relativaAlRepositorio(ruta);
  if (relativa === null) bloquear(`'${ruta}' está fuera del repositorio`);

  const decision = puedeEditar(rol, relativa, yaEstaEnMain);
  if (!decision.ok) bloquear(decision.motivo);
});
