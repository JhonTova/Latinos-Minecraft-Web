-- =============================================================================
-- V13 — Administradores de la plataforma
-- =============================================================================
--
-- Hasta ahora solo existian roles DENTRO de una organizacion: quien manda en su
-- propio servidor. Falta el otro eje: quien manda en la plataforma entera y
-- puede cambiar precios o mirar los pedidos de todo el mundo.
--
-- Tabla aparte y no una columna 'is_admin' en users, por tres motivos:
--
--   1. Deja rastro. Quien concedio el permiso y cuando queda escrito. Con una
--      columna booleana, un UPDATE no deja ni una pista de quien lo hizo.
--   2. Se revoca sin perder el historial: se marca la fecha, no se borra la
--      fila. Importa saber que alguien fue administrador entre dos fechas
--      cuando haya que revisar quien cambio un precio.
--   3. Es dificil de activar por accidente. Un UPDATE mal escrito sobre users
--      puede hacer administrador a todo el mundo de golpe; aqui haria falta un
--      INSERT deliberado por cada persona.
--
-- El primer administrador se crea a mano con SQL (ver DESPLIEGUE.md). No hay
-- endpoint de "hazme administrador": un arranque asi, aunque solo funcione
-- cuando la tabla esta vacia, es una puerta que solo hace falta una vez y que
-- luego queda abierta para siempre.
--
-- Los CAMBIOS que haga un administrador NO se registran aqui: van a audit_log
-- (V6), que ya existe y ademas seudonimiza al actor cuando vence su plazo de
-- retencion (ADR-0010). Una segunda tabla de auditoria seria el mismo concepto
-- por duplicado y, peor, se saltaria ese plazo.
-- =============================================================================

CREATE TABLE platform_admins (
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,

    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Quien lo concedio. NULL solo para el primero, creado a mano al desplegar.
    granted_by  UUID REFERENCES users (id) ON DELETE SET NULL,

    revoked_at  TIMESTAMPTZ,

    notes       TEXT
);

COMMENT ON TABLE platform_admins IS
    'Quien puede administrar la plataforma entera. Distinto de organization_members, que es por organizacion.';

-- La consulta de permisos se hace en CADA peticion administrativa: tiene que
-- resolverse por indice, no escaneando la tabla.
CREATE INDEX idx_platform_admins_vigentes
    ON platform_admins (user_id)
 WHERE revoked_at IS NULL;
