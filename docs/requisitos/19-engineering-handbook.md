# Requisito 19 — Engineering Handbook / Development Standards

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** manual de ingeniería — sin código
> **Nota:** el usuario saltó del documento 17 al 19. **El documento 18 no ha sido entregado.**

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 19

ENGINEERING HANDBOOK

DEVELOPMENT STANDARDS

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define los estándares oficiales de ingeniería para todo el proyecto.

NO escribir código.

NO implementar funcionalidades.

NO diseñar interfaces.

Tu misión consiste en crear el manual técnico que regirá el desarrollo durante toda la vida del proyecto.

Piensa como un Staff Software Engineer de una empresa de clase mundial.

================================================================================
PRINCIPIOS
================================================================================

Define principios obligatorios.

Simplicity First. Security by Design. Performance by Default.
Observability by Default. Documentation as Code. Backward Compatibility.
Automation First.

================================================================================
ESTILO DE CÓDIGO
================================================================================

Define convenciones para:

Backend. Frontend. Plugin. SDK. Scripts. Nombres.
Estructura de carpetas. Comentarios. Documentación.

================================================================================
GIT
================================================================================

Define estrategia.

Git Flow vs Trunk Based. Conventional Commits. Semantic Versioning.
Branch Protection. Release Branches. Hotfix.

================================================================================
PULL REQUESTS
================================================================================

Define requisitos.

Checklist. Review. Tamaño máximo. Cobertura. Benchmarks. Documentación.

================================================================================
TESTING
================================================================================

Define estándares.

Unit. Integration. Contract. E2E. Performance. Security.
Mutation Testing (evaluar).

Cobertura mínima por módulo.

================================================================================
CI/CD
================================================================================

Define pipeline.

Lint. Build. Tests. Security Scan. SBOM. Licencias. Deploy. Rollback.

================================================================================
DEPENDENCIAS
================================================================================

Política para incorporar nuevas librerías.

Licencias. Mantenimiento. Popularidad. Riesgo.

================================================================================
DOCUMENTACIÓN
================================================================================

ADR. RFC. CHANGELOG. README. Diagramas. OpenAPI. AsyncAPI.

================================================================================
SEGURIDAD
================================================================================

Reglas obligatorias.

Secretos. Validación. Rate Limits. Headers. Criptografía.

================================================================================
PERFORMANCE
================================================================================

Presupuestos. Latencia. Uso de memoria. Consultas. Caching.

================================================================================
OBSERVABILIDAD
================================================================================

Todo nuevo módulo deberá incluir.

Logs. Tracing. Metrics. Health Checks.

================================================================================
QUALITY GATES
================================================================================

Ningún módulo podrá fusionarse si falla:

Cobertura. Seguridad. Lint. Performance. Compatibilidad.

================================================================================
DEFINITION OF READY
================================================================================

Condiciones para empezar una tarea.

================================================================================
DEFINITION OF DONE
================================================================================

Condiciones para cerrar una tarea.

================================================================================
ARQUITECTURA
================================================================================

Reglas de acoplamiento. Dependencias. Eventos. APIs.

================================================================================
PLUGIN
================================================================================

Buenas prácticas específicas para Paper/Velocity.

Compatibilidad. Migraciones.

================================================================================
API
================================================================================

Versionado. Errores. Paginación. Idempotencia.

================================================================================
FRONTEND
================================================================================

Accesibilidad. Responsive. Design System. Performance.

================================================================================
BACKEND
================================================================================

DDD. Clean Architecture. CQRS (donde aporte valor). Event Driven.

================================================================================
OUTPUT
================================================================================

Quiero un manual de ingeniería que cualquier desarrollador pueda seguir para mantener un nivel de calidad consistente durante años.

---

## 🔴 Hallazgo técnico principal: Velocity y las recompensas a jugadores desconectados

La sección PLUGIN es la primera de los 18 requisitos que menciona **Velocity**.
Es una omisión importante de todos los documentos anteriores, y tiene dos
consecuencias de diseño que hasta ahora no estaban contempladas.

### 1. Arquitectura de red (proxy + servidores backend)

Los networks de Minecraft no son un servidor: son un **proxy** (Velocity o
BungeeCord) delante de varios servidores Paper (lobby, survival, minijuegos…).

| Dónde se instala el plugin | Consecuencia |
|---|---|
| Solo en el proxy (Velocity) | Ve a todos los jugadores, pero **no puede ejecutar comandos de juego** (dar objetos, dinero) porque el proxy no gestiona el mundo |
| Solo en los backend (Paper) | Puede dar recompensas, pero cada servidor ve solo a sus propios jugadores |
| **Ambos, coordinados** | ✅ Correcto: el proxy recibe el voto, el backend entrega la recompensa |

Esto afecta directamente al modelo de datos: un **network es una organización
con varios servidores**, pero desde fuera es **una sola entrada en el ranking**.
La relación "servidor registrado ↔ instancias reales" no es 1:1.

### 2. La mayoría de los votos llegan con el jugador desconectado

Es el caso más común y el más fácil de olvidar: el jugador vota **desde la
web**, no desde el juego. Con frecuencia no está conectado en ese momento.

```
Jugador vota en la web  →  plataforma valida  →  envía al servidor
→  el jugador NO está conectado  →  ¿qué ocurre con la recompensa?
```

**Requisito obligatorio derivado:**

> El plugin debe **almacenar recompensas pendientes** y entregarlas en el
> siguiente inicio de sesión del jugador.

Combinado con el requisito ya establecido en el Requisito 13 (persistir los
IDs de votos ya procesados para evitar duplicados), el plugin necesita
**almacenamiento local persistente** para dos cosas distintas:

| Dato | Propósito |
|---|---|
| IDs de votos ya aplicados | Evitar recompensas duplicadas tras reintentos |
| Recompensas pendientes | Entregar al jugador cuando se conecte |

Un plugin sin persistencia local falla en ambos casos, y los fallos son
silenciosos.

---

## Notas del CTO sobre este requisito

### Solapamiento con documentos anteriores

Es el **tercer documento sobre estándares de ingeniería**. Comparación:

| Tema | Doc 07 | Doc 08 | Doc 19 |
|---|:---:|:---:|:---:|
| Principios de ingeniería | ✔ | ✔ | ✔ |
| Convenciones de código | ✔ | ✔ | ✔ |
| Estrategia Git y commits | | ✔ | ✔ |
| Testing y cobertura | ✔ | ✔ | ✔ |
| CI/CD | | ✔ | ✔ |
| Política de dependencias | ✔ | ✔ | ✔ |
| Documentación (ADR/RFC) | ✔ | ✔ | ✔ |
| Observabilidad | ✔ | ✔ | ✔ |
| Quality Gates | ✔ | | ✔ |
| Definition of Ready / Done | ✔ | | ✔ |
| Seguridad | ✔ | ✔ | ✔ |

Solapamiento estimado: **~80%**. Es el más alto detectado hasta ahora.

Los tres documentos se fusionarán en **un único manual de ingeniería**. Tres
versiones de la misma norma garantizan que se contradigan con el tiempo, y ya
hay señales de deriva entre documentos (ver Requisito 16: runbooks para
RabbitMQ y Redis, descartados en el Requisito 08).

### Contenido genuinamente nuevo y valioso

| Elemento | Veredicto |
|---|---|
| **AsyncAPI** | ✅ **Muy buena aportación.** Es el equivalente de OpenAPI para eventos y webhooks. Encaja perfectamente con el catálogo de 22 eventos extraído del Requisito 14 y con los webhooks del Requisito 05. Permite generar documentación y clientes de eventos automáticamente. |
| **Estándares de Pull Request** (tamaño máximo, checklist) | ✅ Válido. Con un solo desarrollador la revisión es autorrevisión, pero limitar el tamaño de los cambios sigue siendo valioso: un cambio pequeño es fácil de revertir; uno grande no. |
| **Mutation Testing** | 🟡 **Sí, pero solo en módulos críticos.** Es lento y ruidoso a escala general, pero en antifraude y ledger responde a la pregunta correcta: *¿mis tests detectarían realmente un fallo?* Recomendado para: Votos, Trust&Abuse, Ledger, Pagos. Descartado en el resto. |
| **Presupuestos de rendimiento** | ✅ Correcto: fijar límites de latencia y consultas por endpoint evita degradación silenciosa |
| **Backward Compatibility** como principio | ✅ Correcto y coherente con la API pública versionada |
| **Buenas prácticas Paper/Velocity** | ✅ **Crítico** — ver hallazgo principal arriba |

### Coherencia con decisiones ya tomadas

| Pregunta del documento | Decisión vigente |
|---|---|
| Git Flow vs Trunk Based | **Trunk Based** con ramas cortas (Requisito 08) |
| Conventional Commits | ✅ Adoptado (Requisito 08) |
| CQRS "donde aporte valor" | Solo read models para ranking (Requisito 03) |
| Cobertura mínima por módulo | Escalonada por criticidad (Requisito 07): completa en módulos críticos, proporcionada en el resto |

### Documento 18 ausente

La secuencia pasa de 17 a 19. Pendiente de confirmar con el usuario si el
documento 18 existe y quedó sin enviar.
