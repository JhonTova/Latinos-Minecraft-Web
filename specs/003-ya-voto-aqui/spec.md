# Spec 003 — Avisar al plugin de que el jugador ya votó a este servidor

- **Estado:** Implementada, pendiente de la fusión (aprobada el 17/11/2026, con las respuestas P-1 a P-7 del plan)
- **Rama:** `feat/003-ya-voto-aqui`
- **Parte:** backend
- **Riesgo:** 🔴 crítico (módulo `voting`; la respuesta sale por `/api/v1/plugin/*`)

## Problema

`CastVoteService.puedeVotar` existe para que el plugin pregunte **antes** de
darle un código al jugador (`POST /api/v1/plugin/vote-tokens`, en
`PluginController.emitirToken`). Su documentación dice que el motivo puede ser
`YA_VOTO_HOY` o `YA_VOTO_AQUI`, y el plugin (`JhonTova/LatinosMC`,
`ComandoVotar`) ya tiene un mensaje para cada uno.

Pero cuando la regla «un voto al día» está desactivada, `puedeVotar` responde
siempre que sí: **nunca devuelve `YA_VOTO_AQUI`**. El comentario del código lo
justifica con que «aquí solo llega el identificador, no la ficha» y cada
servidor tenía su ventana. Eso dejó de ser cierto con ADR-0023: la ventana es
la misma para todos y la fija la plataforma (`reglas.ventana()`).

Caso concreto: con la regla global desactivada y una ventana de 24 h, Steve
vota a «Servidor A» a las 9:00. A las 15:00 escribe `/vote` en ese mismo
servidor: recibe un código, abre el enlace, y solo entonces `votar()` lo
rechaza con `vote.window.open`. Cuatro pasos para llegar a un «no» que la
plataforma sabía desde el principio, y que el plugin ya sabe explicar.

## Qué se construye

Con la regla global desactivada, si el jugador tiene un voto **aceptado** a ese
servidor dentro de la ventana, `puedeVotar` responde que no puede, con el
motivo `YA_VOTO_AQUI` y el tiempo que le falta. Sigue siendo una consulta de
cortesía: no escribe nada y quien decide es `votar()`.

## Criterios de aceptación

Notación: «la ventana» es `reglas.ventana()` (ADR-0023); «el último voto aquí»
es el último voto **aceptado** de esa identidad a ese servidor.

| ID | Dado / cuando / entonces |
|---|---|
| CA-1 | Dada la regla global desactivada, una ventana de 24 h y un último voto aquí hace 6 h, cuando se llama a `puedeVotar`, entonces devuelve `puede = false`, `motivo = "YA_VOTO_AQUI"` y `restante = 18 h` exactas. |
| CA-2 | Dada la regla global desactivada y un último voto aquí cuyo fin de ventana coincide exactamente con el instante actual, cuando se llama a `puedeVotar`, entonces devuelve `puede = true`, `motivo = null` y `restante = 0` (mismo borde que `votar()`: la ventana es semiabierta). Un segundo antes del fin, devuelve `YA_VOTO_AQUI` con `restante = 1 s`. |
| CA-3 | Dada la regla global desactivada y un último voto aquí más antiguo que la ventana, cuando se llama a `puedeVotar`, entonces devuelve `puede = true`, `motivo = null` y `restante = 0`. |
| CA-4 | Dada la regla global desactivada y ningún voto aceptado aquí (aunque haya votos aceptados recientes a **otros** servidores), cuando se llama a `puedeVotar`, entonces devuelve `puede = true`, `motivo = null` y `restante = 0`: sin la regla global, votar a otro servidor no bloquea este. |
| CA-5 | Dada la regla global desactivada, cuando se llama a `puedeVotar`, entonces la búsqueda del último voto se hace por el **servidor recibido** y el hash de la identidad (la que también usa `votar()`), y no se consulta el último voto en cualquier servidor. |
| CA-6 | Dada la regla global **activada**, cuando se llama a `puedeVotar`, entonces el comportamiento es el de hoy: `YA_VOTO_HOY` con su tiempo restante si hay un voto aceptado en cualquier servidor dentro de la ventana, y `puede = true` si no. |
| CA-7 | Dado cualquier caso, cuando se llama a `puedeVotar`, entonces no escribe nada: no inserta votos, ni recompensas, ni señales, ni publica eventos. |
| CA-8 | Dada la regla global desactivada y un último voto aquí dentro de la ventana, cuando el plugin llama a `POST /api/v1/plugin/vote-tokens`, entonces la respuesta tiene la misma forma que hoy con `puedeVotar: false`, `motivo: "YA_VOTO_AQUI"`, `segundosRestantes` igual al restante en segundos, `token` y `url` nulos, y no se emite ningún código. |
| CA-9 | Dado el diff de la rama, entonces no cambia la forma de `RespuestaToken` ni de `PeticionToken`, ni ninguna otra ruta; no hay migración, ni dependencia nueva, ni toca el ledger; el comentario de `puedeVotar` que justificaba no comprobar la ventana se reemplaza por uno que cite ADR-0023; y `mvn -B verify` pasa en `backend/`. |

## Fuera de alcance

- Cambiar `votar()`: sigue siendo quien decide, con su orden de comprobaciones.
- Con la regla global activada, distinguir «ya votó aquí» de «ya votó hoy en
  otro»: hoy devuelve `YA_VOTO_HOY` en ambos casos y así sigue (decidido en
  P-3 de [plan.md](plan.md)).
- **Posible mejora: redondear `segundosRestantes` hacia arriba.** El
  controlador lo calcula con `Duration.toSeconds()`, que trunca: si falta menos
  de un segundo, el plugin recibe `puedeVotar: false` con
  `segundosRestantes: 0`. Ya pasa hoy con `YA_VOTO_HOY`. Se deja así (P-1); si
  hace falta, irá en otra spec.
- Actualizar el estado de ADR-0016 («Propuesto» aunque está implementado): va
  en una rama `docs/` aparte (P-7).
- Adelantar en `puedeVotar` otras reglas de `votar()`: cupo por IP, antifraude,
  identidades no verificables, servidor inactivo.
- Cambiar el plugin (`JhonTova/LatinosMC`): ya entiende `YA_VOTO_AQUI`.
- Cambiar la forma de la respuesta o el nombre de los motivos.

## Paradas

- [x] Cambia el contrato `/api/v1/plugin/*` — **en su comportamiento, no en su
  forma**: `POST /api/v1/plugin/vote-tokens` empezará a devolver
  `motivo: "YA_VOTO_AQUI"`, un valor documentado que hasta ahora nunca salía.
  **Aprobada por el desarrollador** el 17/11/2026. Él comprobó en el
  repositorio del plugin que la release publicada `v0.2.0` ya lo maneja:
  `ComandoVotar.avisarQueYaVoto` usa la clave `ya-voto-aqui` («Ya votaste a
  este servidor. Vuelve en {restante}.») y lee `segundosRestantes`. **No hace
  falta publicar una versión nueva del plugin.** Se avisa en la primera línea
  del PR.
- [ ] Necesita una migración Flyway
- [ ] Escribe en el ledger
- [ ] Agrega una dependencia

## Decisiones del desarrollador

Las preguntas P-1 a P-7 de [plan.md](plan.md) se respondieron el 17/11/2026.
Ninguna cambia los criterios: la parada del contrato queda aprobada, la
compatibilidad del plugin está comprobada (`v0.2.0`) y lo demás queda fuera de
alcance o se resuelve en el proceso (prueba de CA-8, mutación, cobertura).

## Cambios durante la implementación

Vacío mientras lo implementado coincida con lo especificado.
