# Tareas 001 — Fijar el comportamiento del motor antifraude

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. Aquí no hay código de
producción: el «rojo» es el informe PIT con los mutantes vivos antes de las
pruebas nuevas, y el «verde», el mismo informe sin supervivientes en
`RuleBasedTrustEngine`.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe del 13/11/2026, resumido en «Mapa del cambio»
- [x] T2 · `pruebas-backend` · Rojo: ejecutar `mvn -B -Pmutacion -pl trust -am verify` **antes** de escribir pruebas y anotar en «Evidencia» los mutantes `SURVIVED` de `RuleBasedTrustEngine` (línea y mutador) — se comprueba con `backend/trust/target/pit-reports/mutations.xml`; se esperan los 10 del plan
- [x] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-5 en `RuleBasedTrustEngineLimitesTest` — se comprueba con `mvn -B -pl trust -am verify` en verde sin tocar `src/main` (si alguna falla, se para: la cifra de la spec o el motor no son lo que se creía)
- [x] T4 · `pruebas-backend` · Pruebas de CA-6 a CA-9 en la misma clase — mismo comando
- [x] T5 · `pruebas-backend` · Verde: volver a ejecutar PIT y clasificar cada mutante de `RuleBasedTrustEngine` (muerto / superviviente con motivo); `mvn -B verify` desde `backend/` con el suelo de `trust` en `0.57` — CA-10 y CA-11
- [x] T6 · `orquestador` · Commit `test(trust): ...` y comprobación de CA-12 (`git diff --stat 8557294...HEAD -- backend/*/src/main` vacío)
- [x] T7 · `revisor` · Revisión del diff de la rama, con su propia ejecución de la mutación en `trust`
- [x] T8 · `orquestador` · Cierre: evidencia, `pr.md` con la matriz y la evidencia (sin push ni `gh pr create` en esta sesión)

## Evidencia

Salida real de los comandos, ejecutados en esta sesión el 13/11/2026.
Mutadores: grupo DEFAULTS de PIT 1.30.2 (`pitest-junit5-plugin` 1.2.3). Maven
se ejecutó con `-f backend/pom.xml` (ver «Observaciones»), equivalente a
hacerlo desde `backend/`.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo | `mvn -B -Pmutacion -pl trust -am verify` antes de crear la prueba (`pruebas-backend`); reproducido por el `revisor` con `-DexcludedTestClasses=com.latinosmc.trust.RuleBasedTrustEngineLimitesTest` | `Generated 29 mutations Killed 11 (38%)` · `Mutations with no coverage 8. Test strength 52%` · `Line Coverage 33/57 (58%)`. 10 `SURVIVED`, todos en `RuleBasedTrustEngine.evaluar` (tabla abajo) |
| Verde | `mvn -B -Pmutacion -pl trust -am verify` con la prueba nueva (`pruebas-backend` y, de nuevo, `revisor`) | `Generated 29 mutations Killed 21 (72%)` · `Mutations with no coverage 8. Test strength 100%` · `Ran 152 tests`. Los 21 mutantes de `RuleBasedTrustEngine`, `KILLED`; 0 `SURVIVED` en el módulo |
| Pruebas de `trust` | `mvn -B -pl trust -am verify` | `Tests run: 35, Failures: 0, Errors: 0` (27 nuevas + 8 previas) |
| Build y cobertura | `mvn -B verify` | `BUILD SUCCESS` en los 11 módulos; Tests run: shared 50, identity 27, servers 79, voting 37, trust 35, billing 22, app 106, todos con 0 Failures/Errors/Skipped. JaCoCo: «All coverage checks have been met». `trust`: 33/57 líneas (57,9 %); suelo `0.57` sin cambios |
| Sin cambios en producción | `git diff --stat 8557294...HEAD -- backend/*/src/main` | Salida vacía (orquestador y `revisor`) |
| Revisión | `revisor` | Sin hallazgos altos. 1 medio y 3 bajos (abajo) |

### Mutantes de `RuleBasedTrustEngine` (clasificación, ADR-0032)

| Línea | Mutador | Antes | Después | Lo mata |
|---|---|---|---|---|
| 55 | NegateConditionals | KILLED | KILLED | CA-1, CA-2 |
| 56 | Increments | KILLED | KILLED | CA-2 |
| 62 | ConditionalsBoundary | **SURVIVED** | KILLED | CA-3 (N = 3) |
| 62 | NegateConditionals | KILLED | KILLED | CA-3 |
| 63 | Math (resta→suma) | **SURVIVED** | KILLED | CA-3 (N = 4) |
| 63 | Math (multiplicación→división) | KILLED | KILLED | CA-3 |
| 64 | Math | KILLED | KILLED | CA-3, CA-6 |
| 70 | ConditionalsBoundary | **SURVIVED** | KILLED | CA-4 (V = 6) |
| 70 | NegateConditionals | KILLED | KILLED | CA-4 |
| 71 | Math (dos mutantes) | **SURVIVED** ×2 | KILLED ×2 | CA-4 (V = 7, V = 10) |
| 72 | Math (suma→resta) | **SURVIVED** | KILLED | CA-4, CA-6 |
| 81 | NegateConditionals | KILLED | KILLED | CA-5 |
| 82 | Increments | **SURVIVED** | KILLED | CA-5 (H = 0) |
| 84 | ConditionalsBoundary | **SURVIVED** | KILLED | CA-5 (H = 10) |
| 84 | NegateConditionals | **SURVIVED** | KILLED | CA-5 (H = 1, 9) |
| 85 | Increments | KILLED | KILLED | CA-5, CA-7 |
| 91 | ConditionalsBoundary | **SURVIVED** | KILLED | CA-8 (riesgo 60) |
| 91 | NegateConditionals | KILLED | KILLED | CA-8 |
| 92, 94 | NullReturnVals | KILLED | KILLED | CA-1, CA-8 |

Sin supervivientes que clasificar como «falta una prueba» ni como
«equivalente». Los 8 `NO_COVERAGE` son de `TrustQueries` (JDBC), fuera de
alcance por la deuda de ADR-0021.

### Hallazgos del `revisor`

| # | Severidad | Dónde | Qué | Resolución |
|---|---|---|---|---|
| 1 | Media | `tareas.md` | Evidencia vacía y T2–T8 sin marcar; el rojo no constaba en el repositorio porque `pit-reports/` está ignorado | Corregido por el orquestador en este cierre |
| 2 | Baja | `plan.md`, fila CA-6 | Atribuía a CA-6 matar L64, que ya estaba muerto | Corregido (ver «Cambios durante la implementación») |
| 3 | Baja | `RuleBasedTrustEngineLimitesTest`, caso >100 de CA-7 | No comprueba la lista de señales ni el motivo | Aceptado sin cambio: el `revisor` lo marcó opcional; las aportaciones las fijan CA-3, CA-4 y CA-6 y el motivo, CA-8 |
| 4 | Baja | `unaIdentidadVerificableNoSumaPorIdentidad` | Misma entrada que CA-1 (redundante) | Aceptado sin cambio: el `revisor` indicó que no hace falta corregirlo |

## Cambios durante la implementación

- La spec se aprobó con sus 12 criterios. El mapa mostró que la cobertura de
  `trust` no puede subir con pruebas solo del motor (ya está al 100 %; el resto
  es `TrustQueries`), así que CA-11 se cumple con el suelo en `0.57`. No cambia
  ningún criterio.
- A petición del desarrollador, P-5 a P-8 (salidas del mapa) se movieron del
  plan a la spec, junto a P-1–P-4, antes del commit `docs(specs)`.
- `plan.md`: la fila de CA-6 de la matriz decía que mataba L64 y L72. L64 ya
  estaba muerto antes de las pruebas; queda «L72, también cubierto por CA-4»
  (hallazgo 2 del `revisor`).
- No hubo `dev-backend` ni commit `feat`: los commits son `docs(specs)`,
  `test(trust)` y el de cierre `docs(specs)`, como preveía el plan.

## Qué hubo que corregirle a la IA

- El `analista` propuso comprobar el detalle de las señales con
  `contains(String.valueOf(N))`. Con N = 4 esa aserción pasa siempre, porque el
  texto ya contiene «24 h». El plan usa `startsWith(N + " ")`. Lo detectó el
  orquestador al escribir el plan.
- El orquestador escribió en la matriz del plan que CA-6 mataba L64 sin
  comprobarlo contra el informe del rojo (en el informe del `analista` L64 ya
  figuraba como muerto). Lo detectaron `pruebas-backend` y el `revisor`.
- El orquestador no marcó T2–T6 en el commit `test(trust)` que las completó,
  como pide `spec-sdd`; quedaron marcadas en el commit de cierre. Lo señaló el
  `revisor` (hallazgo 1).

## Observaciones sobre el sistema de agentes

- El hook rechazó `ls` al orquestador (`ls specs/ ...`): su lista solo admite
  `git` y `gh`. Se resolvió con `Glob`. Es coherente con la política, pero la
  skill `spec-sdd` dice «lista `specs/`» sin decir con qué herramienta.
- El hook rechazó a `pruebas-backend` `cd backend && mvn ... 2>&1 | tail`
  (no admite `cd`, redirecciones ni tuberías). Lo resolvió con
  `mvn -f <ruta>/backend/pom.xml`. `AGENTS.md` y `backend/AGENTS.md` dan los
  comandos «desde `backend/`», que tal cual no se pueden ejecutar bajo el hook.
- La sesión se cortó por el límite de uso después de escribir `spec.md` y de
  recibir el informe del `analista`. Al retomar no se volvió a encargar el mapa:
  el informe ya estaba en la conversación y la spec no había cambiado.
- La regla «la prueba antes que el código» y el paso «Verde» del flujo
  (`dev-backend`, commit `feat`/`fix`) no encajan con una spec que solo fija
  comportamiento existente: aquí no hay `dev-backend` ni commit `feat`, y el
  rojo es un informe de mutación, no una prueba que falla. Convendría que el
  flujo del orquestador y `spec-sdd` contemplaran este tipo de spec.
- El informe de PIT vive en `target/`, ignorado por git, y se sobrescribe en
  cada ejecución: la evidencia del rojo solo sobrevive si alguien la copia a
  `tareas.md`. El `revisor` pudo reproducirla excluyendo la clase nueva con
  `-DexcludedTestClasses`, una técnica que convendría documentar en
  `docs/calidad/README.md`.
- Todos los subagentes respetaron sus permisos; ninguno tuvo que repetir su
  encargo.
