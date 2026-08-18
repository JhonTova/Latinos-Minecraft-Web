# Requisito 13 — Vote Platform Engine

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** diseño del motor de votos — sin código
> **Valoración del CTO:** 🔴 **Es el documento más importante del proyecto.** Este módulo *es* el producto.

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 13

VOTE PLATFORM ENGINE

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define el diseño completo del motor de votaciones de la plataforma.

NO escribir código.

NO implementar endpoints.

NO crear base de datos.

NO crear APIs.

NO implementar el plugin.

Tu misión consiste en diseñar el sistema de votaciones más robusto, seguro y escalable del ecosistema Minecraft.

El sistema debe ser capaz de soportar:

- Millones de votos diarios.
- Miles de servidores registrados.
- Millones de jugadores.
- Procesamiento distribuido.
- Tolerancia a fallos.
- Alta disponibilidad.
- Consistencia.
- Auditoría completa.

Piensa como un Principal Distributed Systems Engineer de:

Google. Cloudflare. Stripe. Discord. GitHub.

================================================================================
FILOSOFÍA
================================================================================

Un voto NO es únicamente un clic.

Es un evento de negocio.

Debe ser:

Verificable. Auditable. Idempotente. Escalable. Recuperable. Trazable.

Nunca podrá perderse un voto.

Nunca podrá duplicarse un voto.

Nunca podrá entregarse una recompensa dos veces.

================================================================================
DOMINIO
================================================================================

Diseña completamente el dominio.

Vote. Vote Window. Vote Site. Vote Source. Vote Identity. Vote Verification.
Vote Reward. Vote Queue. Vote Session. Vote Attempt. Vote Result.
Vote History. Vote Trust. Vote Campaign. Vote Rules.

================================================================================
ARQUITECTURA
================================================================================

Diseña una arquitectura Event-Driven.

Explica:

Eventos. Comandos. Proyecciones. Read Models. Outbox Pattern. Idempotency.
Event Replay. Compensaciones.

================================================================================
FLUJO COMPLETO
================================================================================

Diseña el flujo completo.

Jugador. Sitio de votos. API. Motor de validación. Cola. Procesamiento.
Plugin. Entrega de recompensa. Confirmación. Auditoría.

================================================================================
VENTANAS DE VOTO
================================================================================

Diseña un sistema configurable.

24 horas. 12 horas. 48 horas. Personalizado. Timezone Aware. Horario UTC.
Horario local.

Explica cómo evitar errores de cambio horario.

================================================================================
VALIDACIÓN
================================================================================

Cada voto deberá pasar por múltiples etapas.

Validación sintáctica. Validación semántica. Validación temporal.
Validación del servidor. Validación del jugador. Validación de identidad.
Validación antifraude. Validación del plugin.

================================================================================
IDEMPOTENCIA
================================================================================

Diseña completamente.

Claves. Expiración. Persistencia. Reintentos. Duplicados.

================================================================================
QUEUE
================================================================================

Diseña la cola.

Retries. DLQ. Prioridades. Reprocesamiento. Orden. Consistencia.

================================================================================
RECOMPENSAS
================================================================================

Diseña el sistema.

Entrega. Confirmación. Rollback. Compensación. Errores. Reintentos.

================================================================================
PLUGIN
================================================================================

Diseña la interacción.

Heartbeat. ACK. Retry. Offline Mode. Store & Forward. Sincronización.

================================================================================
FRAUD ENGINE
================================================================================

Diseña un motor antifraude multicapa.

IP Reputation. ASN Detection. VPN Detection. Proxy Detection.
Hosting Provider Detection. Device Fingerprint. Browser Fingerprint.
Velocity Rules. Behavior Analysis. Geo Detection. Cookie Validation.
Session Validation. User Agent Consistency. Timezone Consistency.
Historical Patterns. Machine Learning (analizar si aporta valor).
Risk Engine.

================================================================================
TRUST SCORE
================================================================================

Diseña un sistema de puntuación.

Para jugadores. Para servidores. Para organizaciones. Para cuentas.
Para plugins.

Factores positivos. Factores negativos. Penalizaciones. Bonificaciones.
Revisión manual.

================================================================================
RATE LIMITING
================================================================================

Diseña múltiples capas.

IP. Cuenta. Servidor. Plugin. API. ASN. País. Organización.

================================================================================
RISK ENGINE
================================================================================

Cada voto deberá obtener un Risk Score.

Explica.

Algoritmo. Pesos. Factores. Acciones. Bloqueo. Revisión. Aprobación.

================================================================================
REPUTACIÓN
================================================================================

Diseña un sistema de reputación.

Servidor. Jugador. Organización. Plugin.

================================================================================
RANKINGS
================================================================================

Diseña completamente.

Tiempo real. Diario. Semanal. Mensual. Histórico. Regional. Global.
Categorías.

================================================================================
MODERACIÓN
================================================================================

Diseña herramientas.

Investigación. Historial. Timeline. Eventos. Búsqueda. Bloqueos.
Apelaciones.

================================================================================
RULE ENGINE
================================================================================

Diseña un motor configurable.

IF. THEN. ELSE. Condiciones. Acciones. Expresiones. Prioridades.

================================================================================
ANALYTICS
================================================================================

Diseña métricas.

Conversión. Abandono. Fraude. Latencia. Errores. Recompensas. Actividad.

================================================================================
OBSERVABILIDAD
================================================================================

Todo deberá generar.

Logs. Tracing. Metrics. Dashboards. Alertas.

================================================================================
WEBHOOKS
================================================================================

Diseña completamente.

Entrega. Firmas. Retries. Versionado. Replay Protection.

================================================================================
PUBLIC API
================================================================================

Diseña.

Consulta. Historial. Estado. Rankings. Estadísticas.

================================================================================
SDK
================================================================================

Diseña SDKs oficiales.

Java. JavaScript. PHP. Go. Python.

================================================================================
SIMULADOR
================================================================================

Diseña un simulador.

Votos. Errores. Fraude. Carga. Pruebas.

================================================================================
ESCALABILIDAD
================================================================================

Explica cómo soportar.

100 mil servidores. 20 millones de jugadores. 500 millones de votos.

================================================================================
FAILURE MODES
================================================================================

Analiza.

Plugin Offline. API Offline. Cola caída. Base de datos lenta. Redis caído.
Webhook fallido.

================================================================================
DISASTER RECOVERY
================================================================================

Diseña.

Backups. Replay. Recovery. Compensaciones.

================================================================================
DIAGRAMAS
================================================================================

Genera.

Sequence Diagrams. Mermaid. Event Flow. State Machines. Dependency Graph.

================================================================================
ADR
================================================================================

Genera ADR para cada decisión importante.

================================================================================
OUTPUT
================================================================================

No escribir código.

No implementar APIs.

No crear base de datos.

Quiero un documento equivalente al diseño de un sistema distribuido de clase mundial.

Debe incluir:

Arquitectura. Eventos. Diagramas. Matriz de riesgos. Casos límite.
Escenarios de fallo. Escenarios de recuperación. Checklist.
Roadmap evolutivo.

Y cualquier mejora que convierta este sistema en la referencia del sector.

---

## Notas del CTO sobre este requisito

### Valoración

Es el documento **más importante de los trece**. Todo lo demás son accesorios
alrededor de este motor. El nivel de exigencia planteado es adecuado: aquí
sí corresponde el trabajo de diseño más profundo y la Definition of Done
completa sin excepciones.

Los modos de fallo solicitados son los correctos y están bien elegidos.

### Hallazgo estratégico 1 — Votifier: compatibilidad instantánea con todo el ecosistema

El documento incluye "Vote Site" y "Vote Source" en el dominio, pero no
menciona **Votifier**, el protocolo de facto del ecosistema Minecraft para
que una lista de servidores notifique un voto al servidor.

Realidad del mercado: **la mayoría de servidores de Minecraft ya tienen
instalado NuVotifier** (o equivalente), porque es lo que exigen las listas de
votos existentes. Es un protocolo abierto y conocido (v1 basado en RSA, v2
basado en token/HMAC).

**Consecuencia estratégica:**

En el Requisito 11 se identificó que la instalación del plugin es el punto
donde se pierde a la mayoría de usuarios. Hablar Votifier elimina ese punto
por completo para quien ya lo tiene:

| Vía de integración | Fricción | Funcionalidad |
|---|---|---|
| **Votifier (compatibilidad)** | 🟢 **Cero** — ya lo tienen instalado | Solo entrega de votos |
| **Plugin propio** | 🟡 Media — hay que instalarlo | Votos + heartbeat + estadísticas + alertas + recompensas avanzadas |

**Recomendación:** soportar **ambas**. Votifier como puerta de entrada sin
fricción (el servidor se activa en minutos, sin instalar nada), y el plugin
propio como mejora opcional que desbloquea las funciones diferenciales.

Es la diferencia entre pedirle al cliente que instale algo antes de ver valor,
y darle valor antes de pedirle que instale nada.

⚠️ Nota de verificación: antes de comprometerse, debe confirmarse el estado
actual del protocolo y de NuVotifier, y revisar su licencia para asegurar
compatibilidad legal con una implementación propia del lado emisor.

### Hallazgo 2 — Las tres garantías de la filosofía no son simultáneamente alcanzables sin diseño explícito

El documento exige:

1. Nunca perder un voto
2. Nunca duplicar un voto
3. Nunca entregar una recompensa dos veces

En sistemas distribuidos, la entrega **exactamente-una-vez** a través de una
frontera de red es imposible. Escenario concreto e inevitable:

```
Plataforma → envía voto al servidor → el plugin entrega la recompensa
→ el servidor se cae antes de enviar el ACK
→ la plataforma no recibe confirmación → reintenta
→ ¿el jugador recibe la recompensa dos veces?
```

**Lo que sí es alcanzable —y es el diseño correcto—:**

| Garantía | Mecanismo |
|---|---|
| Nunca perder un voto | **At-least-once**: Outbox + reintentos con backoff + DLQ |
| Nunca duplicar el efecto | **Consumidor idempotente**: el plugin descarta IDs ya procesados |

**Requisito de diseño que se deriva y que es fácil pasar por alto:**

> El plugin **debe persistir localmente** los identificadores de votos ya
> aplicados, en almacenamiento que sobreviva a un reinicio del servidor.

Si el plugin guarda esos IDs solo en memoria, un reinicio provoca recompensas
duplicadas en cuanto lleguen los reintentos. Es un fallo silencioso, difícil de
diagnosticar y directamente explotable por un dueño de servidor.

### Hallazgo 3 — La ventana de voto es la mayor fuente de incidencias del sector

El documento pide "24h / 12h / 48h / personalizado / timezone aware / horario
local". Esa combinación multiplica los errores de cambio horario.

**Comparativa de las dos estrategias posibles:**

| | **Reinicio a medianoche** (día natural) | **Ventana deslizante** (24 h desde tu último voto) |
|---|---|---|
| Errores de horario de verano | 🔴 Sí — días de 23 y 25 horas | 🟢 Ninguno |
| Zona horaria del jugador vs del servidor | 🔴 Ambigua, genera reclamaciones | 🟢 Irrelevante |
| Carga del sistema | 🔴 **Pico masivo a las 00:00** | 🟢 Distribuida uniformemente |
| Cálculo | Requiere calendario y zona horaria | Una resta de instantes |
| Percepción del jugador | "Voto cada día" | "Vuelve en X horas" |

**Recomendación: ventana deslizante calculada en UTC.**

El argumento decisivo no es la corrección horaria, sino la **carga**: con
reinicio a medianoche, todos los jugadores elegibles llegan simultáneamente.
Es un pico de tráfico autoinfligido, evitable por diseño, justo en el momento
en que el fraude también se concentra.

En la interfaz debe mostrarse siempre una **cuenta atrás** ("puedes volver a
votar en 6 h 42 min"), nunca una fecha absoluta, que obliga al jugador a
razonar sobre husos horarios.

### Ambigüedad pendiente de resolver

El dominio incluye `Vote Site` y `Vote Source`, lo que admite dos lecturas
incompatibles:

| Lectura | Implicación |
|---|---|
| **A)** La plataforma **es** el sitio de votación | Producto ya definido en los documentos anteriores |
| **B)** La plataforma **agrega** votos de otros sitios externos | Producto distinto: integrador/agregador, con dependencias de terceros |

Ambas son negocios viables, pero exigen diseños diferentes. Debe confirmarse.

### Elementos correctos que se mantienen sin cambios

- Voto como evento de negocio (no como registro CRUD) ✅
- Outbox + cola + DLQ + reintentos ✅
- Trust Score por jugador, servidor y organización ✅ — la puntuación por
  **organización** es especialmente valiosa para detectar redes de colusión
- Herramientas de moderación con **apelaciones** ✅ — imprescindible: habrá
  falsos positivos y son clientes de pago
- Motor de reglas configurable ✅ — permite reaccionar a nuevos patrones de
  fraude sin desplegar código
- Simulador de votos, errores, fraude y carga ✅ — **excelente idea**: es la
  única forma de probar el antifraude sin esperar a sufrir un ataque real
