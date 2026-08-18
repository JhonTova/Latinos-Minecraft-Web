package com.latinosmc.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.identity.IdentityHasher;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Cuentas y sesiones.
 *
 * <p>Casi todo lo que se comprueba aqui es invisible desde la pantalla: que el
 * token de sesion nunca se guarde tal cual, que la IP no se escriba en claro y
 * que fallar el login cueste lo mismo exista o no el correo. Son propiedades que
 * no se rompen con un error visible —la aplicacion sigue funcionando igual de
 * bien— y por eso tienen que estar escritas en un test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-15T09:00:00Z");
    private static final UUID USUARIO = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private IdentityRepository repositorio;

    private AuthService servicio;

    private final Passwords passwords = new Passwords();
    private final IdentityHasher hasher = new IdentityHasher("pepper-de-pruebas-suficientemente-largo-de-sobra");

    @BeforeEach
    void preparar() {
        servicio = new AuthService(repositorio, passwords, hasher, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(repositorio.existeEmail(anyString())).thenReturn(false);
    }

    // --- Registro ------------------------------------------------------------

    @Nested
    @DisplayName("Al registrarse")
    class Registro {

        /**
         * Si el correo se guardara tal cual se escribe, "Ana@Gmail.com" y
         * "ana@gmail.com" serian dos cuentas distintas para la misma persona, y
         * la segunda se quedaria fuera de su propio historial.
         */
        @Test
        @DisplayName("el correo se guarda normalizado, no como se escribio")
        void emailNormalizado() {
            servicio.registrar("  Ana@GMAIL.com ", "Ana", "contrasena-larga", "Firefox", "1.2.3.4");

            verify(repositorio).crearUsuario(any(), eq("ana@gmail.com"), eq("Ana"), anyString());
        }

        @Test
        @DisplayName("un correo ya registrado se rechaza diciendolo")
        void emailRepetido() {
            when(repositorio.existeEmail("ana@gmail.com")).thenReturn(true);

            assertThatThrownBy(() ->
                            servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "Firefox", "1.2.3.4"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasFieldOrPropertyWithValue("code", "email.taken");
        }

        @Test
        @DisplayName("un correo sin forma de correo no crea nada")
        void emailInvalido() {
            assertThatThrownBy(() -> servicio.registrar("nada", "Ana", "contrasena-larga", "Firefox", "1.2.3.4"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasFieldOrPropertyWithValue("code", "email.invalid");

            verify(repositorio, never()).crearUsuario(any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("sin nombre no se crea la cuenta")
        void sinNombre() {
            assertThatThrownBy(() ->
                            servicio.registrar("ana@gmail.com", "   ", "contrasena-larga", "Firefox", "1.2.3.4"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasFieldOrPropertyWithValue("code", "name.missing");
        }

        /**
         * La comprobacion de longitud vive en {@link Passwords} y salta antes de
         * escribir nada. Si saltara despues, quedaria una cuenta a medias.
         */
        @Test
        @DisplayName("una contrasena corta no llega a crear el usuario")
        void passwordCorta() {
            assertThatThrownBy(() -> servicio.registrar("ana@gmail.com", "Ana", "corta", "Firefox", "1.2.3.4"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .hasFieldOrPropertyWithValue("code", "password.too_short");

            verify(repositorio, never()).crearUsuario(any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("la contrasena nunca se guarda en claro")
        void passwordHasheada() {
            servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "Firefox", "1.2.3.4");

            var hash = ArgumentCaptor.forClass(String.class);
            verify(repositorio).crearUsuario(any(), anyString(), anyString(), hash.capture());

            assertThat(hash.getValue()).doesNotContain("contrasena-larga").startsWith("$argon2");
        }
    }

    // --- Inicio de sesion ----------------------------------------------------

    @Nested
    @DisplayName("Al iniciar sesion")
    class Login {

        /**
         * El correo desconocido y la contrasena incorrecta tienen que ser
         * indistinguibles desde fuera. Si no, el formulario de acceso se
         * convierte en un buscador de cuentas registradas.
         */
        @Test
        @DisplayName("el error es el mismo exista o no el correo")
        void mismoErrorSiempre() {
            when(repositorio.buscarPorEmail("nadie@gmail.com")).thenReturn(Optional.empty());
            when(repositorio.buscarPorEmail("ana@gmail.com"))
                    .thenReturn(Optional.of(new IdentityRepository.UsuarioConCredencial(
                            USUARIO, "ana@gmail.com", "Ana", passwords.hash("contrasena-buena"))));

            var sinCuenta = catchCode(() -> servicio.iniciarSesion("nadie@gmail.com", "loquesea", "F", "1.2.3.4"));
            var malaPassword = catchCode(() -> servicio.iniciarSesion("ana@gmail.com", "la-que-no-es", "F", "1.2.3.4"));

            assertThat(sinCuenta).isEqualTo(malaPassword).isEqualTo("auth.invalid_credentials");
        }

        /**
         * La otra mitad de lo mismo. Aunque el mensaje sea identico, salir antes
         * de verificar cuando el correo no existe haria la respuesta
         * perceptiblemente mas rapida, y cronometrarla revelaria igual que hay
         * cuenta. Por eso se verifica siempre, contra un hash senuelo.
         */
        @Test
        @DisplayName("con un correo desconocido tambien se verifica una contrasena")
        void verificaContraElSenuelo() {
            var espia = new PasswordsEspia();
            var conEspia = new AuthService(repositorio, espia, hasher, Clock.fixed(AHORA, ZoneOffset.UTC));
            espia.verificaciones = 0;
            when(repositorio.buscarPorEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> conEspia.iniciarSesion("nadie@gmail.com", "loquesea", "F", "1.2.3.4"))
                    .isInstanceOf(AuthService.CredencialesInvalidas.class);

            assertThat(espia.verificaciones).isEqualTo(1);
        }

        @Test
        @DisplayName("la contrasena correcta abre sesion")
        void loginCorrecto() {
            when(repositorio.buscarPorEmail("ana@gmail.com"))
                    .thenReturn(Optional.of(new IdentityRepository.UsuarioConCredencial(
                            USUARIO, "ana@gmail.com", "Ana", passwords.hash("contrasena-buena"))));

            var sesion = servicio.iniciarSesion(" ANA@gmail.com ", "contrasena-buena", "Firefox", "1.2.3.4");

            assertThat(sesion.usuarioId()).isEqualTo(USUARIO);
            assertThat(sesion.token()).isNotBlank();
        }

        /**
         * Quien entro solo con Google no tiene contrasena. Ese caso devuelve
         * false en vez de reventar: una excepcion distinta aqui volveria a
         * distinguir cuentas que existen de cuentas que no.
         */
        @Test
        @DisplayName("una cuenta sin contrasena (solo OAuth) no entra por el formulario")
        void cuentaSoloOauth() {
            when(repositorio.buscarPorEmail("ana@gmail.com"))
                    .thenReturn(Optional.of(
                            new IdentityRepository.UsuarioConCredencial(USUARIO, "ana@gmail.com", "Ana", null)));

            assertThatThrownBy(() -> servicio.iniciarSesion("ana@gmail.com", "loquesea", "F", "1.2.3.4"))
                    .isInstanceOf(AuthService.CredencialesInvalidas.class);
        }
    }

    // --- Sesiones ------------------------------------------------------------

    @Nested
    @DisplayName("La sesion")
    class Sesiones {

        /**
         * Lo que se guarda es el hash del token, no el token. Con la tabla de
         * sesiones a la vista —una copia de seguridad, una consulta de soporte—
         * nadie puede suplantar a nadie.
         */
        @Test
        @DisplayName("guarda el hash del token, nunca el token")
        void tokenNoSeGuarda() {
            var sesion = servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "Firefox", "1.2.3.4");

            var guardado = ArgumentCaptor.forClass(String.class);
            verify(repositorio).crearSesion(any(), any(), guardado.capture(), anyString(), any(), any());

            assertThat(guardado.getValue()).isNotEqualTo(sesion.token()).hasSize(64).matches("[0-9a-f]+");
        }

        /** La misma IP en claro en la tabla convertiria un volcado en un mapa de quien es quien. */
        @Test
        @DisplayName("la IP se guarda hasheada, no en claro")
        void ipHasheada() {
            servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "Firefox", "1.2.3.4");

            var ip = ArgumentCaptor.forClass(byte[].class);
            verify(repositorio).crearSesion(any(), any(), anyString(), anyString(), ip.capture(), any());

            assertThat(new String(ip.getValue(), StandardCharsets.UTF_8)).doesNotContain("1.2.3.4");
            assertThat(ip.getValue()).isEqualTo(hasher.hashIp("1.2.3.4"));
        }

        @Test
        @DisplayName("caduca a los 30 dias")
        void caducidad() {
            servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "Firefox", "1.2.3.4");

            verify(repositorio)
                    .crearSesion(any(), any(), anyString(), anyString(), any(), eq(AHORA.plus(Duration.ofDays(30))));
        }

        @Test
        @DisplayName("dos sesiones nunca comparten token")
        void tokensDistintos() {
            var primera = servicio.registrar("ana@gmail.com", "Ana", "contrasena-larga", "F", "1.2.3.4");
            var segunda = servicio.registrar("bea@gmail.com", "Bea", "contrasena-larga", "F", "1.2.3.4");

            assertThat(primera.token()).isNotEqualTo(segunda.token());
        }

        @Test
        @DisplayName("un token vacio no llega ni a consultarse")
        void tokenVacio() {
            assertThat(servicio.usuarioDe(null)).isEmpty();
            assertThat(servicio.usuarioDe("   ")).isEmpty();

            verify(repositorio, never()).usuarioDeSesion(anyString(), any());
        }

        @Test
        @DisplayName("un token valido resuelve el usuario y refresca el ultimo uso")
        void tokenValido() {
            var usuario = new IdentityRepository.Usuario(USUARIO, "ana@gmail.com", "Ana");
            when(repositorio.usuarioDeSesion(anyString(), eq(AHORA))).thenReturn(Optional.of(usuario));

            assertThat(servicio.usuarioDe("un-token")).contains(usuario);
            verify(repositorio).tocarSesion(anyString(), eq(AHORA));
        }

        @Test
        @DisplayName("un token desconocido no resuelve nada ni refresca nada")
        void tokenDesconocido() {
            when(repositorio.usuarioDeSesion(anyString(), any())).thenReturn(Optional.empty());

            assertThat(servicio.usuarioDe("un-token")).isEmpty();
            verify(repositorio, never()).tocarSesion(anyString(), any());
        }

        /** Cerrar sesion busca por hash: el token en claro no aparece en ninguna consulta. */
        @Test
        @DisplayName("cerrar sesion revoca por hash, no por token")
        void cierreDeSesion() {
            servicio.cerrarSesion("un-token");

            var hash = ArgumentCaptor.forClass(String.class);
            verify(repositorio).revocarSesion(hash.capture(), eq(AHORA), eq("logout"));

            assertThat(hash.getValue()).isNotEqualTo("un-token").hasSize(64);
        }

        @Test
        @DisplayName("cerrar una sesion vacia no toca la base de datos")
        void cierreSinToken() {
            servicio.cerrarSesion(null);
            servicio.cerrarSesion("  ");

            verify(repositorio, never()).revocarSesion(anyString(), any(), anyString());
        }

        /**
         * Google y Discord confirman la identidad por su cuenta, asi que ahi no
         * hay contrasena que verificar. La sesion que se abre tiene que ser
         * exactamente igual de solida que la del formulario.
         */
        @Test
        @DisplayName("la sesion de un acceso con Google se guarda igual que las demas")
        void sesionDeOauth() {
            var sesion = servicio.abrirSesionPara(USUARIO, "Chrome", "5.6.7.8");

            assertThat(sesion.usuarioId()).isEqualTo(USUARIO);
            var hash = ArgumentCaptor.forClass(String.class);
            verify(repositorio)
                    .crearSesion(any(), eq(USUARIO), hash.capture(), eq("Chrome"), eq(hasher.hashIp("5.6.7.8")), eq(AHORA.plus(Duration.ofDays(30))));
            assertThat(hash.getValue()).isNotEqualTo(sesion.token());
        }
    }

    private static String catchCode(Runnable accion) {
        try {
            accion.run();
            throw new AssertionError("Se esperaba una excepcion y no hubo ninguna");
        } catch (DomainException e) {
            return e.code();
        }
    }

    /** Cuenta cuantas veces se verifica una contrasena, que es lo que el test observa. */
    private static class PasswordsEspia extends Passwords {
        int verificaciones;

        @Override
        public boolean coincide(String enClaro, String hashGuardado) {
            verificaciones++;
            return super.coincide(enClaro, hashGuardado);
        }
    }
}
