// Hook PreToolUse de LatinosMC. Está registrado en `.claude/settings.json`, así
// que corre en toda sesión de Claude Code sobre este repositorio y también
// dentro de los subagentes.
//
// Decide por el campo `agent_type` de la llamada: si es uno de los ocho roles
// del proyecto, aplica su política (`politica.mjs`); si no, deja pasar.
//
// Por qué aquí y no en el frontmatter de cada agente: los hooks del frontmatter
// de un agente del proyecto solo se ejecutan si la carpeta fue aceptada en el
// diálogo de confianza, y una sesión `claude -p` no cuenta. Cuando no se
// ejecutan no avisan: el agente queda sin límites. Se comprobó el 13/11/2026
// (docs/agentes/README.md, cambio 7).

import { bloquear, conCierreSeguro, leerLlamada, relativaAlRepositorio, yaEstaEnMain } from "./entrada.mjs";
import { decidirLlamada } from "./politica.mjs";

await conCierreSeguro(async () => {
  const llamada = await leerLlamada();
  const decision = decidirLlamada(llamada, { relativaAlRepositorio, yaEstaEnMain });
  if (!decision.ok) bloquear(decision.motivo);
});
