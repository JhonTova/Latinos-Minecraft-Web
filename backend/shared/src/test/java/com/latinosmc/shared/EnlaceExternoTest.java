package com.latinosmc.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Un enlace de patrocinador acaba en un {@code href} de nuestra portada.
 *
 * <p>Si ahi entra {@code javascript:}, el ataque no es contra el patrocinador:
 * es codigo ejecutandose en nuestro dominio, con nuestras cookies delante, para
 * todo el que visite la pagina.
 */
class EnlaceExternoTest {

    @Nested
    @DisplayName("Enlaces que se aceptan")
    class Validos {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "https://hosting-ejemplo.com",
                    "http://hosting-ejemplo.com",
                    "https://hosting-ejemplo.com/planes?ref=latinosmc",
                    "https://sub.dominio.ejemplo.com/ruta/larga/pero/normal",
                    "HTTPS://MAYUSCULAS.COM"
                })
        void seAceptan(String url) {
            assertThat(EnlaceExterno.validar(url)).isNotBlank();
        }

        @Test
        @DisplayName("los espacios de alrededor se quitan")
        void seRecortan() {
            assertThat(EnlaceExterno.validar("  https://ejemplo.com  "))
                    .isEqualTo("https://ejemplo.com");
        }
    }

    @Nested
    @DisplayName("Enlaces que se rechazan")
    class Invalidos {

        /** El ataque directo: codigo en vez de direccion. */
        @ParameterizedTest
        @ValueSource(
                strings = {
                    "javascript:alert(1)",
                    "JavaScript:alert(1)",
                    "  javascript:alert(document.cookie)",
                    "vbscript:msgbox(1)",
                    "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
                    "file:///etc/passwd"
                })
        void esquemasPeligrosos(String url) {
            assertThatThrownBy(() -> EnlaceExterno.validar(url))
                    .isInstanceOf(DomainException.Conflict.class);
        }

        /**
         * Partir el esquema con un caracter de control: una comprobacion ingenua ve
         * "java\nscript" y no lo reconoce, pero el navegador ignora el salto de
         * linea y lo ejecuta igual.
         */
        @Test
        @DisplayName("un esquema partido con caracteres de control no cuela")
        void esquemaPartido() {
            assertThatThrownBy(() -> EnlaceExterno.validar("java\nscript:alert(1)"))
                    .isInstanceOf(DomainException.Conflict.class);
            assertThatThrownBy(() -> EnlaceExterno.validar("java\tscript:alert(1)"))
                    .isInstanceOf(DomainException.Conflict.class);
            assertThatThrownBy(() -> EnlaceExterno.validar("java\0script:alert(1)"))
                    .isInstanceOf(DomainException.Conflict.class);
        }

        /**
         * El ojo lee "banco-de-confianza.com" y el navegador va a "sitio-malo.com".
         * No hay ningun uso legitimo de esto en un enlace publicado.
         */
        @Test
        @DisplayName("credenciales dentro de la URL para disfrazar el destino")
        void urlConUsuario() {
            assertThatThrownBy(() -> EnlaceExterno.validar("https://banco-de-confianza.com@sitio-malo.com"))
                    .isInstanceOf(DomainException.Conflict.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"https:", "http:///sin-dominio", "//sin-esquema.com", "solo-texto"})
        void sinEsquemaOSinDominio(String url) {
            assertThatThrownBy(() -> EnlaceExterno.validar(url))
                    .isInstanceOf(DomainException.Conflict.class);
        }

        @Test
        @DisplayName("vacio o nulo")
        void vacio() {
            assertThatThrownBy(() -> EnlaceExterno.validar(null)).isInstanceOf(DomainException.Conflict.class);
            assertThatThrownBy(() -> EnlaceExterno.validar("   ")).isInstanceOf(DomainException.Conflict.class);
        }

        @Test
        @DisplayName("enlaces absurdamente largos")
        void demasiadoLargo() {
            String largo = "https://ejemplo.com/" + "a".repeat(600);
            assertThatThrownBy(() -> EnlaceExterno.validar(largo))
                    .isInstanceOf(DomainException.Conflict.class);
        }
    }
}
