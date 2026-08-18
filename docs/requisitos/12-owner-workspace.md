# Requisito 12 — Owner Experience Platform / Workspace Operating System

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** experiencia del propietario — sin pantallas ni código

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 12

OWNER EXPERIENCE PLATFORM

WORKSPACE OPERATING SYSTEM

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define la experiencia completa del propietario de un servidor.

NO diseñar componentes.

NO escribir código.

NO crear páginas.

NO implementar frontend.

Tu misión consiste en diseñar el Workspace más completo para administrar servidores de Minecraft.

No estamos construyendo un Dashboard.

Estamos construyendo un Operating System.

Debe sentirse como una mezcla entre:

Stripe Dashboard

GitHub

Vercel

Cloudflare

Linear

Notion

Supabase

Firebase Console

================================================================================
FILOSOFÍA
================================================================================

El propietario nunca debería preguntarse:

"¿Dónde está esa opción?"

Toda acción debe encontrarse en menos de tres clics.

Toda información importante debe estar visible.

Todo el sistema debe ser modular.

================================================================================
WORKSPACE
================================================================================

Diseña completamente.

Workspace. Organizations. Projects. Servers. Members. Permissions.
Contexts. Switchers.

================================================================================
MULTI ORGANIZACIÓN
================================================================================

Un usuario podrá administrar:

1 servidor

10 servidores

500 servidores

Todo debe escalar.

Diseña completamente.

================================================================================
MULTI SERVER
================================================================================

Un Workspace podrá contener múltiples servidores.

Cada servidor tendrá:

Analytics. Votes. Economy. Plugin. Billing. Integrations. Marketplace.
Security. Audit. Settings.

================================================================================
HOME
================================================================================

Diseña una Home inteligente.

Widgets. Actividad. Alertas. Estado. Noticias. KPIs. Quick Actions.

================================================================================
PERSONALIZACIÓN
================================================================================

El usuario podrá:

Mover widgets. Guardar layouts. Crear dashboards. Compartir dashboards.
Crear vistas. Crear favoritos.

================================================================================
NAVIGATION
================================================================================

Diseña.

Sidebar. Topbar. Context Switcher. Command Palette. Breadcrumbs.
Global Search. Recent. Favorites.

================================================================================
GLOBAL SEARCH
================================================================================

La búsqueda deberá encontrar.

Servidor. Jugador. Pago. Factura. API Key. Webhook. Evento. Usuario.
Documento.

================================================================================
COMMAND PALETTE
================================================================================

Diseña una Command Palette estilo Linear.

Crear servidor. Ir a Analytics. Crear API. Buscar usuario. Crear subasta.
Abrir configuración.

Todo mediante teclado.

================================================================================
SERVER DASHBOARD
================================================================================

Diseña completamente.

Estado. Online. TPS. Jugadores. Votos. Ingresos. Errores. Alertas. Plugin.
Integraciones.

================================================================================
ANALYTICS
================================================================================

Diseña dashboards profesionales.

Tiempo real. Históricos. Comparativas. Embudos. Conversiones. Retention.
LTV. Revenue. Votes.

================================================================================
PLUGIN CENTER
================================================================================

Diseña un centro completo.

Estado. Versión. Actualizaciones. Logs. Config. Permisos. Diagnóstico.
Health.

================================================================================
API CENTER
================================================================================

Diseña.

API Keys. Scopes. Rotación. Logs. Revocación. Webhooks. SDK.

================================================================================
MARKETPLACE
================================================================================

Diseña un Marketplace.

Plugins. Integraciones. Temas. Plantillas. Automatizaciones.

================================================================================
INTEGRATIONS
================================================================================

Diseña.

Discord. GitHub. Google. Stripe. Mercado Pago. PayPal. Cloudflare.
Webhook. Slack.

================================================================================
AUTOMATIONS
================================================================================

Diseña.

Triggers. Conditions. Actions. Workflows. Cron. Notifications.

================================================================================
NOTIFICATION CENTER
================================================================================

Diseña.

Alertas. Pagos. Fraude. Plugin Offline. Servidor Offline. Webhook Failed.
Auction.

================================================================================
TEAM MANAGEMENT
================================================================================

Diseña.

Invitaciones. Roles. Permisos. Equipos. Accesos. Auditoría.

================================================================================
AUDIT CENTER
================================================================================

Todo deberá registrarse.

Cambios. Permisos. Facturación. API. Subastas. Configuraciones.

================================================================================
SECURITY CENTER
================================================================================

Diseña.

Sesiones. Dispositivos. MFA. Passkeys. Activity. Risk Score. API Keys.

================================================================================
BILLING CENTER
================================================================================

Diseña.

Planes. Suscripciones. Facturas. Pagos. Métodos. Cupones. Wallet. Créditos.

================================================================================
AUCTION CENTER
================================================================================

Diseña la experiencia completa.

Calendario. Inscripción. Ranking. Tiempo real. Pujas. Historial. Ganadores.
Pagos. Sanciones.

================================================================================
SUPPORT CENTER
================================================================================

Diseña.

Tickets. Estado. Historial. Prioridades. Logs. Adjuntos.

================================================================================
STATUS CENTER
================================================================================

Diseña una página tipo Status Page.

Servicios. Incidentes. Mantenimiento. Historial.

================================================================================
FILE CENTER
================================================================================

Diseña.

Uploads. Assets. Backups. Logs. Exports.

================================================================================
REPORTS
================================================================================

Diseña.

PDF. CSV. Excel. API. Programados.

================================================================================
FEATURE FLAGS
================================================================================

Diseña.

Activación. Rollback. Beta. Canary.

================================================================================
ORGANIZATION SETTINGS
================================================================================

General. Branding. Facturación. Seguridad. Miembros. API.

================================================================================
USER SETTINGS
================================================================================

Idioma. Tema. Accesibilidad. Notificaciones. Sesiones. Dispositivos.

================================================================================
OBSERVABILIDAD
================================================================================

Todo Workspace deberá tener.

Health. Logs. Tracing. Metrics. Usage.

================================================================================
EXTENSIBILIDAD
================================================================================

Diseña el Workspace para aceptar módulos futuros sin modificar la navegación principal.

Utiliza arquitectura basada en módulos y registros dinámicos.

================================================================================
OUTPUT
================================================================================

No crear pantallas.

No implementar frontend.

No escribir código.

Genera una especificación completa del Workspace.

Incluye:

Mapa de navegación. Diagramas Mermaid. Jerarquía. Árbol de información.
User Flows. Estados. Permisos. Buenas prácticas. Roadmap de evolución.

Y cualquier recomendación para convertir esta plataforma en la mejor herramienta de administración de servidores de Minecraft del mercado.

---

## Notas del CTO sobre este requisito

### Contradicción central del documento

| Filosofía declarada | Contenido solicitado |
|---|---|
| "El propietario nunca debería preguntarse: *¿dónde está esa opción?*" | **20 secciones de primer nivel** |
| "Toda acción en menos de tres clics" | Plugin Center, API Center, Marketplace, Integrations, Automations, Notification Center, Team Management, Audit Center, Security Center, Billing Center, Auction Center, Support Center, Status Center, File Center, Reports, Feature Flags, Org Settings, User Settings, Analytics, Home |

Veinte secciones de primer nivel **son** el problema de "¿dónde está esa
opción?". Las referencias que cita el propio documento no funcionan así: los
paneles de Stripe, Vercel y Linear tienen entre 6 y 8 entradas de navegación
principal. La profundidad vive **dentro** de cada sección, no en la barra
lateral.

**Navegación propuesta (7 entradas):**

| Entrada | Qué contiene |
|---|---|
| **Inicio** | Resumen, alertas, acciones rápidas |
| **Servidores** | Estado, plugin, votos, recompensas, ajustes del servidor |
| **Analíticas** | Gráficos, comparativas, exportaciones e informes |
| **Integraciones** | Discord, webhooks, API Keys, SDK |
| **Equipo** | Miembros, roles, invitaciones |
| **Facturación** | Planes, facturas, métodos de pago |
| **Ajustes** | Organización, seguridad, sesiones, auditoría, preferencias |

Audit Center → dentro de Ajustes › Seguridad. Status Page → página **pública**,
fuera del panel. File Center y Reports → dentro de Analíticas. Support →
enlace, no sección.

### El error de diseño más costoso: optimizar para el usuario que no tienes

El documento diseña para quien administra **500 servidores**. La realidad del
mercado es la contraria: la inmensa mayoría de servidores de Minecraft son
pequeños y pertenecen a una sola persona.

**El día 1, el usuario típico tiene: 1 servidor, 0 votos, 0 miembros de equipo,
0 facturas y 5 minutos de paciencia.**

Un panel con 20 secciones vacías no transmite potencia; transmite abandono y
complejidad. Esa primera pantalla decide la activación.

**Recomendación:** diseñar la Home primero para el estado vacío, con un único
objetivo visible: *conecta tu servidor*. La densidad de información debe
**crecer con los datos del usuario**, no estar presente desde el principio.

### Confusión entre herramienta interna y funcionalidad de cliente

| Solicitado | Problema | Corrección |
|---|---|---|
| **Feature Flags** con canary y rollback en el panel del cliente | Los feature flags son una herramienta **interna** de despliegue. Exponer "canary" y "rollback" al dueño de un servidor no significa nada para él. | Mantener internos. Como mucho, un interruptor "participar en la beta". |
| **Observabilidad** por workspace: Health, Logs, **Tracing**, Metrics | El tracing distribuido es diagnóstico interno de la plataforma. Ningún cliente quiere ver trazas. | Exponer solo: estado del plugin, últimos eventos, uso frente al límite del plan |
| **Marketplace** de plugins, temas, plantillas y automatizaciones | Producto completo e independiente | Fuera de v1 (ya acordado) |
| **Automations** (triggers, conditions, actions, workflows, cron) | Es construir un Zapier dentro del panel | Fuera de v1. Sustituto barato: webhooks + notificaciones de Discord cubren el 90% de los casos reales |

### Elementos correctos que se mantienen

| Elemento | Valoración |
|---|---|
| Context switcher entre organizaciones | ✅ Necesario desde el día 1 por la decisión de multi-organización |
| Audit Center | ✅ Correcto — obligatorio con equipos y dinero |
| Security Center (sesiones, dispositivos, MFA) | ✅ Correcto |
| Status Page pública | ✅ Correcta, y además genera confianza en clientes potenciales |
| Notificaciones de plugin/servidor caído | ✅ **Alto valor real.** Avisar al dueño de que su servidor está caído antes de que se entere él es una función por la que se paga. |
| Extensibilidad por registro de módulos en la navegación | ✅ Correcto y barato si se diseña desde el inicio |

### Recomendación de mayor impacto competitivo

De todo el documento, la funcionalidad con mejor relación valor/esfuerzo es la
**alerta proactiva**: "tu servidor lleva 12 minutos caído" o "tu plugin dejó de
responder" enviada por Discord.

Es barata de construir (ya se tienen heartbeats y ping directo), tiene utilidad
inmediata, se nota todos los días, y da una razón concreta para mantener el
plugin instalado incluso a servidores que reciben pocos votos.
