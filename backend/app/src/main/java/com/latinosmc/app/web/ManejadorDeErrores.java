package com.latinosmc.app.web;

import com.latinosmc.shared.DomainException;
import com.latinosmc.voting.CastVoteService;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Formato unico de error para toda la API: Problem Details (RFC 9457), tal como
 * fija el ADR-0006.
 *
 * <p>El campo {@code type} lleva un codigo estable y legible por maquina; es el
 * contrato. El {@code detail} es para humanos y puede cambiar sin previo aviso:
 * un cliente que dependa del texto se rompera, y debe hacerlo.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);
    private static final String BASE_TIPO = "https://latinosmc.com/errores/";

    /** El jugador ya voto y su ventana sigue abierta. */
    @ExceptionHandler(CastVoteService.VentanaAbiertaException.class)
    public ProblemDetail ventanaAbierta(CastVoteService.VentanaAbiertaException e) {
        ProblemDetail problema = crear(HttpStatus.CONFLICT, e.code(), e.getMessage());
        // Segundos restantes para que el cliente pinte una cuenta atras en vez de
        // una fecha absoluta, que obligaria al jugador a pensar en husos horarios.
        problema.setProperty("segundosRestantes", e.restante().toSeconds());
        return problema;
    }

    /**
     * Ya voto hoy, a otro servidor.
     *
     * <p>Lleva los segundos restantes igual que la ventana por servidor, para que
     * la web pinte una cuenta atras. Sin ese dato solo se puede decir "vuelve
     * manana", que es peor: manana no es un momento concreto.
     */
    @ExceptionHandler(CastVoteService.VotoDiarioConsumidoException.class)
    public ProblemDetail votoDiarioConsumido(CastVoteService.VotoDiarioConsumidoException e) {
        ProblemDetail problema = crear(HttpStatus.CONFLICT, e.code(), e.getMessage());
        problema.setProperty("segundosRestantes", e.restante().toSeconds());
        return problema;
    }

    /**
     * Demasiados jugadores desde la misma conexion.
     *
     * <p>Se manda el limite para que la web pueda explicarlo. Quien comparte
     * conexion no ha hecho nada malo y merece entender que ha pasado, no un
     * "voto rechazado" a secas que parece un fallo del sitio.
     */
    @ExceptionHandler(CastVoteService.CupoDeIpAgotadoException.class)
    public ProblemDetail cupoDeIpAgotado(CastVoteService.CupoDeIpAgotadoException e) {
        ProblemDetail problema = crear(HttpStatus.CONFLICT, e.code(), e.getMessage());
        problema.setProperty("limitePorConexion", e.limite());
        return problema;
    }

    /** Falta sesion. 401 y no 403: el problema es que no sabemos quien eres. */
    @ExceptionHandler(UsuarioActualResolver.NoAutenticado.class)
    public ProblemDetail noAutenticado(UsuarioActualResolver.NoAutenticado e) {
        return crear(HttpStatus.UNAUTHORIZED, e.code(), e.getMessage());
    }

    /**
     * Credenciales incorrectas. El mensaje no distingue si el fallo fue el correo
     * o la contrasena: hacerlo permitiria averiguar que cuentas existen.
     */
    @ExceptionHandler(com.latinosmc.identity.AuthService.CredencialesInvalidas.class)
    public ProblemDetail credencialesInvalidas(com.latinosmc.identity.AuthService.CredencialesInvalidas e) {
        return crear(HttpStatus.UNAUTHORIZED, e.code(), e.getMessage());
    }

    @ExceptionHandler(DomainException.NotFound.class)
    public ProblemDetail noEncontrado(DomainException.NotFound e) {
        return crear(HttpStatus.NOT_FOUND, e.code(), e.getMessage());
    }

    @ExceptionHandler(DomainException.Conflict.class)
    public ProblemDetail conflicto(DomainException.Conflict e) {
        return crear(HttpStatus.CONFLICT, e.code(), e.getMessage());
    }

    /** Hay sesion pero no alcanza. 403 y no 401: reintentar el login no arregla nada. */
    @ExceptionHandler(DomainException.Forbidden.class)
    public ProblemDetail prohibido(DomainException.Forbidden e) {
        return crear(HttpStatus.FORBIDDEN, e.code(), e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail dominio(DomainException e) {
        return crear(HttpStatus.BAD_REQUEST, e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        ProblemDetail problema =
                crear(HttpStatus.BAD_REQUEST, "request.invalid", "Los datos enviados no son validos.");
        problema.setProperty(
                "campos",
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> java.util.Map.of("campo", f.getField(), "error", String.valueOf(f.getDefaultMessage())))
                        .toList());
        return problema;
    }

    /**
     * Red de seguridad. Nunca se expone el detalle interno hacia fuera: un stack
     * trace le dice a un atacante que framework, que version y que estructura hay
     * detras. Al usuario se le da un identificador; el detalle vive en los logs.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        String referencia = java.util.UUID.randomUUID().toString();
        log.error("Error inesperado [{}]", referencia, e);
        ProblemDetail problema = crear(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "Algo ha fallado por nuestra parte. Si el problema persiste, indica esta referencia al soporte.");
        problema.setProperty("referencia", referencia);
        return problema;
    }

    private ProblemDetail crear(HttpStatus estado, String codigo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatus(estado);
        problema.setType(URI.create(BASE_TIPO + codigo));
        problema.setTitle(codigo);
        problema.setDetail(detalle);
        return problema;
    }
}
