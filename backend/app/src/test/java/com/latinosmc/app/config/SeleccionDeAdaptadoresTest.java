package com.latinosmc.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.shared.ports.EmailSender;
import com.latinosmc.shared.ports.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Que adaptador se activa segun lo que haya en el {@code .env}.
 *
 * <p>Esto existe porque falló en producción. Las condiciones estaban escritas con
 * {@code @ConditionalOnProperty} y {@code @ConditionalOnMissingBean}, y las dos
 * mienten de una forma que no se ve leyendo el código:
 *
 * <ul>
 *   <li>{@code @ConditionalOnProperty} da por presente cualquier valor que no sea
 *       "false" — y la cadena vacía lo es. Como el {@code application.yml} define
 *       {@code ${SMTP_HOST:}}, la propiedad SIEMPRE existe, vacía o no, así que
 *       la condición siempre se cumplía.
 *   <li>{@code @ConditionalOnMissingBean} en configuraciones propias depende del
 *       orden en que Spring las procesa, y ese orden no está garantizado.
 * </ul>
 *
 * <p>Resultado: dos beans {@code emailSender} y la aplicación negándose a
 * arrancar. Un fallo que no aparece compilando ni ejecutando los tests de
 * unidad, solo levantando el contexto — que es justo lo que hace este test.
 */
class SeleccionDeAdaptadoresTest {

    private ApplicationContextRunner contexto() {
        return new ApplicationContextRunner()
                .withBean(ObjectMapper.class)
                .withPropertyValues("latinosmc.sitio-url=http://localhost:3000")
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withUserConfiguration(
                        EmailPorConsola.class,
                        EmailPorSmtp.class,
                        PasarelaManual.class,
                        PasarelaPayPal.class,
                        PasarelaStripe.class);
    }

    // --- Correo --------------------------------------------------------------

    @Test
    @DisplayName("sin SMTP los codigos salen por consola")
    void correoSinSmtp() {
        contexto().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(EmailSender.class);
        });
    }

    /**
     * El caso exacto que tiró la aplicación: la variable existe pero está vacía,
     * que es como queda un {@code .env} recién copiado del ejemplo.
     */
    @Test
    @DisplayName("SMTP_HOST vacio NO cuenta como configurado")
    void correoConSmtpVacio() {
        contexto().withPropertyValues("spring.mail.host=").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(EmailSender.class);
        });
    }

    @Test
    @DisplayName("SMTP_HOST con solo espacios tampoco cuenta")
    void correoConSmtpEnBlanco() {
        contexto().withPropertyValues("spring.mail.host=   ").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(EmailSender.class);
        });
    }

    @Test
    @DisplayName("con SMTP configurado se envia de verdad y NO se duplica el bean")
    void correoConSmtp() {
        contexto()
                .withPropertyValues("spring.mail.host=smtp-relay.brevo.com", "spring.mail.port=587")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    // Lo que reventaba: dos definiciones con el mismo nombre.
                    assertThat(ctx).hasSingleBean(EmailSender.class);
                    assertThat(ctx.getBeanNamesForType(EmailSender.class)).hasSize(1);
                });
    }

    // --- Pasarelas de pago ---------------------------------------------------

    @Test
    @DisplayName("sin credenciales se cobra a mano")
    void pagoSinCredenciales() {
        contexto().run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(PaymentGateway.class);
            assertThat(ctx.getBean(PaymentGateway.class).codigo()).isEqualTo("MANUAL");
        });
    }

    @Test
    @DisplayName("credenciales vacias no activan ninguna pasarela")
    void pagoConCredencialesVacias() {
        contexto()
                .withPropertyValues("latinosmc.paypal.client-id=", "latinosmc.stripe.secret-key=")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(PaymentGateway.class).codigo()).isEqualTo("MANUAL");
                });
    }

    @Test
    @DisplayName("con PayPal configurado se cobra por PayPal")
    void pagoConPayPal() {
        contexto()
                .withPropertyValues("latinosmc.paypal.client-id=cliente", "latinosmc.paypal.secret=secreto")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(PaymentGateway.class).codigo()).isEqualTo("PAYPAL");
                });
    }

    @Test
    @DisplayName("con solo Stripe configurado se cobra por Stripe")
    void pagoConStripe() {
        contexto().withPropertyValues("latinosmc.stripe.secret-key=sk_test_x").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PaymentGateway.class).codigo()).isEqualTo("STRIPE");
        });
    }

    /**
     * Con las dos configuradas hay dos beans del mismo tipo. Sin {@code @Primary}
     * eso es una excepción de ambigüedad al arrancar — en producción, y sin
     * ninguna pista de por qué.
     */
    @Test
    @DisplayName("con las dos configuradas gana PayPal y la aplicacion arranca")
    void pagoConLasDos() {
        contexto()
                .withPropertyValues(
                        "latinosmc.paypal.client-id=cliente",
                        "latinosmc.paypal.secret=secreto",
                        "latinosmc.stripe.secret-key=sk_test_x")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(PaymentGateway.class).codigo()).isEqualTo("PAYPAL");
                    // La manual se aparta cuando hay cualquiera de las dos.
                    assertThat(ctx.getBeansOfType(PaymentGateway.class)).hasSize(2);
                });
    }
}
