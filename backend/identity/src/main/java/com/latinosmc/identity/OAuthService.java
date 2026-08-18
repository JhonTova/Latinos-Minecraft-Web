package com.latinosmc.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesion con Google y Discord.
 *
 * <p>Flujo de codigo de autorizacion con PKCE. Dos protecciones que no son
 * opcionales:
 *
 * <ul>
 *   <li><b>{@code state}</b> — valor aleatorio que viaja de ida y vuelta. Sin el,
 *       alguien puede provocar que <em>tu</em> navegador complete <em>su</em>
 *       login y acabes con la sesion del atacante sin darte cuenta, subiendo tus
 *       datos a su cuenta.
 *   <li><b>PKCE</b> — el codigo de autorizacion solo sirve si se acompaña del
 *       secreto que genero quien inicio el flujo. Si alguien intercepta el codigo
 *       en la URL de vuelta, no puede canjearlo.
 * </ul>
 *
 * <p>Ambos viajan en una cookie httpOnly de vida corta, no en base de datos: son
 * efimeros y de un solo uso, y una tabla para eso solo añadiria limpieza que
 * mantener.
 */
@Service
public class OAuthService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Map<String, ProveedorOAuth> proveedores;
    private final IdentityRepository identidades;
    private final OAuthRepository cuentas;
    private final AuthService auth;
    private final EmailVerificationService verificacion;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public OAuthService(
            Map<String, ProveedorOAuth> proveedores,
            IdentityRepository identidades,
            OAuthRepository cuentas,
            AuthService auth,
            EmailVerificationService verificacion,
            ObjectMapper json) {
        this.proveedores = proveedores;
        this.identidades = identidades;
        this.cuentas = cuentas;
        this.auth = auth;
        this.verificacion = verificacion;
        this.json = json;
    }

    public ProveedorOAuth proveedor(String codigo) {
        ProveedorOAuth proveedor = proveedores.get(codigo.toUpperCase());
        if (proveedor == null || !proveedor.estaConfigurado()) {
            throw new DomainException.NotFound(
                    "oauth.provider_unavailable", "Ese metodo de acceso no esta disponible ahora mismo.");
        }
        return proveedor;
    }

    /** Proveedores con credenciales, para que el frontend sepa que botones pintar. */
    public java.util.List<String> disponibles() {
        return proveedores.values().stream()
                .filter(ProveedorOAuth::estaConfigurado)
                .map(ProveedorOAuth::codigo)
                .sorted()
                .toList();
    }

    /** Prepara el inicio del flujo: URL a la que enviar al usuario y secretos a guardar. */
    public InicioDeFlujo iniciar(String codigoProveedor, String urlRetorno) {
        ProveedorOAuth proveedor = proveedor(codigoProveedor);

        String state = aleatorio(32);
        String verificador = aleatorio(48);
        String reto = base64Url(sha256(verificador));

        String url = proveedor.urlAutorizacion() + "?"
                + parametros(Map.of(
                        "client_id", proveedor.clienteId(),
                        "redirect_uri", urlRetorno,
                        "response_type", "code",
                        "scope", proveedor.scopes(),
                        "state", state,
                        "code_challenge", reto,
                        "code_challenge_method", "S256"));

        return new InicioDeFlujo(url, state, verificador);
    }

    /**
     * Completa el flujo: canjea el codigo, resuelve la identidad y abre sesion.
     *
     * @param stateRecibido el que devuelve el proveedor
     * @param stateGuardado el que se emitio al iniciar
     */
    @Transactional
    public AuthService.SesionCreada completar(
            String codigoProveedor,
            String codigo,
            String stateRecibido,
            String stateGuardado,
            String verificador,
            String urlRetorno,
            String userAgent,
            String ip) {

        // Comparacion en tiempo constante: comparar con equals filtraria, por el
        // tiempo de respuesta, cuantos caracteres iniciales se acertaron.
        if (stateGuardado == null || stateRecibido == null
                || !MessageDigest.isEqual(
                        stateGuardado.getBytes(StandardCharsets.UTF_8),
                        stateRecibido.getBytes(StandardCharsets.UTF_8))) {
            throw new DomainException.Conflict(
                    "oauth.state_mismatch", "La sesion de acceso ha caducado. Vuelve a intentarlo.");
        }

        ProveedorOAuth proveedor = proveedor(codigoProveedor);
        String accessToken = canjearCodigo(proveedor, codigo, verificador, urlRetorno);
        PerfilExterno perfil = leerPerfil(proveedor, accessToken);

        return abrirSesion(proveedor, perfil, userAgent, ip);
    }

    private AuthService.SesionCreada abrirSesion(
            ProveedorOAuth proveedor, PerfilExterno perfil, String userAgent, String ip) {

        // 1. ¿Ya habia iniciado sesion con este proveedor?
        Optional<UUID> existente = cuentas.buscarUsuario(proveedor.codigo(), perfil.idEnProveedor());
        if (existente.isPresent()) {
            cuentas.registrarAcceso(proveedor.codigo(), perfil.idEnProveedor());
            return auth.abrirSesionPara(existente.get(), userAgent, ip);
        }

        // 2. ¿Existe una cuenta con ese correo? Se vincula.
        //
        //    Solo si el proveedor confirma que el correo esta verificado. Sin esa
        //    comprobacion, alguien podria registrar en un proveedor laxo una
        //    cuenta con el correo de otra persona y quedarse con su cuenta aqui.
        Optional<IdentityRepository.UsuarioConCredencial> porCorreo = perfil.correoVerificado()
                ? identidades.buscarPorEmail(perfil.correo())
                : Optional.empty();

        UUID usuarioId;
        if (porCorreo.isPresent()) {
            usuarioId = porCorreo.get().id();
        } else {
            usuarioId = Ids.newId();
            identidades.crearUsuario(usuarioId, perfil.correo(), perfil.nombre(), null);
        }

        cuentas.vincular(Ids.newId(), usuarioId, proveedor.codigo(), perfil.idEnProveedor(), perfil.correo());

        // El proveedor ya verifico el correo: pedir un codigo seria exigirle al
        // usuario que demuestre algo ya demostrado.
        if (perfil.correoVerificado()) {
            verificacion.marcarVerificadoPorProveedor(usuarioId, perfil.correo());
        }

        return auth.abrirSesionPara(usuarioId, userAgent, ip);
    }

    private String canjearCodigo(
            ProveedorOAuth proveedor, String codigo, String verificador, String urlRetorno) {

        String cuerpo = parametros(Map.of(
                "client_id", proveedor.clienteId(),
                "client_secret", proveedor.clienteSecreto(),
                "code", codigo,
                "grant_type", "authorization_code",
                "redirect_uri", urlRetorno,
                "code_verifier", verificador));

        JsonNode respuesta = pedir(HttpRequest.newBuilder()
                .uri(URI.create(proveedor.urlToken()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)));

        JsonNode token = respuesta.get("access_token");
        if (token == null || token.asText().isBlank()) {
            throw new DomainException.Conflict(
                    "oauth.token_failed", "No se pudo completar el acceso. Vuelve a intentarlo.");
        }
        return token.asText();
    }

    private PerfilExterno leerPerfil(ProveedorOAuth proveedor, String accessToken) {
        JsonNode datos = pedir(HttpRequest.newBuilder()
                .uri(URI.create(proveedor.urlPerfil()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .GET());

        return switch (proveedor.codigo()) {
                // Google devuelve 'email_verified'. Se respeta: si el propio Google
                // dice que no lo ha comprobado, nosotros tampoco lo damos por bueno.
            case "GOOGLE" -> new PerfilExterno(
                    texto(datos, "sub"),
                    texto(datos, "email"),
                    booleano(datos, "email_verified"),
                    primerNoVacio(texto(datos, "name"), texto(datos, "given_name"), "Usuario"));

            case "DISCORD" -> new PerfilExterno(
                    texto(datos, "id"),
                    texto(datos, "email"),
                    booleano(datos, "verified"),
                    primerNoVacio(texto(datos, "global_name"), texto(datos, "username"), "Usuario"));

            default -> throw new IllegalStateException("Proveedor sin mapeo: " + proveedor.codigo());
        };
    }

    private JsonNode pedir(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> respuesta = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() >= 400) {
                throw new DomainException.Conflict(
                        "oauth.provider_error", "El proveedor rechazo la peticion. Vuelve a intentarlo.");
            }
            return json.readTree(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException.Conflict("oauth.interrupted", "Acceso interrumpido.");
        } catch (IOException e) {
            throw new DomainException.Conflict(
                    "oauth.unreachable", "No hemos podido contactar con el proveedor. Intentalo en un momento.");
        }
    }

    // --- utilidades ----------------------------------------------------------

    private String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.isNull() ? null : valor.asText();
    }

    private boolean booleano(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.asBoolean(false);
    }

    private String primerNoVacio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return "Usuario";
    }

    private String parametros(Map<String, String> valores) {
        StringBuilder sb = new StringBuilder();
        valores.forEach((clave, valor) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(clave, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private String aleatorio(int bytes) {
        byte[] datos = new byte[bytes];
        ALEATORIO.nextBytes(datos);
        return base64Url(datos);
    }

    private String base64Url(byte[] datos) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    }

    private byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * @param state valor anti-CSRF que debe volver intacto
     * @param verificadorPkce secreto que prueba que quien canjea el codigo es quien inicio el flujo
     */
    public record InicioDeFlujo(String urlDestino, String state, String verificadorPkce) {}

    /**
     * @param correoVerificado lo que dice el proveedor. Si es false, aqui tampoco
     *     se da por verificado.
     */
    public record PerfilExterno(String idEnProveedor, String correo, boolean correoVerificado, String nombre) {}
}
