package com.latinosmc.identity;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.identity.IdentityHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, inicio de sesion y gestion de sesiones. */
@Service
public class AuthService {

    /** Duracion de una sesion. Larga para no molestar; revocable en cualquier momento. */
    private static final Duration DURACION_SESION = Duration.ofDays(30);

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    /**
     * Hash de una contrasena que no existe, para gastar el mismo tiempo cuando el
     * email no esta registrado. Ver {@link #iniciarSesion}.
     */
    private final String hashSenuelo;

    private final IdentityRepository repositorio;
    private final Passwords passwords;
    private final IdentityHasher hasher;
    private final Clock reloj;

    public AuthService(
            IdentityRepository repositorio, Passwords passwords, IdentityHasher hasher, Clock reloj) {
        this.repositorio = repositorio;
        this.passwords = passwords;
        this.hasher = hasher;
        this.reloj = reloj;
        this.hashSenuelo = passwords.hash("senuelo-para-igualar-tiempos-de-respuesta");
    }

    @Transactional
    public SesionCreada registrar(String email, String nombre, String password, String userAgent, String ip) {
        String normalizado = normalizar(email);

        if (!normalizado.contains("@") || normalizado.length() < 5) {
            throw new DomainException.Conflict("email.invalid", "El correo no es valido.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new DomainException.Conflict("name.missing", "El nombre es obligatorio.");
        }
        if (repositorio.existeEmail(normalizado)) {
            // Se revela que el email existe solo en el registro, no en el login:
            // aqui es inevitable (hay que impedir duplicados) y el usuario
            // necesita saberlo para poder recuperar su cuenta.
            throw new DomainException.Conflict("email.taken", "Ya existe una cuenta con ese correo.");
        }

        UUID usuarioId = Ids.newId();
        repositorio.crearUsuario(usuarioId, normalizado, nombre.trim(), passwords.hash(password));

        return abrirSesion(usuarioId, userAgent, ip);
    }

    /**
     * Inicia sesion.
     *
     * <p>El mensaje de error es el mismo tanto si el email no existe como si la
     * contrasena es incorrecta. Y cuando el email no existe se verifica igualmente
     * contra un hash senuelo: sin eso, la respuesta seria perceptiblemente mas
     * rapida y cualquiera podria averiguar que correos estan registrados
     * cronometrando peticiones.
     */
    @Transactional
    public SesionCreada iniciarSesion(String email, String password, String userAgent, String ip) {
        Optional<IdentityRepository.UsuarioConCredencial> usuario =
                repositorio.buscarPorEmail(normalizar(email));

        if (usuario.isEmpty()) {
            passwords.coincide(password, hashSenuelo);
            throw new CredencialesInvalidas();
        }
        if (!passwords.coincide(password, usuario.get().passwordHash())) {
            throw new CredencialesInvalidas();
        }

        return abrirSesion(usuario.get().id(), userAgent, ip);
    }

    /** Resuelve el usuario de una sesion. Devuelve vacio si no vale o caduco. */
    @Transactional
    public Optional<IdentityRepository.Usuario> usuarioDe(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = hashDeToken(token);
        Optional<IdentityRepository.Usuario> usuario = repositorio.usuarioDeSesion(hash, Instant.now(reloj));
        usuario.ifPresent(u -> repositorio.tocarSesion(hash, Instant.now(reloj)));
        return usuario;
    }

    @Transactional
    public void cerrarSesion(String token) {
        if (token != null && !token.isBlank()) {
            repositorio.revocarSesion(hashDeToken(token), Instant.now(reloj), "logout");
        }
    }

    @Transactional(readOnly = true)
    public List<IdentityRepository.SesionActiva> sesionesDe(UUID usuarioId) {
        return repositorio.sesionesDe(usuarioId, Instant.now(reloj));
    }

    /**
     * Abre sesion para un usuario ya identificado por otra via.
     *
     * <p>Lo usa el flujo de Google y Discord: alli la identidad la confirma el
     * proveedor, asi que no hay contrasena que verificar.
     */
    @Transactional
    public SesionCreada abrirSesionPara(UUID usuarioId, String userAgent, String ip) {
        return abrirSesion(usuarioId, userAgent, ip);
    }

    private SesionCreada abrirSesion(UUID usuarioId, String userAgent, String ip) {
        String token = generarToken();
        Instant ahora = Instant.now(reloj);

        repositorio.crearSesion(
                Ids.newId(),
                usuarioId,
                hashDeToken(token),
                userAgent,
                hasher.hashIp(ip),
                ahora.plus(DURACION_SESION));

        return new SesionCreada(usuarioId, token, DURACION_SESION);
    }

    private String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 y no Argon2: el token son 256 bits aleatorios, no una contrasena
     * humana. Ademas se verifica en cada peticion, asi que un hash lento aqui
     * penalizaria toda la aplicacion sin aportar nada.
     */
    private String hashDeToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param token valor en claro de la sesion; viaja en una cookie httpOnly
     */
    public record SesionCreada(UUID usuarioId, String token, Duration duracion) {}

    /** Mismo error para email inexistente y contrasena incorrecta, a proposito. */
    public static class CredencialesInvalidas extends DomainException {
        public CredencialesInvalidas() {
            super("auth.invalid_credentials", "Correo o contrasena incorrectos.");
        }
    }
}
