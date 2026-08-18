package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Limite de trafico.
 *
 * <p>El plugin se distribuye como un JAR publico: cualquiera puede abrirlo, leer
 * las rutas de la API y escribir un programa que las llame. Lo unico que separa
 * eso de una saturacion del servicio es este filtro, asi que aqui se prueba
 * atacandolo, no usandolo bien.
 */
class ControlDeTraficoTest {

    private ControlDeTrafico filtro;
    private FilterChain siguiente;

    @BeforeEach
    void preparar() {
        filtro = new ControlDeTrafico();
        siguiente = (peticion, respuesta) -> {};
    }

    private int golpear(String ruta, String metodo, String ip, String apiKey, int veces) throws Exception {
        int bloqueadas = 0;

        for (int i = 0; i < veces; i++) {
            var peticion = new MockHttpServletRequest(metodo, ruta);
            peticion.setRemoteAddr(ip);
            if (apiKey != null) {
                peticion.addHeader("X-Api-Key", apiKey);
            }
            var respuesta = new MockHttpServletResponse();

            filtro.doFilter(peticion, respuesta, siguiente);
            if (respuesta.getStatus() == 429) {
                bloqueadas++;
            }
        }
        return bloqueadas;
    }

    @Nested
    @DisplayName("Uso legitimo")
    class Legitimo {

        @Test
        @DisplayName("un plugin normal no se ve afectado")
        void pluginNormal() throws Exception {
            // Un plugin consulta un par de veces por minuto; 30 es diez veces mas.
            assertThat(golpear("/api/v1/plugin/rewards/pending", "GET", "1.2.3.4", "clave-legitima", 30))
                    .isZero();
        }

        /**
         * En un hosting compartido salen muchos servidores por la misma IP. Cada
         * uno tiene su clave y su contador: uno que abuse no puede castigar a los
         * demas.
         */
        @Test
        @DisplayName("servidores distintos tras la misma IP no se gastan el cupo entre ellos")
        void hostingCompartido() throws Exception {
            golpear("/api/v1/plugin/heartbeat", "POST", "9.9.9.9", "servidor-a", 60);

            assertThat(golpear("/api/v1/plugin/heartbeat", "POST", "9.9.9.9", "servidor-b", 10))
                    .as("el servidor B no deberia pagar el abuso del A")
                    .isZero();
        }

        @Test
        @DisplayName("las lecturas publicas son generosas")
        void lecturaPublica() throws Exception {
            assertThat(golpear("/api/v1/ranking", "GET", "1.2.3.4", null, 100)).isZero();
        }
    }

    @Nested
    @DisplayName("Abuso")
    class Abuso {

        /**
         * <strong>El agujero que tenia este filtro.</strong>
         *
         * <p>El contador del plugin se lleva por clave de API, para que en un
         * hosting compartido un servidor no castigue a los demas. Pero la clave la
         * elige quien llama: mandando una INVENTADA DISTINTA en cada peticion se
         * estrenaba un contador nuevo cada vez y el limite no llegaba a aplicarse
         * nunca.
         *
         * <p>No hacia falta ninguna credencial valida: cada una de esas peticiones
         * llegaba igualmente hasta la consulta de autenticacion en base de datos.
         * Era una via directa para saturar la API desde fuera.
         */
        @Test
        @DisplayName("claves inventadas distintas NO permiten saltarse el limite")
        void clavesRotatorias() throws Exception {
            int bloqueadas = 0;

            for (int i = 0; i < 2000; i++) {
                var peticion = new MockHttpServletRequest("POST", "/api/v1/plugin/heartbeat");
                peticion.setRemoteAddr("6.6.6.6");
                peticion.addHeader("X-Api-Key", "inventada-" + i);
                var respuesta = new MockHttpServletResponse();

                filtro.doFilter(peticion, respuesta, siguiente);
                if (respuesta.getStatus() == 429) {
                    bloqueadas++;
                }
            }

            assertThat(bloqueadas)
                    .as("con 2000 peticiones desde una IP, la mayoria tienen que rebotar")
                    .isGreaterThan(1000);
        }

        @Test
        @DisplayName("sin clave tampoco se puede martillear el endpoint del plugin")
        void sinClave() throws Exception {
            assertThat(golpear("/api/v1/plugin/heartbeat", "POST", "7.7.7.7", null, 200))
                    .isGreaterThan(100);
        }

        @Test
        @DisplayName("no se puede votar 100 veces por minuto")
        void votosEnRafaga() throws Exception {
            assertThat(golpear("/api/v1/servers/craftlatino/votes", "POST", "8.8.8.8", null, 100))
                    .isGreaterThan(80);
        }

        @Test
        @DisplayName("no se pueden probar contrasenas por fuerza bruta")
        void fuerzaBruta() throws Exception {
            assertThat(golpear("/api/v1/auth/login", "POST", "5.5.5.5", null, 100))
                    .isGreaterThan(80);
        }

        /**
         * Con {@code String.hashCode()} esto era posible: dos cadenas distintas
         * comparten hashCode con facilidad, asi que se podia fabricar una clave que
         * cayera en el contador de un servidor legitimo y agotarselo desde fuera.
         * El servidor honesto empezaria a recibir 429 sin haber hecho nada.
         *
         * <p>"Aa" y "BB" son el ejemplo clasico: mismo hashCode en Java.
         */
        @Test
        @DisplayName("no se puede agotar el cupo de otro servidor por colision de hash")
        void colisionDeHash() throws Exception {
            assertThat("Aa".hashCode())
                    .as("si esto deja de colisionar, el test ya no prueba lo que cree")
                    .isEqualTo("BB".hashCode());

            golpear("/api/v1/plugin/heartbeat", "POST", "1.1.1.1", "Aa", 60);

            assertThat(golpear("/api/v1/plugin/heartbeat", "POST", "2.2.2.2", "BB", 10))
                    .as("la clave 'BB' no deberia haber gastado el cupo de 'Aa'")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Categorias")
    class Categorias {

        /**
         * Cada tipo de trafico lleva su propio contador. Si compartieran uno,
         * navegar por el ranking dejaria sin cupo para votar.
         */
        @Test
        @DisplayName("agotar el cupo de voto no impide seguir leyendo el ranking")
        void categoriasIndependientes() throws Exception {
            golpear("/api/v1/servers/craftlatino/votes", "POST", "4.4.4.4", null, 50);

            assertThat(golpear("/api/v1/ranking", "GET", "4.4.4.4", null, 20)).isZero();
        }

        @Test
        @DisplayName("el actuator queda fuera del filtro: lo consulta Docker cada 30 s")
        void actuatorExcluido() {
            var peticion = new MockHttpServletRequest("GET", "/actuator/health");
            assertThat(filtro.shouldNotFilter(peticion)).isTrue();
        }
    }
}
