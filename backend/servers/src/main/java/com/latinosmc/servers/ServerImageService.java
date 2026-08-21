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

    /**
     * Devuelve una imagen al modo automatico (ADR-0027).
     *
     * <p>No borra lo que subio el dueño: solo deja de enseñarlo. Alguien que pago
     * a un diseñador y prueba el modo automatico por curiosidad tiene que poder
     * volver con un clic, y si el cambio destruyera el archivo no habria vuelta.
     *
     * @param cual {@code ICON} o {@code BANNER}
     */
    @Transactional
    public void usarImagenAutomatica(UUID servidorId, UUID organizacionId, String cual) {
        exigirProposito(cual);

        if (servidores.cambiarOrigenDeImagen(servidorId, organizacionId, cual, true) == 0) {
            throw new DomainException.NotFound(
                    "server.not_found", "Ese servidor no existe o no puedes gestionarlo.");
        }
    }

    /**
     * Vuelve a la imagen que subio el dueño.
     *
     * @param cual {@code ICON} o {@code BANNER}
     */
    @Transactional
    public void usarImagenSubida(UUID servidorId, UUID organizacionId, String cual) {
        exigirProposito(cual);

        if (servidores.cambiarOrigenDeImagen(servidorId, organizacionId, cual, false) == 0) {
            throw new DomainException.NotFound(
                    "server.not_found", "Ese servidor no existe o no puedes gestionarlo.");
        }
    }

    /**
     * Lista blanca de los dos unicos valores admitidos.
     *
     * <p>El valor llega desde la API y termina eligiendo un nombre de columna. La
     * eleccion la hace el repositorio entre dos literales, nunca interpolando lo
     * que llegue, pero la comprobacion se hace igualmente aqui: dos defensas para
     * lo mismo cuestan una linea, y una sola equivocada cuesta la base de datos.
     */
    private void exigirProposito(String cual) {
        if (!"ICON".equals(cual) && !"BANNER".equals(cual)) {
            throw new DomainException.Conflict("image.unknown_purpose", "Esa imagen no existe.");
        }
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

        // Subir una imagen conmuta esa imagen a manual (ADR-0027). Es lo que
        // quiere decir alguien que sube un banner, y pedirle un segundo clic para
        // explicarlo solo produciria gente que sube su banner, no ve ningun cambio
        // y concluye que la subida no funciona.
        //
        // Lo que subio NO pisa lo automatico: viven en columnas distintas, asi que
        // volver al modo automatico manaña no destruye el archivo de hoy.
        servidores.cambiarOrigenDeImagen(servidorId, organizacionId, proposito, false);

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
