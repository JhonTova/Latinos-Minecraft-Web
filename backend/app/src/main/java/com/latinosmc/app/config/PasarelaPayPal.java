package com.latinosmc.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.PaymentGateway;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Pasarela PayPal (Orders API v2).
 *
 * <p>Mas facil de arrancar que Stripe: una cuenta y dos claves de
 * developer.paypal.com, sin verificacion de empresa ni datos fiscales por
 * adelantado. Y en el mercado hispanohablante mucha gente ya tiene cuenta, lo
 * que suele convertir mejor.
 *
 * <p><strong>Diferencia importante con Stripe:</strong> PayPal aprueba el pago y
 * despues hay que <em>capturarlo</em> con una llamada explicita. Hasta esa
 * llamada, el dinero no se mueve. Por eso existe {@link #capturarAlVolver}: el
 * usuario vuelve del sitio de PayPal, se captura y ahi termina el cobro.
 *
 * <p>El webhook se mantiene como red de seguridad, por si el usuario cierra el
 * navegador antes de volver.
 *
 * <p>Es {@code @Primary} para que, si algun dia estan configuradas las dos
 * pasarelas a la vez, gane esta de forma predecible en vez de que la aplicacion
 * no arranque por ambiguedad.
 */
@Component
@org.springframework.context.annotation.Primary
@ConditionalOnExpression("'${latinosmc.paypal.client-id:}'.trim() != ''")
public class PasarelaPayPal implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PasarelaPayPal.class);

    /**
     * Forma de un identificador de orden de PayPal.
     *
     * <p>Se comprueba porque ese valor llega del navegador y se pega en una URL:
     * sin acotarlo, algo como {@code ../../v1/otra-cosa} apuntaria a una ruta
     * distinta de la API de PayPal.
     */
    private static final java.util.regex.Pattern ORDEN_VALIDA =
            java.util.regex.Pattern.compile("^[A-Za-z0-9-]{5,64}$");

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper json;
    private final String clienteId;
    private final String secreto;
    private final String webhookId;
    private final String api;

    /** Token de acceso cacheado: PayPal los da con caducidad y pedir uno por peticion es tirar tiempo. */
    private volatile String tokenCacheado;

    private volatile Instant tokenCaducaEn = Instant.EPOCH;

    public PasarelaPayPal(
            ObjectMapper json,
            @Value("${latinosmc.paypal.client-id}") String clienteId,
            @Value("${latinosmc.paypal.secret:}") String secreto,
            @Value("${latinosmc.paypal.webhook-id:}") String webhookId,
            @Value("${latinosmc.paypal.modo:sandbox}") String modo) {

        this.json = json;
        this.clienteId = clienteId;
        this.secreto = secreto;
        this.webhookId = webhookId;
        // sandbox = pruebas con dinero falso. live = dinero real.
        this.api = "live".equalsIgnoreCase(modo)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";

        log.info("PayPal activo en modo {}", "live".equalsIgnoreCase(modo) ? "REAL" : "pruebas (sandbox)");

        if (webhookId == null || webhookId.isBlank()) {
            log.warn("PayPal sin webhook-id. Los pagos se confirmaran al volver el usuario, "
                    + "pero si cierra el navegador antes, habra que confirmarlos a mano.");
        }
    }

    @Override
    public String codigo() {
        return "PAYPAL";
    }

    @Override
    public boolean estaConfigurada() {
        return clienteId != null && !clienteId.isBlank() && secreto != null && !secreto.isBlank();
    }

    @Override
    public SesionDePago crearSesion(
            UUID pedidoId,
            long importeCentimos,
            String moneda,
            String descripcion,
            String urlExito,
            String urlCancelacion) {

        // PayPal trabaja en unidades con decimales, no en centimos. La conversion
        // se hace con enteros y se formatea al final: pasar por double aqui es
        // como se cuelan los errores de un centimo que luego nadie encuentra.
        String importe = "%d.%02d".formatted(importeCentimos / 100, Math.abs(importeCentimos % 100));

        String cuerpo =
                """
                {
                  "intent": "CAPTURE",
                  "purchase_units": [{
                    "reference_id": "%s",
                    "description": %s,
                    "custom_id": "%s",
                    "amount": { "currency_code": "%s", "value": "%s" }
                  }],
                  "payment_source": {
                    "paypal": {
                      "experience_context": {
                        "user_action": "PAY_NOW",
                        "return_url": %s,
                        "cancel_url": %s
                      }
                    }
                  }
                }"""
                        .formatted(
                                pedidoId,
                                comillas(recortar(descripcion, 127)),
                                pedidoId,
                                moneda.toUpperCase(),
                                importe,
                                comillas(urlExito),
                                comillas(urlCancelacion));

        JsonNode respuesta = pedir("/v2/checkout/orders", cuerpo, pedidoId.toString());

        String id = texto(respuesta, "id");
        String enlace = enlaceDeAprobacion(respuesta);

        if (id == null || enlace == null) {
            throw new DomainException.Conflict(
                    "payment.session_failed", "No se pudo iniciar el pago. Intentalo de nuevo.");
        }
        return new SesionDePago(enlace, id);
    }

    /**
     * Captura el pago cuando el usuario vuelve de PayPal.
     *
     * <p>Hasta esta llamada el dinero NO se ha movido: PayPal solo tenia la
     * autorizacion del comprador. Es la diferencia principal con Stripe.
     */
    @Override
    public Optional<EventoDePago> capturarAlVolver(String referenciaProveedor) {
        // La referencia se concatena en la URL de PayPal, asi que se acota a lo
        // que PayPal emite de verdad. Sin esto, un valor con barras o con '%'
        // podria alcanzar otra ruta de la API distinta de la que se pretende.
        if (referenciaProveedor == null || !ORDEN_VALIDA.matcher(referenciaProveedor).matches()) {
            return Optional.empty();
        }

        JsonNode respuesta;
        try {
            respuesta = pedir(
                    "/v2/checkout/orders/" + referenciaProveedor + "/capture", "{}", referenciaProveedor);
        } catch (DomainException e) {
            // Orden inexistente, ya capturada o aun sin aprobar. Ninguno es un
            // error que deba ver el usuario: si el cobro existe, el webhook lo
            // confirmara. Devolver vacio deja la pagina en "pendiente".
            log.info("No se pudo capturar la orden de PayPal: {}", e.getMessage());
            return Optional.empty();
        }

        String estado = texto(respuesta, "status");
        if (!"COMPLETED".equals(estado)) {
            log.warn("Captura de PayPal en estado {}", estado);
            return Optional.empty();
        }

        JsonNode unidad = respuesta.path("purchase_units").path(0);
        JsonNode captura = unidad.path("payments").path("captures").path(0);

        // custom_id viaja en la unidad de compra o en la captura segun la version
        // de la respuesta; reference_id es el ultimo recurso.
        UUID pedidoId = aUuid(
                texto(unidad, "custom_id"), texto(captura, "custom_id"), texto(unidad, "reference_id"));
        if (pedidoId == null) {
            return Optional.empty();
        }

        return Optional.of(new EventoDePago(
                // El id de la captura sirve de clave de idempotencia: si el usuario
                // recarga la pagina de retorno, la segunda vez se detecta repetida.
                "capture-" + texto(captura, "id"),
                "payment.succeeded",
                pedidoId,
                texto(captura, "id"),
                centimos(captura.path("amount")),
                texto(captura.path("amount"), "currency_code")));
    }

    /**
     * Verifica un webhook de PayPal.
     *
     * <p>PayPal no firma con HMAC como Stripe: hay que preguntarle a PayPal si la
     * firma es autentica. Menos elegante y mas lento —una llamada de red por
     * webhook— pero es el metodo oficial, y aqui la correccion importa mas que la
     * elegancia.
     *
     * <p>Sin webhook-id configurado no se acepta nada, que es el comportamiento
     * seguro: mejor perder una confirmacion que aceptar un cobro falso.
     */
    @Override
    public Optional<EventoDePago> verificarWebhook(
            String cuerpoCrudo, java.util.function.Function<String, String> cabecera) {

        if (webhookId == null || webhookId.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode evento = json.readTree(cuerpoCrudo);
            String tipo = texto(evento, "event_type");

            // Solo CAPTURE.COMPLETED cuenta como cobro. CHECKOUT.ORDER.APPROVED
            // significa "el usuario dijo que si", no "el dinero se movio": en el
            // flujo de captura de PayPal el dinero no sale hasta que llamamos a
            // /capture. Tratar "aprobado" como "pagado" regalaria el espacio a
            // quien aprobara y cerrara el navegador, y PayPal anularia la orden a
            // los tres dias sin cobrar nada.
            String nuestroTipo = switch (tipo == null ? "" : tipo) {
                case "PAYMENT.CAPTURE.COMPLETED" -> "payment.succeeded";
                case "PAYMENT.CAPTURE.REFUNDED", "PAYMENT.CAPTURE.REVERSED" -> "payment.refunded";
                default -> null;
            };
            if (nuestroTipo == null) {
                return Optional.empty();
            }

            if (!firmaAutentica(cuerpoCrudo, cabecera)) {
                log.warn("Webhook de PayPal con firma no verificada. Descartado.");
                return Optional.empty();
            }

            JsonNode recurso = evento.path("resource");
            UUID pedidoId = aUuid(texto(recurso, "custom_id"), null);
            if (pedidoId == null) {
                return Optional.empty();
            }

            return Optional.of(new EventoDePago(
                    texto(evento, "id"),
                    nuestroTipo,
                    pedidoId,
                    texto(recurso, "id"),
                    centimos(recurso.path("amount")),
                    texto(recurso.path("amount"), "currency_code")));

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Pregunta a PayPal si la firma del webhook es suya.
     *
     * <p>Las cinco cabeceras van juntas: son inseparables. Si falta una, PayPal no
     * puede verificar nada y se rechaza sin llegar a preguntar.
     */
    private boolean firmaAutentica(String cuerpoCrudo, java.util.function.Function<String, String> cabecera) {
        try {
            String[] partes = {
                cabecera.apply("PAYPAL-AUTH-ALGO"),
                cabecera.apply("PAYPAL-CERT-URL"),
                cabecera.apply("PAYPAL-TRANSMISSION-ID"),
                cabecera.apply("PAYPAL-TRANSMISSION-SIG"),
                cabecera.apply("PAYPAL-TRANSMISSION-TIME")
            };
            for (String parte : partes) {
                if (parte == null || parte.isBlank()) {
                    return false;
                }
            }

            String cuerpo =
                    """
                    {
                      "auth_algo": %s,
                      "cert_url": %s,
                      "transmission_id": %s,
                      "transmission_sig": %s,
                      "transmission_time": %s,
                      "webhook_id": %s,
                      "webhook_event": %s
                    }"""
                            .formatted(
                                    comillas(partes[0]),
                                    comillas(partes[1]),
                                    comillas(partes[2]),
                                    comillas(partes[3]),
                                    comillas(partes[4]),
                                    comillas(webhookId),
                                    cuerpoCrudo);

            JsonNode respuesta = pedir("/v1/notifications/verify-webhook-signature", cuerpo, null);
            return "SUCCESS".equals(texto(respuesta, "verification_status"));

        } catch (RuntimeException e) {
            // Ante la duda, se rechaza. Un webhook perdido se recupera; un cobro
            // falso aceptado, no.
            log.warn("No se pudo verificar la firma de PayPal: {}", e.getMessage());
            return false;
        }
    }

    // --- HTTP ----------------------------------------------------------------

    private synchronized String token() {
        if (tokenCacheado != null && Instant.now().isBefore(tokenCaducaEn)) {
            return tokenCacheado;
        }

        String credenciales = Base64.getEncoder()
                .encodeToString((clienteId + ":" + secreto).getBytes(StandardCharsets.UTF_8));

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(api + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + credenciales)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 400) {
                log.error("PayPal rechazo las credenciales: {}", respuesta.statusCode());
                throw new DomainException.Conflict(
                        "payment.gateway_error", "No se pudo iniciar el pago. Intentalo de nuevo.");
            }
            JsonNode datos = json.readTree(respuesta.body());
            tokenCacheado = texto(datos, "access_token");
            // Se renueva un minuto antes de caducar, para no usar uno justo
            // vencido en una peticion en curso.
            tokenCaducaEn = Instant.now().plusSeconds(Math.max(60, datos.path("expires_in").asLong(3600) - 60));
            return tokenCacheado;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException.Conflict("payment.interrupted", "Pago interrumpido.");
        } catch (IOException e) {
            throw new DomainException.Conflict(
                    "payment.unreachable", "No hemos podido contactar con PayPal. Intentalo en un momento.");
        }
    }

    private JsonNode pedir(String ruta, String cuerpo, String claveIdempotencia) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(api + ruta))
                .header("Authorization", "Bearer " + token())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo));

        if (claveIdempotencia != null) {
            // Evita que un reintento nuestro cree dos pedidos o capture dos veces.
            builder.header("PayPal-Request-Id", claveIdempotencia);
        }

        try {
            HttpResponse<String> respuesta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 400) {
                log.error("PayPal respondio {} en {}: {}", respuesta.statusCode(), ruta, respuesta.body());
                throw new DomainException.Conflict(
                        "payment.gateway_error", "No se pudo completar la operacion. Intentalo de nuevo.");
            }
            return json.readTree(respuesta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException.Conflict("payment.interrupted", "Pago interrumpido.");
        } catch (IOException e) {
            throw new DomainException.Conflict(
                    "payment.unreachable", "No hemos podido contactar con PayPal. Intentalo en un momento.");
        }
    }

    // --- utilidades ----------------------------------------------------------

    private String enlaceDeAprobacion(JsonNode respuesta) {
        for (JsonNode enlace : respuesta.path("links")) {
            if ("payer-action".equals(texto(enlace, "rel")) || "approve".equals(texto(enlace, "rel"))) {
                return texto(enlace, "href");
            }
        }
        return null;
    }

    /** Convierte "25.00" a 2500 sin pasar por coma flotante. */
    private long centimos(JsonNode importe) {
        String valor = texto(importe, "value");
        if (valor == null) {
            return 0;
        }
        String[] partes = valor.split("\\.");
        long enteros = Long.parseLong(partes[0]);
        long decimales = partes.length > 1 ? Long.parseLong((partes[1] + "00").substring(0, 2)) : 0;
        return enteros * 100 + decimales;
    }

    private UUID aUuid(String... candidatos) {
        for (String candidato : candidatos) {
            if (candidato == null || candidato.isBlank()) {
                continue;
            }
            try {
                return UUID.fromString(candidato);
            } catch (IllegalArgumentException ignorado) {
                // Siguiente candidato.
            }
        }
        return null;
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    /** Escapa un texto para incrustarlo como cadena JSON. */
    private String comillas(String valor) {
        if (valor == null) {
            return "null";
        }
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }

    private String recortar(String valor, int max) {
        if (valor == null) {
            return "";
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
