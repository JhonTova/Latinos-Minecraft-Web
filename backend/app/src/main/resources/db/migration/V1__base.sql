-- =============================================================================
-- V1 — Base del esquema
-- =============================================================================
-- Convenciones del proyecto:
--   * Claves primarias UUIDv7 generadas en la aplicacion (ADR-0002), no en la BD.
--     La BD no impone la generacion para que el identificador exista antes de
--     persistir: hace falta para el patron Outbox y para los tests.
--   * Todos los instantes son timestamptz y se almacenan en UTC (ADR-0011).
--   * Toda tabla con datos personales declara su politica de retencion en la
--     migracion que la crea (ADR-0010). Sin politica declarada, no se acepta.
-- =============================================================================

-- pgcrypto: hashes para seudonimizacion de IP e identidades (ADR-0010).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- citext: emails sin distincion de mayusculas. Evita que "Ana@x.com" y
-- "ana@x.com" se registren como dos cuentas distintas.
CREATE EXTENSION IF NOT EXISTS citext;

-- -----------------------------------------------------------------------------
-- Tipos de juego
-- -----------------------------------------------------------------------------
-- "Minecraft" NO se incrusta en el dominio (convenciones del proyecto, regla 13). La version,
-- la identidad del jugador, el protocolo de ping y el tipo de recompensa cuelgan
-- de un tipo de juego. Lo especifico de Minecraft vive en adaptadores aislados.
--
-- Coste hoy: una tabla de dos columnas. Coste de anadirlo despues: migrar el
-- esquema entero.
-- -----------------------------------------------------------------------------
CREATE TABLE game_types (
    code        TEXT PRIMARY KEY,
    name        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE game_types IS
    'Tipos de juego soportados. Evita incrustar Minecraft en el dominio. Retencion: permanente, sin datos personales.';

INSERT INTO game_types (code, name) VALUES
    ('MINECRAFT_JAVA', 'Minecraft: Java Edition');

-- -----------------------------------------------------------------------------
-- Tipos de identidad del votante
-- -----------------------------------------------------------------------------
-- Decision abierta 4.1 (DECISIONES.md): no esta resuelto si se aceptan servidores
-- en offline-mode. Modelamos la identidad como (tipo, valor) en lugar de asumir
-- un UUID de Mojang, para poder soportar ambos con reglas antifraude distintas
-- sin cambiar el esquema.
--
--   MOJANG_UUID   -> verificable contra Mojang. Confianza alta.
--   OFFLINE_NAME  -> texto arbitrario del servidor. Confianza nula.
--                    Ojo: vector de inyeccion (ADR-0012).
-- -----------------------------------------------------------------------------
CREATE TABLE identity_types (
    code            TEXT PRIMARY KEY,
    name            TEXT    NOT NULL,
    is_verifiable   BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE identity_types IS
    'Como se identifica a un votante. is_verifiable decide que reglas antifraude aplican. Retencion: permanente.';

INSERT INTO identity_types (code, name, is_verifiable) VALUES
    ('MOJANG_UUID',  'UUID verificado por Mojang', TRUE),
    ('OFFLINE_NAME', 'Nombre en servidor offline-mode', FALSE);
