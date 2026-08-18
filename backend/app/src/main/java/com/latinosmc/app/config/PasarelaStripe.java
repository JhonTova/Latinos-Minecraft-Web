package com.latinosmc.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.PaymentGateway;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Pasarela Stripe usando Checkout alojado.
 *
 * <p>El usuario paga en el formulario de Stripe, no en el nuestro. Aqui nunca
 * pasa un numero de tarjeta (ADR-0004): eso mantiene el alcance PCI-DSS en el
 * minimo (SAQ-A) y elimina de raiz la clase de incidente mas cara que existe.
 *
 * <p><strong>Sin el SDK oficial, a proposito.</strong> Solo se necesitan dos
 * operaciones —crear una sesion y verificar una firma— y el SDK arrastra su
 * propia pila HTTP, su propio manejo de reintentos y un ciclo de versiones que
 * habria que seguir. Con HTTP directo el codigo cabe en una pantalla y se ve
 * exactamente que se envia y que se comprueba. Si algun dia hacen falta
 * suscripciones o facturacion recurrente, ahi si compensara el SDK.
 */
@Component
@ConditionalOnExpression("'${latinosmc.stripe.secret-key:}'.trim() != ''")
public class PasarelaStripe implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PasarelaStripe.class);

    private static final String API = "https://api.stripe.com/v1";

    /**
     * Margen para el desfase de reloj entre Stripe y nosotros.
     *
     * <p>Es la proteccion contra repeticion: sin este limite, alguien que capture
     * un webhook valido podria reenviarlo dentro de un año y seguiria pasando la
     * verificacion de firma. Cinco minutos es lo que recomienda Stripe.
     */
    private static final Duration TOLERANCIA_FIRMA = Duration.ofMinutes(5);

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final ObjectMapper json;
    private final String claveSecreta;
    private final String secretoWebhook;

    public PasarelaStripe(
            ObjectMapper json,
            @Value("${latinosmc.stripe.secret-key}") String claveSecreta,
            @Value("${latinosmc.stripe.webhook-secret:}") String secretoWebhook) {
        this.json = json;
        this.claveSecreta = claveSecreta;
        this.secretoWebhook = secretoWebhook;

        if (secretoWebhook == null || secretoWebhook.isBlank()) {
            // Aviso alto y claro: sin secreto de webhook, los cobros se crean pero
            // nunca se confirman. Es un fallo silencioso que se descubre cuando un
            // cliente pregunta por que pago y no tiene nada.
            log.warn("Stripe activo SIN secreto de webhook. Los pagos no se confirmaran solos.");
        }
    }

    @Override
    public String codigo() {
        return "STRIPE";
    }

    @Override
    public boolean estaConfigurada() {
        return claveSecreta != null && !claveSecreta.isBlank();
    }

    @Override
    public SesionDePago crearSesion(
            UUID pedidoId,
            long importeCentimos,
            String moneda,
            String descripcion,
            String urlExito,
            String urlCancelacion) {

        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("mode", "payment");
        parametros.put("success_url", urlExito);
        parametros.put("cancel_url", urlCancelacion);

        // Nuestro identificador viaja de ida y vuelta. Es lo que permite saber a
        // que pedido corresponde el cobro cuando llega el webhook.
        parametros.put("client_reference_id", pedidoId.toString());
        parametros.put("metadata[pedidoId]", pedidoId.toString());

        parametros.put("line_items[0][quantity]", "1");
        parametros.put("line_items[0][price_data][currency]", moneda.toLowerCase());
        // Stripe trabaja en la unidad minima de la moneda, igual que nosotros.
        // Que ambos usemos centimos enteros evita cualquier conversion y cualquier
        // error de redondeo.
        parametros.put("line_items[0][price_data][unit_amount]", String.valueOf(importeCentimos));
        parametros.put("line_items[0][price_data][product_data][name]", descripcion);

        JsonNode respuesta = pedir("/checkout/sessions", parametros, pedidoId.toString());

        String url = texto(respuesta, "url");
        String id = texto(respuesta, "id");
        if (url == null || id == null) {
            throw new DomainException.Conflict(
                    "payment.session_failed", "No se pudo iniciar el pago. Intentalo de nuevo.");
        }
        return new SesionDePago(url, id);
    }

    /**
     * Verifica la firma de un webhook de Stripe.
     *
     * <p>Es la comprobacion mas importante del modulo de pagos. Sin ella,
     * cualquiera que descubra la URL puede enviar un "pago completado" falso y
     * llevarse el producto sin pagar.
     *
     * <p>La cabecera tiene la forma {@code t=1614556800,v1=abc...}. Se firma la
     * cadena {@code timestamp.cuerpo} con el secreto del endpoint y se compara con
     * lo que llega, <strong>en tiempo constante</strong>: una comparacion normal
     * filtraria, por el tiempo de respuesta, cuantos caracteres iniciales se
     * acertaron, y eso permite reconstruir la firma byte a byte.
     */
    @Override
    public Optional<EventoDePago> verificarWebhook(
            String cuerpoCrudo, java.util.function.Function<String, String> cabecera) {

        String cabeceraFirma = cabecera.apply("Stripe-Signature");
        if (secretoWebhook == null || secretoWebhook.isBlank() || cabeceraFirma == null) {
            return Optional.empty();
        }

        String marcaDeTiempo = null;
        String firmaRecibida = null;
        for (String parte : cabeceraFirma.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                marcaDeTiempo = kv[1];
            } else if ("v1".equals(kv[0]) && firmaRecibida == null) {
                firmaRecibida = kv[1];
            }
        }

        if (marcaDeTiempo == null || firmaRecibida == null) {
            return Optional.empty();
        }

        // Proteccion contra repeticion. Sin esto, un webhook valido capturado hoy
        // seguiria siendo valido dentro de un año.
        try {
            Instant momento = Instant.ofEpochSecond(Long.parseLong(marcaDeTiempo));
            if (Duration.between(momento, Instant.now()).abs().compareTo(TOLERANCIA_FIRMA) > 0) {
                log.warn("Webhook de Stripe fuera de la ventana de tiempo. Descartado.");
                return Optional.empty();
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        String esperada = hmacHex(marcaDeTiempo + "." + cuerpoCrudo);
        if (!MessageDigest.isEqual(
                esperada.getBytes(StandardCharsets.UTF_8), firmaRecibida.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Webhook de Stripe con firma invalida. Descartado.");
            return Optional.empty();
        }

        return interpretarEvento(cuerpoCrudo);
    }

    private Optional<EventoDePago> interpretarEvento(String cuerpoCrudo) {
        try {
            JsonNode evento = json.readTree(cuerpoCrudo);
            String tipo = texto(evento, "type");
            JsonNode objeto = evento.path("data").path("object");

            UUID pedidoId = extraerPedido(objeto);
            if (pedidoId == null) {
                // Evento de Stripe que no es nuestro o no lleva referencia. No es
                // un error: se ignora sin ruido.
                return Optional.empty();
            }

            String nuestroTipo = switch (tipo == null ? "" : tipo) {
                case "checkout.session.completed", "payment_intent.succeeded" -> "payment.succeeded";
                case "charge.refunded", "refund.created" -> "payment.refunded";
                default -> null;
            };

            if (nuestroTipo == null) {
                return Optional.empty();
            }

            return Optional.of(new EventoDePago(
                    texto(evento, "id"),
                    nuestroTipo,
                    pedidoId,
                    primerNoNulo(texto(objeto, "payment_intent"), texto(objeto, "id")),
                    objeto.path("amount_total").asLong(objeto.path("amount").asLong(0)),
                    texto(objeto, "currency")));

        } catch (IOException e) {
            log.warn("Webhook de Stripe con cuerpo ilegible");
            return Optional.empty();
        }
    }

    private UUID extraerPedido(JsonNode objeto) {
        String referencia = primerNoNulo(
                texto(objeto, "client_reference_id"), texto(objeto.path("metadata"), "pedidoId"));
        if (referencia == null) {
            return null;
        }
        try {
            return UUID.fromString(referencia);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @param claveIdempotencia la enviamos a Stripe para que un reintento nuestro
     *     no cree dos sesiones de pago
     */
    private JsonNode pedir(String ruta, Map<String, String> parametros, String claveIdempotencia) {
        String cuerpo = codificar(parametros);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API + ruta))
                .header("Authorization", "Bearer " + claveSecreta)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Idempotency-Key", claveIdempotencia)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 400) {
                // El detalle va al log, nunca al usuario: los errores de Stripe
                // pueden revelar configuracion de la cuenta.
                log.error("Stripe respondio {}: {}", respuesta.statusCode(), respuesta.body());
                throw new DomainException.Conflict(
                        "payment.gateway_error", "No se pudo iniciar el pago. Intentalo de nuevo.");
            }
            return json.readTree(respuesta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException.Conflict("payment.interrupted", "Pago interrumpido.");
        } catch (IOException e) {
            throw new DomainException.Conflict(
                    "payment.unreachable", "No hemos podido contactar con la pasarela. Intentalo en un momento.");
        }
    }

    private String hmacHex(String mensaje) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular la firma", e);
        }
    }

    private String codificar(Map<String, String> valores) {
        StringBuilder sb = new StringBuilder();
        valores.forEach((clave, valor) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(clave, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private String primerNoNulo(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}
