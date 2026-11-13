# Tareas 001 — Fijar el comportamiento del motor antifraude

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. Aquí no hay código de
producción: el «rojo» es el informe PIT con los mutantes vivos antes de las
pruebas nuevas, y el «verde», el mismo informe sin supervivientes en
`RuleBasedTrustEngine`.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe del 13/11/2026, resumido en «Mapa del cambio»
- [ ] T2 · `pruebas-backend` · Rojo: ejecutar `mvn -B -Pmutacion -pl trust -am verify` **antes** de escribir pruebas y anotar en «Evidencia» los mutantes `SURVIVED` de `RuleBasedTrustEngine` (línea y mutador) — se comprueba con `backend/trust/target/pit-reports/mutations.xml`; se esperan los 10 del plan
- [ ] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-5 en `RuleBasedTrustEngineLimitesTest` — se comprueba con `mvn -B -pl trust -am verify` en verde sin tocar `src/main` (si alguna falla, se para: la cifra de la spec o el motor no son lo que se creía)
- [ ] T4 · `pruebas-backend` · Pruebas de CA-6 a CA-9 en la misma clase — mismo comando
- [ ] T5 · `pruebas-backend` · Verde: volver a ejecutar PIT y clasificar cada mutante de `RuleBasedTrustEngine` (muerto / superviviente con motivo); `mvn -B verify` desde `backend/` con el suelo de `trust` en `0.57` — CA-10 y CA-11
- [ ] T6 · `orquestador` · Commit `test(trust): ...` y comprobación de CA-12 (`git diff --stat 8557294...HEAD -- backend/*/src/main` vacío)
- [ ] T7 · `revisor` · Revisión del diff de la rama, con su propia ejecución de la mutación en `trust`
- [ ] T8 · `orquestador` · Cierre: evidencia, `pr.md` con la matriz y la evidencia (sin push ni `gh pr create` en esta sesión)

## Evidencia

Salida real de los comandos, pegada al cerrar la spec. Mutadores: grupo DEFAULTS
de PIT 1.30.2.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo | `mvn -B -Pmutacion -pl trust -am verify` (antes de T3) | |
| Verde | `mvn -B -Pmutacion -pl trust -am verify` (después de T4) | |
| Build y cobertura | `mvn -B verify` (desde `backend/`) | |
| Sin cambios en producción | `git diff --stat 8557294...HEAD -- backend/*/src/main` | |
| Revisión | `revisor` | |

## Cambios durante la implementación

- La spec se aprobó con sus 12 criterios. El mapa mostró que la cobertura de
  `trust` no puede subir con pruebas solo del motor (ya está al 100 %; el resto
  es `TrustQueries`), así que CA-11 se cumple con el suelo en `0.57`. No cambia
  ningún criterio.

## Qué hubo que corregirle a la IA

- El `analista` propuso comprobar el detalle de las señales con
  `contains(String.valueOf(N))`. Con N = 4 esa aserción pasa siempre, porque el
  texto ya contiene «24 h». El plan usa `startsWith(N + " ")`. Lo detectó el
  orquestador al escribir el plan.

## Observaciones sobre el sistema de agentes

- El hook rechazó `ls` al orquestador (`ls specs/ ...`): su lista solo admite
  `git` y `gh`. Se resolvió con `Glob`. Es coherente con la política, pero la
  skill `spec-sdd` dice «lista `specs/`» sin decir con qué herramienta.
- La sesión se cortó por el límite de uso después de escribir `spec.md` y de
  recibir el informe del `analista`. Al retomar no se volvió a encargar el mapa:
  el informe ya estaba en la conversación y la spec no había cambiado.
- La regla «la prueba antes que el código» y el paso «Verde» del flujo
  (`dev-backend`, commit `feat`/`fix`) no encajan con una spec que solo fija
  comportamiento existente: aquí no hay `dev-backend` ni commit `feat`, y el
  rojo es un informe de mutación, no una prueba que falla. Convendría que el
  flujo del orquestador y `spec-sdd` contemplaran este tipo de spec.
