package com.latinosmc.servers.imagenes;

import static org.assertj.core.api.Assertions.assertThat;

import com.latinosmc.servers.ping.motd.MotdMinecraft;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class GeneradorDeBannerMotdTest {

    private final GeneradorDeBannerMotd generador = new GeneradorDeBannerMotd();

    private static BufferedImage leer(byte[] png) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private static List<MotdMinecraft.Linea> motd(String texto) {
        return MotdMinecraft.analizarTexto(texto);
    }

    private static byte[] iconoDePrueba() throws Exception {
        var img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setColor(new Color(0x1E883C));
        g.fillRect(0, 0, 64, 64);
        g.dispose();

        var salida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", salida);
        return salida.toByteArray();
    }

    @Test
    void generaUnPngDelTamanoDelHueco() throws Exception {
        BufferedImage banner = leer(generador.generar(motd("§aHola"), iconoDePrueba(), "Servidor"));

        assertThat(banner.getWidth()).isEqualTo(GeneradorDeBannerMotd.ANCHO * GeneradorDeBannerMotd.ESCALA);
        assertThat(banner.getHeight()).isEqualTo(GeneradorDeBannerMotd.ALTO * GeneradorDeBannerMotd.ESCALA);
    }

    @Test
    void elMismoMotdProduceExactamenteLosMismosBytes() throws Exception {
        byte[] icono = iconoDePrueba();

        byte[] uno = generador.generar(motd("§aHola"), icono, "Servidor");
        byte[] dos = generador.generar(motd("§aHola"), icono, "Servidor");

        // De esto depende todo el ahorro: si el mismo MOTD diera bytes distintos,
        // el hash cambiaria en cada ronda y regenerariamos banners identicos para
        // siempre (ADR-0027).
        assertThat(uno).isEqualTo(dos);
    }

    @Test
    void sinIconoSiguePintandoElMotd() throws Exception {
        byte[] png = generador.generar(motd("§dSKYBLOCK"), null, "Servidor");

        assertThat(leer(png)).isNotNull();
        assertThat(png.length).isGreaterThan(0);
    }

    @Test
    void sinMotdCaeAlNombreDelServidor() throws Exception {
        // Un servidor que no anuncia MOTD tiene que salir con algo. Un banner en
        // blanco seria peor que el rectangulo gris que esto viene a sustituir.
        byte[] conNombre = generador.generar(List.of(), null, "Craft Latino");
        byte[] sinNada = generador.generar(List.of(), null, null);

        assertThat(leer(conNombre)).isNotNull();
        assertThat(leer(sinNada)).isNotNull();
        assertThat(conNombre).isNotEqualTo(sinNada);
    }

    @Test
    void unMotdDesmesuradoNoSeSaleDelBanner() throws Exception {
        // Se reduce la letra y, si aun asi no cabe, se recorta. Lo que no puede
        // pasar es que el texto se pinte fuera y salga cortado a media palabra.
        byte[] png = generador.generar(
                motd("§a" + "palabra ".repeat(40) + "\n§7" + "otra ".repeat(40)), iconoDePrueba(), "X");

        BufferedImage banner = leer(png);
        assertThat(banner.getWidth()).isEqualTo(GeneradorDeBannerMotd.ANCHO * GeneradorDeBannerMotd.ESCALA);
    }

    @Test
    void unIconoIlegibleDaBannerSinIconoEnLugarDeFallar() throws Exception {
        // El icono viene de una maquina ajena. Que no sea una imagen es un servidor
        // mal configurado, no un motivo para dejarle sin ficha.
        byte[] png = generador.generar(motd("§aHola"), new byte[] {1, 2, 3}, "Servidor");

        assertThat(leer(png)).isNotNull();
    }

    @Test
    void pesaLoBastantePocoParaPintarUnaPaginaEntera() throws Exception {
        // Una pagina del directorio son unas veinte tarjetas. A trescientos
        // kilobytes cada una —que es lo que pesaba la primera version— serian seis
        // megabytes de fondo, y en un movil con datos eso es un visitante que se va
        // antes de ver nada.
        byte[] png = generador.generar(motd("§6§lSERVIDOR §f» §aSurvival\n§7Entra ya"), iconoDePrueba(), "X");

        assertThat(png.length).isLessThan(150 * 1024);
    }
}
