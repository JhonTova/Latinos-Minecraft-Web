# Requisito 08 — RFC-003: Technology Stack & Engineering Standards

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** selección de stack — sin código ni configuración
> **Valoración del CTO:** ✅ **Alto valor real.** Documento respaldado y recomendado para ejecución.

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 08

RFC-003

TECHNOLOGY STACK & ENGINEERING STANDARDS

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define el stack tecnológico oficial del proyecto.

NO debes escribir código.

NO debes implementar módulos.

NO debes generar configuración.

NO debes crear archivos de proyecto.

Tu misión consiste en diseñar y justificar el stack tecnológico que utilizará toda la plataforma durante los próximos diez años.

Actúa como un Architecture Review Board (ARB) compuesto por:

- Principal Software Architect
- Staff Backend Engineer
- Staff Frontend Engineer
- Principal DevOps Engineer
- Principal Security Engineer
- Principal Database Engineer
- Staff QA Engineer
- Platform Engineer

Cada decisión debe justificarse técnica y estratégicamente.

================================================================================
FILOSOFÍA
================================================================================

El objetivo NO es utilizar las tecnologías más nuevas.

El objetivo es construir una plataforma estable, mantenible y preparada para evolucionar.

Aplicar los siguientes principios:

- Boring Technology First.
- API First.
- Documentation Driven Development.
- Modular Monolith First.
- Cloud Native Ready.
- Open Source First.
- Security by Design.
- Performance by Default.
- Observability by Default.
- Automation First.
- Developer Experience First.
- AI Friendly Codebase.

================================================================================
CRITERIOS DE EVALUACIÓN
================================================================================

Cada tecnología candidata deberá evaluarse mediante una matriz con puntuación.

Como mínimo considerar:

- Madurez.
- Comunidad.
- Rendimiento.
- Seguridad.
- Mantenibilidad.
- Ecosistema.
- Curva de aprendizaje.
- Coste operativo.
- Compatibilidad.
- Licencia.
- Riesgo de abandono.
- Documentación.
- Herramientas disponibles.
- Integración con IA.
- Facilidad para contratar desarrolladores.

Justifica la puntuación de cada criterio.

================================================================================
BACKEND
================================================================================

Analiza y compara.

- Java 21 LTS vs Java 25 LTS.
- Spring Boot.
- Quarkus.
- Micronaut.
- Helidon.
- Kotlin (como alternativa).
- Virtual Threads.
- GraalVM Native Image (analizar si aporta valor).

Define:

- Lenguaje oficial.
- Framework oficial.
- Estándar de empaquetado.
- Gestión de dependencias.
- Convenciones.
- Modularización.

================================================================================
FRONTEND
================================================================================

Analiza.

- React.
- Next.js.
- Vue.
- Nuxt.
- SvelteKit.

Para la UI analiza:

- HeroUI.
- shadcn/ui.
- Mantine.
- MUI.
- Tailwind CSS.
- TanStack.

Define:

- Framework oficial.
- Librería UI.
- Gestión de estado.
- Routing.
- Formularios.
- Validación.
- Internacionalización.
- Accesibilidad.
- Estrategia de diseño.

================================================================================
BASE DE DATOS
================================================================================

Compara.

- PostgreSQL.
- CockroachDB.
- YugabyteDB.
- MariaDB.
- MySQL.

Analiza.

- UUIDv7.
- Flyway.
- JSONB.
- Materialized Views.
- Particionamiento.
- Read Replicas.
- PgBouncer.

Selecciona una arquitectura oficial.

================================================================================
CACHE
================================================================================

Analiza.

- Redis.
- Valkey.
- Caffeine.
- Hazelcast.

Explica cuándo utilizar:

- Cache local.
- Cache distribuida.
- Cache HTTP.
- CDN.

================================================================================
MENSAJERÍA
================================================================================

Compara.

- RabbitMQ.
- Kafka.
- NATS.
- Redis Streams.

Explica.

- Eventos internos.
- Integraciones.
- Outbox Pattern.
- Retry.
- DLQ.

Selecciona la solución oficial.

================================================================================
ALMACENAMIENTO
================================================================================

Analiza.

- Amazon S3.
- Cloudflare R2.
- MinIO.
- Azure Blob.
- Google Cloud Storage.

Explica estrategia local y producción.

================================================================================
AUTENTICACIÓN
================================================================================

Selecciona tecnologías para:

- OAuth 2.1.
- OIDC.
- Passkeys.
- JWT.
- Sesiones.
- MFA.

Justifica todas las decisiones.

================================================================================
OBSERVABILIDAD
================================================================================

Compara.

- OpenTelemetry.
- Prometheus.
- Grafana.
- Loki.
- Tempo.
- Jaeger.

Diseña la plataforma de observabilidad.

================================================================================
TESTING
================================================================================

Selecciona herramientas oficiales.

- JUnit.
- Testcontainers.
- MockMvc.
- Playwright.
- Vitest.
- Cypress.
- Gatling.
- k6.

Explica cuándo utilizar cada una.

================================================================================
CALIDAD DE CÓDIGO
================================================================================

Define herramientas oficiales.

- Spotless.
- Checkstyle.
- Error Prone.
- PMD.
- SpotBugs.
- SonarQube.
- SonarCloud.

Establece reglas mínimas de calidad.

================================================================================
SEGURIDAD
================================================================================

Selecciona herramientas para:

- SCA (Software Composition Analysis).
- Secret Scanning.
- Dependencias vulnerables.
- SBOM.
- Firma de artefactos.
- Renovación automática de dependencias.

Analiza.

- Dependabot.
- Renovate.
- Trivy.
- Syft.
- Grype.
- Sigstore.
- Cosign.

================================================================================
CI/CD
================================================================================

Compara.

- GitHub Actions.
- GitLab CI.
- Jenkins.
- CircleCI.

Diseña un pipeline profesional.

Debe incluir:

- Build.
- Test.
- Lint.
- Security Scan.
- SBOM.
- Docker Build.
- Firma.
- Publicación.
- Deploy.
- Smoke Tests.
- Rollback.

================================================================================
CONTENERIZACIÓN
================================================================================

Analiza.

- Docker.
- Podman.

Define imágenes base.

Estrategia multi-stage.

Hardening.

Imágenes mínimas.

================================================================================
ORQUESTACIÓN
================================================================================

Analiza.

- Docker Compose.
- Kubernetes.
- Nomad.

Justifica cuándo migrar de uno a otro.

================================================================================
INFRAESTRUCTURA COMO CÓDIGO
================================================================================

Analiza.

- Terraform.
- Pulumi.
- Ansible.

Selecciona la estrategia oficial.

================================================================================
CONTROL DE VERSIONES
================================================================================

Define.

- Git Flow.
- Trunk Based Development.
- GitHub Flow.

Selecciona el modelo oficial.

================================================================================
ESTÁNDAR DE COMMITS
================================================================================

Analiza.

- Conventional Commits.
- Gitmoji.
- Commits tradicionales.

Selecciona el estándar oficial.

================================================================================
VERSIONADO
================================================================================

Define.

- Semantic Versioning.
- Release Cadence.
- Release Branches.
- LTS.

================================================================================
DOCUMENTACIÓN
================================================================================

Toda la documentación deberá mantenerse como código.

Selecciona herramientas para:

- ADR.
- RFC.
- Mermaid.
- MkDocs.
- Docusaurus.

================================================================================
DESARROLLO LOCAL
================================================================================

Diseña una experiencia de desarrollo reproducible.

Debe incluir.

- Docker Compose.
- Datos de prueba.
- Seeds.
- Variables de entorno.
- Scripts.
- Hot Reload.
- Debugging.

================================================================================
DEPENDENCIAS
================================================================================

Define una política oficial.

Antes de incorporar una dependencia deberá analizarse:

- Mantenimiento.
- Comunidad.
- Seguridad.
- Licencia.
- Compatibilidad.
- Alternativas.
- Coste de migración.

================================================================================
DEPRECACIÓN
================================================================================

Diseña una política.

Tecnologías obsoletas.

Migraciones.

Actualizaciones.

Compatibilidad.

================================================================================
AI ASSISTED DEVELOPMENT
================================================================================

Define estándares para trabajar con IA.

- Código legible.
- Responsabilidades pequeñas.
- Interfaces claras.
- Comentarios únicamente cuando aporten valor.
- Documentación cercana al código.
- Contexto suficiente para futuras sesiones de IA.
- Evitar abstracciones innecesarias.
- Evitar metaprogramación excesiva.
- Evitar "magia".

================================================================================
ARCHITECTURE REVIEW BOARD
================================================================================

Antes de aprobar cualquier nueva tecnología deberá responderse:

¿Existe una alternativa más madura?

¿Existe una alternativa con menor coste operativo?

¿Existe una alternativa más segura?

¿Cuál es el coste de migración futuro?

¿Qué ocurre si el proyecto original deja de mantenerse?

¿Qué impacto tendrá en la contratación de desarrolladores?

¿Qué impacto tendrá en la experiencia del desarrollador?

================================================================================
OUTPUT ESPERADO
================================================================================

No selecciones tecnologías por popularidad.

Genera un documento profesional que incluya:

- Comparativas detalladas.
- Matrices de decisión ponderadas.
- Pros y contras.
- Costes operativos.
- Riesgos.
- Plan de evolución a cinco y diez años.
- ADR para cada decisión importante.
- Recomendación final claramente justificada.

Este documento será el estándar oficial de ingeniería de la plataforma y cualquier cambio futuro deberá justificarse mediante un nuevo ADR.

---

## Notas del CTO sobre este requisito

### Valoración

**Documento respaldado.** Es, junto al 02 (PRD) y al 09 (Roadmap), uno de los
tres que aportan valor irreemplazable. Las decisiones de lenguaje, framework,
base de datos y frontend son caras de revertir una vez hay código, y baratas
de tomar ahora.

Además, el criterio rector que propone —**Boring Technology First**— es
correcto y es exactamente el filtro que debe aplicarse.

### Contradicción interna del documento

El documento proclama **"Boring Technology First"** y a continuación pide
evaluar y adoptar **~60 tecnologías**, entre ellas Kafka, Kubernetes,
Terraform, Pulumi, Nomad, Jaeger, Tempo, Loki, Hazelcast, Sigstore y Cosign.

"Boring Technology First" no significa elegir tecnologías aburridas: significa
**gastar el escaso presupuesto de novedad en lo que da ventaja competitiva y
usar lo más aburrido posible en todo lo demás**. Adoptar 60 herramientas es lo
contrario: son 60 cosas que operar, actualizar, parchear y depurar.

Regla propuesta: **cada herramienta debe justificar por qué el proyecto no
puede avanzar sin ella hoy.** No "es buena práctica", sino "sin esto, esta
semana, algo se rompe".

### Veredictos preliminares del ARB

| Área | Decisión | Justificación resumida |
|---|---|---|
| **Lenguaje** | **Java 25 LTS** (backend) | LTS reciente, larga vida útil, Virtual Threads maduros |
| **Bytecode del plugin** | **Java 21** | ⚠️ Los servidores Minecraft ejecutan Java 21. El plugin **no puede** exigir Java 25 o no arrancará en los servidores de los clientes. Backend y plugin llevan targets distintos. |
| **Kotlin** | ❌ Descartado | Buen lenguaje, pero el ecosistema Paper es Java; un solo lenguaje reduce fricción sin perder nada decisivo |
| **Framework** | **Spring Boot** | Ecosistema mayor, más documentación, mucho más material de entrenamiento para asistencia por IA (criterio que el propio documento exige) |
| Quarkus / Micronaut / Helidon | ❌ Descartados | Su ventaja es arranque en frío y consumo bajo → relevante en serverless, que ya se descartó |
| **Virtual Threads** | ✅ Sí | Encaje excelente: los heartbeats son I/O puro y masivo. Miles de conexiones concurrentes sin complejidad asíncrona. |
| **GraalVM Native** | ❌ No | Builds lentos, problemas con reflexión, sin beneficio en un proceso de larga vida |
| **Base de datos** | **PostgreSQL** | Ya decidido (ADR-0002) |
| CockroachDB / Yugabyte | ❌ Descartados | Resuelven escritura multi-región que no existe aquí; coste y limitaciones de dialecto altos |
| MySQL / MariaDB | ❌ Descartados | Sin JSONB equivalente, sin particionado tan sólido, peor para este caso |
| **Cache** | **Caffeine** (local) primero, **Valkey** cuando haya varias instancias | Caffeine cubre el 80% sin añadir un servicio. Valkey: fork de Redis bajo Linux Foundation, licencia BSD limpia y compatible drop-in |
| **Mensajería** | **PostgreSQL como cola** (tabla Outbox) | 🔴 Decisión importante: **ni Kafka ni RabbitMQ al inicio.** Un servicio menos que operar; Postgres soporta este volumen sobradamente. RabbitMQ cuando haya evidencia de necesidad. Kafka, probablemente nunca. |
| **Almacenamiento** | **Cloudflare R2** + MinIO en local | R2 no cobra egreso. Como se sirven imágenes (banners, iconos de servidor), el egreso es el coste dominante en S3. |
| **Frontend** | **Next.js (React)** | Ecosistema, contratación, compatibilidad con shadcn/ui |
| **UI** | **shadcn/ui + Tailwind** | El código se copia al repositorio: **se posee**, no es dependencia. Sin lock-in y personalizable — clave para un aspecto propio y profesional. HeroUI/MUI imponen su estética y su ciclo de versiones. |
| **Orquestación** | **Docker Compose** en un VPS | 🔴 **Kubernetes: NO.** Operarlo es un empleo a tiempo completo. Migrar solo con equipo dedicado. |
| **IaC** | Ninguna al inicio | Terraform cuando haya infraestructura real que versionar |
| **CI/CD** | **GitHub Actions** | Integración nativa, gratis en repos públicos |
| **Ramas** | **Trunk-based** con ramas cortas | GitFlow es sobrecarga pura para un equipo pequeño |
| **Commits** | **Conventional Commits** | Habilita CHANGELOG y versionado automáticos |
| **Docs** | **Docusaurus** | Portal de desarrollador + ADRs + RFCs en un solo sitio |
| **Testing** | JUnit 5 + Testcontainers + Playwright | Gatling/k6/Cypress: más adelante |
| **Calidad** | Spotless + Error Prone + SonarCloud | Checkstyle/PMD/SpotBugs se solapan y generan ruido |
| **Observabilidad** | Logs estructurados + Actuator primero | Prometheus/Grafana/Loki/Tempo = 4 servicios más que operar. Añadir cuando haya algo que observar. |
| **Seguridad de dependencias** | Dependabot + Trivy | Gratis, integrado. Sigstore/Cosign cuando se distribuya el plugin masivamente. |

### Hallazgo específico de alto valor

**El backend y el plugin necesitan versiones de Java distintas.**

El plugin se ejecuta en servidores de terceros que corren la versión de Java
exigida por su versión de Minecraft. Compilar el plugin con un target superior
al que usan los servidores de los clientes hace que el plugin **no cargue**.

Consecuencia práctica: el módulo compartido entre backend y plugin (DTOs,
contratos, SDK) debe compilarse con el target **más bajo** de los dos.
