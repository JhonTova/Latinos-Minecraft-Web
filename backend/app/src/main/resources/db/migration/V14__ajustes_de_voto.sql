-- =============================================================================
-- V14 — Ajustes de voto configurables
-- =============================================================================
--
-- Hasta ahora la regla era una sola: un voto por servidor y por jugador cada 24
-- horas (ADR-0011). Se añade una segunda, GLOBAL: un jugador solo puede votar a
-- UN servidor al dia, no a uno por cada servidor.
--
-- Es un cambio de producto grande, no un ajuste tecnico:
--
--   * Reduce el volumen total de votos. Un jugador de tres servidores pasa de
--     tres votos diarios a uno.
--   * Cambia el significado del ranking: pasa de medir "cuanta gente juega
--     aqui" a medir "cual es tu servidor FAVORITO". Es defendible, y de hecho
--     mas dificil de manipular, pero es otra cosa.
--
-- Por eso todo vive en una tabla y no en constantes del codigo: la decision se
-- puede revisar desde administracion en diez segundos, sin desplegar.
--
-- -----------------------------------------------------------------------------
-- SOBRE EL LIMITE POR IP
-- -----------------------------------------------------------------------------
--
-- Bloquear por IP tiene un problema real en el mercado objetivo: los operadores
-- moviles de Latinoamerica usan CGNAT y meten MILES de clientes detras de una
-- sola IPv4. Lo mismo pasa en una casa con hermanos, en un cibercafe o en un
-- colegio.
--
-- Con el limite a 1, el primer jugador que vote desde una IP compartida deja
-- fuera a todos los demas ese dia, y esos jugadores no entienden por que ni
-- pueden hacer nada. Es el tipo de fallo que no genera un error: genera
-- abandono silencioso.
--
-- Se deja en 1 porque es lo pedido, pero el valor es editable. Cuando lleguen
-- las quejas de usuarios moviles, subirlo a 3-5 es cuestion de segundos.
-- =============================================================================

CREATE TABLE vote_settings (
    -- Fila unica. El CHECK sobre una constante es lo que garantiza que no pueda
    -- haber dos configuraciones distintas conviviendo.
    id                      BOOLEAN PRIMARY KEY DEFAULT TRUE,

    -- Si un jugador solo puede votar a un servidor al dia en toda la plataforma.
    -- Con FALSE se vuelve al comportamiento anterior: uno por servidor.
    one_vote_per_day        BOOLEAN NOT NULL DEFAULT TRUE,

    -- Cuantos jugadores DISTINTOS pueden votar desde la misma IP en un dia.
    -- Ver la nota sobre CGNAT de arriba antes de bajarlo.
    max_players_per_ip      INT     NOT NULL DEFAULT 1,

    -- Duracion de la ventana global, en horas. 24 = un dia.
    window_hours            INT     NOT NULL DEFAULT 24,

    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by              UUID REFERENCES users (id) ON DELETE SET NULL,

    CONSTRAINT vote_settings_fila_unica CHECK (id IS TRUE),
    CONSTRAINT vote_settings_ip_positivo CHECK (max_players_per_ip >= 1),
    CONSTRAINT vote_settings_ventana_valida CHECK (window_hours BETWEEN 1 AND 168)
);

INSERT INTO vote_settings (id) VALUES (TRUE);

COMMENT ON TABLE vote_settings IS
    'Reglas de voto de toda la plataforma. Editables desde administracion sin desplegar.';

-- -----------------------------------------------------------------------------
-- Indices para las dos consultas nuevas
-- -----------------------------------------------------------------------------
--
-- Las dos se ejecutan en CADA voto, antes de aceptarlo. Sin indice, cada voto
-- escanearia la tabla entera de votos, y esa tabla solo crece.

-- "¿Ha votado ya esta identidad hoy, a cualquier servidor?"
CREATE INDEX idx_votes_identidad_reciente
    ON votes (identity_hash, created_at DESC)
 WHERE status_code = 'ACCEPTED';

-- "¿Cuantos jugadores distintos han votado hoy desde esta IP?"
CREATE INDEX idx_votes_ip_reciente
    ON votes (ip_hash, created_at DESC)
 WHERE status_code = 'ACCEPTED' AND ip_hash IS NOT NULL;
