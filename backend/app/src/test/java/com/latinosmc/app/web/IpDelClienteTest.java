package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * De donde sale la IP con la que se juzga un voto.
 *
 * <p>Esto decide si el cupo por IP y las reglas de antifraude significan algo. Si
 * quien vota puede elegir la IP con la que se le cuenta, no hay cupo: vota mil
 * veces con mil IPs inventadas y el ranking se compra por el precio de un script.
 */
class IpDelClienteTest {

    private static HttpServletRequest peticion(String socket, Map<String, String> cabeceras) {
        var http = Mockito.mock(HttpServletRequest.class);
        Mockito.when(http.getRemoteAddr()).thenReturn(socket);
        Mockito.when(http.getHeader(Mockito.anyString()))
                .thenAnswer(llamada -> cabeceras.get(llamada.<String>getArgument(0)));
        return http;
    }

    private static Map<String, String> cabeceras(String... pares) {
        Map<String, String> mapa = new HashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            mapa.put(pares[i], pares[i + 1]);
        }
        return mapa;
    }

    @Test
    @DisplayName("usa la cabecera que reescribe el proxy")
    void cabeceraDelProxy() {
        var http = peticion("10.0.0.1", cabeceras("X-Forwarded-For", "203.0.113.7"));

        assertThat(IpDelCliente.de(http)).isEqualTo("203.0.113.7");
    }

    /**
     * El fallo que este test cierra.
     *
     * <p>Antes se miraba {@code CF-Connecting-IP} <em>antes</em> que nada, y el
     * proxy documentado no la limpiaba. Quien averiguara la IP del servidor podia
     * saltarse Cloudflare y mandarla a mano: una IP distinta en cada voto, cupo
     * por IP anulado, ranking a la venta.
     *
     * <p>Ahora esa cabecera es texto sin ningun efecto, como cualquier otra que
     * se invente quien llama.
     */
    @Test
    @DisplayName("una cabecera que el proxy no limpia no cambia nada")
    void cabeceraInventadaNoCuenta() {
        var http = peticion(
                "10.0.0.1",
                cabeceras(
                        "CF-Connecting-IP", "6.6.6.6",
                        "X-Real-IP", "7.7.7.7",
                        "X-Forwarded-For", "203.0.113.7"));

        assertThat(IpDelCliente.de(http))
                .as("solo cuenta la cabecera que el proxy reescribe")
                .isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("sin la cabecera del proxy, ninguna otra la sustituye")
    void sinCabeceraDelProxy() {
        var http = peticion("10.0.0.1", cabeceras("CF-Connecting-IP", "6.6.6.6", "X-Real-IP", "7.7.7.7"));

        assertThat(IpDelCliente.de(http))
                .as("caer en una cabecera que nadie limpia es justo el agujero que habia")
                .isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("sin proxy delante vale la direccion del socket")
    void desarrolloLocal() {
        var http = peticion("127.0.0.1", cabeceras());

        assertThat(IpDelCliente.de(http)).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("de una cadena de proxies se toma la primera entrada")
    void cadenaDeProxies() {
        var http = peticion("10.0.0.1", cabeceras("X-Forwarded-For", "203.0.113.7, 70.41.3.18, 150.172.238.178"));

        assertThat(IpDelCliente.de(http)).isEqualTo("203.0.113.7");
    }

    /**
     * Hay proxies que mandan el texto literal "unknown" en lugar de omitir la
     * cabecera. Tomarlo por una IP juntaria bajo el mismo cubo a gente que no
     * tiene nada que ver, y los limites por IP saltarian a destiempo.
     */
    @Test
    @DisplayName("'unknown' no es una IP")
    void desconocida() {
        assertThat(IpDelCliente.de(peticion("10.0.0.1", cabeceras("X-Forwarded-For", "unknown"))))
                .isEqualTo("10.0.0.1");
        assertThat(IpDelCliente.de(peticion("10.0.0.1", cabeceras("X-Forwarded-For", "   "))))
                .isEqualTo("10.0.0.1");
        assertThat(IpDelCliente.de(peticion("10.0.0.1", cabeceras("X-Forwarded-For", " , 8.8.8.8"))))
                .isEqualTo("10.0.0.1");
    }
}
