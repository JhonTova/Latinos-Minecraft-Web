package com.latinosmc.voting;

/** Estados de un voto. Los codigos coinciden con la tabla {@code vote_statuses}. */
public enum VoteStatus {
    /** Creado, aun sin veredicto del antifraude. */
    PENDING,

    /** Cuenta para el ranking y genera recompensa. */
    ACCEPTED,

    /** El antifraude lo rechazo. No cuenta y NO consume la ventana de voto. */
    REJECTED,

    /** Se acepto y luego se revoco tras revision manual o deteccion posterior. */
    REVOKED
}
