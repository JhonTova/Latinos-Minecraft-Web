package com.latinosmc.servers.imagenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.latinosmc.servers.ProcesadorDeImagenes;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.servers.ping.PingMinecraft;
import com.latinosmc.shared.ports.FileStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que se prueba aqui es cuando NO hay que hacer nada.
 *
 * <p>Generar el banner es la parte facil y ya tiene sus pruebas. La que se rompe
 * sola es la de decidir: sin los frenos, esto regenera imagenes en cada ronda de
 * sondeo, de cada servidor, para siempre — porque muchos MOTD llevan dentro el
 * contador de jugadores y cambian de verdad cada minuto (ADR-0027).
 */
class ImagenesAutomaticasServiceTest {

    private static final UUID SERVIDOR = UUID.randomUUID();
    private static final UUID ORGANIZACION = UUID.randomUUID();
    private static final ObjectMapper JSON = new ObjectMapper();

    private ServerRepository servidores;
    private FileStorage almacen;
    private ImagenesAutomaticasService servicio;

    @BeforeEach
    void preparar() {
        servidores = mock(ServerRepository.class);
        almacen = mock(FileStorage.class);

        servicio = new ImagenesAutomaticasService(
                servidores,
                new ProcesadorDeImagenes(),
                new GeneradorDeBannerMotd(),
                almacen,
                "https://latinosmc.com");
    }

    // --- Utilidades ----------------------------------------------------------

    private static byte[] faviconPng() throws Exception {
        var img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        var salida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", salida);
        return salida.toByteArray();
    }

    private static JsonNode motd(String texto) throws Exception {
        return JSON.readTree("\"" + texto + "\"");
    }

    private PingMinecraft.Resultado sondeoCon(String textoMotd, byte[] favicon) throws Exception {
        return new PingMinecraft.Resultado(
                true, 10, 100, 30, "1.21", null, favicon, motd(textoMotd));
    }

    private ServerRepository.EstadoDeImagenesAutomaticas estado(
            boolean iconoAuto,
            boolean bannerAuto,
            String hashMotd,
            String hashFavicon,
            Instant actualizadoEn,
            boolean bloqueado) {

        return new ServerRepository.EstadoDeImagenesAutomaticas(
                ORGANIZACION,
                "Craft Latino",
                "play.ejemplo.com",
                iconoAuto,
                bannerAuto,
                hashMotd,
                hashFavicon,
                hashMotd == null ? null : "servidores/x/icon_auto-viejo.png",
                hashMotd == null ? null : "servidores/x/banner_auto-viejo.png",
                actualizadoEn,
                bloqueado);
    }

    private void conEstado(ServerRepository.EstadoDeImagenesAutomaticas estado) {
        when(servidores.estadoDeImagenesAutomaticas(SERVIDOR)).thenReturn(Optional.of(estado));
    }

    // --- Pruebas -------------------------------------------------------------

    @Test
    @DisplayName("adopta icono y banner la primera vez")
    void primeraAdopcion() throws Exception {
        conEstado(estado(true, true, null, null, null, false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aHola", faviconPng()), false);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.ACTUALIZADO);
        verify(servidores)
                .guardarImagenesAutomaticas(eq(SERVIDOR), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("un servidor caido no borra lo que ya tenia")
    void servidorCaido() {
        var resultado = servicio.actualizar(
                SERVIDOR, PingMinecraft.Resultado.sinConexion("timeout"), false);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.SERVIDOR_CAIDO);
        // Lo importante no es el codigo, es que no se toco nada: un servidor que
        // esta reiniciando no puede perder su ficha por eso.
        verify(servidores, never())
                .guardarImagenesAutomaticas(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("con las dos imagenes en manual no hace nada")
    void todoManual() throws Exception {
        conEstado(estado(false, false, null, null, null, false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aHola", faviconPng()), false);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.TODO_MANUAL);
        verify(almacen, never()).guardar(any(), any(), any());
    }

    @Test
    @DisplayName("un servidor bloqueado no vuelve a adoptar nada")
    void bloqueado() throws Exception {
        conEstado(estado(true, true, null, null, null, true));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aHola", faviconPng()), false);

        // Es la mitad que hace util la otra: sin esto, retirar unas imagenes
        // duraria hasta el siguiente sondeo.
        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.BLOQUEADO);
        verify(almacen, never()).guardar(any(), any(), any());
    }

    @Test
    @DisplayName("dentro de las seis horas no se regenera aunque cambie el MOTD")
    void frenoDeSeisHoras() throws Exception {
        conEstado(estado(true, true, "hash-viejo", "hash-viejo", Instant.now().minusSeconds(3600), false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aOtra cosa", faviconPng()), false);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.DEMASIADO_PRONTO);
        verify(almacen, never()).guardar(any(), any(), any());
    }

    @Test
    @DisplayName("el boton del panel se salta el freno de las seis horas")
    void aPeticionDelDueno() throws Exception {
        conEstado(estado(true, true, "hash-viejo", "hash-viejo", Instant.now().minusSeconds(3600), false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aOtra cosa", faviconPng()), true);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.ACTUALIZADO);
    }

    @Test
    @DisplayName("el boton del panel tampoco admite clics seguidos")
    void aPeticionPeroDemasiadoSeguido() throws Exception {
        conEstado(estado(true, true, "hash-viejo", "hash-viejo", Instant.now().minusSeconds(5), false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aOtra cosa", faviconPng()), true);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.DEMASIADO_PRONTO);
    }

    @Test
    @DisplayName("si nada ha cambiado, no se genera ni se guarda nada")
    void sinCambios() throws Exception {
        byte[] favicon = faviconPng();

        // Se adopta una vez para quedarse con los hashes que produjo.
        conEstado(estado(true, true, null, null, null, false));
        servicio.actualizar(SERVIDOR, sondeoCon("§aHola", favicon), false);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(servidores)
                .guardarImagenesAutomaticas(
                        eq(SERVIDOR), anyString(), anyString(), anyString(), captor.capture(), anyString());
        String hashMotd = captor.getValue();

        // Y ahora se vuelve a sondear lo mismo, con el freno ya vencido.
        var servidoresDos = mock(ServerRepository.class);
        var almacenDos = mock(FileStorage.class);
        var servicioDos = new ImagenesAutomaticasService(
                servidoresDos,
                new ProcesadorDeImagenes(),
                new GeneradorDeBannerMotd(),
                almacenDos,
                "https://latinosmc.com");

        var hashFavicon = hashDe(favicon);
        when(servidoresDos.estadoDeImagenesAutomaticas(SERVIDOR))
                .thenReturn(Optional.of(new ServerRepository.EstadoDeImagenesAutomaticas(
                        ORGANIZACION,
                        "Craft Latino",
                        "play.ejemplo.com",
                        true,
                        true,
                        hashMotd,
                        hashFavicon,
                        "servidores/x/icon_auto-a.png",
                        "servidores/x/banner_auto-a.png",
                        Instant.now().minusSeconds(60 * 60 * 24),
                        false)));

        var resultado = servicioDos.actualizar(SERVIDOR, sondeoCon("§aHola", favicon), false);

        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.SIN_CAMBIOS);
        verify(almacenDos, never()).guardar(any(), any(), any());
    }

    @Test
    @DisplayName("un icono ilegible no impide generar el banner")
    void iconoBasura() throws Exception {
        conEstado(estado(true, true, null, null, null, false));

        var resultado = servicio.actualizar(SERVIDOR, sondeoCon("§aHola", new byte[] {1, 2, 3}), false);

        // Un servidor mal configurado no puede quedarse sin ficha por eso.
        assertThat(resultado).isEqualTo(ImagenesAutomaticasService.Resultado.ACTUALIZADO);
        verify(servidores)
                .registrarSubida(any(), any(), eq(null), anyString(), anyString(), anyLong(), anyInt(), anyInt(), eq("BANNER_AUTO"));
    }

    @Test
    @DisplayName("solo el banner en automatico: no se toca el icono subido")
    void soloElBannerEsAutomatico() throws Exception {
        conEstado(estado(false, true, null, null, null, false));

        servicio.actualizar(SERVIDOR, sondeoCon("§aHola", faviconPng()), false);

        verify(servidores, never())
                .registrarSubida(any(), any(), any(), anyString(), anyString(), anyLong(), anyInt(), anyInt(), eq("ICON_AUTO"));
        verify(servidores)
                .registrarSubida(any(), any(), any(), anyString(), anyString(), anyLong(), anyInt(), anyInt(), eq("BANNER_AUTO"));
    }

    private String hashDe(byte[] bytes) throws Exception {
        return java.util.HexFormat.of()
                .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
