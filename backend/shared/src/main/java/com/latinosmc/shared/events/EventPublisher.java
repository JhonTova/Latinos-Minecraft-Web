package com.latinosmc.shared.events;

/**
 * Puerto de publicacion de eventos. Lo implementa el modulo {@code delivery}
 * escribiendo en la tabla Outbox (ADR-0003).
 *
 * <p><strong>Contrato critico:</strong> la implementacion DEBE escribir en la
 * misma transaccion que la operacion de dominio que genero el evento. Ese es el
 * motivo entero de existir del patron Outbox.
 *
 * <p>Si el evento se publicara fuera de la transaccion, existiria una ventana en
 * la que el voto queda guardado pero el evento se pierde (o al reves). En este
 * producto eso significa un voto aceptado que nunca llega al servidor: para el
 * jugador, una recompensa que no aparece; para el dueno, un motivo para
 * marcharse.
 */
public interface EventPublisher {

    /**
     * Encola el evento para publicacion. No hace E/S de red: solo escribe en la
     * base de datos dentro de la transaccion en curso.
     */
    void publish(DomainEvent event);
}
