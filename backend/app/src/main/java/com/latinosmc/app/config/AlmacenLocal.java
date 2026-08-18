package com.latinosmc.app.config;

import com.latinosmc.shared.ports.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Almacenamiento en disco local.
 *
 * <p>Sirve para desarrollo y para un despliegue de una sola maquina con volumen
 * persistente. Cuando haya varias instancias hara falta el adaptador de R2 o S3:
 * con disco local, cada instancia veria solo sus propios archivos y la mitad de
 * los iconos aparecerian rotos segun a que servidor cayera la peticion.
 */
@Configuration
public class AlmacenLocal {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AlmacenLocal.class);

    /**
     * Comprueba al arrancar que la carpeta existe y se puede escribir.
     *
     * <p>Falla PRONTO y con un mensaje que dice como arreglarlo. Sin esto, el
     * problema aparece la primera vez que alguien sube una imagen —quiza dias
     * despues— y lo unico que se ve es "no se pudo subir", que no apunta a
     * ninguna parte.
     *
     * <p>El caso real: la carpeta se borra con sudo, Docker la recrea como root
     * al levantar el contenedor, y la aplicacion corre como usuario sin
     * privilegios. Todo parece correcto hasta que alguien intenta escribir.
     */
    private static void comprobarQueSePuedeEscribir(Path raiz) {
        try {
            Files.createDirectories(raiz);
            Path prueba = raiz.resolve(".escritura-" + System.nanoTime());
            Files.write(prueba, new byte[] {0});
            Files.deleteIfExists(prueba);
            log.info("Almacen de archivos listo en {}", raiz);

        } catch (IOException e) {
            log.error(
                    "NO SE PUEDE ESCRIBIR EN {}. Las subidas de imagenes fallaran. "
                            + "Si la carpeta se borro con sudo, Docker la recreo como root: "
                            + "corrige el dueño con chown y reinicia el contenedor.",
                    raiz,
                    e);
        }
    }

    @Bean
    @ConditionalOnMissingBean(FileStorage.class)
    public FileStorage fileStorage(
            @Value("${latinosmc.almacen.ruta:./datos/archivos}") String ruta,
            @Value("${latinosmc.almacen.url-publica:/archivos}") String urlPublica) {

        Path raiz = Path.of(ruta).toAbsolutePath().normalize();
        comprobarQueSePuedeEscribir(raiz);

        return new FileStorage() {
            @Override
            public String guardar(String clave, byte[] contenido, String contentType) {
                Path destino = resolver(clave);
                try {
                    Files.createDirectories(destino.getParent());
                    Files.write(destino, contenido);
                    return clave;
                } catch (java.nio.file.AccessDeniedException e) {
                    // El caso real: la carpeta se borro con sudo y Docker la
                    // recreo como root, mientras el contenedor corre sin
                    // privilegios. El mensaje lleva la solucion porque el
                    // sintoma —"no se pudo subir la imagen"— no apunta a nada.
                    log.error("Sin permiso de escritura en {}. Ejecuta: chown -R en esa carpeta.", raiz, e);
                    throw new com.latinosmc.shared.DomainException.Conflict(
                            "storage.not_writable",
                            "El servidor no puede escribir en su carpeta de archivos. Avisa al administrador.");
                } catch (IOException e) {
                    log.error("No se pudo guardar {}", clave, e);
                    throw new com.latinosmc.shared.DomainException.Conflict(
                            "storage.failed", "No se pudo guardar la imagen. Intentalo de nuevo.");
                }
            }

            @Override
            public Optional<InputStream> leer(String clave) {
                Path origen = resolver(clave);
                if (!Files.exists(origen)) {
                    return Optional.empty();
                }
                try {
                    return Optional.of(Files.newInputStream(origen));
                } catch (IOException e) {
                    return Optional.empty();
                }
            }

            @Override
            public void borrar(String clave) {
                try {
                    Files.deleteIfExists(resolver(clave));
                } catch (IOException e) {
                    // Un archivo que no se puede borrar es basura acumulada, no una
                    // operacion fallida: no tiene sentido romper la peticion del
                    // usuario por esto.
                }
            }

            @Override
            public String urlPublica(String clave) {
                return urlPublica.replaceAll("/$", "") + "/" + clave;
            }

            /**
             * Resuelve la clave dentro de la raiz y comprueba que no se sale.
             *
             * <p>Sin esta comprobacion, una clave con {@code ../../} permitiria
             * escribir o leer cualquier archivo del servidor. Es el ataque de
             * travesia de rutas, y es de los mas viejos que existen precisamente
             * porque sigue funcionando cuando alguien se confia.
             */
            private Path resolver(String clave) {
                Path destino = raiz.resolve(clave).normalize();
                if (!destino.startsWith(raiz)) {
                    throw new SecurityException("Clave de archivo fuera del almacen: " + clave);
                }
                return destino;
            }
        };
    }
}
