# Requisito 03 — Software Architecture Blueprint (SAB)

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** arquitectura de software — sin implementación

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM
## DOCUMENT 03
### SOFTWARE ARCHITECTURE BLUEPRINT (SAB)

=====================================================================
OBJETIVO
=====================================================================

Este documento definirá la arquitectura completa del sistema.

Todavía NO debes escribir código.

NO debes implementar absolutamente nada.

Tu única misión consiste en diseñar una arquitectura empresarial que pueda soportar millones de usuarios durante los próximos diez años.

Piensa como un Principal Software Architect de Google, Stripe, Cloudflare o GitHub.

No aceptes soluciones simples si existe una mejor alternativa.

Cada decisión debe estar justificada.

Cada patrón debe explicarse.

Cada módulo debe tener una responsabilidad única.

=====================================================================
FILOSOFÍA ARQUITECTÓNICA
=====================================================================

Quiero una arquitectura extremadamente modular.

Debe priorizar:

Escalabilidad

Seguridad

Mantenibilidad

Extensibilidad

Observabilidad

Alta disponibilidad

Testing

Documentación

Todo el sistema debe diseñarse para soportar crecimiento sin reescribir componentes.

=====================================================================
PRINCIPIOS OBLIGATORIOS
=====================================================================

Diseña la plataforma utilizando únicamente buenas prácticas modernas.

Debe aplicar cuando tenga sentido:

Domain Driven Design (DDD)

Clean Architecture

SOLID

Repository Pattern

Specification Pattern

Factory Pattern

Strategy Pattern

Builder Pattern

Dependency Injection

Mediator Pattern

Observer Pattern

Command Pattern

Decorator Pattern

Adapter Pattern

Facade Pattern

Circuit Breaker

Retry Pattern

Outbox Pattern

Inbox Pattern

Event Sourcing (analiza si aporta valor)

CQRS (analiza si realmente conviene)

Saga Pattern (para pagos y procesos distribuidos)

Idempotency

Optimistic Locking

Pessimistic Locking cuando sea necesario

=====================================================================
ARQUITECTURA GENERAL
=====================================================================

Analiza y decide cuál es la mejor opción.

Monolito

Microservicios

Monolito Modular

Hybrid Architecture

Serverless

Explica ventajas.

Desventajas.

Costos.

Complejidad.

Mi preferencia inicial es un Monolito Modular preparado para migrar a microservicios.

Analiza si realmente es la mejor decisión.

=====================================================================
MÓDULOS
=====================================================================

Divide completamente el sistema.

Ejemplo.

Core

Identity

Users

Organizations

Servers

Votes

Vote Engine

Ranking Engine

Auction Engine

Payment Engine

Analytics

Notifications

Marketplace

Admin

Dashboard

Licensing

Plugin API

Public API

Internal API

Audit

Feature Flags

Billing

Settings

Media

Search

Jobs

Scheduler

Webhooks

SDK

Status

Monitoring

Health

Define responsabilidades.

Dependencias.

Comunicación.

Límites.

=====================================================================
DDD
=====================================================================

Define completamente los Bounded Contexts.

Define:

Entities

Value Objects

Aggregates

Repositories

Domain Events

Factories

Policies

Specifications

Application Services

Infrastructure

Explica cada uno.

=====================================================================
COMUNICACIÓN ENTRE MÓDULOS
=====================================================================

NO quiero dependencias innecesarias.

Analiza:

Eventos

Mensajería

Interfaces

Puertos

Adaptadores

Event Bus

Domain Events

Integration Events

Cuando usar llamadas directas.

Cuando usar eventos.

=====================================================================
EVENT BUS
=====================================================================

Diseña un Event Bus interno.

Todos los módulos deberán comunicarse mediante eventos cuando sea posible.

Ejemplos:

VoteReceived

VoteValidated

VoteClaimed

AuctionStarted

AuctionEnded

PaymentAuthorized

PaymentSucceeded

PaymentFailed

NotificationCreated

PluginConnected

ServerHeartbeatReceived

OrganizationCreated

UserRegistered

ServerVerified

FeatureEnabled

Explica todo el flujo.

=====================================================================
PLUGIN SYSTEM
=====================================================================

La plataforma debe ser extensible.

Diseña un sistema de módulos internos.

Cada módulo debe poder instalarse, deshabilitarse o evolucionar independientemente.

No quiero un Core gigante.

=====================================================================
FEATURE FLAGS
=====================================================================

Diseña un sistema profesional.

Debe permitir activar funciones por:

Usuario.

Organización.

Servidor.

Plan.

Región.

Versión.

Porcentaje de usuarios.

Beta testers.

=====================================================================
CONFIGURACIÓN
=====================================================================

Diseña un Configuration Service.

Centralizado.

Versionado.

Auditable.

Con rollback.

=====================================================================
CACHE
=====================================================================

Diseña completamente la estrategia.

Qué se cachea.

Qué no.

TTL.

Invalidación.

Redis.

Cache local.

Caffeine.

Cache distribuida.

=====================================================================
JOBS
=====================================================================

Diseña un Job Engine.

Todo proceso pesado debe ejecutarse aquí.

Ejemplos.

Correos.

Discord.

Recalcular rankings.

Actualizar estadísticas.

Generar imágenes.

Exportar datos.

Actualizar servidores.

Sincronizar plugins.

=====================================================================
SCHEDULER
=====================================================================

Diseña un Scheduler independiente.

Debe manejar.

Subastas.

Limpieza.

Backups.

Expiraciones.

Heartbeats.

Rotaciones.

Reportes.

=====================================================================
WEBHOOKS
=====================================================================

Diseña un Webhook Engine.

Reintentos.

Firmas.

Seguridad.

Logs.

Versionado.

Idempotencia.

=====================================================================
HEARTBEAT
=====================================================================

Diseña un Heartbeat Service.

Los plugins deberán reportar información periódicamente.

Versión.

TPS.

Jugadores.

Estado.

Memoria.

Uptime.

Último contacto.

=====================================================================
API GATEWAY
=====================================================================

Analiza si conviene implementar un API Gateway.

Qué responsabilidades tendría.

Rate Limits.

Autenticación.

Logging.

Routing.

=====================================================================
OBSERVABILIDAD
=====================================================================

Diseña completamente.

Logging.

Metrics.

Tracing.

Prometheus.

Grafana.

Health Checks.

Alertas.

Dashboards.

=====================================================================
MULTITENANCY
=====================================================================

La plataforma será multiempresa.

Diseña completamente el aislamiento entre organizaciones.

Permisos.

Recursos.

Facturación.

Configuraciones.

=====================================================================
ESCALABILIDAD
=====================================================================

Diseña la plataforma para soportar como mínimo:

100.000 servidores.

20 millones de usuarios.

500 millones de votos.

Miles de WebSockets.

Miles de plugins conectados.

Explica cómo escalar cada componente.

=====================================================================
DIAGRAMAS
=====================================================================

Quiero diagramas profesionales.

Context Diagram

Container Diagram

Component Diagram

Deployment Diagram

Sequence Diagrams

Mermaid

C4 Model

=====================================================================
ARCHITECTURE DECISION RECORDS
=====================================================================

Genera los primeros ADR del proyecto.

Ejemplos.

ADR-001

Arquitectura elegida.

ADR-002

Lenguaje.

ADR-003

Base de datos.

ADR-004

Mensajería.

ADR-005

Cache.

ADR-006

Monolito Modular.

Justifica cada decisión.

=====================================================================
RIESGOS
=====================================================================

Analiza riesgos arquitectónicos.

Cuellos de botella.

Single Points of Failure.

Dependencias.

Escalabilidad.

Complejidad.

Mantenibilidad.

Propón mitigaciones.

=====================================================================
ENTREGA
=====================================================================

NO escribir código.

NO implementar.

NO crear clases.

NO crear endpoints.

Quiero exclusivamente un Software Architecture Blueprint profesional.

Debe sentirse como un documento interno de una empresa tecnológica de primer nivel.

Incluye tablas, diagramas Mermaid, C4, comparativas, ADR, ejemplos y explicaciones técnicas detalladas.

Si encuentras una arquitectura mejor que la inicialmente planteada, justifica el cambio y adopta la opción superior.

---

## Notas del CTO sobre este requisito

### Valoración general

Es el documento más consecuente de los tres. También el que contiene los
errores más caros si se ejecuta literalmente.

Aciertos del prompt:

- Pide **analizar** si el monolito modular es correcto, en vez de imponerlo
- Marca explícitamente Event Sourcing y CQRS como "analiza si aporta valor"
- Exige justificación por decisión

### Problema 1 — Los objetivos de escala no son realistas y encarecen todo

| Objetivo pedido | Comentario |
|---|---|
| 100.000 servidores | Implica indexar prácticamente todos los servidores de Minecraft del mundo simultáneamente |
| 20.000.000 usuarios registrados | Escala de red social consolidada, no de directorio de nicho |
| 500.000.000 votos | ~1.400.000 votos/día sostenidos durante un año |
| Miles de WebSockets | Alcanzable y razonable |
| Miles de plugins conectados | Alcanzable y razonable |

**Por qué importa:** estos números empujan hacia decisiones caras y prematuras
—sharding, multi-región, Kafka, almacenes especializados, equipo de SRE— que
no aportan nada en los primeros años y multiplican el coste operativo y la
complejidad de desarrollo.

**Mitigación propuesta:** diseñar para **10× el objetivo realista de 24 meses**,
y garantizar que las fronteras de módulos permitan escalar componentes
individualmente cuando los datos lo justifiquen. Un PostgreSQL bien
modelado, con read models para el ranking, absorbe varios órdenes de magnitud
más de lo que este proyecto verá en sus primeros años.

### Problema 2 — 24 patrones listados no son un diseño

Un patrón aplicado donde no hace falta **es deuda técnica**, no calidad.
Postura del CTO por patrón polémico:

| Patrón | Veredicto | Motivo |
|---|---|---|
| Event Sourcing | **No**, salvo auditoría financiera | Complejidad enorme, consultas difíciles, migraciones dolorosas. El valor de auditoría se consigue con un log append-only. |
| CQRS completo (dos bases) | **No** | Consistencia eventual sin necesidad |
| CQRS ligero (read models) | **Sí** | El ranking es un read model natural |
| Saga | **Sí**, solo en pagos y subastas | Procesos multi-paso reversibles |
| Outbox / Inbox | **Sí** | Entrega fiable de votos = requisito de negocio |
| Idempotencia | **Sí**, obligatorio | Reintentos de plugin y webhooks |
| Optimistic Locking | **Sí** | Por defecto |
| Pessimistic Locking | **Solo en pujas de subasta** | Contención real |
| Circuit Breaker / Retry | **Sí** | Llamadas salientes a servidores de terceros |
| Mediator | **Opcional** | Útil dentro del módulo; no imponerlo globalmente |
| Specification | **Solo en filtros de búsqueda** | Sobrecarga innecesaria fuera de ahí |

### Problema 3 — 32 módulos no es modularidad, es fragmentación

Varios elementos de la lista no son módulos:

| Elemento | Qué es en realidad |
|---|---|
| Core | Antipatrón: "Core" atrae responsabilidades hasta convertirse en el monolito que se quería evitar |
| Users / Identity / Organizations | Un único contexto de Identidad y Acceso |
| Public API / Internal API / Plugin API / SDK | Capas de presentación del mismo dominio, no módulos |
| Health / Status / Monitoring | Una sola preocupación transversal |
| Settings / Feature Flags / Configuration | Un único módulo de configuración |
| Dashboard / Admin | Interfaces de usuario, no módulos de dominio |
| Jobs / Scheduler | Infraestructura compartida |

Objetivo razonable: **8-12 módulos de dominio reales** más infraestructura
transversal. Cada módulo cruzado añade coste de coordinación permanente.

### Problema 4 — "Plugin System" interno (módulos instalables/deshabilitables)

Un sistema de módulos cargables dinámicamente dentro de la aplicación implica
carga dinámica, aislamiento, versionado de contratos internos y un modelo de
permisos. Es una plataforma dentro de la plataforma.

**Alternativa recomendada:** feature flags + fronteras de módulo limpias.
Consigue el 90% del beneficio (activar/desactivar funciones, evolución
independiente) con el 5% del coste.

### Problema 5 — Numeración de ADR inconsistente

El prompt propone ADR-001 = "Arquitectura elegida" y ADR-006 = "Monolito
Modular": son la misma decisión duplicada. Además choca con la numeración ya
iniciada por el CTO. Se unificará una sola secuencia al ejecutar.

### Conclusión preliminar del CTO sobre la arquitectura general

La preferencia del usuario (**Monolito Modular preparado para migrar**) es
**correcta** y se mantiene. Descartados: microservicios (coste operativo sin
beneficio con un equipo pequeño), serverless (arranques en frío y conexiones
persistentes de plugins encajan mal), monolito clásico (sin fronteras internas
degenera).
