package com.latinosmc.app.web;

import com.latinosmc.identity.AuthService;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registro, inicio y cierre de sesion. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final OrganizationRepository organizaciones;
    private final com.latinosmc.identity.EmailVerificationService verificacion;
    private final com.latinosmc.identity.VerificationRepository verificacionRepo;
    private final boolean cookieSegura;

    public AuthController(
            AuthService auth,
            OrganizationRepository organizaciones,
            com.latinosmc.identity.EmailVerificationService verificacion,
            com.latinosmc.identity.VerificationRepository verificacionRepo,
            org.springframework.core.env.Environment entorno) {
        this.auth = auth;
        this.organizaciones = organizaciones;
        this.verificacion = verificacion;
        this.verificacionRepo = verificacionRepo;
        // En produccion la cookie solo viaja por HTTPS. En desarrollo no puede
        // ser 'secure' o el navegador la descartaria en http://localhost y no se
        // podria probar nada.
        this.cookieSegura = entorno.matchesProfiles("prod");
    }

    /**
     * Crea la cuenta y su organizacion personal.
     *
     * <p>La organizacion se crea aqui, en la capa de ensamblado, y no dentro del
     * modulo de identidad: los modulos de dominio no se conocen entre si
     * (ADR-0001). Este es el sitio donde componerlos es legitimo.
     *
     * <p>Todo dueno tiene organizacion desde el primer momento aunque trabaje
     * solo. Asi, invitar a alguien mas adelante es anadir un miembro, no migrar
     * la estructura entera.
     */
    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<RespuestaSesion> registrar(
            @RequestBody @Valid PeticionRegistro peticion, HttpServletRequest http) {

        var sesion = auth.registrar(
                peticion.email(),
                peticion.nombre(),
                peticion.password(),
                http.getHeader(HttpHeaders.USER_AGENT),
                IpDelCliente.de(http));

        organizaciones.crearOrganizacionPersonal(sesion.usuarioId(), peticion.nombre());
        verificacion.enviarCodigo(sesion.usuarioId(), peticion.email(), peticion.nombre());

        return conCookie(sesion, HttpStatus.CREATED, peticion.nombre());
    }

    /**
     * Confirma el codigo recibido por correo.
     *
     * <p>La cuenta ya existe y la sesion ya esta abierta: registrarse no queda
     * bloqueado esperando al correo. Lo que exige verificacion son las acciones
     * que importan, como registrar un servidor. Bloquear todo desde el minuto cero
     * hunde la activacion sin ganar nada: la cuenta sin verificar no puede hacer
     * daño.
     */
    @PostMapping("/verificar")
    public ResponseEntity<Void> verificar(
            @UsuarioActual IdentityRepository.Usuario usuario, @RequestBody @Valid PeticionCodigo peticion) {

        verificacion.verificar(usuario.id(), peticion.codigo());
        return ResponseEntity.noContent().build();
    }

    /** Reenvia el codigo. Con espera entre envios para que no sirva de arma. */
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Void> reenviar(@UsuarioActual IdentityRepository.Usuario usuario) {
        verificacion.enviarCodigo(usuario.id(), usuario.email(), usuario.nombre());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaSesion> login(
            @RequestBody @Valid PeticionLogin peticion, HttpServletRequest http) {

        var sesion = auth.iniciarSesion(
                peticion.email(), peticion.password(), http.getHeader(HttpHeaders.USER_AGENT), IpDelCliente.de(http));

        var usuario = auth.usuarioDe(sesion.token()).orElseThrow();
        return conCookie(sesion, HttpStatus.OK, usuario.nombre());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = UsuarioActualResolver.COOKIE_SESION, required = false) String token) {

        auth.cerrarSesion(token);

        // Cookie vacia y caducada: no basta con revocar en servidor, hay que
        // decirle al navegador que la borre.
        ResponseCookie borrado = cookieDeSesion("", Duration.ZERO);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, borrado.toString())
                .build();
    }

    /**
     * Quien soy. Lo usa el frontend para decidir si pintar el panel, el login o el
     * aviso de "verifica tu correo".
     */
    @GetMapping("/yo")
    public ResponseEntity<PerfilActual> yo(@UsuarioActual Optional<IdentityRepository.Usuario> usuario) {
        return usuario.map(u -> ResponseEntity.ok(new PerfilActual(
                        u.id(), u.email(), u.nombre(), verificacionRepo.tieneEmailVerificado(u.id()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Sesiones abiertas, para que el usuario pueda cerrarlas desde su panel. */
    @GetMapping("/sesiones")
    public List<IdentityRepository.SesionActiva> sesiones(@UsuarioActual IdentityRepository.Usuario usuario) {
        return auth.sesionesDe(usuario.id());
    }

    private ResponseEntity<RespuestaSesion> conCookie(
            AuthService.SesionCreada sesion, HttpStatus estado, String nombre) {

        ResponseCookie cookie = cookieDeSesion(sesion.token(), sesion.duracion());
        return ResponseEntity.status(estado)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new RespuestaSesion(sesion.usuarioId(), nombre));
    }

    /**
     * Cookie de sesion.
     *
     * <p>{@code httpOnly} para que JavaScript no pueda leerla: es lo que convierte
     * un XSS en una molestia en lugar de en un robo de cuentas.
     *
     * <p>{@code SameSite=Lax} para que otra web no pueda disparar peticiones
     * autenticadas en nombre del usuario (CSRF). No se usa Strict porque
     * rompe el retorno desde pasarelas de pago y desde el login con Google.
     */
    private ResponseCookie cookieDeSesion(String valor, Duration duracion) {
        return ResponseCookie.from(UsuarioActualResolver.COOKIE_SESION, valor)
                .httpOnly(true)
                .secure(cookieSegura)
                .sameSite("Lax")
                .path("/")
                .maxAge(duracion)
                .build();
    }

    public record PeticionRegistro(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(min = 10, max = 200) String password) {}

    public record PeticionLogin(@NotBlank String email, @NotBlank String password) {}

    /** Se aceptan espacios y guiones: la gente los pega tal cual desde el correo. */
    public record PeticionCodigo(@NotBlank @Size(min = 6, max = 12) String codigo) {}

    public record RespuestaSesion(UUID usuarioId, String nombre) {}

    public record PerfilActual(UUID id, String email, String nombre, boolean emailVerificado) {}
}
