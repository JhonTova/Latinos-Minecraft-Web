# ADR-0007 — Política de deuda técnica

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03
- **Corrige:** "nunca introducir deuda técnica" de los documentos 07 y del mandato inicial

## Contexto

El mandato original prohibía toda deuda técnica. Ninguna empresa de referencia
opera así: la deuda técnica es una metáfora financiera y cierta deuda es
correcta porque el capital que libera vale más que sus intereses.

Una regla de cero-deuda absoluta produce dos patologías predecibles:

1. **Parálisis de diseño** — cada decisión se convierte en un debate arquitectónico
2. **Deuda oculta** — se dejan de registrar los atajos, porque registrarlos es
   admitir una violación de la norma. La deuda invisible es la única que mata.

## Decisión

Se adopta el cuadrante de Fowler como criterio:

| | **Prudente** | **Temeraria** |
|---|---|---|
| **Deliberada** | ✅ **Permitida con ADR** | ❌ Prohibida |
| **Inadvertida** | Inevitable; se corrige al detectarla | ❌ Prohibida |

> Prohibida toda deuda temeraria. La deuda **deliberada y prudente** es una
> herramienta legítima y requiere: un ADR que la registre, un responsable
> nombrado, una fecha de pago y **un test que falle** cuando el atajo deje de
> ser aceptable.

## Consecuencias

- La deuda se registra en `docs/decisiones/` como cualquier otra decisión
- El test asociado convierte la fecha de pago en algo verificable, no en una
  intención
- Un atajo sin ADR es un defecto, no una decisión
