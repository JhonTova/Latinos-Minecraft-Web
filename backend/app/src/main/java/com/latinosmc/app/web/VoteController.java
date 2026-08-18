package com.latinosmc.app.web;

import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.voting.CastVoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/servers/{slug}/votes")
public class VoteController {

    private final CastVoteService servicio;

    public VoteController(CastVoteService servicio) {
        this.servicio = servicio;
    }

    /**
     * Emite un voto.
     *
     * <p>La cabecera {@code Idempotency-Key} es opcional pero muy recomendable: con
     * ella, un reintento por red inestable devuelve el mismo voto en lugar de
     * intentar crear otro. Sin ella se genera una por peticion, asi que un
     * reintento cuenta como intento nuevo (y lo frenara la ventana de voto, no la
     * idempotencia).
     */
    @PostMapping
    public ResponseEntity<RespuestaVoto> votar(
            @PathVariable String slug,
            @RequestBody @jakarta.validation.Valid PeticionVoto peticion,
            @RequestHeader(value = "Idempotency-Key", required = false) String claveIdempotencia,
            HttpServletRequest request) {

        PlayerIdentity identidad = peticion.aIdentidad();
        String clave = (claveIdempotencia == null || claveIdempotencia.isBlank())
                ? UUID.randomUUID().toString()
                : claveIdempotencia;

        var resultado = servicio.votar(slug, identidad, IpDelCliente.de(request), clave);

        var cuerpo = new RespuestaVoto(
                resultado.voto().id(),
                resultado.voto().estado().name(),
                resultado.yaExistia(),
                resultado.proximoVotoEn() == null ? null : resultado.proximoVotoEn().toSeconds());

        HttpStatus estado = resultado.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(cuerpo);
    }

    /**
     * @param tipoIdentidad {@code MOJANG_UUID} o {@code OFFLINE_NAME}
     * @param identidad UUID o nombre del jugador, segun el tipo
     */
    public record PeticionVoto(@NotBlank String tipoIdentidad, @NotBlank String identidad) {

        PlayerIdentity aIdentidad() {
            return switch (tipoIdentidad.toUpperCase()) {
                case "MOJANG_UUID" -> PlayerIdentity.mojangUuid(identidad);
                case "OFFLINE_NAME" -> PlayerIdentity.offlineName(identidad);
                default -> throw new com.latinosmc.shared.DomainException.Conflict(
                        "identity.type.unknown", "Tipo de identidad no reconocido.");
            };
        }
    }

    /**
     * @param yaExistia true si vino de un reintento con la misma clave
     * @param segundosHastaProximoVoto null si el voto fue rechazado
     */
    public record RespuestaVoto(UUID votoId, String estado, boolean yaExistia, Long segundosHastaProximoVoto) {}
}
