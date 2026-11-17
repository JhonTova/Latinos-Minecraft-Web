---
paths:
  - "backend/**/src/test/**/*.java"
---

# Pruebas del backend

Se carga al leer o editar una prueba de `backend/`.

- JUnit 5, Mockito y AssertJ. **Ninguna prueba usa una base de datos real** ni
  la red: los repositorios se sustituyen por dobles.
- La prueba vive en el mismo módulo y en el mismo paquete que la clase que
  prueba, en `src/test/java`.
- Nombre del método en español, que describa el comportamiento:
  `rechazaElVotoSiLaVentanaSigueAbierta`. Usa `@DisplayName` cuando ayude a leer
  el informe.
- **El tiempo se fija** con `Clock.fixed(...)`; una prueba que depende de la
  hora real falla un día sin que nadie haya tocado nada.
- **Una prueba tiene que poder fallar.** Antes de dar por buena una prueba
  nueva, comprueba que falla por la razón esperada sin el código (Red). Evita:
  - aserciones que repiten lo que se acaba de preparar en el mock;
  - `verify(...)` como única comprobación cuando hay un resultado que mirar;
  - pruebas sin aserción o con `assertTrue(true)`;
  - un `try/catch` que se traga el fallo.
- En los módulos críticos (`identity`, `voting`, `trust`, `billing`) se prueban
  los bordes y los errores, no solo el camino feliz: el límite exacto de una
  ventana, el valor nulo, el duplicado, el segundo intento.
- Los rastros de excepción en la consola pueden venir de pruebas que provocan
  errores a propósito. Mira el resumen final de Maven.
