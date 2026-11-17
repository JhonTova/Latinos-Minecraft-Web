# Tareas 002 — Decir cuánto falta cuando el voto del día ya se usó

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe del 13/11/2026, sin paradas obligatorias
- [x] T2 · `pruebas-frontend` · `src/lib/espera.test.ts` con CA-1 a CA-6 en rojo — se comprueba con `npm run test` fallando porque `./espera` no existe (P-6), con el motivo copiado
- [x] T3 · `pruebas-frontend` · `src/lib/rechazo-de-voto.test.ts` con la parte lógica de CA-7, CA-9, CA-10 y CA-11 en rojo — se comprueba con `npm run test` fallando porque `./rechazo-de-voto` no existe
- [x] T4 · `orquestador` · Commit `test(frontend): ...` con T2 y T3
- [x] T5 · `dev-frontend` · `src/lib/espera.ts` y `src/lib/rechazo-de-voto.ts` que ponen en verde T2 y T3 — se comprueba con `npm run test`
- [x] T6 · `dev-frontend` · `boton-votar.tsx` usa el clasificador, añade el aviso de voto diario con `role="status"` y quita el `formatearEspera` local y el `?? 0` — se comprueba con `npm run lint`, `npm run test` y `npm run build`
- [ ] T7 · `verificador-ui` · CA-7 a CA-11 en `/votar/[token]`, móvil y escritorio, claro y oscuro, consola limpia, y el voto correcto (200) sin cambios — se comprueba con su informe y capturas. **Pendiente:** el `verificador-ui` no pudo abrir la pantalla (ver «Evidencia» y «Observaciones»); solo revisó el código
- [x] T8 · `orquestador` · Commit `feat(frontend): ...` con T5 y T6
- [x] T9 · `revisor` · Revisión del diff de la rama, incluido CA-12 (`git diff --stat`) — sin hallazgos altos abiertos
- [x] T10 · `orquestador` · Evidencia, «Qué hubo que corregirle a la IA» y `pr.md` con la matriz

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo | `cd frontend && npm run test` (`pruebas-frontend`, 2.ª vuelta) | `FAIL src/lib/espera.test.ts` — `Error: Cannot find module './espera' imported from …/frontend/src/lib/espera.test.ts`; `FAIL src/lib/rechazo-de-voto.test.ts` — `Error: Cannot find module './rechazo-de-voto' imported from …/frontend/src/lib/rechazo-de-voto.test.ts`. `Test Files 2 failed \| 1 passed (3)`, `Tests 57 passed (57)` (las 57 son de `puente.test.ts`; los archivos nuevos no llegan a ejecutar pruebas). Rojo por módulo inexistente, aceptado en P-6 |
| Verde | `cd frontend && npm run lint` · `npm run test` · `npm run build` (`dev-frontend`, 1.ª vuelta) | lint sin errores ni avisos; Vitest 5.1.0: 3 archivos, 110 pruebas, todas pasan (aviso previo sobre `vitest.config.ts` cargado como CommonJS); `next build` 16.3.1 (Turbopack) compila, TypeScript pasa, 15 páginas estáticas. El build no reescribió `frontend/AGENTS.md` ni `next-env.d.ts` |
| Visual | `cd frontend && npm run dev` (`verificador-ui`) | **No se pudo ver la pantalla.** `npm run dev` arrancó (`Ready in 376ms`), pero el rol no tiene herramientas de navegador y el hook le bloqueó `cat`, `cd .`, `sleep` e `Invoke-WebRequest`. Además, `votar/[token]/page.tsx` llama a `resolverEnlaceDeVoto(token)` en el servidor antes de pintar `BotonVotar`, y sin backend esa llamada falla, así que interceptar `/api/votar/*` en el navegador tampoco bastaría. Solo revisó el código: textos, `role="status"`, ausencia de `text-danger` y botón oculto coinciden con CA-7 a CA-11. Sin capturas, consola, contraste ni anchos |
| Visual (después del cierre, 17/11/2026) | `npx next dev` del frontend de esta rama contra un backend **simulado** (`evidencia/backend-simulado.mjs`), recorrido con Playwright (`evidencia/mirar.py`) a 420 px de ancho | **Hecha fuera del sistema de agentes, por la sesión que lanzó al orquestador.** El backend real no se levantó: el simulador responde `GET /vote-tokens/{token}` con los datos del enlace y `POST /vote-tokens/{token}/vote` con el 409 de cada criterio. CA-7: «Ya usaste tu voto de hoy / Solo se puede votar a un servidor al día. / Podrás volver a votar en 6 h 42 min.», en `role="status"`, sin `text-danger`, sin botón y sin el `detail`. CA-9 (sin tiempo y con `"abc"`): el mismo aviso sin la frase del tiempo. CA-10: «Ya votaste hace poco / Puedes volver a votar en 1 h.», y sin tiempo, sin la frase. CA-11 (otro `title` y cuerpo no JSON): mensaje en rojo con `role="alert"` y botón disponible. Capturas en `evidencia/*.png`. No se miró contraste con herramienta ni otros anchos |
| Revisión | `git log --stat a4a381d..HEAD`, `cd frontend && npm run lint`, `cd frontend && npm run test` (`revisor`, 1.ª vuelta) | Orden TDD correcto (`docs` → `test` → `feat`); el diff solo toca `frontend/src/` y `specs/002-espera-voto-diario/` (CA-12). Lint sin errores ni avisos; `Test Files 3 passed (3)`, `Tests 110 passed (110)`. Matriz completa y sin pruebas débiles: las pruebas distinguen `ceil` de `round` y de `floor`. Mutación: no aplica (frontend, ADR-0032). **Sin hallazgos altos.** Hallazgos: 1 medio (T7 sin verificación visual: se declara en el PR como condición antes de fusionar), 2 bajos (`role="status"` en el bloque de ventana sin anotar en la spec: anotado; T9/T10 y evidencia sin cerrar: cerrados aquí). El revisor no ejecutó `npm run build` (su rol no puede); solo lo respalda el informe de `dev-frontend` |

## Qué hubo que corregirle a la IA

Lo que un agente hizo mal y cómo se detectó. Vacío si no pasó nada.

- **`pruebas-frontend`, 1.ª vuelta: no confirmó el rojo.** Escribió bien las
  dos pruebas, pero intentó `cd` con ruta absoluta, `--prefix` y una tubería
  `| tail`, y el hook rechazó todo. Informó `ESTADO: no se pudo confirmar` sin
  inventar la salida, lo que es correcto. El orquestador leyó
  `.claude/hooks/politica.mjs`, vio que `cd` solo se acepta con ruta relativa
  (`cd frontend`) y lo reencargó con el comando exacto; en la 2.ª vuelta
  funcionó al primer intento.
- **El orquestador amplió el encargo sin anotarlo.** Le pidió a `dev-frontend`
  `role="status"` también en el bloque «Ya votaste hace poco», algo que la spec
  no decía. `dev-frontend` lo hizo tal cual. Lo detectó el `revisor` (hallazgo
  bajo) y se anotó en «Cambios durante la implementación» de la spec.
- **El orquestador escribió en la spec una referencia que todavía no existía.**
  El borrador de la spec remitía a «la lista al final de `plan.md`» antes de
  que el plan existiera; por eso el `analista` marcó su mapa como
  `ESTADO: incompleto`. Se corrigió al aprobar la spec.
- **`verificador-ui` no pudo verificar.** No fue un error del agente: informó
  con claridad lo que no pudo ver y no inventó capturas. No se reencargó
  porque los tres motivos (sin navegador, comandos bloqueados, página que
  necesita el backend) se habrían repetido igual en la segunda vuelta.

## Observaciones sobre el sistema de agentes

- **Etapa 1–3 (13/11/2026).** El hook no bloqueó nada al orquestador (solo
  editó `specs/` y usó `git`). El `analista` respondió en una sola vuelta y
  detectó que la spec remitía a un `plan.md` inexistente (ver arriba).
- **El hook no le dice al subagente cómo debe escribir el comando.** La
  política acepta `cd frontend && npm run test`, pero el subagente no lo sabe:
  su definición no menciona que `cd` tiene que ser relativo ni que se prohíben
  las tuberías, y el mensaje de rechazo no sugiere la forma válida. Costó una
  vuelta entera. Conviene que el mensaje del hook muestre el comando aceptado o
  que la definición del agente lo diga.
- **Rojo por importación.** `pruebas-frontend` solo puede editar `*.test.ts`,
  así que no puede crear un módulo vacío para que el rojo sea por aserción; el
  flujo no dice qué rojo vale en un módulo nuevo (ver P-6 del plan).
- **`verificador-ui` no puede abrir un navegador.** Su definición dice que
  comprueba «en un navegador real», pero en esta sesión no tenía ninguna
  herramienta de navegador (ni `mcp__Claude_Browser__*` ni
  `mcp__claude-in-chrome__*`), y la política del hook solo le deja
  `npm ci` y `npm run build|start|dev`: no puede ejecutar Playwright ni hacer
  una petición HTTP (le bloqueó `cat`, `cd .`, `sleep` e `Invoke-WebRequest`).
  Tal como está, el rol no puede cumplir su función. Hace falta darle un
  navegador o un comando de Playwright en la política.
- **Las pantallas que cargan datos en el servidor no se pueden verificar sin
  backend.** `/votar/[token]` llama al backend desde el servidor de Next antes
  de pintar el botón, así que interceptar en el navegador no basta. Una
  verificación sin backend necesitaría un stub de la API en `:8080` o datos de
  prueba.
- **El `revisor` no puede ejecutar `npm run build`.** Su política solo le deja
  `lint` y `test`, así que la compilación de tipos solo la respalda el informe
  de `dev-frontend`. En esta rama la ejecutó `dev-frontend` en verde.
