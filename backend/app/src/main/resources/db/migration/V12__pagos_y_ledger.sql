-- =============================================================================
-- V12 — Pagos, slots destacados y ledger
-- =============================================================================
-- Reglas que manda la propia base de datos, no el código:
--
--   * El dinero se guarda en CÉNTIMOS como entero. Nunca decimales flotantes:
--     0.1 + 0.2 no da 0.3 en coma flotante, y en contabilidad eso es un descuadre
--     que aparece meses después y nadie sabe de dónde salió.
--
--   * El ledger es INMUTABLE. Hay un disparador que rechaza UPDATE y DELETE.
--     No es una convención que haya que recordar: es imposible saltársela.
--
--   * Nunca se guarda un dato de tarjeta (ADR-0004). Ni aquí ni en ningún sitio.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Catálogo de lo que se vende
-- -----------------------------------------------------------------------------
CREATE TABLE product_types (
    code            TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    description     TEXT
);

INSERT INTO product_types (code, name, description) VALUES
    ('FEATURED_SLOT',   'Slot destacado',      'Aparece en el bloque de destacados de la portada'),
    ('BRAND_SPONSOR',   'Patrocinio de marca', 'Espacio para marcas: hostings, proveedores de VPS'),
    ('PLAN_PLUS',       'Plan Plus',           'Servidores ilimitados en la organizacion');

-- -----------------------------------------------------------------------------
-- Slots destacados (ADR-0013)
-- -----------------------------------------------------------------------------
-- Precio fijo por slot, configurable desde el panel de administracion. Nada de
-- subastas: el cliente sabe exactamente que paga y cuando lo recibe.
-- -----------------------------------------------------------------------------
CREATE TABLE featured_slots (
    position        INT PRIMARY KEY,
    price_cents     BIGINT NOT NULL,
    currency        TEXT   NOT NULL DEFAULT 'EUR',
    -- Duracion de cada periodo. Semanal por defecto.
    period_days     INT    NOT NULL DEFAULT 7,
    -- Cuantos periodos se pueden vender por adelantado. Vender 20 semanas es
    -- retener dinero por un servicio muy lejano, con el mismo riesgo de
    -- reembolsos que llevo a descartar el wallet (ADR-0013).
    max_ahead       INT    NOT NULL DEFAULT 4,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT featured_slots_posicion_valida CHECK (position BETWEEN 1 AND 5),
    CONSTRAINT featured_slots_precio_no_negativo CHECK (price_cents >= 0)
);

INSERT INTO featured_slots (position, price_cents) VALUES
    (1, 2500), (2, 2000), (3, 1500), (4, 1200), (5, 1000);

COMMENT ON TABLE featured_slots IS
    'Precios de los 5 espacios destacados. Editables desde administracion sin desplegar.';

-- -----------------------------------------------------------------------------
-- Pedidos
-- -----------------------------------------------------------------------------
CREATE TABLE order_statuses (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

INSERT INTO order_statuses (code, name) VALUES
    ('PENDING',   'Esperando pago'),
    ('PAID',      'Pagado'),
    ('CANCELLED', 'Cancelado'),
    ('REFUNDED',  'Reembolsado'),
    ('FAILED',    'Pago fallido');

CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    organization_id     UUID NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    created_by          UUID          REFERENCES users (id) ON DELETE SET NULL,

    product_code        TEXT   NOT NULL REFERENCES product_types (code),
    status_code         TEXT   NOT NULL REFERENCES order_statuses (code),

    amount_cents        BIGINT NOT NULL,
    currency            TEXT   NOT NULL,

    -- Qué pasarela lo cobró. Puerto intercambiable: el mercado hispanohablante
    -- necesita medios locales (Mercado Pago, PSE) además de los internacionales.
    gateway             TEXT,
    -- Identificador del pago EN la pasarela. Es la referencia para conciliar.
    gateway_reference   TEXT,

    -- Idempotencia: un doble clic o un reintento no puede generar dos cobros.
    idempotency_key     TEXT NOT NULL,

    -- Contexto del pedido (qué slot, qué fechas). JSONB porque varía por
    -- producto y no es criterio de búsqueda.
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at             TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,

    CONSTRAINT orders_importe_no_negativo CHECK (amount_cents >= 0)
);

COMMENT ON TABLE orders IS
    'Pedidos. Nunca contienen datos de tarjeta (ADR-0004). Retencion: permanente por obligacion contable.';

CREATE UNIQUE INDEX orders_idempotency_unico ON orders (idempotency_key);
CREATE UNIQUE INDEX orders_gateway_ref_unico
    ON orders (gateway, gateway_reference)
    WHERE gateway_reference IS NOT NULL;

CREATE INDEX orders_por_organizacion ON orders (organization_id, created_at DESC);
CREATE INDEX orders_pendientes ON orders (created_at) WHERE status_code = 'PENDING';

-- -----------------------------------------------------------------------------
-- Reservas de slot
-- -----------------------------------------------------------------------------
-- Cola FIFO por slot. Si el espacio esta ocupado, el comprador entra en la
-- siguiente rotacion con FECHAS EXACTAS, nunca con un "proximamente".
-- -----------------------------------------------------------------------------
CREATE TABLE slot_bookings (
    id              UUID PRIMARY KEY,
    slot_position   INT  NOT NULL REFERENCES featured_slots (position),
    server_id       UUID NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,

    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at    TIMESTAMPTZ,

    CONSTRAINT slot_bookings_periodo_valido CHECK (ends_at > starts_at)
);

COMMENT ON TABLE slot_bookings IS
    'Periodos de slot vendidos. La cola FIFO sale de ordenar por starts_at.';

-- Dos servidores no pueden ocupar el mismo slot a la vez. Lo impide la base de
-- datos con un indice de exclusion sobre rangos de tiempo: sin esto, dos
-- compras simultaneas del mismo hueco pasarian ambas.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE slot_bookings
    ADD CONSTRAINT slot_bookings_sin_solape
    EXCLUDE USING gist (
        slot_position WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (cancelled_at IS NULL);

CREATE INDEX slot_bookings_vigentes
    ON slot_bookings (slot_position, starts_at)
    WHERE cancelled_at IS NULL;

CREATE INDEX slot_bookings_por_servidor ON slot_bookings (server_id);

-- -----------------------------------------------------------------------------
-- LEDGER — inmutable
-- -----------------------------------------------------------------------------
-- Solo se añaden asientos. Jamás UPDATE ni DELETE sobre dinero: una corrección
-- es un asiento nuevo de signo contrario, igual que en la contabilidad de
-- verdad. Así el historial siempre explica cómo se llegó al saldo actual.
-- -----------------------------------------------------------------------------
CREATE TABLE ledger_entries (
    id                  UUID PRIMARY KEY,
    organization_id     UUID REFERENCES organizations (id) ON DELETE RESTRICT,
    order_id            UUID REFERENCES orders (id) ON DELETE RESTRICT,

    entry_type          TEXT   NOT NULL,
    -- Positivo = entra dinero. Negativo = sale (reembolso, contracargo).
    amount_cents        BIGINT NOT NULL,
    currency            TEXT   NOT NULL,

    description         TEXT NOT NULL,
    -- Asiento que corrige a otro. Es lo que hace posible rectificar sin borrar.
    reverses_entry_id   UUID REFERENCES ledger_entries (id),

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE ledger_entries IS
    'Libro mayor INMUTABLE. Solo INSERT. Las correcciones son asientos compensatorios. Retencion: permanente por obligacion legal (ADR-0010).';

CREATE INDEX ledger_por_organizacion ON ledger_entries (organization_id, created_at DESC);
CREATE INDEX ledger_por_pedido ON ledger_entries (order_id);

-- El disparador que lo hace de verdad inmutable.
--
-- Sin esto, "el ledger es inmutable" sería una regla que alguien tiene que
-- recordar. Con esto, es imposible saltársela: ni un UPDATE con prisa a las 3 de
-- la mañana, ni un ORM mal configurado, ni un script de migración despistado.
CREATE OR REPLACE FUNCTION ledger_solo_lectura() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'El ledger es inmutable: no se permite % (ADR-0004). Registra un asiento compensatorio.', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_sin_update
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_solo_lectura();

CREATE TRIGGER ledger_sin_delete
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_solo_lectura();

-- -----------------------------------------------------------------------------
-- Webhooks de la pasarela
-- -----------------------------------------------------------------------------
-- Las pasarelas reenvían el mismo evento varias veces si no reciben un 200 a
-- tiempo. Sin este registro, un reenvío contaría el pago dos veces.
-- -----------------------------------------------------------------------------
CREATE TABLE payment_webhooks (
    id              UUID PRIMARY KEY,
    gateway         TEXT NOT NULL,
    -- Identificador del evento EN la pasarela. Es la clave de idempotencia.
    event_id        TEXT NOT NULL,
    event_type      TEXT NOT NULL,

    payload         JSONB NOT NULL,
    processed_at    TIMESTAMPTZ,
    error           TEXT,

    received_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE payment_webhooks IS
    'Eventos recibidos de la pasarela. Retencion: 12 meses para conciliacion.';

CREATE UNIQUE INDEX payment_webhooks_evento_unico ON payment_webhooks (gateway, event_id);
CREATE INDEX payment_webhooks_sin_procesar ON payment_webhooks (received_at) WHERE processed_at IS NULL;

-- -----------------------------------------------------------------------------
-- Patrocinadores de marca
-- -----------------------------------------------------------------------------
CREATE TABLE brand_sponsors (
    id              UUID PRIMARY KEY,
    order_id        UUID REFERENCES orders (id) ON DELETE RESTRICT,

    name            TEXT NOT NULL,
    target_url      TEXT NOT NULL,
    logo_key        TEXT,

    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,

    -- Menor número = aparece antes.
    display_order   INT NOT NULL DEFAULT 100,
    approved_at     TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at    TIMESTAMPTZ,

    CONSTRAINT brand_sponsors_periodo_valido CHECK (ends_at > starts_at)
);

COMMENT ON TABLE brand_sponsors IS
    'Patrocinadores (hostings, VPS...). Requieren aprobacion manual antes de publicarse: un enlace saliente desde nuestro dominio es nuestra reputacion.';

CREATE INDEX brand_sponsors_activos
    ON brand_sponsors (display_order, starts_at)
    WHERE cancelled_at IS NULL AND approved_at IS NOT NULL;
