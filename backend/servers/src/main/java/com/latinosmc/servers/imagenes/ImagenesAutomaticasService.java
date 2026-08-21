package com.latinosmc.servers.imagenes;

import com.latinosmc.servers.ProcesadorDeImagenes;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.servers.ping.PingMinecraft;
import com.latinosmc.servers.ping.motd.MotdJson;
import com.latinosmc.servers.ping.motd.MotdMinecraft;
import com.latinosmc.servers.ping.motd.SaneadorDeMotd;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.ports.FileStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adopta como imagenes del servidor el icono y el MOTD que anuncia su ping
 * (ADR-0027).
 *
 * <p>El servidor que acaba de darse de alta ya tiene identidad visual: el icono y
 * el MOTD que enseña el menu multijugador. Estan diseñados, llevan los colores de
 * su marca y el jugador los reconoce. Antes de esto, su ficha era un rectangulo
 * gris hasta que su dueño abriera un editor de imagenes; ahora tiene ficha en
 * cuanto responde al primer sondeo.
 *
 * <p>El plugin no interviene, y no debe: corre en hardware del cliente y no es
 * fuente de verdad de nada (ADR-0009). Estos datos llegan por el sondeo que
 * hacemos nosotros, desde nuestra infraestructura.
 */
@Service
public class ImagenesAutomaticasService {

    private static final Logger log = LoggerFactory.getLogger(ImagenesAutomaticasService.class);

    /**
     * Cada cuanto, como mucho, se rehace el material automatico de un servidor.
     *
     * <p>Sin este freno el trabajo seria continuo, y no por un fallo: muchisimos
     * MOTD llevan dentro el contador de jugadores o un texto que rota, asi que
     * cambian de verdad en cada sondeo. Detectar el cambio no basta como freno
     * porque el cambio es real; hace falta decidir que no merece la pena.
     */
    private static final Duration ENTRE_ACTUALIZACIONES = Duration.ofHours(6);

    /**
     * Freno del boton "actualizar ahora" del panel.
     *
     * <p>Corto: quien acaba de cambiar el icono de su servidor quiere verlo, y
     * hacerle esperar seis horas para eso convertiria una funcion util en una
     * fuente de tickets de soporte. Un minuto solo impide el clic repetido.
     */
    private static final Duration ENTRE_ACTUALIZACIONES_A_MANO = Duration.ofMinutes(1);

    private final ServerRepository servidores;
    private final ProcesadorDeImagenes procesador;
    private final GeneradorDeBannerMotd generador;
    private final FileStorage almacen;

    /**
     * Dominios de la plataforma. No se quitan del MOTD: un servidor que anuncia
     * "vota en nuestro sitio" nos esta haciendo publicidad, no compitiendo.
     */
    private final Set<String> dominiosPropios;

    public ImagenesAutomaticasService(
            ServerRepository servidores,
            ProcesadorDeImagenes procesador,
            GeneradorDeBannerMotd generador,
            FileStorage almacen,
            @Value("${latinosmc.sitio-url:}") String sitioUrl) {

        this.servidores = servidores;
        this.procesador = procesador;
        this.generador = generador;
        this.almacen = almacen;
        this.dominiosPropios = dominioDe(sitioUrl);
    }

    /**
     * Rehace el material automatico de un servidor a partir de su sondeo.
     *
     * <p>Es el unico punto de entrada, y es publico y transaccional a proposito.
     * Con los envoltorios de conveniencia que tenia antes, las llamadas se hacian
     * de un metodo a otro dentro de la misma instancia, y en Spring eso no pasa
     * por el proxy: la anotacion no se aplicaba y el metodo corria sin transaccion
     * ninguna, en silencio.
     *
     * @param aPeticion true si lo pidio el dueño desde su panel, que tiene un
     *     freno mucho mas corto que la ronda automatica
     * @return que se hizo, o por que no se hizo nada
     */
    @Transactional
    public Resultado actualizar(UUID servidorId, PingMinecraft.Resultado sondeo, boolean aPeticion) {
        if (!sondeo.enLinea()) {
            return Resultado.SERVIDOR_CAIDO;
        }
        var estado = servidores.estadoDeImagenesAutomaticas(servidorId).orElse(null);
        if (estado == null) {
            return Resultado.SIN_CAMBIOS;
        }
        if (estado.bloqueado()) {
            return Resultado.BLOQUEADO;
        }
        if (!estado.iconoAutomatico() && !estado.bannerAutomatico()) {
            return Resultado.TODO_MANUAL;
        }
        if (demasiadoPronto(estado.actualizadoEn(), aPeticion)) {
            return Resultado.DEMASIADO_PRONTO;
        }

        // El MOTD se sanea ANTES de hashearlo. Si se hashease el original, un
        // servidor que solo cambia la URL que anunciamos y borramos nos haria
        // regenerar un banner identico cada seis horas para siempre.
        List<MotdMinecraft.Linea> crudo = MotdMinecraft.analizar(sondeo.descripcion());
        var motd = SaneadorDeMotd.sanear(crudo, estado.host(), dominiosPropios);

        String hashMotd = hash(motd.comoJson());
        String hashFavicon = hash(sondeo.favicon());

        boolean cambioIcono = !igual(hashFavicon, estado.hashFavicon());
        boolean cambioMotd = !igual(hashMotd, estado.hashMotd());

        boolean hayQueTocarIcono = estado.iconoAutomatico() && (cambioIcono || estado.claveIconoAuto() == null);
        // El banner lleva el icono dentro, asi que un icono nuevo tambien lo cambia.
        boolean hayQueTocarBanner = estado.bannerAutomatico()
                && (cambioMotd || cambioIcono || estado.claveBannerAuto() == null);

        if (!hayQueTocarIcono && !hayQueTocarBanner) {
            return Resultado.SIN_CAMBIOS;
        }

        byte[] iconoProcesado = procesarIconoSiLoHay(sondeo.favicon());

        String claveIcono = estado.claveIconoAuto();
        if (hayQueTocarIcono) {
            claveIcono = iconoProcesado == null
                    ? null
                    : guardar(estado, servidorId, iconoProcesado, "image/png", "ICON_AUTO",
                            ProcesadorDeImagenes.FAVICON_LADO, ProcesadorDeImagenes.FAVICON_LADO);
            marcarHuerfana(estado.claveIconoAuto(), claveIcono);
        }

        String claveBanner = estado.claveBannerAuto();
        if (hayQueTocarBanner) {
            // Si el servidor no anuncia icono, el banner sale solo con el MOTD; y
            // si tampoco anuncia MOTD, con el nombre. Nunca se deja sin banner:
            // que un servidor caido o parco enseñe un hueco gris es justo lo que
            // esto viene a arreglar.
            byte[] png = generador.generar(motd.lineas(), iconoProcesado, estado.nombre());
            claveBanner = guardar(estado, servidorId, png, "image/png", "BANNER_AUTO",
                    GeneradorDeBannerMotd.ANCHO * GeneradorDeBannerMotd.ESCALA,
                    GeneradorDeBannerMotd.ALTO * GeneradorDeBannerMotd.ESCALA);
            marcarHuerfana(estado.claveBannerAuto(), claveBanner);
        }

        servidores.guardarImagenesAutomaticas(
                servidorId, claveIcono, claveBanner, motd.comoJson(), hashMotd, hashFavicon);

        if (!motd.quitados().isEmpty()) {
            // Se registra para poder explicarselo al dueño si pregunta por que su
            // MOTD sale recortado en nuestra web.
            log.info("MOTD de {}: se quitaron direcciones ajenas {}", servidorId, motd.quitados());
        }
        return Resultado.ACTUALIZADO;
    }

    private byte[] procesarIconoSiLoHay(byte[] favicon) {
        if (favicon == null || favicon.length == 0) {
            return null;
        }
        try {
            return procesador.procesarFavicon(favicon).contenido();
        } catch (RuntimeException e) {
            // Un servidor puede anunciar cualquier cosa en ese campo. Que no sea una
            // imagen valida es un servidor mal configurado, no un error nuestro:
            // se sigue adelante y su banner sale sin icono.
            log.debug("Icono del ping descartado: {}", e.getMessage());
            return null;
        }
    }

    private String guardar(
            ServerRepository.EstadoDeImagenesAutomaticas estado,
            UUID servidorId,
            byte[] contenido,
            String contentType,
            String proposito,
            int ancho,
            int alto) {

        String clave = "servidores/%s/%s-%s.png"
                .formatted(servidorId, proposito.toLowerCase(), Ids.newId());

        almacen.guardar(clave, contenido, contentType);

        // Se registra en `uploads` como cualquier subida, con uploaded_by a nulo
        // porque no lo subio nadie. Asi la auditoria y la limpieza de huerfanos
        // que ya existen cubren esto sin codigo nuevo.
        servidores.registrarSubida(
                Ids.newId(),
                estado.organizacionId(),
                null,
                clave,
                contentType,
                contenido.length,
                ancho,
                alto,
                proposito);

        return clave;
    }

    private void marcarHuerfana(String anterior, String nueva) {
        if (anterior != null && !anterior.equals(nueva)) {
            servidores.marcarSubidaHuerfana(anterior);
        }
    }

    /**
     * El dominio de la plataforma, sacado de su propia URL publica.
     *
     * <p>Se deduce en vez de configurarse aparte porque una segunda propiedad que
     * hay que acordarse de cambiar a la vez que la primera acaba desincronizada, y
     * el sintoma —que empecemos a borrar nuestro propio dominio de los MOTD— no lo
     * relacionaria nadie con la causa.
     */
    private static Set<String> dominioDe(String sitioUrl) {
        if (sitioUrl == null || sitioUrl.isBlank()) {
            return Set.of();
        }
        try {
            String host = java.net.URI.create(sitioUrl.trim()).getHost();
            return host == null || host.isBlank() ? Set.of() : Set.of(host.toLowerCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Set.of();
        }
    }

    private boolean demasiadoPronto(Instant ultima, boolean aPeticion) {
        if (ultima == null) {
            return false;
        }
        Duration espera = aPeticion ? ENTRE_ACTUALIZACIONES_A_MANO : ENTRE_ACTUALIZACIONES;
        return ultima.isAfter(Instant.now().minus(espera));
    }

    private boolean igual(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Huella del contenido, solo para saber si cambio.
     *
     * <p>SHA-256 y no un {@code hashCode}: aqui una colision significaria dejar de
     * actualizar el banner de un servidor para siempre, y encontrar dos MOTD que
     * choquen en 32 bits es cuestion de minutos.
     */
    private String hash(Object contenido) {
        if (contenido == null) {
            return null;
        }
        byte[] bytes = contenido instanceof byte[] crudo
                ? crudo
                : contenido.toString().getBytes(StandardCharsets.UTF_8);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 deberia estar siempre disponible", e);
        }
    }

    /** Lo que paso, para poder decirselo al dueño cuando pulsa "actualizar ahora". */
    public enum Resultado {
        ACTUALIZADO,
        SIN_CAMBIOS,
        DEMASIADO_PRONTO,
        SERVIDOR_CAIDO,
        TODO_MANUAL,
        BLOQUEADO
    }
}
