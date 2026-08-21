package com.latinosmc.app.mantenimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.EmailSender;
import com.latinosmc.shared.ports.FileStorage;
import java.time.Clock;
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
 * Purga de servidores archivados hace mucho.
 *
 * <p>Esto destruye datos sin que nadie lo pida, asi que lo que se comprueba aqui
 * no es que purgue: es que <strong>no purgue de mas</strong>, y que lo que
 * conserva lo conserve de verdad. Purgar de menos cuesta unos megabytes; purgar
 * de mas le cuesta a alguien su servidor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurgaDeServidoresArchivadosTest {

    private static final Instant AHORA = Instant.parse("2026-08-21T05:00:00Z");
    private static final UUID SERVIDOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORGANIZACION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DUENO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private ServerRepository servidores;
    @Mock private OrganizationRepository organizaciones;
    @Mock private IdentityRepository identidades;
    @Mock private BillingRepository facturacion;
    @Mock private FileStorage almacen;
    @Mock private EmailSender correo;

    private PurgaDeServidoresArchivados purga;

    @BeforeEach
    void preparar() {
        purga = new PurgaDeServidoresArchivados(
                servidores,
                organizaciones,
                identidades,
                facturacion,
                almacen,
                correo,
                Clock.fixed(AHORA, ZoneOffset.UTC),
                "https://latinosmc.com",
                30,
                7);

        when(organizaciones.administradoresDe(ORGANIZACION)).thenReturn(List.of(DUENO));
        when(identidades.buscarPorId(DUENO))
                .thenReturn(Optional.of(new IdentityRepository.Usuario(
                        DUENO, "dueno@ejemplo.com", "Dueño")));
        when(facturacion.destacadoVigenteDe(any(), any())).thenReturn(Optional.empty());
        when(servidores.purgar(SERVIDOR)).thenReturn(1);
    }

    private ServerRepository.ServidorParaPurgar servidor(int diasArchivado, Instant avisadoEn) {
        return new ServerRepository.ServidorParaPurgar(
                SERVIDOR,
                ORGANIZACION,
                "craft-latino",
                "Craft Latino",
                AHORA.minusSeconds(diasArchivado * 86400L),
                avisadoEn,
                List.of("servidores/x/icon.png", "servidores/x/banner.png"));
    }

    private void conCandidato(ServerRepository.ServidorParaPurgar s) {
        when(servidores.archivadosDesdeHace(anyInt(), anyInt())).thenReturn(List.of(s));
    }

    // --- Lo que NO debe pasar -----------------------------------------------

    @Test
    @DisplayName("a los 29 dias todavia no se purga")
    void todaviaNo() {
        conCandidato(servidor(29, Instant.parse("2026-08-01T00:00:00Z")));

        purga.ronda();

        verify(servidores, never()).purgar(any());
        verify(almacen, never()).borrar(anyString());
    }

    @Test
    @DisplayName("un servidor con espacio destacado pagado no se purga")
    void destacadoPagado() {
        // Es dinero ya cobrado. Purgarlo dejaria la portada vendiendo un hueco
        // vacio y a nosotros debiendo un servicio que no prestamos.
        conCandidato(servidor(60, null));
        when(facturacion.destacadoVigenteDe(eq(SERVIDOR), any()))
                .thenReturn(Optional.of(AHORA.plusSeconds(86400)));

        purga.ronda();

        verify(servidores, never()).purgar(any());
        verify(almacen, never()).borrar(anyString());
    }

    @Test
    @DisplayName("el aviso no se repite")
    void avisoUnico() {
        // Repetirlo cada noche durante una semana es la forma mas rapida de
        // acabar en spam, y entonces tampoco se lee el que importa.
        conCandidato(servidor(25, Instant.parse("2026-08-18T00:00:00Z")));

        purga.ronda();

        verify(servidores, never()).marcarAvisoDePurga(any());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("si la fila no se pudo actualizar, no se borra ningun archivo")
    void sinPurgarNoSeBorra() {
        // El orden importa: primero la fila, despues los archivos. Al reves, un
        // fallo entre medias dejaria un servidor apuntando a imagenes que ya no
        // existen — fichas rotas a la vista de todos.
        conCandidato(servidor(45, Instant.parse("2026-08-01T00:00:00Z")));
        when(servidores.purgar(SERVIDOR)).thenReturn(0);

        purga.ronda();

        verify(almacen, never()).borrar(anyString());
    }

    // --- Lo que si debe pasar ------------------------------------------------

    @Test
    @DisplayName("a los 30 dias se purga: archivos borrados y claves revocadas")
    void purgaCompleta() {
        conCandidato(servidor(30, Instant.parse("2026-08-01T00:00:00Z")));

        purga.ronda();

        verify(servidores).purgar(SERVIDOR);
        verify(servidores).revocarClaves(SERVIDOR);
        verify(almacen).borrar("servidores/x/icon.png");
        verify(almacen).borrar("servidores/x/banner.png");
        verify(servidores).marcarHuerfanoBorrado("servidores/x/icon.png");
    }

    @Test
    @DisplayName("a los 23 dias se avisa, faltando 7")
    void avisaAntes() {
        conCandidato(servidor(23, null));

        purga.ronda();

        verify(servidores).marcarAvisoDePurga(SERVIDOR);
        verify(servidores, never()).purgar(any());

        var cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq("dueno@ejemplo.com"), anyString(), cuerpo.capture(), any());
        assertThat(cuerpo.getValue()).contains("7 dias");
    }

    @Test
    @DisplayName("el correo deja claro que los votos NO se borran")
    void elCorreoTranquiliza() {
        // Es la diferencia entre un aviso y un susto. Quien lea "hemos liberado
        // los datos de tu servidor" va a asumir lo peor si no se le dice.
        conCandidato(servidor(45, Instant.parse("2026-08-01T00:00:00Z")));

        purga.ronda();

        var cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(anyString(), anyString(), cuerpo.capture(), any());
        assertThat(cuerpo.getValue()).contains("NO se han borrado");
    }

    @Test
    @DisplayName("si el almacen falla, la purga sigue y el archivo se reintenta")
    void almacenCaido() {
        conCandidato(servidor(45, Instant.parse("2026-08-01T00:00:00Z")));
        doThrow(new IllegalStateException("R2 caido")).when(almacen).borrar("servidores/x/icon.png");

        purga.ronda();

        // La fila se purga igual, y el archivo que fallo NO se marca como
        // borrado: queda huerfano y la ronda de limpieza vuelve a intentarlo.
        verify(servidores).purgar(SERVIDOR);
        verify(servidores, never()).marcarHuerfanoBorrado("servidores/x/icon.png");
        verify(servidores).marcarHuerfanoBorrado("servidores/x/banner.png");
    }

    @Test
    @DisplayName("si el correo falla, el servidor se purga igual")
    void correoCaido() {
        conCandidato(servidor(45, Instant.parse("2026-08-01T00:00:00Z")));
        doThrow(new IllegalStateException("SMTP caido"))
                .when(correo)
                .enviar(anyString(), anyString(), anyString(), any());

        purga.ronda();

        verify(servidores).purgar(SERVIDOR);
    }

    @Test
    @DisplayName("sin candidatos no se toca nada")
    void sinCandidatos() {
        when(servidores.archivadosDesdeHace(anyInt(), anyInt())).thenReturn(List.of());

        purga.ronda();

        verify(servidores, never()).purgar(any());
        verify(correo, never()).enviar(anyString(), anyString(), anyString(), any());
    }
}
