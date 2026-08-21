package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.RegisterServerService;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Baja de un servidor.
 *
 * <p>Lo que se prueba aqui es que la confirmacion por nombre <strong>no se puede
 * saltar llamando a la API directamente</strong>. Una confirmacion que solo vive
 * en el navegador es teatro: cualquiera abre las herramientas de desarrollo y
 * manda la peticion a mano. Si estos tests pasaran sin la comprobacion en el
 * backend, la proteccion no existiria.
 */
class BajaDeServidorTest {

    private static final UUID SERVIDOR = UUID.randomUUID();
    private static final UUID ORGANIZACION = UUID.randomUUID();

    private static final IdentityRepository.Usuario DUENO =
            new IdentityRepository.Usuario(UUID.randomUUID(), "dueno@ejemplo.com", "Dueño");

    private static final IdentityRepository.Usuario AJENO =
            new IdentityRepository.Usuario(UUID.randomUUID(), "otro@ejemplo.com", "Otro");

    private ServerRepository servidores;
    private OrganizationRepository organizaciones;
    private BillingRepository facturacion;
    private ServerController controlador;

    @BeforeEach
    void preparar() {
        servidores = mock(ServerRepository.class);
        organizaciones = mock(OrganizationRepository.class);
        facturacion = mock(BillingRepository.class);

        controlador = new ServerController(
                mock(RegisterServerService.class),
                organizaciones,
                servidores,
                mock(com.latinosmc.identity.VerificationRepository.class),
                mock(com.latinosmc.shared.ports.FileStorage.class),
                facturacion);

        when(servidores.detalle(SERVIDOR)).thenReturn(Optional.of(detalle()));
        when(organizaciones.puedeGestionar(DUENO.id(), ORGANIZACION)).thenReturn(true);
        when(organizaciones.puedeGestionar(AJENO.id(), ORGANIZACION)).thenReturn(false);
        when(facturacion.destacadoVigenteDe(any(), any())).thenReturn(Optional.empty());
    }

    private ServerRepository.DetalleServidor detalle() {
        return new ServerRepository.DetalleServidor(
                SERVIDOR,
                ORGANIZACION,
                "craft-latino",
                "Craft Latino",
                "Un servidor",
                "ACTIVE",
                "craftlatino.com",
                25565,
                true,
                null,
                null,
                true,
                "1.8",
                "1.21",
                "España",
                "DEFECTO",
                // Imagenes automaticas (ADR-0027): irrelevantes para dar de baja.
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                false);
    }

    @Test
    @DisplayName("con el nombre correcto se da de baja y se revocan las claves")
    void bajaCorrecta() {
        controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("Craft Latino"));

        verify(servidores).darDeBaja(SERVIDOR, ORGANIZACION);

        // Sin revocar la clave, el plugin de un servidor archivado seguiria
        // autenticandose para siempre: el dueño lo dio de baja en la web pero el
        // JAR sigue instalado en su maquina, latiendo cada minuto.
        verify(servidores).revocarClaves(SERVIDOR);
    }

    /**
     * La confirmacion tiene que absorber lo que escribe un humano: mayusculas
     * distintas y espacios de los extremos. Existe para obligar a MIRAR que se
     * borra, no para jugar a las adivinanzas con el teclado.
     */
    @Test
    @DisplayName("acepta el nombre con otra caja y con espacios de sobra")
    void nombreTolerante() {
        controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("  craft latino  "));
        verify(servidores).darDeBaja(SERVIDOR, ORGANIZACION);
    }

    @Test
    @DisplayName("con el nombre equivocado NO se borra nada")
    void nombreEquivocado() {
        assertThatThrownBy(() ->
                        controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("Craft Latin")))
                .isInstanceOf(DomainException.Conflict.class);

        // Se comprueba el efecto, no solo la excepcion: una validacion puesta
        // DESPUES de la escritura lanzaria igual y el servidor ya estaria de baja.
        verify(servidores, never()).darDeBaja(any(), any());
    }

    @Test
    @DisplayName("el nombre vacio no cuela")
    void nombreVacio() {
        assertThatThrownBy(() -> controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("")))
                .isInstanceOf(DomainException.Conflict.class);
        verify(servidores, never()).darDeBaja(any(), any());
    }

    /**
     * Control de acceso a nivel de objeto: lo que OWASP llama BOLA y sigue siendo
     * el fallo numero uno en APIs multi-tenant. El mismo error que "no existe",
     * para que nadie averigue que servidores hay probando identificadores.
     */
    @Test
    @DisplayName("nadie puede dar de baja el servidor de otro, aunque acierte el nombre")
    void servidorAjeno() {
        assertThatThrownBy(() ->
                        controlador.darDeBaja(AJENO, SERVIDOR, new ServerController.PeticionBaja("Craft Latino")))
                .isInstanceOf(DomainException.NotFound.class);

        verify(servidores, never()).darDeBaja(any(), any());
    }

    /**
     * Un espacio destacado en curso es dinero ya pagado. Darlo de baja lo tiraria
     * a la basura sin avisar, y esa llamada la acabamos atendiendo nosotros.
     */
    @Test
    @DisplayName("no se da de baja un servidor con un destacado pagado en curso")
    void conDestacadoActivo() {
        when(facturacion.destacadoVigenteDe(any(), any()))
                .thenReturn(Optional.of(Instant.now().plusSeconds(86_400 * 5)));

        assertThatThrownBy(() ->
                        controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("Craft Latino")))
                .isInstanceOf(DomainException.Conflict.class)
                .hasMessageContaining("destacado");

        verify(servidores, never()).darDeBaja(any(), any());
    }

    @Test
    @DisplayName("un servidor que no existe da el mismo error que uno ajeno")
    void servidorInexistente() {
        UUID fantasma = UUID.randomUUID();
        when(servidores.detalle(fantasma)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controlador.darDeBaja(DUENO, fantasma, new ServerController.PeticionBaja("Lo que sea")))
                .isInstanceOf(DomainException.NotFound.class);
    }

    @Test
    @DisplayName("la respuesta confirma la baja")
    void respuesta() {
        assertThat(controlador.darDeBaja(DUENO, SERVIDOR, new ServerController.PeticionBaja("Craft Latino")))
                .containsEntry("dadoDeBaja", true);
    }
}
