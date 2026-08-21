package com.latinosmc.servers.ping.motd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El MOTD lo escribe el dueño del servidor y nosotros lo publicamos en nuestro
 * dominio. Estas pruebas fijan las dos cosas que no pueden pasar de largo: que
 * acabemos anunciando a la competencia, y que alguien cuele caracteres que
 * cambian lo que se lee.
 */
class SaneadorDeMotdTest {

    private static final Set<String> PROPIOS = Set.of("latinosmc.com");

    private static SaneadorDeMotd.Resultado sanear(String motd, String host) {
        return SaneadorDeMotd.sanear(MotdMinecraft.analizarTexto(motd), host, PROPIOS);
    }

    @Nested
    @DisplayName("Direcciones ajenas")
    class Dominios {

        @Test
        void quitaElSitioDeVotosDeLaCompetencia() {
            // Publicar esto tal cual seria pagar hosting para anunciar a la
            // competencia en nuestra propia web.
            var r = sanear("Mi server\nVota en minecraftlist.com y gana", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("minecraftlist.com");
            assertThat(r.quitados()).contains("minecraftlist.com");
        }

        @Test
        void conservaLaDireccionDelPropioServidor() {
            // Es el dato mas util del MOTD para el jugador: borrarlo seria peor que
            // el problema que venimos a resolver.
            var r = sanear("Entra en play.ejemplo.com", "play.ejemplo.com");

            assertThat(r.textoPlano()).contains("play.ejemplo.com");
            assertThat(r.quitados()).isEmpty();
        }

        @Test
        void conservaOtrosSubdominiosDeLaMismaMarca() {
            // El servidor registrado como play.ejemplo.com tiene derecho a anunciar
            // su tienda: es su marca, no la de otro.
            var r = sanear("Tienda: tienda.ejemplo.com", "play.ejemplo.com");

            assertThat(r.textoPlano()).contains("tienda.ejemplo.com");
        }

        @Test
        void conservaNuestroPropioDominio() {
            var r = sanear("Votanos en latinosmc.com", "play.ejemplo.com");

            assertThat(r.textoPlano()).contains("latinosmc.com");
        }

        @Test
        void quitaUrlsCompletas() {
            var r = sanear("Mira https://otro.net/algo ya", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("otro.net");
        }

        @Test
        void quitaIpsDeOtrosServidores() {
            var r = sanear("Prueba 51.161.24.7:25565", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("51.161.24.7");
        }

        @Test
        void noConfundeUnaVersionConUnDominio() {
            // "1.21.4" tiene puntos pero su ultima etiqueta no es una extension:
            // borrarla dejaria a media plataforma sin poder decir su version.
            var r = sanear("Survival 1.21.4 abierto", "play.ejemplo.com");

            assertThat(r.textoPlano()).contains("1.21.4");
        }

        @Test
        void noConfundeUnPuntoSinEspacioConUnDominio() {
            var r = sanear("¡Bienvenido!.Entra ya", "play.ejemplo.com");

            assertThat(r.textoPlano()).contains("Entra ya");
        }

        @Test
        void detectaUnDominioRepartidoEntreVariosColores() {
            // "§bvota.§fejemplo.net" son tres tramos distintos y ninguno parece un
            // dominio por separado. Es la forma mas facil de esquivar un filtro que
            // mire tramo a tramo.
            var r = sanear("§bvota.§fotrositio§7.net ya", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("otrositio");
        }
    }

    @Nested
    @DisplayName("Caracteres que cambian lo que se lee")
    class Invisibles {

        @Test
        void quitaLasMarcasDeDireccionBidireccional() {
            // Reordenan visualmente el texto de alrededor: es como un nombre se
            // hace pasar por otro en una lista donde se elige por lo que se lee.
            var r = sanear("Servidor‮roditsujA", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("‮");
        }

        @Test
        void quitaLosEspaciosDeAnchoCero() {
            var r = sanear("mine​craftlist.com", "play.ejemplo.com");

            assertThat(r.textoPlano()).doesNotContain("​");
            // Y al juntarse, el dominio queda a la vista y tambien se va.
            assertThat(r.textoPlano()).doesNotContain("minecraftlist.com");
        }
    }

    @Nested
    @DisplayName("Presentacion")
    class Presentacion {

        @Test
        void laLineaQueLlevabaUnaDireccionAjenaSeVaEntera() {
            // Recortar solo el dominio dejaba "Vota en y en", que en el banner se
            // lee como que la plataforma esta rota, no como que el servidor
            // anunciaba a otro sitio.
            var r = sanear("El mejor server\nVota en otro.net y en 51.161.24.7", "play.ejemplo.com");

            assertThat(r.lineas()).hasSize(1);
            assertThat(r.textoPlano()).isEqualTo("El mejor server");
        }

        @Test
        void laLineaConLaDireccionPropiaSeQuedaComoEstaba() {
            var r = sanear("Mi servidor\nEntra en play.ejemplo.com", "play.ejemplo.com");

            assertThat(r.lineas()).hasSize(2);
            assertThat(r.textoPlano()).contains("Entra en play.ejemplo.com");
        }

        @Test
        void unMotdQueEraSoloUnaUrlSeQuedaVacio() {
            var r = sanear("otro.net", "play.ejemplo.com");

            assertThat(r.estaVacio()).isTrue();
            // Y entonces el banner cae al nombre del servidor, no a un banner en
            // blanco (lo cubre GeneradorDeBannerMotdTest).
        }

        @Test
        void acotaLaLongitudDeCadaLinea() {
            var r = sanear("x".repeat(300), "play.ejemplo.com");

            assertThat(r.lineas().get(0).textoPlano().length()).isLessThanOrEqualTo(64);
        }

        @Test
        void conservaLosColoresDeLoQueSobrevive() {
            var r = sanear("§6Oro §aVerde", "play.ejemplo.com");

            assertThat(r.lineas().get(0).tramos())
                    .extracting(MotdMinecraft.Tramo::color)
                    .containsExactly(0xFFAA00, 0x55FF55);
        }
    }
}
