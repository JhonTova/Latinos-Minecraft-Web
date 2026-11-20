No toca `/api/v1/plugin/*`, ni migraciones, ni el ledger, ni dependencias. Módulo 🔴 `voting`.

## Qué cambia y por qué

`POST /api/v1/servers/{slug}/votes` acepta la cabecera `Idempotency-Key`. Si
el navegador de un jugador con mala conexión repite un voto que ya fue
aceptado, la respuesta llegaba con `segundosHastaProximoVoto: null` y el
jugador no sabía cuándo podía volver a votar.

Ahora `CastVoteService` hace esto con los reintentos:

- Si el voto guardado es `ACCEPTED`, el reintento devuelve
  `min(V, max(0, V − (ahora − creadoEn)))`.
  - `V` es la ventana vigente (`ajustes.actuales().ventana()`, ADR-0023).
  - `ahora` sale del `Clock` inyectado.
  - Por la ventana semiabierta (ADR-0011), en `creadoEn + V` exacto ya da 0, y
    nunca da un valor negativo.
  - El tope en `V` evita que un desfase de reloj entre instancias anuncie más
    espera que la ventana completa.
- Si el voto es `REJECTED`, `PENDING` o `REVOKED`, sigue devolviendo `null` y
  no se leen los ajustes.
- La misma fórmula se aplica cuando se pierde la carrera de inserción.
- Un reintento sigue sin evaluar nada: no consulta el directorio, las
  ventanas, el cupo de IP ni el antifraude. Tampoco escribe nada: ni voto, ni
  señales, ni recompensa, ni evento.

El JSON mantiene su forma, con `segundosHastaProximoVoto` como `Long` que puede
ser `null`. El frontend no lee ese campo.

- Spec: https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/005-reintento-voto-espera/spec.md
- Plan: https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/005-reintento-voto-espera/plan.md
- Tareas y evidencia: https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/005-reintento-voto-espera/tareas.md

## Riesgo conocido (fuera de alcance, decisión P-1)

**El reintento por `/api/v1/vote-tokens/{token}/vote` con el token ya consumido
sigue devolviendo `null`.** `VoteTokenController.votar` comprueba primero
`tokens.votoYaEmitido(token)` y, si el voto existe, responde `200` con
`segundosHastaProximoVoto = null` fijo, sin pasar por `CastVoteService.votar`.
El código está en `backend/app/src/main/java/com/latinosmc/app/web/VoteTokenController.java:69-74`.
Queda para una spec posterior.

Otras decisiones aprobadas:

- Se usa la ventana vigente en el reintento, aunque haya cambiado después del
  voto (P-2). No hace falta migración.
- El resultado tiene un tope en `V` (P-3).
- Los votos que no son `ACCEPTED` devuelven `null` (P-4).

## Matriz criterio → prueba

Todas las pruebas están en `CastVoteServiceTest › Reintento idempotente`, salvo
las de `VoteControllerTest`. Las de servicio usan V = 24 h, salvo donde se
indica, y A = 2026-08-03T12:00Z con `Clock.fixed`.

| Criterio | Prueba | Esperado |
|---|---|---|
| CA-1 | `aceptadoHaceSeisHorasDevuelveDieciocho`, `reintentoUsaLaVentanaVigente` (V = 48 h); `VoteControllerTest › reintentoAceptadoDevuelve200YSegundos` | 18 h, 42 h; `200` con `64800` |
| CA-2 | `aceptadoEnElMismoInstanteDevuelveLaVentana` | 24 h |
| CA-3 | `unSegundoAntesDeCerrarDevuelveUnSegundo`, `justoAlCerrarLaVentanaDevuelveCero` | 1 s, 0 |
| CA-4 | `unSegundoDespuesDeCerrarDevuelveCero`, `tresVentanasDespuesDevuelveCero` | 0, 0 |
| CA-5 | `noAceptadoDevuelveNull` (REJECTED, PENDING, REVOKED); `VoteControllerTest › reintentoRechazadoDevuelveNull` | `null`, sin leer ajustes |
| CA-6 | `reintentoNoEvaluaNiEscribeNada` (ACCEPTED, REJECTED) | `verifyNoInteractions(directorio, antifraude, publicador)` + `verifyNoMoreInteractions(repositorio)` |
| CA-7 | `carreraConAceptadoAplicaLaFormula`, `carreraUsaLaVentanaVigente`, `carreraConRechazadoDevuelveNull` | 18 h, 42 h, `null`; sin recompensa ni evento |
| CA-8 | `votoNuevoAceptadoSigueDevolviendoLaVentana`, `votoNuevoRechazadoSigueDevolviendoNull`, `VoteControllerTest › votoNuevoDevuelve201YLaVentana` y las pruebas de `409` que ya existían | 24 h; `null`; `201` con `86400` |
| CA-9 | `creadoEnElFuturoNoSuperaLaVentana` | 24 h, no 25 h |
| CA-10 | `git diff --stat main...HEAD`: 6 archivos (`CastVoteService.java`, dos clases de prueba y tres documentos de la spec) | Sin cambios en plugin, migraciones, ledger, `frontend/` ni `pom.xml` |

## Evidencia

- **Rojo** (commit `e2ddc9d`), con `cd backend && mvn -B -pl voting -am test`:
  - `voting`: Tests run: 63, Failures: 8, Errors: 0.
  - Los 8 fallos son de aserción, del tipo `expected: 18H but was: null`.
  - Las pruebas de CA-5, CA-6, CA-7 (rechazado) y CA-8 ya pasaban: son de
    regresión.
- **Verde**, con `mvn -B -f backend/pom.xml verify`: BUILD SUCCESS.
  - `voting`: Tests run: 66, Failures: 0, Errors: 0, Skipped: 0.
  - `app`: Tests run: 118, Failures: 0, Errors: 0, Skipped: 0, incluidos
    `VoteControllerTest` (3) y `ArquitecturaTest` (4).
- **Cobertura de líneas de `voting`** (JaCoCo, BUNDLE): **147/346 = 42,49 %**.
  El suelo actual es 0.40. La cobertura de `main` no se midió.
  `cobertura.suelo` se sube en un commit aparte de esta rama.
- **Mutación**, con `cd backend && mvn -B -Pmutacion -pl voting -am verify`:
  - En `voting`: 114 mutantes, 68 muertos, 2 supervivientes y 44 sin
    cobertura. Fuerza: 97 %.
  - En `CastVoteService`: 40 mutantes, 38 muertos, 1 superviviente y 1 sin
    cobertura.
  - El único superviviente del código nuevo (`restante`, `> 0` → `>= 0`) es
    equivalente.
  - Los demás supervivientes y sin cobertura ya existían antes de esta rama.
    Están clasificados en `tareas.md`.
- **Revisión:** dos vueltas del `revisor`.
  - En la primera hubo un hallazgo medio: la ventana vigente no estaba
    probada con un valor distinto de 24 h. Se corrigió en `a1ad07e`.
  - En la segunda no hubo hallazgos nuevos.
- No hace falta verificación visual, porque no se toca `frontend/`.

## Commits

1. `docs(specs)`: spec, plan y tareas.
2. `test(voting)`: pruebas en rojo.
3. `feat(voting)`: cálculo de la espera en el reintento.
4. `test(voting)`: pruebas de los hallazgos del revisor (ventana de 48 h,
   `verifyNoMoreInteractions` y CA-8).
5. `docs(specs)`: cierre con evidencia y descripción del pull request.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
