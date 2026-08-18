package com.latinosmc.billing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a pedidos, slots y ledger. Solo el modulo {@code billing} toca estas tablas. */
@Repository
public class BillingRepository {

    private final JdbcClient jdbc;

    public BillingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // --- Slots ---------------------------------------------------------------

    public List<Slot> slots() {
        return jdbc.sql(
                        """
                        SELECT position, price_cents, currency, period_days, max_ahead, enabled
                          FROM featured_slots
                         ORDER BY position
                        """)
                .query((rs, fila) -> new Slot(
                        rs.getInt("position"),
                        rs.getLong("price_cents"),
                        rs.getString("currency"),
                        rs.getInt("period_days"),
                        rs.getInt("max_ahead"),
                        rs.getBoolean("enabled")))
                .list();
    }

    public Optional<Slot> slot(int posicion) {
        return jdbc.sql(
                        """
                        SELECT position, price_cents, currency, period_days, max_ahead, enabled
                          FROM featured_slots WHERE position = :posicion
                        """)
                .param("posicion", posicion)
                .query((rs, fila) -> new Slot(
                        rs.getInt("position"),
                        rs.getLong("price_cents"),
                        rs.getString("currency"),
                        rs.getInt("period_days"),
                        rs.getInt("max_ahead"),
                        rs.getBoolean("enabled")))
                .optional();
    }

    /**
     * Cambia la configuracion de un espacio.
     *
     * <p><strong>No afecta a lo ya vendido.</strong> Los periodos reservados
     * guardan sus propias fechas y el pedido guarda su propio importe: subir el
     * precio hoy no recalcula lo que alguien pago la semana pasada, ni acorta un
     * periodo que ya esta corriendo. Es lo que hace que el precio se pueda tocar
     * sin miedo.
     */
    public int actualizarSlot(
            int posicion, long precioCentimos, int diasPeriodo, int maxAdelanto, boolean activo) {
        return jdbc.sql(
                        """
                        UPDATE featured_slots
                           SET price_cents = :precio,
                               period_days = :dias,
                               max_ahead   = :adelanto,
                               enabled     = :activo,
                               updated_at  = now()
                         WHERE position = :posicion
                        """)
                .param("posicion", posicion)
                .param("precio", precioCentimos)
                .param("dias", diasPeriodo)
                .param("adelanto", maxAdelanto)
                .param("activo", activo)
                .update();
    }

    /**
     * Fin del ultimo periodo reservado de un slot.
     *
     * <p>Es lo que define la cola FIFO: el siguiente comprador empieza justo
     * cuando termina el anterior. Vacio significa que el slot esta libre ahora.
     */
    public Optional<Instant> finDeLaCola(int posicion, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT MAX(ends_at) FROM slot_bookings
                         WHERE slot_position = :posicion
                           AND cancelled_at IS NULL
                           AND ends_at > :ahora
                        """)
                .param("posicion", posicion)
                .param("ahora", Timestamp.from(ahora))
                .query(Instant.class)
                .optional();
    }

    public int periodosEnCola(int posicion, Instant ahora) {
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*) FROM slot_bookings
                         WHERE slot_position = :posicion
                           AND cancelled_at IS NULL
                           AND ends_at > :ahora
                        """)
                .param("posicion", posicion)
                .param("ahora", Timestamp.from(ahora))
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }

    public void crearReserva(
            UUID id, int posicion, UUID servidorId, UUID pedidoId, Instant desde, Instant hasta) {
        jdbc.sql(
                        """
                        INSERT INTO slot_bookings (id, slot_position, server_id, order_id, starts_at, ends_at)
                        VALUES (:id, :posicion, :servidor, :pedido, :desde, :hasta)
                        """)
                .param("id", id)
                .param("posicion", posicion)
                .param("servidor", servidorId)
                .param("pedido", pedidoId)
                .param("desde", Timestamp.from(desde))
                .param("hasta", Timestamp.from(hasta))
                .update();
    }

    /** Slots ocupados ahora mismo, para pintar el bloque de destacados. */
    public List<SlotOcupado> destacadosVigentes(Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT b.slot_position, s.id AS server_id, s.slug, s.name, s.description,
                               s.icon_key, s.banner_key, s.host, s.port,
                               s.version_min, s.version_max, s.hosting_region, b.ends_at
                          FROM slot_bookings b
                          JOIN servers s ON s.id = b.server_id
                         WHERE b.cancelled_at IS NULL
                           AND b.starts_at <= :ahora
                           AND b.ends_at > :ahora
                           AND s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                         ORDER BY b.slot_position
                        """)
                .param("ahora", Timestamp.from(ahora))
                .query((rs, fila) -> new SlotOcupado(
                        rs.getInt("slot_position"),
                        rs.getObject("server_id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("icon_key"),
                        rs.getString("banner_key"),
                        rs.getString("host"),
                        rs.getObject("port", Integer.class),
                        rs.getString("version_min"),
                        rs.getString("version_max"),
                        rs.getString("hosting_region"),
                        rs.getTimestamp("ends_at").toInstant()))
                .list();
    }

    /**
     * Fecha en que termina el ultimo espacio destacado pagado de un servidor.
     *
     * <p>Vacio si no tiene ninguno vigente. Se consulta antes de dar de baja el
     * servidor: alguien que acaba de pagar una semana de portada y borra el
     * servidor por error pierde el dinero, y esa llamada la acabamos atendiendo
     * nosotros.
     */
    public Optional<Instant> destacadoVigenteDe(UUID servidorId, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT MAX(ends_at) FROM slot_bookings
                         WHERE server_id = :servidor
                           AND cancelled_at IS NULL
                           AND ends_at > :ahora
                        """)
                .param("servidor", servidorId)
                .param("ahora", Timestamp.from(ahora))
                .query(Instant.class)
                .optional();
    }

    // --- Pedidos -------------------------------------------------------------

    public Optional<Pedido> buscarPorClaveIdempotencia(String clave) {
        return jdbc.sql(
                        """
                        SELECT id, organization_id, product_code, status_code, amount_cents,
                               currency, gateway, gateway_reference
                          FROM orders WHERE idempotency_key = :clave
                        """)
                .param("clave", clave)
                .query(BillingRepository::mapearPedido)
                .optional();
    }

    public Optional<Pedido> buscarPedido(UUID id) {
        return jdbc.sql(
                        """
                        SELECT id, organization_id, product_code, status_code, amount_cents,
                               currency, gateway, gateway_reference
                          FROM orders WHERE id = :id
                        """)
                .param("id", id)
                .query(BillingRepository::mapearPedido)
                .optional();
    }

    /**
     * Crea el pedido. Devuelve {@code false} si otra peticion gano la carrera con
     * la misma clave: la proteccion es el indice unico, no una comprobacion
     * previa en codigo.
     */
    public boolean crearPedido(
            UUID id,
            UUID organizacionId,
            UUID usuarioId,
            String producto,
            long importeCentimos,
            String moneda,
            String claveIdempotencia,
            String metadatosJson) {

        int filas = jdbc.sql(
                        """
                        INSERT INTO orders (id, organization_id, created_by, product_code, status_code,
                                            amount_cents, currency, idempotency_key, metadata)
                        VALUES (:id, :org, :usuario, :producto, 'PENDING',
                                :importe, :moneda, :clave, CAST(:metadatos AS jsonb))
                        ON CONFLICT (idempotency_key) DO NOTHING
                        """)
                .param("id", id)
                .param("org", organizacionId)
                .param("usuario", usuarioId)
                .param("producto", producto)
                .param("importe", importeCentimos)
                .param("moneda", moneda)
                .param("clave", claveIdempotencia)
                .param("metadatos", metadatosJson)
                .update();
        return filas > 0;
    }

    /**
     * Marca un pedido como pagado.
     *
     * <p>La condicion {@code status_code = 'PENDING'} viaja dentro del UPDATE. Es
     * lo que hace que un webhook reenviado por la pasarela no vuelva a
     * contabilizar el mismo cobro: la segunda vez afecta a cero filas.
     *
     * @return filas afectadas; 0 significa que ya estaba pagado
     */
    public int marcarPagado(UUID pedidoId, String pasarela, String referencia, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE orders
                           SET status_code = 'PAID', gateway = :pasarela,
                               gateway_reference = :referencia, paid_at = :ahora
                         WHERE id = :id AND status_code = 'PENDING'
                        """)
                .param("id", pedidoId)
                .param("pasarela", pasarela)
                .param("referencia", referencia)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    public int marcarEstado(UUID pedidoId, String estado, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE orders
                           SET status_code = :estado,
                               cancelled_at = CASE WHEN :estado IN ('CANCELLED','FAILED') THEN :ahora ELSE cancelled_at END
                         WHERE id = :id AND status_code = 'PENDING'
                        """)
                .param("id", pedidoId)
                .param("estado", estado)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    public List<Pedido> pedidosDe(UUID organizacionId) {
        return jdbc.sql(
                        """
                        SELECT id, organization_id, product_code, status_code, amount_cents,
                               currency, gateway, gateway_reference
                          FROM orders
                         WHERE organization_id = :org
                         ORDER BY created_at DESC
                         LIMIT 100
                        """)
                .param("org", organizacionId)
                .query(BillingRepository::mapearPedido)
                .list();
    }

    /**
     * Todos los pedidos, para administracion.
     *
     * <p>Con tope: una pantalla de administracion no necesita diez mil filas, y
     * una consulta sin limite es la que un dia tumba la base de datos sin que
     * nadie sepa por que.
     */
    public List<Pedido> todosLosPedidos(int limite) {
        return jdbc.sql(
                        """
                        SELECT id, organization_id, product_code, status_code, amount_cents,
                               currency, gateway, gateway_reference
                          FROM orders
                         ORDER BY created_at DESC
                         LIMIT :limite
                        """)
                .param("limite", Math.clamp(limite, 1, 500))
                .query(BillingRepository::mapearPedido)
                .list();
    }

    /**
     * Cuanto dinero ha entrado, segun el ledger.
     *
     * <p>Se calcula sobre {@code ledger_entries} y no sumando {@code orders}: el
     * ledger es inmutable y contiene tambien los reembolsos como asientos
     * negativos. Sumar pedidos daria una cifra que no cuadra con la contabilidad
     * en cuanto haya una sola devolucion.
     */
    public List<Ingresos> ingresosPorMoneda() {
        return jdbc.sql(
                        """
                        SELECT currency,
                               COALESCE(SUM(amount_cents), 0) AS total,
                               COUNT(*) AS asientos
                          FROM ledger_entries
                         GROUP BY currency
                         ORDER BY currency
                        """)
                .query((rs, fila) ->
                        new Ingresos(rs.getString("currency"), rs.getLong("total"), rs.getInt("asientos")))
                .list();
    }

    /** @param totalCentimos neto: los reembolsos son asientos negativos y restan */
    public record Ingresos(String moneda, long totalCentimos, int asientos) {}

    /**
     * Metadatos del slot comprado en un pedido.
     *
     * <p>Se leen del JSON que guardamos NOSOTROS al crear el pedido. La pasarela
     * confirma que se cobro; que se compro lo decidimos aqui. Fiarse de datos que
     * vengan en el webhook permitiria manipular que se entrega.
     */
    public Optional<MetadatosDeSlot> metadatosDePedido(UUID pedidoId) {
        return jdbc.sql(
                        """
                        SELECT (metadata->>'slot')::int        AS slot,
                               (metadata->>'servidorId')::uuid AS servidor,
                               (metadata->>'desde')::timestamptz AS desde,
                               (metadata->>'hasta')::timestamptz AS hasta
                          FROM orders
                         WHERE id = :id
                           AND metadata->>'tipo' = 'FEATURED_SLOT'
                        """)
                .param("id", pedidoId)
                .query((rs, fila) -> new MetadatosDeSlot(
                        rs.getInt("slot"),
                        rs.getObject("servidor", UUID.class),
                        rs.getTimestamp("desde").toInstant(),
                        rs.getTimestamp("hasta").toInstant()))
                .optional();
    }

    public record MetadatosDeSlot(int slot, UUID servidorId, Instant desde, Instant hasta) {}

    // --- Ledger --------------------------------------------------------------

    /**
     * Registra un asiento.
     *
     * <p>Solo hay INSERT: la tabla tiene un disparador que rechaza UPDATE y
     * DELETE. Corregir un asiento es anadir otro de signo contrario.
     */
    public void registrarAsiento(
            UUID id,
            UUID organizacionId,
            UUID pedidoId,
            String tipo,
            long importeCentimos,
            String moneda,
            String descripcion,
            UUID revierteA) {

        jdbc.sql(
                        """
                        INSERT INTO ledger_entries (id, organization_id, order_id, entry_type,
                                                    amount_cents, currency, description, reverses_entry_id)
                        VALUES (:id, :org, :pedido, :tipo, :importe, :moneda, :descripcion, :revierte)
                        """)
                .param("id", id)
                .param("org", organizacionId)
                .param("pedido", pedidoId)
                .param("tipo", tipo)
                .param("importe", importeCentimos)
                .param("moneda", moneda)
                .param("descripcion", descripcion)
                .param("revierte", revierteA)
                .update();
    }

    /**
     * ¿Hay ya un asiento de ese tipo para el pedido?
     *
     * <p>El indice unico de V18 lo impide de todas formas, pero saltar antes de
     * escribir convierte un reenvio —que es lo normal, no un error— en una
     * salida limpia en lugar de en una excepcion de integridad.
     */
    public boolean existeAsiento(UUID pedidoId, String tipo) {
        return jdbc.sql(
                        """
                        SELECT EXISTS (
                            SELECT 1 FROM ledger_entries
                             WHERE order_id = :pedido AND entry_type = :tipo
                        )
                        """)
                .param("pedido", pedidoId)
                .param("tipo", tipo)
                .query(Boolean.class)
                .single();
    }

    // --- Webhooks ------------------------------------------------------------

    /**
     * Toma el turno para procesar un evento de la pasarela.
     *
     * <p>La condicion no es "no lo habiamos visto" sino "no lo hemos terminado".
     * Un evento que se recibio pero fallo a medias sigue estando pendiente, y
     * cuando la pasarela lo reenvia hay que volver a intentarlo: si se
     * descartara por conocido, un cobro real se quedaria sin asiento y sin
     * reserva para siempre, y ademas en silencio (ADR-0021).
     *
     * <p>Un evento ya terminado con exito no se vuelve a coger nunca. Es lo que
     * impide que un reenvio cuente el pago dos veces.
     *
     * @return {@code false} si ese evento ya se proceso correctamente
     */
    public boolean registrarWebhook(
            UUID id, String pasarela, String eventoId, String tipo, String payloadJson) {

        int filas = jdbc.sql(
                        """
                        INSERT INTO payment_webhooks (id, gateway, event_id, event_type, payload)
                        VALUES (:id, :pasarela, :evento, :tipo, CAST(:payload AS jsonb))
                        ON CONFLICT (gateway, event_id) DO UPDATE
                           SET received_at = now(),
                               error = NULL
                         WHERE payment_webhooks.processed_at IS NULL
                        """)
                .param("id", id)
                .param("pasarela", pasarela)
                .param("evento", eventoId)
                .param("tipo", tipo)
                .param("payload", payloadJson)
                .update();
        return filas > 0;
    }

    /**
     * Da el evento por terminado: no se volvera a procesar.
     *
     * <p>Se llama tanto cuando salio bien como cuando fallo de forma definitiva
     * —un pedido que no existe no va a existir por reintentarlo—. La diferencia
     * queda en {@code error}, que es lo que se mira al conciliar.
     */
    public void marcarWebhookProcesado(String pasarela, String eventoId, String error) {
        jdbc.sql(
                        """
                        UPDATE payment_webhooks
                           SET processed_at = now(), error = :error
                         WHERE gateway = :pasarela AND event_id = :evento
                        """)
                .param("pasarela", pasarela)
                .param("evento", eventoId)
                .param("error", error)
                .update();
    }

    /**
     * Anota un fallo y deja el evento pendiente para el siguiente reintento.
     *
     * <p>Deliberadamente NO toca {@code processed_at}: mientras siga en nulo, el
     * reenvio de la pasarela vuelve a coger el turno y se intenta otra vez.
     */
    public void anotarFalloDeWebhook(String pasarela, String eventoId, String error) {
        jdbc.sql(
                        """
                        UPDATE payment_webhooks
                           SET error = :error
                         WHERE gateway = :pasarela AND event_id = :evento
                        """)
                .param("pasarela", pasarela)
                .param("evento", eventoId)
                .param("error", error)
                .update();
    }

    private static Pedido mapearPedido(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        return new Pedido(
                rs.getObject("id", UUID.class),
                rs.getObject("organization_id", UUID.class),
                rs.getString("product_code"),
                rs.getString("status_code"),
                rs.getLong("amount_cents"),
                rs.getString("currency"),
                rs.getString("gateway"),
                rs.getString("gateway_reference"));
    }

    /**
     * @param precioCentimos siempre entero. Jamas decimales flotantes: 0.1 + 0.2
     *     no da 0.3 y en contabilidad eso es un descuadre que aparece meses
     *     despues.
     */
    public record Slot(
            int posicion, long precioCentimos, String moneda, int diasPeriodo, int maxAdelanto, boolean activo) {}

    public record SlotOcupado(
            int posicion,
            UUID servidorId,
            String slug,
            String nombre,
            String descripcion,
            String claveIcono,
            String claveBanner,
            String host,
            Integer puerto,
            String versionMinima,
            String versionMaxima,
            String region,
            Instant terminaEn) {}

    public record Pedido(
            UUID id,
            UUID organizacionId,
            String producto,
            String estado,
            long importeCentimos,
            String moneda,
            String pasarela,
            String referencia) {}
}
