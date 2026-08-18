package com.latinosmc.voting;

import java.time.Duration;

/**
 * Reglas de voto de toda la plataforma.
 *
 * <p>Viven en base de datos y no en constantes del codigo porque son decisiones
 * de producto, no de ingenieria: revisarlas no deberia exigir un despliegue.
 *
 * @param unVotoAlDia si un jugador solo puede votar a UN servidor al dia en toda
 *     la plataforma. Con {@code false} vuelve la regla anterior: uno por
 *     servidor y dia (ADR-0011).
 * @param maxJugadoresPorIp cuantos jugadores distintos pueden votar desde la
 *     misma IP dentro de la ventana. <strong>Ojo con bajarlo:</strong> los
 *     operadores moviles de Latinoamerica usan CGNAT y meten miles de clientes
 *     detras de una IPv4; una casa con hermanos o un cibercafe comparten una
 *     sola. Un valor de 1 deja fuera a todos menos al primero, sin que ellos
 *     entiendan por que.
 * @param ventana duracion de la ventana global
 */
public record VoteSettings(boolean unVotoAlDia, int maxJugadoresPorIp, Duration ventana) {

    /**
     * Valores de emergencia, solo si la tabla no se puede leer.
     *
     * <p>Son los <strong>permisivos</strong> a proposito: si la configuracion no
     * carga, es mejor aceptar votos que podrian ser dudosos que bloquear a todo
     * el mundo. Un fallo de lectura de configuracion no deberia poder tumbar la
     * funcion principal del producto.
     */
    public static VoteSettings porDefecto() {
        return new VoteSettings(false, 50, Duration.ofHours(24));
    }
}
