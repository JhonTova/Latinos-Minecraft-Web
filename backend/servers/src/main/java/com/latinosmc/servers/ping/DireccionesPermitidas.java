package com.latinosmc.servers.ping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Decide si una direccion se puede sondear.
 *
 * <p><strong>Esto existe para impedir un SSRF.</strong> El host de un servidor lo
 * escribe el cliente en su panel. Sin esta comprobacion, cualquiera podria
 * registrar un "servidor" apuntando a {@code 127.0.0.1}, a {@code 169.254.169.254}
 * o a una IP interna de nuestra red, y usar nuestro sondeador para explorar la
 * infraestructura desde dentro — con nuestros permisos y desde nuestra IP.
 *
 * <p>El caso de {@code 169.254.169.254} es especialmente grave: en la mayoria de
 * proveedores cloud es el servicio de metadatos, y a menudo entrega credenciales
 * de la maquina a quien pregunte.
 *
 * <p>La resolucion se hace aqui y el resultado se sondea por IP, no por nombre.
 * Si se resolviera dos veces, un dominio podria responder una IP publica en la
 * comprobacion y una privada en la conexion real: es el ataque de "DNS rebinding".
 */
@Component
public class DireccionesPermitidas {

    private static final Logger log = LoggerFactory.getLogger(DireccionesPermitidas.class);

    /**
     * Resuelve el host y devuelve la IP si es sondeable.
     *
     * @return la direccion resuelta, o {@code null} si no se debe sondear
     */
    public InetAddress resolverSiEsSegura(String host) {
        try {
            InetAddress direccion = InetAddress.getByName(host);

            if (esInterna(direccion)) {
                log.warn("Sondeo bloqueado hacia direccion interna: {} -> {}", host, direccion.getHostAddress());
                return null;
            }
            return direccion;

        } catch (UnknownHostException e) {
            // Un dominio que no resuelve es un servidor caido, no un error del
            // sistema: se registra como "sin conexion" y ya esta.
            return null;
        }
    }

    private boolean esInterna(InetAddress direccion) {
        return direccion.isLoopbackAddress() // 127.x.x.x, ::1
                || direccion.isSiteLocalAddress() // 10.x, 172.16-31.x, 192.168.x
                || direccion.isLinkLocalAddress() // 169.254.x — incluye metadatos cloud
                || direccion.isAnyLocalAddress() // 0.0.0.0
                || direccion.isMulticastAddress()
                || esRangoReservado(direccion);
    }

    /**
     * Rangos que Java no clasifica pero tampoco deben sondearse.
     *
     * <p>{@code 100.64.0.0/10} es el espacio compartido de los operadores (CGNAT):
     * dentro de un centro de datos puede llevar a maquinas de otros clientes.
     */
    private boolean esRangoReservado(InetAddress direccion) {
        byte[] bytes = direccion.getAddress();
        if (bytes.length != 4) {
            return false;
        }
        int primero = bytes[0] & 0xFF;
        int segundo = bytes[1] & 0xFF;

        // 100.64.0.0/10 — CGNAT
        if (primero == 100 && segundo >= 64 && segundo <= 127) {
            return true;
        }
        // 192.0.0.0/24 y 192.0.2.0/24 — asignaciones especiales y documentacion
        if (primero == 192 && segundo == 0) {
            return true;
        }
        // 198.18.0.0/15 — pruebas de rendimiento entre redes
        return primero == 198 && (segundo == 18 || segundo == 19);
    }
}
