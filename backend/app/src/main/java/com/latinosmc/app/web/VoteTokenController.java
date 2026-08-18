package com.latinosmc.app.web;

import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.voting.CastVoteService;
import com.latinosmc.voting.VoteTokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Votar con token (ADR-0016).
 *
 * <p>El jugador escribe {@code /vote} en su servidor, recibe un enlace y la web
 * abre ya sabiendo quien es. No hay formulario de nick: si lo hubiera, cualquiera
 * podria votar en nombre de cualquiera sin haber pisado nunca ese servidor.
 */
@RestController
@RequestMapping("/api/v1/vote-tokens")
public class VoteTokenController {

    private final VoteTokenService tokens;
    private final CastVoteService votos;

    private final com.latinosmc.servers.ServerRepository servidores;

    public VoteTokenController(
            VoteTokenService tokens, CastVoteService votos, com.latinosmc.servers.ServerRepository servidores) {
        this.servidores = servidores;
        this.tokens = tokens;
        this.votos = votos;
    }

    /**
     * Resuelve el token para que la web sepa a quien y a que servidor va el voto.
     *
     * <p>No lo consume: el jugador todavia no ha pulsado nada. Si el enlace ya no
     * vale, se le dice claramente que vuelva a escribir {@code /vote}.
     */
    @GetMapping("/{token}")
    public DatosParaVotar resolver(@PathVariable String token) {
        var datos = tokens.resolver(token);
        return new DatosParaVotar(datos.servidorSlug(), datos.servidorNombre(), datos.identidadValor());
    }

    /**
     * Emite el voto.
     *
     * <p>El token hace de clave de idempotencia. Es lo que hace que dos
     * pulsaciones seguidas del boton —o un doble clic, o volver atras y repetir—
     * devuelvan el mismo voto en lugar de crear dos.
     *
     * <p>El token se consume <b>despues</b> del voto, no antes: si se consumiera
     * primero y el voto fallara porque la ventana sigue abierta, el jugador se
     * quedaria sin enlace por haber llegado pronto.
     */
    @PostMapping("/{token}/vote")
    public ResponseEntity<VoteController.RespuestaVoto> votar(
            @PathVariable String token, HttpServletRequest peticion) {

        // Doble clic, o volver atras y repetir: el token ya se gasto pero el voto
        // existe. Se devuelve ese mismo resultado en lugar de "enlace no valido",
        // que pareceria un fallo cuando en realidad todo salio bien.
        var yaEmitido = tokens.votoYaEmitido(token);
        if (yaEmitido.isPresent()) {
            var voto = yaEmitido.get();
            return ResponseEntity.ok(new VoteController.RespuestaVoto(
                    voto.id(), voto.estado().name(), true, null));
        }

        var datos = tokens.resolver(token);

        // No se aceptan votos a un servidor caido. Votar a algo donde no se puede
        // entrar no mide nada, y la recompensa no llegaria nunca porque no hay
        // plugin escuchando al otro lado.
        //
        // Si NO tenemos ping reciente se deja votar: la duda beneficia al
        // servidor. Un fallo de nuestra medicion no puede costarle los votos del
        // dia a alguien que si esta funcionando.
        if (!servidores.respondeAlPing(datos.servidorId())) {
            throw new com.latinosmc.shared.DomainException.Conflict(
                    "server.offline",
                    "Este servidor no responde ahora mismo. Vuelve a intentarlo cuando esté en línea.");
        }

        PlayerIdentity identidad = switch (datos.identidadTipo()) {
            case "MOJANG_UUID" -> PlayerIdentity.mojangUuid(datos.identidadValor());
            default -> PlayerIdentity.offlineName(datos.identidadValor());
        };

        var resultado = votos.votar(datos.servidorSlug(), identidad, IpDelCliente.de(peticion), "token:" + token);

        if (!resultado.yaExistia()) {
            tokens.consumir(token, resultado.voto().id());
        }

        var cuerpo = new VoteController.RespuestaVoto(
                resultado.voto().id(),
                resultado.voto().estado().name(),
                resultado.yaExistia(),
                resultado.proximoVotoEn() == null ? null : resultado.proximoVotoEn().toSeconds());

        return ResponseEntity.status(resultado.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(cuerpo);
    }

    /**
     * @param jugador nick ya resuelto. La web lo muestra para que el jugador
     *     confirme que es el; nunca lo pregunta.
     */
    public record DatosParaVotar(String servidorSlug, String servidorNombre, String jugador) {}

    /** Identificador del voto creado, para trazabilidad. */
    public record VotoCreado(UUID votoId) {}
}
