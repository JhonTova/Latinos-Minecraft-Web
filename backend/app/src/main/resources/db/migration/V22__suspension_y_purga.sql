-- =============================================================================
-- V22 — Suspensión, purga por inactividad y limpieza de archivos (ADR-0028)
-- =============================================================================
--
-- Tres huecos que se descubrieron juntos:
--
--   1. 'SUSPENDED' existía en el catálogo desde la V3 y ninguna línea de código
--      lo usaba. No había forma de moderar un servidor.
--   2. Un servidor archivado por inactividad se quedaba archivado para siempre,
--      con sus imágenes ocupando espacio y sus claves de API vivas.
--   3. La limpieza de huérfanos que el ADR-0017 daba por hecha nunca existió.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Suspensión
-- -----------------------------------------------------------------------------
--
-- Es una decisión NUESTRA sobre alguien que hizo algo, no una constatación de
-- que no hay nadie en casa. Por eso no caduca y no purga nunca: la levanta una
-- persona, o se queda (ADR-0028).
--
-- Colgar un temporizador de borrado de la suspensión destruiría la prueba de
-- por qué suspendimos a alguien justo cuando más falta hace —cuando vuelve, o
-- reclama, o abre otra cuenta— y le bastaría con esperar treinta días para que
-- su historial se limpiara solo.

ALTER TABLE servers
    ADD COLUMN suspended_at     TIMESTAMPTZ,
    ADD COLUMN suspended_reason TEXT,
    -- Quién la puso. ON DELETE SET NULL: si esa cuenta desaparece, la suspensión
    -- sigue en pie. Lo contrario permitiría levantar una suspensión borrando al
    -- administrador que la puso.
    ADD COLUMN suspended_by     UUID REFERENCES users (id) ON DELETE SET NULL;

COMMENT ON COLUMN servers.suspended_at IS
    'Suspension administrativa. No caduca y no purga: la levanta una persona (ADR-0028).';

ALTER TABLE servers
    ADD CONSTRAINT servers_motivo_suspension_acotado
        CHECK (suspended_reason IS NULL OR length(suspended_reason) <= 500);

-- -----------------------------------------------------------------------------
-- Purga por inactividad
-- -----------------------------------------------------------------------------
--
-- NO borra la fila, y no es una preferencia: `votes` y `vote_rewards` apuntan
-- aquí con ON DELETE RESTRICT, así que PostgreSQL rechazaría el borrado. Y
-- `slot_bookings` apunta con ON DELETE CASCADE, así que un borrado se llevaría
-- por delante y en silencio el registro de espacios destacados YA PAGADOS.
--
-- Lo que se elimina es lo que cuesta dinero o estorba: las imágenes del almacén,
-- las claves de API y lo leído del ping. Lo que se conserva es lo que el resto
-- de la plataforma necesita: los votos, que son las señales con las que el
-- antifraude detecta patrones en OTROS servidores.

ALTER TABLE servers
    ADD COLUMN purged_at       TIMESTAMPTZ,
    ADD COLUMN purge_warned_at TIMESTAMPTZ;

COMMENT ON COLUMN servers.purged_at IS
    'Cuando se le quitaron imagenes y claves por inactividad prolongada. No vuelve solo (ADR-0028).';

-- Candidatos a purgar: archivados por nosotros, no suspendidos y no purgados
-- todavía. Índice parcial porque la consulta nunca mira a los demás.
CREATE INDEX servers_para_purgar
    ON servers (archived_at)
    WHERE deleted_at IS NULL
      AND archived_at IS NOT NULL
      AND purged_at IS NULL
      AND suspended_at IS NULL;

-- -----------------------------------------------------------------------------
-- Archivos huérfanos
-- -----------------------------------------------------------------------------
--
-- `uploads.deleted_at` se marcaba desde el primer día y nadie lo leía jamás:
-- cada icono o banner reemplazado seguía en el almacén desde entonces. El
-- ADR-0027, que genera imágenes solo, convierte eso de conveniente en necesario.
--
-- Se separa "marcado como huérfano" de "ya borrado del almacén" porque son dos
-- momentos distintos y hay siete días entre medias: con una sola columna, un
-- fallo a mitad del borrado dejaría filas que no se sabe si tienen archivo
-- detrás o no.

ALTER TABLE uploads
    ADD COLUMN purged_at TIMESTAMPTZ;

COMMENT ON COLUMN uploads.purged_at IS
    'Cuando se borro el archivo del almacen. La fila se conserva: es el registro de quien subio que.';

-- Los que toca borrar: marcados huérfanos y con el archivo todavía puesto.
CREATE INDEX uploads_pendientes_de_borrar
    ON uploads (deleted_at)
    WHERE deleted_at IS NOT NULL AND purged_at IS NULL;
