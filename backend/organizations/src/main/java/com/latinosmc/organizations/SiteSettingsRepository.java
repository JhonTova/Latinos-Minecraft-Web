package com.latinosmc.organizations;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Titulo, descripcion e imagen social de la portada.
 *
 * <p>Estaban escritos en el codigo. Son justo las cosas que mas se retocan: el
 * texto que decide si alguien pulsa un enlace o pasa de largo se afina probando,
 * y no deberia costar un despliegue cada intento.
 */
@Repository
public class SiteSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(SiteSettingsRepository.class);

    /**
     * Valores de emergencia si la tabla no se puede leer.
     *
     * <p>Existen para que un fallo de base de datos no deje la portada sin titulo
     * ni descripcion: una pagina sin metadatos no solo se ve mal al compartirla,
     * sino que Google puede indexarla asi y tardar semanas en corregirlo.
     */
    private static final AjustesDelSitio EMERGENCIA = new AjustesDelSitio(
            "Servidores de Minecraft en español — Ranking de servidores latinos",
            "Los mejores servidores de Minecraft en español y latinos. Ranking por votos verificados.",
            null,
            "LatinosMC",
            null,
            null);

    private final JdbcClient jdbc;

    public SiteSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public AjustesDelSitio actuales() {
        try {
            return jdbc.sql(
                            """
                            SELECT site_title, site_description, og_image_key, site_name,
                                   logo_key, plugin_url
                              FROM site_settings
                             WHERE id IS TRUE
                            """)
                    .query((rs, fila) -> new AjustesDelSitio(
                            rs.getString("site_title"),
                            rs.getString("site_description"),
                            rs.getString("og_image_key"),
                            rs.getString("site_name"),
                            rs.getString("logo_key"),
                            rs.getString("plugin_url")))
                    .single();

        } catch (RuntimeException e) {
            log.error("No se pudieron leer los ajustes del sitio; se usan los de emergencia", e);
            return EMERGENCIA;
        }
    }

    public void guardar(AjustesDelSitio ajustes, UUID actorId) {
        jdbc.sql(
                        """
                        UPDATE site_settings
                           SET site_title       = :titulo,
                               site_description = :descripcion,
                               site_name        = :nombre,
                               plugin_url       = :plugin,
                               updated_at       = now(),
                               updated_by       = :actor
                         WHERE id IS TRUE
                        """)
                .param("titulo", ajustes.titulo())
                .param("descripcion", ajustes.descripcion())
                .param("nombre", ajustes.nombre())
                .param("plugin", ajustes.urlPlugin())
                .param("actor", actorId)
                .update();
    }

    /** Las imagenes se guardan aparte: se suben por multipart, no viajan con el formulario. */
    public void guardarLogo(String claveLogo, UUID actorId) {
        jdbc.sql(
                        """
                        UPDATE site_settings
                           SET logo_key = :clave, updated_at = now(), updated_by = :actor
                         WHERE id IS TRUE
                        """)
                .param("clave", claveLogo)
                .param("actor", actorId)
                .update();
    }

    public void guardarImagen(String claveOgImage, UUID actorId) {
        jdbc.sql(
                        """
                        UPDATE site_settings
                           SET og_image_key = :clave, updated_at = now(), updated_by = :actor
                         WHERE id IS TRUE
                        """)
                .param("clave", claveOgImage)
                .param("actor", actorId)
                .update();
    }

    /**
     * @param claveOgImage clave en el almacen, no una URL: la URL publica cambia
     *     segun donde este el almacen y la compone quien pinta
     */
    public record AjustesDelSitio(
            String titulo,
            String descripcion,
            String claveOgImage,
            String nombre,
            String claveLogo,
            String urlPlugin) {}
}
