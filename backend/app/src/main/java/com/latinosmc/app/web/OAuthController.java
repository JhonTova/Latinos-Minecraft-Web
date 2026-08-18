package com.latinosmc.app.web;

import com.latinosmc.identity.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Acceso con Google y Discord. */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class OAuthController {

    private static final String COOKIE_STATE = "lmc_oauth_state";
    private static final String COOKIE_PKCE = "lmc_oauth_pkce";

    /** Corta a proposito: es un ida y vuelta de segundos, no una sesion. */
    private static final Duration VIDA_COOKIE_FLUJO = Duration.ofMinutes(10);

    /**
     * Ruta por la que la web reenvía estas dos llamadas al backend.
     *
     * <p>Está aquí porque la URL de retorno tiene que ser una dirección del sitio,
     * no de la API. Ver {@link #urlRetorno(String)}.
     */
    private static final String RUTA_EN_LA_WEB = "/api/backend/auth/oauth/";

    private final OAuthService oauth;
    private final String sitioUrl;
    private final boolean cookieSegura;

    public OAuthController(
            OAuthService oauth,
            @Value("${latinosmc.sitio-url}") String sitioUrl,
            org.springframework.core.env.Environment entorno) {
        this.oauth = oauth;
        this.sitioUrl = sinBarraFinal(sitioUrl);
        this.cookieSegura = entorno.matchesProfiles("prod");
    }

    /** Proveedores configurados, para que la web sepa qué botones pintar. */
    @GetMapping
    public List<String> disponibles() {
        return oauth.disponibles();
    }

    /** Envía al usuario al proveedor. */
    @GetMapping("/{proveedor}")
    public ResponseEntity<Void> iniciar(@PathVariable String proveedor) {
        var inicio = oauth.iniciar(proveedor, urlRetorno(proveedor));

        // El state y el verificador PKCE viajan en cookies httpOnly. No pueden ir
        // en la URL: acabarian en el historial del navegador y en los logs del
        // proveedor, y dejarian de ser secretos.
        return ResponseEntity.status(302)
                .location(URI.create(inicio.urlDestino()))
                .header(HttpHeaders.SET_COOKIE, cookieDeFlujo(COOKIE_STATE, inicio.state()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieDeFlujo(COOKIE_PKCE, inicio.verificadorPkce()).toString())
                .build();
    }

    /**
     * Vuelta desde el proveedor.
     *
     * <p>Responde con una redirección al sitio, no con JSON: el navegador llega
     * aquí siguiendo un enlace, no por una llamada de JavaScript.
     */
    @GetMapping("/{proveedor}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String proveedor,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = COOKIE_STATE, required = false) String stateGuardado,
            @CookieValue(name = COOKIE_PKCE, required = false) String verificador,
            HttpServletRequest http) {

        // El usuario pulsó "cancelar" en la pantalla del proveedor. No es un fallo:
        // se le devuelve al login sin ruido.
        if (error != null || code == null) {
            return redirigirA("/login?error=cancelado");
        }

        try {
            var sesion = oauth.completar(
                    proveedor,
                    code,
                    state,
                    stateGuardado,
                    verificador,
                    urlRetorno(proveedor),
                    http.getHeader(HttpHeaders.USER_AGENT),
                    IpDelCliente.de(http));

            ResponseCookie sesionCookie = ResponseCookie.from(
                            UsuarioActualResolver.COOKIE_SESION, sesion.token())
                    .httpOnly(true)
                    .secure(cookieSegura)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(sesion.duracion())
                    .build();

            return ResponseEntity.status(302)
                    .location(URI.create(sitioUrl + "/panel"))
                    .header(HttpHeaders.SET_COOKIE, sesionCookie.toString())
                    // Las cookies del flujo ya no valen para nada: se borran para no
                    // dejar secretos usados dando vueltas por el navegador.
                    .header(HttpHeaders.SET_COOKIE, borrar(COOKIE_STATE).toString())
                    .header(HttpHeaders.SET_COOKIE, borrar(COOKIE_PKCE).toString())
                    .build();

        } catch (RuntimeException e) {
            return redirigirA("/login?error=oauth");
        }
    }

    /**
     * URL de retorno.
     *
     * <p>Se construye en el servidor a partir de la configuración, nunca con datos
     * de la petición. Si viniera del cliente, alguien podría hacer que el
     * proveedor devolviera el código de autorización a su propio servidor.
     *
     * <p><b>Apunta al sitio, no a la API.</b> La cookie de sesión se guarda para el
     * dominio que responde a la vuelta del proveedor. Si esta URL apuntara a
     * {@code api.dominio.com}, la sesión quedaría en ese dominio y la web —que está
     * en {@code dominio.com}— nunca la vería: el usuario acabaría el flujo entero
     * sin haber entrado, y sin ningún error que lo explicara. La web reenvía esta
     * ruta al backend, así que la vuelta aterriza en el dominio correcto.
     */
    private String urlRetorno(String proveedor) {
        return sitioUrl + RUTA_EN_LA_WEB + proveedor.toLowerCase() + "/callback";
    }

    private ResponseEntity<Void> redirigirA(String ruta) {
        return ResponseEntity.status(302)
                .location(URI.create(sitioUrl + ruta))
                .header(HttpHeaders.SET_COOKIE, borrar(COOKIE_STATE).toString())
                .header(HttpHeaders.SET_COOKIE, borrar(COOKIE_PKCE).toString())
                .build();
    }

    private ResponseCookie cookieDeFlujo(String nombre, String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(cookieSegura)
                // Lax y no Strict: con Strict, el navegador no enviaría la cookie al
                // volver desde el dominio del proveedor y el flujo fallaría siempre.
                .sameSite("Lax")
                .path("/")
                .maxAge(VIDA_COOKIE_FLUJO)
                .build();
    }

    private ResponseCookie borrar(String nombre) {
        return ResponseCookie.from(nombre, "")
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
