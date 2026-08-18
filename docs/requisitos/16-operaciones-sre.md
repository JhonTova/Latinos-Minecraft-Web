# Requisito 16 — Platform Operations / SRE / Reliability Program

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** operaciones y fiabilidad — sin infraestructura ni scripts

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 16

PLATFORM OPERATIONS

SITE RELIABILITY ENGINEERING

PLATFORM RELIABILITY PROGRAM

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define cómo operar la plataforma en producción durante los próximos diez años.

NO escribir código.

NO configurar servidores.

NO crear pipelines.

NO implementar monitoreo.

Tu misión consiste en diseñar una estrategia completa de operaciones, confiabilidad y continuidad del negocio.

Actúa como un equipo formado por:

Principal SRE. Principal DevOps Engineer. Cloud Architect.
Platform Engineer. Incident Commander. Security Operations Engineer.

================================================================================
FILOSOFÍA
================================================================================

La plataforma deberá diseñarse para fallar de forma controlada.

Todo componente podrá fallar.

Toda dependencia podrá caerse.

Toda integración podrá responder lentamente.

La plataforma deberá seguir funcionando.

================================================================================
SRE PRINCIPLES
================================================================================

Define principios oficiales.

Reliability First. Automation First. Everything Observable.
Everything Measurable. Everything Recoverable. Small Deployments.
Blameless Culture. Continuous Improvement.

================================================================================
SLI
================================================================================

Define Service Level Indicators.

Disponibilidad. Latencia. Errores. Tiempo de respuesta. Éxito de votos.
Éxito de pagos. Tiempo de sincronización del plugin.
Entrega de webhooks. Tiempo de procesamiento de colas.

================================================================================
SLO
================================================================================

Diseña objetivos.

API. Dashboard. Vote Engine. Auction Engine. Automation Engine.
Marketplace. Plugin Gateway.

================================================================================
SLA
================================================================================

Diseña niveles de servicio.

Community. Pro. Business. Enterprise.

Incluye:

Disponibilidad. Soporte. Tiempo de respuesta. Tiempo de recuperación.

================================================================================
ERROR BUDGET
================================================================================

Diseña un sistema completo.

Consumo. Alertas. Acciones. Congelamiento de releases.

================================================================================
OBSERVABILIDAD
================================================================================

Diseña una plataforma basada en OpenTelemetry.

Logs. Tracing. Metrics. Profiles. Events. Correlation IDs.

================================================================================
MONITORING
================================================================================

Diseña dashboards.

Sistema. API. Plugins. Pagos. Subastas. Votes. Infraestructura. Seguridad.

================================================================================
ALERTING
================================================================================

Diseña alertas inteligentes.

No generar ruido. Agrupar. Correlacionar. Escalar. Silenciar.

================================================================================
ON-CALL
================================================================================

Diseña.

Rotaciones. Escalamiento. Runbooks. Incidentes críticos.

================================================================================
INCIDENT MANAGEMENT
================================================================================

Diseña un proceso profesional.

Detección. Clasificación. Asignación. Mitigación. Comunicación.
Resolución. Postmortem. Seguimiento.

================================================================================
RUNBOOKS
================================================================================

Define runbooks para.

API caída. Redis caído. RabbitMQ detenido. Plugin Gateway sin conexión.
Webhook congestionado. Pago bloqueado. Subasta inconsistente.

================================================================================
DISASTER RECOVERY
================================================================================

Diseña.

RTO. RPO. Backups. Snapshots. Restauración. Pruebas periódicas.

================================================================================
BUSINESS CONTINUITY
================================================================================

Analiza.

Pérdida de proveedor cloud. Caída regional. Caída de DNS. Fallo de CDN.
Compromiso de credenciales.

================================================================================
CAPACITY PLANNING
================================================================================

Diseña planificación para.

Usuarios. Servidores. Votos. Pagos. Eventos. Almacenamiento.

================================================================================
AUTOSCALING
================================================================================

Analiza.

Horizontal. Vertical. Basado en CPU. Basado en colas. Basado en latencia.
Basado en eventos.

================================================================================
DEPLOYMENTS
================================================================================

Diseña estrategias.

Rolling. Blue/Green. Canary. Feature Flags. Rollback automático.

================================================================================
CHAOS ENGINEERING
================================================================================

Diseña experimentos.

API lenta. Base de datos caída. Cola saturada. Webhook caído.
Plugin desconectado.

================================================================================
PLATFORM STATUS
================================================================================

Diseña una Status Platform pública.

Historial. Incidentes. Mantenimiento. Suscripciones.

================================================================================
OPERATIONS CONTROL CENTER
================================================================================

Diseña un centro de operaciones.

Mapa de servicios. Estado. Alertas. Colas. Workers. Plugins. Pagos.
Subastas. Votos. Webhooks.

================================================================================
SECURITY OPERATIONS
================================================================================

Diseña.

SOC. SIEM. Detección. Correlación. Threat Intelligence.

================================================================================
COST MANAGEMENT
================================================================================

Diseña métricas.

Coste por voto. Coste por servidor. Coste por API.
Coste por organización. Optimización.

================================================================================
FINOPS
================================================================================

Diseña prácticas.

Presupuestos. Forecast. Alertas. Optimización.

================================================================================
COMPLIANCE OPERATIONS
================================================================================

Diseña.

Auditorías. Retención. Accesos. Logs. Revisiones.

================================================================================
POSTMORTEMS
================================================================================

Todo incidente deberá generar.

Resumen. Impacto. Causa raíz. Línea temporal. Acciones. Seguimiento.

================================================================================
DOCUMENTACIÓN OPERATIVA
================================================================================

Toda operación deberá documentarse.

Runbooks. Playbooks. Diagramas. Checklist.

================================================================================
KPIs
================================================================================

Define.

MTTR. MTBF. Availability. Latency. Error Rate. Deployment Frequency.
Rollback Rate. Incident Rate.

================================================================================
ROADMAP
================================================================================

Diseña la evolución operacional.

Fase inicial. Escala media. Escala masiva.

================================================================================
OUTPUT
================================================================================

No implementar infraestructura.

No escribir scripts.

Quiero un documento equivalente al manual de operaciones de una plataforma SaaS de clase mundial.

Incluye:

SLO. SLI. SLA. Runbooks. Diagramas Mermaid. Planes de contingencia.
Matrices de riesgo. Roadmap operacional. Buenas prácticas. Checklist.

Y cualquier recomendación para mantener una disponibilidad cercana al 99.99%.

---

## Notas del CTO sobre este requisito

### Objeción 1 — El 99,99% no es alcanzable con la estructura actual

El documento cierra pidiendo "disponibilidad cercana al 99.99%". Traducción a
tiempo real de caída permitido **al año**:

| Objetivo | Caída máxima anual | Caída máxima mensual |
|---|---|---|
| 99,99% | **52 minutos** | 4,3 minutos |
| 99,9% | 8,7 horas | 43 minutos |
| 99,5% | 43,8 horas | 3,6 horas |
| 99% | 3,65 días | 7,2 horas |

Esos 52 minutos anuales incluyen **todo**: despliegues, migraciones de base de
datos, caídas del proveedor cloud, incidentes de DNS y errores humanos.

Requisitos reales para sostener 99,99%: redundancia en cada capa, despliegue
multi-región, base de datos con réplica y conmutación automática, y **personal
de guardia 24/7**. No es cuestión de esfuerzo: es cuestión de estructura.

**Realidad operativa del proyecto:** un VPS, una base de datos, una persona
que además duerme. Un solo incidente nocturno de 8 horas consume el
presupuesto anual completo de 99,9%.

**Recomendación:**

| Fase | Objetivo interno (SLO) | Compromiso público (SLA) |
|---|---|---|
| Inicio (VPS único, 1 persona) | **99,5%** | **Ninguno** |
| Con redundancia básica | 99,9% | Ninguno |
| Con equipo y guardias | 99,9% | Solo en plan Enterprise |

Coherente con lo ya señalado en el Requisito 05: un SLA es un contrato con
penalizaciones, no un objetivo. No se publica hasta poder sostenerlo.

### Objeción 2 — "Rotación de guardias" con una persona no es una rotación

Con un equipo de uno, el on-call es 24/7/365 de forma permanente. Es
insostenible y constituye un **riesgo real de continuidad del negocio**: si
esa persona enferma, se va de vacaciones o se quema, la plataforma queda sin
operador. El factor bus es 1.

**Reencuadre correcto para un operador único:**

> El objetivo no es *recuperarse rápido*, sino que los fallos sean
> **sobrevivibles sin intervención humana inmediata**.

Medidas baratas y de alto impacto:

| Medida | Efecto |
|---|---|
| Reinicio automático ante fallo de health check | Resuelve la mayoría de incidentes sin nadie despierto |
| **Degradación elegante** | Si el ranking no se puede recalcular, servir el último válido en vez de devolver error |
| **Arquitectura de colas** (ya decidida: Outbox) | Si el gateway de plugins cae, los votos se encolan y se entregan después. **Ningún voto se pierde aunque nadie intervenga.** |
| Status page pública | Reduce drásticamente los tickets de soporte durante un incidente |
| Runbooks escritos | Un runbook es un mensaje a tu yo futuro a las 3 de la mañana |

La decisión del Outbox del Requisito 13 es, en realidad, **una decisión
operativa**: convierte una caída en un retraso.

### Elemento infravalorado y de alto valor: coste por voto y por servidor

La sección COST MANAGEMENT pide "coste por voto, por servidor, por API, por
organización". Es de lo más valioso del documento y suele ignorarse.

Combinado con el cálculo del Requisito 05 (los heartbeats generan ~100× más
tráfico que los votos), la conclusión es directa:

> **Cada servidor del plan gratuito genera coste continuo aunque no reciba un
> solo voto.** Sin conocer el coste por servidor, no se puede fijar el precio
> del plan de pago ni los límites del plan gratuito.

Es la métrica que determina si el plan gratuito es una herramienta de
crecimiento o una vía de quiebra.

### Elementos correctos que se mantienen

| Elemento | Valoración |
|---|---|
| Diseñar para fallar de forma controlada | ✅ Filosofía correcta |
| Blameless culture + postmortems | ✅ Correcto |
| Runbooks | ✅ **Alto valor con equipo pequeño**, no bajo |
| Status page pública | ✅ Barata y reduce carga de soporte |
| Error budget | ✅ Buen concepto, aplicable incluso en solitario |
| Despliegues pequeños y frecuentes | ✅ Correcto: reduce el riesgo por despliegue |
| RTO / RPO + **pruebas periódicas de restauración** | ✅ Crítico. Un backup que nunca se ha restaurado no es un backup. |

### Elementos aplazados

| Elemento | Motivo |
|---|---|
| Chaos Engineering | Sin redundancia, romper cosas a propósito solo produce caídas |
| SOC / SIEM / Threat Intelligence | Requiere equipo de seguridad dedicado |
| Operations Control Center | Un dashboard basta al inicio |
| Autoscaling | Sin variabilidad de carga real que justifique el coste |
| Blue/Green y Canary | Rolling + rollback rápido es suficiente al principio |

### Inconsistencia detectada

Los runbooks solicitados incluyen **"RabbitMQ detenido"** y **"Redis caído"**,
pero en el Requisito 08 se decidió no usar RabbitMQ (PostgreSQL como cola) y
usar Caffeine en lugar de Redis/Valkey al inicio.

Es un síntoma de que los documentos se están escribiendo de forma
independiente y empiezan a divergir entre sí. Al ejecutar se unificará contra
las decisiones vigentes.
