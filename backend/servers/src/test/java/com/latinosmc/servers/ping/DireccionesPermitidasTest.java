package com.latinosmc.servers.ping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DireccionesPermitidasTest {

    private final DireccionesPermitidas permitidas = new DireccionesPermitidas();

    /**
     * El test que justifica que esta clase exista.
     *
     * <p>El host de un servidor lo escribe el cliente en su panel. Sin este
     * bloqueo, cualquiera podría registrar un "servidor" apuntando a nuestra red
     * interna y usar el sondeador para explorarla desde dentro, con nuestra IP y
     * saltándose el cortafuegos.
     */
    @ParameterizedTest
    @DisplayName("bloquea direcciones internas")
    @ValueSource(
            strings = {
                "127.0.0.1", // el propio servidor
                "localhost",
                "0.0.0.0",
                "10.0.0.1", // red privada
                "192.168.1.1", // red doméstica
                "172.16.0.1", // red privada
                "100.64.0.1", // CGNAT: otras máquinas del centro de datos
                "198.18.0.1" // rango de pruebas entre redes
            })
    void bloqueaInternas(String host) {
        assertThat(permitidas.resolverSiEsSegura(host)).isNull();
    }

    /**
     * 169.254.169.254 es el servicio de metadatos en la mayoría de proveedores
     * cloud. En muchos entrega credenciales de la máquina a quien pregunte, sin
     * autenticación. Es el destino favorito de cualquier SSRF.
     */
    @Test
    @DisplayName("bloquea el servicio de metadatos de la nube")
    void bloqueaMetadatosCloud() {
        assertThat(permitidas.resolverSiEsSegura("169.254.169.254")).isNull();
    }

    @Test
    @DisplayName("permite una dirección pública normal")
    void permitePublicas() {
        assertThat(permitidas.resolverSiEsSegura("8.8.8.8")).isNotNull();
    }

    @Test
    @DisplayName("un dominio que no existe se trata como servidor caído, no como error")
    void dominioInexistente() {
        assertThat(permitidas.resolverSiEsSegura("no-existe-de-verdad-12345.invalid"))
                .isNull();
    }
}
