package com.latinosmc.servers.ping.motd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y recupera un MOTD ya saneado.
 *
 * <p>El formato es propio y compacto en lugar de la serializacion automatica de
 * los registros. No es preferencia estetica: un MOTD con degradado son sesenta
 * tramos de un caracter cada uno, y con nombres largos —{@code "estilo":
 * {"color":..., "negrita":false, "cursiva":false, ...}} por tramo— eso pasa de
 * ocho kilobytes por una fila que en realidad guarda dos lineas de texto.
 *
 * <p>Se guarda en JSON y no como texto con codigos § porque los codigos § solo
 * saben expresar dieciseis colores, y los MOTD que mas se lo curran usan
 * hexadecimales para hacer degradados. Guardarlo en el formato viejo perderia
 * justo lo que da identidad a esos servidores (ADR-0027).
 *
 * <p>Formato: una lista de lineas, cada linea una lista de tramos.
 *
 * <pre>
 * [[{"t":"Mi ","c":16755200},{"t":"Servidor","c":16777215,"f":"b"}]]
 * </pre>
 *
 * <ul>
 *   <li>{@code t} — el texto
 *   <li>{@code c} — el color como entero RGB; ausente si es blanco
 *   <li>{@code f} — formato: b negrita, i cursiva, u subrayado, s tachado;
 *       ausente si no hay ninguno
 * </ul>
 */
public final class MotdJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MotdJson() {}

    public static String serializar(List<MotdMinecraft.Linea> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return null;
        }

        ArrayNode raiz = JSON.createArrayNode();
        for (MotdMinecraft.Linea linea : lineas) {
            ArrayNode nodoLinea = raiz.addArray();

            for (MotdMinecraft.Tramo tramo : linea.tramos()) {
                if (tramo.texto().isEmpty()) {
                    continue;
                }
                ObjectNode nodoTramo = nodoLinea.addObject();
                nodoTramo.put("t", tramo.texto());

                if (tramo.color() != MotdMinecraft.COLOR_POR_DEFECTO) {
                    nodoTramo.put("c", tramo.color());
                }
                String formato = formatoDe(tramo.estilo());
                if (!formato.isEmpty()) {
                    nodoTramo.put("f", formato);
                }
            }
        }
        return raiz.toString();
    }

    /**
     * @return las lineas guardadas; vacio si el valor es nulo o esta corrupto
     */
    public static List<MotdMinecraft.Linea> deserializar(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        try {
            JsonNode raiz = JSON.readTree(texto);
            if (!raiz.isArray()) {
                return List.of();
            }

            var lineas = new ArrayList<MotdMinecraft.Linea>();
            for (JsonNode nodoLinea : raiz) {
                var tramos = new ArrayList<MotdMinecraft.Tramo>();
                for (JsonNode nodoTramo : nodoLinea) {
                    String contenido = nodoTramo.path("t").asText("");
                    if (contenido.isEmpty()) {
                        continue;
                    }
                    int color = nodoTramo.has("c")
                            ? nodoTramo.get("c").asInt(MotdMinecraft.COLOR_POR_DEFECTO)
                            : MotdMinecraft.COLOR_POR_DEFECTO;
                    String formato = nodoTramo.path("f").asText("");

                    tramos.add(new MotdMinecraft.Tramo(
                            contenido,
                            new MotdMinecraft.Estilo(
                                    color,
                                    formato.indexOf('b') >= 0,
                                    formato.indexOf('i') >= 0,
                                    formato.indexOf('u') >= 0,
                                    formato.indexOf('s') >= 0)));
                }
                lineas.add(new MotdMinecraft.Linea(List.copyOf(tramos)));
            }
            return List.copyOf(lineas);

        } catch (Exception e) {
            // Un MOTD ilegible es una ficha sin MOTD, no una pagina rota. Esto se
            // lee al pintar el directorio: fallar aqui tumbaria la portada entera
            // por una fila mal guardada.
            return List.of();
        }
    }

    private static String formatoDe(MotdMinecraft.Estilo estilo) {
        var sb = new StringBuilder(4);
        if (estilo.negrita()) {
            sb.append('b');
        }
        if (estilo.cursiva()) {
            sb.append('i');
        }
        if (estilo.subrayado()) {
            sb.append('u');
        }
        if (estilo.tachado()) {
            sb.append('s');
        }
        return sb.toString();
    }
}
