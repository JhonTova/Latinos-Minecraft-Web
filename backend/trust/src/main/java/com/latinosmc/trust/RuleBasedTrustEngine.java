package com.latinosmc.trust;

import com.latinosmc.shared.identity.IdentityHasher;
import com.latinosmc.shared.ports.TrustEngine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Motor antifraude basado en reglas.
 *
 * <p>Reglas y no machine learning, deliberadamente (ADR-0005). Un modelo necesita
 * votos etiquetados como fraudulentos que todavia no existen; y cuando se
 * suspende a un servidor de pago hay que poder explicar por que. "El modelo lo
 * decidio" no es una respuesta defendible. Las reglas, ademas, van generando el
 * conjunto de datos etiquetado que un modelo necesitaria mas adelante.
 *
 * <p>Las senales de reputacion de IP (VPN, proxy, centro de datos) NO estan aqui:
 * son servicios de pago por consulta y los proxies residenciales las derrotan.
 * Son senal de apoyo, no defensa principal. Lo que si funciona es el
 * comportamiento: patrones temporales y relaciones entre identidades.
 */
@Component
public class RuleBasedTrustEngine implements TrustEngine {

    /** A partir de esta puntuacion el voto se rechaza. */
    private static final int UMBRAL_RECHAZO = 60;

    private static final Duration VENTANA_LARGA = Duration.ofHours(24);
    private static final Duration VENTANA_RAFAGA = Duration.ofHours(1);

    private static final int MAX_IDENTIDADES_POR_IP = 3;
    private static final int MAX_VOTOS_RAFAGA = 6;

    private final TrustQueries consultas;
    private final IdentityHasher hasher;

    public RuleBasedTrustEngine(TrustQueries consultas, IdentityHasher hasher) {
        this.consultas = consultas;
        this.hasher = hasher;
    }

    @Override
    public Veredicto evaluar(Peticion peticion) {
        List<Senal> senales = new ArrayList<>();
        int riesgo = 0;

        byte[] ipHash = hasher.hashIp(peticion.ip());

        // --- Identidad no verificable -------------------------------------------
        // En offline-mode el nombre no lo respalda nadie: escribir otro nombre es
        // gratis. No se rechaza por si solo (excluiria a medio mercado
        // hispanohablante), pero parte con desventaja.
        if (!peticion.identidad().type().esVerificable()) {
            riesgo += 25;
            senales.add(new Senal("IDENTITY_UNVERIFIABLE", 25, "Identidad de servidor offline-mode"));
        }

        // --- Multiples identidades desde una misma IP ---------------------------
        int identidades = consultas.identidadesDistintasPorIp(ipHash, peticion.momento().minus(VENTANA_LARGA));
        if (identidades > MAX_IDENTIDADES_POR_IP) {
            int delta = Math.min(45, (identidades - MAX_IDENTIDADES_POR_IP) * 15);
            riesgo += delta;
            senales.add(new Senal("IP_MULTI_IDENTITY", delta, identidades + " identidades distintas en 24 h"));
        }

        // --- Rafaga de votos desde una misma IP ---------------------------------
        int votosRecientes = consultas.votosPorIp(ipHash, peticion.momento().minus(VENTANA_RAFAGA));
        if (votosRecientes > MAX_VOTOS_RAFAGA) {
            int delta = Math.min(40, (votosRecientes - MAX_VOTOS_RAFAGA) * 10);
            riesgo += delta;
            senales.add(new Senal("IP_BURST", delta, votosRecientes + " votos en 1 h"));
        }

        // --- Historial del votante ----------------------------------------------
        // Un votante con historial es caro de fabricar: no se rechaza a los nuevos,
        // pero un historial limpio compensa otras senales. Esto evita que un
        // jugador legitimo que comparte IP con su hermano quede bloqueado.
        int historial = consultas.historialIdentidad(peticion.identidadHash());
        if (historial == 0) {
            riesgo += 10;
            senales.add(new Senal("IDENTITY_NEW", 10, "Primer voto de esta identidad"));
        } else if (historial >= 10) {
            riesgo -= 15;
            senales.add(new Senal("IDENTITY_TRUSTED", -15, historial + " votos aceptados previos"));
        }

        riesgo = Math.clamp(riesgo, 0, 100);

        if (riesgo >= UMBRAL_RECHAZO) {
            return Veredicto.rechazar(riesgo, "fraud.risk_too_high", senales);
        }
        return Veredicto.aceptar(riesgo, senales);
    }
}
