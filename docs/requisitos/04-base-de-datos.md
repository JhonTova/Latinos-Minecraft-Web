# Requisito 04 — Database Architecture & Data Model Specification

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** modelo de datos — sin SQL ni migraciones

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 04

DATABASE ARCHITECTURE & DATA MODEL SPECIFICATION

VERSION 1.0

=====================================================================
OBJETIVO
=====================================================================

Este documento define la arquitectura completa de la base de datos.

NO debes escribir código.

NO debes generar entidades.

NO debes crear migraciones.

NO debes generar SQL.

NO debes implementar absolutamente nada.

Tu misión consiste únicamente en diseñar el modelo de datos de nivel empresarial que utilizará toda la plataforma.

Piensa como un Database Architect Senior de Stripe, GitHub, Atlassian o Cloudflare.

Cada decisión debe justificarse.

No quiero simplemente una lista de tablas.

Quiero una arquitectura de datos profesional.

=====================================================================
OBJETIVOS
=====================================================================

La base de datos deberá ser capaz de soportar:

• Más de 100.000 servidores registrados.

• Más de 20 millones de usuarios.

• Más de 500 millones de votos.

• Más de 100 millones de registros de auditoría.

• Miles de conexiones simultáneas.

• Escalado horizontal mediante réplicas de lectura.

• Alta disponibilidad.

• Mantenimiento sin downtime.

=====================================================================
TECNOLOGÍA
=====================================================================

Analiza y justifica la elección.

Mi preferencia inicial es:

PostgreSQL

Última versión estable.

Flyway.

UUIDv7.

JSONB cuando tenga sentido.

Índices parciales.

Materialized Views cuando aporten valor.

Explica si existe alguna alternativa superior.

=====================================================================
FILOSOFÍA
=====================================================================

La base de datos debe priorizar:

Consistencia.

Escalabilidad.

Normalización.

Rendimiento.

Auditoría.

Mantenibilidad.

Seguridad.

Extensibilidad.

No quiero una base de datos improvisada.

Debe poder evolucionar durante los próximos diez años.

=====================================================================
CONVENCIONES
=====================================================================

Define todas las convenciones.

Nombres de tablas.

Nombres de columnas.

UUID.

Fechas.

Timezones.

Soft Deletes.

Versionado.

Optimistic Locking.

Created At.

Updated At.

Deleted At.

Audit Columns.

=====================================================================
MODELO DE DATOS
=====================================================================

Diseña absolutamente todas las entidades necesarias.

No omitas ninguna.

Analiza el dominio completo del proyecto.

Incluye como mínimo:

Usuarios.

Organizaciones.

Miembros.

Roles.

Permisos.

Servidores.

Versiones.

Plugins.

Licencias.

API Keys.

OAuth Accounts.

Sesiones.

Dispositivos.

Autenticación.

Tokens.

Votos.

Reclamaciones.

Recompensas.

Rankings.

Snapshots.

Subastas.

Lotes de subastas.

Pujas.

Ganadores.

Pagos.

Facturas.

Transacciones.

Ledger.

Webhooks.

Notificaciones.

Emails.

Discord.

Feature Flags.

Configuraciones.

Marketplace.

Comentarios.

Reportes.

Favoritos.

Analytics.

Heartbeats.

Estado de servidores.

Logs.

Audit Logs.

Errores.

Background Jobs.

Scheduler.

Health Checks.

Uploads.

Media.

Archivos.

Todo lo necesario.

=====================================================================
RELACIONES
=====================================================================

Diseña completamente las relaciones.

Uno a uno.

Uno a muchos.

Muchos a muchos.

Entidades puente.

Relaciones opcionales.

Relaciones obligatorias.

Restricciones.

ON DELETE.

ON UPDATE.

=====================================================================
ÍNDICES
=====================================================================

Para cada tabla.

Analiza:

Primary Keys.

Unique Keys.

Foreign Keys.

Composite Indexes.

Partial Indexes.

GIN.

GiST.

BRIN.

Hash.

Explica cuándo utilizar cada uno.

=====================================================================
PARTICIONAMIENTO
=====================================================================

Analiza si conviene particionar.

Especialmente para:

Votes.

Audit Logs.

Analytics.

Payments.

Heartbeats.

Explica ventajas y desventajas.

=====================================================================
JSONB
=====================================================================

Analiza cuidadosamente cuándo utilizar JSONB.

No abuses de él.

Explica cuándo es correcto.

Cuándo NO.

=====================================================================
AUDITORÍA
=====================================================================

Diseña un sistema completo.

Toda modificación importante debe quedar registrada.

Nunca eliminar información.

Explica qué tablas requieren auditoría.

Qué eventos registrar.

Cómo consultar el historial.

=====================================================================
LEDGER
=====================================================================

Diseña un Ledger financiero.

Todas las operaciones económicas deben ser inmutables.

Nunca modificar registros.

Nunca eliminar registros.

Solo agregar movimientos.

Explica completamente el modelo.

=====================================================================
MULTITENANCY
=====================================================================

La plataforma será multiempresa.

Diseña completamente el aislamiento.

Organizaciones.

Usuarios.

Permisos.

Configuraciones.

Facturación.

Servidores.

Explica cómo garantizar que nunca exista fuga de datos entre organizaciones.

=====================================================================
ESTRATEGIA DE MIGRACIONES
=====================================================================

Diseña una estrategia profesional.

Flyway.

Versionado.

Rollback.

Migraciones destructivas.

Migraciones seguras.

Backfills.

Compatibilidad.

=====================================================================
ESCALABILIDAD
=====================================================================

Explica cómo escalar.

Read Replicas.

Connection Pool.

PgBouncer.

Particionamiento.

Archivado.

Compresión.

Materialized Views.

Cache.

=====================================================================
OPTIMIZACIÓN
=====================================================================

Explica técnicas para:

Reducir locks.

Reducir deadlocks.

Reducir scans completos.

Reducir IO.

Optimizar consultas.

Optimizar escritura.

Optimizar lectura.

=====================================================================
BACKUPS
=====================================================================

Diseña la estrategia.

Backups completos.

Incrementales.

PITR.

Snapshots.

Retención.

Pruebas de restauración.

=====================================================================
SEGURIDAD
=====================================================================

Explica.

Encriptación.

Secrets.

RLS (Row Level Security).

Permisos.

Accesos.

Rotación.

Auditoría.

=====================================================================
DIAGRAMAS
=====================================================================

Quiero:

Entity Relationship Diagram.

Mermaid.

Dependencias.

Agrupación por dominios.

=====================================================================
DATA LIFECYCLE
=====================================================================

Explica el ciclo de vida completo de los datos.

Creación.

Actualización.

Archivado.

Expiración.

Eliminación lógica.

Eliminación física.

Retención.

=====================================================================
ARCHITECTURE DECISION RECORDS
=====================================================================

Genera ADR relacionados con la base de datos.

Ejemplo.

ADR-010

UUIDv7 vs BIGINT.

ADR-011

PostgreSQL.

ADR-012

JSONB.

ADR-013

Ledger.

ADR-014

Soft Deletes.

ADR-015

Partitioning.

=====================================================================
RIESGOS
=====================================================================

Analiza.

Cuellos de botella.

Crecimiento.

Migraciones.

Locks.

Integridad.

Pérdida de datos.

Recuperación.

Complejidad.

=====================================================================
ENTREGA
=====================================================================

NO escribir código.

NO generar SQL.

NO generar entidades Java.

NO crear migraciones.

Quiero únicamente el diseño profesional de la base de datos.

Debe sentirse como el documento interno de arquitectura de datos de una empresa tecnológica de primer nivel.

Incluye tablas, diagramas Mermaid, ejemplos, comparativas y justificaciones técnicas para cada decisión.

Si detectas oportunidades de mejora, propón mejoras adicionales.

---

## Notas del CTO sobre este requisito

### Descubrimiento importante: el stack queda revelado

El prompt dice *"NO generar entidades **Java**"*. Combinado con señales de los
documentos anteriores —**Flyway**, **Caffeine**, **plugin Paper**— el stack
previsto es:

**Java + Spring Boot + PostgreSQL + Flyway**

Esto responde una de las preguntas bloqueantes que el CTO había planteado.
Valoración: **es una buena elección para este dominio concreto**, porque el
plugin de Minecraft es obligatoriamente Java. Un solo lenguaje para backend y
plugin permite compartir modelos, contratos y SDK, y elimina una clase entera
de errores de desincronización. Pendiente de confirmación explícita.

### Decisiones del prompt que el CTO respalda

| Decisión | Veredicto | Motivo |
|---|---|---|
| PostgreSQL | ✅ Correcta | Transaccional real (necesario para Outbox y Ledger), JSONB, window functions para rankings, particionado nativo, ecosistema maduro |
| **UUIDv7** | ✅ Muy buena | Ordenado temporalmente: evita la fragmentación de índices típica de UUIDv4 sin exponer IDs secuenciales adivinables. Elección de calidad. |
| Flyway | ✅ Correcta | Estándar en el ecosistema Java |
| Ledger inmutable (solo append) | ✅ Correcta e innegociable | Nunca hacer UPDATE sobre dinero. Correcciones = asiento compensatorio. |
| RLS para multitenancy | ✅ Correcta | Red de seguridad a nivel de motor: aunque un `WHERE organization_id` falte en el código, la fuga de datos no ocurre |
| Particionar votes / audit / heartbeats | ✅ Correcta en el diseño | Implementar cuando el volumen lo justifique, no antes |
| "No abuses de JSONB" | ✅ Buena advertencia | JSONB solo para datos heterogéneos y no consultables como criterio principal |

### Problema 1 — "Diseña absolutamente todas las entidades. No omitas ninguna."

La lista contiene ~50 grupos de entidades, lo que en la práctica son
**80-120 tablas**.

Diseñar las tablas de subastas, marketplace, pujas, lotes y ganadores **hoy**,
para funcionalidades que se construirán dentro de 1-2 años, garantiza que
estarán mal: el diseño de datos correcto depende de reglas de negocio que
todavía no existen.

Y sin embargo, el esquema es lo más caro de cambiar una vez hay datos reales.
Esa combinación —máximo coste de error, mínima información disponible— es
exactamente donde el diseño anticipado falla.

**Propuesta:**

| Grupo | Profundidad de diseño |
|---|---|
| Identity, Organizations, Servers, Votes, Audit, Ledger, Trust&Abuse | **Completo**: columnas, tipos, índices, constraints, particionado |
| Rankings, Analytics, Notifications, Webhooks, Jobs | **Medio**: entidades y relaciones, sin detalle de columnas |
| Marketplace, Subastas, Publicidad, Licencias | **Solo fronteras**: qué contexto las poseerá y cómo se relacionarán |

### Problema 2 — Conflicto legal real: "Nunca eliminar información" vs. derecho de supresión

El prompt lo repite dos veces: *"Nunca eliminar información"*, *"Nunca eliminar
registros"*, más soft deletes en todas partes.

Esto **choca frontalmente con el RGPD (Art. 17, derecho de supresión)**. Un
usuario europeo puede exigir el borrado de sus datos personales, y "los
guardamos todos para siempre" no es una respuesta válida. El riesgo se agrava
porque una parte importante de la base de usuarios de Minecraft son menores.

Matiz que resuelve el conflicto: el derecho de supresión **tiene excepciones**,
entre ellas el cumplimiento de obligaciones legales (los registros contables y
fiscales deben conservarse durante años).

**Diseño correcto:**

| Tipo de dato | Política |
|---|---|
| Registros financieros (ledger, facturas) | **Se conservan** — obligación legal. Se anonimiza al titular, no el importe. |
| Datos personales (email, IP, nombre) | **Se anonimizan o borran** a petición |
| Votos | Se conserva el hecho agregado; se desvincula la identidad |
| Audit logs | Se conserva la acción; se pseudonimiza al actor tras el plazo de retención |
| IPs para antifraude | Retención corta y definida (ej. 90 días), después hash o borrado |

Sin esta política definida **antes** de crear el esquema, cada tabla nace con
una bomba de relojería legal.

### Problema 3 — Herencia del sobredimensionado del Documento 03

Se repiten los mismos objetivos (20M usuarios, 500M votos, 100M audit logs).
Consecuencia aquí: PgBouncer, réplicas de lectura, particionado activo,
archivado y compresión son correctos **como diseño previsto**, pero
**no deben implementarse el primer año**. Se documentan como plan de
crecimiento con umbrales de activación medibles.

### Numeración de ADR

El prompt propone ADR-010 a ADR-015, dejando un hueco entre los ADR de
arquitectura y los de datos. Se unificará en una única secuencia continua.
