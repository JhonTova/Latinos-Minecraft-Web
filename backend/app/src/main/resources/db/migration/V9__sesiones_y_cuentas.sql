-- =============================================================================
-- V9 — Sesiones y cuentas vinculadas
-- =============================================================================
-- Sesiones en base de datos y no JWT autocontenidos.
--
-- Un JWT no se puede revocar: una vez emitido es valido hasta que caduca, y si
-- alguien roba una sesion o hay que expulsar a una cuenta suspendida, no hay
-- forma de cortarla salvo esperar. En una plataforma con dinero y con paneles de
-- administracion, poder cortar el acceso al instante vale mas que ahorrarse una
-- consulta.
--
-- Ademas es lo que permite la pantalla de "dispositivos conectados" con boton de
-- cerrar sesion, que es una funcion que el usuario espera encontrar.
-- =============================================================================

CREATE TABLE sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Solo el hash del token, igual que las claves de API: si la base se filtra,
    -- las sesiones vivas no son secuestrables.
    token_hash          TEXT NOT NULL,

    -- Para que el usuario reconozca sus sesiones en el panel.
    user_agent          TEXT,
    ip_hash             BYTEA,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    revoked_reason      TEXT
);

COMMENT ON TABLE sessions IS
    'Sesiones activas. DATOS PERSONALES (ip_hash, user_agent). Retencion: se purgan 30 dias despues de caducar (ADR-0010).';

CREATE UNIQUE INDEX sessions_token_unico ON sessions (token_hash);

-- Validacion en cada peticion: tiene que ser barata.
CREATE INDEX sessions_vigentes
    ON sessions (user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX sessions_purga ON sessions (expires_at);

-- -----------------------------------------------------------------------------
-- Cuentas de proveedores externos
-- -----------------------------------------------------------------------------
-- Discord antes que Facebook: en el ecosistema de Minecraft, Discord es donde
-- vive la comunidad entera. Facebook no lo usa practicamente nadie de este
-- publico.
--
-- Tabla aparte y no columnas en 'users' porque un mismo usuario puede vincular
-- varios proveedores, y porque anadir uno nuevo debe ser un INSERT y no una
-- migracion de esquema.
-- -----------------------------------------------------------------------------
CREATE TABLE oauth_accounts (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    provider            TEXT NOT NULL,
    -- Identificador del usuario EN el proveedor. Nunca el email: en algunos
    -- proveedores el email cambia, y si se usara como enlace, cambiarlo en
    -- Google secuestraria la cuenta de otro.
    provider_user_id    TEXT NOT NULL,

    email_at_provider   CITEXT,
    linked_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at       TIMESTAMPTZ
);

COMMENT ON TABLE oauth_accounts IS
    'Cuentas externas vinculadas. DATOS PERSONALES. Retencion: se borran con la cuenta (ADR-0010).';

-- Una cuenta de un proveedor pertenece a un unico usuario de la plataforma.
CREATE UNIQUE INDEX oauth_accounts_unica ON oauth_accounts (provider, provider_user_id);

CREATE INDEX oauth_accounts_por_usuario ON oauth_accounts (user_id);

-- -----------------------------------------------------------------------------
-- Limites por plan
-- -----------------------------------------------------------------------------
-- El limite vive en la organizacion y no en el codigo: cambiar el plan de un
-- cliente debe ser un UPDATE, no un despliegue.
-- -----------------------------------------------------------------------------
CREATE TABLE plans (
    code            TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    max_servers     INT,          -- NULL = sin limite
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO plans (code, name, max_servers) VALUES
    ('FREE', 'Gratuito', 3),
    ('PLUS', 'Plus',     NULL);

ALTER TABLE organizations
    ADD COLUMN plan_code TEXT NOT NULL REFERENCES plans (code) DEFAULT 'FREE';

COMMENT ON COLUMN organizations.plan_code IS
    'Plan contratado. Determina el limite de servidores. FREE permite 3; PLUS, ilimitados.';
