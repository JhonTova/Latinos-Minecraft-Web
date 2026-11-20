package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.Ids;
import com.latinosmc.voting.CastVoteService;
import com.latinosmc.voting.Vote;
import com.latinosmc.voting.VoteStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Lo que ve el cliente HTTP cuando reintenta un voto (spec 005). El servicio se
 * simula: aqui solo se fija la conversion de {@code Duration} a segundos, el
 * codigo 200 del reintento y que el JSON conserve su forma.
 */
class VoteControllerTest {

    private static final Instant AHORA = Instant.parse("2026-08-03T12:00:00Z");

    private CastVoteService servicio;
    private VoteController controlador;
    private MockHttpServletRequest peticionHttp;

    @BeforeEach
    void preparar() {
        servicio = mock(CastVoteService.class);
        controlador = new VoteController(servicio);
        peticionHttp = new MockHttpServletRequest();
        peticionHttp.setRemoteAddr("203.0.113.7");
    }

    private Vote voto(VoteStatus estado) {
        return new Vote(
                Ids.newId(),
                Ids.newId(),
                estado,
                "OFFLINE_NAME",
                "Steve",
                new byte[] {1},
                5,
                estado == VoteStatus.REJECTED ? "fraud.risk_too_high" : null,
                AHORA.minus(Duration.ofHours(6)));
    }

    private VoteController.PeticionVoto peticion() {
        return new VoteController.PeticionVoto("OFFLINE_NAME", "Steve");
    }

    @Test
    @DisplayName("CA-1: el reintento de un voto aceptado responde 200 con los segundos que faltan")
    void reintentoAceptadoDevuelve200YSegundos() {
        var aceptado = voto(VoteStatus.ACCEPTED);
        when(servicio.votar(anyString(), any(), anyString(), anyString()))
                .thenReturn(new CastVoteService.Resultado(aceptado, true, Duration.ofHours(18)));

        var respuesta = controlador.votar("craftlatino", peticion(), "clave-repetida", peticionHttp);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().votoId()).isEqualTo(aceptado.id());
        assertThat(respuesta.getBody().estado()).isEqualTo("ACCEPTED");
        assertThat(respuesta.getBody().yaExistia()).isTrue();
        assertThat(respuesta.getBody().segundosHastaProximoVoto()).isEqualTo(64800L);
    }

    @Test
    @DisplayName("CA-8: un voto nuevo responde 201 con la ventana entera en segundos")
    void votoNuevoDevuelve201YLaVentana() {
        var aceptado = voto(VoteStatus.ACCEPTED);
        when(servicio.votar(anyString(), any(), anyString(), anyString()))
                .thenReturn(new CastVoteService.Resultado(aceptado, false, Duration.ofHours(24)));

        var respuesta = controlador.votar("craftlatino", peticion(), "clave-nueva", peticionHttp);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().votoId()).isEqualTo(aceptado.id());
        assertThat(respuesta.getBody().yaExistia()).isFalse();
        assertThat(respuesta.getBody().segundosHastaProximoVoto()).isEqualTo(86400L);
    }

    @Test
    @DisplayName("CA-5: el reintento de un voto rechazado responde 200 sin segundos")
    void reintentoRechazadoDevuelveNull() {
        var rechazado = voto(VoteStatus.REJECTED);
        when(servicio.votar(anyString(), any(), anyString(), anyString()))
                .thenReturn(new CastVoteService.Resultado(rechazado, true, null));

        var respuesta = controlador.votar("craftlatino", peticion(), "clave-repetida", peticionHttp);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().estado()).isEqualTo("REJECTED");
        assertThat(respuesta.getBody().yaExistia()).isTrue();
        assertThat(respuesta.getBody().segundosHastaProximoVoto()).isNull();
    }
}
