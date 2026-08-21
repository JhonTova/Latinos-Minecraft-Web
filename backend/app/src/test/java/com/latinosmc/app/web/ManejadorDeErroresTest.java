package com.latinosmc.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.latinosmc.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Lo que se prueba aqui es sobre todo lo que <strong>no</strong> debe llegar a la
 * red de seguridad.
 *
 * <p>Antes no habia manejador para las peticiones mal formadas, asi que una URL
 * inexistente o un identificador con un caracter de mas salian como 500 con una
 * referencia de soporte y una traza completa en el registro. Dos consecuencias:
 * la respuesta le decia a quien integra que el fallo era nuestro, y el registro
 * de un sitio publico se llenaba de errores graves provocados por rastreadores
 * probando rutas al azar.
 */
class ManejadorDeErroresTest {

    private final ManejadorDeErrores manejador = new ManejadorDeErrores();

    private static HttpStatusCode estadoDe(org.springframework.http.ProblemDetail problema) {
        return HttpStatusCode.valueOf(problema.getStatus());
    }

    @Nested
    @DisplayName("Culpa de quien llama")
    class DelCliente {

        @Test
        @DisplayName("una URL que no existe es 404, no 500")
        void rutaInexistente() {
            var problema = manejador.rutaInexistente(
                    new org.springframework.web.servlet.resource.NoResourceFoundException(
                            org.springframework.http.HttpMethod.GET, "/api/v1/inventada"));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(problema.getTitle()).isEqualTo("not_found");
        }

        @Test
        @DisplayName("la ruta existe pero con otro metodo: 405")
        void metodoNoPermitido() {
            var problema = manejador.metodoNoPermitido(
                    new org.springframework.web.HttpRequestMethodNotSupportedException("DELETE"));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(problema.getDetail()).contains("DELETE");
        }

        @Test
        @DisplayName("un identificador que no es un UUID es 400")
        void identificadorInvalido() throws Exception {
            var problema = manejador.tipoInvalido(
                    new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                            "no-soy-un-uuid", java.util.UUID.class, "servidorId", null, null));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(problema.getDetail()).contains("servidorId");
        }

        @Test
        @DisplayName("no se devuelve el valor recibido, solo el nombre del campo")
        void noRefleja() {
            // Reflejar la entrada tal cual en la respuesta es como se construye un
            // XSS reflejado si algun cliente la pinta sin escapar.
            String malicioso = "<script>alert(1)</script>";

            var problema = manejador.tipoInvalido(
                    new org.springframework.web.method.annotation.MethodArgumentTypeMismatchException(
                            malicioso, java.util.UUID.class, "servidorId", null, null));

            assertThat(problema.getDetail()).doesNotContain("script");
        }

        @Test
        @DisplayName("un cuerpo ilegible es 400 y no cuenta como fue de ilegible")
        void cuerpoIlegible() {
            // El mensaje de la excepcion trae nombres de clases de Java y la
            // posicion exacta del fallo: eso describe nuestra implementacion, no
            // el problema de quien llama.
            var problema = manejador.cuerpoIlegible(
                    new org.springframework.http.converter.HttpMessageNotReadableException(
                            "com.fasterxml.jackson.databind.JsonMappingException en la linea 3",
                            (org.springframework.http.HttpInputMessage) null));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(problema.getDetail()).doesNotContain("jackson");
        }

        @Test
        @DisplayName("un archivo demasiado grande es 413, igual que lo que corta el proxy")
        void archivoDemasiadoGrande() {
            var problema = manejador.archivoDemasiadoGrande(
                    new org.springframework.web.multipart.MaxUploadSizeExceededException(1024));

            // 413 y no 500 para que el cliente trate igual el corte del proxy y el
            // nuestro. La web ya lo hace.
            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("Culpa nuestra")
    class DelServidor {

        @Test
        @DisplayName("un fallo inesperado sigue siendo 500 con referencia")
        void inesperado() {
            var problema = manejador.inesperado(new IllegalStateException("se rompio algo"));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(problema.getProperties()).containsKey("referencia");
        }

        @Test
        @DisplayName("nunca se filtra el detalle interno")
        void noFiltraElDetalle() {
            var problema = manejador.inesperado(
                    new IllegalStateException("password=secreto en org.postgresql.Driver"));

            assertThat(problema.getDetail()).doesNotContain("secreto");
            assertThat(problema.getDetail()).doesNotContain("postgresql");
        }
    }

    @Nested
    @DisplayName("Errores de dominio")
    class Dominio {

        @Test
        void noEncontrado() {
            var problema = manejador.noEncontrado(
                    new DomainException.NotFound("server.not_found", "No existe."));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(problema.getTitle()).isEqualTo("server.not_found");
        }

        @Test
        @DisplayName("prohibido es 403: reintentar el login no arregla nada")
        void prohibido() {
            var problema = manejador.prohibido(
                    new DomainException.Forbidden("admin.required", "Solo administradores."));

            assertThat(estadoDe(problema)).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
