-- =============================================================================
-- V3 — Directorio de servidores
-- =============================================================================

CREATE TABLE server_statuses (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO server_statuses (code, name) VALUES
    ('DRAFT',                 'Borrador'),
    ('PENDING_VERIFICATION',  'Esperando conexion del plugin'),
    ('ACTIVE',                'Activo'),
    ('SUSPENDED',             'Suspendido'),
    ('ARCHIVED',              'Archivado');

-- -----------------------------------------------------------------------------

CREATE TABLE servers (
    id                  UUID PRIMARY KEY,
    organization_id     UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    game_type_code      TEXT NOT NULL REFERENCES game_types (code),
    status_code         TEXT NOT NULL REFERENCES server_statuses (code),

    slug                TEXT NOT NULL,
    name                TEXT NOT NULL,
    description         TEXT,

    -- Direccion para el ping directo. Es NUESTRA fuente de verdad para
    -- jugadores conectados y disponibilidad (ADR-0009): jamas el numero que
    -- reporta el plugin, porque corre en hardware del cliente.
    host                TEXT NOT NULL,
    port                INT  NOT NULL,

    website_url         TEXT,
    discord_url         TEXT,

    -- Ventana de voto deslizante en horas (ADR-0011). Acotada para que un
    -- servidor no pueda fijar 1 hora e inflar su ranking.
    vote_window_hours   INT NOT NULL DEFAULT 24,

    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT servers_puerto_valido
        CHECK (port BETWEEN 1 AND 65535),
    CONSTRAINT servers_ventana_valida
        CHECK (vote_window_hours BETWEEN 12 AND 48)
);

COMMENT ON TABLE servers IS
    'Servidores del directorio. Retencion: permanente mientras exista la organizacion.';
COMMENT ON COLUMN servers.host IS
    'Destino del ping directo. Fuente autoritativa de estado y jugadores (ADR-0009).';

CREATE UNIQUE INDEX servers_slug_unico
    ON servers (slug)
    WHERE deleted_at IS NULL;

CREATE INDEX servers_por_organizacion
    ON servers (organization_id)
    WHERE deleted_at IS NULL;

-- El ranking solo mira servidores activos: indice parcial, mucho mas pequeno
-- que uno completo.
CREATE INDEX servers_activos
    ON servers (game_type_code)
    WHERE status_code = 'ACTIVE' AND deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Credenciales del plugin
-- -----------------------------------------------------------------------------
-- Se guarda solo el hash. Si la base de datos se filtra, las claves no son
-- utilizables. El valor en claro se muestra una unica vez al crearla.
--
-- Tabla aparte y no una columna en 'servers' porque una clave tiene ciclo de
-- vida propio: rotacion, caducidad, revocacion e historico de uso.
-- -----------------------------------------------------------------------------
CREATE TABLE server_api_keys (
    id              UUID PRIMARY KEY,
    server_id       UUID NOT NULL REFERENCES servers (id) ON DELETE CASCADE,

    key_hash        TEXT NOT NULL,
    -- Prefijo visible para que el usuario reconozca cual es cual sin exponerla.
    key_prefix      TEXT NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users (id) ON DELETE SET NULL,
    last_used_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    revoked_reason  TEXT
);

COMMENT ON TABLE server_api_keys IS
    'Claves del plugin. Solo hash almacenado. Retencion: 90 dias tras revocacion, despues purga.';

CREATE UNIQUE INDEX server_api_keys_hash_unico ON server_api_keys (key_hash);

CREATE INDEX server_api_keys_activas
    ON server_api_keys (server_id)
    WHERE revoked_at IS NULL;

-- -----------------------------------------------------------------------------
-- Estado observado por NOSOTROS (ping directo)
-- -----------------------------------------------------------------------------
-- Fuente autoritativa. Lo que reporta el plugin va en otra tabla y NUNCA
-- alimenta el ranking (ADR-0009).
-- -----------------------------------------------------------------------------
CREATE TABLE server_pings (
    id                  UUID PRIMARY KEY,
    server_id           UUID NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    observed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_online           BOOLEAN NOT NULL,
    players_online      INT,
    players_max         INT,
    latency_ms          INT,
    game_version        TEXT,
    error_message       TEXT
);

COMMENT ON TABLE server_pings IS
    'Medicion propia del estado del servidor. Fuente autoritativa (ADR-0009). Retencion: 90 dias en detalle, luego agregados. Candidata a particionado.';

CREATE INDEX server_pings_recientes
    ON server_pings (server_id, observed_at DESC);
