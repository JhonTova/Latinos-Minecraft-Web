Sin cambios en `/api/v1/plugin/*`, sin migración, sin ledger y sin dependencias nuevas. Solo agrega pruebas: el comportamiento en producción no cambia.

## Qué cambia y por qué

La primera medición de mutación (`docs/calidad/README.md`, línea base del
13/11/2026) dejó 10 mutantes vivos en `RuleBasedTrustEngine.evaluar`. Se podían
mover los límites del antifraude (más de 3 identidades por IP, más de 6 votos en
una hora, historial de 10 votos, umbral de 60) o cambiar la aritmética del
puntaje sin que fallara ninguna prueba: las pruebas miraban que la señal
apareciera, no cuánto sumaba ni en qué valor exacto se disparaba.

Este PR agrega `RuleBasedTrustEngineLimitesTest` (27 casos en 14 métodos). Fija
el comportamiento **actual** del motor: el borde exacto de cada regla, su
aportación, sus topes, el recorte a 0–100, el umbral de rechazo y los argumentos
con que consulta a `TrustQueries`. No toca `src/main`.

- Spec: [`specs/001-fijar-motor-antifraude/spec.md`](spec.md) · Plan: [`plan.md`](plan.md) · Tareas y evidencia: [`tareas.md`](tareas.md)
- ADR: 0005 (reglas explicables), 0032 (cobertura con trinquete y mutación), 0021 (por qué `TrustQueries` queda fuera).

## Matriz criterio → prueba

Todas en `backend/trust/src/test/java/com/latinosmc/trust/RuleBasedTrustEngineLimitesTest.java`.

| Criterio | Prueba |
|---|---|
| CA-1 | `laPeticionBaseSeAceptaConRiesgoCeroYSinSenales` |
| CA-2 | `unaIdentidadNoVerificableSumaExactamente25`, `unaIdentidadVerificableNoSumaPorIdentidad` |
| CA-3 | `lasIdentidadesPorIpAportanSuCifraExacta` (N = 3, 4, 5, 6, 7, 30) |
| CA-4 | `losVotosEnUnaHoraAportanSuCifraExacta` (V = 6, 7, 10, 11, 50) |
| CA-5 | `elHistorialAportaSuCifraExacta` (H = 0, 1, 9, 10, 50) |
| CA-6 | `elRiesgoEsLaSumaExactaDeLasAportaciones` |
| CA-7 | `unaSumaNegativaSeRecortaACero`, `unaSumaMayorDeCienSeRecortaACien` |
| CA-8 | `unRiesgoDe55SeAceptaSinMotivo`, `unRiesgoDeExactamente60SeRechazaConSusSenales` |
| CA-9 | tres métodos `consulta…`, uno por consulta, con *stubs* de argumentos exactos |
| CA-10 | `mvn -B -Pmutacion -pl trust -am verify` (abajo) |
| CA-11 | `mvn -B verify` (abajo) |
| CA-12 | `git diff --stat 8557294...HEAD -- backend/*/src/main`, salida vacía |

## Evidencia

Mutación en `trust` (PIT 1.30.2, mutadores DEFAULTS):

| | Mutantes | Muertos | Sin cobertura | Sobreviven | Fuerza |
|---|---|---|---|---|---|
| Antes (rojo) | 29 | 11 | 8 | **10** | 52 % |
| Después (verde) | 29 | 21 | 8 | **0** | 100 % |

```
>> Line Coverage (for mutated classes only): 33/57 (58%)
>> Generated 29 mutations Killed 21 (72%)
>> Mutations with no coverage 8. Test strength 100%
```

- Los 21 mutantes de `RuleBasedTrustEngine` mueren. Los 8 sin cobertura son de
  `TrustQueries` (JDBC), deuda de ADR-0021. La clasificación línea a línea está
  en `tareas.md`.
- `mvn -B verify`: `BUILD SUCCESS` en los 11 módulos, 0 fallos;
  `trust` 35 pruebas (27 nuevas). JaCoCo: «All coverage checks have been met».
- Cobertura de `trust`: 33/57 = 57,9 %, igual que antes. El motor ya estaba al
  100 % y el resto es `TrustQueries`, así que `cobertura.suelo` se queda en
  `0.57` (ADR-0032: no baja).
- El `revisor` volvió a ejecutar la mutación, reprodujo el rojo excluyendo la
  clase nueva y no encontró hallazgos altos. Quedan dos bajos aceptados sin
  cambio, explicados en `tareas.md`.

## Preguntas abiertas para el desarrollador (no bloquean este PR)

Las pruebas fijan el comportamiento actual en todos estos casos. Si se decide
cambiar alguno, irá en otra spec. El detalle está en la spec, en «Preguntas
abiertas».

- **P-1.** Una identidad Mojang nueva desde una IP con 30 identidades suma 55 y se acepta.
- **P-2.** Una identidad offline con 10 votos previos desde una IP con 30 identidades suma 55 y se acepta.
- **P-3.** El recorte a 0 absorbe el −15 del historial: la suma de señales no siempre coincide con el puntaje.
- **P-4.** Las aportaciones van de 5 en 5, así que el umbral de 60 se comporta igual que uno de 56.
- **P-5.** Con una IP nula o en blanco se omiten las reglas de IP sin que quede ninguna señal.
- **P-6.** Los conteos por IP incluyen votos rechazados y al propio votante.
- **P-7.** `CastVoteService` y el motor usan ventanas y límites distintos para la misma IP.
- **P-8.** ADR-0005 pide reglas configurables en caliente, pero las cifras están fijas en el código.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
