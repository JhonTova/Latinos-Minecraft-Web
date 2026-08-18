# ADR-0006 — OpenAPI y AsyncAPI como fuente de verdad de los contratos

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Decisión

- **OpenAPI 3.1** es la fuente de verdad de la API HTTP
- **AsyncAPI** es la fuente de verdad de los eventos y webhooks
- Los SDK se **generan**, no se escriben a mano
- Los cambios incompatibles se detectan en CI comparando contra la
  especificación publicada

## Superficies de API

Tres, no once (corrige el documento 05):

| Superficie | Consumidor | Autenticación |
|---|---|---|
| **Pública** | Web propia, partners, integraciones de terceros | OAuth 2.1 + API Keys con scopes |
| **Plugin** | Plugin Paper/Velocity | API Key del servidor + firma |
| **Admin** | Panel interno | Sesión privilegiada, aislada |

Webhooks es tráfico **saliente**, no una API. Health, metrics y status son
endpoints de operación, no productos.

## Estándares adoptados

| Estándar | Uso |
|---|---|
| Problem Details (RFC 9457) | Formato único de error en toda la API |
| Idempotency-Key | Obligatorio en votos, pagos y entregas |
| ETags / Cache-Control | El ranking es altamente cacheable |
| Paginación por cursor | Listados grandes y rankings |
| HMAC-SHA256 + timestamp + nonce | Firma de webhooks salientes |

## Descartados

| | Motivo |
|---|---|
| GraphQL | Rate limiting y caché HTTP muy difíciles; superficie de abuso mayor en API pública |
| gRPC | Sin sentido dentro de un monolito modular |
| HATEOAS completo / HAL / JSON:API | Coste alto, adopción real casi nula |

## SDK oficiales

Solo **Java** en v1 (es el público real: desarrolladores de plugins).
Node/TypeScript en segunda fase. Para el resto se publica la especificación
OpenAPI: cualquiera genera su cliente en un minuto y la plataforma no adquiere
obligación de mantenimiento.
