package com.latinosmc.app.mantenimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.EmailSender;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Retirada de servidores que dejaron de dar señales.
 *
 * <p>Esto quita cosas del directorio sin que nadie lo pida, asi que lo que se
 * comprueba aqui no es que retire: es que <strong>no retire de mas</strong>. Un
 * servidor vivo que desaparece del ranking por un fallo de esta ronda es un
 * cliente perdido que ademas se entera por sus jugadores.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LimpiezaDeServidoresInactivosTest {

    private static final Instant AHORA = Instant.parse("2026-08-15T04:30:00Z");
    private static final UUID SERVIDOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORGANIZACION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DUENO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ServerRepository servidores;

    @Mock
    private OrganizationRepository organizaciones;

    @Mock
    private IdentityRepository identidades;

    @Mock
    private BillingRepository facturacion;

    @Mock
    private EmailSender correo;

    private LimpiezaDeServidoresInactivos limpieza;

    @BeforeEach
    void preparar() {
        limpieza = new LimpiezaDeServidoresInactivos(
                servidores,
                organizaciones,
                identidades,
                facturacion,
                correo,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                "https://latinosmc.com",
                7,
                15);

        when(organizaciones.administradoresDe(ORGANIZACION)).thenReturn(List.of(DUENO));
        when(identidades.buscarPorId(DUENO))
                .thenReturn(Optional.of(new IdentityRepository.Usuario(DUENO, "duena@correo.com", "Ana")));
        when(facturacion.destacadoVigenteDe(any(), any())).thenReturn(Optional.empty());
        when(servidores.archivarPorInactividad(any())).thenReturn(1);
    }

    private ServerRepository.ServidorSinSenal servidor(long diasSinSenal, Instant avisadoEn) {
        return new ServerRepository.ServidorSinSenal(
                SERVIDOR,
                ORGANIZACION,
                "craft-latino",
                "Craft Latino",
                AHORA.minus(Duration.ofDays(diasSinSenal)),
                avisadoEn);
    }

    private void hay(ServerRepository.ServidorSinSenal... encontrados) {
        when(servidores.sinSenalDelPlugin(anyInt(), anyInt())).thenReturn(List.of(encontrados));
    }

    // --- Retirada ------------------------------------------------------------

    @Test
    @DisplayName("a los 15 dias sin señales se retira del directorio")
    void seRetiraALos15Dias() {
        hay(servidor(15, null));

        limpieza.ronda();

        verify(servidores).archivarPorInactividad(SERVIDOR);
    }

    /**
     * Retirar sin avisar es como cerrarle la tienda a alguien y que se entere por
     * un cliente. El aviso llega antes y dice cuanto queda.
     */
    @Test
    @DisplayName("a los 7 dias se avisa, no se retira")
    void alos7DiasSoloSeAvisa() {
        hay(servidor(7, null));

        limpieza.ronda();

        verify(servidores, never()).archivarPorInactividad(any());
        verify(servidores).marcarAvisoDeInactividad(SERVIDOR);

        var asunto = ArgumentCaptor.forClass(String.class);
        var cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq("duena@correo.com"), asunto.capture(), cuerpo.capture(), any());

        assertThat(asunto.getValue()).contains("Craft Latino");
        assertThat(cuerpo.getValue()).contains("8 dias", "panel");
    }

    /**
     * El aviso se manda una vez. Repetirlo cada noche durante ocho dias es la
     * forma mas rapida de que el dueño mande LatinosMC a la carpeta de spam, y
     * entonces tampoco vera el que importa.
     */
    @Test
    @DisplayName("no se avisa dos veces al mismo servidor")
    void elAvisoNoSeRepite() {
        hay(servidor(9, AHORA.minus(Duration.ofDays(2))));

        limpieza.ronda();

        verify(correo, never()).enviar(anyString(), anyString(), anyString(), any());
        verify(servidores, never()).marcarAvisoDeInactividad(any());
    }

    @Test
    @DisplayName("al retirarlo se avisa de que vuelve solo")
    void elCorreoDeRetiradaExplicaComoVolver() {
        hay(servidor(20, AHORA.minus(Duration.ofDays(13))));

        limpieza.ronda();

        var cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq("duena@correo.com"), anyString(), cuerpo.capture(), any());

        assertThat(cuerpo.getValue())
                .as("quien lee esto tiene que saber que no ha perdido nada")
                .contains("No hemos borrado nada")
                .contains("vuelve solo");
    }

    // --- Lo que NO se toca ---------------------------------------------------

    /**
     * Un espacio destacado es dinero ya cobrado. Retirar ese servidor dejaria la
     * portada vendiendo un hueco vacio y a nosotros debiendo un servicio que no
     * prestamos.
     */
    @Test
    @DisplayName("un servidor con espacio destacado pagado no se retira")
    void elDestacadoPagadoSeRespeta() {
        hay(servidor(30, null));
        when(facturacion.destacadoVigenteDe(eq(SERVIDOR), any()))
                .thenReturn(Optional.of(AHORA.plus(Duration.ofDays(5))));

        limpieza.ronda();

        verify(servidores, never()).archivarPorInactividad(any());
    }

    @Test
    @DisplayName("con 14 dias todavia no se retira")
    void justoAntesDelPlazo() {
        hay(servidor(14, AHORA.minus(Duration.ofDays(7))));

        limpieza.ronda();

        verify(servidores, never()).archivarPorInactividad(any());
    }

    @Test
    @DisplayName("sin candidatos no se hace nada")
    void rondaVacia() {
        hay();

        limpieza.ronda();

        verify(servidores, never()).archivarPorInactividad(any());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), any());
    }

    /**
     * Un proveedor de correo caido no puede dejar la ronda a medias: el servidor
     * ya se retiro, y abandonar aqui dejaria los siguientes sin revisar hasta
     * mañana.
     */
    @Test
    @DisplayName("si el correo falla, la ronda sigue")
    void elCorreoNoTumbaLaRonda() {
        hay(servidor(20, null));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP caido"))
                .when(correo)
                .enviar(anyString(), anyString(), anyString(), any());

        limpieza.ronda();

        verify(servidores).archivarPorInactividad(SERVIDOR);
    }

    @Test
    @DisplayName("un servidor sin dueño localizable se retira igual")
    void sinDuenoLocalizable() {
        hay(servidor(20, null));
        when(organizaciones.administradoresDe(ORGANIZACION)).thenReturn(List.of());

        limpieza.ronda();

        verify(servidores).archivarPorInactividad(SERVIDOR);
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), any());
    }
}
