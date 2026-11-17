package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.servers.ApiKeys;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.voting.CastVoteService;
import com.latinosmc.voting.RewardRepository;
import com.latinosmc.voting.VoteTokenService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contrato de /api/v1/plugin/vote-tokens con el plugin (spec 003, CA-8).
 *
 * <p>El controlador no cambia con esta spec: la prueba fija la forma de la
 * respuesta cuando el jugador no puede votar, para que nadie la rompa sin que
 * el plugin desplegado lo note.
 */
class PluginControllerTest {

    private static final UUID SERVIDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CLAVE = "clave";
    private static final String UUID_JUGADOR = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    private ServerRepository servidores;
    private VoteTokenService tokens;
    private CastVoteService votos;
    private PluginController controlador;

    @BeforeEach
    void preparar() {
        servidores = mock(ServerRepository.class);
        tokens = mock(VoteTokenService.class);
        votos = mock(CastVoteService.class);

        controlador = new PluginController(
                servidores, mock(RewardRepository.class), tokens, votos, "https://latinosmc.com");

        when(servidores.servidorPorClaveApi(ApiKeys.hash(CLAVE))).thenReturn(Optional.of(SERVIDOR_ID));
    }

    @Test
    @DisplayName("CA-8: si ya voto aqui, responde el motivo y los segundos sin emitir codigo")
    void emitirToken_yaVotoAqui_devuelveMotivoYSegundosSinCodigo() {
        when(votos.puedeVotar(eq(SERVIDOR_ID), eq(PlayerIdentity.mojangUuid(UUID_JUGADOR))))
                .thenReturn(new CastVoteService.Disponibilidad(false, "YA_VOTO_AQUI", Duration.ofHours(18)));

        var respuesta = controlador.emitirToken(CLAVE, new PluginController.PeticionToken("MOJANG_UUID", UUID_JUGADOR));

        assertThat(respuesta.puedeVotar()).isFalse();
        assertThat(respuesta.motivo()).isEqualTo("YA_VOTO_AQUI");
        assertThat(respuesta.segundosRestantes()).isEqualTo(64_800L);
        assertThat(respuesta.token()).isNull();
        assertThat(respuesta.url()).isNull();
        assertThat(respuesta.vigenciaSegundos()).isZero();
        verify(tokens, never()).emitir(any(), any());
    }
}
