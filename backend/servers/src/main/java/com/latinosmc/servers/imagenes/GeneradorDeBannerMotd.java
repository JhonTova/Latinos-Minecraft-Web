package com.latinosmc.servers.imagenes;

import com.latinosmc.servers.ping.motd.MotdMinecraft;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Dibuja el banner de un servidor con el aspecto del menu multijugador del
 * juego: el icono a la izquierda y el MOTD a la derecha, con sus colores.
 *
 * <p>Existe para el dueño que no sube banner (ADR-0027). Su servidor ya tiene
 * identidad visual —el icono y el MOTD que enseña el menu del juego— y el
 * jugador la reconoce. Solo hay que ir a buscarla y pintarla.
 *
 * <p><strong>Aqui no se pinta ningun dato que cambie.</strong> Ni jugadores
 * conectados, ni ping, ni barras de señal. El banner se regenera como mucho cada
 * seis horas, asi que un numero cocido dentro estaria casi siempre desfasado —y
 * seria NUESTRA imagen dando un dato falso sobre un servidor ajeno. Lo que
 * cambia se pinta encima en HTML, donde siempre es el de ahora.
 *
 * <p>Se genera al doble de resolucion y se muestra escalado con pixeles duros.
 * En una estetica de pixel eso no es una concesion: es exactamente el aspecto que
 * se busca.
 */
@Component
public class GeneradorDeBannerMotd {

    /** Medidas logicas, las del hueco del banner en la ficha (ADR-0017). */
    public static final int ANCHO = 1200;

    public static final int ALTO = 300;

    /** Se dibuja al doble y se enseña a la mitad: nitido en pantallas densas. */
    public static final int ESCALA = 2;

    private static final int MARGEN = 28 * ESCALA;
    private static final int ICONO_LADO = 168 * ESCALA;
    private static final int HUECO_TRAS_ICONO = 32 * ESCALA;

    /**
     * Tamaños de letra candidatos, de mayor a menor.
     *
     * <p>Multiplos de ocho a proposito: Monocraft es una fuente de pixel, y en
     * tamaños que no son multiplo de su rejilla los trazos salen de grosor
     * desigual. Con el suavizado desactivado —que es lo que da el aspecto de
     * pixel— eso se ve muchisimo.
     */
    private static final int[] TAMANOS = escalar(48, 40, 36, 32, 28, 24, 20, 16, 12);

    /**
     * Techo del tamaño de letra segun cuantas lineas haya.
     *
     * <p>Sin techo, un MOTD de una palabra —"Survival"— se estiraria hasta ocupar
     * el ancho entero con letras enormes y no pareceria un servidor, pareceria un
     * cartel. Con dos lineas el techo baja: dos lineas grandes se comen el alto y
     * quedan pegadas a los bordes.
     */
    private static final int TAMANO_MAXIMO_UNA_LINEA = 48 * ESCALA;

    private static final int TAMANO_MAXIMO_DOS_LINEAS = 32 * ESCALA;

    private static final float INTERLINEADO = 1.45f;

    /**
     * Sombra del texto, la firma visual del juego.
     *
     * <p>El cliente pinta cada letra dos veces: primero desplazada abajo a la
     * derecha con el color dividido por cuatro, y encima la letra real. Sin esto
     * el texto se ve plano y deja de parecer Minecraft al instante.
     */
    private static final int DESPLAZAMIENTO_SOMBRA = 3 * ESCALA;

    private static final int FACTOR_SOMBRA = 4;

    // --- Fondo ---------------------------------------------------------------

    /**
     * Semilla fija: todos los banners comparten el mismo fondo.
     *
     * <p>Es lo que hace el juego —todas las entradas del menu se dibujan sobre el
     * mismo fondo— y ademas hace que generar el mismo banner dos veces produzca
     * exactamente los mismos bytes, que es lo que permite comparar por hash.
     */
    private static final long SEMILLA_FONDO = 0x1A7105CAFEL;

    private static final Color PIEDRA = new Color(0x26, 0x26, 0x2B);

    /**
     * Lado del grano.
     *
     * <p>Afinarlo hasta un pixel logico se probo y sale caro: el grano fino
     * multiplica los datos distintos que hay que guardar y el archivo se vuelve a
     * ir a trescientos kilobytes. Lo que oculta la repeticion es el contraste, no
     * el detalle (ver {@link #PASO_DE_GRANO}).
     */
    private static final int GRANO = 4 * ESCALA;

    /**
     * El fondo se teje repitiendo una baldosa pequeña, y a saltos de tono.
     *
     * <p>No es estetica, es peso, y costo. La primera version pintaba ruido libre
     * de veintitres tonos sobre un degradado continuo: miles de colores distintos
     * y nada que se repita, que es justo lo que un PNG no puede comprimir. Cada
     * banner pesaba 330 KB. Una pagina del directorio con veinte tarjetas eran
     * seis megabytes de fondo de piedra, y en un movil con datos eso es un
     * visitante que se va antes de ver nada — el mismo argumento que el limite de
     * los GIF en el ADR-0017.
     *
     * <p>Repitiendo una baldosa de 32 pixeles el compresor encuentra la misma
     * secuencia una y otra vez dentro de su ventana de busqueda, y el archivo baja
     * a unos 30 KB. Es ademas lo que hace el propio juego, que dibuja sus fondos
     * repitiendo texturas de 16 pixeles: a la vista es piedra, no un patron.
     */
    private static final int BALDOSA = 16 * ESCALA;

    private static final int TONOS_DE_GRANO = 5;

    /**
     * Separacion entre tonos.
     *
     * <p>Lo que hace invisible una textura repetida no es el tamaño de la baldosa,
     * es el contraste. Con seis de separacion la rejilla se veia clarisima aunque
     * la baldosa fuera grande; con dos, sobre un fondo casi negro, no se distingue
     * y ademas permite usar la baldosa pequeña, que es la que comprime bien.
     */
    private static final int PASO_DE_GRANO = 2;

    private static final int BANDAS_DE_DEGRADADO = 8;

    private static final float OSCURECIDO_MAXIMO = 0.30f;

    /**
     * @param motd las lineas del MOTD ya saneadas; puede estar vacio
     * @param iconoPng el icono del servidor tal como lo devolvio el ping, ya
     *     reprocesado a PNG; null si el servidor no publica ninguno
     * @param nombreDeRespaldo se pinta solo si no hay MOTD; un banner con el
     *     fondo vacio seria peor que no tener banner
     * @return PNG de {@value #ANCHO}x{@value #ALTO} logicos, al doble de tamaño
     */
    public byte[] generar(List<MotdMinecraft.Linea> motd, byte[] iconoPng, String nombreDeRespaldo) {
        int ancho = ANCHO * ESCALA;
        int alto = ALTO * ESCALA;

        BufferedImage lienzo = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = lienzo.createGraphics();
        try {
            // Suavizado desactivado en todo: es una imagen de pixel. Con
            // interpolacion el icono de 64x64 saldria emborronado y el texto
            // perderia el borde duro que lo hace reconocible.
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            pintarFondo(g, ancho, alto);

            BufferedImage icono = decodificarIcono(iconoPng);
            List<MotdMinecraft.Linea> lineas = motd == null || motd.isEmpty()
                    ? List.of(lineaDeRespaldo(nombreDeRespaldo))
                    : motd;

            int anchoDelIcono = icono == null ? 0 : ICONO_LADO + HUECO_TRAS_ICONO;
            int disponible = ancho - MARGEN * 2 - anchoDelIcono;

            int tamano = elegirTamano(g, lineas, disponible);
            int anchoDelTexto = anchoDelBloque(g, lineas, tamano);

            // El conjunto entero —icono y texto— se centra en el banner. Un MOTD
            // largo lo llena y queda pegado a la izquierda, como en el menu del
            // juego; uno de una palabra queda centrado con su icono al lado, en
            // vez de dejar medio banner en blanco entre los dos.
            int x = Math.max(MARGEN, (ancho - anchoDelIcono - anchoDelTexto) / 2);

            if (icono != null) {
                pintarIcono(g, icono, x, (alto - ICONO_LADO) / 2);
            }
            pintarTexto(g, lineas, x + anchoDelIcono, ancho - MARGEN, alto, tamano);
            pintarMarco(g, ancho, alto);

        } finally {
            g.dispose();
        }
        return aPng(lienzo);
    }

    // --- Fondo ---------------------------------------------------------------

    /**
     * Fondo de piedra oscura, generado por codigo.
     *
     * <p>Generado y no una textura del juego: las texturas de Minecraft son de
     * Mojang y no se pueden redistribuir. Un ruido por bloques de pixel da el
     * mismo efecto de superficie mineral y es enteramente nuestro.
     */
    private void pintarFondo(Graphics2D g, int ancho, int alto) {
        BufferedImage baldosa = tejerBaldosa();
        for (int y = 0; y < alto; y += BALDOSA) {
            for (int x = 0; x < ancho; x += BALDOSA) {
                g.drawImage(baldosa, x, y, null);
            }
        }

        // Oscurecido progresivo hacia abajo: sin el, el fondo se lee como una
        // trama plana y el texto claro pierde separacion en la mitad inferior.
        int altoBanda = (int) Math.ceil((double) alto / BANDAS_DE_DEGRADADO);
        for (int banda = 0; banda < BANDAS_DE_DEGRADADO; banda++) {
            float intensidad = OSCURECIDO_MAXIMO * (banda + 0.5f) / BANDAS_DE_DEGRADADO;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensidad));
            g.setColor(Color.BLACK);
            g.fillRect(0, banda * altoBanda, ancho, altoBanda);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** La baldosa de piedra que se repite por todo el fondo. */
    private BufferedImage tejerBaldosa() {
        Color[] tonos = new Color[TONOS_DE_GRANO];
        for (int i = 0; i < TONOS_DE_GRANO; i++) {
            int delta = (i - TONOS_DE_GRANO / 2) * PASO_DE_GRANO;
            tonos[i] = new Color(
                    acotar(PIEDRA.getRed() + delta),
                    acotar(PIEDRA.getGreen() + delta),
                    acotar(PIEDRA.getBlue() + delta));
        }

        var baldosa = new BufferedImage(BALDOSA, BALDOSA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = baldosa.createGraphics();
        try {
            var azar = new Random(SEMILLA_FONDO);
            for (int y = 0; y < BALDOSA; y += GRANO) {
                for (int x = 0; x < BALDOSA; x += GRANO) {
                    g.setColor(tonos[azar.nextInt(TONOS_DE_GRANO)]);
                    g.fillRect(x, y, GRANO, GRANO);
                }
            }
        } finally {
            g.dispose();
        }
        return baldosa;
    }

    /** Borde negro con un filo claro por dentro: el biselado de la interfaz del juego. */
    private void pintarMarco(Graphics2D g, int ancho, int alto) {
        int grosor = 2 * ESCALA;

        g.setColor(new Color(0, 0, 0, 200));
        for (int i = 0; i < grosor; i++) {
            g.drawRect(i, i, ancho - 1 - i * 2, alto - 1 - i * 2);
        }
        g.setColor(new Color(255, 255, 255, 22));
        g.drawRect(grosor, grosor, ancho - 1 - grosor * 2, alto - 1 - grosor * 2);
    }

    // --- Icono ---------------------------------------------------------------

    private void pintarIcono(Graphics2D g, BufferedImage icono, int x, int y) {
        // Placa oscura detras: muchos iconos tienen fondo transparente, y sin ella
        // el dibujo flota sobre la piedra y se lee como un recorte mal hecho.
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRect(x - 3 * ESCALA, y - 3 * ESCALA, ICONO_LADO + 6 * ESCALA, ICONO_LADO + 6 * ESCALA);

        g.drawImage(icono, x, y, ICONO_LADO, ICONO_LADO, null);

        g.setColor(new Color(0, 0, 0, 140));
        g.drawRect(x - 3 * ESCALA, y - 3 * ESCALA, ICONO_LADO + 6 * ESCALA - 1, ICONO_LADO + 6 * ESCALA - 1);
    }

    private BufferedImage decodificarIcono(byte[] png) {
        if (png == null || png.length == 0) {
            return null;
        }
        try {
            // Ya viene reprocesado por ProcesadorDeImagenes: aqui solo se decodifica.
            return ImageIO.read(new java.io.ByteArrayInputStream(png));
        } catch (IOException | RuntimeException e) {
            // Un icono ilegible da un banner sin icono, no un banner que falla.
            return null;
        }
    }

    // --- Texto ---------------------------------------------------------------

    private void pintarTexto(
            Graphics2D g, List<MotdMinecraft.Linea> lineas, int desde, int hasta, int alto, int tamano) {

        var metricas = g.getFontMetrics(FuentePixel.normal(tamano));
        int interlineado = Math.round(tamano * INTERLINEADO);

        // El alto del bloque es la distancia entre lineas mas lo que ocupa una
        // linea de verdad, no el interlineado por el numero de lineas: eso ultimo
        // cuenta de mas el hueco que sobra debajo de la ultima y deja el texto
        // visiblemente descentrado hacia arriba.
        int altoBloque = interlineado * (lineas.size() - 1) + metricas.getAscent() + metricas.getDescent();
        int y = (alto - altoBloque) / 2 + metricas.getAscent();

        for (MotdMinecraft.Linea linea : lineas) {
            pintarLinea(g, linea, desde, y, tamano, hasta - desde);
            y += interlineado;
        }
    }

    /** Lo que ocupa a lo ancho la linea mas larga. */
    private int anchoDelBloque(Graphics2D g, List<MotdMinecraft.Linea> lineas, int tamano) {
        int ancho = 0;
        for (MotdMinecraft.Linea linea : lineas) {
            ancho = Math.max(ancho, anchoDe(g, linea, tamano));
        }
        return ancho;
    }

    /**
     * El mayor tamaño con el que cabe la linea mas larga.
     *
     * <p>Un MOTD puede ser "Survival" o cuarenta y cinco caracteres con emojis. Un
     * tamaño fijo obligaria a elegir entre desperdiciar medio banner en el primer
     * caso o cortar el segundo, y en ese caso el dueño ve su MOTD mutilado en
     * nuestra web.
     */
    private int elegirTamano(Graphics2D g, List<MotdMinecraft.Linea> lineas, int disponible) {
        int techo = lineas.size() >= 2 ? TAMANO_MAXIMO_DOS_LINEAS : TAMANO_MAXIMO_UNA_LINEA;

        for (int tamano : TAMANOS) {
            if (tamano <= techo && todasCaben(g, lineas, tamano, disponible)) {
                return tamano;
            }
        }
        // Ni con el mas pequeño: se usa ese y se recorta con puntos suspensivos.
        return TAMANOS[TAMANOS.length - 1];
    }

    private boolean todasCaben(Graphics2D g, List<MotdMinecraft.Linea> lineas, int tamano, int disponible) {
        for (MotdMinecraft.Linea linea : lineas) {
            if (anchoDe(g, linea, tamano) > disponible) {
                return false;
            }
        }
        return true;
    }

    private int anchoDe(Graphics2D g, MotdMinecraft.Linea linea, int tamano) {
        int ancho = 0;
        for (MotdMinecraft.Tramo tramo : linea.tramos()) {
            ancho += anchoDeTramo(g, tramo, tamano);
        }
        return ancho;
    }

    private int anchoDeTramo(Graphics2D g, MotdMinecraft.Tramo tramo, int tamano) {
        Font fuente = fuenteDe(tramo.estilo(), tamano);
        FontRenderContext contexto = g.getFontRenderContext();
        int ancho = (int) fuente.getStringBounds(tramo.texto(), contexto).getWidth();
        // La negrita del juego se hace pintando la letra dos veces desplazada un
        // pixel, asi que ocupa ese pixel de mas por caracter.
        return tramo.estilo().negrita() ? ancho + tramo.texto().length() * ESCALA : ancho;
    }

    private void pintarLinea(
            Graphics2D g, MotdMinecraft.Linea linea, int x, int y, int tamano, int disponible) {

        int limite = x + disponible;

        for (MotdMinecraft.Tramo tramo : linea.tramos()) {
            if (x >= limite) {
                return;
            }
            String texto = recortarSiHaceFalta(g, tramo, tamano, limite - x);
            if (texto.isEmpty()) {
                continue;
            }

            Font fuente = fuenteDe(tramo.estilo(), tamano);
            g.setFont(fuente);

            // Sombra primero, texto encima. Es el orden del cliente del juego.
            g.setColor(new Color(sombraDe(tramo.color())));
            dibujar(g, texto, x + DESPLAZAMIENTO_SOMBRA, y + DESPLAZAMIENTO_SOMBRA, tramo.estilo().negrita());

            g.setColor(new Color(tramo.color()));
            dibujar(g, texto, x, y, tramo.estilo().negrita());

            int ancho = (int) fuente.getStringBounds(texto, g.getFontRenderContext()).getWidth();
            pintarLineasDeFormato(g, tramo.estilo(), x, y, ancho, tamano);
            x += ancho + (tramo.estilo().negrita() ? texto.length() * ESCALA : 0);
        }
    }

    private void dibujar(Graphics2D g, String texto, int x, int y, boolean negrita) {
        g.drawString(texto, x, y);
        if (negrita) {
            // El juego no tiene glifos en negrita: pinta el mismo dos veces
            // desplazado un pixel. Con una fuente que tampoco tiene variante
            // negrita, imitarlo sale mas fiel que dejar que el sistema la simule.
            g.drawString(texto, x + ESCALA, y);
        }
    }

    private void pintarLineasDeFormato(
            Graphics2D g, MotdMinecraft.Estilo estilo, int x, int y, int ancho, int tamano) {

        int grosor = Math.max(ESCALA, tamano / 16);

        if (estilo.tachado()) {
            g.fillRect(x, y - Math.round(tamano * 0.28f), ancho, grosor);
        }
        if (estilo.subrayado()) {
            g.fillRect(x, y + grosor, ancho + grosor, grosor);
        }
    }

    private Font fuenteDe(MotdMinecraft.Estilo estilo, int tamano) {
        return estilo.cursiva() ? FuentePixel.cursiva(tamano) : FuentePixel.normal(tamano);
    }

    /**
     * Recorta con puntos suspensivos lo que no quepa.
     *
     * <p>Solo actua cuando ya se ha bajado al tamaño mas pequeño y aun asi sobra
     * texto. Cortar a medio caracter dejaria el banner con una palabra partida,
     * que se lee como un error de la plataforma y no como un MOTD demasiado largo.
     */
    private String recortarSiHaceFalta(
            Graphics2D g, MotdMinecraft.Tramo tramo, int tamano, int espacio) {

        if (anchoDeTramo(g, tramo, tamano) <= espacio) {
            return tramo.texto();
        }

        Font fuente = fuenteDe(tramo.estilo(), tamano);
        FontRenderContext contexto = g.getFontRenderContext();
        String texto = tramo.texto();

        while (!texto.isEmpty()
                && fuente.getStringBounds(texto + "...", contexto).getWidth() > espacio) {
            texto = texto.substring(0, texto.length() - 1);
        }
        return texto.isEmpty() ? "" : texto + "...";
    }

    private MotdMinecraft.Linea lineaDeRespaldo(String nombre) {
        String texto = nombre == null || nombre.isBlank() ? "Servidor de Minecraft" : nombre.trim();
        return new MotdMinecraft.Linea(
                List.of(new MotdMinecraft.Tramo(texto, MotdMinecraft.Estilo.PREDETERMINADO)));
    }

    // --- Utilidades ----------------------------------------------------------

    /** El color de la sombra del juego: cada componente dividido por cuatro. */
    private int sombraDe(int color) {
        return ((color >> 16 & 0xFF) / FACTOR_SOMBRA) << 16
                | ((color >> 8 & 0xFF) / FACTOR_SOMBRA) << 8
                | (color & 0xFF) / FACTOR_SOMBRA;
    }

    private int acotar(int valor) {
        return Math.max(0, Math.min(255, valor));
    }

    /**
     * Pasa medidas logicas a pixeles reales.
     *
     * <p>Todas las constantes de este generador estan en unidades logicas —las del
     * hueco de 1200x300— y se multiplican por la escala aqui. Escrito al reves,
     * cambiar {@link #ESCALA} dejaria el texto con el tamaño de la escala anterior
     * sobre un lienzo del doble, y no habria forma de darse cuenta salvo mirando
     * la imagen.
     */
    private static int[] escalar(int... tamanos) {
        int[] escalados = new int[tamanos.length];
        for (int i = 0; i < tamanos.length; i++) {
            escalados[i] = tamanos[i] * ESCALA;
        }
        return escalados;
    }

    private byte[] aPng(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo codificar el banner generado", e);
        }
    }
}
