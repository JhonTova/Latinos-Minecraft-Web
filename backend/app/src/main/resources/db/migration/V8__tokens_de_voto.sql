-- =============================================================================
-- V8 — Tokens de voto de un solo uso (ADR-0016)
-- =============================================================================
-- Corrige el agujero mas grande del flujo original: que el jugador escribiera su
-- nick en la web. Si el nick lo teclea el visitante, cualquiera puede votar en
-- nombre de cualquiera sin haber pisado nunca ese servidor.
--
-- Con token, la identidad queda probada por construccion: para obtener uno hay
-- que estar DENTRO del servidor y ejecutar /vote alli.
-- =============================================================================

CREATE TABLE vote_tokens (
    id                  UUID PRIMARY KEY,
    server_id           UUID NOT NULL REFERENCES servers (id) ON DELETE CASCADE,

    -- Solo el hash, igual que las claves de API: si la base de datos se filtra,
    -- los tokens vivos no son utilizables.
    token_hash          TEXT NOT NULL,

    -- Identidad ya resuelta por el plugin. La web no la pregunta: la lee de aqui.
    identity_type_code  TEXT NOT NULL REFERENCES identity_types (code),
    identity_value      TEXT NOT NULL,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,

    -- Un solo uso: al votar se marca y deja de servir. Un enlace reenviado a un
    -- amigo no vale para nada.
    consumed_at         TIMESTAMPTZ,
    consumed_vote_id    UUID REFERENCES votes (id) ON DELETE SET NULL
);

COMMENT ON TABLE vote_tokens IS
    'Tokens de un solo uso emitidos por /vote (ADR-0016). DATOS PERSONALES (identity_value). Retencion: purga a los 7 dias, usados o no.';

CREATE UNIQUE INDEX vote_tokens_hash_unico ON vote_tokens (token_hash);

-- Busqueda del token al abrir el enlace: solo interesan los vivos y sin usar.
CREATE INDEX vote_tokens_vigentes
    ON vote_tokens (expires_at)
    WHERE consumed_at IS NULL;

-- Evita que un jugador acumule tokens ejecutando /vote en bucle: el servicio
-- reutiliza el ultimo token vigente en lugar de emitir uno nuevo.
CREATE INDEX vote_tokens_por_jugador
    ON vote_tokens (server_id, identity_value, expires_at DESC)
    WHERE consumed_at IS NULL;
