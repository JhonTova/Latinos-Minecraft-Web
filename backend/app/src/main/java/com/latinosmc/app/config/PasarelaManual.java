package com.latinosmc.app.config;

import com.latinosmc.shared.ports.PaymentGateway;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pasarela de cobro manual: transferencia, Bizum o acuerdo directo.
 *
 * <p>Existe para que el flujo completo funcione desde el primer dia sin contratar
 * nada. El pedido se crea, queda pendiente y el administrador lo confirma a mano
 * desde el panel. Es exactamente como se cobran los primeros clientes de casi
 * cualquier plataforma pequeña.
 *
 * <p>Se desactiva sola en cuanto se configure una pasarela real. La condicion
 * nombra las credenciales de cada pasarela en lugar de usar
 * {@code @ConditionalOnMissingBean}: esa anotacion, en configuraciones propias,
 * depende del orden en que Spring procesa las clases, y ese orden no esta
 * garantizado. Cuando se añada otra pasarela hay que añadirla tambien aqui —
 * es el precio de que el resultado sea el mismo en cada arranque.
 *
 * <p><strong>Para enchufar Mercado Pago o cualquier otra</strong> basta con crear
 * una clase que implemente {@link PaymentGateway} y declararla como bean. Nada
 * mas del sistema cambia: ni el servicio de compra, ni el ledger, ni el
 * controlador de webhooks.
 */
@Configuration
public class PasarelaManual {

    @Bean
    @ConditionalOnExpression(
            "'${latinosmc.paypal.client-id:}'.trim() == '' and '${latinosmc.stripe.secret-key:}'.trim() == ''")
    public PaymentGateway cobroManual(@Value("${latinosmc.sitio-url}") String sitioUrl) {
        String base = sitioUrl.endsWith("/") ? sitioUrl.substring(0, sitioUrl.length() - 1) : sitioUrl;

        return new PaymentGateway() {
            @Override
            public String codigo() {
                return "MANUAL";
            }

            @Override
            public boolean estaConfigurada() {
                return true;
            }

            @Override
            public SesionDePago crearSesion(
                    UUID pedidoId,
                    long importeCentimos,
                    String moneda,
                    String descripcion,
                    String urlExito,
                    String urlCancelacion) {

                // No hay redireccion a ninguna pasarela: se envia al usuario a una
                // pagina propia con las instrucciones de pago.
                return new SesionDePago(base + "/pedido/" + pedidoId, "manual-" + pedidoId);
            }

            @Override
            public Optional<EventoDePago> verificarWebhook(
                    String cuerpoCrudo, java.util.function.Function<String, String> cabecera) {
                // Sin pasarela no hay webhooks que verificar. Devolver vacio y no
                // lanzar es deliberado: si alguien descubre la URL y envia algo,
                // se descarta en silencio en vez de dar pistas con un error.
                return Optional.empty();
            }
        };
    }
}
