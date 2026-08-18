package com.latinosmc.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.events.DomainEvent;
import com.latinosmc.shared.events.EventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Implementa {@link EventPublisher} escribiendo en la tabla Outbox (ADR-0003).
 *
 * <p>No hace E/S de red: solo un INSERT que participa en la transaccion en curso.
 * Esa es toda la idea del patron. Si esta clase intentara entregar el evento aqui
 * mismo, existiria una ventana en la que el voto queda guardado pero el evento se
 * pierde —o al reves— y el resultado seria una recompensa que nunca llega.
 *
 * <p>La entrega real la hace {@link OutboxWorker}, despues, y con reintentos.
 */
@Component
public class OutboxPublisher implements EventPublisher {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public OutboxPublisher(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void publish(DomainEvent evento) {
        String payload;
        try {
            payload = json.writeValueAsString(evento);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Un evento que no se puede serializar es un error de programacion, no
            // una condicion de ejecucion. Debe romper la transaccion entera: es
            // preferible rechazar el voto que aceptarlo sin poder entregarlo.
            throw new IllegalStateException("Evento no serializable: " + evento.eventType(), e);
        }

        jdbc.sql(
                        """
                        INSERT INTO outbox (id, aggregate_type, aggregate_id, event_type, event_version,
                                            payload, status_code)
                        VALUES (:id, :tipoAgregado, :idAgregado, :tipoEvento, :version,
                                CAST(:payload AS jsonb), 'PENDING')
                        """)
                .param("id", Ids.newId())
                .param("tipoAgregado", evento.aggregateType())
                .param("idAgregado", evento.aggregateId())
                .param("tipoEvento", evento.eventType())
                .param("version", evento.eventVersion())
                .param("payload", payload)
                .update();
    }
}
