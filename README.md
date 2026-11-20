# LatinosMC

Plataforma de votaciones y ranking para servidores de Minecraft.

**El bucle central:** un jugador vota en el directorio → validamos que el voto es
legítimo → avisamos al servidor → el plugin entrega la recompensa → el ranking se
actualiza → el directorio manda tráfico al servidor.

El producto real es **la credibilidad del ranking**. Si el ranking se puede
comprar o falsear, el directorio pierde tráfico y la plataforma no vale nada.

---

## Estado actual

| Pieza | Estado |
|---|---|
| Motor de votos (ventana, idempotencia, entrega) | ✅ Funcionando |
| Antifraude por reglas con decisiones explicables | ✅ Funcionando |
| Directorio de servidores + verificación por plugin | ✅ Funcionando |
| Ranking orgánico + bloque patrocinado separado | ✅ Funcionando |
| Outbox y entrega fiable de eventos | ✅ Funcionando |
| Plugin Paper con persistencia anti-duplicados | ✅ Compila y probado |
| Web pública (Next.js) | ✅ Funcionando |
| Cuentas, login (correo + OAuth) y panel de propietario | ✅ Funcionando |
| Panel de administración de la plataforma | ✅ Funcionando |
| Slots destacados: cobro por PayPal, cola y calendario | ✅ Funcionando |
| Ping directo a servidores | ✅ Funcionando |

Lo que queda sin cubrir: **el flujo de OAuth** (`OAuthService`, 317 líneas),
dentro de `identity`. Y no hay ninguna prueba contra una base de datos real, así
que todo el SQL está revisado pero no ejecutado por una máquina
([ADR-0021](docs/decisiones/0021-reintento-de-eventos-de-pago.md) lo registra
como deuda con fecha de pago). La verificación por correo ya tiene pruebas
(spec 004).

Para abrir al público, la lista es [LANZAMIENTO.md](LANZAMIENTO.md). Para salir
en Google, [docs/POSICIONAMIENTO.md](docs/POSICIONAMIENTO.md).

---

## Estructura

```
LatinosMC/
├── LANZAMIENTO.md         ← qué hacer antes de abrir al público
├── docker-compose.yml     ← despliegue completo (db, api, web)
├── docs/
│   ├── DECISIONES.md      ← decisiones vigentes consolidadas
│   ├── POSICIONAMIENTO.md ← cómo salir en Google
│   ├── decisiones/        ← 32 ADRs
│   └── requisitos/        ← los 19 documentos de entrada
├── scripts/               ← actualizar.sh, comprobar.sh
├── backend/               ← Java 21 + Spring Boot (Maven multi-módulo)
└── frontend/              ← Next.js 16 + Tailwind 4
```

El plugin **no está en este repositorio**: vive en
[JhonTova/LatinosMC](https://github.com/JhonTova/LatinosMC). Ver
[El plugin](#el-plugin).

---

## Arrancar en local

Requisitos: **Java 21**, **Maven**, **Node 22+**, **PostgreSQL 18**.

```bash
# 1. Base de datos
createdb latinosmc
psql -c "CREATE ROLE latinosmc LOGIN PASSWORD 'latinosmc_dev'"
psql -c "ALTER DATABASE latinosmc OWNER TO latinosmc"

# 2. Backend
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/latinosmc
export DB_USER=latinosmc
export DB_PASSWORD=latinosmc_dev
mvn install -DskipTests
mvn -pl app flyway:migrate         # aplica el esquema
java -jar app/target/app-0.1.0-SNAPSHOT.jar

# 3. Frontend (otra terminal)
cd frontend
npm install
npm run dev                        # http://localhost:3000
```

## Tests

```bash
cd backend && mvn verify           # pruebas, arquitectura y cobertura con suelo
cd frontend && npm run lint && npm run test && npm run build
```

En el frontend, `npm run test` ejecuta Vitest sobre la lógica sin interfaz y
sobre la lista blanca del puente `/api/backend`
([ADR-0031](docs/decisiones/0031-vitest-en-el-frontend.md)).

Incluye **tests de arquitectura** que fallan si un módulo cruza una frontera que
no debe. Sin ellos, las reglas se erosionan solas.

**Integración continua.** [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
ejecuta lo mismo en cada pull request hacia `main` y en cada push a `main`:
`mvn -B verify` en el backend y `npm ci`, `npm run lint`, `npm run test` y
`npm run build` en el frontend. Son trabajos separados que corren en paralelo, y cada uno solo se
ejecuta si cambió código de su carpeta (los cambios solo de Markdown no
cuentan). Si falla el backend, los reportes de las pruebas
quedan como artefacto de la ejecución durante 7 días.

Dos trabajos más protegen lo que las pruebas no ven: `migraciones` falla si un
pull request modifica, renombra o borra una migración Flyway que ya está en
`main` (ya se aplicó en producción), y `arnes` ejecuta las pruebas de los hooks
de permisos de los agentes cuando cambia `.claude/hooks/`.

El ruleset `proteger-main` exige pull request y los checks `cambios`, `backend`
y `frontend` para fusionar a `main`.

---

## El arnés: cómo trabaja la IA en este repositorio

El arnés es todo lo que rodea al modelo para que trabaje bien aquí: lo que lee
al iniciar, lo que carga cuando lo necesita, los roles que lo ejecutan, el flujo
que sigue y los controles que verifican lo que produce. La herramienta es
**Claude Code**. Todo se versiona y cambia por pull request, igual que el código.

### Piezas y dónde está cada una

| Pieza | Dónde | Cuándo actúa |
|---|---|---|
| Memoria del proyecto | [`AGENTS.md`](AGENTS.md), [`backend/AGENTS.md`](backend/AGENTS.md), [`frontend/AGENTS.md`](frontend/AGENTS.md) | Al iniciar cada sesión (la de la raíz) y al trabajar en cada carpeta |
| Reglas | [`.claude/rules/`](.claude/rules/) | `git-y-pull-requests.md` siempre; `backend-java`, `backend-pruebas`, `frontend` y `migraciones`, al tocar ese tipo de archivo (`paths:`) |
| Skills | [`.claude/skills/`](.claude/skills/), [`backend/.claude/skills/`](backend/.claude/skills/), [`frontend/.claude/skills/`](frontend/.claude/skills/) | Solo cuando la tarea las necesita |
| Sistema de agentes | [`.claude/agents/`](.claude/agents/): ocho roles | Con `claude --agent orquestador` desde la raíz |
| Permisos por rol | [`.claude/hooks/`](.claude/hooks/), registrados en [`.claude/settings.json`](.claude/settings.json) | En cada edición y cada comando de un agente |
| Specs (SDD) | [`specs/`](specs/) | Una carpeta por funcionalidad |
| Decisiones | [`docs/decisiones/`](docs/decisiones/) e índice en [`docs/DECISIONES.md`](docs/DECISIONES.md) | Antes de proponer un cambio de diseño |
| Calidad | [`docs/calidad/README.md`](docs/calidad/README.md) y [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | En cada pull request |

No hay `constitution.md` porque no se usa Spec Kit: los principios ya estaban en
`AGENTS.md` y los ADR ([por qué](specs/README.md)).

### Para qué sirve cada skill

| Skill | Qué problema resuelve aquí | Cuándo se invoca | La usa |
|---|---|---|---|
| [`spec-sdd`](.claude/skills/spec-sdd/SKILL.md) | Que cada funcionalidad tenga spec, plan y tareas con la misma estructura, y criterios que se puedan probar | Al empezar una funcionalidad | `orquestador` |
| [`revision-de-pruebas`](.claude/skills/revision-de-pruebas/SKILL.md) | Que una prueba que pasa no se tome por una prueba que verifica: matriz criterio → prueba y pruebas débiles | Al escribir pruebas desde los criterios y al revisar un diff | `pruebas-backend`, `pruebas-frontend`, `revisor` |
| [`adr-nuevo`](.claude/skills/adr-nuevo/SKILL.md) | Que una decisión (dependencia, contrato, deuda) quede escrita con su porqué y en el índice | Cuando se decide algo de arquitectura | `orquestador` (la invoca cuando hace falta; no la carga de entrada) |
| [`migracion-flyway`](backend/.claude/skills/migracion-flyway/SKILL.md) | Que un cambio de esquema sea una migración nueva y bien numerada, nunca una edición de una aplicada | Al cambiar tablas o columnas | `dev-backend` |
| [`ruta-del-puente`](frontend/.claude/skills/ruta-del-puente/SKILL.md) | Abrir un endpoint al navegador sin abrir de más: lista blanca con el patrón más estrecho y su prueba | Cuando el navegador necesita un endpoint nuevo | `dev-frontend` |
| [`vercel-react-best-practices`](frontend/.claude/skills/vercel-react-best-practices/SKILL.md) | Rendimiento en React y Next.js. Es de terceros (Vercel) y genérica: si choca con `frontend/AGENTS.md`, gana el proyecto | Al escribir o revisar componentes | `dev-frontend` |

### Cómo se usa

```powershell
# Desde la raíz del repositorio
claude --agent orquestador
```

Se le pide la funcionalidad y sigue este orden, deteniéndose en los pasos
marcados con ✋:

1. **Spec** con `spec-sdd` → ✋ aprobación.
2. **Mapa** del `analista` (módulos, riesgo, contrato) → ✋ si hay una parada
   obligatoria: contrato del plugin, migración, ledger o dependencia nueva.
3. **Plan y tareas** → ✋ aprobación. Commit `docs(specs)`.
4. **Rojo:** `pruebas-backend` / `pruebas-frontend`. Commit `test(...)`.
5. **Verde:** `dev-backend` / `dev-frontend`. Commit `feat(...)` o `fix(...)`.
   `verificador-ui` si hay pantalla.
6. **Revisión:** `revisor` sobre el diff completo, con mutación si toca un
   módulo crítico. Un hallazgo alto impide abrir el PR.
7. **PR** con la descripción en `specs/NNN-*/pr.md`. **La fusión la hace una
   persona.**

Detalle de cada rol, sus permisos y su modelo: [docs/agentes/README.md](docs/agentes/README.md).

### Las cinco specs

| Spec | Qué hizo | PR |
|---|---|---|
| [001](specs/001-fijar-motor-antifraude/) | Fijó con pruebas los límites del antifraude: los 10 mutantes vivos de `RuleBasedTrustEngine` pasaron a muertos, sin cambiar el comportamiento | [#15](https://github.com/JhonTova/Latinos-Minecraft-Web/pull/15) |
| [002](specs/002-espera-voto-diario/) | La web dice cuánto falta cuando el jugador ya usó su voto del día | [#16](https://github.com/JhonTova/Latinos-Minecraft-Web/pull/16) |
| [003](specs/003-ya-voto-aqui/) | El plugin se entera de que el jugador ya votó antes de darle el código (`YA_VOTO_AQUI`) | [#17](https://github.com/JhonTova/Latinos-Minecraft-Web/pull/17) |
| [004](specs/004-intentos-verificacion-correo/) | Intentos restantes al verificar el correo. Destapó y corrigió un fallo de seguridad: el límite de 5 intentos nunca se aplicaba | [#19](https://github.com/JhonTova/Latinos-Minecraft-Web/pull/19) |
| [005](specs/005-reintento-voto-espera/) | El reintento idempotente de un voto aceptado devuelve el tiempo que falta | [#21](https://github.com/JhonTova/Latinos-Minecraft-Web/pull/21) |

Cada `tareas.md` cuenta lo que pasó de verdad, incluido «Qué hubo que
corregirle a la IA» y lo que no funcionó del sistema de agentes.

### Qué cambió respecto del taller

El diseño del taller está en
[docs/agentes/Taller-Sistema-de-Agentes-Jhon-Oliveros.pdf](docs/agentes/Taller-Sistema-de-Agentes-Jhon-Oliveros.pdf).
Los doce cambios, cada uno con su motivo y lo observado, están en
[docs/agentes/README.md](docs/agentes/README.md#cambios-respecto-del-diseño-del-taller).
Los más importantes:

- **Los hooks de permisos pasaron del frontmatter de cada agente a
  `.claude/settings.json`** (cambio 7). Una sonda antes de la primera spec
  mostró que en `claude -p` los del frontmatter no corrían y el sistema fallaba
  abierto sin avisar.
- **El suelo de cobertura es un trinquete, no un 80 %** (cambio 6). Con la
  cobertura real de partida, un 80 % habría dejado la CI en rojo desde el
  primer día (ADR-0032).
- **Lo aprendido en las specs** (cambios 8 a 12): el `verificador-ui` no tuvo
  navegador, el `revisor` ahora compila el frontend, una spec que solo fija
  comportamiento tiene otro tipo de rojo, y el hook dice qué forma de comando
  sí pasa.

### Calidad del código que escribe la IA

| Mecanismo | Evidencia |
|---|---|
| TDD visible en el historial: `test(...)` en rojo antes que `feat`/`fix` | `git log` de cada PR y «Evidencia» de cada `tareas.md` |
| Matriz criterio → prueba | `plan.md` y descripción de cada PR |
| CI en cada PR, exigida por `proteger-main` | Checks de los PR #15–#21 |
| Cobertura con trinquete (JaCoCo) | `cobertura.suelo` en cada `pom.xml` crítico; subió en las specs 003, 004 y 005 |
| Mutación (PIT) con supervivientes clasificados | Trabajo `mutacion` de la CI y «Evidencia» de cada `tareas.md` |
| Revisión independiente (`revisor`) | Hallazgos y cómo se atendieron, en cada `tareas.md` |
| Permisos por rol con pruebas | `node --test .claude/hooks/` en el trabajo `arnes` |

Cómo ejecutarlos y la línea base: [docs/calidad/README.md](docs/calidad/README.md).

---

## El plugin

Es el último eslabón del bucle: pregunta a la plataforma por votos nuevos y
entrega la recompensa dentro del servidor.

Su código **ya no está en este repositorio**. Vive en el repositorio público
[JhonTova/LatinosMC](https://github.com/JhonTova/LatinosMC), que es la fuente de
verdad y de donde salen las releases con el `.jar`. Allí está también la
documentación para quien lo instala: requisitos, configuración y comandos.

Estaba duplicado en los dos sitios y las copias se sincronizaban a mano, así que
un arreglo hecho aquí no llegaba al `.jar` que descargan los dueños de
servidores. Ahora solo hay una copia.

```bash
git clone https://github.com/JhonTova/LatinosMC.git
cd LatinosMC && mvn verify          # 22 tests
```

El contrato entre ambos son las cuatro rutas `/api/v1/plugin/*`. Al cambiarlas
en el backend hay que actualizar el plugin en su repositorio y publicar una
release nueva.

---

## Cómo se trabaja aquí

Las decisiones vivas están en [docs/DECISIONES.md](docs/DECISIONES.md) y en los
ADRs. Lo esencial:

1. **El plugin nunca es fuente de verdad.** Corre en hardware del cliente, que es
   quien tiene incentivo para hacer trampas.
2. **Nunca concatenar texto para formar comandos de Minecraft.**
3. **El ledger es inmutable.** Solo se añaden asientos.
4. **Lo patrocinado nunca se mezcla con el ranking orgánico.**
5. **Ningún módulo accede a tablas de otro módulo** (una excepción registrada en
   [ADR-0015](docs/decisiones/0015-antifraude-lee-tabla-votos.md)).
6. **Toda pantalla necesita sus estados:** cargando, vacío, error, sin permisos.

El proceso es proporcional al riesgo: máxima exigencia en votos, antifraude,
ledger, pagos y autenticación; ligera en interfaz y textos.
