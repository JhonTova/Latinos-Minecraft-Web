# Tareas 003 — Avisar al plugin de que el jugador ya votó a este servidor

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe «completo» del 13/11/2026
- [x] T2 · `orquestador` · Aprobación de spec y plan, y respuestas P-1 a P-7. Se comprueba con la respuesta del desarrollador y el commit `docs(specs): ...` con los tres documentos — aprobados el 17/11/2026
- [x] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-5 y CA-7 en `CastVoteServiceTest`, en rojo. Se comprueba con `cd backend && mvn -B -pl voting -am test`: fallan las de CA-1 y CA-2 («un segundo antes»), porque hoy devuelve `puede = true`, y no por un error de compilación ni de configuración.
- [x] T4 · `pruebas-backend` · Pruebas de regresión de CA-6 (regla global) y `PluginControllerTest` para CA-8 (aprobada en P-2). Se comprueba con que pasan en verde ya con el código actual; se explica en el informe por qué es lo correcto.
- [x] T5 · `orquestador` · Commit `test(voting): ...` con T3 y T4.
- [x] T6 · `dev-backend` · `puedeVotar` comprueba la ventana por servidor con la regla global desactivada, y se reemplaza el comentario citando ADR-0023. Se comprueba con `cd backend && mvn -B -pl voting -am test` en verde y `mvn -B -f backend/pom.xml verify` en verde (incluye `ArquitecturaTest` y el suelo de JaCoCo).
- [x] T7 · `orquestador` · Commit `feat(voting): ...`.
- [x] T8 · `revisor` · Revisión del diff completo de la rama, CA-9 y mutación con `mvn -B -Pmutacion -pl voting -am verify`. Clasifica los supervivientes de `puedeVotar` y lee la cobertura de `voting` contra el suelo `0.36`. Hubo dos vueltas y una tercera de confirmación. Ningún hallazgo alto; las correcciones están en los commits `0a521ed` y `d39857e`.
- [x] T9 · `orquestador` · Cierre: Evidencia, correcciones y `pr.md` con la parada del contrato `/api/v1/plugin/*` en la primera línea. Sin push ni PR en esta sesión.

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo (voting) | `cd backend && mvn -B -pl voting -am test` (lo ejecutó `pruebas-backend`) | `Tests run: 46, Failures: 3, Errors: 0`, BUILD FAILURE. Fallan por aserción: CA-1 (esperado `[false, YA_VOTO_AQUI, PT18H]`, obtenido `[true, null, PT0S]`), CA-2 un segundo antes (esperado `[false, YA_VOTO_AQUI, PT1S]`, obtenido `[true, null, PT0S]`) y CA-5 consulta correcta (`Expecting value to be false but was true`). Las demás pruebas nuevas pasan, como se esperaba. |
| CA-8 (contrato) | `cd backend && mvn -B -pl app -am test -Dtest=PluginControllerTest -Dsurefire.failIfNoSpecifiedTests=false` | `Tests run: 1, Failures: 0`, BUILD SUCCESS; verde desde el principio a propósito, porque fija el contrato que no cambia |
| Verde (voting) | `cd backend && mvn -B -pl voting -am test` (lo ejecutó `dev-backend`) | `shared` 50 y `voting` 46 pruebas, todas con Failures 0 y Errors 0, BUILD SUCCESS |
| Suite completa | `mvn -B -f backend/pom.xml verify` (lo ejecutó `dev-backend`) | `shared` 50, `identity` 27, `servers` 79, `voting` 46, `trust` 35, `billing` 22 y `app` 107 pruebas, todas con Failures 0 y Errors 0 (`ArquitecturaTest` 4/4, `PluginControllerTest` 1/1). `jacoco:check` dice «All coverage checks have been met». BUILD SUCCESS |
| Cobertura de `voting` | `backend/voting/target/site/jacoco/jacoco.csv`, tras el `verify` | 137 de 336 líneas cubiertas, es decir **0,408**, por encima del suelo de 0,36. `CastVoteService` tiene 80 de 81 líneas. El suelo no se sube en esta rama (P-6) |
| Correcciones (M-1, B-1, B-2, B-3) | `cd backend && mvn -B -pl voting -am test` (lo ejecutó `pruebas-backend`) | `voting`: 48 pruebas, 0 fallos y 0 errores; `CastVoteServiceTest`: 31. BUILD SUCCESS. `PluginControllerTest`: 1 prueba, 0 fallos |
| Suite completa (2.ª vuelta) | `mvn -B -q -f backend/pom.xml verify` (lo ejecutó `revisor`) | Sin BUILD FAILURE. En surefire no hay fallos ni errores: `CastVoteServiceTest` 31, `PluginControllerTest` 1, `ArquitecturaTest` 4 |
| Mutación | `cd backend && mvn -B -q -Pmutacion -pl voting -am verify` (lo ejecutó `revisor`, en las dos vueltas) | Módulo `voting`: 106 mutantes, 61 matados, 1 superviviente y 44 sin cobertura; fuerza de las pruebas, 98 %. **`puedeVotar`: 9 mutantes, los 9 matados** (condición negada en las líneas 81, 83, 85, 101 y 103; retorno nulo en las 86, 91, 104 y 107). No queda ningún superviviente que clasificar. Informe: `backend/voting/target/pit-reports/mutations.xml` |
| Mutantes de `CastVoteService` fuera de `puedeVotar` | Mismo informe | Se listan sin clasificar (P-5). Superviviente: línea 317, `CupoDeIpAgotadoException.<init>` (condición `limite == 1` negada). Para el `revisor`, falta una prueba del texto del mensaje; es anterior a esta rama y queda fuera del diff. Sin cobertura: líneas 222 (lambda de `votar`), 299 y 324 |
| Cobertura de `voting` (2.ª vuelta) | `backend/voting/target/site/jacoco/jacoco.csv` | 137 de 336 líneas, **0,408** (suelo: 0,36). `CastVoteService`: 80 de 81 líneas y 33 de 36 ramas. **Valor que hay que llevar al suelo**: lo sube la sesión que lanzó al orquestador, en un commit propio **en esta misma rama**, después del cierre del orquestador y fuera del sistema de agentes (P-6). Al cerrar esta tarea, el commit todavía no existe |
| Revisión, 1.ª vuelta | `revisor` sobre `git diff feat/002-espera-voto-diario...HEAD` | Se puede abrir el PR. Hallazgos medios: M-1 (todas las pruebas de `puedeVotar` usaban una ventana de 24 h, así que un 24 h fijo pasaba sin que se notara) y M-2 (la Evidencia estaba incompleta). Bajos: B-1 (la prueba de «otro servidor» no podía fallar) y B-2 (`PluginControllerTest` no comprobaba la identidad) |
| Revisión, 2.ª vuelta | `revisor`, después de `0a521ed` | M-1, B-1 y B-2 cerrados. Hallazgo nuevo B-3 (bajo): etiqueta CA-2 en una prueba que es de CA-3. Se corrigió en `d39857e` |
| Revisión, 3.ª vuelta (confirmación) | `revisor` después de `d39857e` y `b4d550c`; `mvn -B -f backend/pom.xml verify` | **Se puede abrir el PR.** BUILD SUCCESS en los 11 módulos: `shared` 50, `identity` 27, `servers` 79, `voting` 48, `trust` 35, `billing` 22, `app` 107, sin fallos ni errores. `ArquitecturaTest` 4/4, `CastVoteServiceTest` 31/31 y `PluginControllerTest` 1/1. El revisor cotejó la matriz y las cifras de los documentos con el código, `mutations.xml` y `jacoco.csv`: no hay nada inventado. B-3 y M-2 quedan cerrados. Hallazgos nuevos: M-4 (medio), el suelo sigue en 0,36 frente al 0,408 medido y ADR-0032 pide subirlo en el mismo PR; se deja al commit propio acordado en P-6. B-4 (bajo), esta misma fila decía «ver la fila siguiente». B-5 (bajo), `pr.md` no mencionaba M-2. B-4 y B-5 los corrigió el orquestador en el commit de esta fila |

## Qué hubo que corregirle a la IA

- **Pruebas que no protegían el fundamento de la spec (M-1).** El
  `pruebas-backend` escribió todas las pruebas de `puedeVotar` con una ventana
  de 24 h, que coincide con la de los ajustes por defecto. Un `Duration.ofHours(24)`
  fijo en lugar de `reglas.ventana()`, que es justo lo que ADR-0023 prohíbe,
  habría pasado. PIT tampoco lo detecta, porque no muta llamadas a método. Lo
  encontró el `revisor`. Se corrigió con dos pruebas con una ventana de 12 h.
  Esas pruebas no se vieron en rojo: el código ya era correcto y el rol de
  pruebas no puede tocar `src/main`. Que pueden fallar se razonó, no se ejecutó.
- **Una prueba que no podía fallar (B-1).** La prueba de «voto solo en otro
  servidor» pasaba con cualquier `servidorId`, porque el `@BeforeEach` ya
  devolvía vacío. Lo encontró el `revisor` y se reescribió para que consulte
  los dos servidores.
- **Etiqueta de criterio equivocada (B-3).** La corrección de M-1 etiquetó como
  CA-2 un caso que es de CA-3. Lo encontró el `revisor` en la segunda vuelta;
  sin la corrección, la matriz del PR habría quedado mal.
- **Un comentario confuso en un informe.** El `dev-backend` escribió que
  `PluginControllerTest` «ejecuta una sola prueba» y que, «si CA-5 debía tener
  más casos», habría que revisarlo. Mezclaba la prueba de CA-8 con CA-5. No
  tuvo consecuencias: el orquestador lo comprobó contra el plan.

## Observaciones sobre el sistema de agentes

- **La sesión se cortó por el límite de uso** después de crear la rama, escribir
  `spec.md` y recibir el informe del `analista`, y antes de escribir el plan. Al
  reanudar se comprobó el estado con `git status` y `git branch --show-current`
  y se reutilizó el informe del analista, que estaba completo. No se repitió
  ningún paso.
- **El analista no marcó su mapa como incompleto** por la falta de `plan.md`.
  En el encargo se le avisó explícitamente, siguiendo lo aprendido en la 002, y
  `spec.md` no enlaza a `plan.md` hasta que existe.
- **El hook no deja a ningún rol editar `backend/*/pom.xml`.** La política
  (`.claude/hooks/politica.mjs`) limita `dev-backend` a `backend/*/src/main/` y
  `pruebas-backend` a `backend/*/src/test/`. ADR-0032 y `backend/AGENTS.md`
  piden subir `cobertura.suelo` en el mismo PR cuando la cobertura sube, y
  ningún agente puede hacerlo. Ver P-6 del plan. El desarrollador decidió que
  lo suba la sesión que lanzó al orquestador, fuera del sistema de agentes.
- **El hook bloqueó `git -C "<ruta>" log` y `git -C "<ruta>" diff` al
  `revisor`** en la primera vuelta. Lo resolvió ejecutando `git` sin `-C`. En
  los encargos siguientes se añadió «ni `git -C`» a la lista de formas que el
  hook rechaza, junto con las redirecciones, las tuberías y el `cd` con ruta
  absoluta.
- **El rol de pruebas no puede demostrar el rojo de una prueba añadida después
  del verde.** Las pruebas de la revisión (M-1, B-1 y B-2) pasaron en verde
  desde el principio, porque el código ya era correcto. Para verlas fallar
  habría que mutar `src/main`, y ningún rol puede a la vez tocar `src/main` y
  probar. Quedó como argumento escrito en el informe. Mejora posible: que el
  `revisor` las confirme con una mutación manual o con PIT, si el mutador
  cubre el caso.
- **Varias sesiones.** La etapa (a) se cortó por el límite de uso y se reanudó
  sin repetir pasos. La etapa (b) empezó con la aprobación del desarrollador,
  transmitida por la sesión que lanzó al orquestador.
- **La regla «el revisor después de cada vuelta» y el límite de 3 vueltas.**
  Hubo dos vueltas de corrección (`0a521ed` y `d39857e`) y ninguna repitió los
  hallazgos de la anterior.
