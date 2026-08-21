-- =============================================================================
-- V21 — Icono y banner automáticos desde el ping (ADR-0027)
-- =============================================================================
--
-- El Server List Ping que ya ejecutamos cada minuto para medir jugadores
-- conectados devuelve, en la misma respuesta, el icono del servidor y su MOTD.
-- Los estábamos descartando. Con ellos se puede montar la ficha entera de un
-- servidor sin que su dueño abra un editor de imágenes.
--
-- El dato lo controla el dueño del servidor, así que NO alimenta el ranking ni
-- nada que puntúe (ADR-0009). Es identidad visual, y de eso el dueño es la
-- fuente legítima.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Origen de cada imagen
-- -----------------------------------------------------------------------------
--
-- Dos columnas y no un único interruptor "modo automático": icono manual con
-- banner automático es una combinación normal, y con un solo interruptor sería
-- imposible.

CREATE TABLE image_sources (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO image_sources (code, name) VALUES
    ('AUTO',   'Automática, desde el servidor'),
    ('MANUAL', 'Subida por el dueño');

ALTER TABLE servers
    ADD COLUMN icon_source   TEXT NOT NULL DEFAULT 'AUTO'
        REFERENCES image_sources (code),
    ADD COLUMN banner_source TEXT NOT NULL DEFAULT 'AUTO'
        REFERENCES image_sources (code),

    -- Claves separadas de icon_key / banner_key a propósito. Volver al modo
    -- automático NO puede destruir lo que subió el dueño: alguien que pagó a un
    -- diseñador y prueba el automático por curiosidad tiene que poder volver.
    ADD COLUMN icon_key_auto   TEXT,
    ADD COLUMN banner_key_auto TEXT;

COMMENT ON COLUMN servers.icon_source IS
    'AUTO usa icon_key_auto (favicon del ping reprocesado); MANUAL usa icon_key (ADR-0027).';
COMMENT ON COLUMN servers.banner_source IS
    'AUTO usa banner_key_auto (banner generado del MOTD); MANUAL usa banner_key (ADR-0027).';

-- -----------------------------------------------------------------------------
-- Qué imagen se enseña
-- -----------------------------------------------------------------------------
--
-- Columnas generadas y no un CASE repetido en cada consulta. La regla la
-- necesitan tres módulos —el directorio, el ranking y los destacados—, y una
-- regla copiada en tres sitios es una regla que acabará siendo dos: alguien
-- añade la cuarta consulta y se olvida, y ese servidor enseña la imagen que no
-- es sin que nadie entienda por qué.
--
-- Aquí la calcula PostgreSQL: es imposible leer la columna y saltarse la regla.
-- El COALESCE de cada rama es la red: quien eligió manual y todavía no ha
-- subido nada ve la automática en vez de un hueco gris.
ALTER TABLE servers
    ADD COLUMN display_icon_key TEXT GENERATED ALWAYS AS (
        CASE WHEN icon_source = 'MANUAL'
             THEN COALESCE(icon_key, icon_key_auto)
             ELSE COALESCE(icon_key_auto, icon_key)
        END) STORED,

    ADD COLUMN display_banner_key TEXT GENERATED ALWAYS AS (
        CASE WHEN banner_source = 'MANUAL'
             THEN COALESCE(banner_key, banner_key_auto)
             ELSE COALESCE(banner_key_auto, banner_key)
        END) STORED;

COMMENT ON COLUMN servers.display_icon_key IS
    'La que se enseña. Calculada: no la escribe nadie. Toda consulta publica debe usar esta y no icon_key.';
COMMENT ON COLUMN servers.display_banner_key IS
    'La que se enseña. Calculada: no la escribe nadie. Toda consulta publica debe usar esta y no banner_key.';

-- Los servidores que YA tienen imagen subida se quedan en manual. El DEFAULT
-- 'AUTO' aplicado sin más les habría ocultado el banner que ya habían
-- diseñado, sin avisar y sin que hubieran tocado nada.
UPDATE servers SET icon_source   = 'MANUAL' WHERE icon_key   IS NOT NULL;
UPDATE servers SET banner_source = 'MANUAL' WHERE banner_key IS NOT NULL;

-- -----------------------------------------------------------------------------
-- Lo que se lee del ping
-- -----------------------------------------------------------------------------

ALTER TABLE servers
    -- MOTD ya saneado (sin URLs de otros sitios, sin caracteres de control),
    -- guardado con su estructura de colores y formato.
    --
    -- Se guarda ADEMÁS del banner porque la web lo pinta también como texto: un
    -- lector de pantalla no puede leer un PNG, y el buscador tampoco.
    --
    -- En JSON y no como texto con códigos §: los MOTD modernos usan colores
    -- hexadecimales para hacer degradados, y los códigos § solo saben expresar
    -- dieciséis colores. Guardarlo en el formato viejo perdería justo lo que le
    -- da identidad a los servidores que más se lo curran.
    ADD COLUMN motd_json TEXT,

    -- Hashes para no regenerar lo que no ha cambiado. Muchos MOTD llevan dentro
    -- un contador de jugadores, así que cambian cada minuto: el hash detecta el
    -- cambio, pero quien frena de verdad es auto_images_updated_at.
    ADD COLUMN motd_hash    TEXT,
    ADD COLUMN favicon_hash TEXT,

    ADD COLUMN auto_images_updated_at TIMESTAMPTZ,

    -- Adoptamos contenido de terceros y lo servimos desde nuestro dominio. Si
    -- alguien pone algo que no debe, esto lo devuelve al marcador de posición y
    -- lo deja ahí: no se readopta nunca más, aunque cambie el favicon.
    ADD COLUMN auto_images_blocked_at     TIMESTAMPTZ,
    ADD COLUMN auto_images_blocked_reason TEXT;

COMMENT ON COLUMN servers.motd_json IS
    'MOTD leido del ping, saneado y con su formato. Declarado por el dueño: nunca alimenta el ranking (ADR-0009).';
COMMENT ON COLUMN servers.auto_images_blocked_at IS
    'Bloqueo administrativo de las imagenes automaticas de este servidor (ADR-0027).';

ALTER TABLE servers
    ADD CONSTRAINT servers_motd_acotado
        CHECK (motd_json IS NULL OR length(motd_json) <= 8192);

-- Los candidatos a regenerar: activos, sin bloquear, y con la última
-- regeneración vieja o inexistente. Índice parcial porque la consulta nunca
-- mira los bloqueados.
CREATE INDEX servers_imagenes_automaticas
    ON servers (auto_images_updated_at NULLS FIRST)
    WHERE deleted_at IS NULL
      AND status_code = 'ACTIVE'
      AND auto_images_blocked_at IS NULL;

-- -----------------------------------------------------------------------------
-- Propósitos nuevos en `uploads`
-- -----------------------------------------------------------------------------
--
-- Las imágenes automáticas se registran igual que una subida, con purpose
-- propio. Así la auditoría y la limpieza de huérfanos que ya existen las cubren
-- sin código nuevo, y se distingue de un vistazo lo que subió una persona de lo
-- que generamos nosotros.
--
-- uploaded_by queda NULL: no lo subió nadie.

COMMENT ON COLUMN uploads.purpose IS
    'ICON, BANNER, LOGO, SOCIAL (subidos por una persona) o ICON_AUTO, BANNER_AUTO (generados desde el ping, ADR-0027).';
