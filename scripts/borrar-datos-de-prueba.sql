-- =============================================================================
-- Borrado de los datos de prueba
-- =============================================================================
--
-- Deja la plataforma en cero: sin pedidos, sin votos, sin espacios ocupados.
-- CONSERVA usuarios, organizaciones, servidores, precios y administradores.
--
--   docker compose exec db psql -U latinosmc -d latinosmc \
--     -v confirmo=SI -f /scripts/borrar-datos-de-prueba.sql
--
-- Sin la variable `confirmo`, el script se niega a hacer nada. No es burocracia:
-- es un fichero que borra dinero y que va a seguir estando en el repositorio
-- cuando ya haya clientes de verdad.
--
-- -----------------------------------------------------------------------------
-- POR QUE ESTO NO DEBERIA EXISTIR DESPUES DE ABRIR AL PUBLICO
-- -----------------------------------------------------------------------------
--
-- El ledger es inmutable por diseño: hay dos triggers que rechazan cualquier
-- UPDATE o DELETE sobre asientos. Es una regla del proyecto, no una precaucion.
--
-- Este script usa TRUNCATE, que NO dispara esos triggers porque actuan por fila.
-- Dicho claro: **esto se salta la proteccion del ledger**. Es aceptable hoy
-- porque no hay un solo euro real dentro. En cuanto lo haya, la forma de
-- corregir el ledger es un asiento compensatorio, jamas un borrado, y este
-- script deja de tener ningun uso legitimo.
--
-- Si alguien lo ejecuta con datos reales, la contabilidad deja de cuadrar y no
-- hay forma de reconstruirla.
-- =============================================================================

\set ON_ERROR_STOP on

\if :{?confirmo}
\else
\echo ''
\echo 'ABORTADO: falta la confirmacion.'
\echo 'Vuelve a ejecutarlo con:  -v confirmo=SI'
\echo ''
\quit
\endif

BEGIN;

-- Aviso en el propio log de la base de datos. Si un dia hay que investigar por
-- que falta contabilidad, esto es lo que aparecera.
DO $$
BEGIN
    RAISE WARNING 'Borrando datos transaccionales, incluido el ledger. Solo es valido antes de tener cobros reales.';
END $$;

-- El orden no importa: TRUNCATE con varias tablas resuelve entre ellas las
-- claves ajenas. Las que apuntan a users, organizations o servers siguen
-- siendo validas porque esas tablas no se tocan.
TRUNCATE
    slot_bookings,
    brand_sponsors,
    ledger_entries,
    payment_webhooks,
    orders,
    vote_risk_signals,
    vote_rewards,
    vote_tokens,
    votes,
    outbox_deliveries,
    outbox,
    server_pings,
    email_verification_codes,
    audit_log;

COMMIT;

\echo ''
\echo 'Listo. Los seis espacios quedan libres y no hay ningun pedido.'
\echo 'Se conservan usuarios, organizaciones, servidores, precios y administradores.'
\echo ''

SELECT 'pedidos' AS tabla, COUNT(*) FROM orders
UNION ALL SELECT 'asientos del ledger', COUNT(*) FROM ledger_entries
UNION ALL SELECT 'espacios ocupados', COUNT(*) FROM slot_bookings
UNION ALL SELECT 'votos', COUNT(*) FROM votes
UNION ALL SELECT 'usuarios (se conservan)', COUNT(*) FROM users
UNION ALL SELECT 'servidores (se conservan)', COUNT(*) FROM servers;
