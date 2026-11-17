# ADR-0032 — Cobertura con trinquete y mutación en los módulos críticos

- **Estado:** ✅ Vigente
- **Fecha:** 2026-11-13
- **Relacionado:** ADR-0007 (deuda técnica), ADR-0021 (lo que las pruebas no cubren), `docs/agentes/README.md`, `docs/calidad/README.md`

## Contexto

Buena parte del código de este repositorio lo escribe un agente de IA. Las 329
pruebas del backend pasan, pero eso dice poco por sí solo: una prueba generada
puede ejecutar el código sin comprobar nada, y nunca se había medido ni cuánto
código ejecutan las pruebas ni si notarían un cambio de comportamiento.

El diseño del sistema de agentes proponía un suelo del **80 % de líneas** en los
módulos críticos (`identity`, `voting`, `trust`, `billing`). Al medir por primera
vez, el 13 de noviembre de 2026, la cobertura de líneas resultó ser:

| Módulo | Cobertura de líneas |
|---|---|
| `identity` | 15,5 % |
| `billing` | 31,2 % |
| `voting` | 36,3 % |
| `trust` | 57,9 % |

La causa principal es conocida: los repositorios JDBC no tienen pruebas porque
ninguna prueba usa una base de datos real (ADR-0021). Un suelo del 80 % habría
dejado la CI en rojo desde el primer día, que es lo que ya pasó con Spotless, y
la salida fácil habría sido escribir pruebas solo para subir la cifra.

## Decisión

**Cobertura con JaCoCo, con trinquete.**

- JaCoCo mide todos los módulos en `mvn verify` y deja el informe en
  `<módulo>/target/site/jacoco/`.
- Cada módulo crítico tiene un suelo (`cobertura.suelo` en su `pom.xml`) igual a
  su cobertura medida, redondeada hacia abajo. Si baja, `mvn verify` falla.
- El suelo **solo sube**: una spec que mejora la cobertura de un módulo crítico
  sube su suelo en el mismo pull request. La meta sigue siendo el 80 %.
- En los demás módulos el suelo es cero: JaCoCo solo informa.

**Mutación con PIT, solo en los módulos críticos.**

- Perfil de Maven `mutacion`, que no corre en el `verify` normal:
  `mvn -B -Pmutacion -pl <módulo> -am verify`.
- El informe queda en `<módulo>/target/pit-reports/`.
- No hay un umbral que haga fallar la compilación. La regla es de proceso: en un
  pull request que toca un módulo crítico, cada mutante superviviente **de las
  clases tocadas** se clasifica como *falta una prueba* (y se escribe) o
  *equivalente* (y se dice por qué). La clasificación va en el `tareas.md` de la
  spec.
- La CI ejecuta la mutación cuando el pull request toca un módulo crítico y sube
  el informe como artefacto. Es informativa: no bloquea la fusión.

## Alternativas descartadas

- **Suelo del 80 % desde ya.** CI en rojo permanente o pruebas escritas para la
  cifra. Las dos cosas enseñan a ignorar el control.
- **Excluir los repositorios JDBC de la medición.** Daría una cifra más alta sin
  que nada estuviera mejor probado, y escondería la deuda de ADR-0021.
- **Umbral de mutación que bloquea.** Hay mutantes equivalentes que ninguna
  prueba puede matar; un umbral obligaría a perseguirlos. Clasificarlos exige
  leerlos, que es lo que se busca.
- **Mutar los diez módulos.** Tarda demasiado para lo que aporta fuera del
  núcleo que protege la credibilidad del ranking.

## Consecuencias

- La cobertura de los módulos críticos ya no puede empeorar sin que alguien lo
  decida a la vista, bajando un número en un `pom.xml` dentro de un pull request.
- El suelo actual es bajo y hay que decirlo así: no certifica calidad, solo
  impide retroceder.
- `mvn verify` tarda unos segundos más por la instrumentación.
- Dos complementos de compilación más que mantener al día. Ninguno llega al
  artefacto de producción.
