package com.latinosmc.servers.imagenes;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La tipografia de pixel con la que se pinta el banner automatico.
 *
 * <p>Es <strong>Monocraft</strong>, bajo licencia SIL Open Font License 1.1. No
 * es la fuente del juego: esa es un asset propiedad de Mojang y no se puede
 * empaquetar en nuestro backend ni servir desde nuestro dominio, ni siquiera
 * sacandola del jar del juego (ADR-0027). Monocraft es una recreacion libre y
 * redistribuible.
 *
 * <p>Diferencia visible: Monocraft es monoespaciada y la del juego es
 * proporcional, asi que el espaciado no es identico caracter a caracter. A la
 * distancia a la que se mira un banner no se nota, y es el precio de no tener un
 * problema legal.
 *
 * <p>Si la fuente no carga, se cae a la monoespaciada del sistema en vez de
 * fallar: un banner feo es mejor que una ficha sin banner.
 */
final class FuentePixel {

    private static final Logger log = LoggerFactory.getLogger(FuentePixel.class);

    private static final String RECURSO = "/fuentes/Monocraft.ttf";

    /** La fuente base, cargada una sola vez. Es inmutable, asi que se comparte sin candados. */
    private static final Font BASE = cargar();

    /**
     * Inclinacion de la cursiva.
     *
     * <p>El juego no tiene glifos en cursiva: deforma los que tiene. Se hace lo
     * mismo, y por el mismo motivo — Monocraft tampoco trae variante cursiva, y
     * dejar que el sistema se la invente da un resultado distinto en cada maquina.
     */
    private static final double INCLINACION = -0.21;

    private FuentePixel() {}

    static Font normal(float tamano) {
        return BASE.deriveFont(tamano);
    }

    static Font cursiva(float tamano) {
        return BASE.deriveFont(tamano)
                .deriveFont(AffineTransform.getShearInstance(INCLINACION, 0));
    }

    private static Font cargar() {
        try (InputStream entrada = FuentePixel.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                log.warn("No se encontro {}; el banner automatico usara la fuente del sistema.", RECURSO);
                return Font.decode(Font.MONOSPACED).deriveFont(Font.PLAIN, 12f);
            }

            Font fuente = Font.createFont(Font.TRUETYPE_FONT, entrada);

            // Registrarla evita que cada deriveFont vuelva a mirar en el sistema.
            // Falla en algunos entornos restringidos, y no pasa nada: la fuente ya
            // esta cargada en memoria y sirve igual.
            try {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fuente);
            } catch (RuntimeException e) {
                log.debug("No se pudo registrar la fuente en el entorno grafico: {}", e.getMessage());
            }
            return fuente;

        } catch (IOException | FontFormatException e) {
            log.warn("No se pudo cargar {}: {}. Se usara la fuente del sistema.", RECURSO, e.getMessage());
            return Font.decode(Font.MONOSPACED).deriveFont(Font.PLAIN, 12f);
        }
    }
}
