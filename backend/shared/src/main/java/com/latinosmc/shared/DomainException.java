package com.latinosmc.shared;

/**
 * Error de negocio: la operacion es invalida segun las reglas del dominio.
 *
 * <p>Se distingue de un fallo tecnico (base de datos caida, timeout) a proposito:
 * un error de dominio se le explica al usuario y no se reintenta; un fallo
 * tecnico se registra, se reintenta y no se detalla hacia fuera.
 *
 * <p>El {@code code} es estable y legible por maquina. Es lo que viaja en el
 * campo {@code type} de Problem Details (RFC 9457) y lo que un cliente puede
 * usar para decidir; el mensaje es para humanos y puede cambiar sin previo aviso.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** El recurso solicitado no existe, o el actor no tiene permiso para verlo. */
    public static class NotFound extends DomainException {
        public NotFound(String code, String message) {
            super(code, message);
        }
    }

    /** La peticion es sintacticamente valida pero viola una regla de negocio. */
    public static class Conflict extends DomainException {
        public Conflict(String code, String message) {
            super(code, message);
        }
    }

    /**
     * Hay sesion, pero no alcanza para esto.
     *
     * <p>Distinto de {@link NotFound}: alli se oculta la existencia del recurso
     * porque revelarla ya seria una fuga —quien prueba identificadores ajenos
     * aprenderia cuales existen—. Aqui no hay nada que ocultar: la ruta de
     * administracion es publica y conocida, y decir "no eres administrador" no le
     * da a nadie informacion que no tuviera.
     */
    public static class Forbidden extends DomainException {
        public Forbidden(String code, String message) {
            super(code, message);
        }
    }
}
