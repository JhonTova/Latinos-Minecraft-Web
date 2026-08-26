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

Lo que queda sin cubrir: **el flujo de OAuth** (`OAuthService`, 317 líneas) y la
**verificación por correo**, ambos dentro de `identity`. Y no hay ningún test
contra una base de datos real —ni CI—, así que todo el SQL está revisado pero no
ejecutado por una máquina ([ADR-0021](docs/decisiones/0021-reintento-de-eventos-de-pago.md)
lo registra como deuda con fecha de pago).

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
│   ├── decisiones/        ← 30 ADRs
│   └── requisitos/        ← los 19 documentos de entrada
├── scripts/               ← actualizar.sh, comprobar.sh
├── backend/               ← Java 21 + Spring Boot (Maven multi-módulo)
├── frontend/              ← Next.js 16 + Tailwind 4
└── plugin/                ← Plugin Paper
```

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
cd backend && mvn verify           # 329 tests
cd plugin  && mvn verify           # 22 tests
cd frontend && npm run lint && npm run build
```

Incluye **tests de arquitectura** que fallan si un módulo cruza una frontera que
no debe. Sin ellos, las reglas se erosionan solas.

---

## El plugin

Es el último eslabón del bucle: pregunta a la plataforma por votos nuevos y
entrega la recompensa dentro del servidor. También da a los jugadores el comando
`/votar` (alias `/vote`, `/votelmc`, `/votarlmc`), que genera su enlace personal.

**Nunca es fuente de verdad.** La plataforma no le manda comandos, le manda un
*tipo* de recompensa; es el dueño del servidor quien decide en `config.yml` qué
comando ejecuta cada tipo. Así, aunque alguien colara texto raro en un nombre de
jugador, no hay nada que inyectar.

### Descarga

| | |
|---|---|
| Versión | **0.1.0** |
| Descarga | [LatinosMC-0.1.0.jar](https://github.com/JhonTova/LatinosMC/releases/download/v0.1.0/LatinosMC-0.1.0.jar) (36 KB) |
| Releases | [JhonTova/LatinosMC](https://github.com/JhonTova/LatinosMC/releases) |
| Requiere | Paper 1.21 o superior, Java 21 |

### Instalación

1. Deja el `.jar` en la carpeta `plugins/` del servidor y arranca.
2. Abre `plugins/LatinosMC/config.yml` y pega en `clave-api` la clave que te dio
   el panel al registrar el servidor. **Solo se muestra una vez**: trátala como
   una contraseña, porque quien la tenga puede recibir tus recompensas.
3. Mapea las recompensas. La mínima es una sola línea:

   ```yaml
   recompensas:
     DEFAULT: "give {player} diamond 1"
   ```

   Los únicos valores que se sustituyen son `{player}` y `{uuid}`, y se validan
   antes de usarse.
4. Reinicia. El plugin manda un latido cada 60 s y pregunta por recompensas cada
   30 s.

### Código fuente

Vive en **dos sitios**: `plugin/` dentro de este monorepo, y el repo público
[JhonTova/LatinosMC](https://github.com/JhonTova/LatinosMC), que es de donde
salen las releases con el `.jar` adjunto.

> ⚠️ Las dos copias se sincronizan **a mano**. Un arreglo hecho aquí no llega al
> repo público solo, y los dueños de servidores seguirían descargando el `.jar`
> viejo. Al tocar `plugin/`, replica y publica release nueva.

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
