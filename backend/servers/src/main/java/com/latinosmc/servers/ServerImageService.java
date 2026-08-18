package com.latinosmc.servers;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.ports.FileStorage;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Subida del icono y el banner de un servidor. */
@Service
public class ServerImageService {

    private final ServerRepository servidores;
    private final ProcesadorDeImagenes procesador;
    private final FileStorage almacen;

    public ServerImageService(
            ServerRepository servidores, ProcesadorDeImagenes procesador, FileStorage almacen) {
        this.servidores = servidores;
        this.procesador = procesador;
        this.almacen = almacen;
    }

    @Transactional
    public String subirIcono(UUID servidorId, UUID organizacionId, UUID usuarioId, byte[] archivo) {
        var procesada = procesador.procesarIcono(archivo);
        return guardar(servidorId, organizacionId, usuarioId, procesada, "ICON");
    }

    @Transactional
    public String subirBanner(UUID servidorId, UUID organizacionId, UUID usuarioId, byte[] archivo) {
        var procesada = procesador.procesarBanner(archivo);
        return guardar(servidorId, organizacionId, usuarioId, procesada, "BANNER");
    }

    private String guardar(
            UUID servidorId,
            UUID organizacionId,
            UUID usuarioId,
            ProcesadorDeImagenes.Resultado imagen,
            String proposito) {

        // La clave lleva un identificador aleatorio, no el nombre del archivo ni
        // un contador. Dos motivos: el nombre original es entrada no confiable, y
        // una clave adivinable permitiria enumerar los archivos de otros.
        String extension = imagen.contentType().equals("image/gif") ? "gif" : "png";
        String clave = "servidores/%s/%s-%s.%s"
                .formatted(servidorId, proposito.toLowerCase(), Ids.newId(), extension);

        almacen.guardar(clave, imagen.contenido(), imagen.contentType());

        servidores.registrarSubida(
                Ids.newId(),
                organizacionId,
                usuarioId,
                clave,
                imagen.contentType(),
                imagen.contenido().length,
                imagen.ancho(),
                imagen.alto(),
                proposito);

        // La imagen anterior queda huerfana. Se marca para que el proceso de
        // limpieza la borre del almacen: si no, se paga por bytes que ya nadie
        // mira, y eso solo crece.
        String anterior = "ICON".equals(proposito)
                ? servidores.claveIcono(servidorId)
                : servidores.claveBanner(servidorId);
        if (anterior != null) {
            servidores.marcarSubidaHuerfana(anterior);
        }

        int filas = "ICON".equals(proposito)
                ? servidores.actualizarIcono(servidorId, organizacionId, clave)
                : servidores.actualizarBanner(servidorId, organizacionId, clave);

        if (filas == 0) {
            // La condicion por organizacion viaja en el UPDATE. Si no toco ninguna
            // fila, el servidor no es de esta organizacion: se aborta la
            // transaccion entera y el archivo guardado queda sin referencia.
            throw new DomainException.NotFound(
                    "server.not_found", "Ese servidor no existe o no puedes gestionarlo.");
        }

        return almacen.urlPublica(clave);
    }
}
