package com.latinosmc.billing;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compra de un slot destacado (ADR-0013).
 *
 * <p>Precio fijo, no subasta. Cola FIFO por slot: si esta ocupado, el comprador
 * entra en la siguiente rotacion <em>con fechas exactas</em>, nunca con un
 * "proximamente" sin compromiso.
 */
@Service
public class CompraDeSlotService {

    private static final Logger log = LoggerFactory.getLogger(CompraDeSlotService.class);

    private final BillingRepository repositorio;
    private final Clock reloj;

    public CompraDeSlotService(BillingRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /** Disponibilidad de los cinco espacios, para pintar la pagina de destacar. */
    @Transactional(readOnly = true)
    public List<Disponibilidad> disponibilidad() {
        Instant ahora = Instant.now(reloj);

        return repositorio.slots().stream()
                .map(slot -> {
                    Instant desde = repositorio.finDeLaCola(slot.posicion(), ahora).orElse(ahora);
                    int enCola = repositorio.periodosEnCola(slot.posicion(), ahora);
                    boolean agotado = enCola >= slot.maxAdelanto();

                    return new Disponibilidad(
                            slot.posicion(),
                            slot.precioCentimos(),
                            slot.moneda(),
                            slot.diasPeriodo(),
                            slot.activo() && !agotado,
                            desde,
                            desde.plus(Duration.ofDays(slot.diasPeriodo())),
                            enCola);
                })
                .toList();
    }

    /**
     * Reserva el siguiente periodo libre de un slot y crea el pedido.
     *
     * <p>El pedido nace en PENDING: la reserva solo se materializa cuando la
     * pasarela confirma el cobro. Reservar antes de cobrar permitiria bloquear
     * los cinco espacios gratis simplemente iniciando compras y no pagandolas.
     */
    @Transactional
    public Reserva iniciarCompra(
            int posicion, UUID servidorId, UUID organizacionId, UUID usuarioId, String claveIdempotencia) {

        // Idempotencia primero: un doble clic no puede generar dos pedidos.
        var yaExiste = repositorio.buscarPorClaveIdempotencia(claveIdempotencia);
        if (yaExiste.isPresent()) {
            return Reserva.repetida(yaExiste.get());
        }

        var slot = repositorio
                .slot(posicion)
                .filter(BillingRepository.Slot::activo)
                .orElseThrow(() -> new DomainException.NotFound(
                        "slot.not_available", "Ese espacio no esta disponible ahora mismo."));

        Instant ahora = Instant.now(reloj);

        if (repositorio.periodosEnCola(posicion, ahora) >= slot.maxAdelanto()) {
            throw new DomainException.Conflict(
                    "slot.queue_full",
                    "Ese espacio esta reservado durante las proximas semanas. Prueba con otro o vuelve mas adelante.");
        }

        Instant desde = repositorio.finDeLaCola(posicion, ahora).orElse(ahora);
        Instant hasta = desde.plus(Duration.ofDays(slot.diasPeriodo()));

        UUID pedidoId = Ids.newId();
        String metadatos =
                """
                {"tipo":"FEATURED_SLOT","slot":%d,"servidorId":"%s","desde":"%s","hasta":"%s"}"""
                        .formatted(posicion, servidorId, desde, hasta);

        boolean creado = repositorio.crearPedido(
                pedidoId,
                organizacionId,
                usuarioId,
                "FEATURED_SLOT",
                slot.precioCentimos(),
                slot.moneda(),
                claveIdempotencia,
                metadatos);

        if (!creado) {
            return repositorio
                    .buscarPorClaveIdempotencia(claveIdempotencia)
                    .map(Reserva::repetida)
                    .orElseThrow(() -> new IllegalStateException("Conflicto de idempotencia sin pedido asociado"));
        }

        log.info("Compra iniciada: slot {} para servidor {} del {} al {}", posicion, servidorId, desde, hasta);
        return new Reserva(pedidoId, posicion, slot.precioCentimos(), slot.moneda(), desde, hasta, false);
    }

    /**
     * Confirma el cobro: materializa la reserva y anota el asiento contable.
     *
     * <p>Lo llama el manejador de webhooks, ya verificada la firma. Todo ocurre en
     * una transaccion: o hay reserva y asiento, o no hay ninguna de las dos.
     */
    @Transactional
    public void confirmarPago(UUID pedidoId, String pasarela, String referencia) {
        var pedido = repositorio
                .buscarPedido(pedidoId)
                .orElseThrow(() -> new DomainException.NotFound("order.not_found", "Pedido desconocido."));

        int filas = repositorio.marcarPagado(pedidoId, pasarela, referencia, Instant.now(reloj));
        if (filas == 0) {
            // Ya estaba pagado. Ocurre cuando la pasarela reenvia el webhook, que
            // es lo normal, no un error: se sale sin duplicar nada.
            log.debug("Pedido {} ya estaba pagado; webhook duplicado ignorado", pedidoId);
            return;
        }

        // El asiento se registra SIEMPRE que entra dinero, sea cual sea el
        // producto. Es lo que permite cuadrar la contabilidad sin depender de
        // recorrer tablas de negocio.
        repositorio.registrarAsiento(
                Ids.newId(),
                pedido.organizacionId(),
                pedidoId,
                "PAYMENT_RECEIVED",
                pedido.importeCentimos(),
                pedido.moneda(),
                "Cobro de " + pedido.producto(),
                null);

        if ("FEATURED_SLOT".equals(pedido.producto())) {
            materializarReserva(pedidoId);
        }
    }

    /**
     * Registra un reembolso.
     *
     * <p>No modifica el asiento original: anade uno de signo contrario que lo
     * referencia. Asi el historial sigue explicando como se llego al saldo
     * actual, que es justamente para lo que sirve un libro mayor.
     */
    @Transactional
    public void registrarReembolso(UUID pedidoId, String motivo) {
        var pedido = repositorio
                .buscarPedido(pedidoId)
                .orElseThrow(() -> new DomainException.NotFound("order.not_found", "Pedido desconocido."));

        // Un reenvio del evento de reembolso no puede devolver el dinero dos
        // veces en el libro. El indice unico de V18 tambien lo impide, pero
        // salir aqui deja el reenvio como lo que es —normal— y no como un error.
        if (repositorio.existeAsiento(pedidoId, "REFUND")) {
            log.debug("Pedido {} ya tenia asiento de reembolso; evento duplicado ignorado", pedidoId);
            return;
        }

        repositorio.registrarAsiento(
                Ids.newId(),
                pedido.organizacionId(),
                pedidoId,
                "REFUND",
                -pedido.importeCentimos(),
                pedido.moneda(),
                "Reembolso: " + motivo,
                null);
    }

    /**
     * Convierte un pedido pagado en una reserva real del slot.
     *
     * <p>Las fechas y el slot se leen de los metadatos que guardamos NOSOTROS al
     * crear el pedido, nunca de lo que llegue en el webhook. La pasarela confirma
     * que se cobro; qué se compró lo decidimos aquí.
     */
    private void materializarReserva(UUID pedidoId) {
        var metadatos = repositorio.metadatosDePedido(pedidoId);
        if (metadatos.isEmpty()) {
            log.error("Pedido {} pagado sin metadatos de slot: requiere revision manual", pedidoId);
            return;
        }

        var datos = metadatos.get();
        try {
            repositorio.crearReserva(
                    Ids.newId(),
                    datos.slot(),
                    datos.servidorId(),
                    pedidoId,
                    datos.desde(),
                    datos.hasta());

            log.info("Slot {} reservado para el servidor {} del {} al {}",
                    datos.slot(), datos.servidorId(), datos.desde(), datos.hasta());

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // La restriccion de exclusion de la base de datos impidio un solape.
            // Solo puede pasar si dos compras del mismo hueco se pagaron a la vez.
            // El cobro es valido, asi que NO se revierte: se marca para revision
            // y se reasigna a mano. Perder el dinero del cliente seria peor que
            // una intervencion manual.
            log.error(
                    "Solape al reservar el slot del pedido {}. El cobro es valido: reasignar manualmente.",
                    pedidoId,
                    e);
        }
    }

    /**
     * @param desde inicio del periodo comprado. Siempre fecha exacta, nunca un
     *     "proximamente".
     */
    public record Reserva(
            UUID pedidoId,
            int posicion,
            long importeCentimos,
            String moneda,
            Instant desde,
            Instant hasta,
            boolean yaExistia) {

        static Reserva repetida(BillingRepository.Pedido pedido) {
            return new Reserva(
                    pedido.id(), 0, pedido.importeCentimos(), pedido.moneda(), null, null, true);
        }
    }

    /**
     * @param disponible false si el slot esta desactivado o su cola esta llena
     * @param proximoDesde cuando empezaria el periodo que se compre ahora
     */
    public record Disponibilidad(
            int posicion,
            long precioCentimos,
            String moneda,
            int diasPeriodo,
            boolean disponible,
            Instant proximoDesde,
            Instant proximoHasta,
            int periodosEnCola) {}
}
