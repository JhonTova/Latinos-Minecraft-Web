package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.identity.PlatformAdminRepository;
import com.latinosmc.shared.DomainException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * Control de acceso a la administracion de la plataforma.
 *
 * <p>El fallo realista aqui no es que la comprobacion de permiso este mal
 * escrita: es que alguien añada un endpoint nuevo dentro de seis meses y se
 * olvide de ponerla. Por eso el test central recorre los endpoints
 * <strong>por reflexion</strong> en lugar de listarlos a mano. Un metodo nuevo
 * sin guardian rompe el test el dia que se escribe, no el dia que alguien
 * descubre que puede cambiar los precios.
 */
class AdminControllerTest {

    private static final IdentityRepository.Usuario CUALQUIERA =
            new IdentityRepository.Usuario(UUID.randomUUID(), "alguien@ejemplo.com", "Alguien");

    private static final IdentityRepository.Usuario JEFA =
            new IdentityRepository.Usuario(UUID.randomUUID(), "jefa@ejemplo.com", "Jefa");

    private PlatformAdminRepository administradores;
    private BillingRepository facturacion;
    private com.latinosmc.billing.PatrocinadoresRepository patrocinadores;
    private com.latinosmc.voting.VoteSettingsRepository reglasDeVoto;
    private com.latinosmc.organizations.SiteSettingsRepository sitio;
    private AdminController controlador;

    @BeforeEach
    void preparar() {
        administradores = mock(PlatformAdminRepository.class);
        facturacion = mock(BillingRepository.class);
        patrocinadores = mock(com.latinosmc.billing.PatrocinadoresRepository.class);
        reglasDeVoto = mock(com.latinosmc.voting.VoteSettingsRepository.class);
        sitio = mock(com.latinosmc.organizations.SiteSettingsRepository.class);

        controlador = new AdminController(
                administradores,
                facturacion,
                patrocinadores,
                reglasDeVoto,
                sitio,
                mock(com.latinosmc.servers.ProcesadorDeImagenes.class),
                mock(com.latinosmc.shared.ports.FileStorage.class));

        when(administradores.esAdministrador(CUALQUIERA.id())).thenReturn(false);
        when(administradores.esAdministrador(JEFA.id())).thenReturn(true);
    }

    /**
     * El test que importa: <strong>todos</strong> los endpoints, sin listarlos.
     *
     * <p>Solo se excluye {@code /soy}, que es deliberadamente abierto —el panel lo
     * usa para decidir si pinta el enlace de administracion— y que no da acceso a
     * nada por si mismo.
     */
    @Test
    @DisplayName("ningun endpoint de administracion responde a quien no es administrador")
    void todosLosEndpointsExigenPermiso() throws Exception {
        List<Method> endpoints = endpointsProtegidos();

        // Si esto falla, es que la deteccion por reflexion dejo de encontrar los
        // metodos y el test estaria pasando sin comprobar nada.
        assertThat(endpoints).as("endpoints detectados").hasSizeGreaterThanOrEqualTo(4);

        for (Method endpoint : endpoints) {
            assertThatThrownBy(() -> invocar(endpoint, CUALQUIERA))
                    .as("el endpoint %s deberia exigir permiso de administrador", endpoint.getName())
                    .isInstanceOf(DomainException.Forbidden.class);
        }
    }

    @Test
    @DisplayName("un administrador si puede leer los espacios")
    void administradorLee() {
        when(facturacion.slots()).thenReturn(List.of());
        assertThat(controlador.slots(JEFA)).isEmpty();
    }

    /**
     * Lo que un cambio de precio no puede hacer es tocar nada si quien lo pide no
     * tiene permiso. Se comprueba el efecto, no solo la excepcion: un guardian
     * puesto DESPUES de la escritura lanzaria igual y habria cambiado el precio.
     */
    @Test
    @DisplayName("un intento denegado no llega a escribir en la base de datos")
    void intentoDenegadoNoEscribe() {
        var peticion = new AdminController.PeticionSlot(100, 7, 4, true);

        assertThatThrownBy(() -> controlador.actualizarSlot(CUALQUIERA, 1, peticion))
                .isInstanceOf(DomainException.Forbidden.class);

        verify(facturacion, never()).actualizarSlot(anyInt(), anyLong(), anyInt(), anyInt(), anyBoolean());
    }

    /** Cambiar un precio es una accion sobre dinero: tiene que dejar rastro. */
    @Test
    @DisplayName("un cambio de precio queda registrado en la auditoria")
    void cambioDePrecioSeAudita() {
        var slot = new BillingRepository.Slot(1, 2500, "EUR", 7, 4, true);
        when(facturacion.slot(1)).thenReturn(Optional.of(slot));

        controlador.actualizarSlot(JEFA, 1, new AdminController.PeticionSlot(3000, 7, 4, true));

        verify(facturacion).actualizarSlot(1, 3000L, 7, 4, true);
        verify(administradores)
                .registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("/soy responde sin exigir permiso, para que el panel sepa si pintar el enlace")
    void consultarSiSoyAdministrador() {
        assertThat(controlador.soyAdministrador(CUALQUIERA)).containsEntry("administrador", false);
        assertThat(controlador.soyAdministrador(JEFA)).containsEntry("administrador", true);
    }

    // --- Apoyo ---------------------------------------------------------------

    private List<Method> endpointsProtegidos() {
        return java.util.Arrays.stream(AdminController.class.getDeclaredMethods())
                .filter(m -> m.getAnnotation(GetMapping.class) != null
                        || m.getAnnotation(PostMapping.class) != null
                        || m.getAnnotation(PutMapping.class) != null
                        || m.getAnnotation(PatchMapping.class) != null
                        || m.getAnnotation(DeleteMapping.class) != null)
                .filter(m -> !m.getName().equals("soyAdministrador"))
                .toList();
    }

    /**
     * Invoca un endpoint con argumentos de relleno.
     *
     * <p>Los valores no importan: la comprobacion de permiso ocurre antes de
     * mirarlos. Si algun dia un endpoint validara sus argumentos primero, este
     * metodo fallaria con otra excepcion y el test lo diria — que tambien seria
     * una señal correcta, porque validar antes de autorizar filtra informacion
     * sobre lo que existe.
     */
    private void invocar(Method endpoint, IdentityRepository.Usuario usuario) throws Exception {
        Object[] argumentos = java.util.Arrays.stream(endpoint.getParameterTypes())
                .map(tipo -> {
                    if (tipo == IdentityRepository.Usuario.class) return usuario;
                    if (tipo == int.class) return 1;
                    if (tipo == long.class) return 1L;
                    if (tipo == boolean.class) return false;
                    if (tipo == AdminController.PeticionSlot.class) {
                        return new AdminController.PeticionSlot(100, 7, 4, true);
                    }
                    if (tipo == AdminController.PeticionPatrocinador.class) {
                        return new AdminController.PeticionPatrocinador(
                                "Hosting",
                                "https://ejemplo.com",
                                null,
                                java.time.Instant.now(),
                                java.time.Instant.now().plusSeconds(86400),
                                10);
                    }
                    if (tipo == AdminController.PeticionReglasDeVoto.class) {
                        return new AdminController.PeticionReglasDeVoto(true, 1, 24);
                    }
                    if (tipo == UUID.class) {
                        return UUID.randomUUID();
                    }
                    if (tipo == AdminController.PeticionSitio.class) {
                        return new AdminController.PeticionSitio(
                                "Un titulo valido", "Una descripcion suficientemente larga.", "LatinosMC", null);
                    }
                    if (tipo == org.springframework.web.multipart.MultipartFile.class) {
                        return mock(org.springframework.web.multipart.MultipartFile.class);
                    }
                    return null;
                })
                .toArray();

        try {
            endpoint.invoke(controlador, argumentos);
        } catch (InvocationTargetException e) {
            // La reflexion envuelve lo que lance el metodo; se desenvuelve para que
            // el test vea la excepcion de verdad.
            if (e.getCause() instanceof Exception causa) {
                throw causa;
            }
            throw e;
        }
    }
}
