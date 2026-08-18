-- =============================================================================
-- V15 — La identidad manda, la IP acompaña
-- =============================================================================
--
-- V14 dejo el cupo por IP en 1: un solo jugador por conexion y dia. Se sube a
-- 10 tras revisarlo.
--
-- El razonamiento: lo que se quiere es "un voto por PERSONA al dia", y la IP no
-- identifica a una persona. En Latinoamerica los operadores moviles usan CGNAT
-- y meten miles de clientes detras de una IPv4; en una casa comparten conexion
-- los hermanos; en un cibercafe, veinte desconocidos.
--
--   * Con el cupo en 1, el primero que vota deja fuera a todos los demas ese
--     dia. Ninguno ha hecho nada malo y ninguno entiende por que.
--   * Con el cupo en 10, un hogar o un movil funcionan con normalidad, y treinta
--     nicks distintos desde la misma conexion siguen sin poder votar todos.
--
-- Quien realmente impide votar dos veces es la IDENTIDAD: si el nick 'Steve'
-- voto al servidor 1, no puede votar al 2 ese dia, salga por la IP que salga.
-- Eso no cambia. La IP pasa a ser lo que siempre debio ser: una señal para el
-- antifraude, no la regla.

UPDATE vote_settings
   SET max_players_per_ip = 10,
       updated_at = now()
 WHERE id IS TRUE
   AND max_players_per_ip = 1;

-- -----------------------------------------------------------------------------
-- Tokens escribibles a mano
-- -----------------------------------------------------------------------------
--
-- Los tokens eran 32 caracteres en base64. Perfecto para pulsar un enlace,
-- inservible para quien no puede pulsarlo.
--
-- Los jugadores de Bedrock no pueden hacer clic en enlaces del chat. Es una
-- parte enorme del publico hispanohablante —moviles y consolas— y con el
-- formato anterior no tenian forma de votar: ni pulsando ni copiando.
--
-- El token nuevo tiene 12 caracteres en tres grupos de cuatro (K7M2-9QX4-RT3F).
-- Se lee de un chat y se escribe en unos segundos. El alfabeto excluye I, L, O
-- y U para que nadie confunda un uno con una ele ni un cero con una o.
--
-- 12 caracteres sobre 32 simbolos son 60 bits. Con 10 minutos de vigencia, un
-- solo uso y el limite de trafico por IP, adivinar uno es inalcanzable.
--
-- La columna guarda el hash, no el token, asi que no hace falta migrar nada:
-- los tokens antiguos caducan solos en 10 minutos.

COMMENT ON COLUMN vote_tokens.token_hash IS
    'SHA-256 del codigo en claro. El codigo son 12 caracteres escribibles a mano: los jugadores de Bedrock no pueden pulsar enlaces del chat.';
