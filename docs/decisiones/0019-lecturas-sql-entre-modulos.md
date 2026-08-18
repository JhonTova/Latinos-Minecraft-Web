# ADR-0019 — Deuda deliberada: lecturas SQL de solo lectura entre módulos

- **Estado:** ✅ Vigente · **Deuda deliberada y prudente** (ADR-0007)
- **Fecha:** 2026-08-12
- **Responsable:** el equipo de plataforma
- **Fecha de pago:** cuando se extraiga cualquier módulo a un servicio aparte, o cuando esta lista pase de 4 entradas
- **Amplía:** [ADR-0015](0015-antifraude-lee-tabla-votos.md), que registró el primer caso

---

## Contexto

La regla 9 de las convenciones del proyecto dice: *ningún módulo accede a tablas de otro módulo*.
ADR-0015 registró una excepción concreta. Al conectar el bloque de destacados
apareció otra, y al revisarlo salieron más de las que había registradas.

Este ADR las nombra todas. El motivo es la propia regla de deuda técnica: la
deuda **inadvertida** está prohibida, la deliberada no. Una violación sin
registrar es inadvertida por definición, aunque alguien la conociera.

## Inventario

| Módulo | Tabla ajena que lee | Para qué |
|---|---|---|
| `trust` | `votes` (de `voting`) | Historial para las reglas antifraude — ver ADR-0015 |
| `ranking` | `servers`, `votes`, `server_pings`, `server_game_modes` | Componer la fila del ranking en una sola consulta, y acotarla a una modalidad para las páginas por categoría (ADR-0025) |
| `billing` | `servers` | Pintar el nombre y el icono del servidor destacado |

Todas son **de solo lectura**. Ningún módulo escribe en tablas de otro: eso sí
sigue prohibido sin excepción, y es la mitad de la regla que de verdad protege
la integridad de los datos.

## Decisión

Se aceptan estas tres lecturas. Las alternativas —un read model propio por
módulo alimentado por eventos, o un puerto de consulta en `shared` por cada
caso— cuestan bastante más de lo que resuelven hoy:

- El read model duplica datos y mete consistencia eventual en la portada y en la
  ruta crítica del antifraude.
- El puerto de consulta acabaría siendo un reflejo exacto de la consulta que
  necesita quien llama. El acoplamiento sería el mismo, con una capa de
  indirección que solo lo disimula.

Con un monolito modular (ADR-0001) desplegado como un proceso y una base de
datos, el coste real de estas lecturas hoy es cero.

## Cuándo deja de ser aceptable

En el momento en que **cualquier módulo se extraiga a un servicio aparte**, cada
una de estas lecturas se convierte en una llamada de red o en una réplica. Ese
es el día de pagar la deuda, y este ADR es la lista de lo que hay que tocar.

También si la lista crece: cuatro entradas significan que la frontera ya no está
donde creemos y hay que replantear el reparto de tablas, no seguir apuntando
excepciones.

## Lo que NO se acepta

- **Escrituras** entre módulos. Sin excepción.
- Lecturas nuevas sin añadirlas a la tabla de arriba. Una lectura sin registrar
  es deuda inadvertida, que está prohibida (ADR-0007).
- Que la aplicación entera se salte la frontera "porque ya hay excepciones". Las
  tres de arriba son las tres de arriba.

## Consecuencias

**A favor:** consultas simples y rápidas, sin duplicar datos ni introducir
consistencia eventual donde importa.

**En contra:** un cambio de esquema en `votes`, `servers` o `server_pings` puede
romper un módulo que no es su dueño. Los tests de integración lo detectan, pero
el compilador no — que es justo lo que hace que esto sea deuda y no diseño.

**Lo que esta deuda NO cubre:** la separación sigue siendo real en el código
Java. `ArquitecturaTest` comprueba que ningún módulo importa clases de otro, y
esa regla no tiene excepciones. Lo que se relaja aquí es solo el acceso SQL.
