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
cd backend && mvn verify           # 329 tests
cd frontend && npm run lint && npm run build
```

Incluye **tests de arquitectura** que fallan si un módulo cruza una frontera que
no debe. Sin ellos, las reglas se erosionan solas.

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
