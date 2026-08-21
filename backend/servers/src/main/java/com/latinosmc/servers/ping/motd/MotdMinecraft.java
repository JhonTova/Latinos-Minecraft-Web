package com.latinosmc.servers.ping.motd;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interpreta el MOTD que devuelve el Server List Ping.
 *
 * <p>El MOTD es lo que el jugador ve bajo el nombre del servidor en el menu
 * multijugador. Llega en el mismo JSON del que ya sacamos los jugadores
 * conectados, en el campo {@code description}, y puede venir de tres formas
 * distintas segun la version y el software del servidor:
 *
 * <ol>
 *   <li>Texto plano con codigos de seccion: {@code "§6Mi §fServidor"}
 *   <li>Un componente de chat: {@code {"text":"Mi ","color":"gold",
 *       "extra":[{"text":"Servidor","color":"white"}]}}
 *   <li>Una lista de componentes
 * </ol>
 *
 * <p>Las tres conviven hoy: un Paper moderno manda componentes, un Spigot viejo
 * manda texto plano, y hay plugins que mezclan las dos (componentes cuyo texto
 * lleva ademas codigos §). Se soportan todas porque el objetivo es que esto
 * funcione en el servidor que ya existe, no en el que nos gustaria.
 *
 * <p>Codigo especifico de Minecraft, aislado en su paquete adaptador: el dominio
 * no sabe que existe (CLAUDE.md, regla 13).
 *
 * <p><strong>Todo esto es entrada no confiable.</strong> Lo escribe el dueño del
 * servidor. No alimenta el ranking (ADR-0009) y pasa por
 * {@link SaneadorDeMotd} antes de enseñarse a nadie.
 */
public final class MotdMinecraft {

    /** El caracter de escape de Minecraft. Su nombre real es "signo de seccion". */
    public static final char SECCION = '§';

    /** Blanco. Es lo que pinta el cliente cuando no se dice otra cosa. */
    public static final int COLOR_POR_DEFECTO = 0xFFFFFF;

    /**
     * Los dieciseis colores clasicos.
     *
     * <p>Los valores son los del cliente, no una aproximacion: parte de la gracia
     * de esto es que el banner se vea igual que el menu del juego, y un "verde"
     * distinto se nota al lado del original.
     */
    private static final Map<Character, Integer> COLORES_LEGACY = Map.ofEntries(
            Map.entry('0', 0x000000),
            Map.entry('1', 0x0000AA),
            Map.entry('2', 0x00AA00),
            Map.entry('3', 0x00AAAA),
            Map.entry('4', 0xAA0000),
            Map.entry('5', 0xAA00AA),
            Map.entry('6', 0xFFAA00),
            Map.entry('7', 0xAAAAAA),
            Map.entry('8', 0x555555),
            Map.entry('9', 0x5555FF),
            Map.entry('a', 0x55FF55),
            Map.entry('b', 0x55FFFF),
            Map.entry('c', 0xFF5555),
            Map.entry('d', 0xFF55FF),
            Map.entry('e', 0xFFFF55),
            Map.entry('f', 0xFFFFFF));

    /** Los mismos colores, por el nombre que usan los componentes de chat. */
    private static final Map<String, Integer> COLORES_POR_NOMBRE = Map.ofEntries(
            Map.entry("black", 0x000000),
            Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00),
            Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000),
            Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00),
            Map.entry("gray", 0xAAAAAA),
            Map.entry("grey", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555),
            Map.entry("dark_grey", 0x555555),
            Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55),
            Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555),
            Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("white", 0xFFFFFF));

    /**
     * Tope de anidamiento de componentes.
     *
     * <p>Un componente puede llevar {@code extra} con mas componentes dentro, sin
     * limite en el protocolo. Un servidor hostil puede anidar diez mil niveles y
     * desbordar la pila del hilo que este analizando. El limite corta ahi: nadie
     * escribe un MOTD con treinta niveles de anidamiento a mano.
     */
    private static final int PROFUNDIDAD_MAXIMA = 32;

    /** Tope de caracteres. Un MOTD real son dos lineas de unos 45 caracteres. */
    private static final int CARACTERES_MAXIMOS = 512;

    private MotdMinecraft() {}

    /**
     * Analiza el campo {@code description} de la respuesta del ping.
     *
     * @return las lineas del MOTD, cada una como una lista de tramos con estilo
     *     propio; lista vacia si no hay MOTD legible
     */
    public static List<Linea> analizar(JsonNode descripcion) {
        if (descripcion == null || descripcion.isNull() || descripcion.isMissingNode()) {
            return List.of();
        }

        var acumulador = new ArrayList<Caracter>();
        recorrer(descripcion, Estilo.PREDETERMINADO, acumulador, 0);
        return repartirEnLineas(acumulador);
    }

    /** Analiza un MOTD que ya viene como texto plano con codigos §. */
    public static List<Linea> analizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        var acumulador = new ArrayList<Caracter>();
        aplanarTextoLegacy(texto, Estilo.PREDETERMINADO, acumulador);
        return repartirEnLineas(acumulador);
    }

    // --- Recorrido del arbol de componentes ----------------------------------

    private static void recorrer(
            JsonNode nodo, Estilo heredado, List<Caracter> destino, int profundidad) {

        if (nodo == null || profundidad > PROFUNDIDAD_MAXIMA || destino.size() >= CARACTERES_MAXIMOS) {
            return;
        }

        if (nodo.isTextual()) {
            aplanarTextoLegacy(nodo.asText(), heredado, destino);
            return;
        }

        if (nodo.isArray()) {
            // Una lista de componentes hereda el estilo del PRIMERO, que es como
            // lo interpreta el cliente. Aqui se simplifica: cada elemento hereda
            // del contexto. La diferencia solo se nota en MOTD retorcidos.
            for (JsonNode hijo : nodo) {
                recorrer(hijo, heredado, destino, profundidad + 1);
            }
            return;
        }

        if (!nodo.isObject()) {
            return;
        }

        Estilo propio = heredado.fusionarCon(nodo);

        JsonNode texto = nodo.get("text");
        if (texto != null && texto.isTextual()) {
            aplanarTextoLegacy(texto.asText(), propio, destino);
        }

        // Los componentes traducibles ({"translate":"..."}) no se resuelven: haria
        // falta el catalogo de idiomas del juego. Se ignoran en silencio; nadie
        // pone un MOTD traducible.

        JsonNode extra = nodo.get("extra");
        if (extra != null && extra.isArray()) {
            for (JsonNode hijo : extra) {
                recorrer(hijo, propio, destino, profundidad + 1);
            }
        }
    }

    /**
     * Convierte texto con codigos § en caracteres con estilo.
     *
     * <p>Se hace tambien dentro de los componentes modernos a proposito: es
     * frecuente que un plugin genere un componente cuyo texto lleva ademas
     * codigos §, y el cliente los interpreta. Si aqui no se hicieran, esos MOTD
     * saldrian con las letras "§6" a la vista.
     */
    private static void aplanarTextoLegacy(String texto, Estilo inicial, List<Caracter> destino) {
        Estilo actual = inicial;

        for (int i = 0; i < texto.length() && destino.size() < CARACTERES_MAXIMOS; i++) {
            char c = texto.charAt(i);

            if (c != SECCION || i + 1 >= texto.length()) {
                destino.add(new Caracter(c, actual));
                continue;
            }

            char codigo = Character.toLowerCase(texto.charAt(++i));
            actual = switch (codigo) {
                    // Un color reinicia el formato, igual que en el juego: §cTEXTO
                    // despues de §l sale rojo y SIN negrita.
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' ->
                    Estilo.PREDETERMINADO.conColor(COLORES_LEGACY.get(codigo));
                case 'l' -> actual.conNegrita();
                case 'm' -> actual.conTachado();
                case 'n' -> actual.conSubrayado();
                case 'o' -> actual.conCursiva();
                case 'r' -> inicial;
                    // §k es la animacion de glifos aleatorios. En una imagen fija no
                    // existe, asi que se ignora el codigo y se pinta el texto de
                    // debajo (ADR-0027).
                case 'k' -> actual;
                    // Codigo desconocido: el cliente se lo come sin pintar nada.
                default -> actual;};
        }
    }

    private static List<Linea> repartirEnLineas(List<Caracter> caracteres) {
        var lineas = new ArrayList<Linea>();
        var enCurso = new ArrayList<Caracter>();

        for (Caracter c : caracteres) {
            if (c.valor() == '\n') {
                lineas.add(new Linea(agrupar(enCurso)));
                enCurso = new ArrayList<>();
            } else {
                enCurso.add(c);
            }
        }
        lineas.add(new Linea(agrupar(enCurso)));

        // Un MOTD son dos lineas. Un servidor puede mandar veinte metiendo saltos
        // de linea; el cliente solo pinta las dos primeras y nosotros tambien.
        while (lineas.size() > 2) {
            lineas.remove(lineas.size() - 1);
        }
        while (!lineas.isEmpty() && lineas.get(lineas.size() - 1).estaVacia()) {
            lineas.remove(lineas.size() - 1);
        }
        return List.copyOf(lineas);
    }

    /** Junta caracteres consecutivos con el mismo estilo en un solo tramo. */
    private static List<Tramo> agrupar(List<Caracter> caracteres) {
        var tramos = new ArrayList<Tramo>();
        if (caracteres.isEmpty()) {
            return tramos;
        }

        Estilo estilo = caracteres.get(0).estilo();
        var texto = new StringBuilder();

        for (Caracter c : caracteres) {
            if (!c.estilo().equals(estilo)) {
                tramos.add(new Tramo(texto.toString(), estilo));
                texto = new StringBuilder();
                estilo = c.estilo();
            }
            texto.append(c.valor());
        }
        tramos.add(new Tramo(texto.toString(), estilo));
        return tramos;
    }

    // --- Modelo --------------------------------------------------------------

    /** Un caracter con el estilo que le toca. Representacion intermedia. */
    record Caracter(char valor, Estilo estilo) {}

    /**
     * Un trozo de texto con estilo uniforme.
     *
     * @param color RGB de 24 bits, ya resuelto (nunca el nombre ni el codigo)
     */
    public record Tramo(String texto, Estilo estilo) {
        public int color() {
            return estilo.color();
        }
    }

    /** Una linea del MOTD. Un MOTD tiene una o dos. */
    public record Linea(List<Tramo> tramos) {

        public boolean estaVacia() {
            return textoPlano().isBlank();
        }

        public String textoPlano() {
            var sb = new StringBuilder();
            tramos.forEach(t -> sb.append(t.texto()));
            return sb.toString();
        }
    }

    /**
     * Estilo de un tramo.
     *
     * <p>No lleva {@code obfuscated}: se descarta al analizar, porque es una
     * animacion y esto acaba en una imagen fija.
     */
    public record Estilo(int color, boolean negrita, boolean cursiva, boolean subrayado, boolean tachado) {

        public static final Estilo PREDETERMINADO =
                new Estilo(COLOR_POR_DEFECTO, false, false, false, false);

        Estilo conColor(int nuevo) {
            return new Estilo(nuevo, false, false, false, false);
        }

        Estilo conNegrita() {
            return new Estilo(color, true, cursiva, subrayado, tachado);
        }

        Estilo conCursiva() {
            return new Estilo(color, negrita, true, subrayado, tachado);
        }

        Estilo conSubrayado() {
            return new Estilo(color, negrita, cursiva, true, tachado);
        }

        Estilo conTachado() {
            return new Estilo(color, negrita, cursiva, subrayado, true);
        }

        /** Aplica encima los atributos que declare un componente de chat. */
        Estilo fusionarCon(JsonNode componente) {
            int nuevoColor = color;

            JsonNode nodoColor = componente.get("color");
            if (nodoColor != null && nodoColor.isTextual()) {
                Integer resuelto = resolverColor(nodoColor.asText());
                if (resuelto != null) {
                    nuevoColor = resuelto;
                }
            }

            return new Estilo(
                    nuevoColor,
                    booleano(componente, "bold", negrita),
                    booleano(componente, "italic", cursiva),
                    booleano(componente, "underlined", subrayado),
                    booleano(componente, "strikethrough", tachado));
        }

        private static boolean booleano(JsonNode nodo, String campo, boolean porDefecto) {
            JsonNode valor = nodo.get(campo);
            return valor != null && valor.isBoolean() ? valor.asBoolean() : porDefecto;
        }
    }

    /**
     * Resuelve un color de componente: nombre clasico o hexadecimal moderno.
     *
     * <p>El hexadecimal ({@code "#FF8800"}) es lo que usan los MOTD con degradado,
     * que hoy son mayoria en los servidores grandes: pintan cada letra de un color
     * ligeramente distinto. Sin esto, todos esos MOTD saldrian blancos y el banner
     * perderia justo lo que le da identidad al servidor.
     *
     * @return el RGB, o null si el valor no es un color reconocible
     */
    static Integer resolverColor(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim().toLowerCase();

        if (limpio.startsWith("#")) {
            if (limpio.length() != 7) {
                return null;
            }
            try {
                return Integer.parseInt(limpio.substring(1), 16);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return COLORES_POR_NOMBRE.get(limpio);
    }
}
