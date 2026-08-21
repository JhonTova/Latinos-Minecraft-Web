package com.latinosmc.servers;

import com.latinosmc.shared.DomainException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Valida y reprocesa las imagenes subidas (ADR-0017).
 *
 * <p>La regla de fondo: <strong>nunca se guarda el archivo que llego</strong>. Se
 * decodifica a pixeles y se vuelve a codificar. Lo que sobreviva a ese viaje son
 * pixeles y nada mas.
 *
 * <p>Eso descarta de golpe una familia entera de ataques: cargas utiles
 * escondidas en metadatos, archivos poliglota que son PNG y HTML a la vez, y
 * datos pegados detras del final del formato. Tambien borra la geolocalizacion
 * que llevan las fotos hechas con movil, que la gente sube sin saber que la
 * lleva.
 */
@Component
public class ProcesadorDeImagenes {

    public static final int ICONO_LADO = 128;
    public static final int BANNER_ANCHO_MAX = 1200;
    public static final int BANNER_ALTO_MAX = 300;

    public static final long MAX_ICONO_BYTES = 2L * 1024 * 1024;
    public static final long MAX_BANNER_BYTES = 4L * 1024 * 1024;

    /**
     * Limite agresivo a proposito (ADR-0017). Un GIF animado "normal" pesa de 3 a
     * 8 MB; con cinco destacados en portada serian 40 MB de descarga. En un movil
     * con datos, eso es un visitante que se va.
     */
    public static final long MAX_GIF_BYTES = 1024 * 1024;

    /** Lado maximo del logotipo del sitio. Se enseña grande en la portada. */
    public static final int LOGO_LADO = 512;

    /**
     * Lado del icono que anuncia un servidor en su ping.
     *
     * <p>El protocolo lo fija en 64x64 y el cliente del juego rechaza cualquier
     * otra cosa. Se reprocesa a ese tamaño exacto para que el aspecto pixelado sea
     * el mismo que en el menu multijugador (ADR-0027).
     */
    public static final int FAVICON_LADO = 64;

    /**
     * Tope del icono que llega por el ping.
     *
     * <p>Mucho mas estrecho que el de una subida: un icono de 64x64 en PNG no pasa
     * de unos 16 KB ni siendo ruido puro. Lo que llegue por encima de esto no es un
     * icono de servidor.
     */
    public static final long MAX_FAVICON_BYTES = 64 * 1024;

    /**
     * Tope de dimensiones ANTES de decodificar.
     *
     * <p>Esto no es una comodidad, es lo que impide una bomba de descompresion. Un
     * PNG de treinta kilobytes puede declarar 30000x30000 pixeles y ser un
     * rectangulo de un solo color: se comprime a nada y se descomprime a 3,6 GB.
     * Al decodificarlo, Java reserva la trama entera antes de que nadie pueda
     * mirar su tamaño, y el proceso muere.
     *
     * <p>Por eso la cabecera se lee primero y se rechaza por dimensiones sin
     * llegar a decodificar ni un pixel. El limite de bytes del archivo no protege
     * de esto: precisamente el ataque consiste en que el archivo sea diminuto.
     *
     * <p>Diecisiete megapixeles dan de sobra: una foto de movil de 12 MP
     * (4000x3000) entra sin problema.
     */
    private static final int LADO_MAXIMO_ENTRADA = 6000;

    private static final long PIXELES_MAXIMOS_ENTRADA = 17_000_000L;

    /** Proporcion 1.91:1, la que esperan Discord, WhatsApp, Twitter y Facebook. */
    public static final int SOCIAL_ANCHO = 1200;

    public static final int SOCIAL_ALTO = 630;

    /** Firmas de archivo. La extension y el Content-Type los pone el cliente: no valen nada. */
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_GIF = {'G', 'I', 'F', '8'};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};

    /** Icono: siempre PNG cuadrado de 128x128. */
    public Resultado procesarIcono(byte[] original) {
        exigirTamano(original, MAX_ICONO_BYTES, "El icono no puede pasar de 2 MB.");

        BufferedImage imagen = decodificar(original);
        BufferedImage cuadrada = redimensionar(imagen, ICONO_LADO, ICONO_LADO);
        return new Resultado(codificarPng(cuadrada), "image/png", ICONO_LADO, ICONO_LADO);
    }

    /**
     * Banner: PNG reprocesado, o GIF conservado tal cual si esta dentro del limite.
     *
     * <p>El GIF animado no se puede reprocesar con las herramientas de la
     * biblioteca estandar sin perder la animacion, asi que se valida a fondo y se
     * guarda como esta. Es la deuda registrada en el ADR-0017.
     */
    public Resultado procesarBanner(byte[] original) {
        if (esGif(original)) {
            exigirTamano(
                    original,
                    MAX_GIF_BYTES,
                    "Un banner animado no puede pasar de 1 MB. Un GIF más pesado hace que tu ficha "
                            + "tarde en cargar y los visitantes se van antes de verla.");
            // Se decodifica solo para confirmar que es un GIF de verdad y no algo
            // disfrazado. El resultado se descarta: se guarda el original.
            decodificar(original);
            return new Resultado(original, "image/gif", null, null);
        }

        exigirTamano(original, MAX_BANNER_BYTES, "El banner no puede pasar de 4 MB.");

        BufferedImage imagen = decodificar(original);
        BufferedImage ajustada = ajustarADentroDe(imagen, BANNER_ANCHO_MAX, BANNER_ALTO_MAX);
        return new Resultado(
                codificarPng(ajustada), "image/png", ajustada.getWidth(), ajustada.getHeight());
    }

    /**
     * Logotipo del sitio.
     *
     * <p>Hasta 512 px de lado. Mas grande que un icono de servidor porque este se
     * enseña grande en la portada: a 128 px se veria borroso justo en el sitio
     * donde mas se mira.
     *
     * <p>Se conserva la proporcion en lugar de recortar a cuadrado. Un logotipo
     * apaisado recortado por el centro pierde media marca, y el que lo sube no
     * entiende por que.
     */
    public Resultado procesarLogo(byte[] original) {
        exigirTamano(original, MAX_ICONO_BYTES, "El logotipo no puede pasar de 2 MB.");

        BufferedImage imagen = decodificar(original);
        BufferedImage ajustada = ajustarADentroDe(imagen, LOGO_LADO, LOGO_LADO);
        return new Resultado(codificarPng(ajustada), "image/png", ajustada.getWidth(), ajustada.getHeight());
    }

    /**
     * Icono leido del ping de un servidor (ADR-0027).
     *
     * <p>Pasa por el mismo reprocesado que una subida, y no es una formalidad:
     * estos bytes los controla el dueño del servidor —que es justo quien tendria
     * el incentivo para abusar— y acabarian servidos desde nuestro dominio. Venir
     * "de un ping" y no "de un formulario" no lo hace mas de fiar; si acaso, menos,
     * porque nadie ha pulsado un boton para enviarlo.
     *
     * <p>Se reprocesa a 64x64 exactos, que es lo que fija el protocolo, y no a los
     * 128 del icono subido: ampliar 64 a 128 solo produce una imagen borrosa.
     */
    public Resultado procesarFavicon(byte[] original) {
        exigirTamano(original, MAX_FAVICON_BYTES, "El icono del servidor es demasiado grande.");

        BufferedImage imagen = decodificar(original);
        BufferedImage cuadrada = redimensionar(imagen, FAVICON_LADO, FAVICON_LADO);
        return new Resultado(codificarPng(cuadrada), "image/png", FAVICON_LADO, FAVICON_LADO);
    }

    /**
     * Imagen social: la que se ve al compartir un enlace en Discord o WhatsApp.
     *
     * <p>1200x630 no es un capricho: es la proporcion 1.91:1 que esperan Discord,
     * WhatsApp, Twitter y Facebook. Una imagen con otra forma se recorta por el
     * centro, y ahi es donde suele estar el logotipo.
     *
     * <p>Siempre se reprocesa a PNG, incluso un GIF: una vista previa animada no
     * la muestra ninguna red, y guardarla animada solo serviria para que el
     * rastreador se descargue megabytes que va a tirar.
     */
    public Resultado procesarSocial(byte[] original) {
        exigirTamano(original, MAX_BANNER_BYTES, "La imagen no puede pasar de 4 MB.");

        BufferedImage imagen = decodificar(original);
        BufferedImage ajustada = ajustarADentroDe(imagen, SOCIAL_ANCHO, SOCIAL_ALTO);
        return new Resultado(codificarPng(ajustada), "image/png", ajustada.getWidth(), ajustada.getHeight());
    }

    // -------------------------------------------------------------------------

    /**
     * Distingue un SVG de otro formato desconocido, solo para dar mejor mensaje.
     *
     * <p><strong>Esto NO es lo que protege de los SVG.</strong> Lo que protege es
     * la firma del archivo: un SVG no empieza por los bytes de PNG, JPEG, GIF ni
     * RIFF, asi que {@link #decodificar} lo rechaza sin mas. Aqui solo se mira si
     * ademas parece XML, para poder decirle al usuario por que exactamente.
     *
     * <p>Antes esto se ejecutaba ANTES de comprobar la firma y buscaba la cadena
     * "&lt;svg" dentro de los primeros 512 bytes en crudo. Un PNG normal —los que
     * exportan las herramientas de diseño llevan metadatos XMP, que son XML—
     * podia contener esa secuencia y quedaba rechazado con un mensaje que decia
     * que aceptabamos PNG. Un heuristico de texto sobre datos binarios estaba de
     * sobra encima de una comprobacion que ya era definitiva.
     */
    private boolean pareceSvg(byte[] datos) {
        String comienzo = new String(datos, 0, Math.min(datos.length, 256), java.nio.charset.StandardCharsets.UTF_8)
                .toLowerCase()
                .stripLeading();
        return comienzo.startsWith("<?xml") || comienzo.startsWith("<svg") || comienzo.startsWith("<!doctype svg");
    }

    private void exigirTamano(byte[] datos, long maximo, String mensaje) {
        if (datos.length == 0) {
            throw new DomainException.Conflict("image.empty", "El archivo esta vacio.");
        }
        if (datos.length > maximo) {
            throw new DomainException.Conflict("image.too_large", mensaje);
        }
    }

    /**
     * Decodifica a pixeles. Si esto falla, no era una imagen — daba igual lo que
     * dijera su extension o su Content-Type.
     */
    /**
     * Decodifica la imagen tras comprobar su firma.
     *
     * <p>La firma es la comprobacion que manda, y va primero. Se mira el
     * CONTENIDO, no la extension: renombrar un archivo a .png no lo convierte en
     * uno, y confiar en el nombre es como se cuelan los ficheros que no son lo
     * que dicen ser.
     */
    private BufferedImage decodificar(byte[] datos) {
        if (!firmaConocida(datos)) {
            if (pareceSvg(datos)) {
                throw new DomainException.Conflict(
                        "image.svg_not_allowed",
                        "No aceptamos SVG. Formatos validos: PNG, JPG, WebP y GIF.");
            }
            throw new DomainException.Conflict(
                    "image.unsupported", "Formato no admitido. Usa PNG, JPG, WebP o GIF.");
        }
        exigirDimensionesRazonables(datos);

        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(datos));
            if (imagen == null) {
                throw new DomainException.Conflict(
                        "image.unreadable", "No hemos podido leer esa imagen. Prueba con otra.");
            }
            return imagen;
        } catch (IOException e) {
            throw new DomainException.Conflict("image.unreadable", "No hemos podido leer esa imagen.");
        }
    }

    /**
     * Lee la cabecera y rechaza por tamaño sin decodificar los pixeles.
     *
     * <p>Va antes de {@link ImageIO#read}: si se hiciera despues, la memoria ya
     * estaria reservada y la comprobacion llegaria tarde. Ver
     * {@link #LADO_MAXIMO_ENTRADA}.
     */
    private void exigirDimensionesRazonables(byte[] datos) {
        try (javax.imageio.stream.ImageInputStream flujo =
                ImageIO.createImageInputStream(new ByteArrayInputStream(datos))) {

            if (flujo == null) {
                throw new DomainException.Conflict(
                        "image.unreadable", "No hemos podido leer esa imagen.");
            }

            var lectores = ImageIO.getImageReaders(flujo);
            if (!lectores.hasNext()) {
                throw new DomainException.Conflict(
                        "image.unsupported", "Formato no admitido. Usa PNG, JPG, WebP o GIF.");
            }

            javax.imageio.ImageReader lector = lectores.next();
            try {
                lector.setInput(flujo);
                long ancho = lector.getWidth(0);
                long alto = lector.getHeight(0);

                if (ancho > LADO_MAXIMO_ENTRADA
                        || alto > LADO_MAXIMO_ENTRADA
                        || ancho * alto > PIXELES_MAXIMOS_ENTRADA) {
                    throw new DomainException.Conflict(
                            "image.dimensions_too_large",
                            "Esa imagen es demasiado grande: " + ancho + "x" + alto
                                    + " píxeles. El máximo es " + LADO_MAXIMO_ENTRADA + " por lado.");
                }
            } finally {
                lector.dispose();
            }

        } catch (IOException e) {
            throw new DomainException.Conflict("image.unreadable", "No hemos podido leer esa imagen.");
        }
    }

    private boolean firmaConocida(byte[] datos) {
        return empiezaPor(datos, FIRMA_PNG)
                || empiezaPor(datos, FIRMA_JPEG)
                || empiezaPor(datos, FIRMA_GIF)
                || empiezaPor(datos, FIRMA_RIFF);
    }

    private boolean esGif(byte[] datos) {
        return empiezaPor(datos, FIRMA_GIF);
    }

    private boolean empiezaPor(byte[] datos, byte[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (datos[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    /** Escala sin deformar y recorta al centro. */
    private BufferedImage redimensionar(BufferedImage origen, int ancho, int alto) {
        double escala = Math.max((double) ancho / origen.getWidth(), (double) alto / origen.getHeight());
        int anchoEscalado = (int) Math.ceil(origen.getWidth() * escala);
        int altoEscalado = (int) Math.ceil(origen.getHeight() * escala);

        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(
                origen,
                (ancho - anchoEscalado) / 2,
                (alto - altoEscalado) / 2,
                anchoEscalado,
                altoEscalado,
                null);
        g.dispose();
        return destino;
    }

    /** Reduce si excede el maximo; nunca amplia (ampliar solo produce imagen borrosa). */
    private BufferedImage ajustarADentroDe(BufferedImage origen, int anchoMax, int altoMax) {
        double escala = Math.min(
                (double) anchoMax / origen.getWidth(), (double) altoMax / origen.getHeight());
        if (escala >= 1) {
            return convertirARgb(origen);
        }
        int ancho = Math.max(1, (int) (origen.getWidth() * escala));
        int alto = Math.max(1, (int) (origen.getHeight() * escala));

        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(origen, 0, 0, ancho, alto, null);
        g.dispose();
        return destino;
    }

    private BufferedImage convertirARgb(BufferedImage origen) {
        if (origen.getType() == BufferedImage.TYPE_INT_ARGB) {
            return origen;
        }
        BufferedImage destino =
                new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destino.createGraphics();
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return destino;
    }

    private byte[] codificarPng(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo codificar la imagen", e);
        }
    }

    /**
     * @param contenido bytes ya reprocesados, listos para guardar
     * @param ancho null en GIF animado, donde no se reprocesa
     */
    public record Resultado(byte[] contenido, String contentType, Integer ancho, Integer alto) {}
}
