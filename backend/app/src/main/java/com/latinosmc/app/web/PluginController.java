package com.latinosmc.app.web;

import com.latinosmc.servers.ApiKeys;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.voting.RewardRepository;
import com.latinosmc.voting.VoteTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del plugin. Superficie separada de la publica (ADR-0006): otro consumidor,
 * otra autenticacion y otro ciclo de versionado.
 *
 * <p><strong>Modelo de consulta, no de envio.</strong> El plugin pregunta por sus
 * recompensas en vez de recibirlas por webhook. Es deliberado: la mayoria de
 * servidores de Minecraft estan detras de NAT o cortafuegos y no son alcanzables
 * desde internet. Un modelo de envio dejaria fuera a buena parte del mercado.
 */
@RestController
@RequestMapping("/api/v1/plugin")
public class PluginController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginController.class);

    private static final int LOTE_MAXIMO = 100;

    private final ServerRepository servidores;
    private final RewardRepository recompensas;
    private final VoteTokenService tokens;
    private final com.latinosmc.voting.CastVoteService votos;
    private final String sitioUrl;

    /**
     * Tope de codigos que un servidor puede pedir por minuto.
     *
     * <p>La clave de API es del dueno del servidor, y un dueno deshonesto puede
     * pedir codigos para nicks de jugadores que jamas han pisado su servidor. El
     * limite general del plugin (60/min) daba para 86.000 codigos al dia: mas que
     * suficiente para fabricar una base de votos falsa.
     *
     * <p>Doce por minuto es holgado para un servidor real —son doce jugadores
     * distintos escribiendo /vote en el mismo minuto— y convierte el ataque en
     * algo lento y muy visible en el historial.
     */
    private static final int CODIGOS_POR_MINUTO = 12;

    private final java.util.concurrent.ConcurrentHashMap<UUID, Ventana> emisiones =
            new java.util.concurrent.ConcurrentHashMap<>();

    public PluginController(
            ServerRepository servidores,
            RewardRepository recompensas,
            VoteTokenService tokens,
            com.latinosmc.voting.CastVoteService votos,
            @org.springframework.beans.factory.annotation.Value("${latinosmc.sitio-url}") String sitioUrl) {
        this.servidores = servidores;
        this.recompensas = recompensas;
        this.tokens = tokens;
        this.votos = votos;
        this.sitioUrl = sitioUrl.endsWith("/") ? sitioUrl.substring(0, sitioUrl.length() - 1) : sitioUrl;
    }

    /**
     * Latido del plugin. Confirma conectividad y, la primera vez, verifica el
     * servidor: si el plugin puede autenticarse con la clave que solo ve el dueno,
     * es que la instalo en su servidor.
     *
     * <p>Lo que el plugin reporte aqui es informativo y NO alimenta el ranking
     * (ADR-0009): corre en hardware del cliente, que es justo quien tiene incentivo
     * para inflar sus numeros. Los jugadores conectados los medimos nosotros con
     * ping directo.
     */
    @PostMapping("/heartbeat")
    @Transactional
    public RespuestaHeartbeat heartbeat(@RequestHeader("X-Api-Key") String clave) {
        UUID servidorId = autenticar(clave);
        boolean verificadoAhora = servidores.activarPorPrimeraConexion(servidorId) > 0;

        // Si se habia retirado por inactividad, este latido lo devuelve al
        // directorio (ADR-0024). Volver es automatico a proposito: quien
        // reinstala el plugin ya ha hecho lo que hacia falta, y obligarle ademas
        // a escribir a soporte seria castigarle por arreglarlo.
        if (servidores.reactivarTrasInactividad(servidorId) > 0) {
            log.info("Servidor {} vuelve al directorio: el plugin ha conectado de nuevo.", servidorId);
        }

        return new RespuestaHeartbeat(servidorId, verificadoAhora);
    }

    /**
     * Emite un token de voto para un jugador (ADR-0016).
     *
     * <p>Lo pide el plugin cuando el jugador escribe {@code /vote}. Como solo un
     * servidor autenticado puede pedirlo, y solo puede pedirlo para jugadores que
     * estan en el, la identidad queda probada: la web ya no necesita preguntar el
     * nick, que era el agujero del flujo original.
     */
    @PostMapping("/vote-tokens")
    @Transactional
    public RespuestaToken emitirToken(
            @RequestHeader("X-Api-Key") String clave, @RequestBody @Valid PeticionToken peticion) {

        UUID servidorId = autenticar(clave);

        PlayerIdentity identidad = switch (peticion.tipoIdentidad().toUpperCase()) {
            case "MOJANG_UUID" -> PlayerIdentity.mojangUuid(peticion.identidad());
            case "OFFLINE_NAME" -> PlayerIdentity.offlineName(peticion.identidad());
            default -> throw new DomainException.Conflict(
                    "identity.type.unknown", "Tipo de identidad no reconocido.");
        };

        // Se pregunta ANTES de emitir. Sin esto, quien ya voto recibe igualmente
        // su codigo, va a la web, lo teclea y solo entonces descubre que no
        // valia: cuatro pasos para llegar a un no que se sabia desde el principio.
        var disponibilidad = votos.puedeVotar(servidorId, identidad);
        if (!disponibilidad.puede()) {
            return RespuestaToken.noPuedeVotar(disponibilidad.motivo(), disponibilidad.restante().toSeconds());
        }

        if (!hayCupoParaEmitir(servidorId)) {
            log.warn("Servidor {} pidiendo codigos por encima del ritmo normal", servidorId);
            throw new DomainException.Conflict(
                    "vote.token.rate", "Se han pedido demasiados codigos seguidos. Intentalo en un minuto.");
        }

        var emitido = tokens.emitir(servidorId, identidad);

        return new RespuestaToken(
                emitido.token(),
                sitioUrl + "/votar/" + com.latinosmc.voting.CodigoDeVoto.normalizar(emitido.token()),
                emitido.vigencia().toSeconds(),
                true,
                null,
                0);
    }

    /**
     * Cupo de emision por servidor.
     *
     * <p>En memoria y no en base de datos: es un limite de ritmo, no un dato. Si
     * el proceso se reinicia se pierde el contador, y no pasa nada — un atacante
     * no puede provocar reinicios a voluntad, y persistirlo costaria una escritura
     * en cada /vote de cada servidor.
     */
    private boolean hayCupoParaEmitir(UUID servidorId) {
        long minutoActual = System.currentTimeMillis() / 60_000;

        Ventana ventana = emisiones.compute(servidorId, (id, actual) -> {
            if (actual == null || actual.minuto != minutoActual) {
                return new Ventana(minutoActual, 1);
            }
            return new Ventana(minutoActual, actual.usos + 1);
        });

        // El mapa solo crece con servidores activos, pero conviene que no crezca
        // para siempre si alguien da de alta miles.
        if (emisiones.size() > 10_000) {
            emisiones.entrySet().removeIf(e -> e.getValue().minuto < minutoActual);
        }

        return ventana.usos <= CODIGOS_POR_MINUTO;
    }

    private record Ventana(long minuto, int usos) {}

    /** Recompensas pendientes de entregar en este servidor. */
    @GetMapping("/rewards/pending")
    @Transactional
    public ResponseEntity<List<RecompensaDto>> pendientes(
            @RequestHeader("X-Api-Key") String clave, @RequestParam(defaultValue = "50") int limite) {

        UUID servidorId = autenticar(clave);
        int limiteSeguro = Math.clamp(limite, 1, LOTE_MAXIMO);

        var pendientes = recompensas.pendientesDe(servidorId, limiteSeguro);

        // Se cuenta el intento al entregarlas al plugin. Si el plugin no confirma,
        // se reintenta; pero un fallo permanente (jugador que nunca vuelve, tipo de
        // recompensa que ese servidor no soporta) no puede reintentarse para
        // siempre.
        recompensas.registrarIntento(servidorId, pendientes.stream().map(RewardRepository.RecompensaPendiente::id).toList());

        return ResponseEntity.ok(pendientes.stream()
                .map(r -> new RecompensaDto(r.id(), r.tipo(), r.identidadTipo(), r.identidadValor()))
                .toList());
    }

    /**
     * El plugin confirma que entrego estas recompensas.
     *
     * <p>Es la mitad del acuerdo de entrega al-menos-una-vez: nosotros reintentamos
     * hasta recibir confirmacion, y el plugin descarta los identificadores que ya
     * aplico. Por eso el plugin DEBE persistir esos identificadores en disco: si
     * los guarda solo en memoria, un reinicio del servidor provoca recompensas
     * duplicadas en cuanto lleguen los reintentos.
     */
    @PostMapping("/rewards/ack")
    @Transactional
    public RespuestaAck confirmar(@RequestHeader("X-Api-Key") String clave, @RequestBody PeticionAck peticion) {
        UUID servidorId = autenticar(clave);
        int confirmadas = recompensas.marcarEntregadas(servidorId, peticion.recompensaIds());
        return new RespuestaAck(confirmadas);
    }

    /**
     * Resuelve la clave del plugin al servidor al que pertenece.
     *
     * <p>Se compara por hash: la clave en claro nunca se ha guardado. Un fallo
     * devuelve siempre el mismo error, sin distinguir "clave inexistente" de
     * "clave revocada", para no dar informacion util a quien este probando claves.
     */
    private UUID autenticar(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new DomainException.NotFound("plugin.auth.invalid", "Clave de API no valida.");
        }
        String hash = ApiKeys.hash(clave);
        UUID servidorId = servidores
                .servidorPorClaveApi(hash)
                .orElseThrow(() -> new DomainException.NotFound("plugin.auth.invalid", "Clave de API no valida."));
        servidores.registrarUsoDeClave(hash);
        return servidorId;
    }

    public record RespuestaHeartbeat(UUID servidorId, boolean verificadoAhora) {}

    /**
     * @param tipoIdentidad {@code MOJANG_UUID} en servidores premium,
     *     {@code OFFLINE_NAME} en offline-mode
     */
    public record PeticionToken(@NotBlank String tipoIdentidad, @NotBlank String identidad) {}

    /**
     * @param url enlace completo que el plugin muestra al jugador en el chat
     * @param vigenciaSegundos cuanto dura antes de caducar
     */
    /**
     * @param puedeVotar false si el jugador ya voto; entonces token y url son null
     * @param motivo {@code YA_VOTO_HOY} o {@code YA_VOTO_AQUI}
     * @param segundosRestantes cuanto falta para que pueda volver a votar
     */
    public record RespuestaToken(
            String token,
            String url,
            long vigenciaSegundos,
            boolean puedeVotar,
            String motivo,
            long segundosRestantes) {

        static RespuestaToken noPuedeVotar(String motivo, long segundosRestantes) {
            return new RespuestaToken(null, null, 0, false, motivo, segundosRestantes);
        }
    }

    public record RecompensaDto(UUID id, String tipo, String identidadTipo, String identidadValor) {}

    public record PeticionAck(List<UUID> recompensaIds) {}

    public record RespuestaAck(int confirmadas) {}
}
