# Tareas 002 — Decir cuánto falta cuando el voto del día ya se usó

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe del 13/11/2026, sin paradas obligatorias
- [x] T2 · `pruebas-frontend` · `src/lib/espera.test.ts` con CA-1 a CA-6 en rojo — se comprueba con `npm run test` fallando porque `./espera` no existe (P-6), con el motivo copiado
- [x] T3 · `pruebas-frontend` · `src/lib/rechazo-de-voto.test.ts` con la parte lógica de CA-7, CA-9, CA-10 y CA-11 en rojo — se comprueba con `npm run test` fallando porque `./rechazo-de-voto` no existe
- [x] T4 · `orquestador` · Commit `test(frontend): ...` con T2 y T3
- [ ] T5 · `dev-frontend` · `src/lib/espera.ts` y `src/lib/rechazo-de-voto.ts` que ponen en verde T2 y T3 — se comprueba con `npm run test`
- [ ] T6 · `dev-frontend` · `boton-votar.tsx` usa el clasificador, añade el aviso de voto diario con `role="status"` y quita el `formatearEspera` local y el `?? 0` — se comprueba con `npm run lint`, `npm run test` y `npm run build`
- [ ] T7 · `verificador-ui` · CA-7 a CA-11 en `/votar/[token]`, móvil y escritorio, claro y oscuro, consola limpia, y el voto correcto (200) sin cambios — se comprueba con su informe y capturas
- [ ] T8 · `orquestador` · Commit `feat(frontend): ...` con T5 y T6
- [ ] T9 · `revisor` · Revisión del diff de la rama, incluido CA-12 (`git diff --stat`) — sin hallazgos altos abiertos
- [ ] T10 · `orquestador` · Evidencia, «Qué hubo que corregirle a la IA» y `pr.md` con la matriz

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo | `cd frontend && npm run test` (`pruebas-frontend`, 2.ª vuelta) | `FAIL src/lib/espera.test.ts` — `Error: Cannot find module './espera' imported from …/frontend/src/lib/espera.test.ts`; `FAIL src/lib/rechazo-de-voto.test.ts` — `Error: Cannot find module './rechazo-de-voto' imported from …/frontend/src/lib/rechazo-de-voto.test.ts`. `Test Files 2 failed \| 1 passed (3)`, `Tests 57 passed (57)` (las 57 son de `puente.test.ts`; los archivos nuevos no llegan a ejecutar pruebas). Rojo por módulo inexistente, aceptado en P-6 |
| Verde | | |
| Visual | | |
| Revisión | | |

## Qué hubo que corregirle a la IA

Lo que un agente hizo mal y cómo se detectó. Vacío si no pasó nada.

- **`pruebas-frontend`, 1.ª vuelta: no confirmó el rojo.** Escribió bien las
  dos pruebas, pero intentó `cd` con ruta absoluta, `--prefix` y una tubería
  `| tail`, y el hook rechazó todo. Informó `ESTADO: no se pudo confirmar` sin
  inventar la salida, lo que es correcto. El orquestador leyó
  `.claude/hooks/politica.mjs`, vio que `cd` solo se acepta con ruta relativa
  (`cd frontend`) y lo reencargó con el comando exacto; en la 2.ª vuelta
  funcionó al primer intento.

## Observaciones sobre el sistema de agentes

- **Etapa 1–3 (13/11/2026).** El hook no bloqueó nada al orquestador (solo
  editó `specs/` y usó `git`). El `analista` respondió en una sola vuelta;
  marcó `ESTADO: incompleto` solo porque la spec remitía a un `plan.md` que
  todavía no existía (se escribe después del mapa), no por falta de
  información: el orden spec → mapa → plan hace que esa referencia de la spec
  esté rota mientras corre el analista. Conviene que la plantilla de la spec no
  remita al plan, o que el analista lo trate como esperado.
- **El hook no le dice al subagente cómo debe escribir el comando.** La
  política acepta `cd frontend && npm run test`, pero el subagente no lo sabe:
  su definición no menciona que `cd` tiene que ser relativo ni que se prohíben
  las tuberías, y el mensaje de rechazo no sugiere la forma válida. Costó una
  vuelta entera. Conviene que el mensaje del hook muestre el comando aceptado o
  que la definición del agente lo diga.
- **Rojo por importación.** `pruebas-frontend` solo puede editar `*.test.ts`,
  así que no puede crear un módulo vacío para que el rojo sea por aserción; el
  flujo no dice qué rojo vale en un módulo nuevo (ver P-6 del plan).
