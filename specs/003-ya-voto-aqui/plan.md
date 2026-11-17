# Plan 003 — Avisar al plugin de que el jugador ya votó a este servidor

Spec: [spec.md](spec.md)

## Mapa del cambio

Resumen del informe del `analista`, del 13/11/2026, con estado «completo».

- **Módulos.** `voting` (🔴 crítico): solo cambia `CastVoteService.puedeVotar` y
  su comentario. `app` (—): `PluginController.emitirToken` **no cambia**, porque
  ya pasa `motivo` y `restante` a `RespuestaToken.noPuedeVotar`
  (`PluginController.java:128-131`).
- **Repositorio.** Ya existe lo que hace falta:
  `VoteRepository.ultimoVotoAceptado(UUID servidorId, byte[] identidadHash)`
  (línea 52). Devuelve `Optional<Instant>` y filtra `status_code = 'ACCEPTED'`
  en el SQL. Es la misma consulta que usa `votar()`. No hace falta ninguna
  consulta nueva ni ninguna lectura de tablas ajenas (ADR-0019).
- **Ajustes.** `VoteSettingsRepository.actuales()` devuelve
  `VoteSettings(boolean unVotoAlDia, int maxJugadoresPorIp, Duration ventana)`.
  Si la lectura falla, cae en `VoteSettings.porDefecto()` = `(false, 50, 24h)`,
  justo en la rama que cambia. Con este cambio ese caso pasa a comprobar la
  ventana por servidor, igual que hace `votar()`.
- **Pruebas existentes.**
  - `backend/voting/src/test/java/com/latinosmc/voting/CastVoteServiceTest.java`
    usa Mockito `LENIENT`, el reloj fijo `2026-08-03T12:00:00Z` y el
    `IdentityHasher` real. Su `@BeforeEach` deja por defecto
    `VoteSettings(false, 50, 24h)` y los dos `ultimoVoto…` vacíos.
  - **No hay ninguna prueba de `puedeVotar`**, así que ninguna prueba se rompe.
  - **No existe `PluginControllerTest`.**
- **Frontend.** No consume `POST /api/v1/plugin/vote-tokens`: usa
  `/vote-tokens/{token}` (`frontend/src/lib/api.ts:77`). No le afecta.
- **ADR que aplican.**
  - [ADR-0011](../../docs/decisiones/0011-ventana-de-voto.md): ventana
    deslizante y semiabierta, se puede votar cuando `último + ventana <= ahora`.
  - [ADR-0023](../../docs/decisiones/0023-la-ventana-de-voto-es-de-la-plataforma.md):
    la ventana es global y la fija la plataforma. Es el fundamento de la spec.
  - [ADR-0016](../../docs/decisiones/0016-token-de-voto.md): flujo de token. Su
    estado es «Propuesto» aunque ya está implementado, lo que es higiene ajena a
    esta spec.
  - ADR-0009: el plugin no es fuente de verdad; `votar()` sigue mandando.
  - [ADR-0032](../../docs/decisiones/0032-cobertura-con-trinquete-y-mutacion.md):
    suelo de cobertura de `voting` en `0.36` (`backend/voting/pom.xml:23`) y
    mutación obligatoria en `voting`.
  - Ninguno contradice la spec.
- **Paradas.** Contrato `/api/v1/plugin/*`: **sí, en su comportamiento, no en
  su forma**. Migración, ledger y dependencia: no.

## Decisiones técnicas

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Rama `else` de `puedeVotar`: `repositorio.ultimoVotoAceptado(servidorId, identidadHash)` + `ultimo.plus(reglas.ventana())` + `ahora.isBefore(disponibleEn)` | Una consulta o un método de dominio nuevo | Es exactamente la comprobación del paso 3 de `votar()`. Copiar su forma garantiza el mismo borde (CA-2) y no amplía el repositorio. |
| Comparar con `isBefore` (semiabierta) | `!isAfter` | ADR-0011 y `votar()`. Con `!isAfter`, en el instante exacto del fin `puedeVotar` diría «no» a quien `votar()` acepta, que es el error que la propia documentación de `puedeVotar` prohíbe («nunca al revés»). |
| Con la regla global activada no se toca nada | Comprobar también la ventana por servidor y devolver `YA_VOTO_AQUI` si el último voto fue aquí | Fuera de alcance según la spec: con la regla activada, la ventana global contiene a la del servidor. Queda como pregunta P-3. |
| No extraer un método común entre `votar()` y `puedeVotar` | Refactorizar las dos comprobaciones en un ayudante privado | Así el diff de un módulo 🔴 queda mínimo y `votar()` no cambia (fuera de alcance). El `dev-backend` puede proponerlo en la fase de refactor solo si `votar()` queda sin cambio de comportamiento; por defecto, no. |
| CA-8 con una **prueba unitaria nueva** `PluginControllerTest`, Mockito puro, sin contexto de Spring, que llama a `emitirToken` con un `CastVoteService` simulado | Darlo por cumplido por inspección, porque el controlador no cambia | Un criterio sin prueba deja el plan incompleto, y CA-8 es el contrato con el plugin. La prueba fija la forma de la respuesta (`token`/`url` nulos, `motivo`, `segundosRestantes`) y que no se llama a `VoteTokenService.emitir`. Pasará en verde desde el principio, porque protege lo que no debe cambiar. Ver P-2. |
| `segundosRestantes` sigue saliendo de `Duration.toSeconds()` (trunca) | Redondear hacia arriba en el controlador | Cambiarlo afecta también a `YA_VOTO_HOY` y al contrato. Queda fuera de esta spec y es la pregunta P-1. |
| Sustituir el comentario de las líneas 92-96 y ajustar el Javadoc de `puedeVotar` citando ADR-0023 | Dejar el comentario | CA-9: el comentario actual justifica un comportamiento que deja de existir. |

## Archivos que se tocan

| Archivo | Cambio | Agente |
|---|---|---|
| `backend/voting/src/test/java/com/latinosmc/voting/CastVoteServiceTest.java` | modifica: pruebas nuevas de `puedeVotar` (CA-1 a CA-7) | `pruebas-backend` |
| `backend/app/src/test/java/com/latinosmc/app/web/PluginControllerTest.java` | nuevo: CA-8 | `pruebas-backend` |
| `backend/voting/src/main/java/com/latinosmc/voting/CastVoteService.java` | modifica: solo `puedeVotar`, su Javadoc y su comentario | `dev-backend` |
| `backend/voting/pom.xml` | modifica `cobertura.suelo` **si** la cobertura medida sube (ADR-0032) | **ningún rol puede**; ver P-6 |
| `specs/003-ya-voto-aqui/*` | spec, plan, tareas, pr | `orquestador` |

No se tocan: `PluginController.java`, `VoteRepository.java`,
`VoteSettingsRepository.java`, `VoteSettings.java`, migraciones ni `frontend/`.

## Matriz criterio → prueba

Nombres definitivos, tras las dos vueltas del `revisor`. Todas las pruebas de
`CastVoteServiceTest` están en
`backend/voting/src/test/java/com/latinosmc/voting/`.

| Criterio | Prueba | Tipo |
|---|---|---|
| CA-1 | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_votoAquiHaceSeisHoras_respondeYaVotoAquiConDieciochoHoras` (ventana de 24 h) y `#puedeVotar_sinReglaGlobal_ventanaDeDoceHoras_votoHaceSeisHoras_respondeSeisHoras` (ventana de 12 h: protege que la ventana sale de `reglas.ventana()`, ADR-0023) | unitaria |
| CA-2 | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_finDeVentanaJustoAhora_puede` y `#puedeVotar_sinReglaGlobal_unSegundoAntesDelFin_respondeYaVotoAquiConUnSegundo` | unitaria (borde) |
| CA-3 | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_votoAquiFueraDeVentana_puede` (25 h con 24 h) y `#puedeVotar_sinReglaGlobal_ventanaDeDoceHoras_votoHaceTrece_puede` (13 h con 12 h) | unitaria |
| CA-4 | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_votoRecienteEnOtroServidor_puede` (la consulta global devuelve un voto de hace 1 h y la de este servidor, vacío) | unitaria |
| CA-5 | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_consultaSoloElServidorYLaIdentidadCorrectos` (hash exacto de la identidad y `never()` sobre la consulta global) y `#puedeVotar_sinReglaGlobal_usaElServidorIdRecibido` (con un voto solo en el servidor B: A puede votar y B recibe `YA_VOTO_AQUI`) | unitaria |
| CA-6 | `CastVoteServiceTest#puedeVotar_conReglaGlobal_votoHoyEnCualquiera_respondeYaVotoHoy` y `#puedeVotar_conReglaGlobal_sinVotoHoy_puede` | unitaria (regresión) |
| CA-7 | `verificarQueNoHuboEfectos()` al final de las 11 pruebas de `puedeVotar`: `verifyNoInteractions(publicador)` y `never()` sobre `insertar`, `crearRecompensaPendiente` y `guardarSenales` | unitaria |
| CA-8 | `PluginControllerTest#emitirToken_yaVotoAqui_devuelveMotivoYSegundosSinCodigo`, en `backend/app/src/test/java/com/latinosmc/app/web/`. `puedeVotar` simulado para el servidor y la identidad exactos (`PlayerIdentity.mojangUuid(...)`) devuelve `(false, "YA_VOTO_AQUI", 18 h)`. Se espera `puedeVotar=false`, `motivo="YA_VOTO_AQUI"`, `segundosRestantes=64800`, `token` y `url` nulos, `vigenciaSegundos=0` y que nunca se llama a `emitir` | unitaria (contrato) |
| CA-9 | Revisión del diff por el `revisor` (`git diff` contra la base: no cambian `RespuestaToken`, `PeticionToken`, rutas, migraciones ni `pom.xml` de dependencias) + `mvn -B -f backend/pom.xml verify` en verde, con `ArquitecturaTest` | revisión + suite completa |

**Mutación (ADR-0032).** `mvn -B -Pmutacion -pl voting -am verify` desde
`backend/`. En `tareas.md` se clasifican los mutantes supervivientes **de
`puedeVotar`**; los de `votar()` se listan sin clasificar (P-5). Los esperados
en el borde son `isBefore`, `plus(ventana)` y `Duration.between`: las pruebas
de CA-1 y CA-2 tienen que matarlos.

## Riesgos y cómo se comprueban

| Riesgo | Qué lo detecta |
|---|---|
| `puedeVotar` dice «no» a quien `votar()` aceptaría, el error que la documentación prohíbe (borde de la ventana, servidor equivocado, votos rechazados contados) | CA-2 con los dos lados del borde y CA-5 con el servidor equivocado. Los votos rechazados los excluye el SQL de `ultimoVotoAceptado`, que no tiene prueba de repositorio (deuda de ADR-0021); es la misma consulta que ya usa `votar()` en producción. |
| El plugin desplegado no trata bien `YA_VOTO_AQUI` (no se puede verificar desde este repo) | Nada en este repositorio. Depende de la confirmación del desarrollador (P-4) y del aviso en la primera línea del PR. |
| Se rompe la rama de la regla global | CA-6 (hoy sin pruebas; esta spec las añade). |
| Con la regla desactivada, una consulta más por cada `/vote` | Coste aceptable: es la consulta indexada que ya usa `votar()` con `LIMIT 1`, y `emitirToken` tiene un tope por minuto. No se mide. |
| `segundosRestantes = 0` con `puedeVotar = false` si faltan menos de 1 s (truncado) | Ya pasa hoy con `YA_VOTO_HOY`. Se deja anotado (P-1). |
| La cobertura sube y el suelo no se actualiza, contra ADR-0032 | El `revisor` lee el informe de JaCoCo. Para subir el suelo hace falta decidir quién edita `pom.xml` (P-6). |

## Preguntas para el desarrollador

- **P-1.** `segundosRestantes` se trunca (`toSeconds()`). ¿Se deja así, igual
  que hoy con `YA_VOTO_HOY`, o se abre otra spec para redondear hacia arriba?
  Propuesta: dejarlo y abrir otra spec si el plugin llega a mostrar «0 s».
- **P-2.** CA-8: ¿te vale una `PluginControllerTest` nueva con Mockito puro?
  Pasará en verde desde el principio, porque fija un contrato que no cambia.
  ¿O prefieres cubrirlo solo con la revisión?
- **P-3.** Con la regla global activada sigue saliendo `YA_VOTO_HOY` aunque el
  voto fuera a este mismo servidor. ¿Se deja así o se abre una spec aparte?
- **P-4.** ¿Confirmas que el `ComandoVotar` **desplegado** maneja
  `YA_VOTO_AQUI` con `segundosRestantes`, sin necesitar una release nueva?
- **P-5.** Mutación: ¿se clasifican solo los supervivientes de `puedeVotar` o
  todos los de `CastVoteService`?
- **P-6.** Si la cobertura de `voting` sube, ADR-0032 pide subir
  `cobertura.suelo` en este PR. Ningún rol puede editar `backend/voting/pom.xml`
  (el hook solo deja a `dev-backend` en `src/main` y a `pruebas-backend` en
  `src/test`). ¿Lo editas tú, se amplía la política en otra rama o se deja para
  otro PR?
- **P-7.** ADR-0016 figura como «Propuesto» pese a estar implementado.
  Propuesta: no tocarlo aquí y llevarlo a una rama `docs/` aparte.

## Respuestas del desarrollador (17/11/2026)

- **Parada del contrato:** aprobada; va en la primera línea del PR.
- **P-1:** se deja truncado; anotado en la spec como posible mejora fuera de
  alcance.
- **P-2:** sí a `PluginControllerTest` para CA-8.
- **P-3:** se deja como está, fuera de alcance.
- **P-4:** comprobado por el desarrollador: la release `v0.2.0` del plugin ya
  maneja `YA_VOTO_AQUI` (`ComandoVotar.avisarQueYaVoto`, clave `ya-voto-aqui`,
  lee `segundosRestantes`). No hace falta una release nueva.
- **P-5:** se clasifican solo los supervivientes de `puedeVotar`.
- **P-6:** **ningún agente edita `pom.xml`.** Se anota el valor medido en
  `tareas.md` y `pr.md`; el suelo lo sube la sesión que lanzó al orquestador,
  en un commit propio, fuera del sistema de agentes.
- **P-7:** otra rama `docs/`.

Con esto, la fila de `backend/voting/pom.xml` de «Archivos que se tocan» no la
hace ningún agente de esta rama.
