-- =============================================================================
-- V2 — Usuarios, organizaciones y miembros
-- =============================================================================
-- Un servidor NO pertenece a una persona, pertenece a una organizacion. Los
-- networks de Minecraft se administran en equipo. Convertir un modelo
-- mono-propietario en multi-tenant despues exige migrar cada tabla que
-- referencia al dueno y reescribir toda la autorizacion: dias ahora, meses
-- despues.
-- =============================================================================

CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    email               CITEXT,
    email_verified_at   TIMESTAMPTZ,
    display_name        TEXT        NOT NULL,
    password_hash       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    -- Marca de anonimizacion irreversible (ADR-0010). Cuando se ejecuta, email
    -- y display_name se sustituyen por valores sin contenido personal, pero la
    -- fila permanece para no romper la integridad referencial del historico.
    anonymized_at       TIMESTAMPTZ
);

COMMENT ON TABLE users IS
    'Cuentas de usuario. DATOS PERSONALES. Retencion: hasta baja + 30 dias, despues anonimizacion irreversible (ADR-0010).';

-- Email unico solo entre cuentas vivas y no anonimizadas: tras anonimizar, el
-- email desaparece y no debe bloquear un registro futuro.
CREATE UNIQUE INDEX users_email_unico
    ON users (email)
    WHERE deleted_at IS NULL AND anonymized_at IS NULL;

-- -----------------------------------------------------------------------------

CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    slug        TEXT        NOT NULL,
    name        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

COMMENT ON TABLE organizations IS
    'Unidad de aislamiento multi-tenant. Ninguna consulta cruza de organizacion. Retencion: permanente (entidad, no persona).';

CREATE UNIQUE INDEX organizations_slug_unico
    ON organizations (slug)
    WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Miembros
-- -----------------------------------------------------------------------------
-- Los roles se guardan como texto contra una tabla de referencia en lugar de un
-- ENUM de Postgres: anadir un rol nuevo debe ser un INSERT, no una migracion que
-- bloquea la tabla.
-- -----------------------------------------------------------------------------
CREATE TABLE organization_roles (
    code        TEXT PRIMARY KEY,
    name        TEXT    NOT NULL,
    rank        INT     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON COLUMN organization_roles.rank IS
    'Menor numero = mas privilegios. Un miembro no puede modificar a otro de rank igual o menor.';

INSERT INTO organization_roles (code, name, rank) VALUES
    ('OWNER',           'Propietario',        0),
    ('CO_OWNER',        'Co-propietario',     1),
    ('MANAGER',         'Gestor',             2),
    ('DEVELOPER',       'Desarrollador',      3),
    ('BILLING_MANAGER', 'Gestor de cobros',   3),
    ('MODERATOR',       'Moderador',          4),
    ('SUPPORT',         'Soporte',            5);

CREATE TABLE organization_members (
    organization_id UUID        NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users (id)         ON DELETE CASCADE,
    role_code       TEXT        NOT NULL REFERENCES organization_roles (code),
    invited_by      UUID                 REFERENCES users (id)         ON DELETE SET NULL,
    invited_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    joined_at       TIMESTAMPTZ,
    -- Permisos temporales: acceso que caduca solo (p. ej. un contratista).
    expires_at      TIMESTAMPTZ,
    PRIMARY KEY (organization_id, user_id)
);

COMMENT ON TABLE organization_members IS
    'Pertenencia usuario-organizacion. Un usuario puede estar en varias. Retencion: sigue a la organizacion.';

CREATE INDEX organization_members_por_usuario
    ON organization_members (user_id);

-- Toda organizacion necesita al menos un OWNER vivo. Se verifica en el dominio;
-- este indice hace barata esa comprobacion.
CREATE INDEX organization_members_owners
    ON organization_members (organization_id)
    WHERE role_code = 'OWNER';
