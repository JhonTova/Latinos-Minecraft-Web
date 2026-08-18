# ADR-0002 — PostgreSQL como almacén primario

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Decisión

**PostgreSQL** (última versión estable) con **Flyway** para migraciones y
**UUIDv7** como clave primaria.

## Justificación

| Alternativa | Veredicto |
|---|---|
| **PostgreSQL** | ✅ Transaccionalidad real (necesaria para Outbox y ledger), JSONB para señales de fraude heterogéneas, window functions para rankings, particionado nativo, índices parciales y GIN |
| CockroachDB / YugabyteDB | ❌ Resuelven escritura multi-región, problema que no existe aquí. Coste alto y limitaciones de dialecto. |
| MySQL / MariaDB | ❌ Sin equivalente real a JSONB, particionado más limitado, peor ajuste al caso |

### UUIDv7 sobre UUIDv4 y BIGINT

| Opción | Problema |
|---|---|
| BIGINT secuencial | IDs adivinables; facilita enumeración de recursos |
| UUIDv4 | Aleatorio puro → fragmenta los índices B-tree y degrada la inserción |
| **UUIDv7** | ✅ Ordenado temporalmente: localidad de inserción como un secuencial, sin exponer un contador |

## Consecuencias

- Toda migración pasa por Flyway, versionada en el repositorio
- JSONB se usa **solo** para datos heterogéneos que no son criterio principal
  de consulta (señales de fraude, metadatos). Nunca como sustituto de columnas.
- Particionado de `votes`, `audit_log` y `heartbeats` **diseñado desde el
  inicio**, activado cuando el volumen lo justifique
- Réplicas de lectura, PgBouncer, archivado y compresión: documentados como
  plan de crecimiento con umbrales, **no implementados en la v1**
