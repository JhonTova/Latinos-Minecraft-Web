package com.latinosmc.app.web;

import com.latinosmc.organizations.SiteSettingsRepository;
import com.latinosmc.shared.ports.FileStorage;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Textos e imagen social de la portada.
 *
 * <p>Publico y sin sesion: lo lee el servidor de la web para componer las
 * etiquetas {@code <meta>} de cada pagina.
 *
 * <p><strong>Por que esto lo pinta el servidor y no el navegador.</strong> Los
 * rastreadores de Discord, WhatsApp, Twitter y Facebook <em>no ejecutan
 * JavaScript</em>. Piden el HTML, leen las etiquetas que ya vienen dentro y se
 * van. Una web que inserta sus metadatos desde React —con Helmet o similar—
 * queda sin vista previa: el rastreador nunca llega a ver esas etiquetas.
 *
 * <p>Aqui la web es Next.js con render en servidor, asi que los metadatos viajan
 * en el HTML inicial y los rastreadores los ven.
 */
@RestController
@RequestMapping("/api/v1/sitio")
public class SitioController {

    private final SiteSettingsRepository ajustes;
    private final FileStorage almacen;

    public SitioController(SiteSettingsRepository ajustes, FileStorage almacen) {
        this.ajustes = ajustes;
        this.almacen = almacen;
    }

    /**
     * Cache de cinco minutos: se pide en cada pagina servida y cambia como mucho
     * una vez al mes. Es la consulta con mejor relacion entre lo que ahorra y lo
     * poco que importa que tarde unos minutos en reflejar un cambio.
     */
    @GetMapping
    public ResponseEntity<AjustesPublicos> ajustes() {
        var a = ajustes.actuales();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(new AjustesPublicos(
                        a.titulo(),
                        a.descripcion(),
                        a.nombre(),
                        a.claveOgImage() == null ? null : almacen.urlPublica(a.claveOgImage()),
                        a.claveLogo() == null ? null : almacen.urlPublica(a.claveLogo()),
                        a.urlPlugin()));
    }

    /**
     * @param ogImageUrl null si nadie ha subido imagen; entonces la web no la declara
     * @param logoUrl null si nadie ha subido logotipo; entonces la web pinta el
     *     bloque de hierba dibujado en SVG, que no depende de ninguna subida
     * @param urlPlugin null oculta el boton de descarga en vez de dejarlo roto
     */
    public record AjustesPublicos(
            String titulo,
            String descripcion,
            String nombre,
            String ogImageUrl,
            String logoUrl,
            String urlPlugin) {}
}
