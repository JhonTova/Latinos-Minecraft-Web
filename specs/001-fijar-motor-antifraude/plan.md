# Plan 001 — Fijar el comportamiento del motor antifraude

Spec: [spec.md](spec.md)

## Mapa del cambio

Informe del `analista` (13/11/2026), resumido:

- **Módulo:** solo `trust` (🔴). Clase bajo prueba:
  `backend/trust/src/main/java/com/latinosmc/trust/RuleBasedTrustEngine.java`,
  que **no se modifica** (CA-12). La llama `CastVoteService` (`voting`) antes de
  insertar el voto, así que las tres consultas de `TrustQueries` solo ven votos
  previos.
- **Contrato:** no toca `/api/v1/plugin/*` ni el frontend. Sin migración, sin
  ledger, sin dependencia nueva: `junit-jupiter-params` ya llega por
  `spring-boot-starter-test` (hay `@ParameterizedTest` en `voting`, `servers` y
  `shared`), y PIT 1.30.2 con `pitest-junit5-plugin` ya está configurado en el
  perfil `mutacion` de `backend/pom.xml` (líneas 186-213 y 313-332).
- **PIT:** grupo de mutadores DEFAULTS (sin `<mutators>`), `targetClasses`
  `com.latinosmc.trust.*`, sin umbral, informe en
  `backend/trust/target/pit-reports/` (ignorado por git: el «rojo» se conserva
  resumido en `tareas.md`).
- **Línea base real** (`mutations.xml`): 21 mutantes en
  `RuleBasedTrustEngine`, 11 muertos y 10 vivos, en las líneas 62 (límite), 63
  (resta→suma), 70 (límite), 71 (resta→suma y multiplicación→división), 72
  (suma→resta), 82 (incremento), 84 (límite y negación) y 91 (límite). Los 8
  `NO_COVERAGE` del módulo son todos de `TrustQueries`.
- **Cobertura:** `RuleBasedTrustEngine` ya está al 100 % (33/33 líneas) y
  `TrustQueries` al 0 % (0/24). Con pruebas solo del motor la cobertura de
  `trust` **no puede subir** de 57,9 %, así que `cobertura.suelo`
  (`backend/trust/pom.xml:23`, `0.57`) se queda igual: CA-11 se cumple por su
  rama «nunca baja».
- **Firmas que usan las pruebas** (`backend/shared/.../ports/TrustEngine.java`):
  `Peticion(UUID servidorId, PlayerIdentity identidad, byte[] identidadHash, String ip, Instant momento)`,
  `Veredicto(boolean aceptado, int puntuacionRiesgo, String motivoRechazo, List<Senal> senales)`
  (`motivoRechazo` es `null` si se acepta), `Senal(String regla, int delta, String detalle)`.

ADR que aplican:

- [ADR-0005](../../docs/decisiones/0005-antifraude-core.md): reglas explicables;
  todo veredicto lleva sus señales (CA-6, CA-8).
- [ADR-0032](../../docs/decisiones/0032-cobertura-con-trinquete-y-mutacion.md):
  el suelo solo sube; los supervivientes de las clases tocadas se clasifican en
  `tareas.md`.
- [ADR-0021](../../docs/decisiones/0021-reintento-de-eventos-de-pago.md),
  sección «Lo que estos tests NO cubren»: por qué `TrustQueries` queda fuera.
- [ADR-0015](../../docs/decisiones/0015-antifraude-lee-tabla-votos.md): las
  pruebas no deben leer `votes` por otra vía (lo vigila `ArquitecturaTest`).

## Decisiones técnicas

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Clase nueva `RuleBasedTrustEngineLimitesTest` en el mismo paquete | Reescribir `RuleBasedTrustEngineTest` | Las 7 pruebas actuales documentan la intención de cada regla (el hogar compartido, la granja); las nuevas fijan cifras. Separarlas deja el diff revisable y no borra nada que hoy pasa. |
| `@ParameterizedTest` con `@CsvSource` para los bordes de CA-3, CA-4 y CA-5 | Un método por valor | Cada fila es un borde con nombre en el informe; ya se usa en el proyecto y no es dependencia nueva. |
| Aserción sobre la lista completa de señales con `extracting(Senal::regla, Senal::delta).containsExactly(tuple(...))` | `anyMatch` sobre la regla, como hoy | `anyMatch` es justo lo que deja vivos los mutantes: no mira la cifra ni detecta señales de más. |
| Detalle comprobado con `startsWith(N + " ")` | `contains(String.valueOf(N))` (lo sugería el analista) o el texto exacto | `contains("4")` pasaría siempre, porque el texto de la regla ya dice «24 h»; el texto exacto acopla la prueba a la redacción. |
| CA-5 y la aritmética se prueban sobre identidad offline (parte de 25) | Sobre la petición base (parte de 0) | Con base 0, el −15 de historial se recorta a 0 y el riesgo no distingue −15 de −10 ni de 0; partiendo de 25 la aportación se ve en el puntaje (25 − 15 = 10; 25 + 10 = 35). |
| CA-9 con *stubs* de argumentos exactos (`eq(hash)`, `eq(AHORA.minus(...))`) que devuelven un valor que dispara la regla, y aserción sobre el veredicto; los demás argumentos devuelven el valor base | `verify(...)` como única comprobación | `backend-pruebas.md` desaconseja el `verify` solo cuando hay un resultado que mirar. Si el motor consultara con otra ventana u otro hash, el *stub* no casaría y la señal no aparecería. El `identidadHash` de la petición será un `byte[]` distinto del que calcula `hashIdentidad`, para que no se confundan. |
| El tiempo es la constante `AHORA` de la prueba actual | `Clock` | El motor no lee la hora: recibe `momento` en la `Peticion`; no hay reloj que fijar. |
| No hay `feat(...)` ni `dev-backend` | Seguir el flujo completo | No se escribe código de producción (CA-12). El «rojo» es el informe PIT con 10 supervivientes antes de las pruebas; el «verde», el mismo informe sin supervivientes en `RuleBasedTrustEngine`. Los commits serán `docs(specs)` y `test(trust)`. |
| `cobertura.suelo` de `trust` se queda en `0.57` | Subirlo | No puede subir (ver mapa); bajarlo está prohibido por ADR-0032. |

## Archivos que se tocan

| Archivo | Cambio | Agente |
|---|---|---|
| `backend/trust/src/test/java/com/latinosmc/trust/RuleBasedTrustEngineLimitesTest.java` | nuevo | `pruebas-backend` |
| `backend/trust/pom.xml` | sin cambio (suelo `0.57`; solo se tocaría si la cobertura subiera) | — |
| `specs/001-fijar-motor-antifraude/{spec,plan,tareas,pr}.md` | nuevo | `orquestador` |

Nada bajo `backend/*/src/main/`.

## Matriz criterio → prueba

Todas en `RuleBasedTrustEngineLimitesTest` salvo las tres últimas filas.

| Criterio | Prueba | Tipo | Mutantes vivos que mata |
|---|---|---|---|
| CA-1 | `#laPeticionBaseSeAceptaConRiesgoCeroYSinSenales` | unitaria | (refuerza los de retorno, ya muertos) |
| CA-2 | `#unaIdentidadNoVerificableSumaExactamente25` y `#unaIdentidadVerificableNoSumaPorIdentidad` | unitaria | — (ya muertos; fija la cifra) |
| CA-3 | `#lasIdentidadesPorIpAportanSuCifraExacta` (N = 3 → sin señal; 4 → 15; 5 → 30; 6 → 45; 7 → 45; 30 → 45) | unitaria parametrizada | L62 límite, L63 resta→suma |
| CA-4 | `#losVotosEnUnaHoraAportanSuCifraExacta` (V = 6 → sin señal; 7 → 10; 10 → 40; 11 → 40; 50 → 40) | unitaria parametrizada | L70 límite, L71 resta→suma, L71 mult→div, L72 suma→resta |
| CA-5 | `#elHistorialAportaSuCifraExacta` (H = 0 → `IDENTITY_NEW` +10, riesgo 35; 1 y 9 → sin señal, riesgo 25; 10 y 50 → `IDENTITY_TRUSTED` −15, riesgo 10) | unitaria parametrizada | L82 incremento, L84 límite, L84 negación |
| CA-6 | `#elRiesgoEsLaSumaExactaDeLasAportaciones` (offline + 4 identidades + 7 votos + H = 0 = 60; el riesgo es igual a la suma de los `delta`) | unitaria | L72 (suma→resta), también cubierto por CA-4 |
| CA-7 | `#unaSumaNegativaSeRecortaACero` y `#unaSumaMayorDeCienSeRecortaACien` | unitaria | (fija `Math.clamp`, que DEFAULTS no muta) |
| CA-8 | `#unRiesgoDe55SeAceptaSinMotivo` y `#unRiesgoDeExactamente60SeRechazaConSusSenales` | unitaria | L91 límite |
| CA-9 | `#consultaConLasVentanasYLosHashesDeLaPeticion` (tres casos: cada consulta solo dispara su regla si recibe sus argumentos exactos) | unitaria | — (DEFAULTS no muta esos argumentos; es garantía de comportamiento) |
| CA-10 | `mvn -B -Pmutacion -pl trust -am verify` antes y después; `RuleBasedTrustEngine` sin `SURVIVED` en `mutations.xml` | mutación (observación) | todos |
| CA-11 | `mvn -B verify` desde `backend/`; JaCoCo de `trust` ≥ 57 % y suelo sin cambios | build (observación) | — |
| CA-12 | `git diff --stat 8557294...HEAD -- backend/*/src/main` vacío | observación | — |

Para CA-12 la base es `8557294` (punta de `fix/permisos-en-settings`, de donde
sale esta rama), no `main`, porque el arnés aún no está en `main`.

## Riesgos y cómo se comprueban

- **Que las pruebas fijen el valor equivocado.** Se escriben contra el código
  actual: si una prueba nueva falla sin tocar `src/main`, la cifra de la spec
  está mal o el motor hace otra cosa; se para y se pregunta, no se «arregla» la
  prueba ni el motor. Lo detecta `mvn -B verify`.
- **Pruebas que pasan pero no matan.** Lo detecta CA-10 (PIT) y el `revisor`,
  que vuelve a ejecutar la mutación.
- **Despliegue.** Solo se agregan pruebas: lo que llega a `main` no cambia el
  comportamiento en producción. CA-12 lo comprueba.
- **Mutadores más fuertes en el futuro** (`STRONGER`/`ALL`) generarían mutantes
  sobre `Math.min`, `Math.clamp` y las constantes; las pruebas de topes de CA-3,
  CA-4 y CA-7 ya los cubren. Se anota en `tareas.md` que se midió con DEFAULTS.

## Preguntas que salieron del mapa (no bloquean el plan)

P-5 a P-8 (IP nula, qué cuentan N y V, las dos reglas de IP, «configurable en
caliente») están en la spec, en «Preguntas abiertas», junto a P-1–P-4. Ninguna
cambia el motor en esta spec; las pruebas fijan el comportamiento actual.
