package com.latinosmc.shared.ports;

import java.util.Optional;
import java.util.UUID;

/**
 * Pasarela de pago. Puerto, no implementacion.
 *
 * <p>Intercambiable desde el primer dia (ADR-0004). El mercado hispanohablante
 * necesita medios locales —Mercado Pago, PSE— ademas de los internacionales, y
 * casarse con un solo proveedor sale caro de deshacer.
 *
 * <p><strong>Aqui nunca pasa un dato de tarjeta.</strong> La pasarela aloja su
 * propio formulario y nosotros solo recibimos una referencia. Es lo que mantiene
 * el alcance PCI-DSS en el minimo (SAQ-A) y elimina de raiz la clase de
 * incidente mas cara que existe.
 */
public interface PaymentGateway {

    /** Codigo del proveedor, tal como se guarda en la tabla de pedidos. */
    String codigo();

    /** Si tiene credenciales. Sin ellas, no se ofrece como forma de pago. */
    boolean estaConfigurada();

    /**
     * Crea una sesion de pago y devuelve a donde enviar al usuario.
     *
     * @param pedidoId nuestro identificador; viaja de vuelta en el webhook
     * @param importeCentimos siempre en centimos enteros, jamas decimales
     * @param urlExito a donde vuelve el usuario tras pagar
     */
    SesionDePago crearSesion(
            UUID pedidoId,
            long importeCentimos,
            String moneda,
            String descripcion,
            String urlExito,
            String urlCancelacion);

    /**
     * Verifica la firma de un webhook y devuelve el evento si es autentico.
     *
     * <p><strong>Es la comprobacion mas importante de todo el modulo.</strong> Sin
     * ella, cualquiera que descubra la URL puede enviar un "pago completado"
     * falso y llevarse el producto sin pagar. La firma es lo unico que prueba que
     * el mensaje viene de la pasarela y no de un tercero.
     *
     * @param cuerpoCrudo el cuerpo EXACTO recibido, byte a byte. Si se
     *     deserializa y se vuelve a serializar antes de verificar, la firma deja
     *     de coincidir.
     * @param cabecera funcion para leer cualquier cabecera de la peticion. Se pasa
     *     asi, y no una cabecera concreta, porque cada proveedor usa las suyas:
     *     Stripe firma en una sola, PayPal reparte la informacion en cinco. Con
     *     esto el controlador no necesita conocer los nombres de ninguna.
     * @return el evento si la firma es valida; vacio si no lo es
     */
    Optional<EventoDePago> verificarWebhook(String cuerpoCrudo, java.util.function.Function<String, String> cabecera);

    /**
     * Confirma un pago cuando el usuario vuelve del proveedor.
     *
     * <p>Existe porque no todas las pasarelas funcionan igual: Stripe cobra al
     * completarse el pago y solo avisa por webhook, mientras que PayPal aprueba
     * primero y exige una llamada explicita de captura para mover el dinero.
     *
     * <p>Por defecto no hace nada, que es lo correcto para las pasarelas que
     * cobran solas. Las que necesitan capturar lo sobrescriben.
     *
     * @param referenciaProveedor identificador que el proveedor devuelve en la URL
     *     de retorno
     * @return el evento de cobro si se completo aqui; vacio si no aplica
     */
    default Optional<EventoDePago> capturarAlVolver(String referenciaProveedor) {
        return Optional.empty();
    }

    /**
     * @param urlPago a donde redirigir al usuario
     * @param referencia identificador del pago en la pasarela
     */
    record SesionDePago(String urlPago, String referencia) {}

    /**
     * @param eventoId identificador del evento en la pasarela; sirve de clave de
     *     idempotencia porque las pasarelas reenvian si no reciben un 200
     * @param pedidoId nuestro identificador, recuperado de los metadatos
     */
    record EventoDePago(
            String eventoId, String tipo, UUID pedidoId, String referencia, long importeCentimos, String moneda) {}
}
