# ADR-0003 — PostgreSQL como cola de mensajes (sin Kafka ni RabbitMQ)

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Contexto

Los documentos 03 y 08 proponían evaluar Kafka, RabbitMQ, NATS y Redis Streams
para la mensajería interna y la entrega de eventos.

Restricciones reales del proyecto: equipo de una persona, sin ingeniería de
operaciones dedicada, objetivo de ~200.000 votos/día a 24 meses (ver
`DECISIONES.md` § 5).

## Decisión

**PostgreSQL como cola**, mediante tabla Outbox con marcado transaccional y
consumo con `SELECT ... FOR UPDATE SKIP LOCKED`.

Ni Kafka ni RabbitMQ en la v1.

## Justificación

| Criterio | Postgres | RabbitMQ | Kafka |
|---|---|---|---|
| Servicios adicionales que operar | **0** | 1 | 1 + coordinación |
| Transaccionalidad con la escritura de dominio | ✅ Nativa | ❌ Requiere Outbox igualmente | ❌ Igual |
| Capacidad para el volumen objetivo | ✅ Sobrada | ✅ | ✅ Excesiva |
| Coste operativo | Nulo | Medio | Alto |
| Depuración a las 3 de la mañana | `SELECT * FROM outbox` | Herramientas propias | Complejo |

El punto decisivo: **el Outbox hace falta de todos modos**. Para garantizar que
un voto aceptado se entrega, la escritura de dominio y la publicación del
evento deben compartir transacción — y eso solo lo garantiza la base de datos.
Añadir un broker no elimina la tabla Outbox; añade un componente más.

## Consecuencias

- La cola es inspeccionable con SQL: enorme ventaja operativa con un solo
  operador
- Reintentos con backoff exponencial y DLQ se implementan como columnas de
  estado en la tabla
- **Umbral de revisión:** si la tabla Outbox supera sostenidamente el millón de
  filas pendientes, o si aparecen consumidores que necesitan replay histórico
  independiente, se reabre esta decisión con un nuevo ADR
- Los contratos de evento se definen igual que si fueran de red (AsyncAPI), de
  modo que migrar a un broker no cambie el código de dominio
