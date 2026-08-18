-- =============================================================================
-- V4 — Motor de votos
-- =============================================================================
-- Modulo 🔴 critico. Un voto no es un clic: es un evento de negocio que debe ser
-- verificable, auditable e idempotente.
-- =============================================================================

CREATE TABLE vote_statuses (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO vote_statuses (code, name) VALUES
    ('PENDING',   'En validacion'),
    ('ACCEPTED',  'Aceptado'),
    ('REJECTED',  'Rechazado por antifraude'),
    ('REVOKED',   'Revocado tras revision');

CREATE TABLE reward_statuses (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO reward_statuses (code, name) VALUES
    ('PENDING',    'Pendiente de entrega'),
    ('DELIVERED',  'Entregada'),
    ('FAILED',     'Fallo temporal, se reintentara'),
    ('ABANDONED',  'Agotados los reintentos');

-- -----------------------------------------------------------------------------

CREATE TABLE votes (
    id                      UUID PRIMARY KEY,
    server_id               UUID NOT NULL REFERENCES servers (id) ON DELETE RESTRICT,
    status_code             TEXT NOT NULL REFERENCES vote_statuses (code),

    -- Identidad del votante como (tipo, valor). No se asume UUID de Mojang:
    -- la decision sobre offline-mode sigue abierta (DECISIONES.md 4.1).
    identity_type_code      TEXT NOT NULL REFERENCES identity_types (code),
    -- Valor en claro: necesario para entregar la recompensa al jugador correcto.
    -- NUNCA se interpola en un comando (ADR-0012).
    identity_value          TEXT NOT NULL,
    -- Hash estable del par (tipo, valor). Es lo que se usa para la ventana de
    -- voto y el antifraude, de modo que al anonimizar se pueda borrar
    -- identity_value conservando la capacidad de detectar duplicados historicos.
    identity_hash           BYTEA NOT NULL,

    -- IP seudonimizada. Nunca en claro. Retencion 90 dias (ADR-0010).
    ip_hash                 BYTEA,
    ip_country              TEXT,

    -- De donde llego el voto. Permite agregar votos de terceros en el futuro
    -- sin cambiar el esquema (DECISIONES.md 4.3).
    source                  TEXT NOT NULL DEFAULT 'PLATFORM',

    -- Idempotencia obligatoria (convenciones del proyecto, regla 12). Un reintento del cliente
    -- nunca puede crear dos votos.
    idempotency_key         TEXT NOT NULL,

    risk_score              INT,
    rejection_reason        TEXT,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at              TIMESTAMPTZ,

    CONSTRAINT votes_risk_score_valido
        CHECK (risk_score IS NULL OR risk_score BETWEEN 0 AND 100)
);

COMMENT ON TABLE votes IS
    'Votos emitidos. DATOS PERSONALES (identity_value, ip_hash). Retencion: ip_hash 90 dias; identity_value se borra al anonimizar conservando identity_hash (ADR-0010). Candidata a particionado por created_at.';

-- La idempotencia es una restriccion de la base de datos, no una comprobacion
-- en codigo: bajo concurrencia, dos peticiones simultaneas pasarian ambas un
-- "SELECT antes de INSERT". Aqui la segunda choca contra el indice.
CREATE UNIQUE INDEX votes_idempotency_key_unico
    ON votes (idempotency_key);

-- Consulta de la ventana deslizante (ADR-0011): "cuando fue el ultimo voto
-- ACEPTADO de esta identidad a este servidor". Indice parcial y descendente:
-- responde leyendo una sola fila.
CREATE INDEX votes_ventana
    ON votes (server_id, identity_hash, created_at DESC)
    WHERE status_code = 'ACCEPTED';

-- Recuento del ranking y analiticas por servidor.
CREATE INDEX votes_por_servidor_aceptados
    ON votes (server_id, created_at DESC)
    WHERE status_code = 'ACCEPTED';

-- Investigacion antifraude: todos los votos de una identidad en el tiempo.
CREATE INDEX votes_por_identidad
    ON votes (identity_hash, created_at DESC);

-- -----------------------------------------------------------------------------
-- Senales de antifraude
-- -----------------------------------------------------------------------------
-- Tabla aparte: las senales son heterogeneas y cambian a menudo (cada regla
-- nueva aporta campos distintos). JSONB es el uso correcto aqui — datos
-- variables que no son criterio principal de consulta (ADR-0002).
--
-- Separadas de 'votes' tambien por retencion: las senales caducan antes que el
-- voto.
-- -----------------------------------------------------------------------------
CREATE TABLE vote_risk_signals (
    id          UUID PRIMARY KEY,
    vote_id     UUID NOT NULL REFERENCES votes (id) ON DELETE CASCADE,
    rule_code   TEXT NOT NULL,
    score_delta INT  NOT NULL,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE vote_risk_signals IS
    'Por que el motor de reglas puntuo un voto. Necesario para explicar una decision a un cliente que reclama. Retencion: 180 dias.';

CREATE INDEX vote_risk_signals_por_voto ON vote_risk_signals (vote_id);
CREATE INDEX vote_risk_signals_por_regla ON vote_risk_signals (rule_code, created_at DESC);

-- -----------------------------------------------------------------------------
-- Recompensas
-- -----------------------------------------------------------------------------
-- La recompensa NO es una cadena de comando (ADR-0012). Es un tipo con
-- parametros validados; el plugin traduce cada tipo a la accion concreta.
--
-- Estado propio porque la entrega es asincrona y puede tardar dias: la mayoria
-- de los votos llegan con el jugador desconectado.
-- -----------------------------------------------------------------------------
CREATE TABLE vote_rewards (
    id              UUID PRIMARY KEY,
    vote_id         UUID NOT NULL REFERENCES votes (id) ON DELETE CASCADE,
    server_id       UUID NOT NULL REFERENCES servers (id) ON DELETE RESTRICT,

    reward_type     TEXT  NOT NULL,
    parameters      JSONB NOT NULL DEFAULT '{}'::jsonb,

    status_code     TEXT  NOT NULL REFERENCES reward_statuses (code),
    attempts        INT   NOT NULL DEFAULT 0,
    delivered_at    TIMESTAMPTZ,
    last_error      TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE vote_rewards IS
    'Recompensa asociada a un voto aceptado. Retencion: sigue al voto.';

CREATE UNIQUE INDEX vote_rewards_por_voto ON vote_rewards (vote_id);

CREATE INDEX vote_rewards_pendientes
    ON vote_rewards (server_id, created_at)
    WHERE status_code IN ('PENDING', 'FAILED');
