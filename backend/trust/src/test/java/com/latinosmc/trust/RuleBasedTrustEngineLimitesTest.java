package com.latinosmc.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.identity.IdentityHasher;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.shared.ports.TrustEngine;
import com.latinosmc.shared.ports.TrustEngine.Senal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Fija las cifras exactas del motor (spec 001): el limite de cada regla, cuanto
 * suma, el recorte a 0-100 y el umbral de rechazo. {@link RuleBasedTrustEngineTest}
 * documenta la intencion de cada regla; esta clase hace fallar cualquier cambio
 * de numero.
 *
 * <p>La "base" es una identidad verificable (Mojang), 1 identidad por IP, 1 voto
 * por IP e historial de 5: riesgo 0 y sin senales.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleBasedTrustEngineLimitesTest {

    private static final Instant AHORA = Instant.parse("2026-08-03T12:00:00Z");
    private static final UUID SERVIDOR = UUID.randomUUID();
    private static final String IP = "1.2.3.4";

    private static final PlayerIdentity MOJANG = PlayerIdentity.mojangUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final PlayerIdentity OFFLINE = PlayerIdentity.offlineName("Steve");

    @Mock
    private TrustQueries consultas;

    private final IdentityHasher hasher = new IdentityHasher("pepper-de-pruebas-suficientemente-largo-de-sobra");

    private RuleBasedTrustEngine motor;

    @BeforeEach
    void preparar() {
        motor = new RuleBasedTrustEngine(consultas, hasher);
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(1);
        when(consultas.votosPorIp(any(), any())).thenReturn(1);
        when(consultas.historialIdentidad(any())).thenReturn(5);
    }

    private TrustEngine.Veredicto evaluar(PlayerIdentity identidad, int identidades, int votos, int historial) {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(identidades);
        when(consultas.votosPorIp(any(), any())).thenReturn(votos);
        when(consultas.historialIdentidad(any())).thenReturn(historial);
        return evaluar(identidad);
    }

    private TrustEngine.Veredicto evaluar(PlayerIdentity identidad) {
        return motor.evaluar(new TrustEngine.Peticion(
                SERVIDOR, identidad, hasher.hashIdentidad(identidad), IP, AHORA));
    }

    // ---- CA-1 -----------------------------------------------------------------

    @Test
    @DisplayName("la peticion base se acepta con riesgo 0 y sin senales")
    void laPeticionBaseSeAceptaConRiesgoCeroYSinSenales() {
        var veredicto = evaluar(MOJANG);

        assertThat(veredicto.aceptado()).isTrue();
        assertThat(veredicto.puntuacionRiesgo()).isZero();
        assertThat(veredicto.motivoRechazo()).isNull();
        assertThat(veredicto.senales()).isEmpty();
    }

    // ---- CA-2 -----------------------------------------------------------------

    @Test
    @DisplayName("una identidad no verificable suma exactamente 25")
    void unaIdentidadNoVerificableSumaExactamente25() {
        var veredicto = evaluar(OFFLINE);

        assertThat(veredicto.aceptado()).isTrue();
        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(25);
        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(tuple("IDENTITY_UNVERIFIABLE", 25));
    }

    @Test
    @DisplayName("una identidad verificable no suma por identidad")
    void unaIdentidadVerificableNoSumaPorIdentidad() {
        var veredicto = evaluar(MOJANG);

        assertThat(veredicto.puntuacionRiesgo()).isZero();
        assertThat(veredicto.senales()).extracting(Senal::regla).doesNotContain("IDENTITY_UNVERIFIABLE");
    }

    // ---- CA-3 -----------------------------------------------------------------

    @ParameterizedTest(name = "{0} identidades en la IP aportan {1}")
    @CsvSource({"3,0", "4,15", "5,30", "6,45", "7,45", "30,45"})
    @DisplayName("las identidades por IP aportan su cifra exacta")
    void lasIdentidadesPorIpAportanSuCifraExacta(int identidades, int aporte) {
        var veredicto = evaluar(MOJANG, identidades, 1, 5);

        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(aporte);
        assertThat(veredicto.aceptado()).isTrue();
        if (aporte == 0) {
            assertThat(veredicto.senales()).isEmpty();
        } else {
            assertThat(veredicto.senales())
                    .extracting(Senal::regla, Senal::delta)
                    .containsExactly(tuple("IP_MULTI_IDENTITY", aporte));
            assertThat(veredicto.senales().get(0).detalle()).startsWith(identidades + " ");
        }
    }

    // ---- CA-4 -----------------------------------------------------------------

    @ParameterizedTest(name = "{0} votos en 1 h aportan {1}")
    @CsvSource({"6,0", "7,10", "10,40", "11,40", "50,40"})
    @DisplayName("los votos en una hora aportan su cifra exacta")
    void losVotosEnUnaHoraAportanSuCifraExacta(int votos, int aporte) {
        var veredicto = evaluar(MOJANG, 1, votos, 5);

        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(aporte);
        assertThat(veredicto.aceptado()).isTrue();
        if (aporte == 0) {
            assertThat(veredicto.senales()).isEmpty();
        } else {
            assertThat(veredicto.senales())
                    .extracting(Senal::regla, Senal::delta)
                    .containsExactly(tuple("IP_BURST", aporte));
            assertThat(veredicto.senales().get(0).detalle()).startsWith(votos + " ");
        }
    }

    // ---- CA-5 (sobre offline: parte de 25, asi el recorte no oculta la cifra) ----

    @ParameterizedTest(name = "historial {0} deja el riesgo en {1}")
    @CsvSource({
        "0,35,IDENTITY_NEW,10",
        "1,25,NINGUNA,0",
        "9,25,NINGUNA,0",
        "10,10,IDENTITY_TRUSTED,-15",
        "50,10,IDENTITY_TRUSTED,-15"
    })
    @DisplayName("el historial aporta su cifra exacta")
    void elHistorialAportaSuCifraExacta(int historial, int riesgo, String regla, int delta) {
        var veredicto = evaluar(OFFLINE, 1, 1, historial);

        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(riesgo);
        if (regla.equals("NINGUNA")) {
            assertThat(veredicto.senales())
                    .extracting(Senal::regla, Senal::delta)
                    .containsExactly(tuple("IDENTITY_UNVERIFIABLE", 25));
        } else {
            assertThat(veredicto.senales())
                    .extracting(Senal::regla, Senal::delta)
                    .containsExactly(tuple("IDENTITY_UNVERIFIABLE", 25), tuple(regla, delta));
            if (regla.equals("IDENTITY_TRUSTED")) {
                assertThat(veredicto.senales().get(1).detalle()).startsWith(historial + " ");
            }
        }
    }

    // ---- CA-6 -----------------------------------------------------------------

    @Test
    @DisplayName("el riesgo es la suma exacta de las aportaciones")
    void elRiesgoEsLaSumaExactaDeLasAportaciones() {
        var veredicto = evaluar(OFFLINE, 4, 7, 0);

        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(
                        tuple("IDENTITY_UNVERIFIABLE", 25),
                        tuple("IP_MULTI_IDENTITY", 15),
                        tuple("IP_BURST", 10),
                        tuple("IDENTITY_NEW", 10));
        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(60);
        assertThat(veredicto.senales().stream().mapToInt(Senal::delta).sum()).isEqualTo(60);
    }

    // ---- CA-7 -----------------------------------------------------------------

    @Test
    @DisplayName("una suma negativa se recorta a cero")
    void unaSumaNegativaSeRecortaACero() {
        var veredicto = evaluar(MOJANG, 1, 1, 10);

        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(tuple("IDENTITY_TRUSTED", -15));
        assertThat(veredicto.puntuacionRiesgo()).isZero();
        assertThat(veredicto.aceptado()).isTrue();
    }

    @Test
    @DisplayName("una suma mayor de cien se recorta a cien")
    void unaSumaMayorDeCienSeRecortaACien() {
        var veredicto = evaluar(OFFLINE, 30, 50, 0);

        assertThat(veredicto.senales().stream().mapToInt(Senal::delta).sum()).isEqualTo(120);
        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(100);
        assertThat(veredicto.aceptado()).isFalse();
    }

    // ---- CA-8 -----------------------------------------------------------------

    @Test
    @DisplayName("un riesgo de 55 se acepta sin motivo")
    void unRiesgoDe55SeAceptaSinMotivo() {
        var veredicto = evaluar(OFFLINE, 5, 1, 5);

        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(55);
        assertThat(veredicto.aceptado()).isTrue();
        assertThat(veredicto.motivoRechazo()).isNull();
    }

    @Test
    @DisplayName("un riesgo de exactamente 60 se rechaza y conserva sus senales")
    void unRiesgoDeExactamente60SeRechazaConSusSenales() {
        var veredicto = evaluar(OFFLINE, 4, 7, 0);

        assertThat(veredicto.puntuacionRiesgo()).isEqualTo(60);
        assertThat(veredicto.aceptado()).isFalse();
        assertThat(veredicto.motivoRechazo()).isEqualTo("fraud.risk_too_high");
        assertThat(veredicto.senales())
                .extracting(Senal::regla)
                .containsExactly("IDENTITY_UNVERIFIABLE", "IP_MULTI_IDENTITY", "IP_BURST", "IDENTITY_NEW");
    }

    // ---- CA-9 -----------------------------------------------------------------
    // Cada stub exacto solo casa si el motor consulta con esa ventana y ese hash;
    // si no casa, la consulta devuelve el valor base y la senal no aparece.

    private TrustEngine.Veredicto evaluarConHashDistinto() {
        return motor.evaluar(new TrustEngine.Peticion(SERVIDOR, MOJANG, new byte[] {1, 2, 3}, IP, AHORA));
    }

    @Test
    @DisplayName("consulta las identidades con el hash de la IP y desde momento menos 24 h")
    void consultaLasIdentidadesConElHashDeLaIpYLaVentanaDe24Horas() {
        when(consultas.identidadesDistintasPorIp(eq(hasher.hashIp(IP)), eq(AHORA.minus(Duration.ofHours(24)))))
                .thenReturn(4);

        var veredicto = evaluarConHashDistinto();

        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(tuple("IP_MULTI_IDENTITY", 15));
    }

    @Test
    @DisplayName("consulta los votos con el hash de la IP y desde momento menos 1 h")
    void consultaLosVotosConElHashDeLaIpYLaVentanaDeUnaHora() {
        when(consultas.votosPorIp(eq(hasher.hashIp(IP)), eq(AHORA.minus(Duration.ofHours(1)))))
                .thenReturn(7);

        var veredicto = evaluarConHashDistinto();

        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(tuple("IP_BURST", 10));
    }

    @Test
    @DisplayName("consulta el historial con el hash de identidad de la peticion")
    void consultaElHistorialConElHashDeIdentidadDeLaPeticion() {
        when(consultas.historialIdentidad(eq(new byte[] {1, 2, 3}))).thenReturn(0);

        var veredicto = evaluarConHashDistinto();

        assertThat(veredicto.senales())
                .extracting(Senal::regla, Senal::delta)
                .containsExactly(tuple("IDENTITY_NEW", 10));
    }
}
