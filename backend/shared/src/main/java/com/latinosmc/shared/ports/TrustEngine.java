package com.latinosmc.shared.ports;

import com.latinosmc.shared.identity.PlayerIdentity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Veredicto antifraude sobre un intento de voto.
 *
 * <p>El antifraude es dominio core desde la fase 1 (ADR-0005), no un filtro
 * anadido despues: el producto que se vende es un ranking, y un ranking que se
 * puede comprar no vale nada.
 *
 * <p>El motor devuelve una decision <em>explicable</em>: cada senal que sumo
 * riesgo queda registrada. Cuando se suspenda a un servidor de pago habra que
 * justificarlo, y "el sistema lo decidio" no es una respuesta aceptable.
 */
public interface TrustEngine {

    Veredicto evaluar(Peticion peticion);

    /**
     * @param servidorId servidor al que se vota
     * @param identidad identidad del votante
     * @param identidadHash hash estable de la identidad
     * @param ip direccion IP de origen, sin hashear (el motor decide que hacer con ella)
     * @param momento instante del intento
     */
    record Peticion(UUID servidorId, PlayerIdentity identidad, byte[] identidadHash, String ip, Instant momento) {}

    /**
     * @param aceptado si el voto puede contar para el ranking
     * @param puntuacionRiesgo 0 (limpio) a 100 (fraude claro)
     * @param motivoRechazo codigo estable si se rechaza, null si se acepta
     * @param senales por que se puntuo asi; se persiste para poder explicarlo
     */
    record Veredicto(boolean aceptado, int puntuacionRiesgo, String motivoRechazo, List<Senal> senales) {

        public static Veredicto aceptar(int puntuacion, List<Senal> senales) {
            return new Veredicto(true, puntuacion, null, senales);
        }

        public static Veredicto rechazar(int puntuacion, String motivo, List<Senal> senales) {
            return new Veredicto(false, puntuacion, motivo, senales);
        }
    }

    /**
     * @param regla codigo de la regla que se activo
     * @param delta cuanto riesgo sumo (o resto, si es negativo)
     * @param detalle contexto legible para investigacion
     */
    record Senal(String regla, int delta, String detalle) {}
}
