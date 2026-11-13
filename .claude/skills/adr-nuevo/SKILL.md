---
name: adr-nuevo
description: Registra una decisión de arquitectura de LatinosMC como un ADR nuevo en docs/decisiones y actualiza el índice de docs/DECISIONES.md. Úsala cuando se agregue una dependencia, se cambie el contrato con el plugin, se acepte una deuda técnica, se reemplace una decisión anterior, o cuando alguien pida "un ADR" o "documentar la decisión".
---

# ADR nuevo de LatinosMC

Las decisiones de arquitectura viven en `docs/decisiones/NNNN-titulo.md` y su
índice es la tabla de `docs/DECISIONES.md`. Un ADR explica **por qué** se
decidió algo, para quien llegue después y quiera cambiarlo.

## Cuándo hace falta uno

- Se agrega una dependencia (el stack está cerrado: `AGENTS.md`).
- Cambia una frontera entre módulos o se agrega una lectura SQL entre módulos
  (en ese caso se amplía ADR-0019, no se crea otro).
- Cambia el contrato `/api/v1/plugin/*`.
- Se acepta una deuda a propósito (política en ADR-0007: lleva fecha de pago).
- Se reemplaza una decisión vigente.

No hace falta para un detalle de implementación que cabe en un comentario.

## Pasos

1. **Lee `docs/DECISIONES.md`** y los ADR relacionados. Si la decisión contradice
   uno vigente, es un reemplazo: dilo.
2. **Número:** el mayor de `docs/decisiones/` más uno, con cuatro cifras.
3. **Archivo:** `docs/decisiones/NNNN-titulo-en-kebab-case.md`, sin tildes en el
   nombre.
4. **Contenido**, con este esqueleto:

   ```markdown
   # ADR-NNNN — Título que dice la decisión

   - **Estado:** ✅ Vigente
   - **Fecha:** AAAA-MM-DD
   - **Relacionado:** ADR-XXXX (por qué se relaciona)

   ## Contexto
   El problema concreto, con el caso real que lo destapó.

   ## Decisión
   Qué se hace, con el alcance exacto.

   ## Alternativas descartadas
   Cada una con el motivo. Incluye siempre la obvia.

   ## Consecuencias
   Lo que mejora, lo que empeora y lo que queda sin cubrir.
   ```

5. **Índice:** agrega la fila a la tabla «Índice de ADRs» de
   `docs/DECISIONES.md`.
6. **Si reemplaza a otro:** en el ADR viejo cambia solo la línea de estado a
   `⛔ Reemplazado por ADR-NNNN` y actualiza su fila del índice. El archivo viejo
   **no se borra ni se reescribe**.
7. Si el `README.md` de la raíz cuenta los ADR, actualiza la cifra.

## Reglas

- Escribe el contexto con hechos del proyecto, no con generalidades.
- «Consecuencias» incluye lo malo. Un ADR sin costos no se cree.
- Un ADR vigente no se edita para cambiar la decisión: se reemplaza.
- El ADR va en el mismo pull request que el cambio que lo necesita.
