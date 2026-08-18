# ADR-0015 — Deuda deliberada: el antifraude lee la tabla de votos

- **Estado:** ✅ Vigente · **Deuda deliberada y prudente** (ADR-0007)
- **Fecha:** 2026-08-03
- **Responsable:** el equipo de plataforma
- **Fecha de pago:** cuando el motor antifraude supere 8 reglas o aparezca una segunda fuente de votos

## Contexto

La regla 9 de las convenciones del proyecto dice: *ningún módulo accede a tablas de otro módulo*.

El motor antifraude (`trust`) necesita responder preguntas sobre el historial
completo de votos: cuántas identidades distintas han votado desde una IP,
cuántos votos hubo en la última hora, cuántos votos aceptados acumula una
identidad. Esos datos viven en la tabla `votes`, propiedad del módulo `voting`.

## Alternativas consideradas

| Opción | Coste | Problema |
|---|---|---|
| **A) `trust` lee `votes` directamente** | Nulo | Rompe la frontera de módulos |
| B) Read model propio de `trust` alimentado por eventos | Alto | Duplica el volumen completo de votos; introduce consistencia eventual justo en la ruta crítica de decisión |
| C) `voting` expone un puerto de consulta en `shared` | Medio | El puerto acabaría siendo un reflejo de las consultas del antifraude: acoplamiento igual, con una capa de indirección que solo lo disimula |

## Decisión

Se adopta **A** como deuda deliberada.

El antifraude ejecuta consultas de solo lectura sobre `votes`, encapsuladas en
una única clase (`TrustQueries`). Ningún otro punto de `trust` toca esa tabla.

## Por qué es prudente y no temeraria

- Es **solo lectura**: no puede corromper el estado de `voting`
- Está **encapsulada en una clase**: la superficie de acoplamiento es conocida y pequeña
- Está **registrada aquí**, con responsable y condición de pago
- La alternativa correcta (B) duplicaría todo el volumen de votos para un
  beneficio nulo en la fase actual

## Condición de pago

Se paga cuando ocurra cualquiera de estas dos cosas:

1. El motor antifraude supere las **8 reglas** (señal de que necesita su propio modelo)
2. Aparezca una **segunda fuente de votos** (agregación de terceros), momento en
   que `votes` deja de ser el historial completo

Forma de pago: extraer un read model propio de `trust`, alimentado por eventos
del outbox.

## Test que falla cuando deja de ser aceptable

`ArquitecturaTest.antifraudeSoloLeeVotosDesdeUnaClase` verifica que la única
clase de `trust` que menciona la tabla `votes` es `TrustQueries`. Si alguien
añade una segunda, el test falla y obliga a revisar esta decisión.
