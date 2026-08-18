package com.latinosmc.voting;

import com.latinosmc.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Se acepto un voto. Es el evento central del producto: dispara la entrega de la
 * recompensa y la actualizacion del ranking.
 *
 * <p>Se publica dentro de la misma transaccion que guarda el voto (ADR-0003). Un
 * voto guardado cuyo evento se perdiera seria una recompensa que nunca llega.
 */
public record VoteAcceptedEvent(
        UUID voteId,
        UUID serverId,
        String identidadTipo,
        String identidadValor,
        Instant ocurridoEn)
        implements DomainEvent {

    @Override
    public String aggregateType() {
        return "vote";
    }

    @Override
    public UUID aggregateId() {
        return voteId;
    }

    @Override
    public String eventType() {
        return "vote.accepted";
    }
}
