# Plan 004 — Decir cuántos intentos quedan al verificar el correo

Spec: [spec.md](spec.md)

## Mapa del cambio

Lo encontró el `analista` el 17/11/2026 y el orquestador lo comprobó en los
puntos clave.

- **Módulos.** `identity` (🔴 crítico), `app.web` (`ManejadorDeErrores`) y el
  frontend (`src/lib/` y `formulario-verificar.tsx`).
- **Sospecha del rollback confirmada: hay un fallo de seguridad en
  producción.** `verificar` lleva `@Transactional` sin atributos
  (`EmailVerificationService.java:94`). Llama a `sumarIntento` en la línea 109 y
  lanza `DomainException.Conflict` en la 110, y esa clase extiende
  `RuntimeException` (`shared/.../DomainException.java:14`). Spring revierte
  por defecto ante una `RuntimeException`, así que el `UPDATE attempts =
  attempts + 1` se deshace. En `backend/` no hay ningún `noRollbackFor`,
  `REQUIRES_NEW` ni `TransactionTemplate`. `AuthController.verificar`
  (`AuthController.java:92-98`) no abre ninguna transacción propia. Resultado:
  `attempts` nunca sube y el límite de 5 intentos no protege nada. Solo queda el
  control de tráfico por IP.
- **Endpoints.** `POST /api/v1/auth/verificar` (`AuthController.java:92`) y
  `POST /api/v1/auth/reenviar-codigo` (`:101`). Ya están en la lista blanca del
  puente (`frontend/src/lib/puente.ts:18`) y no hay que tocarla.
- **Pruebas existentes.** En identity solo hay `AuthServiceTest` y
  `PasswordsTest`; el patrón es `Clock.fixed`, `MockitoExtension` y `@Nested`.
  Ninguna prueba del backend levanta un contexto de Spring. Para la prueba
  transaccional bastan las dependencias que ya están: `spring-tx` (por
  `spring-boot-starter-jdbc`), `spring-test`, `spring-context` y Mockito.
  `ManejadorDeErroresTest` existe, pero no cubre `segundosRestantes` ni
  `limitePorConexion`.
- **Cobertura y mutación.** `cobertura.suelo = 0.15` en
  `backend/identity/pom.xml:23`. PIT está activo para identity
  (`mutacion.omitir=false`, `:24`).
- **Frontend.** `mensajeDeError` (`ui.tsx:174-181`) consume el cuerpo de la
  `Response`. El patrón que hay que seguir es el de la spec 002:
  `src/lib/rechazo-de-voto.ts`, que compara `title` y valida el tipo de la
  propiedad.
- **ADR que aplican.**
  - [ADR-0006](../../docs/decisiones/0006-openapi-fuente-de-verdad.md):
    Problem Details. La propiedad va como extensión del problema. No hay ningún
    archivo `openapi*` en el repositorio que actualizar.
  - [ADR-0001](../../docs/DECISIONES.md): monolito modular. La excepción vive
    en identity y `app` la importa, que es la dirección permitida.
  - [ADR-0021](../../docs/decisiones/0021-reintento-de-eventos-de-pago.md): las
    pruebas no usan PostgreSQL.
  - [ADR-0031](../../docs/decisiones/0031-vitest-en-el-frontend.md): solo se
    prueba con Vitest la lógica de `src/lib/`.
  - [ADR-0032](../../docs/decisiones/0032-cobertura-con-trinquete-y-mutacion.md):
    trinquete de cobertura y mutación en identity.
  - ADR-0007: deuda técnica, por lo que queda diferido.
- **Paradas obligatorias:** ninguna. No se toca `/api/v1/plugin/*`, no hay
  migración (la columna `attempts` ya existe, `V10__verificacion_de_email.sql:31`),
  no se toca el ledger ni se agrega ninguna dependencia. Sí hay una **parada de
  seguridad**: el arreglo del rollback (pregunta P-1).

## Decisiones técnicas

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| **D1.** Una excepción anidada `EmailVerificationService.IntentoRechazadoException`, que extiende `DomainException.Conflict` y lleva `int intentosRestantes`. Se usa para `verification.invalid_code` y para `verification.too_many_attempts` (este último con 0). | Dos clases, una por código. | Es el patrón de `CastVoteService.VentanaAbiertaException`. Un solo manejador pone la propiedad en los dos casos (CA-1 y CA-2). `type` y `title` siguen siendo los mismos códigos de hoy. |
| **D2.** `@Transactional(noRollbackFor = IntentoRechazadoException.class)` en `verificar`. | `sumarIntento` en `REQUIRES_NEW` (necesita otro bean, porque una llamada interna no pasa por el proxy). También se descartó devolver un resultado en vez de lanzar, porque cambiaría el controlador y el contrato. | Es el cambio más pequeño y explícito. Con código incorrecto, la única escritura previa es `sumarIntento`, que es justo la que debe persistir. Con `too_many_attempts` no hay ninguna escritura, así que el commit no guarda nada. `expired` y cualquier otra excepción siguen revirtiéndose como hoy. |
| **D3.** `intentosRestantes = max(0, MAX_INTENTOS − (N + 1))`, calculado en el servicio con el `N` leído. | `sumarIntento` con `UPDATE … RETURNING attempts` o `SELECT … FOR UPDATE`. | Hace falta para la carrera, que la spec deja fuera de alcance. El `max(0, …)` garantiza que nunca salga un número negativo. La decisión queda abierta en P-3. |
| **D4.** Un `@ExceptionHandler` nuevo en `ManejadorDeErrores` para `IntentoRechazadoException`, con `setProperty("intentosRestantes", int)`. | Meter la propiedad en el manejador genérico `Conflict`. | El genérico debe seguir sin propiedades (CA-4). Además, Spring elige el manejador de la excepción más específica. |
| **D5.** CA-3 se prueba con un `AnnotationConfigApplicationContext` que tiene `@EnableTransactionManagement`, un `PlatformTransactionManager` simulado con Mockito y el servicio real (proxy CGLIB) con el repositorio simulado. La prueba exige `commit` y ningún `rollback` cuando el código es incorrecto, y comprueba que se llamó a `sumarIntento`. | H2 o Testcontainers, que serían una dependencia nueva. | Demuestra la semántica real del proxy, que es donde está el fallo. No demuestra el `UPDATE` en PostgreSQL: esa es la deuda de ADR-0021. Una prueba solo con Mockito no detecta este fallo; por eso hace falta esta. |
| **D6.** El frontend tiene una función pura en `frontend/src/lib/intentos-de-verificacion.ts`. `textoDeIntentos(x: unknown): string \| null` cubre CA-9 a CA-11. `avisoDeVerificacion(problema: unknown): string \| null` cubre CA-12 y CA-13: con `invalid_code` devuelve «El código no es correcto.» más el texto de intentos; con `too_many_attempts` devuelve el texto de CA-10; con cualquier otra cosa, `null`. | Meter la lógica en el componente, que no tiene pruebas (ADR-0031). | Sigue el patrón de `rechazo-de-voto.ts` de la spec 002. El texto lo pone el frontend, con tildes, y no muestra el `detail` del backend. |
| **D7.** En `formulario-verificar.tsx`: `const problema = await respuesta.clone().json().catch(() => null)`. Si `avisoDeVerificacion(problema)` devuelve texto, se pinta ese; si no, `mensajeDeError(respuesta)`, como hoy. | Cambiar la firma de `mensajeDeError` en `ui.tsx`. | `ui.tsx` no se toca y el resto de pantallas quedan igual (CA-13). |
| **D8.** Textos propuestos. CA-10: «No te quedan intentos. Pide un código nuevo con «No me ha llegado, enviar otro».» Con `invalid_code` y 0 intentos: «El código no es correcto. No te quedan intentos. Pide…». Con 1 a 4: «El código no es correcto. Te quedan N intentos.» | — | Se puede cambiar en P-5 sin tocar el diseño. |
| **D9.** Los límites se fijan con pruebas de comportamiento (CA-5 a CA-7) con `Clock.fixed`, y con `ArgumentCaptor` sobre `guardar` y `codigoVigente`. Las constantes no se hacen públicas. | Exponer las constantes para la prueba. | Una prueba que lee la constante no detecta un cambio de valor. Una que mide `ahora + 15 min` sí lo detecta. |

## Archivos que se tocan

| Archivo | Cambio | Agente |
|---|---|---|
| `backend/identity/src/test/java/com/latinosmc/identity/EmailVerificationServiceTest.java` | nuevo (CA-1, CA-2, CA-5 a CA-8) | `pruebas-backend` |
| `backend/identity/src/test/java/com/latinosmc/identity/EmailVerificationServiceTransaccionTest.java` | nuevo (CA-3) | `pruebas-backend` |
| `backend/app/src/test/java/com/latinosmc/app/web/ManejadorDeErroresTest.java` | modifica (CA-1, CA-2 y CA-4, más la no regresión de `segundosRestantes` y `limitePorConexion`) | `pruebas-backend` |
| `backend/identity/src/main/java/com/latinosmc/identity/EmailVerificationService.java` | modifica (D1, D2, D3) | `dev-backend` |
| `backend/app/src/main/java/com/latinosmc/app/web/ManejadorDeErrores.java` | modifica (D4) | `dev-backend` |
| `frontend/src/lib/intentos-de-verificacion.test.ts` | nuevo (CA-9 a CA-13) | `pruebas-frontend` |
| `frontend/src/lib/intentos-de-verificacion.ts` | nuevo | `dev-frontend` |
| `frontend/src/components/formulario-verificar.tsx` | modifica (D7) | `dev-frontend` |

No se tocan `VerificationRepository.java` (salvo que P-3 diga lo contrario),
`ui.tsx`, `puente.ts`, `api.ts`, `tipos.ts`, migraciones ni ningún `pom.xml`.
Ningún rol puede editar `pom.xml`. Si la cobertura de identity sube, el valor
medido se anota en `tareas.md` y `pr.md`, y la sesión que lanzó al orquestador
sube `cobertura.suelo` en un commit propio.

## Matriz criterio → prueba

| Criterio | Prueba | Tipo |
|---|---|---|
| CA-1 | `EmailVerificationServiceTest › CodigoIncorrecto › restaN` (parametrizada `N = 0..4` → 4..0, comprueba `code`, `intentosRestantes` y `sumarIntento(id)`) + `ManejadorDeErroresTest › intentoRechazadoLlevaIntentosRestantesEntero` + `› springEnrutaElIntentoRechazadoAlManejadorEspecifico` (añadida por el hallazgo 5 del revisor; con `ExceptionHandlerMethodResolver`) | unitaria |
| CA-2 | `EmailVerificationServiceTest › IntentosAgotados › conCincoFallosNiSiquieraElCorrectoVale` (`code`, `intentosRestantes = 0`, sin `sumarIntento`, `marcarConsumido` ni `marcarEmailVerificado`) + `ManejadorDeErroresTest › intentosAgotadosLlevaCero` | unitaria |
| CA-3 | `EmailVerificationServiceTransaccionTest › codigoIncorrectoHaceCommitYNoRollback` (y, como control, `caducadoHaceRollback`) | integración con proxy de Spring, sin base de datos |
| CA-4 | `ManejadorDeErroresTest › conflictoGenericoSinIntentosRestantes`, `› ventanaAbiertaMantieneSegundosRestantes`, `› votoDiarioMantieneSegundosRestantes`, `› cupoDeIpMantieneLimite`, `› springEnrutaUnConflictoNormalAlManejadorGenerico` | unitaria |
| CA-5 | `EmailVerificationServiceTest › Vigencia › guardaConCaducidadDeQuinceMinutos`, `› buscaVigenteConElRelojInyectado`, `› sinVigenteRespondeExpiradoSinEscribir` | unitaria |
| CA-6 | `EmailVerificationServiceTest › CodigoIncorrecto › restaN(4)`, `› IntentosAgotados`, `› ConCuatroFallos › elCorrectoTodaviaVerifica` | unitaria |
| CA-7 | `EmailVerificationServiceTest › EsperaEntreEnvios › a59sRechaza`, `› a0sRechaza`, `› a60sExactosEnvia`, `› aMasDe60sEnvia`, `› sinEnvioPrevioEnvia` | unitaria |
| CA-8 | `EmailVerificationServiceTest › CaminoFeliz › conEspacios`, `› conGuion`, `› marcaConEmailDelCodigoYRelojInyectado`, `› codigoDeSeisDigitosEnElCorreoYSoloSuHashGuardado` | unitaria |
| CA-9 | `intentos-de-verificacion.test.ts › textoDeIntentos › plural y singular` | Vitest |
| CA-10 | `intentos-de-verificacion.test.ts › textoDeIntentos › cero y negativos piden código nuevo` | Vitest |
| CA-11 | `intentos-de-verificacion.test.ts › textoDeIntentos › valores no enteros devuelven null` | Vitest |
| CA-12 | `intentos-de-verificacion.test.ts › avisoDeVerificacion › invalid_code con 2, con 0 y sin propiedad` + `verificador-ui` en `/verificar` (móvil y escritorio, tema claro y oscuro) | Vitest + verificación visual |
| CA-13 | `intentos-de-verificacion.test.ts › avisoDeVerificacion › too_many_attempts` y `› otros títulos, no objeto y null devuelven null` + `verificador-ui` (fallo de red y error genérico) | Vitest + verificación visual |
| CA-14 | El `revisor` comprueba el diff (`git diff main...HEAD --stat`, sin plugin, migraciones, `pom.xml` ni `package.json`; constantes intactas) y se pegan las salidas de `mvn -B -f backend/pom.xml verify`, `npm run lint`, `npm run test` y `npm run build` | revisión + comandos |

**Mutación:** `mvn -B -Pmutacion -pl identity -am verify` sobre
`EmailVerificationService`. Los mutantes que sobrevivan se clasifican en
`tareas.md` (ADR-0032).

## Riesgos y cómo se comprueban

- **Este cambio activa de verdad el límite de 5 intentos en producción.**
  Desde el despliegue, un usuario que falle 5 veces tendrá que pedir otro
  código, cosa que hoy nunca pasa. Es el comportamiento que el código dice
  tener, pero los usuarios lo notarán, y por eso va el aviso de intentos.
  Lo comprueban CA-3 y CA-6.
- **`noRollbackFor` mal acotado.** Si se pusiera sobre `DomainException` o
  `Conflict`, un error futuro haría commit de escrituras a medias. Lo
  comprueban CA-3 (el control `caducadoHaceRollback`) y la revisión del
  `revisor`.
- **Carrera.** Con N = 4, varias peticiones en paralelo leen el mismo valor y
  todas se evalúan: un atacante tendría K intentos en vez de 5, limitado solo
  por el control de tráfico por IP. No se arregla aquí salvo que P-3 diga lo
  contrario. Ninguna prueba sin PostgreSQL lo detectaría (ADR-0021).
- **El frontend lee mal la respuesta.** Si se consume el cuerpo dos veces,
  `mensajeDeError` mostraría «No hay conexión con el servidor.». Lo detecta el
  `verificador-ui` con el caso de error genérico de CA-13.
- **Rompe la frontera entre módulos.** Lo detecta `ArquitecturaTest` dentro de
  `mvn verify`.

## Preguntas para el desarrollador

**Respondidas el 17/11/2026. El plan queda aprobado así:**

- **P-1.** El arreglo va en esta rama.
- **P-2.** Se usa `noRollbackFor`, acotado a la excepción nueva.
- **P-3.** La carrera queda fuera de alcance y es un riesgo conocido. Se
  arreglará en un `fix/` posterior con `SELECT … FOR UPDATE`.
- **P-4.** La prueba con el gestor de transacciones simulado vale. No
  demuestra que el `UPDATE` llegue a PostgreSQL.
- **P-5.** Los textos se aprueban. Con 0 intentos el aviso empieza por
  «El código no es correcto.».
- **P-6.** La spec 003 ya está en `main` (PR #17) y el número 004 se queda.

El texto original de cada pregunta:

- **P-1. ¿El arreglo del rollback va en esta rama o en un `fix/` aparte, antes?**
  Por defecto, en esta rama: sin él, `intentosRestantes` sería falso (siempre 4)
  y CA-1 no se podría cumplir. Si va aparte, esta spec espera a que se fusione
  ese PR y CA-3 sale de aquí. En los dos casos, la primera línea del PR dirá que
  se corrige un fallo de seguridad de `identity` que está en producción.
- **P-2. ¿Se acepta `noRollbackFor` acotado a la excepción nueva (D2)?** La
  alternativa es `REQUIRES_NEW` en un bean aparte.
- **P-3. La carrera de peticiones simultáneas.** Por defecto queda fuera y se
  abre una spec `fix/` justo después. La opción es meterla aquí con
  `SELECT … FOR UPDATE` en `codigoVigente`, que serializa los intentos sobre el
  mismo código. Es una línea de SQL, sin migración, pero ninguna prueba sin
  PostgreSQL puede demostrarla (ADR-0021).
- **P-4. ¿Basta la prueba de CA-3 con un `PlatformTransactionManager`
  simulado (D5)?** Demuestra la semántica del proxy, pero no el `UPDATE` real.
- **P-5. Textos (D8).** ¿Valen «Te quedan N intentos.», «Te queda 1 intento.» y
  «No te quedan intentos. Pide un código nuevo con «No me ha llegado, enviar
  otro».»? ¿Debe el aviso de 0 intentos llevar delante «El código no es
  correcto.»?
- **P-6. El número de la spec.** En `specs/` solo existen la 001 y la 002, así
  que la skill `spec-sdd` daría 003. Se usó 004 porque así lo pidió el
  desarrollador. ¿Hay una 003 en otra rama, o se renumera?
