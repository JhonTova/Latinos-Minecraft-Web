-- =============================================================================
-- V20 — Retirada automatica de servidores que dejaron de dar señales
-- =============================================================================
--
-- Un directorio lleno de servidores muertos no es un directorio: es un cementerio
-- que el visitante recorre pulsando fichas que no llevan a ninguna parte. Y el
-- dueño de un servidor vivo compite en el ranking contra decenas que ya no
-- existen.
--
-- La señal es la del plugin. Cada vez que se autentica —late cada minuto— se
-- actualiza `server_api_keys.last_used_at`. Si esa marca lleva 15 dias parada,
-- el servidor no esta.
--
-- -----------------------------------------------------------------------------
-- SE ARCHIVA, NO SE BORRA
-- -----------------------------------------------------------------------------
--
-- El servidor sale del directorio y del ranking, que es lo que hacia falta. Pero
-- la fila se queda, y esto es deliberado:
--
--   * Quince dias sin señal no prueban que el servidor haya muerto. Prueban que
--     no hemos sabido de el. Una migracion de hosting, unas vacaciones o un mes
--     con la tarjeta caducada dan exactamente la misma señal.
--   * Borrar de verdad arrastraria sus votos y su historial de ranking, que son
--     de la plataforma y no solo suyos.
--   * Volver es automatico: en cuanto el plugin vuelve a latir, el servidor
--     reaparece donde estaba. Sin escribir a soporte y sin registrarse otra vez.
--
-- Un borrado irreversible por no haber dado señales dos semanas convierte un
-- problema pasajero del cliente en una perdida definitiva, y la llamada la
-- acabamos atendiendo nosotros.
--
-- Ver ADR-0024.
-- =============================================================================

ALTER TABLE servers
    -- Cuando se aviso al dueño de que su servidor iba a retirarse. Sin esto, el
    -- aviso se repetiria cada dia hasta que archivemos, que es como se ensena a
    -- la gente a filtrar tus correos.
    ADD COLUMN inactivity_warned_at TIMESTAMPTZ,

    -- Cuando se retiro por inactividad. Distinto de `deleted_at`, que significa
    -- "el dueño lo dio de baja" y no se deshace solo.
    ADD COLUMN archived_at          TIMESTAMPTZ;

COMMENT ON COLUMN servers.archived_at IS
    'Retirado por inactividad. Vuelve solo en cuanto el plugin late de nuevo.';

COMMENT ON COLUMN servers.inactivity_warned_at IS
    'Ultimo aviso de inactividad enviado al dueño. Evita repetirlo cada dia.';

-- La ronda de limpieza recorre los servidores vivos ordenando por su ultima
-- señal. Sin indice, cada noche escanearia la tabla entera.
CREATE INDEX idx_servers_vivos
    ON servers (status_code)
 WHERE deleted_at IS NULL;
