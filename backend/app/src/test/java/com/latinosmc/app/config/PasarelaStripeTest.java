package com.latinosmc.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de la verificacion de firma de Stripe.
 *
 * <p>Es la comprobacion mas critica de todo el modulo de pagos: sin ella,
 * cualquiera que descubra la URL del webhook puede enviar un "pago completado"
 * falso y llevarse el producto sin pagar un euro.
 */
class PasarelaStripeTest {

    private static final String SECRETO = "whsec_secreto_de_pruebas_1234567890";
    private static final UUID PEDIDO = UUID.fromString("019fd425-f9bb-7d4e-a7c4-7dd90b8ed5cf");

    private final ObjectMapper json = new ObjectMapper();
    private final PasarelaStripe stripe = new PasarelaStripe(json, "sk_test_falsa", SECRETO);

    /**
     * Llama a la verificacion simulando la peticion HTTP: solo existe la cabecera
     * que Stripe usa, y cualquier otra devuelve null como haria un servlet real.
     */
    private java.util.Optional<com.latinosmc.shared.ports.PaymentGateway.EventoDePago> verificar(
            PasarelaStripe pasarela, String cuerpo, String firma) {
        return pasarela.verificarWebhook(cuerpo, nombre -> "Stripe-Signature".equals(nombre) ? firma : null);
    }

    private String cuerpoDeEjemplo() {
        return """
                {"id":"evt_123","type":"checkout.session.completed",
                 "data":{"object":{"id":"cs_123","client_reference_id":"%s",
                 "payment_intent":"pi_123","amount_total":2500,"currency":"eur"}}}"""
                .formatted(PEDIDO);
    }

    @Test
    @DisplayName("acepta un webhook con firma valida")
    void firmaValida() {
        String cuerpo = cuerpoDeEjemplo();
        String cabecera = firmar(cuerpo, Instant.now());

        var evento = verificar(stripe, cuerpo, cabecera);

        assertThat(evento).isPresent();
        assertThat(evento.get().pedidoId()).isEqualTo(PEDIDO);
        assertThat(evento.get().tipo()).isEqualTo("payment.succeeded");
        assertThat(evento.get().importeCentimos()).isEqualTo(2500);
    }

    /**
     * El ataque directo: alguien descubre la URL y envia un pago inventado.
     */
    @Test
    @DisplayName("rechaza un webhook sin firma")
    void sinFirma() {
        assertThat(verificar(stripe, cuerpoDeEjemplo(), null)).isEmpty();
    }

    @Test
    @DisplayName("rechaza una firma inventada")
    void firmaInventada() {
        String cabecera = "t=" + Instant.now().getEpochSecond() + ",v1=firmainventada";
        assertThat(verificar(stripe, cuerpoDeEjemplo(), cabecera)).isEmpty();
    }

    /**
     * Firma valida pero para OTRO contenido. Si se aceptara, bastaria con capturar
     * un webhook real de 1 € y cambiarle el importe a 1000 €.
     */
    @Test
    @DisplayName("rechaza una firma valida para otro cuerpo")
    void cuerpoManipulado() {
        String original = cuerpoDeEjemplo();
        String cabecera = firmar(original, Instant.now());

        String manipulado = original.replace("2500", "1");

        assertThat(verificar(stripe, manipulado, cabecera)).isEmpty();
    }

    /**
     * Proteccion contra repeticion. Sin la ventana de tiempo, un webhook valido
     * capturado hoy seguiria funcionando dentro de un año — y bastaria reenviarlo
     * para conseguir producto gratis una y otra vez.
     */
    @Test
    @DisplayName("rechaza un webhook antiguo aunque la firma sea correcta")
    void webhookCaducado() {
        String cuerpo = cuerpoDeEjemplo();
        String cabecera = firmar(cuerpo, Instant.now().minusSeconds(3600));

        assertThat(verificar(stripe, cuerpo, cabecera)).isEmpty();
    }

    @Test
    @DisplayName("rechaza un webhook con marca de tiempo del futuro")
    void webhookDelFuturo() {
        String cuerpo = cuerpoDeEjemplo();
        String cabecera = firmar(cuerpo, Instant.now().plusSeconds(3600));

        assertThat(verificar(stripe, cuerpo, cabecera)).isEmpty();
    }

    @Test
    @DisplayName("ignora eventos de Stripe que no son nuestros")
    void eventoSinReferencia() {
        String cuerpo =
                """
                {"id":"evt_999","type":"checkout.session.completed",
                 "data":{"object":{"id":"cs_999","amount_total":100,"currency":"eur"}}}""";
        String cabecera = firmar(cuerpo, Instant.now());

        assertThat(verificar(stripe, cuerpo, cabecera)).isEmpty();
    }

    @Test
    @DisplayName("ignora tipos de evento que no nos interesan")
    void tipoIrrelevante() {
        String cuerpo =
                """
                {"id":"evt_888","type":"customer.created",
                 "data":{"object":{"client_reference_id":"%s"}}}"""
                        .formatted(PEDIDO);
        String cabecera = firmar(cuerpo, Instant.now());

        assertThat(verificar(stripe, cuerpo, cabecera)).isEmpty();
    }

    @Test
    @DisplayName("reconoce un reembolso")
    void reembolso() {
        String cuerpo =
                """
                {"id":"evt_777","type":"charge.refunded",
                 "data":{"object":{"id":"ch_777","client_reference_id":"%s",
                 "amount":2500,"currency":"eur"}}}"""
                        .formatted(PEDIDO);
        String cabecera = firmar(cuerpo, Instant.now());

        var evento = verificar(stripe, cuerpo, cabecera);

        assertThat(evento).isPresent();
        assertThat(evento.get().tipo()).isEqualTo("payment.refunded");
    }

    @Test
    @DisplayName("sin secreto de webhook no se acepta nada")
    void sinSecretoConfigurado() {
        var sinSecreto = new PasarelaStripe(json, "sk_test_falsa", "");
        String cuerpo = cuerpoDeEjemplo();

        assertThat(verificar(sinSecreto, cuerpo, firmar(cuerpo, Instant.now())))
                .isEmpty();
    }

    /** Construye la cabecera tal y como la envia Stripe. */
    private String firmar(String cuerpo, Instant momento) {
        long t = momento.getEpochSecond();
        String firmado = t + "." + cuerpo;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String firma = HexFormat.of().formatHex(mac.doFinal(firmado.getBytes(StandardCharsets.UTF_8)));
            return "t=" + t + ",v1=" + firma;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
