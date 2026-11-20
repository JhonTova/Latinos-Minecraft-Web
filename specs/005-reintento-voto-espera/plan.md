# Plan 005 — El reintento de un voto aceptado dice cuánto falta para el próximo

Spec: [spec.md](spec.md)

## Mapa del cambio

Lo encontró el `analista` el 19/11/2026. El mapa está completo.

- **Módulo:** solo `voting` (🔴 crítico). El cambio de comportamiento está en
  `CastVoteService`. Hay dos consumidores en `app` que no cambian de código:
  `VoteController.java:48-56` y `VoteTokenController.java:96-108`. Los dos
  convierten `proximoVotoEn` a segundos con `toSeconds()`.
- **Contrato:**
  - `/api/v1/plugin/*` no cambia: `PluginController.java:128` solo usa
    `puedeVotar`.
  - El JSON público mantiene su forma. `segundosHastaProximoVoto` ya es un
    `Long` que puede ser nulo (`VoteController.java:80`). Solo pasa de `null`
    a un número en los reintentos de votos aceptados.
  - El frontend no lee ese campo: no aparece en ningún archivo de
    `frontend/src`.
- **Datos:**
  - `Vote.creadoEn` siempre tiene valor. `VoteRepository.mapear` (línea 206) lo
    lee de `created_at TIMESTAMPTZ NOT NULL` (`V4__votos.sql:63`), y es un
    instante absoluto, así que no hay problema de zona horaria.
  - `VoteSettingsRepository.actuales()` lanza un `SELECT` por clave primaria y
    no usa caché, a propósito. Si falla, devuelve `porDefecto()` (24 h).
- **Pruebas existentes:**
  - `backend/voting/src/test/java/com/latinosmc/voting/CastVoteServiceTest.java`
    usa Mockito en modo `LENIENT`, con
    `Clock.fixed(2026-08-03T12:00:00Z, UTC)` y una ventana de 24 h preparada
    en `@BeforeEach`.
  - Las pruebas `idempotencia` (107-120) y `carreraDeIdempotencia` (247-262)
    no comprueban `proximoVotoEn` y seguirán en verde.
  - No hay pruebas de `VoteController` ni de `VoteTokenController`.
- **Cobertura:** `cobertura.suelo` de `voting` es `0.40`
  (`backend/voting/pom.xml:23`).
- **ADR que aplican:**
  - [ADR-0011](../../docs/decisiones/0011-ventana-de-voto.md): ventana
    deslizante, semiabierta, en UTC. Un rechazo no consume la ventana.
  - [ADR-0023](../../docs/decisiones/0023-la-ventana-de-voto-es-de-la-plataforma.md):
    la ventana es una sola y la fija la plataforma, en `vote_settings`.
  - ADR-0003: el outbox va en la misma transacción. El reintento no publica
    nada.
  - ADR-0021: las pruebas no usan base de datos.
  - [ADR-0032](../../docs/decisiones/0032-cobertura-con-trinquete-y-mutacion.md):
    cobertura con trinquete y mutación en los módulos 🔴.
- **Paradas obligatorias:** ninguna (ver la spec).

## Decisiones técnicas

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| D1. Calcular el restante en `CastVoteService` con un método privado `restante(Vote, Instant ahora, Duration ventana)`. Ese método aplica `min(V, max(0, V − (ahora − creadoEn)))`. `Resultado.repetido` recibe el `Duration` ya calculado o `null`. | Calcularlo en los controladores. | Los controladores no conocen la ventana (ADR-0023) y son dos. El dato es de dominio y se prueba sin Spring. |
| D2. Usar la ventana **vigente** (`ajustes.actuales().ventana()`) en el instante del reintento. | Guardar la ventana del voto en `votes` (migración). | `votar` ya calcula las ventanas abiertas con la vigente (`CastVoteService.java:156-160`), así que el reintento y un voto nuevo dicen lo mismo. Guardarla exigiría una migración (parada). Pregunta P-2. |
| D3. Leer `ajustes.actuales()` **solo** si el voto repetido es `ACCEPTED`. | Leerlo siempre al principio. | Un reintento rechazado no necesita la ventana y se ahorra una consulta. CA-5 lo fija con `verify(ajustes, never()).actuales()`. |
| D4. En el paso 1, tomar `Instant.now(reloj)` antes de calcular. En el paso 7, reutilizar el `ahora` y las `reglas` que ya existen. | Tomar un segundo instante en el paso 7. | La carrera ocurre en la misma petición. Usar el mismo instante que se grabó como `creadoEn` evita que salga un restante distinto por milisegundos. |
| D5. Aplicar la fórmula solo a `VoteStatus.ACCEPTED` y devolver `null` para `REJECTED`, `PENDING` y `REVOKED`. | Usar `fueAceptado()` sin mirar su definición. | Un voto revocado no debería anunciar una espera. El `dev-backend` comprueba qué devuelve `fueAceptado()` y elige la condición que cumpla CA-5. Pregunta P-4. |
| D6. Poner como tope superior la ventana `V`. | Acotar solo por abajo. | Si las instancias tienen el reloj desfasado y `creadoEn > ahora`, se anunciaría más espera que la ventana entera. Pregunta P-3. |
| D7. Añadir una prueba unitaria de `VoteController` en `app/src/test`, sin contexto de Spring: el servicio se simula y la petición es un `MockHttpServletRequest`. Fija el «por HTTP» de CA-1 y CA-5: código `200`, `yaExistia` y segundos. | No probar el controlador. | Hoy nadie prueba la conversión `Duration` → segundos. Es barato y no toca código de `app/src/main`. Pregunta P-5. |

## Archivos que se tocan

| Archivo | Cambio | Agente |
|---|---|---|
| `backend/voting/src/test/java/com/latinosmc/voting/CastVoteServiceTest.java` | modifica: pruebas nuevas de CA-1 a CA-9 (`@Nested` «Reintento idempotente») | `pruebas-backend` |
| `backend/app/src/test/java/com/latinosmc/app/web/VoteControllerTest.java` | nuevo: CA-1 y CA-5 por HTTP (P-5 aprobada) | `pruebas-backend` |
| `backend/voting/src/main/java/com/latinosmc/voting/CastVoteService.java` | modifica: paso 1 (líneas 128-132), paso 7 (216-223), `Resultado.repetido` y su Javadoc (244-258) | `dev-backend` |
| `backend/voting/pom.xml` | `cobertura.suelo`, solo si sube. **Lo hace la sesión que abre el PR**, en su propio commit | — |

No se tocan `VoteController.java`, `VoteTokenController.java`,
`VoteRepository.java` ni `VoteSettingsRepository.java`.

## Matriz criterio → prueba

Valores comprobados con la fórmula de la spec, con `V = 24 h` y
`A = 2026-08-03T12:00:00Z`, el reloj fijo de la prueba. `C` es el `creadoEn`
del voto simulado.

| Criterio | Prueba (`CastVoteServiceTest › Reintento idempotente`, salvo indicación) | `C` | Esperado | Tipo |
|---|---|---|---|---|
| CA-1 | `aceptadoHaceSeisHorasDevuelveDieciocho` | `A − 6 h` | `18 h` (64 800 s), `yaExistia` | unitaria |
| CA-1 | `VoteControllerTest › reintentoAceptadoDevuelve200YSegundos` | — (servicio simulado: `18 h`) | `200`, `yaExistia=true`, `64800` | unitaria |
| CA-2 | `aceptadoEnElMismoInstanteDevuelveLaVentana` | `A` | `24 h` (86 400 s) | unitaria |
| CA-3 | `unSegundoAntesDeCerrarDevuelveUnSegundo` | `A − 24 h + 1 s` | `1 s` | borde |
| CA-3 | `justoAlCerrarLaVentanaDevuelveCero` | `A − 24 h` | `0` | borde |
| CA-4 | `unSegundoDespuesDeCerrarDevuelveCero` | `A − 24 h − 1 s` | `0` | borde |
| CA-4 | `tresVentanasDespuesDevuelveCero` | `A − 72 h` | `0`, nunca negativo | borde |
| CA-5 | `noAceptadoDevuelveNull` (parametrizada: `REJECTED`, `PENDING`, `REVOKED`) | `A − 6 h` | `null`, `verify(ajustes, never()).actuales()` | unitaria |
| CA-5 | `VoteControllerTest › reintentoRechazadoDevuelveNull` | — (servicio simulado: `null`) | `200`, `segundosHastaProximoVoto=null` | unitaria |
| CA-6 | `reintentoNoEvaluaNiEscribeNada` (para aceptado y rechazado) | `A − 6 h` | `verifyNoInteractions(directorio, antifraude, publicador)`; `never()` en `ultimoVotoAceptado*`, `identidadesDistintasDesdeIp`, `insertar`, `guardarSenales` y `crearRecompensaPendiente` | unitaria |
| CA-7 | `carreraConAceptadoAplicaLaFormula` | `A − 6 h` (el voto que gana la carrera) | `18 h`, `yaExistia`; `never()` en `guardarSenales`, `crearRecompensaPendiente` y `publish` | unitaria |
| CA-7 | `carreraConRechazadoDevuelveNull` | `A − 6 h` | `null` | unitaria |
| CA-8 | Pruebas existentes de voto nuevo y de `409` (`CastVoteServiceTest`) + `votoNuevoAceptadoSigueDevolviendoLaVentana` | — | `24 h`, `yaExistia=false` | regresión |
| CA-9 | `creadoEnElFuturoNoSuperaLaVentana` | `A + 1 h` | `24 h` (no `25 h`) | borde |
| CA-1 / D2 | `reintentoUsaLaVentanaVigente` (añadida en la revisión, V = 48 h) | `A − 6 h` | `42 h` (con 24 h fijas saldría 18 h) | unitaria |
| CA-7 / D2 | `carreraUsaLaVentanaVigente` (añadida en la revisión, V = 48 h) | `A − 6 h` | `42 h` | unitaria |
| CA-8 | `votoNuevoRechazadoSigueDevolviendoNull` y `VoteControllerTest › votoNuevoDevuelve201YLaVentana` (añadidas en la revisión) | — | `null` / `201`, `86400` | regresión |
| CA-10 | Revisión del diff por el `revisor` + `mvn -B -f backend/pom.xml verify` + `git diff --stat main` | — | Sin cambios en `plugin`, migraciones, ledger, `frontend/` ni `pom.xml` | revisión |

Mutación (ADR-0032): `mvn -B -Pmutacion -pl voting -am verify`. Se clasifican
en `tareas.md` los supervivientes de `CastVoteService`. Los bordes de CA-3,
CA-4 y CA-9 existen para matar los mutantes de `max`, `min` y de los límites.

## Riesgos y cómo se comprueban

- **Respuesta negativa o mayor que la ventana.** Si el jugador ve una cuenta
  atrás absurda, la confianza en el sitio se resiente. Lo cubren CA-3, CA-4 y
  CA-9, y la mutación.
- **Un reintento que vuelve a evaluar o a escribir.** Daría una segunda
  recompensa o un segundo evento, y eso falsea el ranking. Lo fija CA-6 con
  `verifyNoInteractions` y `never()`.
- **Consulta extra a `vote_settings` en cada reintento aceptado.** Es una
  consulta por clave primaria y su coste es despreciable. Si falla, la ventana
  cae a 24 h por defecto, como ya pasa en `votar`.
- **Cambia la respuesta del `201`.** CA-8 lo protege con las pruebas
  existentes.
- **Lo que no se comprueba:** el comportamiento con PostgreSQL real (ADR-0021)
  y la precisión de microsegundos de `created_at`. `toSeconds()` absorbe esa
  diferencia.

## Preguntas para el desarrollador

Respondidas el 19/11/2026. Se aprueban todas las propuestas: P-1 queda fuera
de alcance como riesgo conocido, y las decisiones D2, D5, D6 y D7 se aprueban
tal como están. Ver «Decisiones del desarrollador» en la spec.

- **P-1.** El reintento por token ya consumido (`VoteTokenController.java:69-74`)
  responde `null` fijo sin pasar por `votar`. Es el caso más común en
  `/vote-tokens`. Propuesta: dejarlo **fuera de alcance**, como dice ahora la
  spec, y abrir otra spec, porque arreglarlo toca `app` y `votoYaEmitido`.
  ¿Se acepta o se amplía esta spec?
- **P-2.** ¿Se usa la ventana **vigente** en el reintento (D2), aunque el
  administrador la haya cambiado después del voto?
- **P-3.** ¿Se acepta el tope superior `V` cuando `creadoEn` es posterior al
  reloj (D6, CA-9)?
- **P-4.** ¿`PENDING` y `REVOKED` repetidos devuelven `null`, como `REJECTED`
  (D5, CA-5)?
- **P-5.** ¿Se añade `VoteControllerTest` en `app/src/test` para el «por HTTP»
  (D7)? No toca código de producción de `app`.
- **P-6.** `cobertura.suelo`: si la cobertura de `voting` sube, se anota el
  valor en `tareas.md` y `pr.md`, y la sesión que abre el PR lo sube en un
  commit propio (CA-10). ¿Conforme?
