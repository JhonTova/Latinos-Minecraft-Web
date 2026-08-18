# ADR-0001 — Monolito modular, despliegue único

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Contexto

Opciones evaluadas: monolito clásico, monolito modular, microservicios,
arquitectura híbrida y serverless.

## Decisión

**Monolito modular** con un único artefacto desplegable, preparado para
extraer módulos a servicios independientes si algún día se justifica.

## Justificación

| Opción | Veredicto |
|---|---|
| Microservicios | ❌ Resuelven un problema de **equipos** (autonomía de despliegue entre escuadrones), no de código. Con un equipo de una persona, solo añaden coste operativo, latencia de red y complejidad de depuración. |
| Serverless | ❌ Los arranques en frío y las conexiones persistentes de plugins encajan mal. Además complica el modelo de costes. |
| Monolito clásico | ❌ Sin fronteras internas degenera en big ball of mud |
| **Monolito modular** | ✅ Fronteras reales, coste operativo de un solo servicio |

Nota de honestidad: lo más probable es que el monolito modular sea el **estado
final**, no una etapa de transición. La opción de extraer se mantiene abierta
porque su coste es casi nulo, no porque se prevea ejercerla.

## Reglas verificadas en CI

"Preparado para migrar" significa cosas concretas y comprobables:

1. Ningún módulo accede a las tablas de otro módulo
2. La comunicación entre módulos ocurre por interfaces públicas explícitas o
   por eventos, nunca importando internals
3. El bus de eventos es in-process, pero su **contrato** (esquema, versionado,
   semántica de entrega) es idéntico al que tendría sobre red
4. Cada módulo posee su propio esquema/namespace en la base de datos

Estas cuatro reglas se verifican con tests de arquitectura en CI. Si se
cumplen, extraer un módulo es trabajo de días.

## Consecuencias

- Un solo pipeline, un solo despliegue, una sola base de datos
- Los tests de arquitectura son obligatorios: sin ellos las reglas se erosionan
- Cambiar esta decisión requiere un nuevo ADR con evidencia medida, no intuición
