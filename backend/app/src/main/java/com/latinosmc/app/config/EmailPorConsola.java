package com.latinosmc.app.config;

import com.latinosmc.shared.ports.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Adaptador de correo para desarrollo: escribe el mensaje en la consola.
 *
 * <p>Permite probar el flujo de verificacion completo sin contratar ningun
 * proveedor. En produccion no se activa: el perfil {@code prod} exige un
 * adaptador real, y si no lo hay la aplicacion no arranca. Es deliberado —
 * arrancar en produccion enviando correos a la consola significaria que ningun
 * usuario recibe su codigo y nadie se entera hasta que se quejen.
 *
 * <p>La condicion es la <strong>contraria exacta</strong> de la de
 * {@link EmailPorSmtp}: o uno o el otro, nunca los dos ni ninguno. Antes esto se
 * resolvia con {@code @ConditionalOnMissingBean}, que en configuraciones propias
 * depende del orden en que Spring las procesa — y ese orden no esta garantizado.
 * El resultado fue que con SMTP configurado se registraban los dos y la
 * aplicacion no arrancaba.
 */
@Configuration
@Profile("!prod")
@ConditionalOnExpression("'${spring.mail.host:}'.trim() == ''")
public class EmailPorConsola {

    private static final Logger log = LoggerFactory.getLogger(EmailPorConsola.class);

    @org.springframework.context.annotation.Bean
    public EmailSender emailSender() {
        return (destinatario, asunto, cuerpoTexto, cuerpoHtml) -> log.info(
                """

                ┌─────────────────────────────────────────────────────
                │ CORREO (modo desarrollo, no se envia de verdad)
                │ Para:    {}
                │ Asunto:  {}
                ├─────────────────────────────────────────────────────
                {}
                └─────────────────────────────────────────────────────
                """,
                destinatario,
                asunto,
                cuerpoTexto);
    }
}
