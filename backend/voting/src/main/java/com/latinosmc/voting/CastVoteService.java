package com.latinosmc.voting;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.events.EventPublisher;
import com.latinosmc.shared.identity.IdentityHasher;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.shared.ports.ServerDirectory;
import com.latinosmc.shared.ports.TrustEngine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emitir un voto. Es el caso de uso central del producto.
 *
 * <p>Todo ocurre en una unica transaccion, incluida la publicacion del evento en
 * el Outbox (ADR-0003). Si algo falla, no queda ni un voto sin evento ni un
 * evento sin voto.
 */
@Service
public class CastVoteService {

    private static final Logger log = LoggerFactory.getLogger(CastVoteService.class);

    /** Umbral a partir del cual el motor antifraude rechaza. */
    private static final String RECOMPENSA_POR_DEFECTO = "DEFAULT";

    private final VoteRepository repositorio;
    private final VoteSettingsRepository ajustes;
    private final ServerDirectory directorio;
    private final TrustEngine antifraude;
    private final IdentityHasher hasher;
    private final EventPublisher publicador;
    private final Clock reloj;

    public CastVoteService(
            VoteRepository repositorio,
            VoteSettingsRepository ajustes,
            ServerDirectory directorio,
            TrustEngine antifraude,
            IdentityHasher hasher,
            EventPublisher publicador,
            Clock reloj) {
        this.repositorio = repositorio;
        this.ajustes = ajustes;
        this.directorio = directorio;
        this.antifraude = antifraude;
        this.hasher = hasher;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    /**
     * Si este jugador puede votar ahora mismo, y por que no si no puede.
     *
     * <p>Existe para que el plugin pregunte ANTES de generar un codigo. Sin esto,
     * quien ya voto recibe igualmente su enlace, lo abre, teclea el codigo y solo
     * entonces descubre que no valia: cuatro pasos para llegar a un no que se
     * sabia desde el principio.
     *
     * <p>No consume nada ni deja rastro: es una consulta, y su respuesta no
     * autoriza nada. La comprobacion que manda sigue estando dentro de
     * {@link #votar}, que es donde ocurre la escritura. Si esta se equivocara por
     * exceso, el jugador recibiria su codigo y el voto se rechazaria despues;
     * nunca al reves.
     */
    @Transactional(readOnly = true)
    public Disponibilidad puedeVotar(UUID servidorId, PlayerIdentity identidad) {
        Instant ahora = Instant.now(reloj);
        byte[] identidadHash = hasher.hashIdentidad(identidad);
        var reglas = ajustes.actuales();

        if (reglas.unVotoAlDia()) {
            var ultimo = repositorio.ultimoVotoAceptadoEnCualquierServidor(identidadHash);
            if (ultimo.isPresent()) {
                Instant disponibleEn = ultimo.get().plus(reglas.ventana());
                if (ahora.isBefore(disponibleEn)) {
                    return new Disponibilidad(false, "YA_VOTO_HOY", Duration.between(ahora, disponibleEn));
                }
            }
            // Con la regla global activa, la ventana por servidor es redundante:
            // quien no ha votado hoy a ninguno tampoco ha votado a este.
            return new Disponibilidad(true, null, Duration.ZERO);
        }

        // Sin la regla global, cada servidor tiene su propia ventana y aqui solo
        // llega el identificador, no la ficha. En lugar de adivinar la ventana
        // —y arriesgarse a decirle que no a quien si puede votar— se responde
        // que si: esto es una cortesia para ahorrarle pasos al jugador, y quien
        // decide de verdad es votar(), que si tiene la ficha delante.
        return new Disponibilidad(true, null, Duration.ZERO);
    }

    /**
     * @param motivo {@code YA_VOTO_HOY} o {@code YA_VOTO_AQUI}; null si puede votar
     * @param restante cuanto falta; cero si puede votar ya
     */
    public record Disponibilidad(boolean puede, String motivo, Duration restante) {}

    /**
     * @param slugServidor servidor al que se vota
     * @param identidad identidad del votante, ya validada sintacticamente
     * @param ip origen de la peticion; se hashea antes de guardarse
     * @param claveIdempotencia clave del cliente para que un reintento no duplique
     */
    @Transactional
    public Resultado votar(String slugServidor, PlayerIdentity identidad, String ip, String claveIdempotencia) {

        // 1. Idempotencia primero. Si esta clave ya se proceso, se devuelve el
        //    mismo resultado sin volver a evaluar nada. Un reintento no puede
        //    consumir la ventana de voto ni generar una segunda recompensa.
        var yaExiste = repositorio.buscarPorClaveIdempotencia(claveIdempotencia);
        if (yaExiste.isPresent()) {
            log.debug("Voto idempotente reutilizado: {}", claveIdempotencia);
            return Resultado.repetido(yaExiste.get());
        }

        var servidor = directorio
                .buscarActivoPorSlug(slugServidor)
                .orElseThrow(() -> new DomainException.NotFound("server.not_found", "El servidor no existe o no admite votos."));

        // 2. Un servidor en modo verificado no acepta identidades que nadie respalda.
        if (!identidad.type().esVerificable() && !servidor.aceptaIdentidadesNoVerificables()) {
            throw new DomainException.Conflict(
                    "vote.identity.not_accepted", "Este servidor solo acepta votos de cuentas verificadas.");
        }

        Instant ahora = Instant.now(reloj);
        byte[] identidadHash = hasher.hashIdentidad(identidad);
        var reglas = ajustes.actuales();

        // 3. Ventana deslizante (ADR-0011). Se calcula desde el ultimo voto
        //    ACEPTADO, en UTC. Sin dias naturales: ni cambios de horario de
        //    verano, ni picos de trafico a medianoche.
        //
        //    La ventana es la MISMA para todos los servidores y la fija la
        //    plataforma (ADR-0023). Cuando cada dueño elegia la suya, bajarla
        //    multiplicaba sus votos sin que sus jugadores hicieran nada
        //    distinto, y el ranking dejaba de comparar servidores.
        var ultimo = repositorio.ultimoVotoAceptado(servidor.id(), identidadHash);
        if (ultimo.isPresent()) {
            Instant disponibleEn = ultimo.get().plus(reglas.ventana());
            if (ahora.isBefore(disponibleEn)) {
                throw new VentanaAbiertaException(Duration.between(ahora, disponibleEn));
            }
        }

        byte[] ipHash = hasher.hashIp(ip);

        // 4. Ventana GLOBAL: un jugador vota a UN servidor al dia, no a uno por
        //    cada servidor. Cambia lo que mide el ranking —de "cuanta gente
        //    juega aqui" a "cual es tu favorito"— y por eso es configurable.
        if (reglas.unVotoAlDia()) {
            var ultimoEnCualquiera = repositorio.ultimoVotoAceptadoEnCualquierServidor(identidadHash);
            if (ultimoEnCualquiera.isPresent()) {
                Instant disponibleEn = ultimoEnCualquiera.get().plus(reglas.ventana());
                if (ahora.isBefore(disponibleEn)) {
                    throw new VotoDiarioConsumidoException(Duration.between(ahora, disponibleEn));
                }
            }
        }

        // 5. Cupo de jugadores distintos por IP.
        //
        //    Es la regla mas delicada del sistema. Los operadores moviles de
        //    Latinoamerica usan CGNAT: miles de clientes comparten una IPv4. Una
        //    casa con hermanos o un cibercafe, tambien. Con el cupo en 1, el
        //    primero que vota deja fuera a todos los demas ese dia.
        //
        //    Se aplica igualmente porque es la decision de producto, pero el
        //    limite se edita desde administracion sin desplegar, y el mensaje al
        //    jugador explica que pasa en vez de decir "voto rechazado".
        if (ipHash != null) {
            int otrosJugadores = repositorio.identidadesDistintasDesdeIp(
                    ipHash, identidadHash, ahora.minus(reglas.ventana()));

            if (otrosJugadores >= reglas.maxJugadoresPorIp()) {
                log.info("Voto bloqueado por cupo de IP: ya votaron {} jugadores desde esa conexion", otrosJugadores);
                throw new CupoDeIpAgotadoException(reglas.maxJugadoresPorIp());
            }
        }

        // 6. Veredicto antifraude.
        var veredicto = antifraude.evaluar(new TrustEngine.Peticion(servidor.id(), identidad, identidadHash, ip, ahora));

        var voto = new Vote(
                Ids.newId(),
                servidor.id(),
                veredicto.aceptado() ? VoteStatus.ACCEPTED : VoteStatus.REJECTED,
                identidad.type().name(),
                identidad.value(),
                identidadHash,
                veredicto.puntuacionRiesgo(),
                veredicto.motivoRechazo(),
                ahora);

        // 7. La insercion puede perder una carrera contra otra peticion con la
        //    misma clave. En ese caso gana la que llego primero y esta devuelve
        //    su resultado: el efecto visible es identico.
        boolean creado = repositorio.insertar(voto, claveIdempotencia, ipHash, "PLATFORM");
        if (!creado) {
            return repositorio
                    .buscarPorClaveIdempotencia(claveIdempotencia)
                    .map(Resultado::repetido)
                    .orElseThrow(() ->
                            new IllegalStateException("Conflicto de idempotencia sin fila asociada: " + claveIdempotencia));
        }

        repositorio.guardarSenales(voto.id(), veredicto.senales());

        if (!voto.fueAceptado()) {
            log.info("Voto rechazado para {} (riesgo {}): {}", slugServidor, veredicto.puntuacionRiesgo(), veredicto.motivoRechazo());
            return Resultado.rechazado(voto);
        }

        // 8. Recompensa y evento, en la misma transaccion que el voto.
        repositorio.crearRecompensaPendiente(voto.id(), servidor.id(), RECOMPENSA_POR_DEFECTO);
        publicador.publish(new VoteAcceptedEvent(
                voto.id(), servidor.id(), identidad.type().name(), identidad.value(), ahora));

        log.info("Voto aceptado para {} (riesgo {})", slugServidor, veredicto.puntuacionRiesgo());
        return Resultado.aceptado(voto, reglas.ventana());
    }

    /**
     * @param voto el voto resultante
     * @param yaExistia si vino de un reintento con la misma clave de idempotencia
     * @param proximoVotoEn cuanto falta para poder volver a votar; null si se rechazo
     */
    public record Resultado(Vote voto, boolean yaExistia, Duration proximoVotoEn) {

        static Resultado aceptado(Vote voto, Duration ventana) {
            return new Resultado(voto, false, ventana);
        }

        static Resultado rechazado(Vote voto) {
            return new Resultado(voto, false, null);
        }

        static Resultado repetido(Vote voto) {
            return new Resultado(voto, true, null);
        }
    }

    /**
     * El jugador ya voto y su ventana sigue abierta.
     *
     * <p>Lleva el tiempo restante para que la interfaz muestre una cuenta atras
     * ("vuelve en 6 h 42 min") en lugar de una fecha absoluta, que obligaria al
     * jugador a razonar sobre husos horarios.
     */
    public static class VentanaAbiertaException extends DomainException.Conflict {
        private final transient Duration restante;

        public VentanaAbiertaException(Duration restante) {
            super("vote.window.open", "Todavia no puedes volver a votar a este servidor.");
            this.restante = restante;
        }

        public Duration restante() {
            return restante;
        }
    }

    /**
     * El jugador ya voto hoy, a otro servidor.
     *
     * <p>Excepcion propia y no {@link VentanaAbiertaException}: el mensaje tiene
     * que ser distinto. "Todavia no puedes votar a ESTE servidor" seria mentira y
     * dejaria al jugador probando otros servidores hasta rendirse.
     */
    public static class VotoDiarioConsumidoException extends DomainException.Conflict {
        private final transient Duration restante;

        public VotoDiarioConsumidoException(Duration restante) {
            super(
                    "vote.daily.used",
                    "Ya has votado hoy. Solo se puede votar a un servidor al dia.");
            this.restante = restante;
        }

        public Duration restante() {
            return restante;
        }
    }

    /**
     * Ya han votado demasiados jugadores distintos desde esta conexion.
     *
     * <p>El mensaje explica el motivo en lugar de decir "voto rechazado". Quien
     * comparte conexion —una casa, un cibercafe, una red movil con CGNAT— no ha
     * hecho nada malo, y merece entender por que no puede votar en lugar de
     * pensar que el sitio esta roto.
     */
    public static class CupoDeIpAgotadoException extends DomainException.Conflict {
        private final transient int limite;

        public CupoDeIpAgotadoException(int limite) {
            super(
                    "vote.ip.quota",
                    limite == 1
                            ? "Ya se ha votado hoy desde esta conexion a internet. Si la compartes con alguien, tendras que esperar a manana."
                            : "Ya han votado " + limite + " jugadores desde esta conexion hoy.");
            this.limite = limite;
        }

        public int limite() {
            return limite;
        }
    }
}
