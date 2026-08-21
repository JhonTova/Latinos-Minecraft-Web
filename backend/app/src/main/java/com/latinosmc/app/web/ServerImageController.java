package com.latinosmc.app.web;

import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerImageService;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.servers.imagenes.ActualizacionManualDeImagenes;
import com.latinosmc.servers.imagenes.ImagenesAutomaticasService;
import com.latinosmc.shared.DomainException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida del icono y el banner de un servidor (ADR-0017).
 *
 * <p>Nada de lo que llega se guarda tal cual: se decodifica a pixeles y se
 * vuelve a codificar. Y el SVG se rechaza siempre — es XML que puede llevar
 * JavaScript, y servirlo desde nuestro dominio ejecutaria el codigo de un tercero
 * con la sesion del visitante.
 */
@RestController
@RequestMapping("/api/v1/servers/{servidorId}/imagenes")
public class ServerImageController {

    private final ServerImageService imagenes;
    private final ServerRepository servidores;
    private final OrganizationRepository organizaciones;
    private final ActualizacionManualDeImagenes actualizacionManual;

    public ServerImageController(
            ServerImageService imagenes,
            ServerRepository servidores,
            OrganizationRepository organizaciones,
            ActualizacionManualDeImagenes actualizacionManual) {
        this.imagenes = imagenes;
        this.servidores = servidores;
        this.organizaciones = organizaciones;
        this.actualizacionManual = actualizacionManual;
    }

    @PostMapping("/icono")
    public RespuestaImagen subirIcono(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable UUID servidorId,
            @RequestParam("archivo") MultipartFile archivo) {

        UUID organizacionId = comprobarPropiedad(usuario, servidorId);
        String url = imagenes.subirIcono(servidorId, organizacionId, usuario.id(), leer(archivo));
        return new RespuestaImagen(url);
    }

    @PostMapping("/banner")
    public RespuestaImagen subirBanner(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable UUID servidorId,
            @RequestParam("archivo") MultipartFile archivo) {

        UUID organizacionId = comprobarPropiedad(usuario, servidorId);
        String url = imagenes.subirBanner(servidorId, organizacionId, usuario.id(), leer(archivo));
        return new RespuestaImagen(url);
    }

    /**
     * Cambia entre la imagen automatica y la subida (ADR-0027).
     *
     * <p>{@code cual} es {@code icono} o {@code banner}; {@code origen} es
     * {@code auto} o {@code manual}.
     */
    @PutMapping("/{cual}/origen")
    public RespuestaOrigen cambiarOrigen(
            @UsuarioActual IdentityRepository.Usuario usuario,
            @PathVariable UUID servidorId,
            @PathVariable String cual,
            @RequestParam("origen") String origen) {

        UUID organizacionId = comprobarPropiedad(usuario, servidorId);
        String proposito = propositoDe(cual);
        boolean automatico = "auto".equalsIgnoreCase(origen);

        if (automatico) {
            imagenes.usarImagenAutomatica(servidorId, organizacionId, proposito);
        } else {
            imagenes.usarImagenSubida(servidorId, organizacionId, proposito);
        }
        return new RespuestaOrigen(automatico ? "auto" : "manual");
    }

    /**
     * Sondea el servidor ahora y readopta lo que anuncie.
     *
     * <p>Para quien acaba de cambiar el icono o el MOTD de su servidor y quiere
     * verlo reflejado sin esperar a la ronda automatica.
     */
    @PostMapping("/automaticas/actualizar")
    public RespuestaActualizacion actualizarAhora(
            @UsuarioActual IdentityRepository.Usuario usuario, @PathVariable UUID servidorId) {

        UUID organizacionId = comprobarPropiedad(usuario, servidorId);
        var resultado = actualizacionManual.ejecutar(servidorId, organizacionId);

        return new RespuestaActualizacion(resultado.name(), mensajeDe(resultado));
    }

    /**
     * El texto que ve el dueño.
     *
     * <p>Cada caso dice que ha pasado y, cuando no se hizo nada, que puede hacer al
     * respecto. Un "no se pudo actualizar" a secas deja a alguien mirando su
     * pantalla sin saber si el fallo es suyo o nuestro.
     */
    private String mensajeDe(ImagenesAutomaticasService.Resultado resultado) {
        return switch (resultado) {
            case ACTUALIZADO -> "Listo. Hemos cogido el icono y el MOTD de tu servidor.";
            case SIN_CAMBIOS -> "Tu servidor anuncia lo mismo que la última vez, así que no hay nada que cambiar.";
            case DEMASIADO_PRONTO -> "Acabas de actualizar. Espera un momento antes de volver a intentarlo.";
            case SERVIDOR_CAIDO -> "Tu servidor no responde ahora mismo, así que no hemos podido leer nada de él.";
            case TODO_MANUAL -> "Tienes las dos imágenes en modo manual. Pon alguna en automático para que esto haga algo.";
            case BLOQUEADO -> "Las imágenes automáticas de este servidor están bloqueadas. Escríbenos si crees que es un error.";
        };
    }

    private String propositoDe(String cual) {
        return switch (cual.toLowerCase()) {
            case "icono" -> "ICON";
            case "banner" -> "BANNER";
            default -> throw new DomainException.NotFound("image.unknown_purpose", "Esa imagen no existe.");
        };
    }

    /**
     * Comprueba que el servidor pertenece a una organizacion que el usuario puede
     * gestionar.
     *
     * <p>Sin esto, cualquiera con sesion podria cambiar el icono de un servidor
     * ajeno pasando su identificador en la URL. Es control de acceso a nivel de
     * objeto — lo que OWASP llama BOLA — y sigue siendo el fallo numero uno en
     * APIs multi-tenant.
     *
     * <p>El mismo error para "no existe" y "no es tuyo": distinguirlos permitiria
     * averiguar que servidores existen.
     */
    private UUID comprobarPropiedad(IdentityRepository.Usuario usuario, UUID servidorId) {
        UUID organizacionId = servidores
                .organizacionDe(servidorId)
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        if (!organizaciones.puedeGestionar(usuario.id(), organizacionId)) {
            throw new DomainException.NotFound(
                    "server.not_found", "Ese servidor no existe o no puedes gestionarlo.");
        }
        return organizacionId;
    }

    private byte[] leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainException.Conflict("image.missing", "No has adjuntado ningun archivo.");
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new DomainException.Conflict("image.unreadable", "No hemos podido leer el archivo.");
        }
    }

    public record RespuestaImagen(String url) {}

    public record RespuestaOrigen(String origen) {}

    /**
     * @param resultado el codigo, para que el frontend decida como pintarlo
     * @param mensaje el texto ya escrito, para no repartir la redaccion entre dos
     *     repositorios y que acabe diciendo cosas distintas en cada sitio
     */
    public record RespuestaActualizacion(String resultado, String mensaje) {}
}
