-- =============================================================================
-- V11 — Imágenes de servidor (icono y banner)
-- =============================================================================
-- Se guarda la CLAVE del archivo en el almacén, no una URL completa.
--
-- El motivo es práctico: si mañana se cambia de proveedor, de dominio o de CDN,
-- una URL guardada en cada fila obliga a migrar toda la tabla. Una clave
-- relativa solo obliga a cambiar el prefijo en la configuración.
--
-- Nada de SVG (ADR-0017): un SVG puede contener JavaScript, y servir uno subido
-- por un usuario desde nuestro dominio es ejecutar su código con nuestra sesión.
-- =============================================================================

ALTER TABLE servers
    ADD COLUMN icon_key   TEXT,
    ADD COLUMN banner_key TEXT;

COMMENT ON COLUMN servers.icon_key IS
    'Clave del icono en el almacen. Siempre PNG reprocesado por nosotros (ADR-0017).';
COMMENT ON COLUMN servers.banner_key IS
    'Clave del banner en el almacen. PNG/WebP reprocesado, o GIF validado con limite de tamano.';

-- -----------------------------------------------------------------------------
-- Registro de subidas
-- -----------------------------------------------------------------------------
-- Tabla aparte porque hace falta para tres cosas que la columna sola no permite:
--   * Saber qué archivos quedaron huérfanos al reemplazar una imagen, para
--     borrarlos del almacén y no pagar por bytes que nadie mira.
--   * Auditar quién subió qué, si alguien sube algo que no debe.
--   * Detectar abuso: alguien subiendo mil imágenes se ve aquí.
-- -----------------------------------------------------------------------------
CREATE TABLE uploads (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    uploaded_by     UUID          REFERENCES users (id) ON DELETE SET NULL,

    storage_key     TEXT NOT NULL,
    content_type    TEXT NOT NULL,
    size_bytes      BIGINT NOT NULL,

    -- Dimensiones tras el reprocesado, no las del archivo original.
    width           INT,
    height          INT,

    purpose         TEXT NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

COMMENT ON TABLE uploads IS
    'Archivos subidos. Retencion: se borran del almacen 7 dias despues de quedar huerfanos.';

CREATE UNIQUE INDEX uploads_clave_unica ON uploads (storage_key);

CREATE INDEX uploads_por_organizacion ON uploads (organization_id, created_at DESC);

-- Para el proceso de limpieza de huerfanos.
CREATE INDEX uploads_huerfanos
    ON uploads (deleted_at)
    WHERE deleted_at IS NOT NULL;
