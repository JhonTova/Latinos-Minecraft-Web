package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.latinosmc.identity.AuthService;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.identity.OAuthService;
import com.latinosmc.organizations.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

/**
 * Acceso con Google y Discord.
 *
 * <p>Los dos fallos que cubre este test no daban ningun error: el usuario
 * recorria el flujo entero y aterrizaba en un panel que no funcionaba. Esa es
 * justo la clase de fallo que nadie detecta leyendo el codigo, y la que hay que
 * dejar clavada en un test.
 */
class AccesoConProveedorTest {

    private static final String SITIO = "https://latinosmc.com";

    private OAuthService oauth;
    private IdentityRepository identidades;
    private OrganizationRepository organizaciones;
    private OAuthController controlador;

    @BeforeEach
    void preparar() {
        oauth = mock(OAuthService.class);
        identidades = mock(IdentityRepository.class);
        organizaciones = mock(OrganizationRepository.class);

        Environment entorno = mock(Environment.class);
        when(entorno.matchesProfiles("prod")).thenReturn(true);

        controlador = new OAuthController(oauth, identidades, organizaciones, SITIO, entorno);
    }

    @Test
    @DisplayName("la vuelta del proveedor apunta al sitio, nunca a la API")
    void urlDeRetornoEnElDominioDelSitio() {
        when(oauth.iniciar(anyString(), anyString()))
                .thenReturn(new OAuthService.InicioDeFlujo("https://accounts.google.com/o/oauth2/v2/auth", "s", "v"));

        controlador.iniciar("google");

        ArgumentCaptor<String> urlRetorno = ArgumentCaptor.forClass(String.class);
        verify(oauth).iniciar(eq("google"), urlRetorno.capture());

        // Si esto vuelve a apuntar a api.latinosmc.com, la cookie de sesion se
        // guarda en ese dominio y la web —que esta en otro— nunca la ve: el
        // usuario termina el flujo sin haber entrado y sin ningun error.
        assertThat(urlRetorno.getValue())
                .isEqualTo(SITIO + "/api/backend/auth/oauth/google/callback");
    }

    @Test
    @DisplayName("quien entra por primera vez con un proveedor sale con su organizacion personal")
    void creaLaOrganizacionPersonal() {
        UUID usuarioId = UUID.randomUUID();
        sesionAbiertaPara(usuarioId, "Jhon");

        var respuesta = controlador.callback(
                "discord", "codigo", "estado", null, "estado", "verificador", peticion());

        // Sin esto, la cuenta nace sin organizacion y no puede registrar ni un
        // servidor: el panel le dice que ha llegado a un limite que no tiene.
        verify(organizaciones).asegurarOrganizacionPersonal(usuarioId, "Jhon");
        assertThat(respuesta.getStatusCode().value()).isEqualTo(302);
        assertThat(respuesta.getHeaders().getLocation()).hasToString(SITIO + "/panel");
    }

    @Test
    @DisplayName("si el usuario cancela en el proveedor no se crea nada")
    void cancelarNoCreaOrganizacion() {
        var respuesta = controlador.callback(
                "discord", null, null, "access_denied", "estado", "verificador", peticion());

        verify(organizaciones, never()).asegurarOrganizacionPersonal(any(), anyString());
        assertThat(respuesta.getHeaders().getLocation())
                .hasToString(SITIO + "/login?error=cancelado");
    }

    private void sesionAbiertaPara(UUID usuarioId, String nombre) {
        when(oauth.completar(
                        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(new AuthService.SesionCreada(usuarioId, "token", Duration.ofDays(30)));
        when(identidades.buscarPorId(usuarioId))
                .thenReturn(Optional.of(new IdentityRepository.Usuario(usuarioId, "jhon@ejemplo.com", nombre)));
    }

    private HttpServletRequest peticion() {
        return mock(HttpServletRequest.class);
    }
}
