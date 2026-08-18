package com.latinosmc.servers.ping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Sondeo del estado de un servidor de Minecraft (Server List Ping).
 *
 * <p>Es el mismo protocolo que usa el cliente del juego para mostrar los
 * jugadores conectados en la lista de servidores. Lo medimos NOSOTROS
 * (ADR-0009): el numero que reporta el plugin corre en hardware del cliente, que
 * es justo quien tiene incentivo para inflarlo. Este dato, en cambio, no se
 * puede falsear sin falsear el servidor entero.
 *
 * <p>Codigo especifico de Minecraft, aislado en su propio paquete adaptador. El
 * dominio no sabe que esto existe (convenciones del proyecto, regla 13).
 *
 * <p><strong>Todo lo que llega por este socket es entrada no confiable.</strong>
 * El servidor del otro lado puede ser hostil: puede anunciar una respuesta de
 * 2 GB, no responder nunca, o enviar basura. De ahi los limites de tamano y los
 * timeouts.
 */
@Component
public class PingMinecraft {

    /** Version de protocolo que anunciamos. -1 significa "solo estoy preguntando". */
    private static final int PROTOCOLO_DESCONOCIDO = -1;

    private static final int ESTADO_STATUS = 1;

    /** Un servidor sano responde en decimas de segundo. */
    private static final int TIMEOUT_MS = 4000;

    /**
     * Tope de la respuesta. Sin el, un servidor hostil anuncia una longitud
     * enorme y nos deja reservando memoria hasta tumbar el proceso. 64 KB sobra:
     * una respuesta real ronda 1 KB, y las que llevan icono, unos 20 KB.
     */
    private static final int MAX_RESPUESTA_BYTES = 64 * 1024;

    private final ObjectMapper json;

    public PingMinecraft(ObjectMapper json) {
        this.json = json;
    }

    /**
     * Sondea un servidor.
     *
     * @return el resultado; nunca lanza — un servidor caido es un dato, no un fallo
     */
    public Resultado sondear(java.net.InetAddress direccion, int puerto, String hostOriginal) {
        long inicio = System.nanoTime();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(direccion, puerto), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream salida = socket.getOutputStream();
            DataInputStream entrada = new DataInputStream(socket.getInputStream());

            enviarHandshake(salida, hostOriginal, puerto);
            enviarPeticionEstado(salida);

            String cuerpo = leerRespuesta(entrada);
            int latenciaMs = (int) ((System.nanoTime() - inicio) / 1_000_000);

            JsonNode datos = json.readTree(cuerpo);
            JsonNode jugadores = datos.path("players");

            return Resultado.enLinea(
                    entero(jugadores, "online"),
                    entero(jugadores, "max"),
                    latenciaMs,
                    datos.path("version").path("name").asText(null));

        } catch (IOException | RuntimeException e) {
            // Caido, filtrado, cambiado de IP, o simplemente apagado. Todo eso es
            // "sin conexion" para el jugador, y asi se registra.
            return Resultado.sinConexion(mensajeCorto(e));
        }
    }

    // --- Protocolo -----------------------------------------------------------

    /**
     * Handshake: version de protocolo, direccion, puerto y estado siguiente.
     *
     * <p>Se envia el host ORIGINAL, no la IP resuelta: muchos servidores usan
     * proxies que enrutan por nombre de dominio y responderian mal con la IP.
     */
    private void enviarHandshake(OutputStream salida, String host, int puerto) throws IOException {
        var cuerpo = new java.io.ByteArrayOutputStream();
        var datos = new DataOutputStream(cuerpo);

        escribirVarInt(datos, 0x00); // id de paquete: handshake
        escribirVarInt(datos, PROTOCOLO_DESCONOCIDO);
        escribirCadena(datos, host);
        datos.writeShort(puerto);
        escribirVarInt(datos, ESTADO_STATUS);

        enviarPaquete(salida, cuerpo.toByteArray());
    }

    private void enviarPeticionEstado(OutputStream salida) throws IOException {
        enviarPaquete(salida, new byte[] {0x00});
    }

    /** Cada paquete va precedido de su longitud como VarInt. */
    private void enviarPaquete(OutputStream salida, byte[] contenido) throws IOException {
        var marco = new java.io.ByteArrayOutputStream();
        var datos = new DataOutputStream(marco);
        escribirVarInt(datos, contenido.length);
        datos.write(contenido);
        salida.write(marco.toByteArray());
        salida.flush();
    }

    private String leerRespuesta(DataInputStream entrada) throws IOException {
        int longitudPaquete = leerVarInt(entrada);
        exigirTamanoRazonable(longitudPaquete);

        int idPaquete = leerVarInt(entrada);
        if (idPaquete != 0x00) {
            throw new IOException("Respuesta inesperada del servidor: paquete " + idPaquete);
        }

        int longitudCadena = leerVarInt(entrada);
        exigirTamanoRazonable(longitudCadena);

        byte[] bytes = new byte[longitudCadena];
        entrada.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void exigirTamanoRazonable(int longitud) throws IOException {
        if (longitud < 0 || longitud > MAX_RESPUESTA_BYTES) {
            throw new IOException("El servidor anuncio una respuesta de tamano no razonable: " + longitud);
        }
    }

    // --- VarInt --------------------------------------------------------------

    /**
     * Entero de longitud variable: 7 bits de datos por byte, el octavo indica si
     * hay continuacion. Es como Minecraft codifica casi todos sus numeros.
     */
    private void escribirVarInt(DataOutputStream salida, int valor) throws IOException {
        while ((valor & ~0x7F) != 0) {
            salida.writeByte((valor & 0x7F) | 0x80);
            valor >>>= 7;
        }
        salida.writeByte(valor);
    }

    private int leerVarInt(InputStream entrada) throws IOException {
        int resultado = 0;
        int desplazamiento = 0;

        while (true) {
            int byteLeido = entrada.read();
            if (byteLeido == -1) {
                throw new IOException("El servidor cerro la conexion a media respuesta");
            }
            resultado |= (byteLeido & 0x7F) << desplazamiento;

            if ((byteLeido & 0x80) == 0) {
                return resultado;
            }
            desplazamiento += 7;

            // Un VarInt legitimo nunca pasa de 5 bytes. Sin este corte, un
            // servidor hostil puede enviar bytes con el bit de continuacion
            // puesto para siempre y dejarnos girando en este bucle.
            if (desplazamiento >= 35) {
                throw new IOException("VarInt malformado");
            }
        }
    }

    private void escribirCadena(DataOutputStream salida, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        escribirVarInt(salida, bytes.length);
        salida.write(bytes);
    }

    private Integer entero(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor == null || !valor.isNumber() ? null : valor.asInt();
    }

    private String mensajeCorto(Exception e) {
        String mensaje = e.getMessage();
        if (mensaje == null) {
            return e.getClass().getSimpleName();
        }
        return mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
    }

    /**
     * @param jugadoresConectados null si el servidor no lo informa
     * @param version texto que anuncia el servidor. Informativo: es autorreportado
     *     y no se usa para el ranking.
     */
    public record Resultado(
            boolean enLinea,
            Integer jugadoresConectados,
            Integer jugadoresMaximos,
            Integer latenciaMs,
            String version,
            String error) {

        static Resultado enLinea(Integer conectados, Integer maximos, int latencia, String version) {
            return new Resultado(true, conectados, maximos, latencia, version, null);
        }

        static Resultado sinConexion(String error) {
            return new Resultado(false, null, null, null, null, error);
        }
    }
}
