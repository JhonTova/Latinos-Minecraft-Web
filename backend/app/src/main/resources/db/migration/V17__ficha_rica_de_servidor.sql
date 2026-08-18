-- =============================================================================
-- V17 — La ficha del servidor pasa a decir algo
-- =============================================================================
--
-- Hasta ahora un servidor tenia nombre, descripcion y direccion. Un jugador que
-- mira una lista quiere saber tres cosas antes de pulsar nada: a que se juega,
-- si su version le sirve y si le va a ir bien de ping. Ninguna estaba.
--
-- Todo lo que se añade aqui lo declara el dueño y NO afecta al ranking. Es
-- deliberado (ADR-0009): en cuanto un dato declarado por el cliente puntua, deja
-- de describir el servidor y pasa a describir lo que le conviene poner.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Modalidades
-- -----------------------------------------------------------------------------
--
-- Tabla y no un array de texto libre. Con texto libre acabarian conviviendo
-- "SkyBlock", "skyblock", "Sky Block" y "sky-block" como cuatro cosas distintas,
-- y filtrar por modalidad —que es para lo que sirve esto— dejaria de funcionar.

CREATE TABLE game_modes (
    code        TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    -- Para agrupar en el selector. Menor numero, aparece antes.
    sort_order  INT NOT NULL DEFAULT 100
);

INSERT INTO game_modes (code, name, sort_order) VALUES
    ('SURVIVAL',   'Survival',    10),
    ('SKYBLOCK',   'SkyBlock',    20),
    ('PVP',        'PvP',         30),
    ('FACTIONS',   'Factions',    40),
    ('PRISON',     'Prison',      50),
    ('MINIGAMES',  'Minijuegos',  60),
    ('BEDWARS',    'BedWars',     70),
    ('SKYWARS',    'SkyWars',     80),
    ('ANARCHY',    'Anarquía',    90),
    ('CREATIVE',   'Creativo',   100),
    ('ROLEPLAY',   'Roleplay',   110),
    ('TOWNY',      'Towny',      120),
    ('EARTH',      'Earth / SMP',130),
    ('MODDED',     'Con mods',   140),
    ('PARKOUR',    'Parkour',    150),
    ('ECONOMY',    'Economía',   160);

CREATE TABLE server_game_modes (
    server_id   UUID NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    mode_code   TEXT NOT NULL REFERENCES game_modes (code) ON DELETE RESTRICT,
    PRIMARY KEY (server_id, mode_code)
);

CREATE INDEX idx_server_game_modes_por_modalidad ON server_game_modes (mode_code);

-- -----------------------------------------------------------------------------
-- Versiones, tema y ubicacion
-- -----------------------------------------------------------------------------

ALTER TABLE servers
    -- Rango de versiones soportadas: "1.8" a "1.21.11". Texto y no numero
    -- porque el versionado de Minecraft no es semantico y no se puede comparar
    -- aritmeticamente sin inventarse reglas que se rompen en la siguiente
    -- version.
    ADD COLUMN version_min TEXT,
    ADD COLUMN version_max TEXT,

    -- Donde esta alojado. Sirve para que el jugador estime su ping ANTES de
    -- entrar: alguien en Chile quiere saber si el servidor esta en Sao Paulo o
    -- en Frankfurt.
    --
    -- Lo declara el dueño. No se comprueba: geolocalizar la IP daria el centro
    -- de datos del proxy, no el del servidor, y con Cloudflare o un anti-DDoS
    -- delante seria simplemente falso.
    ADD COLUMN hosting_region TEXT,

    -- Tema de color de SU ficha. Uno de seis; el codigo valida el valor y la
    -- web solo pinta los que conoce.
    ADD COLUMN theme_code TEXT NOT NULL DEFAULT 'DEFECTO';

COMMENT ON COLUMN servers.hosting_region IS
    'Region declarada por el dueño para que el jugador estime su ping. No se verifica: con un proxy delante, geolocalizar la IP daria un dato falso.';

COMMENT ON COLUMN servers.theme_code IS
    'Tema de color de la ficha publica del servidor. Uno de los seis definidos en el frontend; cualquier otro valor se pinta con el de por defecto.';

ALTER TABLE servers
    ADD CONSTRAINT servers_version_min_corta CHECK (version_min IS NULL OR length(version_min) <= 12),
    ADD CONSTRAINT servers_version_max_corta CHECK (version_max IS NULL OR length(version_max) <= 12),
    ADD CONSTRAINT servers_region_corta CHECK (hosting_region IS NULL OR length(hosting_region) <= 40),
    ADD CONSTRAINT servers_tema_corto CHECK (length(theme_code) <= 20);

-- -----------------------------------------------------------------------------
-- Ajustes del sitio que faltaban
-- -----------------------------------------------------------------------------
--
-- El logotipo y el enlace de descarga del plugin estaban escritos en el codigo.
-- El primero es identidad de marca y el segundo cambia cada vez que se publica
-- una version: ninguno deberia exigir un despliegue.

ALTER TABLE site_settings
    ADD COLUMN logo_key        TEXT,
    ADD COLUMN plugin_url      TEXT;
