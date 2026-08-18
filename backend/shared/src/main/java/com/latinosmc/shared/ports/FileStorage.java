package com.latinosmc.shared.ports;

import java.io.InputStream;
import java.util.Optional;

/**
 * Almacenamiento de archivos. Puerto, no implementacion.
 *
 * <p>En desarrollo, el disco local. En produccion, Cloudflare R2 — elegido
 * porque no cobra egreso, y en una plataforma que sirve iconos y banners en cada
 * carga de la portada, el egreso es el coste dominante, no el almacenamiento.
 *
 * <p>Se trabaja con <em>claves</em> ({@code servidores/abc/icono.png}) y no con
 * URLs completas: cambiar de proveedor o de dominio debe ser cambiar un prefijo
 * en la configuracion, no migrar toda la base de datos.
 */
public interface FileStorage {

    /**
     * Guarda un archivo y devuelve su clave.
     *
     * @param clave ruta relativa dentro del almacen
     * @param contenido datos ya validados y reprocesados; nunca el archivo tal
     *     como llego del usuario
     * @param contentType tipo MIME real, comprobado por nosotros
     */
    String guardar(String clave, byte[] contenido, String contentType);

    Optional<InputStream> leer(String clave);

    void borrar(String clave);

    /** URL publica desde la que se sirve. */
    String urlPublica(String clave);
}
