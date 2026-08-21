package com.latinosmc.servers.ping.motd;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Limpia el MOTD antes de que se publique en nuestro dominio.
 *
 * <p>El MOTD lo escribe el dueño del servidor. En cuanto lo adoptamos
 * automaticamente, ese texto pasa a ser contenido de nuestra web, y hay dos
 * cosas que no pueden pasar de largo.
 *
 * <p><strong>La primera es de producto.</strong> El MOTD de medio Minecraft dice
 * "vota en otrositio.com". Publicarlo tal cual seria pagar hosting para anunciar
 * a la competencia en nuestra propia portada. Se quitan los dominios que no sean
 * la direccion del propio servidor.
 *
 * <p><strong>La segunda es de seguridad.</strong> Los caracteres de control y las
 * marcas de direccion bidireccional pueden reordenar visualmente el texto de
 * alrededor: es el truco con el que un nombre se hace pasar por otro. En una
 * lista donde el jugador elige por lo que lee, eso es suplantacion.
 */
public final class SaneadorDeMotd {

    /**
     * Caracteres por linea.
     *
     * <p>El cliente del juego corta el MOTD por ancho en pixeles, no por numero
     * de caracteres. Este tope solo esta para que nada crezca sin limite; quien
     * recorta de verdad, y por ancho real, es el generador del banner.
     */
    private static final int CARACTERES_POR_LINEA = 64;

    /** Direcciones completas. Lo mas facil de detectar y lo mas descarado. */
    private static final Pattern URL = Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");

    /**
     * Dominios sueltos, sin protocolo. Es la forma habitual de escribirlos en un
     * MOTD: {@code play.ejemplo.com}, {@code discord.gg/abcd}.
     *
     * <p>Se exige que la ultima etiqueta este en una lista de extensiones
     * conocidas. Un patron generico de "algo.algo" marcaria como dominio cosas
     * como {@code ¡Bienvenido!.Entra ya} —el punto sin espacio detras es
     * frecuente— y borraria texto legitimo.
     */
    private static final Pattern DOMINIO = Pattern.compile(
            "(?i)\\b(?:[a-z0-9](?:[a-z0-9-]{0,30}[a-z0-9])?\\.)+"
                    + "(?:com|net|org|info|biz|pro|dev|app|io|gg|tv|me|xyz|top|club|fun|live|link"
                    + "|site|online|store|shop|space|website|world|cc|pw|es|mx|ar|cl|co|pe|ve|uy|py"
                    + "|bo|ec|do|gt|cr|pa|hn|ni|sv|cu|pr|br|pt|us|uk|eu|de|fr|it|nl|be|ch|at|se|no"
                    + "|fi|dk|pl|cz|sk|hu|ro|gr|ie|ru|ua|tr|ca|au|nz|jp|kr|cn|in|za|ma)"
                    + "\\b(?:/\\S*)?");

    /** Direcciones IPv4, con puerto opcional. La otra forma de anunciar otro servidor. */
    private static final Pattern IPV4 =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?\\b");

    /**
     * Caracteres que no se pintan pero cambian lo que se lee.
     *
     * <p>Los de control (C0 y C1) pueden romper el pintado; los de anulacion
     * bidireccional reordenan lo que ya estaba escrito, y los de ancho cero
     * permiten colar texto invisible entre letras para esquivar filtros.
     */
    private static final Pattern INVISIBLES =
            Pattern.compile("[\\p{Cntrl}\\p{Cf}\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]");

    private SaneadorDeMotd() {}

    /**
     * @param hostDelServidor la direccion registrada del servidor; su dominio y
     *     todo lo que cuelgue de el se respeta, porque es el suyo
     * @param dominiosPropios dominios de la plataforma, que tampoco se quitan
     */
    public static Resultado sanear(
            List<MotdMinecraft.Linea> lineas, String hostDelServidor, Set<String> dominiosPropios) {

        Set<String> permitidos = construirPermitidos(hostDelServidor, dominiosPropios);
        var quitados = new LinkedHashSet<String>();
        var salida = new ArrayList<MotdMinecraft.Linea>();

        for (MotdMinecraft.Linea linea : lineas) {
            int quitadosAntes = quitados.size();
            MotdMinecraft.Linea limpia = sanearLinea(linea, permitidos, quitados);

            // Si de esta linea se quito una direccion, se va la linea entera.
            //
            // Recortar solo el dominio deja frases mancas: "Vota en otro.net y
            // gana" se queda en "Vota en y en", que en el banner se lee como que
            // la plataforma esta rota, no como que el servidor anunciaba a la
            // competencia. Y una linea cuyo contenido era una direccion que hemos
            // borrado ya no dice nada: no hay nada que salvar.
            if (quitados.size() > quitadosAntes) {
                continue;
            }
            if (!limpia.estaVacia()) {
                salida.add(limpia);
            }
        }

        return new Resultado(List.copyOf(salida), List.copyOf(quitados));
    }

    // -------------------------------------------------------------------------

    private static MotdMinecraft.Linea sanearLinea(
            MotdMinecraft.Linea linea, Set<String> permitidos, Set<String> quitados) {

        // Se trabaja sobre la linea aplanada a caracteres, no tramo a tramo. Un
        // dominio puede repartirse entre varios tramos —"§bvota.§fejemplo.com" son
        // tres— y mirando cada tramo por separado ninguno parece un dominio.
        List<MotdMinecraft.Caracter> caracteres = aplanar(linea);
        String texto = plano(caracteres);

        boolean[] borrar = new boolean[caracteres.size()];

        // Los invisibles se van siempre y sin avisar: nadie los puso a proposito
        // y decirle al dueño que le hemos quitado un U+200B no le aporta nada.
        marcarTodo(INVISIBLES.matcher(texto), borrar);

        marcarDominiosAjenos(URL.matcher(texto), borrar, permitidos, quitados);
        marcarDominiosAjenos(DOMINIO.matcher(texto), borrar, permitidos, quitados);
        marcarDominiosAjenos(IPV4.matcher(texto), borrar, permitidos, quitados);

        var conservados = new ArrayList<MotdMinecraft.Caracter>();
        for (int i = 0; i < caracteres.size(); i++) {
            if (!borrar[i]) {
                conservados.add(caracteres.get(i));
            }
        }

        return rearmar(colapsarEspacios(conservados));
    }

    private static void marcarTodo(Matcher m, boolean[] borrar) {
        while (m.find()) {
            marcarRango(m, borrar);
        }
    }

    private static void marcarDominiosAjenos(
            Matcher m, boolean[] borrar, Set<String> permitidos, Set<String> quitados) {

        while (m.find()) {
            if (esPropio(m.group(), permitidos)) {
                continue;
            }
            quitados.add(m.group());
            marcarRango(m, borrar);
        }
    }

    private static void marcarRango(Matcher m, boolean[] borrar) {
        for (int i = m.start(); i < m.end() && i < borrar.length; i++) {
            borrar[i] = true;
        }
    }

    /**
     * Decide si una direccion encontrada en el MOTD es la del propio servidor.
     *
     * <p>Se compara por dominio registrable, no por cadena exacta: el servidor
     * registrado como {@code play.ejemplo.com} tiene derecho a anunciar
     * {@code ejemplo.com}, {@code tienda.ejemplo.com} o {@code discord.ejemplo.com}
     * — es su marca, no la de otro.
     */
    private static boolean esPropio(String encontrado, Set<String> permitidos) {
        String limpio = encontrado
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "");

        int corte = limpio.indexOf('/');
        if (corte >= 0) {
            limpio = limpio.substring(0, corte);
        }
        corte = limpio.indexOf(':');
        if (corte >= 0) {
            limpio = limpio.substring(0, corte);
        }

        return permitidos.contains(limpio) || permitidos.contains(registrable(limpio));
    }

    private static Set<String> construirPermitidos(String host, Set<String> propios) {
        var permitidos = new LinkedHashSet<String>();
        if (propios != null) {
            propios.stream()
                    .filter(d -> d != null && !d.isBlank())
                    .forEach(d -> permitidos.add(d.toLowerCase(Locale.ROOT)));
        }
        if (host != null && !host.isBlank()) {
            String limpio = host.toLowerCase(Locale.ROOT).trim();
            permitidos.add(limpio);
            permitidos.add(registrable(limpio));
        }
        return permitidos;
    }

    /**
     * Dominio registrable: las dos ultimas etiquetas.
     *
     * <p>Aproximacion deliberada. Lo exacto exigiria la lista publica de sufijos
     * —esa que sabe que {@code co.uk} es un sufijo y {@code co.com} no— que es un
     * archivo de miles de lineas que hay que mantener al dia. El unico efecto de
     * fallar aqui es dejar pasar el dominio de un tercero bajo un sufijo compuesto,
     * y para eso esta el bloqueo administrativo del ADR-0027.
     */
    private static String registrable(String dominio) {
        String[] partes = dominio.split("\\.");
        if (partes.length < 2) {
            return dominio;
        }
        return partes[partes.length - 2] + "." + partes[partes.length - 1];
    }

    /**
     * Deja un solo espacio seguido y recorta la linea.
     *
     * <p>Hace falta porque al quitar un dominio queda el hueco: "Vota en
     * ejemplo.com y gana" se convertiria en "Vota en  y gana", con dos espacios y
     * una frase manca. No se puede arreglar la frase, pero si el hueco.
     */
    private static List<MotdMinecraft.Caracter> colapsarEspacios(List<MotdMinecraft.Caracter> entrada) {
        var salida = new ArrayList<MotdMinecraft.Caracter>();
        boolean espacioPrevio = true; // arranca en true para comerse los del principio

        for (MotdMinecraft.Caracter c : entrada) {
            boolean esEspacio = Character.isWhitespace(c.valor());
            if (esEspacio && espacioPrevio) {
                continue;
            }
            salida.add(esEspacio ? new MotdMinecraft.Caracter(' ', c.estilo()) : c);
            espacioPrevio = esEspacio;
        }

        while (!salida.isEmpty() && salida.get(salida.size() - 1).valor() == ' ') {
            salida.remove(salida.size() - 1);
        }
        return salida.size() > CARACTERES_POR_LINEA ? salida.subList(0, CARACTERES_POR_LINEA) : salida;
    }

    // --- Conversiones entre linea y lista de caracteres ----------------------

    private static List<MotdMinecraft.Caracter> aplanar(MotdMinecraft.Linea linea) {
        var caracteres = new ArrayList<MotdMinecraft.Caracter>();
        for (MotdMinecraft.Tramo tramo : linea.tramos()) {
            for (char c : tramo.texto().toCharArray()) {
                caracteres.add(new MotdMinecraft.Caracter(c, tramo.estilo()));
            }
        }
        return caracteres;
    }

    private static String plano(List<MotdMinecraft.Caracter> caracteres) {
        var sb = new StringBuilder(caracteres.size());
        caracteres.forEach(c -> sb.append(c.valor()));
        return sb.toString();
    }

    private static MotdMinecraft.Linea rearmar(List<MotdMinecraft.Caracter> caracteres) {
        var tramos = new ArrayList<MotdMinecraft.Tramo>();
        if (caracteres.isEmpty()) {
            return new MotdMinecraft.Linea(tramos);
        }

        MotdMinecraft.Estilo estilo = caracteres.get(0).estilo();
        var texto = new StringBuilder();

        for (MotdMinecraft.Caracter c : caracteres) {
            if (!c.estilo().equals(estilo)) {
                tramos.add(new MotdMinecraft.Tramo(texto.toString(), estilo));
                texto = new StringBuilder();
                estilo = c.estilo();
            }
            texto.append(c.valor());
        }
        tramos.add(new MotdMinecraft.Tramo(texto.toString(), estilo));
        return new MotdMinecraft.Linea(tramos);
    }

    /**
     * @param lineas el MOTD ya limpio, listo para pintar
     * @param quitados lo que se elimino, para poder enseñarselo al dueño en su
     *     panel; sin esto, veria desaparecer texto de su MOTD sin saber por que
     */
    public record Resultado(List<MotdMinecraft.Linea> lineas, List<String> quitados) {

        public boolean estaVacio() {
            return lineas.isEmpty();
        }

        /** Para guardarlo y poder volver a pintarlo, en el banner y como texto en la web. */
        public String comoJson() {
            return MotdJson.serializar(lineas);
        }

        /** Sin formato. Sirve para el texto alternativo de la imagen y para el hash. */
        public String textoPlano() {
            var sb = new StringBuilder();
            for (MotdMinecraft.Linea linea : lineas) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(linea.textoPlano());
            }
            return sb.toString().trim();
        }
    }
}
