package com.latinosmc.shared.events;

import java.util.UUID;

/**
 * Evento de dominio publicable hacia fuera del modulo que lo genera.
 *
 * <p>Los eventos son el unico canal de comunicacion entre modulos junto con los
 * puertos publicos de {@code shared} (ADR-0001). Un modulo nunca llama a otro
 * directamente.
 *
 * <p>El contrato se define en AsyncAPI (ADR-0006) y se versiona: un consumidor
 * antiguo debe poder ignorar lo que no entiende sin romperse. Por eso los
 * eventos solo anaden campos, nunca los quitan ni cambian su significado.
 */
public interface DomainEvent {

    /** Tipo de agregado que lo genero. Ejemplo: {@code "vote"}. */
    String aggregateType();

    /** Identificador del agregado concreto. */
    UUID aggregateId();

    /** Nombre del evento tal y como aparece en el contrato. Ejemplo: {@code "vote.accepted"}. */
    String eventType();

    /** Version del contrato del evento. Empieza en 1 y solo sube. */
    default int eventVersion() {
        return 1;
    }
}
