package com.latinosmc.shared;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/**
 * Generacion de identificadores del sistema.
 *
 * <p>Se usa UUIDv7 (ADR-0002). A diferencia de UUIDv4, que es aleatorio puro y
 * fragmenta los indices B-tree degradando la insercion, UUIDv7 lleva el instante
 * de creacion en los bits altos: las filas nuevas caen juntas en el indice, como
 * un secuencial, pero sin exponer un contador adivinable hacia fuera.
 */
public final class Ids {

    private Ids() {}

    /** Genera un identificador nuevo, ordenado temporalmente. */
    public static UUID newId() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
