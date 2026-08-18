package com.latinosmc.voting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El codigo existe para que un jugador de Bedrock —que no puede pulsar enlaces
 * en el chat de Minecraft— pueda votar escribiendolo a mano.
 *
 * <p>Eso convierte la tolerancia a erratas en un requisito, no en un detalle: si
 * escribir el codigo con una ele en lugar de un uno lo rechaza, el jugador
 * concluye que el sitio no funciona y no vuelve.
 */
class CodigoDeVotoTest {

    @Nested
    @DisplayName("Generacion")
    class Generacion {

        @Test
        @DisplayName("tiene la forma de tres grupos de cuatro")
        void forma() {
            assertThat(CodigoDeVoto.generar()).matches("[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}");
        }

        /**
         * Estos cuatro se excluyeron del alfabeto porque se confunden al leerlos
         * de un chat. Si alguno se colara, el jugador teclearia otra cosa y el
         * codigo fallaria sin motivo aparente.
         */
        @Test
        @DisplayName("nunca contiene I, L, O ni U")
        void sinCaracteresAmbiguos() {
            for (int i = 0; i < 2000; i++) {
                assertThat(CodigoDeVoto.generar()).doesNotContainAnyWhitespaces().doesNotContain("I", "L", "O", "U");
            }
        }

        @Test
        @DisplayName("no se repite")
        void sinRepeticiones() {
            Set<String> vistos = new HashSet<>();
            for (int i = 0; i < 5000; i++) {
                assertThat(vistos.add(CodigoDeVoto.generar())).isTrue();
            }
        }

        /**
         * Un generador roto que devolviera siempre los mismos simbolos pasaria los
         * tests de forma sin problema. Este comprueba que el alfabeto se usa
         * entero: con 12 caracteres por codigo y 500 codigos son 6000 tiradas, y
         * ver menos de 30 simbolos distintos de 32 seria una casualidad absurda.
         */
        @Test
        @DisplayName("usa todo el alfabeto, no solo unos pocos simbolos")
        void distribucion() {
            Set<Character> simbolos = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                for (char c : CodigoDeVoto.generar().replace("-", "").toCharArray()) {
                    simbolos.add(c);
                }
            }
            assertThat(simbolos).hasSizeGreaterThanOrEqualTo(30);
        }
    }

    @Nested
    @DisplayName("Lo que escribe el jugador")
    class Normalizacion {

        @Test
        @DisplayName("un codigo recien generado se normaliza a 12 caracteres sin guiones")
        void idaYVuelta() {
            String generado = CodigoDeVoto.generar();
            String normalizado = CodigoDeVoto.normalizar(generado);

            assertThat(normalizado).hasSize(12).doesNotContain("-");
            assertThat(CodigoDeVoto.tieneForma(normalizado)).isTrue();
            assertThat(CodigoDeVoto.agrupar(normalizado)).isEqualTo(generado);
        }

        /** Todas estas son la misma persona escribiendo el mismo codigo. */
        @ParameterizedTest
        @ValueSource(
                strings = {
                    "K7M2-9QX4-RT3F",
                    "k7m2-9qx4-rt3f",
                    "K7M29QX4RT3F",
                    "  K7M2 9QX4 RT3F  ",
                    "K7M2.9QX4.RT3F",
                    "K7M2 - 9QX4 - RT3F"
                })
        @DisplayName("da igual como lo escriba: guiones, espacios, minusculas")
        void mismasVariantes(String escrito) {
            assertThat(CodigoDeVoto.normalizar(escrito)).isEqualTo("K7M29QX4RT3F");
        }

        /**
         * El caso que de verdad importa. Nadie distingue una I de un 1 ni una O de
         * un 0 leyendo el chat de Minecraft a 12 puntos. Como esos simbolos no
         * existen en el alfabeto, escribirlos solo puede significar una cosa.
         */
        @ParameterizedTest
        @CsvSource({
            "1I34-5678-9ABC, 11345678 9ABC",
            "1L34-5678-9ABC, 11345678 9ABC",
            "0O34-5678-9ABC, 00345678 9ABC",
            "VU34-5678-9ABC, VV345678 9ABC"
        })
        @DisplayName("confundir I, L, O o U se corrige en vez de rechazarse")
        void erratasDeLectura(String escrito, String esperadoConEspacio) {
            assertThat(CodigoDeVoto.normalizar(escrito))
                    .isEqualTo(esperadoConEspacio.replace(" ", ""));
        }

        @Test
        @DisplayName("de mas caracteres se queda con los primeros 12")
        void demasiadoLargo() {
            assertThat(CodigoDeVoto.normalizar("K7M29QX4RT3FZZZZZZZZ")).isEqualTo("K7M29QX4RT3F");
        }

        @Test
        @DisplayName("null y basura no revientan")
        void entradasImposibles() {
            assertThat(CodigoDeVoto.normalizar(null)).isEmpty();
            assertThat(CodigoDeVoto.normalizar("")).isEmpty();
            assertThat(CodigoDeVoto.normalizar("¿?¡!---")).isEmpty();
            assertThat(CodigoDeVoto.tieneForma(CodigoDeVoto.normalizar("corto"))).isFalse();
        }
    }
}
