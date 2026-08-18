-- =============================================================================
-- V18 — Un asiento de cobro y uno de reembolso por pedido, como mucho
-- =============================================================================
--
-- El ledger es inmutable, pero inmutable no es lo mismo que correcto: nada
-- impedia hasta ahora que el mismo pedido generase dos asientos de cobro o dos
-- de reembolso. Y eso no es teorico. Cuando una pasarela no recibe un 200 a
-- tiempo reenvia el evento, asi que la unica defensa era que el codigo se
-- acordase de comprobarlo antes de escribir.
--
-- Aqui esa defensa pasa a la base de datos. Duplicar dinero deja de depender de
-- que nadie se equivoque al escribir un manejador nuevo.
--
-- Se limita a los dos tipos que mueven dinero de un pedido concreto. Los demas
-- asientos (ajustes, correcciones, asientos compensatorios) siguen pudiendo
-- repetirse, que es justo para lo que existen.
--
-- Si algun dia se admiten reembolsos parciales, este indice hay que cambiarlo
-- por uno que incluya el importe o una referencia del reembolso en la pasarela.
-- Mientras solo se reembolse el total, un pedido no puede reembolsarse dos
-- veces.
-- =============================================================================

-- Si esta migracion falla al arrancar, es que ya hay duplicados. No se arreglan
-- borrando: el ledger es inmutable y hay un disparador que lo impide. Primero se
-- ve cuales son, y se decide a mano que hacer con ellos:
--
--   SELECT order_id, entry_type, count(*), sum(amount_cents)
--     FROM ledger_entries
--    WHERE entry_type IN ('PAYMENT_RECEIVED', 'REFUND')
--    GROUP BY order_id, entry_type
--   HAVING count(*) > 1;
--
-- Un duplicado ahi es dinero contado dos veces: hay que cuadrarlo con el extracto
-- de la pasarela antes de seguir, no saltarse el indice.

CREATE UNIQUE INDEX ledger_entries_un_asiento_por_pedido
    ON ledger_entries (order_id, entry_type)
 WHERE entry_type IN ('PAYMENT_RECEIVED', 'REFUND');

COMMENT ON INDEX ledger_entries_un_asiento_por_pedido IS
    'Impide cobrar o reembolsar dos veces el mismo pedido, aunque la pasarela reenvie el evento.';
