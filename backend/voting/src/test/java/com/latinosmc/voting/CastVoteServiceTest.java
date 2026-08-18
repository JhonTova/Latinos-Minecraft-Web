package com.latinosmc.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.events.EventPublisher;
import com.latinosmc.shared.identity.IdentityHasher;
import com.latinosmc.shared.identity.PlayerIdentity;
import com.latinosmc.shared.ports.ServerDirectory;
import com.latinosmc.shared.ports.TrustEngine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
class CastVoteServiceTest {

    private static final Instant AHORA = Instant.parse("2026-08-03T12:00:00Z");
    private static final UUID SERVIDOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String SLUG = "craftlatino";

    @Mock
    private VoteRepository repositorio;

    @Mock
    private VoteSettingsRepository ajustes;

    @Mock
    private ServerDirectory directorio;

    @Mock
    private TrustEngine antifraude;

    @Mock
    private EventPublisher publicador;

    private CastVoteService servicio;

    private final IdentityHasher hasher = new IdentityHasher("pepper-de-pruebas-suficientemente-largo-de-sobra");
    private final PlayerIdentity steve = PlayerIdentity.offlineName("Steve");

    @BeforeEach
    void preparar() {
        servicio = new CastVoteService(
                repositorio,
                ajustes,
                directorio,
                antifraude,
                hasher,
                publicador,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        // Por defecto, las reglas antiguas: un voto por servidor y sin cupo de IP
        // efectivo. Los tests que prueban las reglas nuevas las activan ellos.
        when(ajustes.actuales()).thenReturn(new VoteSettings(false, 50, Duration.ofHours(24)));
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any())).thenReturn(Optional.empty());
        when(repositorio.identidadesDistintasDesdeIp(any(), any(), any())).thenReturn(0);

        when(directorio.buscarActivoPorSlug(SLUG))
                .thenReturn(Optional.of(
                        new ServerDirectory.ServerInfo(SERVIDOR_ID, SLUG, "CraftLatino", true)));
        when(repositorio.buscarPorClaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(repositorio.ultimoVotoAceptado(any(), any())).thenReturn(Optional.empty());
        when(repositorio.insertar(any(), anyString(), any(), anyString())).thenReturn(true);
        when(antifraude.evaluar(any())).thenReturn(TrustEngine.Veredicto.aceptar(10, List.of()));
    }

    @Test
    @DisplayName("un voto limpio se acepta, genera recompensa y publica el evento")
    void votoAceptado() {
        var resultado = servicio.votar(SLUG, steve, "1.2.3.4", "clave-1");

        assertThat(resultado.voto().estado()).isEqualTo(VoteStatus.ACCEPTED);
        assertThat(resultado.yaExistia()).isFalse();
        verify(repositorio).crearRecompensaPendiente(eq(resultado.voto().id()), eq(SERVIDOR_ID), anyString());
        verify(publicador).publish(any(VoteAcceptedEvent.class));
    }

    /**
     * El reintento es la situacion normal, no la excepcion: redes moviles, plugins
     * que reenvian, usuarios que pulsan dos veces. Debe devolver el mismo voto sin
     * volver a evaluar nada.
     */
    @Test
    @DisplayName("repetir la misma clave devuelve el voto original sin crear otro")
    void idempotencia() {
        var original = new Vote(
                Ids.newId(), SERVIDOR_ID, VoteStatus.ACCEPTED, "OFFLINE_NAME", "Steve", new byte[] {1}, 5, null, AHORA);
        when(repositorio.buscarPorClaveIdempotencia("clave-repetida")).thenReturn(Optional.of(original));

        var resultado = servicio.votar(SLUG, steve, "1.2.3.4", "clave-repetida");

        assertThat(resultado.yaExistia()).isTrue();
        assertThat(resultado.voto().id()).isEqualTo(original.id());
        verify(repositorio, never()).insertar(any(), anyString(), any(), anyString());
        verify(publicador, never()).publish(any());
    }

    @Test
    @DisplayName("la ventana de voto bloquea el segundo voto y dice cuanto falta")
    void ventanaAbierta() {
        when(repositorio.ultimoVotoAceptado(eq(SERVIDOR_ID), any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(20))));

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-2"))
                .isInstanceOf(CastVoteService.VentanaAbiertaException.class)
                .satisfies(e -> assertThat(((CastVoteService.VentanaAbiertaException) e).restante())
                        .isEqualTo(Duration.ofHours(4)));

        verify(repositorio, never()).insertar(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("pasada la ventana se puede volver a votar")
    void ventanaCerrada() {
        when(repositorio.ultimoVotoAceptado(eq(SERVIDOR_ID), any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(25))));

        assertThat(servicio.votar(SLUG, steve, "1.2.3.4", "clave-3").voto().estado())
                .isEqualTo(VoteStatus.ACCEPTED);
    }

    /**
     * La ventana la fija la plataforma y es la misma para todos (ADR-0023).
     *
     * <p>Cuando cada dueño elegia la suya, uno con 12 horas recibia el doble de
     * votos que otro identico con 24 sin que sus jugadores hicieran nada
     * distinto. El ranking dejaba de comparar servidores y pasaba a comparar
     * quien habia rellenado mejor el formulario.
     *
     * <p>Aqui se comprueba que la ventana sale de la configuracion de la
     * plataforma: cambiarla alli cambia el resultado del voto. Si algun dia
     * alguien devolviera una ventana por servidor, este test lo cazaria.
     */
    @Test
    @DisplayName("la ventana sale de la configuracion de la plataforma, no del servidor")
    void laVentanaEsDeLaPlataforma() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(false, 50, Duration.ofHours(48)));
        when(repositorio.ultimoVotoAceptado(eq(SERVIDOR_ID), any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(30))));

        // A las 30 horas ya se podria votar con una ventana de 24; con la de 48
        // que dice la plataforma, todavia no.
        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-plataforma"))
                .isInstanceOf(CastVoteService.VentanaAbiertaException.class)
                .satisfies(e -> assertThat(((CastVoteService.VentanaAbiertaException) e).restante())
                        .isEqualTo(Duration.ofHours(18)));
    }

    @Test
    @DisplayName("bajar la ventana en la configuracion deja votar antes")
    void ventanaMasCortaEnLaPlataforma() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(false, 50, Duration.ofHours(12)));
        when(repositorio.ultimoVotoAceptado(eq(SERVIDOR_ID), any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(13))));

        assertThat(servicio.votar(SLUG, steve, "1.2.3.4", "clave-corta").voto().fueAceptado())
                .isTrue();
    }

    /** Por defecto, 24 horas: es lo que la gente espera de un sitio de votos. */
    @Test
    @DisplayName("la ventana por defecto es de 24 horas")
    void ventanaPorDefecto() {
        assertThat(VoteSettings.porDefecto().ventana()).isEqualTo(Duration.ofHours(24));
    }

    /**
     * Un voto rechazado se guarda —hace falta para investigar el fraude— pero no
     * puede generar recompensa ni mover el ranking. Si generase evento, el fraude
     * seria gratis.
     */
    @Test
    @DisplayName("un voto rechazado no genera recompensa ni evento")
    void votoRechazado() {
        when(antifraude.evaluar(any()))
                .thenReturn(TrustEngine.Veredicto.rechazar(
                        95, "fraud.risk_too_high", List.of(new TrustEngine.Senal("IP_BURST", 50, "demasiados"))));

        var resultado = servicio.votar(SLUG, steve, "1.2.3.4", "clave-4");

        assertThat(resultado.voto().estado()).isEqualTo(VoteStatus.REJECTED);
        verify(repositorio, never()).crearRecompensaPendiente(any(), any(), anyString());
        verify(publicador, never()).publish(any());
    }

    @Test
    @DisplayName("las senales del antifraude siempre se guardan, se acepte o no")
    void senalesPersistidas() {
        var senales = List.of(new TrustEngine.Senal("IDENTITY_NEW", 10, "primer voto"));
        when(antifraude.evaluar(any())).thenReturn(TrustEngine.Veredicto.aceptar(10, senales));

        var resultado = servicio.votar(SLUG, steve, "1.2.3.4", "clave-5");

        verify(repositorio).guardarSenales(eq(resultado.voto().id()), anyList());
    }

    @Test
    @DisplayName("un servidor inexistente no acepta votos")
    void servidorInexistente() {
        when(directorio.buscarActivoPorSlug("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.votar("no-existe", steve, "1.2.3.4", "clave-6"))
                .isInstanceOf(DomainException.NotFound.class)
                .hasFieldOrPropertyWithValue("code", "server.not_found");
    }

    @Test
    @DisplayName("un servidor solo-verificados rechaza identidades de offline-mode")
    void identidadNoAceptada() {
        when(directorio.buscarActivoPorSlug(SLUG))
                .thenReturn(Optional.of(new ServerDirectory.ServerInfo(SERVIDOR_ID, SLUG, "CraftLatino", false)));

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-7"))
                .isInstanceOf(DomainException.Conflict.class)
                .hasFieldOrPropertyWithValue("code", "vote.identity.not_accepted");
    }

    /**
     * Dos peticiones simultaneas con la misma clave: ambas pasan la comprobacion
     * previa, pero solo una gana contra el indice unico. La perdedora debe
     * devolver el voto de la ganadora, no reventar.
     */
    @Test
    @DisplayName("si pierde la carrera de idempotencia, devuelve el voto ganador")
    void carreraDeIdempotencia() {
        var ganador = new Vote(
                Ids.newId(), SERVIDOR_ID, VoteStatus.ACCEPTED, "OFFLINE_NAME", "Steve", new byte[] {1}, 5, null, AHORA);
        when(repositorio.insertar(any(), eq("clave-carrera"), any(), anyString())).thenReturn(false);
        when(repositorio.buscarPorClaveIdempotencia("clave-carrera"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ganador));

        var resultado = servicio.votar(SLUG, steve, "1.2.3.4", "clave-carrera");

        assertThat(resultado.yaExistia()).isTrue();
        assertThat(resultado.voto().id()).isEqualTo(ganador.id());
        verify(publicador, never()).publish(any());
    }

    // --- Reglas globales (V14) -----------------------------------------------

    /**
     * La regla nueva: un jugador vota a UN servidor al dia, no a uno por cada
     * servidor. Aqui ya voto a otro servidor hace una hora.
     */
    @Test
    @DisplayName("con la regla global activa, no se puede votar a un segundo servidor el mismo dia")
    void unVotoAlDiaEnTodaLaPlataforma() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 50, Duration.ofHours(24)));
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(1))));

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-global"))
                .isInstanceOf(CastVoteService.VotoDiarioConsumidoException.class)
                .hasFieldOrPropertyWithValue("code", "vote.daily.used");

        verify(repositorio, never()).insertar(any(), anyString(), any(), anyString());
    }

    /**
     * El mensaje importa tanto como el bloqueo. Si a quien ya voto a otro
     * servidor se le dijera "todavia no puedes votar a ESTE servidor", probaria
     * con otro, y con otro, hasta rendirse pensando que el sitio esta roto.
     */
    @Test
    @DisplayName("el motivo distingue 'ya votaste hoy' de 'ya votaste a este servidor'")
    void mensajesDistintos() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 50, Duration.ofHours(24)));
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(2))));

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-mensaje"))
                .hasMessageContaining("un servidor al dia");
    }

    @Test
    @DisplayName("pasadas 24 horas, el voto diario vuelve a estar disponible")
    void elVotoDiarioSeRenueva() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 50, Duration.ofHours(24)));
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(25))));

        assertThat(servicio.votar(SLUG, steve, "1.2.3.4", "clave-renovada").voto().fueAceptado())
                .isTrue();
    }

    @Test
    @DisplayName("con la regla global desactivada se puede votar a varios servidores")
    void reglaGlobalDesactivada() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(false, 50, Duration.ofHours(24)));
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofMinutes(5))));

        assertThat(servicio.votar(SLUG, steve, "1.2.3.4", "clave-suelta").voto().fueAceptado())
                .isTrue();
    }

    // --- Cupo por IP ---------------------------------------------------------

    @Test
    @DisplayName("con el cupo de IP en 1, el segundo jugador de la misma conexion no puede votar")
    void cupoDeIpAgotado() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 1, Duration.ofHours(24)));
        when(repositorio.identidadesDistintasDesdeIp(any(), any(), any())).thenReturn(1);

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-ip"))
                .isInstanceOf(CastVoteService.CupoDeIpAgotadoException.class)
                .hasFieldOrPropertyWithValue("code", "vote.ip.quota");
    }

    /**
     * El caso que se rompe con una implementacion ingenua.
     *
     * <p>Si el recuento por IP no excluyera a quien pregunta, un jugador que ya
     * voto hoy desde su casa se bloquearia A SI MISMO al volver a intentarlo, y
     * recibiria el mensaje equivocado: le diriamos que su conexion esta saturada
     * cuando el problema es que ya voto.
     *
     * <p>El recuento se hace con exclusion, asi que el jugador llega a la regla
     * que le corresponde.
     */
    @Test
    @DisplayName("quien ya voto no consume su propio cupo de IP")
    void elVotanteNoSeBloqueaASiMismo() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 1, Duration.ofHours(24)));
        // Nadie MAS ha votado desde esa IP: la consulta ya excluye al que pregunta.
        when(repositorio.identidadesDistintasDesdeIp(any(), any(), any())).thenReturn(0);
        when(repositorio.ultimoVotoAceptadoEnCualquierServidor(any()))
                .thenReturn(Optional.of(AHORA.minus(Duration.ofHours(3))));

        assertThatThrownBy(() -> servicio.votar(SLUG, steve, "1.2.3.4", "clave-yo-mismo"))
                .isInstanceOf(CastVoteService.VotoDiarioConsumidoException.class);
    }

    /**
     * Un cupo alto es lo que permite que una red movil con CGNAT —donde miles de
     * clientes comparten IPv4— siga funcionando.
     */
    @Test
    @DisplayName("con el cupo alto, varios jugadores de la misma conexion pueden votar")
    void cupoAltoPermiteConexionCompartida() {
        when(ajustes.actuales()).thenReturn(new VoteSettings(true, 20, Duration.ofHours(24)));
        when(repositorio.identidadesDistintasDesdeIp(any(), any(), any())).thenReturn(12);

        assertThat(servicio.votar(SLUG, steve, "1.2.3.4", "clave-cgnat").voto().fueAceptado())
                .isTrue();
    }

    /**
     * Si la configuracion no se puede leer, se usan los valores permisivos. Un
     * fallo de lectura de configuracion no puede tumbar la funcion principal del
     * producto.
     */
    @Test
    @DisplayName("los valores por defecto son permisivos, no restrictivos")
    void valoresPorDefectoPermisivos() {
        var porDefecto = VoteSettings.porDefecto();

        assertThat(porDefecto.unVotoAlDia()).isFalse();
        assertThat(porDefecto.maxJugadoresPorIp()).isGreaterThan(10);
    }
}
