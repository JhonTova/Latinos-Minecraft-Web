package com.latinosmc.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que se comprueba aqui es que <strong>ningun webhook llega a darse por bueno
 * sin pasar por PayPal</strong>.
 *
 * <p>PayPal no firma con HMAC: hay que preguntarle a el si la firma es suya. Eso
 * significa que la unica forma de aceptar un pago falso seria que el codigo se
 * saltara esa llamada. Todos los casos de abajo se quedan cortos antes de
 * llegar a la red — y por eso mismo estos tests no tocan internet: si alguno lo
 * hiciera, seria la señal de que la verificacion se esta saltando.
 */
class PasarelaPayPalTest {

    private static final UUID PEDIDO = UUID.fromString("018f3a2b-1c4d-7e8f-9a0b-1c2d3e4f5a6b");

    private final ObjectMapper json = new ObjectMapper();

    private PasarelaPayPal conWebhook(String webhookId) {
        return new PasarelaPayPal(json, "cliente-de-prueba", "secreto-de-prueba", webhookId, "sandbox");
    }

    /** Las cinco cabeceras que PayPal envia con cada webhook. */
    private Function<String, String> cabecerasCompletas() {
        Map<String, String> cabeceras = Map.of(
                "PAYPAL-AUTH-ALGO", "SHA256withRSA",
                "PAYPAL-CERT-URL", "https://api.sandbox.paypal.com/v1/notifications/certs/CERT-1",
                "PAYPAL-TRANSMISSION-ID", "trans-1",
                "PAYPAL-TRANSMISSION-SIG", "firma-cualquiera",
                "PAYPAL-TRANSMISSION-TIME", "2026-08-11T12:00:00Z");
        return cabeceras::get;
    }

    private String capturaCompletada() {
        return """
                {"id":"WH-EVT-1","event_type":"PAYMENT.CAPTURE.COMPLETED",
                 "resource":{"id":"CAP-1","custom_id":"%s",
                 "amount":{"currency_code":"EUR","value":"25.00"}}}"""
                .formatted(PEDIDO);
    }

    @Test
    @DisplayName("sin webhook-id configurado no se acepta ningun webhook")
    void sinWebhookId() {
        assertThat(conWebhook("").verificarWebhook(capturaCompletada(), cabecerasCompletas()))
                .isEmpty();
    }

    /**
     * El ataque directo: alguien descubre la URL y manda un cobro inventado sin
     * molestarse en firmar.
     */
    @Test
    @DisplayName("rechaza un webhook sin cabeceras de firma")
    void sinCabeceras() {
        assertThat(conWebhook("WH-1").verificarWebhook(capturaCompletada(), nombre -> null))
                .isEmpty();
    }

    /**
     * Variante mas fina: se copian cuatro cabeceras de un webhook real y se omite
     * una. Las cinco son inseparables; con cuatro, PayPal no puede verificar nada.
     */
    @Test
    @DisplayName("rechaza un webhook al que le falta una sola cabecera")
    void cabeceraIncompleta() {
        Function<String, String> completas = cabecerasCompletas();
        Function<String, String> menosUna =
                nombre -> "PAYPAL-TRANSMISSION-SIG".equals(nombre) ? null : completas.apply(nombre);

        assertThat(conWebhook("WH-1").verificarWebhook(capturaCompletada(), menosUna))
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza un webhook con una cabecera vacia")
    void cabeceraVacia() {
        Function<String, String> completas = cabecerasCompletas();
        Function<String, String> conVacia =
                nombre -> "PAYPAL-TRANSMISSION-ID".equals(nombre) ? "  " : completas.apply(nombre);

        assertThat(conWebhook("WH-1").verificarWebhook(capturaCompletada(), conVacia)).isEmpty();
    }

    /**
     * <strong>El error de dinero mas facil de cometer con PayPal.</strong>
     *
     * <p>{@code CHECKOUT.ORDER.APPROVED} significa "el usuario dijo que si", no
     * "el dinero se movio". En el flujo de captura, el dinero no sale de su cuenta
     * hasta que llamamos a /capture, y PayPal anula la orden a los tres dias si no
     * lo hacemos. Tratar "aprobado" como "pagado" regalaria el espacio destacado a
     * cualquiera que aprobase y cerrase el navegador.
     */
    @Test
    @DisplayName("aprobado NO es pagado: CHECKOUT.ORDER.APPROVED no confirma nada")
    void aprobadoNoEsPagado() {
        String aprobado = """
                {"id":"WH-EVT-2","event_type":"CHECKOUT.ORDER.APPROVED",
                 "resource":{"id":"ORD-1","custom_id":"%s",
                 "amount":{"currency_code":"EUR","value":"25.00"}}}"""
                .formatted(PEDIDO);

        assertThat(conWebhook("WH-1").verificarWebhook(aprobado, cabecerasCompletas()))
                .isEmpty();
    }

    @Test
    @DisplayName("ignora eventos que no son de cobro")
    void eventoIrrelevante() {
        String otro = """
                {"id":"WH-EVT-3","event_type":"BILLING.SUBSCRIPTION.CREATED","resource":{"id":"X"}}""";

        assertThat(conWebhook("WH-1").verificarWebhook(otro, cabecerasCompletas())).isEmpty();
    }

    @Test
    @DisplayName("descarta un cuerpo que no es JSON en vez de reventar")
    void cuerpoIlegible() {
        assertThat(conWebhook("WH-1").verificarWebhook("no soy json", cabecerasCompletas()))
                .isEmpty();
    }

    @Test
    @DisplayName("sin secreto no se considera configurada y no se ofrece como forma de pago")
    void sinSecreto() {
        assertThat(new PasarelaPayPal(json, "cliente", "", "", "sandbox").estaConfigurada())
                .isFalse();
        assertThat(conWebhook("WH-1").estaConfigurada()).isTrue();
    }

    @Test
    @DisplayName("el codigo de pasarela es el que se guarda en los pedidos")
    void codigo() {
        assertThat(conWebhook("WH-1").codigo()).isEqualTo("PAYPAL");
    }

    /**
     * La referencia llega del navegador y se pega en la URL de la API de PayPal.
     * Estos valores tienen que quedarse en la puerta: si alguno pasara, la llamada
     * saldria hacia una ruta distinta de la que el codigo cree estar pidiendo.
     *
     * <p>Que estos tests no tarden ni hagan red es parte de lo que comprueban: si
     * la validacion fallara, intentarian salir a PayPal.
     */
    @Test
    @DisplayName("rechaza referencias que no tienen forma de orden de PayPal")
    void referenciaConFormaSospechosa() {
        PasarelaPayPal paypal = conWebhook("WH-1");

        for (String referencia : new String[] {
            "../../v1/oauth2/token",
            "5O190127TN364715T/../../otra",
            "orden%2F..%2Fx",
            "orden con espacios",
            "",
            "  ",
            "ab",
            "x".repeat(200)
        }) {
            assertThat(paypal.capturarAlVolver(referencia))
                    .as("referencia sospechosa: %s", referencia)
                    .isEmpty();
        }

        assertThat(paypal.capturarAlVolver(null)).isEmpty();
    }
}
