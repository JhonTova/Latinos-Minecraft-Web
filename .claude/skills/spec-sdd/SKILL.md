---
name: spec-sdd
description: Crea o completa una spec de LatinosMC con desarrollo guiado por especificaciones (carpeta specs/NNN-nombre con spec.md, plan.md y tareas.md). Úsala cuando alguien pida "una spec", "especificar una funcionalidad", "planear una funcionalidad", "dividir en tareas", o antes de empezar cualquier funcionalidad nueva que no sea un arreglo de una línea.
---

# Spec de LatinosMC (SDD)

El proyecto no usa Spec Kit. Cada funcionalidad vive en una carpeta
`specs/NNN-nombre-corto/` con tres archivos que se escriben **en este orden** y
cada uno se aprueba antes de pasar al siguiente:

| Archivo | Responde | Lo aprueba |
|---|---|---|
| `spec.md` | Qué se construye y por qué, con criterios de aceptación verificables | El desarrollador |
| `plan.md` | Cómo: decisiones técnicas, archivos que se tocan, matriz criterio → prueba | El desarrollador |
| `tareas.md` | El trabajo en pasos pequeños, que se van marcando | — |

Las plantillas están en `plantillas/`, junto a este archivo. Cópialas; no
inventes otra estructura.

## Pasos

1. **Número y rama.** Lista `specs/` con `Glob` (`specs/*/spec.md`) y toma el mayor `NNN` más uno, con tres
   cifras. La rama se llama igual que la carpeta, con su tipo:
   `feat/NNN-nombre-corto`. Una spec, una rama, un pull request.
2. **`spec.md`.** Escribe el problema con un caso concreto del producto. Cada
   criterio de aceptación lleva un identificador (`CA-1`, `CA-2`…) y tiene que
   poder comprobarse con una prueba o con una observación precisa: «responde 404»
   sirve, «funciona bien» no. Declara qué queda **fuera de alcance**.
3. **Paradas obligatorias.** Si la spec toca el contrato `/api/v1/plugin/*`,
   necesita una migración, escribe en el ledger o agrega una dependencia,
   márcalo en la sección «Paradas» y **detente a preguntar** antes del plan.
4. **`plan.md`.** Solo después de aprobada la spec. Lee los ADR que aplican
   (`docs/DECISIONES.md`) y cítalos. Lista los archivos reales que se tocan —
   compruébalos con una búsqueda, no los supongas—. Completa la matriz: cada
   `CA-n` con la prueba que lo verifica. Un criterio sin prueba es un plan
   incompleto.
5. **`tareas.md`.** Una tarea por paso verificable, en orden TDD: primero la
   prueba que falla, luego el código que la hace pasar. Cada tarea dice qué
   agente la hace y cómo se comprueba.
6. **Durante el trabajo.** Marca cada tarea al terminarla (`- [x]`) en el mismo
   commit que la completa. Si lo implementado se aparta de la spec, **corrige la
   spec en el mismo pull request** y anota el motivo en «Cambios durante la
   implementación»: una diferencia sin explicar es un defecto.
7. **Cierre.** Rellena en `tareas.md` la sección «Evidencia» con la salida real
   de los comandos. No escribas un resultado que no ejecutaste.

## Si la spec solo fija comportamiento que ya existe

Pasa cuando el objetivo es proteger con pruebas algo que ya funciona, sin
cambiarlo (como la 001, que fijó los límites del antifraude). Ahí no hay
código de producción que escribir, así que el orden TDD cambia:

- **El rojo no es una prueba que falla**, porque la prueba nueva pasa desde el
  primer momento. El rojo es la evidencia de que el comportamiento estaba sin
  proteger: los mutantes que sobreviven *antes* de añadir la prueba (informe de
  PIT copiado en «Evidencia», porque `target/` se sobrescribe).
- **El verde** es el mismo informe *después*: esos mutantes muertos.
- No hay `dev-backend` ni commit `feat(...)`/`fix(...)`: solo `docs(specs)` y
  `test(...)`. Dilo así en `tareas.md`.
- Para que el `revisor` reproduzca el rojo sin borrar la prueba nueva:
  `mvn -B -Pmutacion -pl <módulo> -am verify -DexcludedTestClasses=<clase>`
  (ver `docs/calidad/README.md`).

## Reglas

- Los criterios describen comportamiento observable, no implementación.
- Una spec cabe en un pull request revisable. Si necesita más de unas diez
  tareas, divídela en dos specs.
- La spec no repite lo que ya está en `AGENTS.md` o en un ADR: lo enlaza.
- Todo en español.
