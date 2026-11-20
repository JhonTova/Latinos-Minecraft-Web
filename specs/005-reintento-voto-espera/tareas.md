# Tareas 005 — El reintento de un voto aceptado dice cuánto falta para el próximo

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md`. Comprobado: informe con
  `ESTADO: completo`, sin paradas obligatorias.
- [x] T2 · `orquestador` · Aprobación de spec y plan (P-1 a P-6) y commit
  `docs(specs): ...` con los tres documentos.
- [x] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-9 en
  `CastVoteServiceTest` (`@Nested` «Reintento idempotente»), en rojo.
  - Se comprueba con `cd backend && mvn -B -pl voting -am test`.
  - Las de CA-1 a CA-4, CA-7 (aceptado) y CA-9 tienen que fallar **por
    aserción**: hoy sale `null`, no el valor esperado.
  - CA-5, CA-6 y CA-8 pueden pasar ya, porque fijan lo que no cambia. Se
    declara cuáles pasan.
  - Antes de aceptar el rojo, el `orquestador` comprueba que cada valor
    esperado sale de la fórmula de la spec (tabla del plan).
- [x] T4 · `pruebas-backend` · `VoteControllerTest` (CA-1 y CA-5 por HTTP;
  P-5 aprobada).
  - Se comprueba con `cd backend && mvn -B -pl app -am test -Dtest=VoteControllerTest -Dsurefire.failIfNoSpecifiedTests=false`.
  - Puede pasar ya: fija la conversión a segundos, que no cambia.
- [x] T5 · `orquestador` · Commit `test(voting): ...` con T3 y T4.
- [x] T6 · `dev-backend` · `CastVoteService`: paso 1, paso 7 y
  `Resultado.repetido` (D1 a D6).
  - Se comprueba con `mvn -B -f backend/pom.xml verify` en verde, incluida
    `ArquitecturaTest`.
  - Se anota la cobertura de `voting` medida por JaCoCo.
- [x] T7 · `orquestador` · Commit `feat(voting): ...`.
- [x] T8 · `revisor` · Revisión del diff completo de la rama y mutación con
  `cd backend && mvn -B -Pmutacion -pl voting -am verify`.
  - Se clasifican aquí los mutantes supervivientes de `CastVoteService`.
  - Se comprueba CA-10 con `git diff --stat main`.
- [x] T9 · `orquestador` · Cierre: «Evidencia», «Qué hubo que corregirle a la
  IA» y `pr.md` con la matriz y el valor de cobertura medido.
  - No hace falta verificación visual: no se toca `frontend/`.

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

Datos tomados de los informes de `pruebas-backend` y del `revisor`. El
orquestador no puede ejecutar `mvn`.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo (commit `e2ddc9d`) | `cd backend && mvn -B -pl voting -am test` | `voting`: Tests run: 63, Failures: 8, Errors: 0, Skipped: 0, BUILD FAILURE. Fallan por aserción `aceptadoHaceSeisHorasDevuelveDieciocho` (expected: 18H but was: null), `aceptadoEnElMismoInstanteDevuelveLaVentana` (24H/null), `unSegundoAntesDeCerrarDevuelveUnSegundo` (1S/null), `justoAlCerrarLaVentanaDevuelveCero` (0S/null), `unSegundoDespuesDeCerrarDevuelveCero` (0S/null), `tresVentanasDespuesDevuelveCero` (0S/null), `carreraConAceptadoAplicaLaFormula` (18H/null) y `creadoEnElFuturoNoSuperaLaVentana` (24H/null). Ya pasaban, como regresión: CA-5, CA-6, CA-7 rechazado y CA-8. |
| Rojo, controlador | `cd backend && mvn -B -pl app -am test -Dtest=VoteControllerTest -Dsurefire.failIfNoSpecifiedTests=false` | Tests run: 2, Failures: 0. Pasa ya: fija una conversión que no cambia. |
| Verde (commits `3701c23` y `a1ad07e`) | `mvn -B -f backend/pom.xml verify` | BUILD SUCCESS en 02:50 min. `voting`: Tests run: 66, Failures: 0, Errors: 0, Skipped: 0 (`ReintentoIdempotente`: 18). `app`: Tests run: 118, Failures: 0, Errors: 0, Skipped: 0 (`VoteControllerTest`: 3, `ArquitecturaTest`: 4). «All coverage checks have been met» en todos los módulos. |
| Cobertura `voting` | JaCoCo, `backend/voting/target/site/jacoco/jacoco.csv`, LINE COVEREDRATIO, BUNDLE | 147/346 = **42,49 %**, con un suelo de 0.40. `CastVoteService`: 90 líneas cubiertas, 1 sin cubrir. No se midió `main`, así que no se sabe cuánto subió exactamente. Según P-6, la sesión que abre el PR sube `cobertura.suelo` en un commit propio. |
| Mutación | `cd backend && mvn -B -Pmutacion -pl voting -am verify` | BUILD SUCCESS. Módulo: 114 generados, 68 muertos, 2 supervivientes, 44 sin cobertura, fuerza 97 % (68/70). `CastVoteService`: 40 generados, 38 muertos, 1 superviviente, 1 sin cobertura, fuerza 97 %. |
| Revisión | `revisor`, 2 vueltas | Primera vuelta: 1 hallazgo medio y 4 bajos. Segunda vuelta: sin hallazgos nuevos y «se puede abrir el pull request». |

### Mutantes supervivientes y sin cobertura de `CastVoteService`

| Mutante | Estado | Clasificación |
|---|---|---|
| `restante`, línea 261: `compareTo(ventana) > 0` pasa a `>= 0` | Sobrevive | **Equivalente.** Solo difiere cuando `falta == ventana`, y entonces devolver `ventana` o `falta` da el mismo valor (CA-2). |
| Línea 230: lambda del `orElseThrow` («conflicto sin fila») | Sin cobertura | Ya existía y esta rama no lo cambia. Falta una prueba; queda para otro trabajo. |
| `CupoDeIpAgotadoException.<init>`, línea 342: `limite == 1` negado | Sobrevive | Ya existía y esta rama no lo cambia. Falta una prueba del mensaje con un límite distinto de 1; queda para otro trabajo. |
| `CupoDeIpAgotadoException.limite` (349) y `VotoDiarioConsumidoException.restante` (324) | Sin cobertura | Ya existían y esta rama no los cambia; quedan para otro trabajo. |

Todos los mutantes nuevos de `restante` y de las dos condiciones
`== VoteStatus.ACCEPTED` mueren, salvo el equivalente.

### Verificación visual

No hace falta `verificador-ui`: el cambio no toca `frontend/`, y el frontend
no lee `segundosHastaProximoVoto`.

## Qué hubo que corregirle a la IA

- **El primer borrador de la spec daba por hecho que el cambio llegaba al
  reintento por token** (`/api/v1/vote-tokens/{token}/vote`). El `analista`
  vio que el caso normal, con el token ya consumido, responde antes de llamar a
  `votar` (`VoteTokenController.java:69-74`) y devuelve `null` fijo. Se corrigió
  el «Problema», ese caso pasó a «Fuera de alcance» y queda la pregunta P-1.
- **El primer borrador no tenía en cuenta tres casos.** Los encontró el
  `analista` y se añadieron:
  - `creadoEn` en el futuro, por reloj desfasado (nuevo CA-9);
  - los estados `PENDING` y `REVOKED` (CA-5 ampliado);
  - el choque entre «ningún `pom.xml`» y el trinquete de cobertura de ADR-0032
    (CA-10 ajustado).
- **Las pruebas del rojo solo usaban una ventana de 24 h.** El `revisor` lo
  marcó como hallazgo medio: una ventana fija en el código, o
  `VoteSettings.porDefecto()`, habría pasado todas las pruebas, y PIT no genera
  ese mutante. El orquestador había revisado los valores contra la fórmula,
  pero no vio que todos salían de la misma `V`. `pruebas-backend` añadió dos
  pruebas con V = 48 h que esperan 42 h (commit `a1ad07e`). Como llegaron
  después del `feat`, nadie las vio en rojo: su capacidad de fallar se apoya en
  la aritmética (con 24 h fijas saldría 18 h), no en una ejecución.
- **Hallazgos bajos del `revisor`, corregidos en la misma vuelta:**
  - CA-6 comprobaba una lista cerrada de `never()`; ahora termina con
    `verifyNoMoreInteractions(repositorio)`;
  - faltaba probar en CA-8 el voto nuevo rechazado y el `201` del controlador.
- **El orquestador olvidó un `never()` en las pruebas que dejó la sesión
  cortada.** Al revisarlas vio que `carreraConRechazadoDevuelveNull` no
  comprobaba `crearRecompensaPendiente`, y `pruebas-backend` lo añadió antes
  del commit `test`.

## Observaciones sobre el sistema de agentes

- **La sesión se cortó a mitad del rojo** por el límite semanal de uso (HTTP
  429), mientras `pruebas-backend` escribía T3 y T4. El agente dejó las dos
  clases de prueba escritas, sin commit y sin informe.
  - Al reanudar, el orquestador miró `git status`, `git log` y `tareas.md`,
    leyó las pruebas que quedaban y comprobó sus valores contra la fórmula.
  - Después encargó a `pruebas-backend` completar un `never()` y ejecutar el
    rojo, en lugar de reescribirlas.
  - No hubo que repetir trabajo, pero el rojo no tuvo evidencia hasta esa
    segunda llamada.
- **El hook bloqueó a `pruebas-backend`** cuando usó `cd <ruta absoluta> && mvn`.
  El agente recurrió a `mvn -B -f backend/pom.xml`, como ya se sabía desde las
  specs 001 y 002. Habría que reforzar la indicación en la definición del agente.
- **`dev-backend` no pudo extraer el resumen `Tests run` por módulo.** Guardó la
  salida en un archivo y solo encontró las líneas de SUCCESS. Las cifras de
  «Evidencia» son del `revisor`. Al terminar no quedó ningún archivo suelto en
  el árbol (`git status` limpio, salvo el cambio esperado).
- **Ningún agente puede medir la cobertura de `main`.** El orquestador no
  ejecuta `mvn`, y no se encargó medirla. Por eso «cuánto subió» queda sin
  respuesta: se da el valor medido en la rama.
- El flujo del orquestador pide esperar la aprobación de la spec antes de
  llamar al `analista`. En esta sesión no interactiva se llamó sin esa
  aprobación, porque el encargo pedía los pasos 1 a 3 seguidos. Spec y plan se
  presentaron juntos y se aprobaron juntos.

- El flujo del orquestador pide esperar la aprobación de la spec antes de
  llamar al `analista`. En esta sesión no interactiva se llamó sin esa
  aprobación, porque el encargo pedía los pasos 1 a 3 seguidos. Spec y plan se
  presentan juntos para aprobarlos.
- Al orquestador no le bloqueó nada ningún hook. Usó `git switch -c`,
  `git add`, `git commit`, `git diff`, `git status` y `git log`, y editó solo
  dentro de `specs/`.
