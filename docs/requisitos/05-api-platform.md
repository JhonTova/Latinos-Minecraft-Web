# Requisito 05 — RFC-001: API Platform Architecture & Integration Specification

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** especificación de plataforma de APIs — sin endpoints ni código

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM
DOCUMENT 05

RFC-001

API PLATFORM ARCHITECTURE & INTEGRATION SPECIFICATION

VERSION 1.0

==============================================================================
OBJETIVO
==============================================================================

Este documento define la arquitectura completa de la plataforma de APIs.

NO debes escribir código.

NO debes implementar endpoints.

NO debes generar controladores.

NO debes generar DTOs.

NO debes crear clases.

NO debes crear SDKs.

Tu única misión consiste en diseñar la plataforma de APIs que utilizarán:

• El sitio web
• El dashboard
• El panel administrativo
• El plugin Paper
• Los SDK oficiales
• Integraciones de terceros
• Aplicaciones móviles
• Herramientas CLI
• Automatizaciones
• Partners

Piensa como un API Architect Senior de Stripe, GitHub, Cloudflare, Discord o Twilio.

La API será uno de los productos principales de la empresa.

No una simple interfaz para el frontend.

==============================================================================
VISIÓN
==============================================================================

Nuestra API debe convertirse en el estándar del ecosistema Minecraft.

Debe ser:

Simple.

Extremadamente rápida.

Muy bien documentada.

Versionada.

Segura.

Escalable.

Consistente.

Autoexplicativa.

Predecible.

Fácil de integrar.

Preparada para millones de solicitudes diarias.

==============================================================================

API FIRST

==============================================================================

Todo el producto debe diseñarse siguiendo la filosofía API First.

La API es la fuente de verdad.

El frontend consume la API.

El plugin consume la API.

Los SDK consumen la API.

Los partners consumen la API.

Ningún componente debe acceder directamente a la lógica de negocio sin pasar por contratos bien definidos.

Explica completamente esta filosofía.

==============================================================================

ESTÁNDARES

==============================================================================

Analiza y justifica:

REST

OpenAPI 3.1

JSON Schema

OAuth 2.1

OIDC

JWT

Problem Details (RFC 9457)

RFC 9110 HTTP Semantics

RFC 7232 ETags

RFC 7240 Prefer Header

Idempotency Keys

HATEOAS (analiza si aporta valor)

HAL

JSON:API

GraphQL (analiza si conviene)

gRPC (analiza si conviene)

Explica por qué utilizar o descartar cada uno.

==============================================================================

VERSIONADO

==============================================================================

Diseña una estrategia profesional.

v1

v2

v3

Compatibilidad.

Deprecación.

Sunset Policy.

Breaking Changes.

Long Term Support.

Migración de clientes.

==============================================================================

ESTRUCTURA DE LA API

==============================================================================

Diseña la estructura completa.

Public API

Private API

Internal API

Partner API

Admin API

Plugin API

Webhook API

Health API

Metrics API

Status API

Analytics API

Explica responsabilidades y límites.

==============================================================================

RECURSOS

==============================================================================

Diseña todos los recursos principales.

Usuarios.

Organizaciones.

Miembros.

Roles.

Servidores.

Rankings.

Votos.

Recompensas.

Subastas.

Pagos.

Facturas.

Marketplace.

Comentarios.

Notificaciones.

Analytics.

Licencias.

Plugins.

API Keys.

Feature Flags.

Configuraciones.

Estado.

Webhooks.

Heartbeats.

No implementes endpoints.

Diseña únicamente el dominio de la API.

==============================================================================

AUTENTICACIÓN

==============================================================================

Diseña completamente.

OAuth 2.1.

Google.

GitHub.

Discord.

Microsoft.

API Keys.

JWT.

Refresh Tokens.

Machine Tokens.

PAT (Personal Access Tokens).

Scopes.

Expiración.

Rotación.

Revocación.

==============================================================================

AUTORIZACIÓN

==============================================================================

Diseña un sistema moderno.

RBAC.

ABAC.

Permisos.

Scopes.

Organizaciones.

Recursos.

Herencia.

Permisos temporales.

Permisos por servidor.

==============================================================================

API KEYS

==============================================================================

Diseña completamente.

Creación.

Rotación.

Expiración.

Scopes.

Último uso.

Fingerprint.

Restricciones IP.

Restricciones por dominio.

Revocación.

Logs.

==============================================================================

RATE LIMITING

==============================================================================

Diseña un sistema empresarial.

Por usuario.

Por API Key.

Por organización.

Por endpoint.

Por plan.

Burst.

Sliding Window.

Token Bucket.

Leaky Bucket.

Explica qué algoritmo utilizar.

==============================================================================

WEBHOOK PLATFORM

==============================================================================

Diseña una plataforma profesional.

Suscripciones.

Eventos.

Firmas HMAC.

Versionado.

Retry.

Backoff.

Idempotencia.

Dead Letter Queue.

Dashboard.

Logs.

Pruebas.

Reenvío manual.

==============================================================================

EVENTOS

==============================================================================

Define absolutamente todos los eventos públicos.

Vote Received.

Vote Claimed.

Reward Delivered.

Auction Started.

Auction Ended.

Payment Completed.

Payment Failed.

Organization Created.

Server Verified.

Plugin Connected.

License Updated.

Analytics Ready.

Feature Enabled.

Notification Created.

Etc.

==============================================================================

REAL TIME

==============================================================================

Analiza.

WebSockets.

Server Sent Events.

Long Polling.

Explica cuándo utilizar cada uno.

Diseña una arquitectura en tiempo real.

==============================================================================

ERRORES

==============================================================================

Diseña un estándar único.

Problem Details.

Error Codes.

Machine Readable.

Localization.

Correlation ID.

Trace ID.

Retryable.

Validation Errors.

==============================================================================

PAGINACIÓN

==============================================================================

Analiza.

Cursor Pagination.

Offset Pagination.

Keyset Pagination.

Explica cuál utilizar para cada recurso.

==============================================================================

FILTROS

==============================================================================

Diseña una sintaxis uniforme.

Filtering.

Sorting.

Searching.

Projection.

Includes.

Expansion.

Sparse Fields.

==============================================================================

CACHE

==============================================================================

Diseña completamente.

ETags.

Last Modified.

Cache Control.

CDN.

Redis.

Invalidación.

==============================================================================

OBSERVABILIDAD

==============================================================================

Cada request debe incluir.

Request ID.

Correlation ID.

Trace ID.

Latency.

Source.

Rate Limit Headers.

==============================================================================

SDK PLATFORM

==============================================================================

La API deberá generar automáticamente SDK oficiales.

Java.

Kotlin.

PHP.

Node.

Python.

Go.

Rust.

C#.

Explica el proceso.

==============================================================================

DOCUMENTACIÓN

==============================================================================

Diseña un portal profesional.

OpenAPI.

Swagger UI.

Redoc.

Tutoriales.

Quick Start.

Cookbooks.

Ejemplos.

Sandbox.

API Explorer.

Changelog.

==============================================================================

SEGURIDAD

==============================================================================

Analiza.

Replay Attacks.

Brute Force.

Credential Stuffing.

Rate Limits.

JWT Theft.

API Key Leakage.

CORS.

CSRF.

SSRF.

Injection.

Mass Assignment.

Broken Object Level Authorization.

Explica mitigaciones.

==============================================================================

OBSOLESCENCIA

==============================================================================

Diseña una política oficial.

Versiones soportadas.

Versiones obsoletas.

Notificaciones.

Migraciones.

==============================================================================

SLA

==============================================================================

Define objetivos.

Disponibilidad.

Latencia.

Errores.

Tiempo de respuesta.

==============================================================================

DIAGRAMAS
==============================================================================

Quiero.

Mermaid.

Sequence Diagrams.

API Flow.

Authentication Flow.

Webhook Flow.

Plugin Flow.

Payment Flow.

==============================================================================

ADR

==============================================================================

Genera Architecture Decision Records.

ADR-020

REST vs GraphQL.

ADR-021

OpenAPI.

ADR-022

OAuth.

ADR-023

JWT.

ADR-024

Rate Limits.

ADR-025

WebSockets.

==============================================================================

RIESGOS

==============================================================================

Analiza.

Escalabilidad.

Compatibilidad.

Seguridad.

Costos.

Abuso.

Bots.

DoS.

Dependencias.

Propón mitigaciones.

==============================================================================

ENTREGA

==============================================================================

NO escribir código.

NO generar endpoints.

NO generar controladores.

NO crear DTOs.

NO implementar SDKs.

Quiero exclusivamente un RFC profesional que defina la plataforma de APIs completa.

Debe sentirse como la especificación oficial de APIs de una empresa del nivel de Stripe, Discord o GitHub.

Incluye tablas, diagramas Mermaid, comparativas, decisiones arquitectónicas, ventajas, desventajas y justificaciones técnicas.

---

## Notas del CTO sobre este requisito

### Valoración general

**Es el prompt técnicamente más sólido de los cinco.** Las referencias a RFC
son correctas y pertinentes (9457 Problem Details, 9110 semántica HTTP, 7232
ETags, 7240 Prefer). Coincide además con el ADR-0006 ya definido por el CTO:
OpenAPI como fuente de verdad y SDK generados, no escritos a mano.

### Decisiones que el CTO respalda sin cambios

| Decisión | Motivo |
|---|---|
| API First | Correcto y consistente con el resto del proyecto |
| REST + OpenAPI 3.1 | Estándar del sector, tooling maduro, generación de SDK |
| Problem Details (RFC 9457) | Errores legibles por máquina y consistentes en toda la API |
| Idempotency Keys | Obligatorio: el plugin reintenta y no puede duplicar recompensas |
| Webhooks firmados con HMAC + reintentos + DLQ | Correcto; es como lo hacen Stripe y GitHub |
| Cursor / keyset pagination | Correcto para rankings y listados grandes |
| Rate limit por API Key, plan y endpoint | Necesario desde el día 1 |

### Veredictos sobre los estándares que el prompt pide analizar

| Estándar | Veredicto | Motivo |
|---|---|---|
| GraphQL | ❌ No | Rate limiting y caché HTTP se vuelven muy difíciles; riesgo de consultas costosas; superficie de abuso mayor en una API pública |
| gRPC | ❌ No ahora | Sin sentido dentro de un monolito modular; se puede añadir para tráfico interno si se extraen servicios |
| HATEOAS completo | ❌ No | Coste alto, adopción real casi nula. Stripe y GitHub no lo implementan de forma estricta |
| HAL / JSON:API | ❌ No | Añaden envoltorio sin beneficio para este consumidor |
| OAuth 2.1 + OIDC | ✅ Sí | Para login de usuarios y aplicaciones de terceros |
| API Keys | ✅ Sí | Para el plugin y automatizaciones servidor-a-servidor |
| ETags / Cache-Control | ✅ Sí | El ranking es altamente cacheable |

### Problema 1 — Once APIs no son once APIs

El prompt define: Public, Private, Internal, Partner, Admin, Plugin, Webhook,
Health, Metrics, Status, Analytics.

Reclasificación real:

| Lo que pide el prompt | Qué es en realidad |
|---|---|
| Public + Partner + Analytics | **Una** API pública, con permisos distintos por scope |
| Private + Internal | La misma API pública consumida por el frontend propio |
| Plugin | **Sí es una superficie distinta**: otro consumidor, otra auth, otro ciclo de versionado |
| Admin | **Sí es una superficie distinta**: acceso privilegiado, aislada por seguridad |
| Webhook | No es una API entrante: es tráfico **saliente** |
| Health + Metrics + Status | Endpoints de operación, no productos |

**Propuesta:** 3 superficies reales (Pública, Plugin, Admin) + webhooks
salientes + endpoints de operación. Cada superficie extra implica su propia
política de versionado, documentación, límites y compatibilidad — coste
permanente.

### Problema 2 — 8 SDK oficiales es una trampa de mantenimiento

Generar un SDK es barato. **Mantenerlo no lo es**: publicación en el registro
del lenguaje, versionado, issues de la comunidad, ejemplos, migraciones ante
cambios incompatibles, idiomática propia de cada lenguaje.

Análisis de audiencia real en el ecosistema Minecraft:

| SDK | Audiencia real | Veredicto |
|---|---|---|
| **Java** | Desarrolladores de plugins — el público principal | ✅ Oficial |
| **Node/TS** | Bots de Discord, webs de servidores | ✅ Oficial (2ª fase) |
| **PHP** | Webs de servidores, muy común en el sector | 🟡 Considerar |
| Kotlin | Consume el SDK de Java sin problema | ❌ Innecesario |
| Python, Go, Rust, C# | Audiencia marginal en este ecosistema | ❌ Bajo demanda |

**Alternativa recomendada:** publicar la especificación OpenAPI. Cualquiera
puede generar su propio cliente en cualquier lenguaje en un minuto, sin que
la plataforma adquiera obligación de mantenimiento.

### Problema 3 — Un SLA público es un contrato, no un objetivo

El prompt pide "definir SLA". Un SLA es un compromiso contractual con
penalizaciones. Publicar 99,9% sin redundancia, sin guardia 24/7 y sin
despliegue multi-zona es prometer algo que se incumplirá.

**Secuencia correcta:** SLO interno primero (objetivo medible, sin
compromiso externo) → medir durante meses → SLA público solo en planes
Enterprise, cuando la infraestructura pueda sostenerlo.

### Observación de diseño: los heartbeats dominarán el tráfico, no los votos

Cálculo con 10.000 servidores conectados:

| Intervalo de heartbeat | Peticiones/día solo por heartbeats |
|---|---|
| 15 s | 57.600.000 |
| 30 s | 28.800.000 |
| 60 s | 14.400.000 |
| 5 min | 2.880.000 |

Los votos, en el mismo escenario, difícilmente pasarían de decenas o cientos
de miles diarios. Es decir: **el intervalo de heartbeat es la decisión que
determina el coste de infraestructura de la plataforma entera**, y merece
tratamiento explícito (intervalo adaptativo, backoff cuando no hay cambios,
payload mínimo, endpoint separado y barato).

### Numeración de ADR

El prompt propone ADR-020 a ADR-025. Se unificará en la secuencia continua
del proyecto.
