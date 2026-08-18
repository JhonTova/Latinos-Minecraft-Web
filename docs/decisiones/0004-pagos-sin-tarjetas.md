# ADR-0004 — Nunca tocar datos de tarjeta

- **Estado:** ✅ Vigente · **Innegociable**
- **Fecha:** 2026-08-03

## Decisión

La plataforma **nunca** recibe, procesa, transmite ni almacena datos de
tarjeta. Todo pago se delega a una pasarela mediante formularios alojados por
ella (tokenización del lado del proveedor).

## Justificación

- Mantiene el alcance PCI-DSS en el nivel más bajo posible (SAQ-A)
- Elimina de raíz la clase de incidente más costosa y reputacionalmente grave
- Un equipo de una persona no puede sostener el cumplimiento de un alcance
  PCI superior

## Decisiones asociadas

| Elemento | Decisión |
|---|---|
| Pasarelas | Diseñadas como **puerto intercambiable** desde el día 1. El mercado hispanohablante necesita medios locales (Mercado Pago, PSE) además de los internacionales. Acoplarse a un solo proveedor sería un error caro. |
| Wallet / créditos prepago / gift cards | ❌ **Descartados.** Retener saldo de clientes implica pasivo diferido contable, posible regulación de dinero electrónico y gestión de reembolsos y saldos huérfanos. |
| Reparto de ingresos y afiliados | ❌ Aplazados. Implican pagar a personas físicas en varios países: verificación de identidad, retenciones e informes fiscales. |
| Ledger | Inmutable, solo append. Las correcciones son asientos compensatorios; jamás UPDATE ni DELETE sobre dinero. |
| Webhooks de la pasarela | Verificación de firma obligatoria + idempotencia + protección contra replay |

## Consecuencias

- El cobro ocurre en el momento de la compra, contra una entrega comprometida
  (ver ADR-0013)
- La conciliación entre el ledger propio y el extracto de la pasarela es un
  proceso periódico obligatorio
- Toda operación económica queda auditada
