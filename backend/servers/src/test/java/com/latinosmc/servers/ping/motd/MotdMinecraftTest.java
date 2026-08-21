package com.latinosmc.servers.ping.motd;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El MOTD llega de tres formas distintas segun la version y el software del
 * servidor, y las tres conviven hoy en servidores reales. Estas pruebas fijan que
 * las tres se entienden, porque el dia que una deje de entenderse el sintoma es
 * un banner en blanco que nadie relaciona con el parser.
 */
class MotdMinecraftTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static List<MotdMinecraft.Linea> analizar(String json) throws Exception {
        return MotdMinecraft.analizar(JSON.readTree(json));
    }

    private static String plano(List<MotdMinecraft.Linea> lineas) {
        return String.join("\n", lineas.stream().map(MotdMinecraft.Linea::textoPlano).toList());
    }

    @Nested
    @DisplayName("Texto plano con codigos de seccion")
    class Legacy {

        @Test
        void aplicaLosDieciseisColores() {
            var lineas = MotdMinecraft.analizarTexto("§6Oro §aVerde");

            assertThat(lineas).hasSize(1);
            assertThat(lineas.get(0).tramos())
                    .extracting(MotdMinecraft.Tramo::texto, MotdMinecraft.Tramo::color)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("Oro ", 0xFFAA00),
                            org.assertj.core.groups.Tuple.tuple("Verde", 0x55FF55));
        }

        @Test
        void unColorReiniciaElFormato() {
            // Es lo que hace el cliente del juego: §c despues de §l sale rojo y sin
            // negrita. Si aqui se acumulara, medio MOTD saldria en negrita.
            var lineas = MotdMinecraft.analizarTexto("§lNegrita§cNormal");

            assertThat(lineas.get(0).tramos().get(0).estilo().negrita()).isTrue();
            assertThat(lineas.get(0).tramos().get(1).estilo().negrita()).isFalse();
        }

        @Test
        void elCodigoDeAnimacionSeIgnoraPeroElTextoSeQueda() {
            // §k pinta glifos aleatorios que cambian solos. En una imagen fija no
            // existe, asi que se pinta lo de debajo (ADR-0027).
            var lineas = MotdMinecraft.analizarTexto("§kSECRETO");

            assertThat(plano(lineas)).isEqualTo("SECRETO");
        }

        @Test
        void reinicioVuelveAlEstiloDeEntrada() {
            var lineas = MotdMinecraft.analizarTexto("§c§lRojo§rNormal");

            var ultimo = lineas.get(0).tramos().get(1);
            assertThat(ultimo.texto()).isEqualTo("Normal");
            assertThat(ultimo.color()).isEqualTo(MotdMinecraft.COLOR_POR_DEFECTO);
            assertThat(ultimo.estilo().negrita()).isFalse();
        }

        @Test
        void elSaltoDeLineaParteEnDos() {
            var lineas = MotdMinecraft.analizarTexto("Primera\nSegunda");

            assertThat(lineas).hasSize(2);
            assertThat(lineas.get(1).textoPlano()).isEqualTo("Segunda");
        }

        @Test
        void masDeDosLineasSeRecortanComoEnElJuego() {
            var lineas = MotdMinecraft.analizarTexto("Una\nDos\nTres\nCuatro");

            assertThat(lineas).hasSize(2);
            assertThat(plano(lineas)).isEqualTo("Una\nDos");
        }

        @Test
        void unCodigoAMediasNoRompeNada() {
            // El § final no tiene codigo detras. Un servidor mal configurado puede
            // mandarlo, y no puede costarnos el MOTD entero.
            assertThat(plano(MotdMinecraft.analizarTexto("Hola§"))).isEqualTo("Hola§");
        }
    }

    @Nested
    @DisplayName("Componentes de chat")
    class Componentes {

        @Test
        void resuelveColoresPorNombre() throws Exception {
            var lineas = analizar("{\"text\":\"Hola\",\"color\":\"gold\"}");

            assertThat(lineas.get(0).tramos().get(0).color()).isEqualTo(0xFFAA00);
        }

        @Test
        void resuelveColoresHexadecimales() throws Exception {
            // Los MOTD con degradado pintan cada letra de un hex distinto. Sin
            // esto saldrian todos blancos y el banner perderia lo que distingue a
            // los servidores que mas se lo curran.
            var lineas = analizar("{\"text\":\"A\",\"color\":\"#FF8800\"}");

            assertThat(lineas.get(0).tramos().get(0).color()).isEqualTo(0xFF8800);
        }

        @Test
        void unHexadecimalInvalidoNoTumbaElAnalisis() throws Exception {
            var lineas = analizar("{\"text\":\"A\",\"color\":\"#ZZZZZZ\"}");

            assertThat(lineas.get(0).tramos().get(0).color()).isEqualTo(MotdMinecraft.COLOR_POR_DEFECTO);
        }

        @Test
        void losHijosHeredanElEstiloDelPadre() throws Exception {
            var lineas = analizar(
                    "{\"text\":\"A\",\"color\":\"red\",\"bold\":true,\"extra\":[{\"text\":\"B\"}]}");

            var hijo = lineas.get(0).tramos().get(0);
            assertThat(hijo.texto()).isEqualTo("AB");
            assertThat(hijo.color()).isEqualTo(0xFF5555);
            assertThat(hijo.estilo().negrita()).isTrue();
        }

        @Test
        void elHijoPuedeSobrescribirLoQueHeredo() throws Exception {
            var lineas = analizar(
                    "{\"text\":\"A\",\"color\":\"red\",\"extra\":[{\"text\":\"B\",\"color\":\"blue\"}]}");

            assertThat(lineas.get(0).tramos())
                    .extracting(MotdMinecraft.Tramo::color)
                    .containsExactly(0xFF5555, 0x5555FF);
        }

        @Test
        void entiendeUnaListaDeComponentes() throws Exception {
            var lineas = analizar("[{\"text\":\"Uno\"},{\"text\":\"Dos\"}]");

            assertThat(plano(lineas)).isEqualTo("UnoDos");
        }

        @Test
        void interpretaCodigosDeSeccionDentroDeUnComponente() throws Exception {
            // Muchos plugins generan componentes cuyo texto lleva ademas codigos §,
            // y el cliente los interpreta. Sin esto, esos MOTD saldrian con las
            // letras "§6" a la vista.
            var lineas = analizar("{\"text\":\"§6Oro\"}");

            assertThat(plano(lineas)).isEqualTo("Oro");
            assertThat(lineas.get(0).tramos().get(0).color()).isEqualTo(0xFFAA00);
        }

        @Test
        void unAnidamientoAbusivoSeCortaSinReventar() throws Exception {
            // Un servidor hostil puede anidar componentes hasta donde le dejen. El
            // parser corta a los 32 niveles: nadie escribe un MOTD con treinta
            // niveles a mano, y sin tope esto recorreria lo que le echen.
            //
            // Cuatrocientos niveles y no ocho mil porque Jackson ya rechaza por su
            // cuenta a partir de mil de profundidad —y cada nivel aqui son dos, el
            // objeto y su lista—, y ahi el que corta es el lector de JSON, no
            // esto. Se prueba el tramo que de verdad llega hasta aqui.
            var json = new StringBuilder();
            int niveles = 400;
            for (int i = 0; i < niveles; i++) {
                json.append("{\"text\":\"x\",\"extra\":[");
            }
            json.append("{\"text\":\"fin\"}");
            for (int i = 0; i < niveles; i++) {
                json.append("]}");
            }

            var lineas = analizar(json.toString());

            // Se queda con lo que llego hasta el tope y descarta lo de mas adentro.
            assertThat(plano(lineas)).doesNotContain("fin");
            assertThat(plano(lineas).length()).isLessThanOrEqualTo(512);
        }

        @Test
        void unJsonDemasiadoAnidadoLoRechazaElLectorAntesDeLlegarAqui() {
            // Defensa por duplicado, y conviene que conste: Jackson corta a los mil
            // niveles y lanza. En la ruta real eso lo recoge PingMinecraft, que lo
            // trata como servidor que no responde bien — no como un fallo nuestro.
            var json = new StringBuilder();
            for (int i = 0; i < 2000; i++) {
                json.append("{\"extra\":[");
            }

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> analizar(json.toString()))
                    .isInstanceOf(java.io.IOException.class);
        }

        @Test
        void unMotdEnormeSeAcotaEnLugarDeCrecerSinFin() throws Exception {
            String largo = "x".repeat(5000);
            var lineas = analizar("{\"text\":\"" + largo + "\"}");

            assertThat(plano(lineas).length()).isLessThanOrEqualTo(512);
        }

        @Test
        void unDescriptorAusenteDaUnMotdVacio() {
            assertThat(MotdMinecraft.analizar(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Ida y vuelta a JSON guardado")
    class Serializacion {

        @Test
        void loQueSeGuardaSeRecuperaIgual() {
            var original = MotdMinecraft.analizarTexto("§6§lOro§r §7gris\n§bAqua");

            var recuperado = MotdJson.deserializar(MotdJson.serializar(original));

            assertThat(recuperado).isEqualTo(original);
        }

        @Test
        void unValorCorruptoDaVacioEnLugarDeReventar() {
            // Esto se lee al pintar el directorio: fallar aqui tumbaria la portada
            // entera por una sola fila mal guardada.
            assertThat(MotdJson.deserializar("{no es esto}")).isEmpty();
            assertThat(MotdJson.deserializar(null)).isEmpty();
        }
    }
}
