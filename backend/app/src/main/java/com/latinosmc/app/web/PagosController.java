package com.latinosmc.app.web;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.billing.CompraDeSlotService;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.ports.PaymentGateway;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Compra de espacios destacados. */
@RestController
@RequestMapping("/api/v1/pagos")
public class PagosController {

    private static final Logger log = LoggerFactory.getLogger(PagosController.class);

    private final CompraDeSlotService compras;
    private final BillingRepository facturacion;
    private final ServerRepository servidores;
    private final OrganizationRepository organizaciones;
    private final PaymentGateway pasarela;
    private final String sitioUrl;

    public PagosController(
            CompraDeSlotService compras,
            BillingRepository facturacion,
            ServerRepository servidores,
            OrganizationRepository organizaciones,
            PaymentGateway pasarela,
            @org.springframework.beans.factory.annotation.Value("${latinosmc.sitio-url}") String sitioUrl) {
        this.compras = compras;
        this.facturacion = facturacion;
        this.servidores = servidores;
        this.organizaciones = organizaciones;
        this.pasarela = pasarela;
        this.sitioUrl = sitioUrl.endsWith("/") ? sitioUrl.substring(0, sitioUrl.length() - 1) : sitioUrl;
    }

    /** Precios y disponibilidad de los cinco espacios. Publico: es un escaparate. */
    @GetMapping("/slots")
    public List<CompraDeSlotService.Disponibilidad> slots() {
        return compras.disponibilidad();
    }

    /**
     * Inicia la compra de un espacio.
     *
     * <p>El importe NO viene en la peticion: se lee de la base de datos. Aceptarlo
     * del cliente permitiria comprar un slot de 25 € por 1 céntimo — es el error
     * clasico en integraciones de pago y sigue apareciendo constantemente.
     */
    @PostMapping("/slots")
    public ResponseEntity<RespuestaCompra> comprarSlot(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @RequestBody @Valid PeticionCompra peticion,
            @RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia) {

        UUID organizacionId = comprobarPropiedad(usuario, peticion.servidorId());

        String clave = (claveIdempotencia == null || claveIdempotencia.isBlank())
                ? UUID.randomUUID().toString()
                : claveIdempotencia;

        var reserva = compras.iniciarCompra(
                peticion.slot(), peticion.servidorId(), organizacionId, usuario.id(), clave);

        var sesion = pasarela.crearSesion(
                reserva.pedidoId(),
                reserva.importeCentimos(),
                reserva.moneda(),
                "Espacio destacado " + peticion.slot(),
                sitioUrl + "/panel/pedidos/" + reserva.pedidoId(),
                sitioUrl + "/destacar");

        return ResponseEntity.status(reserva.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(new RespuestaCompra(
                        reserva.pedidoId(),
                        reserva.importeCentimos(),
                        reserva.moneda(),
                        reserva.desde(),
                        reserva.hasta(),
                        sesion.urlPago(),
                        pasarela.codigo()));
    }

    /** Pedidos de las organizaciones del usuario. */
    @GetMapping("/pedidos")
    public List<BillingRepository.Pedido> pedidos(@UsuarioActual IdentityRepository.Usuario usuario) {
        return organizaciones.organizacionesDe(usuario.id()).stream()
                .flatMap(org -> facturacion.pedidosDe(org.id()).stream())
                .toList();
    }

    /**
     * Webhook de la pasarela.
     *
     * <p>Sin autenticacion de sesion —la pasarela no tiene cuenta aqui— pero
     * <strong>con verificacion de firma obligatoria</strong>. Es lo unico que
     * separa un cobro real de un mensaje enviado por cualquiera que descubra esta
     * URL. Sin ella, la plataforma regalaria producto a quien supiera escribir un
     * JSON.
     *
     * <p>Se recibe el cuerpo como texto crudo a proposito: si se deserializara y
     * se volviera a serializar antes de comprobar la firma, dejaria de coincidir.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String cuerpoCrudo, jakarta.servlet.http.HttpServletRequest peticion) {

        // Se le pasa el lector de cabeceras entero, no una concreta: cada pasarela
        // usa las suyas —Stripe firma en una, PayPal reparte en cinco— y este
        // controlador no tiene por que conocer los nombres de ninguna.
        var evento = pasarela.verificarWebhook(cuerpoCrudo, peticion::getHeader).orElse(null);
        if (evento == null) {
            log.warn("Webhook de pago con firma invalida o no reconocido. Descartado.");
            // 400 y no 401: no se le confirma a quien pruebe que la URL existe y
            // funciona, solo que la peticion no valia.
            return ResponseEntity.badRequest().build();
        }

        return aplicar(evento, cuerpoCrudo);
    }

    /**
     * Confirma el pago cuando el usuario vuelve de la pasarela.
     *
     * <p>Existe por PayPal: aprueba primero y solo mueve el dinero cuando se llama
     * explicitamente a capturar. Sin este paso el usuario pagaria, volveria, y no
     * pasaria nada hasta que PayPal anulase la orden a los tres dias. Con Stripe,
     * que cobra solo, este endpoint no hace nada y responde "pendiente".
     *
     * <p><strong>Sin sesion a proposito.</strong> El usuario puede volver desde la
     * app movil de PayPal sin cookie, y perder el cobro por eso seria peor que el
     * riesgo de dejarlo abierto: la referencia es un identificador opaco emitido
     * por PayPal, el pedido se saca de la respuesta de PayPal —no de lo que manda
     * el cliente— y capturar dos veces la misma orden no duplica nada. Lo unico
     * que se consigue con una referencia inventada es un 200 con "pendiente".
     */
    @PostMapping("/confirmar")
    public ResponseEntity<RespuestaConfirmacion> confirmar(@RequestBody @Valid PeticionConfirmacion peticion) {
        var evento = pasarela.capturarAlVolver(peticion.referencia()).orElse(null);
        if (evento == null) {
            // Ni error ni exito: o la pasarela cobra sola (Stripe) o la orden aun
            // no esta aprobada. En ambos casos manda el webhook.
            return ResponseEntity.ok(new RespuestaConfirmacion("pendiente", null));
        }

        // El cuerpo guardado es una nota, no un mensaje firmado: aqui el evento
        // nace de nuestra propia llamada a la pasarela, no de algo que llego.
        ResponseEntity<Void> resultado = aplicar(evento, "{\"origen\":\"captura-al-volver\"}");
        if (!resultado.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(resultado.getStatusCode())
                    .body(new RespuestaConfirmacion("error", null));
        }
        return ResponseEntity.ok(new RespuestaConfirmacion("pagado", evento.pedidoId()));
    }

    /**
     * Aplica un evento de cobro ya verificado.
     *
     * <p>Compartido entre el webhook y la captura al volver: los dos caminos pueden
     * dispararse para el mismo pago —usuario que vuelve y webhook que llega— y el
     * registro de idempotencia es lo que impide que cuente dos veces.
     */
    private ResponseEntity<Void> aplicar(PaymentGateway.EventoDePago evento, String cuerpoCrudo) {
        // Coge el turno del evento. Solo lo niega si ya se termino con exito: un
        // intento anterior que fallo a medias sigue pendiente y hay que
        // reintentarlo (ADR-0021).
        boolean loCojo = facturacion.registrarWebhook(
                Ids.newId(), pasarela.codigo(), evento.eventoId(), evento.tipo(), cuerpoCrudo);

        if (!loCojo) {
            log.debug("Evento {} ya procesado; se responde 200 para que la pasarela deje de reintentar", evento.eventoId());
            return ResponseEntity.ok().build();
        }

        try {
            if ("payment.succeeded".equals(evento.tipo())) {
                compras.confirmarPago(evento.pedidoId(), pasarela.codigo(), evento.referencia());
            } else if ("payment.refunded".equals(evento.tipo())) {
                compras.registrarReembolso(evento.pedidoId(), "reembolso desde la pasarela");
            }
            facturacion.marcarWebhookProcesado(pasarela.codigo(), evento.eventoId(), null);

        } catch (DomainException.NotFound e) {
            // Definitivo: un pedido que no existe no va a aparecer por
            // reintentarlo. Se cierra el evento con el motivo escrito y se
            // responde 200 para que la pasarela pare. Queda en la tabla para
            // conciliar, porque un cobro sin pedido nuestro merece una mirada.
            facturacion.marcarWebhookProcesado(pasarela.codigo(), evento.eventoId(), e.getMessage());
            log.error("Evento de pago {} sobre un pedido desconocido. No se reintentara.", evento.eventoId(), e);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            // Transitorio hasta que se demuestre lo contrario. El evento se deja
            // PENDIENTE a proposito: si se cerrase aqui, el reenvio de la
            // pasarela lo descartaria por conocido y el cobro se quedaria sin
            // asiento ni reserva, en silencio.
            facturacion.anotarFalloDeWebhook(pasarela.codigo(), evento.eventoId(), e.getMessage());
            log.error("Fallo procesando el evento de pago {}. Queda pendiente de reintento.", evento.eventoId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok().build();
    }

    private UUID comprobarPropiedad(IdentityRepository.Usuario usuario, UUID servidorId) {
        UUID organizacionId = servidores
                .organizacionDe(servidorId)
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        if (!organizaciones.puedeGestionar(usuario.id(), organizacionId)) {
            throw new DomainException.NotFound(
                    "server.not_found", "Ese servidor no existe o no puedes gestionarlo.");
        }
        return organizacionId;
    }

    /** El importe NO se acepta del cliente: se lee de la base de datos. */
    public record PeticionCompra(@Min(1) @Max(5) int slot, UUID servidorId) {}

    /**
     * @param referencia identificador que la pasarela devuelve en la URL de retorno
     *     (en PayPal, el parametro {@code token}). Se limita el tamaño para que
     *     nadie use este campo para mandar megabytes.
     */
    public record PeticionConfirmacion(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 128)
                    String referencia) {}

    /** @param estado {@code pagado}, {@code pendiente} o {@code error} */
    public record RespuestaConfirmacion(String estado, UUID pedidoId) {}

    public record RespuestaCompra(
            UUID pedidoId,
            long importeCentimos,
            String moneda,
            Instant desde,
            Instant hasta,
            String urlPago,
            String pasarela) {}
}
