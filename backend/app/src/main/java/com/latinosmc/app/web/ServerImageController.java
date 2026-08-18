package com.latinosmc.app.web;

import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerImageService;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.DomainException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public ServerImageController(
            ServerImageService imagenes,
            ServerRepository servidores,
            OrganizationRepository organizaciones) {
        this.imagenes = imagenes;
        this.servidores = servidores;
        this.organizaciones = organizaciones;
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
}
