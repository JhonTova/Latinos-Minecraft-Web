// Hook PreToolUse para Bash y PowerShell: deja ejecutar solo los comandos del
// rol. Uso, en el frontmatter del agente:
//
//   command: node
//   args: ["${CLAUDE_PROJECT_DIR}/.claude/hooks/limitar-bash.mjs", "<rol>"]

import { bloquear, conCierreSeguro, leerLlamada } from "./entrada.mjs";
import { puedeEjecutar } from "./politica.mjs";

await conCierreSeguro(async () => {
  const rol = process.argv[2];
  if (!rol) bloquear("al hook limitar-bash le falta el rol");

  const llamada = await leerLlamada();
  const comando = llamada?.tool_input?.command;
  if (typeof comando !== "string") {
    bloquear(`la herramienta ${llamada?.tool_name} no trae un comando que se pueda comprobar`);
  }

  const decision = puedeEjecutar(rol, comando);
  if (!decision.ok) bloquear(decision.motivo);
});
