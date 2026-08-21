-- =============================================================================
-- V23 — Compatibilidad con Bedrock (ADR-0030)
-- =============================================================================
--
-- Casi ningún servidor latino es Bedrock NATIVO. Lo que hay son servidores Java
-- con Geyser, que aceptan jugadores de móvil y consola. Eso no es otro tipo de
-- juego: es una propiedad del servidor Java.
--
-- Por eso una columna y no una fila nueva en `game_types`. Un tipo de juego
-- nuevo arrastraría otro protocolo de ping entero (Bedrock usa RakNet sobre UDP,
-- nada que ver con el Server List Ping de Java) y otro modelo de identidad
-- (gamertags de Xbox Live). Nada de eso hace falta para responder la pregunta
-- que de verdad se hace el jugador: «¿puedo entrar desde el móvil?».
--
-- Lo declara el dueño y NO se verifica. Igual que la región (V17): detectar
-- Geyser desde fuera no es fiable, y por ADR-0009 un dato declarado nunca
-- alimenta el ranking. Este solo filtra.
-- =============================================================================

ALTER TABLE servers
    ADD COLUMN supports_bedrock BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN servers.supports_bedrock IS
    'El servidor acepta jugadores de Bedrock (movil/consola), normalmente via Geyser. Declarado por el dueño, no verificado. Solo filtra: nunca puntua (ADR-0009).';

-- -----------------------------------------------------------------------------
-- Índices de los dos rankings
-- -----------------------------------------------------------------------------
--
-- El directorio pasa a tener DOS clasificaciones (ADR-0030): una de servidores
-- que solo admiten cuentas originales y otra de los que admiten también cuentas
-- no premium. Las dos consultas filtran por `accepts_unverifiable_identities`,
-- así que entra en el índice de activos que ya existía.

DROP INDEX IF EXISTS servers_activos;

CREATE INDEX servers_activos
    ON servers (game_type_code, accepts_unverifiable_identities)
    WHERE status_code = 'ACTIVE' AND deleted_at IS NULL;

COMMENT ON INDEX servers_activos IS
    'Sirve a los dos rankings: el de solo premium y el de premium + no premium (ADR-0030).';

-- Para el filtro de Bedrock del listado.
CREATE INDEX servers_bedrock
    ON servers (supports_bedrock)
    WHERE status_code = 'ACTIVE' AND deleted_at IS NULL AND supports_bedrock;
