package com.latinosmc.voting;

import java.time.Instant;
import java.util.UUID;

/**
 * Un voto emitido.
 *
 * <p>Un voto no es un clic: es un hecho de negocio inmutable. Una vez creado, lo
 * unico que cambia es su estado (aceptado, rechazado, revocado) y eso siempre
 * queda registrado con su motivo.
 *
 * @param id identificador UUIDv7
 * @param serverId servidor al que se voto
 * @param estado situacion actual del voto
 * @param identidadValor identidad en claro; se necesita para entregar la
 *     recompensa al jugador correcto. Se borra al anonimizar (ADR-0010).
 * @param identidadHash hash estable; sobrevive a la anonimizacion y es lo que
 *     usan la ventana de voto y el antifraude
 * @param puntuacionRiesgo 0-100 segun el motor antifraude
 * @param creadoEn instante de creacion, en UTC
 */
public record Vote(
        UUID id,
        UUID serverId,
        VoteStatus estado,
        String identidadTipo,
        String identidadValor,
        byte[] identidadHash,
        Integer puntuacionRiesgo,
        String motivoRechazo,
        Instant creadoEn) {

    public boolean fueAceptado() {
        return estado == VoteStatus.ACCEPTED;
    }
}
