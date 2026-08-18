package com.latinosmc.app.web;

import com.latinosmc.shared.ports.FileStorage;
import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * Sirve los archivos subidos cuando el almacen es el disco local.
 *
 * <p>En produccion con R2 o S3 esto no se usa: los sirve el CDN directamente, que
 * es mas rapido y no consume hilos de la aplicacion.
 */
@RestController
@RequestMapping("/archivos")
public class ArchivosController {

    private final FileStorage almacen;

    public ArchivosController(FileStorage almacen) {
        this.almacen = almacen;
    }

    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> servir(HttpServletRequest peticion) {
        String ruta = (String) peticion.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String clave = ruta.replaceFirst("^/archivos/", "");

        InputStream contenido = almacen.leer(clave).orElse(null);
        if (contenido == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                // Las claves llevan un identificador aleatorio y no se reutilizan:
                // al cambiar la imagen cambia la URL. Por eso se puede cachear
                // agresivamente sin riesgo de servir una version vieja.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header(HttpHeaders.CONTENT_TYPE, tipoDe(clave))
                // Impide que el navegador adivine el tipo. Sin esto, un archivo con
                // contenido inesperado podria acabar interpretandose como HTML y
                // ejecutarse.
                .header("X-Content-Type-Options", "nosniff")
                // Fuerza descarga en lugar de renderizado para cualquier cosa que
                // se cuele: red de seguridad sobre la validacion del ADR-0017.
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new InputStreamResource(contenido));
    }

    private String tipoDe(String clave) {
        if (clave.endsWith(".gif")) return "image/gif";
        if (clave.endsWith(".png")) return "image/png";
        if (clave.endsWith(".webp")) return "image/webp";
        // Todo lo demas se sirve como binario: nunca se adivina un tipo que
        // pudiera ser ejecutable en el navegador.
        return "application/octet-stream";
    }
}
