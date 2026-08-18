-- =============================================================================
-- V5 — Outbox
-- =============================================================================
-- ADR-0003: PostgreSQL es la cola. Ni Kafka ni RabbitMQ.
--
-- El motivo de fondo: para garantizar que un voto aceptado se entrega, la
-- escritura de dominio y la publicacion del evento deben ocurrir en la MISMA
-- transaccion. Eso solo lo garantiza la base de datos. Anadir un broker no
-- elimina esta tabla — la necesitarias igual — solo anade un servicio mas que
-- operar.
--
-- Ventaja operativa con un solo operador: la cola se inspecciona con SELECT.
-- =============================================================================

CREATE TABLE outbox_statuses (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO outbox_statuses (code, name) VALUES
    ('PENDING',    'Pendiente de publicar'),
    ('PUBLISHED',  'Publicado'),
    ('FAILED',     'Fallo temporal, se reintentara'),
    ('DEAD',       'Agotados los reintentos (DLQ)');

-- -----------------------------------------------------------------------------

CREATE TABLE outbox (
    id                  UUID PRIMARY KEY,

    -- Que agregado genero el evento. Permite reconstruir el historial.
    aggregate_type      TEXT NOT NULL,
    aggregate_id        UUID NOT NULL,

    -- Nombre del evento tal y como aparece en el contrato AsyncAPI (ADR-0006).
    event_type          TEXT NOT NULL,
    -- Version del contrato: un consumidor antiguo debe poder ignorar lo que no
    -- entiende sin romperse.
    event_version       INT  NOT NULL DEFAULT 1,
    payload             JSONB NOT NULL,

    status_code         TEXT NOT NULL REFERENCES outbox_statuses (code) DEFAULT 'PENDING',
    attempts            INT  NOT NULL DEFAULT 0,
    -- Backoff exponencial: el consumidor solo coge filas cuyo next_attempt_at
    -- ya paso.
    next_attempt_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error          TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at        TIMESTAMPTZ
);

COMMENT ON TABLE outbox IS
    'Eventos salientes. Sin datos personales en payload salvo lo imprescindible. Retencion: 30 dias tras publicar, despues purga. DEAD se conserva hasta resolucion manual.';

-- Indice de consumo. Es el unico que importa para el rendimiento de la cola:
-- el worker hace SELECT ... FOR UPDATE SKIP LOCKED sobre exactamente esto.
-- Parcial, asi que su tamano depende de la cola pendiente, no del historico.
CREATE INDEX outbox_por_publicar
    ON outbox (next_attempt_at, created_at)
    WHERE status_code IN ('PENDING', 'FAILED');

-- Cola de mensajes muertos: se revisa a mano, debe ser facil de encontrar.
CREATE INDEX outbox_muertos
    ON outbox (created_at DESC)
    WHERE status_code = 'DEAD';

CREATE INDEX outbox_por_agregado
    ON outbox (aggregate_type, aggregate_id, created_at DESC);

-- -----------------------------------------------------------------------------
-- Entregas a destinos externos (webhooks y plugins)
-- -----------------------------------------------------------------------------
-- Un evento del outbox puede tener varios destinatarios. Cada intento de
-- entrega tiene su propio estado: que un webhook falle no debe bloquear la
-- entrega al plugin.
-- -----------------------------------------------------------------------------
CREATE TABLE outbox_deliveries (
    id                  UUID PRIMARY KEY,
    outbox_id           UUID NOT NULL REFERENCES outbox (id) ON DELETE CASCADE,

    destination_type    TEXT NOT NULL,
    destination_id      UUID,
    endpoint_url        TEXT,

    status_code         TEXT NOT NULL REFERENCES outbox_statuses (code) DEFAULT 'PENDING',
    attempts            INT  NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    response_status     INT,
    last_error          TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at        TIMESTAMPTZ
);

COMMENT ON TABLE outbox_deliveries IS
    'Intento de entrega de un evento a un destino concreto. Retencion: 30 dias.';

CREATE INDEX outbox_deliveries_por_entregar
    ON outbox_deliveries (next_attempt_at)
    WHERE status_code IN ('PENDING', 'FAILED');

CREATE INDEX outbox_deliveries_por_evento
    ON outbox_deliveries (outbox_id);
