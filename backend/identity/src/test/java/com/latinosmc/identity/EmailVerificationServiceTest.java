package com.latinosmc.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.identity.VerificationRepository.CodigoVigente;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Los tres limites de la verificacion de correo: 15 minutos de vigencia,
 * 5 intentos y 60 segundos entre envios.
 *
 * <p>Ninguna prueba lee las constantes del servicio (D9): un test que lee la
 * constante no se entera de que alguien cambio su valor. Aqui se mide el
 * comportamiento con un reloj fijo, en el limite exacto y un paso antes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailVerificationServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T10:00:00Z");
    private static final UUID USUARIO = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID CODIGO_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final String EMAIL = "alex@ejemplo.com";
    private static final Pattern SEIS_DIGITOS = Pattern.compile("\\b(\\d{6})\\b");

    @Mock
    private VerificationRepository repositorio;

    @Mock
    private EmailSender correo;

    private EmailVerificationService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EmailVerificationService(repositorio, correo, Clock.fixed(AHORA, ZoneOffset.UTC));
        when(repositorio.ultimoEnvio(USUARIO)).thenReturn(Optional.empty());
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void hayCodigoVigente(String codigoEnClaro, int intentos) {
        when(repositorio.codigoVigente(USUARIO, AHORA))
                .thenReturn(Optional.of(new CodigoVigente(CODIGO_ID, sha256(codigoEnClaro), EMAIL, intentos)));
    }

    private void verificarQueNoHuboEscrituras() {
        verify(repositorio, never()).sumarIntento(any());
        verify(repositorio, never()).marcarConsumido(any(), any());
        verify(repositorio, never()).marcarEmailVerificado(any(), any(), any());
    }

    private void verificarQueNoSeEnvioNada() {
        verify(repositorio, never()).invalidarAnteriores(any(), any());
        verify(repositorio, never()).guardar(any(), any(), any(), any(), any());
        verify(correo, never()).enviar(any(), any(), any(), any());
    }

    @Nested
    @DisplayName("Vigencia de 15 minutos (CA-5)")
    class Vigencia {

        @Test
        @DisplayName("guarda el codigo con caducidad de ahora mas 15 minutos exactos")
        void guardaConCaducidadDeQuinceMinutos() {
            servicio.enviarCodigo(USUARIO, EMAIL, "Alex");

            ArgumentCaptor<Instant> caduca = ArgumentCaptor.forClass(Instant.class);
            verify(repositorio).guardar(any(), eq(USUARIO), anyString(), eq(EMAIL), caduca.capture());
            assertThat(Duration.between(AHORA, caduca.getValue())).isEqualTo(Duration.ofMinutes(15));
            assertThat(caduca.getValue()).isEqualTo(Instant.parse("2026-11-17T10:15:00Z"));
        }

        @Test
        @DisplayName("busca el codigo vigente con el instante del reloj inyectado")
        void buscaVigenteConElRelojInyectado() {
            hayCodigoVigente("123456", 0);

            servicio.verificar(USUARIO, "123456");

            ArgumentCaptor<Instant> instante = ArgumentCaptor.forClass(Instant.class);
            verify(repositorio).codigoVigente(eq(USUARIO), instante.capture());
            assertThat(instante.getValue()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("sin codigo vigente responde expirado y no escribe nada")
        void sinVigenteRespondeExpiradoSinEscribir() {
            when(repositorio.codigoVigente(USUARIO, AHORA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.verificar(USUARIO, "123456"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .extracting(e -> ((DomainException) e).code())
                    .isEqualTo("verification.expired");

            verificarQueNoHuboEscrituras();
        }
    }

    @Nested
    @DisplayName("Espera de 60 segundos entre envios (CA-7)")
    class EsperaEntreEnvios {

        private void rechazaSiElUltimoEnvioFueHace(Duration hace) {
            when(repositorio.ultimoEnvio(USUARIO)).thenReturn(Optional.of(AHORA.minus(hace)));

            assertThatThrownBy(() -> servicio.enviarCodigo(USUARIO, EMAIL, "Alex"))
                    .isInstanceOf(DomainException.Conflict.class)
                    .extracting(e -> ((DomainException) e).code())
                    .isEqualTo("verification.too_soon");

            verificarQueNoSeEnvioNada();
        }

        private void enviaSiElUltimoEnvioFueHace(Duration hace) {
            when(repositorio.ultimoEnvio(USUARIO)).thenReturn(Optional.of(AHORA.minus(hace)));

            servicio.enviarCodigo(USUARIO, EMAIL, "Alex");

            verify(repositorio).invalidarAnteriores(USUARIO, AHORA);
            verify(repositorio).guardar(any(), eq(USUARIO), anyString(), eq(EMAIL), any());
            verify(correo).enviar(eq(EMAIL), anyString(), anyString(), isNull());
        }

        @Test
        @DisplayName("hace 59 segundos rechaza")
        void a59sRechaza() {
            rechazaSiElUltimoEnvioFueHace(Duration.ofSeconds(59));
        }

        @Test
        @DisplayName("hace 0 segundos rechaza")
        void a0sRechaza() {
            rechazaSiElUltimoEnvioFueHace(Duration.ZERO);
        }

        @Test
        @DisplayName("hace exactamente 60 segundos envia")
        void a60sExactosEnvia() {
            enviaSiElUltimoEnvioFueHace(Duration.ofSeconds(60));
        }

        @Test
        @DisplayName("hace mas de 60 segundos envia")
        void aMasDe60sEnvia() {
            enviaSiElUltimoEnvioFueHace(Duration.ofSeconds(61));
        }

        @Test
        @DisplayName("sin envio previo envia")
        void sinEnvioPrevioEnvia() {
            when(repositorio.ultimoEnvio(USUARIO)).thenReturn(Optional.empty());

            servicio.enviarCodigo(USUARIO, EMAIL, "Alex");

            verify(repositorio).invalidarAnteriores(USUARIO, AHORA);
            verify(repositorio).guardar(any(), eq(USUARIO), anyString(), eq(EMAIL), any());
            verify(correo).enviar(eq(EMAIL), anyString(), anyString(), isNull());
        }
    }

    @Nested
    @DisplayName("Codigo incorrecto (CA-1, CA-6)")
    class CodigoIncorrecto {

        @ParameterizedTest(name = "con {0} fallos previos quedan {1} intentos")
        @CsvSource({"0,4", "1,3", "2,2", "3,1", "4,0"})
        @DisplayName("suma un intento y dice cuantos quedan, como entero")
        void restaN(int fallosPrevios, int restantes) {
            hayCodigoVigente("123456", fallosPrevios);

            assertThatThrownBy(() -> servicio.verificar(USUARIO, "000000"))
                    .isInstanceOf(EmailVerificationService.IntentoRechazadoException.class)
                    .isInstanceOf(DomainException.Conflict.class)
                    .satisfies(e -> {
                        var rechazo = (EmailVerificationService.IntentoRechazadoException) e;
                        assertThat(rechazo.code()).isEqualTo("verification.invalid_code");
                        assertThat(rechazo.intentosRestantes()).isEqualTo(restantes);
                    });

            verify(repositorio).sumarIntento(CODIGO_ID);
            verify(repositorio, never()).marcarConsumido(any(), any());
            verify(repositorio, never()).marcarEmailVerificado(any(), any(), any());
        }

        @Test
        @DisplayName("un codigo vacio o nulo tambien cuenta como incorrecto")
        void nuloCuentaComoIncorrecto() {
            hayCodigoVigente("123456", 1);

            assertThatThrownBy(() -> servicio.verificar(USUARIO, null))
                    .isInstanceOf(EmailVerificationService.IntentoRechazadoException.class)
                    .satisfies(e -> assertThat(((EmailVerificationService.IntentoRechazadoException) e)
                                    .intentosRestantes())
                            .isEqualTo(3));

            verify(repositorio).sumarIntento(CODIGO_ID);
        }
    }

    @Nested
    @DisplayName("Intentos agotados (CA-2, CA-6)")
    class IntentosAgotados {

        @ParameterizedTest(name = "con {0} fallos previos ni siquiera el correcto vale")
        @ValueSource(ints = {5, 6})
        void conCincoFallosNiSiquieraElCorrectoVale(int fallosPrevios) {
            // El mismo hash que el codigo enviado: es el caso en que un atacante
            // ya acerto, pero despues de agotar los intentos.
            hayCodigoVigente("123456", fallosPrevios);

            assertThatThrownBy(() -> servicio.verificar(USUARIO, "123456"))
                    .isInstanceOf(EmailVerificationService.IntentoRechazadoException.class)
                    .satisfies(e -> {
                        var rechazo = (EmailVerificationService.IntentoRechazadoException) e;
                        assertThat(rechazo.code()).isEqualTo("verification.too_many_attempts");
                        assertThat(rechazo.intentosRestantes()).isZero();
                    });

            verificarQueNoHuboEscrituras();
        }

        @Test
        @DisplayName("con cinco fallos un codigo incorrecto tampoco suma otro intento")
        void conCincoFallosElIncorrectoNoSuma() {
            hayCodigoVigente("123456", 5);

            assertThatThrownBy(() -> servicio.verificar(USUARIO, "000000"))
                    .isInstanceOf(EmailVerificationService.IntentoRechazadoException.class)
                    .satisfies(e -> assertThat(((DomainException) e).code())
                            .isEqualTo("verification.too_many_attempts"));

            verificarQueNoHuboEscrituras();
        }
    }

    @Nested
    @DisplayName("Con cuatro fallos previos (CA-6)")
    class ConCuatroFallos {

        @Test
        @DisplayName("el quinto intento todavia vale: el codigo correcto verifica")
        void elCorrectoTodaviaVerifica() {
            hayCodigoVigente("654321", 4);

            servicio.verificar(USUARIO, "654321");

            verify(repositorio).marcarConsumido(CODIGO_ID, AHORA);
            verify(repositorio).marcarEmailVerificado(USUARIO, EMAIL, AHORA);
            verify(repositorio, never()).sumarIntento(any());
        }
    }

    @Nested
    @DisplayName("Camino feliz y normalizacion (CA-8)")
    class CaminoFeliz {

        @Test
        @DisplayName("con espacios en medio verifica")
        void conEspacios() {
            hayCodigoVigente("123456", 0);

            servicio.verificar(USUARIO, "123 456");

            verify(repositorio).marcarConsumido(CODIGO_ID, AHORA);
            verify(repositorio).marcarEmailVerificado(USUARIO, EMAIL, AHORA);
        }

        @Test
        @DisplayName("con guion en medio verifica")
        void conGuion() {
            hayCodigoVigente("123456", 0);

            servicio.verificar(USUARIO, "123-456");

            verify(repositorio).marcarConsumido(CODIGO_ID, AHORA);
            verify(repositorio).marcarEmailVerificado(USUARIO, EMAIL, AHORA);
        }

        @Test
        @DisplayName("marca verificado con el email del codigo y el instante del reloj")
        void marcaConEmailDelCodigoYRelojInyectado() {
            when(repositorio.codigoVigente(USUARIO, AHORA))
                    .thenReturn(Optional.of(new CodigoVigente(
                            CODIGO_ID, sha256("000123"), "email.del.codigo@ejemplo.com", 2)));

            servicio.verificar(USUARIO, "000123");

            verify(repositorio).marcarConsumido(CODIGO_ID, AHORA);
            verify(repositorio).marcarEmailVerificado(USUARIO, "email.del.codigo@ejemplo.com", AHORA);
        }

        @Test
        @DisplayName("el codigo tiene 6 digitos (tambien con cero inicial), va en el correo y solo se guarda su hash")
        void codigoDeSeisDigitosEnElCorreoYSoloSuHashGuardado() {
            int envios = 3000;
            for (int i = 0; i < envios; i++) {
                servicio.enviarCodigo(USUARIO, EMAIL, "Alex");
            }

            ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);
            verify(repositorio, times(envios)).guardar(any(), eq(USUARIO), hashes.capture(), eq(EMAIL), any());
            ArgumentCaptor<String> asuntos = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> cuerpos = ArgumentCaptor.forClass(String.class);
            verify(correo, times(envios)).enviar(eq(EMAIL), asuntos.capture(), cuerpos.capture(), isNull());

            boolean algunoEmpiezaPorCero = false;
            for (int i = 0; i < envios; i++) {
                Matcher enCuerpo = SEIS_DIGITOS.matcher(cuerpos.getAllValues().get(i));
                assertThat(enCuerpo.find()).as("el cuerpo contiene un codigo de 6 digitos").isTrue();
                String codigo = enCuerpo.group(1);

                assertThat(asuntos.getAllValues().get(i)).contains(codigo);
                assertThat(hashes.getAllValues().get(i))
                        .as("se guarda el SHA-256 del codigo")
                        .isEqualTo(sha256(codigo))
                        .isNotEqualTo(codigo)
                        .matches("[0-9a-f]{64}");
                algunoEmpiezaPorCero |= codigo.startsWith("0");
            }
            assertThat(algunoEmpiezaPorCero)
                    .as("con 3000 codigos alguno empieza por cero y sigue teniendo 6 digitos")
                    .isTrue();
        }
    }
}
