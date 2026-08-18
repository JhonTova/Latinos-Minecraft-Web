# Requisito 15 — Platform Ecosystem / Developer Platform / Extension Framework

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** ecosistema de extensiones — sin código
> **Valoración del CTO:** ⚠️ Producto independiente. Requiere un mercado de dos lados que aún no existe.

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 15

PLATFORM ECOSYSTEM

DEVELOPER PLATFORM

EXTENSION FRAMEWORK

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define el ecosistema completo de extensibilidad de la plataforma.

NO escribir código.

NO implementar SDK.

NO crear APIs.

NO diseñar interfaces.

Tu misión consiste en diseñar una plataforma donde terceros puedan construir productos completos sobre nuestra infraestructura.

El objetivo es que cualquier desarrollador pueda extender la plataforma sin modificar el núcleo.

Piensa como un Principal Platform Engineer de:

Shopify. Stripe. GitHub. Atlassian. Cloudflare. Discord. Slack. Vercel.

================================================================================
FILOSOFÍA
================================================================================

La plataforma nunca deberá crecer mediante código personalizado.

Deberá crecer mediante extensiones.

Toda nueva funcionalidad debe preguntarse primero:

¿Puede resolverse mediante una extensión?

Si la respuesta es sí, NO debe agregarse al Core.

================================================================================
PLATFORM CORE
================================================================================

Define claramente qué pertenece al Core.

Authentication. Organizations. Permissions. Billing. Payments. Voting.
Auctions. Automation. Analytics. SDK. Event Bus. API Gateway.

Todo lo demás deberá poder desarrollarse como extensión.

================================================================================
EXTENSION FRAMEWORK
================================================================================

Diseña un framework completo.

Apps. Plugins. Widgets. Panels. Themes. Automation Packs. Analytics Packs.
Providers. Payment Gateways. OAuth Providers. Notification Providers.
Storage Providers. AI Providers.

================================================================================
EXTENSION MANIFEST
================================================================================

Diseña un manifiesto estándar.

Nombre. ID. Autor. Versión. Licencia. Compatibilidad. Dependencias.
Permisos. Eventos. APIs utilizadas. Capacidades. Recursos. Hooks.
Migrations. Checksum. Firma Digital. Trust Level. Marketplace Metadata.
Screenshots. Icono. Categoría.

================================================================================
LIFECYCLE
================================================================================

Diseña completamente.

Install. Enable. Disable. Suspend. Update. Rollback. Uninstall. Migration.

================================================================================
HOOK SYSTEM
================================================================================

Diseña.

Before. After. Around. Async. Priority. Cancellation.

================================================================================
EVENT BUS
================================================================================

Diseña todos los eventos públicos.

Vote Events. Payment Events. Auction Events. Organization Events.
Plugin Events. Analytics Events. Webhook Events. Automation Events.
Security Events.

================================================================================
PUBLIC API
================================================================================

Diseña APIs públicas para extensiones.

REST. Webhooks. Streaming. Events. SDK.

================================================================================
SDK
================================================================================

Diseña un SDK oficial.

Java. JavaScript. TypeScript. Go. Python. PHP. C#.
Rust (analizar si aporta valor).

Debe compartir una misma filosofía y modelos.

================================================================================
CLI
================================================================================

Diseña una CLI oficial.

Crear extensión. Validar. Empaquetar. Firmar. Publicar. Actualizar. Probar.

================================================================================
MARKETPLACE
================================================================================

Diseña un Marketplace profesional.

Búsqueda. Categorías. Valoraciones. Instalación. Actualizaciones. Versiones.
Dependencias. Compatibilidad. Licencias. Monetización. Moderación.

================================================================================
SANDBOX
================================================================================

Toda extensión deberá ejecutarse de forma aislada.

Diseña.

Permisos. Recursos. Memoria. CPU. Storage. Secrets. Network. Filesystem.

================================================================================
PERMISSIONS
================================================================================

Diseña permisos granulares.

Read Votes. Write Votes. Payments. Billing. Users. Organizations.
Analytics. Notifications. Secrets.

================================================================================
SECURITY
================================================================================

Diseña.

Firma Digital. Trust Score. Verificación. Code Signing. Supply Chain.
SBOM. Malware Scan. Static Analysis.

================================================================================
CERTIFICACIÓN
================================================================================

Diseña un programa oficial.

Verified. Partner. Enterprise. Community. Internal.

================================================================================
VERSIONADO
================================================================================

Diseña.

Compatibility Matrix. Breaking Changes. Migration Guides.
Deprecation Policy. LTS.

================================================================================
DEPENDENCIAS
================================================================================

Diseña gestión de dependencias.

Resolución. Conflictos. Versiones. SemVer.

================================================================================
UI EXTENSIONS
================================================================================

Permitir agregar.

Páginas. Widgets. Dashboards. Botones. Menús. Panels.

================================================================================
AUTOMATION EXTENSIONS
================================================================================

Permitir agregar.

Triggers. Conditions. Actions. Templates.

================================================================================
PAYMENT EXTENSIONS
================================================================================

Permitir agregar.

Stripe. PayPal. Mercado Pago. PayU. PSE. Criptomonedas (analizar).

================================================================================
AUTH EXTENSIONS
================================================================================

Permitir agregar.

Google. Discord. GitHub. Microsoft. LDAP. SAML. OIDC.

================================================================================
AI EXTENSIONS
================================================================================

Permitir agregar.

Moderación. Analytics. Predicciones. Asistentes. Chat. Automatizaciones.

================================================================================
OBSERVABILIDAD
================================================================================

Toda extensión deberá exponer.

Logs. Tracing. Metrics. Health.

================================================================================
TESTING
================================================================================

Toda extensión deberá poder ejecutarse.

Sandbox. Mock APIs. Fake Events. Test Environment.

================================================================================
DOCUMENTACIÓN
================================================================================

Toda extensión deberá incluir.

README. CHANGELOG. LICENSE. Migration Guide. Permissions. Examples.

================================================================================
GOVERNANCE
================================================================================

Diseña un proceso para aprobar extensiones.

Review. Security Review. Performance Review. Compatibility Review.

================================================================================
OUTPUT
================================================================================

No escribir código.

No implementar SDK.

No crear Marketplace.

Quiero una especificación completa para construir un ecosistema comparable al de Shopify o Atlassian, especializado en Minecraft.

Incluye:

Arquitectura. Diagramas Mermaid. Lifecycle. Manifest. SDK Philosophy.
Marketplace. CLI. Seguridad. Roadmap. Buenas prácticas.

Y cualquier recomendación que facilite la creación de un ecosistema sostenible durante los próximos diez años.

---

## Notas del CTO sobre este requisito

### Objeción principal — un ecosistema de extensiones exige un mercado de dos lados

La filosofía declarada es: *"la plataforma nunca deberá crecer mediante código
personalizado; deberá crecer mediante extensiones"*.

Aplicada a una plataforma con **cero usuarios y cero desarrolladores
terceros**, esa regla se invierte: no habría forma de que la plataforma
creciera en absoluto.

Un marketplace de extensiones es un **mercado de dos lados** y solo funciona
cuando ambos existen simultáneamente:

| Lado | Condición para participar | Estado actual |
|---|---|---|
| Desarrolladores | Suficientes clientes para que compense construir | 0 clientes |
| Clientes | Suficientes extensiones para que aporte valor | 0 extensiones |

Sin uno, el otro no aparece. El framework quedaría construido y vacío.

**Cronología de las referencias citadas:** Shopify abrió su App Store años
después de tener una base grande de comerciantes. GitHub lanzó Actions una
década después de GitHub. Stripe abrió su marketplace con miles de negocios ya
integrados. En todos los casos, **el ecosistema llegó después del producto,
nunca antes**.

### Hallazgo de mayor valor — el 90% de la plataforma de extensiones ya está diseñada y es gratis

**Shopify no ejecuta código de terceros dentro de Shopify.** Las apps de
Shopify son servicios web externos, alojados por su propio desarrollador, que
llaman a la API de Shopify y embeben interfaz mediante iframes.

Ese es el modelo barato, seguro y probado. Y significa que las piezas ya
decididas en el Requisito 05 **ya son una plataforma de extensiones**:

| Pieza necesaria | Estado |
|---|---|
| API pública documentada (OpenAPI) | ✅ Ya diseñada |
| OAuth 2.1 para autorizar apps de terceros | ✅ Ya diseñado |
| Webhooks firmados con eventos | ✅ Ya diseñados |
| Catálogo de eventos públicos | ✅ Extraído del Requisito 14 |
| Scopes y permisos granulares | ✅ Ya diseñados |
| Listado/marketplace y proceso de revisión | 🟡 Barato de añadir cuando haya demanda |

Es decir: **cualquiera podrá construir sobre la plataforma desde el día 1**,
sin construir un framework de extensiones.

### Lo que debe evitarse: ejecutar código de terceros dentro de la plataforma

El documento pide un sandbox con aislamiento de memoria, CPU, almacenamiento,
red y sistema de archivos, más sistema de hooks (before/after/around con
prioridad y cancelación), resolución de dependencias y migraciones por
extensión.

Ejecutar código no confiable de forma segura es uno de los problemas más
difíciles de la informática. Las empresas que lo hacen bien (Cloudflare con
aislados de V8, Deno, Figma) han invertido años de ingeniería especializada.

| Modelo | Coste | Riesgo | Recomendación |
|---|---|---|---|
| **Apps externas vía API + OAuth + webhooks** | ~0 (ya hecho) | Bajo | ✅ **Adoptar** |
| Código de terceros ejecutándose dentro de la plataforma | Muy alto | 🔴 Crítico | ❌ Descartar |

Con el modelo externo desaparecen de golpe: sandbox, límites de recursos,
sistema de hooks, resolución de dependencias, escaneo de malware, análisis
estático, firma de código y migraciones por extensión.

### Contradicción con decisiones previas

La sección PLATFORM CORE incluye **Auctions** y **Automation** como parte del
núcleo. Ambas fueron aplazadas fuera de la v1 en los Requisitos 02 y 14
respectivamente. Declararlas "Core" reabre una decisión ya cerrada, sin ADR.

### Elementos aprovechables

| Elemento | Decisión |
|---|---|
| Catálogo de eventos públicos | ✅ Ya conservado (Requisito 14) |
| Proveedores de pago intercambiables (Stripe, PayPal, Mercado Pago, PSE) | ✅ **Diseñar como interfaz interna desde el día 1.** No como extensión de terceros, sino como abstracción propia: el mercado hispanohablante necesita medios de pago locales, y acoplarse a un solo proveedor sería un error caro. |
| Proveedores de autenticación intercambiables (Google, Discord, Microsoft) | ✅ Igual: interfaz interna, no framework de extensiones. **Discord es prioritario** en este ecosistema. |
| Proveedor de almacenamiento intercambiable | ✅ Interfaz interna (R2/S3/MinIO ya decidido) |
| CLI oficial | 🟡 Aplazada — útil para desarrolladores, sin audiencia todavía |
| Programa de certificación (Verified/Partner/Enterprise) | ❌ Aplazado — requiere ecosistema |
| Marketplace con monetización y reparto de ingresos | ❌ Aplazado — además arrastra implicaciones fiscales y de pagos a terceros |
| Extensiones de IA | ❌ Aplazado |
| LDAP / SAML | ❌ Aplazado — solo tiene sentido con clientes empresariales reales |

### Observación transversal

Los principios de la interfaz de proveedores (pagos, auth, almacenamiento
intercambiables) sí deben aplicarse **desde el primer día**, pero como
**puertos y adaptadores internos** —ya previstos en la arquitectura hexagonal
del Requisito 03— no como un framework público de extensiones. Coste marginal
cero, beneficio idéntico.
