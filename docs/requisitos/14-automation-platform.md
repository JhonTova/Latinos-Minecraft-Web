# Requisito 14 — Automation Platform / Rules Engine / Workflow Engine

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** motor de automatización — sin código
> **Valoración del CTO:** ⚠️ Contiene el elemento de **mayor valor extraíble** y el **riesgo de seguridad más grave** de los 14 documentos

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 14

AUTOMATION PLATFORM

RULES ENGINE

WORKFLOW ENGINE

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define el sistema de automatización completo de la plataforma.

NO escribir código.

NO implementar APIs.

NO implementar workflows.

NO crear interfaces.

Tu misión consiste en diseñar una plataforma de automatización de nivel empresarial.

Debe permitir automatizar cualquier evento de la plataforma sin modificar el código fuente.

Piensa como un Principal Workflow Engineer de:

Zapier. GitHub Actions. Cloudflare. Stripe. Slack. Discord. Notion.

================================================================================
FILOSOFÍA
================================================================================

Toda funcionalidad repetitiva debe convertirse en una automatización.

Toda automatización debe ser configurable.

Toda regla debe ser auditable.

Toda ejecución debe poder reproducirse.

Toda automatización debe ser segura.

================================================================================
CONCEPTOS
================================================================================

Diseña completamente.

Automation. Workflow. Rule. Trigger. Condition. Action. Execution. Context.
Variables. Secrets. Schedules. Templates. History.

================================================================================
ARQUITECTURA
================================================================================

Diseña un motor desacoplado.

Event Driven. Message Queue. Worker Pool. Scheduler. Execution Engine.
Retry Engine. History Store. Dead Letter Queue.

================================================================================
TRIGGERS
================================================================================

Diseña todos los disparadores.

Vote Received. Vote Rejected. Vote Confirmed. Server Registered.
Plugin Online. Plugin Offline. Auction Started. Auction Ended.
Payment Completed. Payment Failed. Subscription Renewed.
Subscription Expired. Webhook Failed. User Registered.
Organization Created. Member Invited. Trust Score Changed.
Risk Score Updated. API Key Revoked. Server Offline. Server Online.
Custom Events.

================================================================================
CONDITIONS
================================================================================

Diseña un lenguaje de condiciones.

Equals. Not Equals. Contains. Starts With. Ends With. Regex.
Greater Than. Less Than. Between. In. Not In. Exists. Date. Time.
Timezone. Geo. Risk Score. Trust Score. Variables.

================================================================================
VARIABLES
================================================================================

Diseña variables dinámicas.

Jugador. Servidor. Organización. Usuario. Pago. Factura. Subasta. Plugin.
API. Webhook. Fecha. Hora. Contexto.

================================================================================
ACTIONS
================================================================================

Diseña acciones reutilizables.

Enviar Webhook. Enviar Discord. Enviar Email. Ejecutar comando Minecraft.
Crear Ticket. Crear Factura. Agregar Crédito. Bloquear Cuenta.
Suspender Servidor. Crear Log. Actualizar Base de Datos.
Enviar Notificación. Actualizar Ranking. Actualizar Estadísticas.
Invocar API. Ejecutar Workflow.

================================================================================
WORKFLOW ENGINE
================================================================================

Diseña.

Secuencia. Paralelismo. Delay. Wait. Timeout. Retries. Rollback.
Compensation.

================================================================================
RULE ENGINE
================================================================================

Diseña un lenguaje declarativo.

IF. THEN. ELSE. ELSE IF. AND. OR. NOT. Switch. Priority. Weight.

================================================================================
SCHEDULER
================================================================================

Diseña.

Cron. Interval. Calendar. Timezone. Business Hours. Delayed Jobs.

================================================================================
WORKFLOW TEMPLATES
================================================================================

Diseña una biblioteca.

Recompensas por votos. Bonificación semanal. Recordatorios.
Expiración de API Keys. Renovación. Backup. Sincronización. Moderación.
Subastas.

================================================================================
WORKFLOW BUILDER
================================================================================

Diseña un editor visual.

Drag & Drop. Nodos. Conexiones. Validación. Versionado. Simulación.

================================================================================
SIMULADOR
================================================================================

Diseña un simulador.

Entradas. Variables. Resultados. Errores. Logs.

================================================================================
EXECUTION HISTORY
================================================================================

Todo workflow deberá almacenar.

Inicio. Fin. Estado. Errores. Variables. Eventos. Logs. Duración.

================================================================================
VERSIONADO
================================================================================

Diseña.

Versiones. Rollback. Draft. Published. Deprecated.

================================================================================
PERMISOS
================================================================================

Diseña permisos granulares.

Crear. Editar. Ejecutar. Eliminar. Publicar. Clonar. Exportar.

================================================================================
MARKETPLACE
================================================================================

Diseña una tienda de automatizaciones.

Templates. Oficiales. Comunidad. Enterprise.

================================================================================
SDK
================================================================================

Diseña una API para crear nuevas acciones.

Plugins. Integraciones. Conectores.

================================================================================
WEBHOOKS
================================================================================

Diseña.

Entrantes. Salientes. Firmas. Reintentos. Replay Protection.

================================================================================
OBSERVABILIDAD
================================================================================

Toda automatización deberá generar.

Logs. Tracing. Metrics. Timeline.

================================================================================
DEBUGGING
================================================================================

Diseña herramientas.

Step By Step. Replay. Breakpoint lógico. Variables. Estado.

================================================================================
ESCALABILIDAD
================================================================================

Diseña para soportar.

Millones de ejecuciones diarias. Miles de workflows simultáneos.

================================================================================
FAILURE MODES
================================================================================

Analiza.

Nodo fallido. Timeout. Webhook caído. API lenta. Plugin Offline.
Condición inválida.

================================================================================
SECURITY
================================================================================

Analiza.

Sandbox. Secrets. Isolation. Rate Limits. Loops infinitos. Recursión.
Abuso.

================================================================================
DIAGRAMAS
================================================================================

Genera.

Mermaid. Execution Flow. Workflow Graph. State Machine.

================================================================================
OUTPUT
================================================================================

No escribir código.

No implementar workflows.

Quiero un documento que defina una plataforma de automatización comparable a Zapier o GitHub Actions, pero especializada en la gestión de servidores de Minecraft.

Incluye:

Arquitectura. Diagramas. DSL del motor de reglas. Casos de uso. Plantillas.
Buenas prácticas. Roadmap evolutivo. Checklist.

Y cualquier mejora que haga el motor más flexible, seguro y escalable.

---

## 🔴 HALLAZGO DE SEGURIDAD CRÍTICO

### Inyección de comandos en servidores de clientes

La combinación de tres elementos de este documento produce el riesgo más grave
detectado en los catorce requisitos:

1. Acción **"Ejecutar comando Minecraft"**
2. **Variables dinámicas** interpoladas por el usuario (nombre de jugador,
   servidor, organización…)
3. **Reglas escritas por el cliente** en un editor visual

El resultado es un sistema de **ejecución remota de comandos en servidores de
terceros, dirigido por plantillas escritas por usuarios**.

#### Escenario de explotación

Una plantilla típica de recompensa sería:

```
give {player} diamond 5
```

Si `{player}` se interpola sin escapar y un jugador puede controlar ese valor:

```
Nombre del jugador: Steve
Comando ejecutado:  give Steve diamond 5          ✅

Nombre del jugador: Steve; op Steve
Comando ejecutado:  give Steve; op Steve diamond 5  🔴 privilegios de administrador
```

**Por qué el riesgo es real y no teórico en este proyecto:**

- Los nombres de jugador en servidores **offline-mode (pirata)** son texto
  arbitrario, sin validación de Mojang. Esta es exactamente la decisión que
  sigue abierta desde el Requisito 04.
- Aunque se restringieran los nombres, otras variables (nombre del servidor,
  nombre de organización, texto de recompensa) son texto libre del usuario.
- La víctima **no es la plataforma**: es el servidor del cliente. Un incidente
  así destruye la confianza del ecosistema entero de forma irreversible.

#### Controles obligatorios si esta función se construye

| Control | Descripción |
|---|---|
| Lista blanca de comandos | Solo comandos aprobados, nunca texto libre |
| Parámetros tipados | `{player}` como identificador validado, no como cadena concatenada |
| Sin concatenación de cadenas | El comando se construye como estructura, no como texto |
| Validación en el plugin | Rechazar en destino lo que no cumpla el contrato, sin confiar en el emisor |
| Escapado obligatorio | Cualquier variable interpolada se sanea antes de la ejecución |
| Auditoría total | Cada comando ejecutado queda registrado con su origen |

Sin estos controles, esta funcionalidad **no debe implementarse**.

---

## Notas del CTO sobre este requisito

### El elemento de mayor valor: la lista de triggers es el catálogo de eventos

La sección TRIGGERS enumera 22 eventos. Aunque no se construya ningún motor de
automatización, **esa lista debe extraerse y conservarse**: es el catálogo
oficial de eventos de dominio de la plataforma.

Alimenta directamente tres decisiones ya tomadas:

| Documento | Uso |
|---|---|
| 03 — Arquitectura | Eventos del bus interno (Event-Driven) |
| 05 — API | Catálogo de eventos públicos para webhooks |
| 13 — Motor de votos | Eventos del ciclo de vida del voto |

Es trabajo aprovechable al 100%, independientemente de la decisión sobre el
resto del documento.

### Objeción de alcance — esto es Zapier

Lo solicitado incluye: editor visual con drag & drop, DSL propio de reglas,
motor de ejecución con paralelismo y compensaciones, sandbox, versionado con
draft/published/deprecated, depurador paso a paso con breakpoints, historial
de ejecuciones, marketplace de plantillas de comunidad y SDK para conectores
de terceros.

Eso no es un módulo: **es Zapier**, una empresa con cientos de ingenieros.

#### Qué quiere automatizar realmente un dueño de servidor de Minecraft

| Necesidad | % estimado de usuarios | Solución adecuada |
|---|---|---|
| "Cuando alguien vote, dale X" | ~95% | **Un formulario de configuración de recompensas** |
| "Avísame por Discord si algo pasa" | ~40% | Notificaciones configurables |
| "Manda los eventos a mi propio sistema" | ~5% | Webhooks (ya diseñados) |
| Workflows condicionales de varios pasos con ramas | <1% | Webhooks + su propio código |

La necesidad dominante —"cuando alguien vote, dale una recompensa"— **no es un
motor de workflows**: es un formulario. Construir Zapier para resolverlo es
una desproporción de dos órdenes de magnitud.

### Recomendación de descomposición

| Elemento | Decisión | Justificación |
|---|---|---|
| **Catálogo de 22 eventos** | ✅ **Extraer y conservar** | Alimenta arquitectura, API y motor de votos |
| **Motor de reglas interno** (antifraude) | ✅ **Construir** | Ya requerido por el Requisito 13. Es interno, no lo edita el cliente. |
| **Configuración de recompensas** | ✅ **Construir** | Cubre el 95% de la necesidad real |
| **Notificaciones a Discord configurables** | ✅ **Construir** | Alto valor, bajo coste |
| **Webhooks salientes** | ✅ Ya diseñados (Requisito 05) | Cubren la automatización avanzada |
| Scheduler interno (cron de tareas propias) | ✅ Construir | Necesario para rankings, limpieza, expiraciones |
| **Editor visual de workflows** | ❌ Fuera de v1 | Producto independiente |
| **DSL propio + sandbox + depurador** | ❌ Fuera de v1 | Coste enorme, audiencia <1% |
| **Marketplace de automatizaciones** | ❌ Fuera de v1 | Ya descartado en el Requisito 02 |
| **SDK de conectores de terceros** | ❌ Fuera de v1 | Superficie de ataque + coste de mantenimiento |

### Riesgo secundario correctamente identificado por el documento

La sección SECURITY menciona **loops infinitos y recursión**. Es una
preocupación acertada: la lista de acciones incluye **"Ejecutar Workflow"**,
lo que permite recursión explícita, y un workflow A que dispara un evento
capturado por un workflow B que vuelve a disparar A produce un bucle
autoalimentado.

Si algún día se construye, son obligatorios: límite de profundidad, límite de
ejecuciones por ventana temporal, detección de ciclos en el grafo antes de
publicar, y corte automático con alerta.
