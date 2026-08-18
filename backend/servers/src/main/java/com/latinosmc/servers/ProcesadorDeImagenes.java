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
