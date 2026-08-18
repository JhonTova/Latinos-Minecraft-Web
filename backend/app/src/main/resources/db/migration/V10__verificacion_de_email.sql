-- =============================================================================
-- V10 — Verificación de correo
-- =============================================================================
-- Sin esto, crear cuentas falsas cuesta cero: basta inventarse un correo. Y como
-- las cuentas dan acceso a registrar servidores y a comprar, una granja de
-- cuentas es el primer paso de casi cualquier abuso.
--
-- Verificar tambien arregla dos problemas menos obvios:
--   * Un correo mal escrito deja al usuario sin recuperacion de contrasena
--     posible, y sin forma de darse cuenta.
--   * Registrarse con el correo de otra persona deja de ser gratis.
--
-- Con Google o Discord NO se pide codigo: el proveedor ya verifico ese correo, y
-- pedirlo otra vez seria fricción sin ninguna ganancia.
-- =============================================================================

CREATE TABLE email_verification_codes (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Solo el hash. Un codigo de 6 digitos no es un secreto fuerte, pero si la
    -- base se filtra tampoco hay que regalar los codigos vivos.
    code_hash       TEXT NOT NULL,

    -- A que direccion se envio. Si el usuario corrige su correo, el codigo
    -- anterior deja de valer: verificar el correo viejo no prueba nada del nuevo.
    email           CITEXT NOT NULL,

    -- Un codigo de 6 digitos son un millon de combinaciones. Con intentos
    -- limitados, adivinarlo es inviable; sin limite, es cuestion de minutos.
    attempts        INT NOT NULL DEFAULT 0,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ
);

COMMENT ON TABLE email_verification_codes IS
    'Codigos de verificacion de correo. DATOS PERSONALES (email). Retencion: purga a los 7 dias (ADR-0010).';

CREATE INDEX email_verification_vigentes
    ON email_verification_codes (user_id, created_at DESC)
    WHERE consumed_at IS NULL;

CREATE INDEX email_verification_purga ON email_verification_codes (created_at);
