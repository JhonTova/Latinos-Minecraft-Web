# LatinosMC — memoria del proyecto para agentes de código

Plataforma de votaciones y ranking para servidores de Minecraft, publicada en
`latinosmc.com`. Este repositorio es un **monorepo**: `backend/` y `frontend/`
tienen cada uno su propio `AGENTS.md` con lo específico de esa parte. Lee este
primero y luego el de la carpeta en la que vayas a trabajar.

El bucle del producto: el jugador vota → el backend valida el voto con reglas
antifraude → se avisa al servidor → el plugin entrega la recompensa → el ranking
se actualiza. **El producto real es la credibilidad del ranking**: cualquier
cambio que permita comprarlo o falsearlo está mal, aunque genere ingresos.

## Stack

| Parte | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot, Maven multimódulo (Gradle no está instalado) |
| Base de datos | PostgreSQL + Flyway; la cola de eventos es una tabla outbox (ni Kafka ni RabbitMQ) |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind 4 |
| Despliegue | Docker Compose en un VPS (`docker-compose.yml`); Kubernetes no |

El plugin de Minecraft **no está aquí**: vive en
[JhonTova/LatinosMC](https://github.com/JhonTova/LatinosMC). Su contrato con
este repositorio son las rutas `/api/v1/plugin/*`. Si cambias una de esas rutas,
avisa en el PR: hay que actualizar el plugin y publicar una release.

## Comandos (PowerShell, desde la raíz del repositorio)

```powershell
# Backend: compilar y ejecutar todas las pruebas (incluye las de arquitectura)
cd backend; mvn -B verify; cd ..

# Frontend: instalar, revisar y compilar
cd frontend; npm ci; npm run lint; npm run build; cd ..

# Frontend en desarrollo (http://localhost:3000)
cd frontend; npm run dev

# Todo junto en contenedores (necesita un .env copiado de .env.example)
docker compose up -d --build
```

Un cambio está terminado cuando `mvn -B verify` (si tocaste `backend/`) y
`npm run lint` + `npm run build` (si tocaste `frontend/`) pasan. Las pruebas del
backend no necesitan una base de datos real. La CI
(`.github/workflows/ci.yml`) ejecuta lo mismo en cada pull request.

Los comandos están en PowerShell porque el equipo trabaja en Windows; `mvn`,
`npm` y `java` se escriben igual en bash.

## Cómo se despliega (importante)

En el servidor, `scripts/actualizar.sh` hace `git pull` de `main` y reconstruye
los contenedores. **Lo que llega a `main` llega a producción**, así que:

- Nunca hagas commit directo a `main`: rama corta + pull request.
- `main` tiene que compilar y pasar las pruebas siempre.
- Un cambio de esquema es una migración Flyway nueva (el siguiente número
  libre en `backend/app/src/main/resources/db/migration/`); nunca edites una migración que ya existe, porque ya se
  aplicó en producción.

## Reglas que no se negocian

1. **El plugin nunca es fuente de verdad.** Corre en el hardware del dueño del
   servidor, que es quien tiene incentivo para hacer trampa. Todo dato que afecta
   al ranking lo mide o lo genera la plataforma (por ejemplo, los jugadores
   conectados se miden con ping directo).
2. **Nunca concatenes texto para formar comandos de Minecraft**: lista blanca de
   comandos y parámetros tipados (ADR-0012).
3. **El ledger financiero es inmutable**: solo se agregan asientos; nunca
   `UPDATE` ni `DELETE` sobre dinero.
4. **Lo patrocinado nunca se mezcla con el ranking orgánico** (ADR-0013).
5. **Ningún módulo del backend lee tablas de otro módulo.** Las excepciones de
   solo lectura están en ADR-0015, ampliado por ADR-0019. Las pruebas de
   arquitectura hacen cumplir la frontera entre módulos.
6. **Toda validación y autorización ocurre en el backend.** El frontend no es de
   fiar.
7. **Toda pantalla necesita sus estados**: cargando, vacío, error y sin permisos.

## Convenciones del equipo

- **Idioma:** documentación, comentarios, textos de la interfaz y mensajes de
  commit en español. Los comentarios explican el *porqué*, no el *qué*.
- **Commits:** Conventional Commits en español y en minúscula, con ámbito:
  `fix(comprobar): ...`, `docs(readme): ...`, `feat(frontend): ...`.
- **Ramas:** cortas, creadas desde `main`: `feat/...`, `fix/...`, `chore/...`,
  `docs/...`. Todo entra a `main` por pull request.
- **Decisiones:** toda decisión de arquitectura va a un ADR nuevo en
  `docs/decisiones/` (siguiente número libre). Un ADR viejo no se borra: se marca
  como reemplazado. Las decisiones vigentes están resumidas en
  `docs/DECISIONES.md`; léelo antes de proponer un cambio de diseño.
- **Proceso proporcional al riesgo:** máxima exigencia (pruebas completas y
  revisión de seguridad) en votos, antifraude, ledger, pagos y autenticación;
  ligera en interfaz y textos.

## Lo que el agente no debe hacer

- No toques `.env` ni ningún secreto. Las variables nuevas se documentan en
  `.env.example`, sin valores reales.
- No modifiques `docker-compose.yml`, los `Dockerfile` ni `scripts/` sin que te
  lo pidan explícitamente: son la configuración de producción.
- No edites migraciones Flyway existentes ni borres ADRs.
- No construyas lo que está fuera de alcance en la v1: subastas, marketplace,
  wallet o créditos prepago, afiliados, Kubernetes, Kafka.
- `CLAUDE.md`, `DESPLIEGUE.md`, `*.local.md` y `.claude/` (salvo
  `.claude/skills/`) son archivos locales de cada desarrollador y están en
  `.gitignore` a propósito: no los agregues al repositorio.
