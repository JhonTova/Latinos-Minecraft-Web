---
name: revision-de-pruebas
description: Revisa si las pruebas de un cambio de LatinosMC de verdad lo verifican - arma la matriz criterio de aceptación a prueba y busca pruebas débiles (test smells) en JUnit o Vitest. Úsala al revisar un diff o un pull request, cuando alguien pregunte "¿estas pruebas sirven?", al escribir pruebas a partir de criterios de aceptación, o antes de abrir un pull request de una spec.
---

# Revisión de pruebas

Una prueba escrita por una IA suele compilar, pasar y no comprobar nada. Esta
revisión responde una sola pregunta por prueba: **¿fallaría si el código
estuviera mal?**

## 1. Matriz criterio → prueba

Toma los criterios de `specs/NNN-.../spec.md` y arma la tabla:

| Criterio | Prueba que lo verifica | ¿Suficiente? |
|---|---|---|
| CA-1 | `CastVoteServiceTest#rechazaDentroDeLaVentana` | sí / no, y por qué |

- Un criterio sin prueba es un hallazgo **alto**.
- Una prueba que no corresponde a ningún criterio no es un problema, pero
  pregunta qué protege.
- Si el criterio habla de un límite («a las 24 horas»), tiene que haber una
  prueba justo en el límite y otra un paso antes.

## 2. Lista de pruebas débiles

Para cada prueba nueva o modificada:

| Señal | Cómo se ve | Severidad |
|---|---|---|
| Sin aserción | No hay `assert`/`expect`, o es `assertTrue(true)` | alta |
| Tautológica | Comprueba el mismo valor que acaba de poner en el mock | alta |
| Solo verifica el mock | `verify(repo).save(any())` cuando había un resultado que mirar | media |
| Traga el fallo | `try/catch` sin `fail()`, o `assertDoesNotThrow` como única prueba | alta |
| Hora real | `Instant.now()` o `new Date()` en la prueba en vez de un `Clock` fijo | media |
| Copia la implementación | La prueba calcula lo esperado con la misma fórmula del código | media |
| Solo camino feliz | En un módulo crítico no hay casos de error ni de borde | alta en 🔴 |
| Depende del orden | Estado compartido entre pruebas | media |
| Nombre que no dice nada | `test1`, `funciona` | baja |

## 3. Comprobar que puede fallar

Leer no alcanza. Usa lo que haya disponible, en este orden:

1. **El orden de los commits.** El `test(...)` va antes que el `feat(...)`. Si
   la prueba llegó en el mismo commit que el código, nadie la vio en rojo.
2. **El motivo del rojo.** El informe de quien escribió la prueba dice si falló
   por compilación o por aserción. Un rojo por compilación solo demuestra que la
   clase no existía.
3. **Mutación**, en los módulos críticos del backend, cuando el proyecto la tenga
   configurada: un mutante vivo es una línea que se puede romper sin que ninguna
   prueba lo note. Cada superviviente se clasifica como *falta una prueba* o
   *equivalente* (con el motivo).

## Qué entregar

- La matriz completa.
- Los hallazgos, cada uno con severidad, `archivo:línea`, la señal y **qué rol lo
  corrige** (quien escribe pruebas o quien escribe código).
- Un veredicto: *se puede abrir el pull request* o *vuelve a…*. Un hallazgo alto
  abierto impide abrirlo.

No corrijas tú las pruebas: quien revisa no edita.
