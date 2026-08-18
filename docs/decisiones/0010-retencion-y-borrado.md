# ADR-0010 — Retención de datos y derecho de supresión

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03
- **Corrige:** "nunca eliminar información" de los documentos 04 y 06

## Contexto

Los documentos 04 y 06 exigían repetidamente "nunca eliminar información" y
borrado lógico en todas las tablas. Es incompatible con el **derecho de
supresión (RGPD art. 17)**, y el riesgo se agrava porque una parte relevante
de la base de usuarios de Minecraft son menores (art. 8, consentimiento
parental).

El propio documento 06 se contradecía: exigía "nunca eliminar registros" en la
sección de auditoría y "derecho al olvido" en la de privacidad.

El matiz que resuelve el conflicto: el derecho de supresión **tiene
excepciones**, entre ellas el cumplimiento de obligaciones legales (los
registros contables deben conservarse durante años).

## Decisión

> Se conserva **el hecho**; se anonimiza **a la persona**.

| Tipo de dato | Política |
|---|---|
| Ledger, facturas, registros fiscales | **Se conservan** (obligación legal). Se sustituye la identidad por un identificador anonimizado; los importes no se tocan. |
| Datos personales (email, nombre, IP) | Se borran o anonimizan a petición |
| Votos | Se conserva el voto agregado; se desvincula la identidad |
| Registros de auditoría | Se conserva la acción; el actor se pseudonimiza pasado el plazo de retención |
| IPs para antifraude | Retención **90 días**, después hash con sal rotativa o borrado |
| Sesiones y dispositivos | Se borran con la cuenta |

### Reglas de implementación

1. Toda tabla con datos personales declara su política de retención **en la
   migración que la crea**. Sin política declarada, la migración no se acepta.
2. La anonimización es un proceso **irreversible y auditado**: queda registro
   de que se ejecutó, sin conservar lo eliminado.
3. El borrado lógico (`deleted_at`) sirve para recuperación operativa y
   coherencia referencial, **no** como excusa para conservar datos personales
   indefinidamente. Tiene su propio plazo tras el cual se purga.
4. Base legal para el tratamiento de IPs en antifraude: **interés legítimo**,
   documentado, con minimización y plazo corto.

## Consecuencias

- El esquema nace con la política de retención incorporada, no parcheada
- Se necesita un proceso programado de purga y pseudonimización
- El ledger sigue siendo inmutable (ADR-0004): anonimizar al titular **no** es
  modificar el asiento contable
- Una solicitud de supresión no puede romper la integridad del ranking
  histórico ni de la contabilidad
