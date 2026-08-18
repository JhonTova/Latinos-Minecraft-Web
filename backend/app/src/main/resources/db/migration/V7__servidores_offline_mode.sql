-- =============================================================================
-- V7 — Aceptacion de identidades no verificables por servidor
-- =============================================================================
-- La decision de plataforma sobre offline-mode sigue abierta (DECISIONES.md 4.1),
-- pero la decision por SERVIDOR si se puede tomar ya: cada dueno declara si su
-- servidor corre en offline-mode y por tanto si sus votantes pueden identificarse
-- con un nombre que nadie respalda.
--
-- Por defecto FALSE: solo identidades verificadas. Es el ajuste conservador —
-- si mas adelante se decide abrir la plataforma a offline-mode, activarlo es un
-- cambio de configuracion; cerrarlo despues de haberlo abierto seria retirar
-- funcionalidad a clientes existentes.
-- =============================================================================

ALTER TABLE servers
    ADD COLUMN accepts_unverifiable_identities BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN servers.accepts_unverifiable_identities IS
    'Si TRUE, el servidor acepta votos identificados por nombre en offline-mode. El antifraude aplica una penalizacion de riesgo a esas identidades (ADR-0005).';
