# Calidad del código que escribe la IA

Qué controles tiene este repositorio sobre el código generado, cómo se ejecutan
y dónde queda su evidencia. La decisión de fondo está en
[ADR-0032](../decisiones/0032-cobertura-con-trinquete-y-mutacion.md) y en el
[sistema de agentes](../agentes/README.md).

## Los mecanismos

| # | Mecanismo | Qué detecta | Cómo se ejecuta | Dónde queda la evidencia |
|---|---|---|---|---|
| 1 | **TDD visible en el historial** | Pruebas escritas después del código para que pasen | El commit `test(...)` en rojo va antes que el `feat(...)` | `git log` de cada rama; sección «Evidencia» del `tareas.md` de la spec |
| 2 | **Matriz criterio → prueba** | Un criterio de aceptación que nadie verifica | Skill `revision-de-pruebas`; la revisa el agente `revisor` | `plan.md` de cada spec y la descripción del pull request |
| 3 | **CI en cada pull request** | Código que no compila o rompe una prueba | `.github/workflows/ci.yml`: trabajos `backend`, `frontend`, `arnes` y `migraciones` | Checks del pull request; `proteger-main` exige que pasen |
| 4 | **Cobertura con trinquete (JaCoCo)** | Código nuevo sin pruebas en un módulo crítico | `mvn -B verify` falla si un módulo crítico baja de su suelo | Resumen del trabajo `backend` y artefacto `cobertura-jacoco` |
| 5 | **Mutación (PIT)** | Pruebas que ejecutan el código sin comprobarlo | `mvn -B -Pmutacion -pl <módulo> -am verify`; en la CI, trabajo `mutacion` | Artefacto `mutacion-pit`; supervivientes clasificados en el `tareas.md` |
| 6 | **Migraciones intactas** | Editar una migración que ya se aplicó en producción | Trabajo `migraciones` de la CI y hook de los agentes | Check del pull request |
| 7 | **Permisos por hooks** | Un agente que edita fuera de su carpeta o ejecuta lo que no es de su rol | Hooks `PreToolUse`; su política tiene pruebas (`node --test .claude/hooks/`) | Trabajo `arnes` de la CI |

## Cómo ejecutarlos en local

```powershell
# Pruebas + cobertura con su suelo (desde backend/)
mvn -B verify
# Informe: backend/<módulo>/target/site/jacoco/index.html

# Mutación de un módulo crítico (desde backend/)
mvn -B -Pmutacion -pl trust -am verify
# Informe: backend/trust/target/pit-reports/index.html

# Pruebas del frontend (desde frontend/)
npm run test

# Política de permisos de los agentes (desde la raíz)
node --test .claude/hooks/
```

### Reproducir el «antes» de una mutación sin borrar la prueba

El informe de PIT queda en `target/`, que git ignora y que se sobrescribe en
cada ejecución. Para ver qué mutantes sobrevivían antes de una prueba nueva, sin
borrarla, se excluye su clase:

```powershell
# Desde backend/: mutación de trust como si RuleBasedTrustEngineLimitesTest no existiera
mvn -B -Pmutacion -pl trust -am verify "-DexcludedTestClasses=com.latinosmc.trust.RuleBasedTrustEngineLimitesTest"
```

Así reprodujo el `revisor` el rojo de la spec 001: los mismos 10 supervivientes
que había antes de la prueba. Como `target/` no se versiona, la salida se copia
a la «Evidencia» del `tareas.md` de la spec.

## Línea base (13 de noviembre de 2026)

Primera medición, sobre las 329 pruebas que ya existían y antes de las specs.
Sirve para comparar: cada spec que toca un módulo crítico anota en su
`tareas.md` cómo quedó después.

### Cobertura de líneas

| Módulo | Líneas cubiertas | Total | Cobertura | Suelo |
|---|---|---|---|---|
| `identity` 🔴 | 64 | 414 | 15,5 % | 15 % |
| `billing` 🔴 | 96 | 308 | 31,2 % | 31 % |
| `voting` 🔴 | 120 | 331 | 36,3 % | 36 % |
| `trust` 🔴 | 33 | 57 | 57,9 % | 57 % |
| `shared` | 61 | 80 | 76,2 % | solo informe |
| `servers` | 661 | 1371 | 48,2 % | solo informe |
| `app` | 531 | 1543 | 34,4 % | solo informe |

`organizations`, `ranking` y `delivery` no tienen pruebas propias, así que
JaCoCo no genera informe para ellos.

### Mutación

| Módulo | Mutantes | Muertos | Sin cobertura | Sobreviven con pruebas | Fuerza de las pruebas |
|---|---|---|---|---|---|
| `identity` | 141 | 34 (24 %) | 106 | 1 | 97 % |
| `voting` | 103 | 52 (50 %) | 50 | 1 | 98 % |
| `billing` | 71 | 26 (37 %) | 45 | 0 | 100 % |
| `trust` | 29 | 11 (38 %) | 8 | **10** | **52 %** |

«Fuerza de las pruebas» es el porcentaje de mutantes muertos entre los que sí
alcanza alguna prueba.

### Lo que dice esta medición

- **El hueco más grande es código que ninguna prueba ejecuta**, sobre todo los
  repositorios JDBC. Es la deuda de ADR-0021 (no hay pruebas con PostgreSQL
  real), ahora con cifra.
- **Donde hay pruebas, casi siempre comprueban algo**: en `identity`, `voting` y
  `billing` casi ningún mutante cubierto sobrevive.
- **La excepción es el antifraude.** En `trust`, 10 mutantes sobreviven dentro de
  `RuleBasedTrustEngine.evaluar`: se pueden mover los límites de las reglas o
  cambiar la aritmética del puntaje sin que falle ninguna prueba. La cobertura
  de `trust` es la más alta de los módulos críticos y aun así no lo delataba; la
  mutación sí. Se corrige en una spec propia.
