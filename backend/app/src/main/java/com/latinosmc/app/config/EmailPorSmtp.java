package com.latinosmc.app.config;

import com.latinosmc.shared.ports.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Envio de correo por SMTP.
 *
 * <p>SMTP y no la API propia de ningun proveedor, a proposito: funciona igual con
 * Resend, Brevo, Amazon SES, Mailgun o Zoho. Cambiar de proveedor es cambiar tres
 * lineas del {@code .env}, no tocar codigo. Y cambiar de proveedor de correo pasa
 * — por precio, por reputacion de entrega o porque te suspenden la cuenta.
 *
 * <p>Se activa solo si hay servidor SMTP configurado. Sin el, sigue el adaptador
 * de consola en desarrollo, y en produccion la aplicacion no arranca.
 *
 * <p>La condicion compara con cadena vacia en lugar de usar
 * {@code @ConditionalOnProperty}. Tiene motivo: la propiedad siempre existe
 * —{@code ${SMTP_HOST:}} la deja vacia, no ausente— y
 * {@code @ConditionalOnProperty} considera presente cualquier valor que no sea
 * "false", vacio incluido. Con esa anotacion, este adaptador se activaba sin
 * servidor SMTP y chocaba con {@link EmailPorConsola}.
 */
@Configuration
@ConditionalOnExpression("'${spring.mail.host:}'.trim() != ''")
public class EmailPorSmtp {

    private static final Logger log = LoggerFactory.getLogger(EmailPorSmtp.class);

    @Bean
    public EmailSender emailSender(
            JavaMailSender mailSender,
            @Value("${latinosmc.correo.remitente:no-responder@latinosmc.com}") String remitente,
            @Value("${latinosmc.correo.nombre-remitente:LatinosMC}") String nombreRemitente) {

        return (destinatario, asunto, cuerpoTexto, cuerpoHtml) -> {
            try {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper =
                        new MimeMessageHelper(mensaje, cuerpoHtml != null, "UTF-8");

                helper.setFrom(remitente, nombreRemitente);
                helper.setTo(destinatario);
                helper.setSubject(asunto);

                if (cuerpoHtml != null) {
                    // Ambas versiones: hay clientes que no muestran HTML, y un
                    // correo solo-HTML puntua peor en los filtros de spam.
                    helper.setText(cuerpoTexto, cuerpoHtml);
                } else {
                    helper.setText(cuerpoTexto, false);
                }

                mailSender.send(mensaje);
                log.debug("Correo enviado a {}", destinatario);

            } catch (Exception e) {
                // No se relanza: que falle un correo no debe tumbar el registro de
                // un usuario. Se registra alto para que se vea en los logs, y el
                // usuario siempre puede pedir el codigo otra vez.
                log.error("No se pudo enviar el correo a {}: {}", destinatario, e.getMessage());
            }
        };
    }
}
