# Spec 005 — El reintento de un voto aceptado dice cuánto falta para el próximo

- **Estado:** Aprobada el 19/11/2026, con las respuestas P-1 a P-6 de [plan.md](plan.md)
- **Rama:** `feat/005-reintento-voto-espera`
- **Parte:** backend
- **Riesgo:** 🔴 crítico (`voting`: ventana e idempotencia, ver `backend/AGENTS.md`)

## Problema

`POST /api/v1/servers/{slug}/votes` acepta la cabecera `Idempotency-Key`. Si la
clave ya se procesó, `CastVoteService.votar` devuelve el voto guardado sin volver
a evaluar nada, con `Resultado.repetido(voto)`. Ese método deja
`proximoVotoEn = null`, así que la respuesta del reintento trae
`segundosHastaProximoVoto: null` aunque el voto original fue aceptado.

Caso concreto: Marta vota desde el móvil con mala cobertura. El backend acepta
su voto (`201`, `segundosHastaProximoVoto: 86400`), pero la respuesta no le
llega. El navegador reintenta con la misma `Idempotency-Key` y recibe `200`,
`estado: "ACCEPTED"`, `yaExistia: true` y `segundosHastaProximoVoto: null`.
Marta sabe que su voto contó, pero no cuándo puede volver a votar.

El mismo `Resultado.repetido` se usa en otros dos caminos que también devuelven
el voto ya guardado:

- la carrera de inserción, cuando otra petición con la misma clave insertó
  primero (`CastVoteService.java`, paso 7);
- `POST /api/v1/vote-tokens/{token}/vote`, que llama a `votar` con la clave
  `token:<token>` (`VoteTokenController.java:96`). Solo llega ahí con una clave
  repetida si el token todavía no se marcó como consumido. El reintento normal
  por token (token ya consumido) responde antes, en
  `VoteTokenController.java:69-74`, con `null` fijo y sin pasar por `votar`:
  esta spec **no** cambia ese camino (ver «Fuera de alcance»).

## Qué se construye

Cuando la clave de idempotencia ya corresponde a un voto **aceptado**, la
respuesta dice cuánto falta de verdad para poder volver a votar: la ventana menos
el tiempo que ya pasó desde que se creó el voto, sin bajar de cero. Un voto
rechazado repetido sigue devolviendo `null`. Nada más cambia.

## Criterios de aceptación

Notación: `V` es la ventana vigente (`ajustes.actuales().ventana()`), `C` es el
`creadoEn` del voto guardado y `A` es el instante del `Clock` inyectado en el
momento del reintento. `restante = min(V, max(0, V − (A − C)))`. La ventana es
semiabierta (ADR-0011): en `A = C + V` ya se puede votar, así que `restante = 0`.
El tope superior `V` solo actúa si `C` es posterior a `A` (desfase de reloj
entre instancias); con relojes sanos la fórmula es `max(0, V − (A − C))`.
Solo un voto con estado `ACCEPTED` usa la fórmula.

| ID | Dado / cuando / entonces |
|---|---|
| CA-1 | Dado un voto aceptado con la clave `K`, creado en `C`, y una ventana `V` de 24 h, cuando llega otro `votar` con `K` en `A = C + 6 h`, entonces el resultado trae el mismo voto, `yaExistia = true` y `proximoVotoEn = 18 h` exactas. Por HTTP: `200` con `segundosHastaProximoVoto = 64800`. |
| CA-2 | Dado el voto de CA-1, cuando el reintento llega en `A = C` (mismo instante), entonces `proximoVotoEn = V` (24 h, `86400` s), igual que la respuesta original. |
| CA-3 | **Borde, fin de la ventana.** Dado el voto de CA-1, cuando el reintento llega en `A = C + V − 1 s`, entonces `proximoVotoEn = 1 s`; en `A = C + V` exacto, `proximoVotoEn = 0`. |
| CA-4 | **Borde, ventana ya pasada.** Dado el voto de CA-1, cuando el reintento llega en `A = C + V + 1 s` o en `A = C + 3 V`, entonces `proximoVotoEn = 0`, nunca negativo. |
| CA-5 | Dado un voto con la clave `K` cuyo estado **no** es `ACCEPTED` (`REJECTED`, y también `PENDING` o `REVOKED`), cuando llega otro `votar` con `K` (en cualquier `A`), entonces `yaExistia = true` y `proximoVotoEn = null`, y no se leen los ajustes de voto. Por HTTP: `segundosHastaProximoVoto: null`. |
| CA-6 | **Nada más cambia.** En los reintentos de CA-1 a CA-5 no se consulta el directorio de servidores, ni la ventana por servidor, ni la global, ni el cupo de IP, ni el antifraude; no se inserta ningún voto, no se guardan señales, no se crea recompensa y no se publica ningún evento. |
| CA-7 | **Carrera de inserción.** Dado que `insertar` devuelve `false` porque otra petición con la misma clave ganó, y el voto que se recupera es aceptado, entonces el resultado aplica la misma fórmula (`yaExistia = true`, `proximoVotoEn = min(V, max(0, V − (A − C)))`); si no es aceptado, `null`. No se guardan señales, no se crea recompensa ni se publica evento. |
| CA-8 | Los caminos que no son reintento no cambian: un voto nuevo aceptado sigue devolviendo `proximoVotoEn = V` con `201`, uno nuevo rechazado `null` con `201`, y las respuestas `409` de ventana abierta, voto diario y cupo de IP siguen igual. |
| CA-9 | **Reloj desfasado.** Dado un voto aceptado con `C = A + 1 h` (creado «en el futuro»), cuando llega el reintento, entonces `proximoVotoEn = V` (24 h), nunca más que la ventana. |
| CA-10 | **Global.** El diff no cambia `/api/v1/plugin/*`, ninguna migración, el ledger ni `frontend/`, y ningún `pom.xml` salvo, si la cobertura de `voting` sube, el `cobertura.suelo` de `backend/voting/pom.xml` (ADR-0032), que se sube en un commit aparte de la sesión que abre el PR. `mvn -B verify` pasa. |

## Fuera de alcance

- Recordar la ventana con la que se aceptó el voto. Si la plataforma cambia
  la ventana entre el voto y el reintento, se usa la vigente (decisión P-2).
- Decir cuánto falta cuando el voto repetido fue rechazado.
- **Riesgo conocido: el reintento por token ya consumido sigue devolviendo
  `null`** (decisión P-1). `POST /api/v1/vote-tokens/{token}/vote` comprueba
  primero `tokens.votoYaEmitido(token)` y, si el voto existe, responde `200`
  con `segundosHastaProximoVoto = null` fijo sin pasar por `votar`
  (`backend/app/src/main/java/com/latinosmc/app/web/VoteTokenController.java:69-74`).
  Es el caso más común de reintento en esa ruta. Arreglarlo toca `app` y
  `votoYaEmitido`, y queda para una spec posterior.
- El redondeo a segundos del controlador: `Duration.toSeconds()` trunca, igual
  que hoy en la respuesta `201` y en las de ventana abierta.
- En modo «un voto al día», el restante describe la ventana **de ese voto**, no
  la del jugador. Si ya es `0` y el jugador votó después a otro servidor, el
  reintento dice `0`; la comprobación real sigue en `votar`.
- Cambios en el frontend o en el texto que se muestra al jugador.
- Cambiar el código de estado del reintento (`200`) o los nombres del JSON.
- Pruebas contra PostgreSQL real (deuda de ADR-0021).

## Paradas

- [ ] Cambia el contrato `/api/v1/plugin/*`
- [ ] Necesita una migración Flyway
- [ ] Escribe en el ledger
- [ ] Agrega una dependencia

Ninguna aplica, según el mapa del `analista`: `votes.created_at` ya existe
(`V4__votos.sql:63`, `TIMESTAMPTZ NOT NULL`), `PluginController` solo usa
`puedeVotar` y no `votar`, no se toca `billing` y no hace falta ninguna
dependencia.

## Decisiones del desarrollador

Respondidas el 19/11/2026 (preguntas P-1 a P-6 de [plan.md](plan.md)):

- **P-1.** El reintento por token ya consumido queda fuera de alcance, como
  riesgo conocido (ver «Fuera de alcance»).
- **P-2.** El reintento usa la ventana **vigente** en el momento del
  reintento (`ajustes.actuales().ventana()`), aunque el administrador la haya
  cambiado después del voto. No se guarda la ventana del voto y no hay
  migración.
- **P-3.** Se aprueba el tope superior `V` (CA-9).
- **P-4.** Los votos `REJECTED`, `PENDING` y `REVOKED` repetidos devuelven
  `null` (CA-5).
- **P-5.** Se añade `VoteControllerTest` en `app/src/test`.
- **P-6.** Si la cobertura de `voting` sube, el valor medido se anota en
  `tareas.md` y `pr.md`, y la sesión que abre el PR sube `cobertura.suelo` en
  un commit propio.

## Cambios durante la implementación

Vacío mientras lo implementado coincida con lo especificado.
