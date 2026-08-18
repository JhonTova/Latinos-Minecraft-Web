package com.latinosmc.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Validacion de un enlace que vamos a publicar apuntando fuera.
 *
 * <p>Un enlace saliente desde nuestro dominio es nuestra reputacion, y ademas
 * una superficie de ataque real: {@code javascript:alert(1)} en un {@code href}
 * es XSS ejecutandose en nuestra pagina, con nuestra sesion y nuestras cookies
 * delante.
 *
 * <p><strong>Lista blanca, no lista negra.</strong> Prohibir {@code javascript:}
 * y {@code data:} deja fuera {@code vbscript:}, {@code file:}, y los esquemas que
 * inventen los navegadores el año que viene. Solo se aceptan {@code http} y
 * {@code https}; cualquier otra cosa se rechaza sin discutir.
 */
public final class EnlaceExterno {

    private static final Set<String> ESQUEMAS_PERMITIDOS = Set.of("http", "https");

    /**
     * Longitud maxima. No hay ningun enlace legitimo de 4 KB, y los muy largos son
     * la forma habitual de colar cargas utiles esperando que algo por el camino
     * los trunque en mitad de un escape.
     */
    private static final int LARGO_MAXIMO = 512;

    private EnlaceExterno() {}

    /**
     * Normaliza y valida el enlace.
     *
     * @return la URL en forma canonica, lista para publicar
     * @throws DomainException.Conflict si no es publicable
     */
    public static String validar(String url) {
        if (url == null || url.isBlank()) {
            throw rechazo("El enlace no puede estar vacio.");
        }

        String limpia = url.trim();

        // Los caracteres de control permiten partir un esquema para que la
        // validacion vea una cosa y el navegador otra: "java\nscript:" pasa un
        // startsWith ingenuo y el navegador lo ejecuta igual.
        for (int i = 0; i < limpia.length(); i++) {
            if (Character.isISOControl(limpia.charAt(i))) {
                throw rechazo("El enlace contiene caracteres no validos.");
            }
        }

        if (limpia.length() > LARGO_MAXIMO) {
            throw rechazo("El enlace es demasiado largo.");
        }

        URI uri;
        try {
            uri = new URI(limpia);
        } catch (URISyntaxException e) {
            throw rechazo("Ese enlace no es una direccion valida.");
        }

        String esquema = uri.getScheme();
        if (esquema == null || !ESQUEMAS_PERMITIDOS.contains(esquema.toLowerCase(Locale.ROOT))) {
            throw rechazo("El enlace tiene que empezar por http:// o https://");
        }

        // Sin host no hay a donde ir: "http:///algo" o "https:" pasan el filtro de
        // esquema y no llevan a ninguna parte.
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw rechazo("Al enlace le falta el dominio.");
        }

        // El usuario dentro de la URL ("https://banco.com@malo.com") existe para
        // que el ojo lea un dominio y el navegador vaya a otro. No hay ningun uso
        // legitimo de esto en un enlace publicado.
        if (uri.getUserInfo() != null) {
            throw rechazo("El enlace no puede llevar credenciales.");
        }

        return uri.toString();
    }

    private static DomainException.Conflict rechazo(String mensaje) {
        return new DomainException.Conflict("link.invalid", mensaje);
    }
}
