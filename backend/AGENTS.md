# Backend de LatinosMC — memoria para agentes de código

Lo general del proyecto (reglas, despliegue, commits) está en el `AGENTS.md` de
la raíz. Aquí va solo lo propio del backend.

API REST en **Java 21 + Spring Boot**, organizada como **monolito modular**
(ADR-0001) con Maven multimódulo. Base de datos PostgreSQL con Flyway.

## Comandos (PowerShell, desde `backend/`)

```powershell
mvn -B verify                         # compila y ejecuta todas las pruebas
mvn -B -pl voting -am verify          # solo un módulo y lo que necesita
mvn -B -pl app -am verify -Dtest=ArquitecturaTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -B install -DskipTests            # empaquetar sin pruebas
java -jar app/target/app-0.1.0-SNAPSHOT.jar   # ejecutar (necesita DB_URL, DB_USER, DB_PASSWORD)
```

Las pruebas usan JUnit 5, Mockito y ArchUnit, y **no necesitan PostgreSQL**:
ninguna prueba toca una base de datos real. Es deuda registrada: ver ADR-0021,
sección «Lo que estos tests NO cubren», y la política de deuda en ADR-0007.
Los rastros de excepciones que salen en la consola durante `verify` son de
pruebas que comprueban errores a propósito: mira el resultado final, no los
rastros.

## Organización

| Módulo | Responsabilidad | Riesgo |
|---|---|---|
| `shared` | Puertos (`shared.ports`), eventos, identidad del jugador, `DomainException`, `Ids` | — |
| `identity` | Cuentas, sesiones, OAuth (Google, Discord), verificación por correo | 🔴 crítico |
| `organizations` | Organizaciones y aislamiento entre ellas | 🟡 |
| `servers` | Directorio de servidores, ping directo, imágenes | 🟡 |
| `voting` | Motor de votos: ventana, token, idempotencia | 🔴 crítico |
| `trust` | Antifraude por reglas, con decisiones explicables | 🔴 crítico |
| `ranking` | Ranking orgánico y los dos tops (ADR-0030) | 🟡 |
| `delivery` | Outbox y entrega de eventos al plugin | 🟡 |
| `billing` | Espacios destacados, PayPal, ledger inmutable | 🔴 crítico |
| `app` | Arranque de Spring, controladores REST (`app.web`), configuración, migraciones | — |

**Qué puede depender de qué:**

- Los módulos de dominio **no se importan entre sí**. Se comunican por eventos o
  por las interfaces de `shared.ports`. Solo `app` conoce a todos y los conecta.
- `ArquitecturaTest` (en `app/src/test`) hace fallar la compilación si un módulo
  cruza esa frontera. Si falla, cambia el diseño; no la regla.
- Las únicas lecturas SQL permitidas sobre tablas de otro módulo están
  inventariadas en ADR-0019 (solo lectura). Agregar una nueva exige actualizar
  ese ADR. Escribir en tablas ajenas está prohibido sin excepción.

**Dónde va cada cosa:**

- Controladores REST: `app/src/main/java/com/latinosmc/app/web/`.
- Migraciones: `app/src/main/resources/db/migration/`. La siguiente es la
  de número más alto más uno (orden numérico: `V10` va después de `V9`), con
  nombre en español y guiones bajos. La skill `migracion-flyway` explica el
  formato completo. Nunca edites una que ya existe.
- Las rutas `/api/v1/plugin/*` son el contrato con el plugin (repositorio
  aparte). No las cambies sin avisarlo en el PR.

## Convenciones del backend

- Las clases del núcleo mezclan inglés y español (`CastVoteService`,
  `CodigoDeVoto`): sigue el idioma de las clases vecinas del mismo paquete.
- El tiempo se inyecta con `java.time.Clock` (bean en `BeansConfig`); nunca uses
  `Instant.now()` directo, porque las pruebas no podrían controlarlo.
- Los errores de negocio se lanzan con `DomainException`.
- Cada clase nueva con lógica lleva su prueba en el mismo módulo, en
  `src/test/java` y en el mismo paquete.
- Los módulos 🔴 críticos exigen pruebas de los casos de borde y de los errores,
  no solo del camino feliz.
- **Formato:** el `pom.xml` declara Spotless (Palantir Java Format), pero el
  código actual no pasa `spotless:check`. **No ejecutes `mvn spotless:apply`**:
  reformatearía cientos de archivos y el diff ocultaría el cambio real. Imita el
  formato del archivo que estás editando.

## Lo que no debes hacer aquí

- No cambies `application.yml` en lo que afecta a producción (perfiles, pool de
  conexiones, URLs) sin pedirlo; los valores reales vienen de variables de
  entorno del `.env`.
- No agregues dependencias de mensajería (Kafka, RabbitMQ): la cola es la tabla
  outbox (ADR-0003).
- No hagas `UPDATE` ni `DELETE` sobre tablas del ledger.
