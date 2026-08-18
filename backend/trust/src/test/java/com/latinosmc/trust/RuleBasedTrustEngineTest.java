package com.latinosmc.trust;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.identity.IdentityHasher;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.shared.ports.TrustEngine;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleBasedTrustEngineTest {

    private static final Instant AHORA = Instant.parse("2026-08-03T12:00:00Z");
    private static final UUID SERVIDOR = UUID.randomUUID();

    @Mock
    private TrustQueries consultas;

    private RuleBasedTrustEngine motor;

    private final IdentityHasher hasher = new IdentityHasher("pepper-de-pruebas-suficientemente-largo-de-sobra");

    @BeforeEach
    void preparar() {
        motor = new RuleBasedTrustEngine(consultas, hasher);
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(1);
        when(consultas.votosPorIp(any(), any())).thenReturn(1);
        when(consultas.historialIdentidad(any())).thenReturn(5);
    }

    private TrustEngine.Veredicto evaluar(PlayerIdentity identidad) {
        return motor.evaluar(new TrustEngine.Peticion(
                SERVIDOR, identidad, hasher.hashIdentidad(identidad), "1.2.3.4", AHORA));
    }

    @Test
    @DisplayName("un votante habitual y verificado pasa con riesgo bajo")
    void votanteLimpio() {
        var veredicto = evaluar(PlayerIdentity.mojangUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5"));

        assertThat(veredicto.aceptado()).isTrue();
        assertThat(veredicto.puntuacionRiesgo()).isLessThan(30);
    }

    /**
     * No se rechaza por ser offline-mode: eso excluiria a buena parte del mercado
     * hispanohablante. Solo parte con desventaja.
     */
    @Test
    @DisplayName("una identidad de offline-mode suma riesgo pero no se rechaza sola")
    void identidadNoVerificable() {
        var veredicto = evaluar(PlayerIdentity.offlineName("Steve"));

        assertThat(veredicto.aceptado()).isTrue();
        assertThat(veredicto.senales()).anyMatch(s -> s.regla().equals("IDENTITY_UNVERIFIABLE"));
    }

    @Test
    @DisplayName("muchas identidades desde una misma IP disparan el rechazo")
    void granjaDeCuentas() {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(30);
        when(consultas.historialIdentidad(any())).thenReturn(0);

        var veredicto = evaluar(PlayerIdentity.offlineName("Bot1"));

        assertThat(veredicto.aceptado()).isFalse();
        assertThat(veredicto.motivoRechazo()).isEqualTo("fraud.risk_too_high");
        assertThat(veredicto.senales()).anyMatch(s -> s.regla().equals("IP_MULTI_IDENTITY"));
    }

    @Test
    @DisplayName("una rafaga de votos desde una IP suma riesgo")
    void rafagaDeVotos() {
        when(consultas.votosPorIp(any(), any())).thenReturn(20);

        var veredicto = evaluar(PlayerIdentity.offlineName("Steve"));

        assertThat(veredicto.senales()).anyMatch(s -> s.regla().equals("IP_BURST"));
    }

    /**
     * Un hogar con dos o tres hermanos jugando es lo normal. Si eso bastara para
     * bloquear, la plataforma castigaria a usuarios legitimos todos los dias.
     */
    @Test
    @DisplayName("dos o tres jugadores en la misma casa no se bloquean")
    void hogarCompartidoNoSeBloquea() {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(3);

        assertThat(evaluar(PlayerIdentity.offlineName("Hermano")).aceptado()).isTrue();
    }

    /**
     * El historial es caro de fabricar. Compensar con el reduce los falsos
     * positivos sin abrir la puerta a cuentas nuevas en masa.
     */
    @Test
    @DisplayName("un historial largo compensa otras senales")
    void historialCompensa() {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(5);

        when(consultas.historialIdentidad(any())).thenReturn(0);
        int riesgoNovato = evaluar(PlayerIdentity.offlineName("Nuevo")).puntuacionRiesgo();

        when(consultas.historialIdentidad(any())).thenReturn(50);
        int riesgoVeterano = evaluar(PlayerIdentity.offlineName("Veterano")).puntuacionRiesgo();

        assertThat(riesgoVeterano).isLessThan(riesgoNovato);
    }

    @Test
    @DisplayName("toda decision viene acompanada de sus senales")
    void decisionExplicable() {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(30);
        when(consultas.historialIdentidad(any())).thenReturn(0);

        var veredicto = evaluar(PlayerIdentity.offlineName("Bot1"));

        // Sin esto, suspender a un servidor de pago seria indefendible ante una
        // reclamacion.
        assertThat(veredicto.senales()).isNotEmpty();
        assertThat(veredicto.senales()).allSatisfy(s -> {
            assertThat(s.regla()).isNotBlank();
            assertThat(s.detalle()).isNotBlank();
        });
    }

    @Test
    @DisplayName("el riesgo siempre queda entre 0 y 100")
    void riesgoAcotado() {
        when(consultas.identidadesDistintasPorIp(any(), any())).thenReturn(999);
        when(consultas.votosPorIp(any(), any())).thenReturn(999);
        when(consultas.historialIdentidad(any())).thenReturn(0);

        assertThat(evaluar(PlayerIdentity.offlineName("Bot1")).puntuacionRiesgo()).isBetween(0, 100);
    }
}
