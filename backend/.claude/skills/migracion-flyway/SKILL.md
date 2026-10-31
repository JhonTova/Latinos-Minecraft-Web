---
name: migracion-flyway
description: Crea una migración Flyway nueva para la base de datos PostgreSQL del backend de LatinosMC siguiendo las convenciones del proyecto (numeración, nombre en español, encabezado que explica el porqué, restricciones en la base de datos). Úsala cuando haya que agregar o cambiar tablas, columnas, índices, restricciones o datos de referencia, o cuando alguien pida "una migración", "cambiar el esquema" o "agregar una columna".
---

# Migración Flyway de LatinosMC

Las migraciones viven en `backend/app/src/main/resources/db/migration/` y se
aplican solas al arrancar la aplicación en producción (`spring.flyway.enabled`,
sin `baseline-on-migrate`). Una migración mal hecha no se puede deshacer
editándola: ya se ejecutó en la base de datos real.

## Pasos

1. **Busca el siguiente número libre.** Lista la carpeta y toma el mayor `V<n>`
   (orden numérico, no alfabético: `V10` va después de `V9`). La nueva es
   `V<n+1>`. Nunca reutilices un número ni dejes huecos.
2. **Nombra el archivo** `V<n+1>__descripcion_en_espanol.sql`: dos guiones
   bajos, todo en minúscula, palabras separadas por `_`, sin tildes ni `ñ`
   (por ejemplo `V27__limite_de_votos_por_ip.sql`).
3. **Escribe el encabezado** con el mismo formato que las migraciones
   existentes:

   ```sql
   -- =============================================================================
   -- V27 — Título corto
   -- =============================================================================
   --
   -- Qué problema resuelve y por qué se hace así. Si se descartó una alternativa
   -- obvia, explica por qué. Si una columna es NULL a propósito, dilo.
   -- =============================================================================
   ```

   El encabezado explica el **porqué**; el SQL ya dice el qué.
4. **Escribe el SQL** siguiendo las reglas de abajo.
5. **Documenta las columnas nuevas** con `COMMENT ON COLUMN ... IS '...';`.
6. **Comprueba:** `mvn -B verify` desde `backend/` (las pruebas de arquitectura
   y el resto deben seguir pasando) y, si cambió una tabla que lee el código,
   actualiza el repositorio Java correspondiente en el mismo PR.

## Reglas

- **Nunca edites ni borres una migración existente.** Para corregir algo, crea
  otra migración que lo arregle.
- **Las claves primarias son `id UUID PRIMARY KEY` sin valor por defecto**: el
  backend genera UUIDv7 con `com.latinosmc.shared.Ids`.
- **Las fechas son `TIMESTAMPTZ`**, normalmente `NOT NULL DEFAULT now()`.
- **Las reglas de los datos van también en la base de datos** (`CHECK`,
  `NOT NULL`, `UNIQUE`, claves foráneas), no solo en la API: si la API falla, la
  base de datos no debe aceptar un dato imposible. Ponle nombre a cada
  restricción: `<tabla>_<que_comprueba>`.
- **Una columna nueva en una tabla con datos es NULL o tiene `DEFAULT`**; si no,
  la migración falla en producción.
- **Cada módulo solo toca sus propias tablas** (ADR-0001). Si la migración
  crea una lectura nueva entre módulos, hay que registrarla en ADR-0019.
- **Nunca `UPDATE` ni `DELETE` sobre tablas del ledger** (`ledger_entries`): el ledger
  es inmutable.
- **Los límites del negocio viven en tablas, no en el código** (por ejemplo,
  `plans.max_servers`): cambiarlos después debe ser un `UPDATE` en una migración.
- **No borres datos personales sin ver ADR-0010** (retención y borrado):
  normalmente se anonimiza a la persona y se conservan los hechos.

## Qué entregar

- El archivo `.sql` nuevo.
- Los cambios en el código Java que lo usa, si hay.
- En la descripción del PR: el número de la migración y si es reversible o no.
