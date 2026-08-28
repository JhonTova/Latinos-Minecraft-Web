-- =============================================================================
-- V26 — Cinco servidores por cuenta
-- =============================================================================
--
-- El limite de tres venia de una escalera de planes que todavia no existe: no
-- hay pasarela de suscripcion, no hay pagina de precios y nadie puede contratar
-- Plus aunque quiera. Un tope pensado para empujar a un plan que no se puede
-- comprar solo consigue que quien tiene una red pequeña se quede fuera.
--
-- Cinco para todos hasta que haya planes de verdad. El limite sigue viviendo en
-- la tabla y no en el codigo (V9): el dia que existan, cambiarlo sera un UPDATE.
--
-- Solo se toca FREE. PLUS conserva su NULL (sin limite) para no reescribir a
-- mano las organizaciones a las que ya se les haya puesto.
-- =============================================================================

UPDATE plans
   SET max_servers = 5
 WHERE code = 'FREE';

COMMENT ON COLUMN organizations.plan_code IS
    'Plan contratado. Determina el limite de servidores. FREE permite 5; PLUS, ilimitados.';
