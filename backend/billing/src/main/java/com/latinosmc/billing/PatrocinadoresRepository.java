package com.latinosmc.billing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Patrocinadores de marca: hostings, VPS y similares.
 *
 * <p>Distinto de los espacios destacados. Aquellos los compra un dueño de
 * servidor por si mismo con una tarjeta; estos se acuerdan hablando con una
 * empresa, y por eso los da de alta un administrador en vez de venir por una
 * pasarela.
 *
 * <p><strong>Nada se publica sin aprobar.</strong> Un enlace saliente desde
 * nuestro dominio es nuestra reputacion: si apunta a una estafa, el problema no
 * es de quien pago, es nuestro. Por eso {@code approved_at} empieza vacio y hay
 * que aprobarlo a mano, incluso cuando lo crea el propio administrador.
 */
@Repository
public class PatrocinadoresRepository {

    private final JdbcClient jdbc;

    public PatrocinadoresRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Los que se ven ahora mismo en la web.
     *
     * <p>Tres condiciones a la vez: aprobado, sin cancelar y dentro de su
     * periodo. Que falte cualquiera basta para no publicarlo.
     */
    public List<Patrocinador> vigentes(Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT id, name, target_url, logo_key, starts_at, ends_at,
                               display_order, approved_at, cancelled_at
                          FROM brand_sponsors
                         WHERE cancelled_at IS NULL
                           AND approved_at IS NOT NULL
                           AND starts_at <= :ahora
                           AND ends_at    > :ahora
                         ORDER BY display_order, name
                        """)
                .param("ahora", Timestamp.from(ahora))
                .query(PatrocinadoresRepository::mapear)
                .list();
    }

    /** Todos, para la pantalla de administracion: incluye los pendientes de aprobar. */
    public List<Patrocinador> todos() {
        return jdbc.sql(
                        """
                        SELECT id, name, target_url, logo_key, starts_at, ends_at,
                               display_order, approved_at, cancelled_at
                          FROM brand_sponsors
                         ORDER BY cancelled_at NULLS FIRST, display_order, created_at DESC
                         LIMIT 200
                        """)
                .query(PatrocinadoresRepository::mapear)
                .list();
    }

    /** Se crea SIN aprobar. Aprobar es siempre un segundo acto deliberado. */
    public void crear(
            UUID id,
            String nombre,
            String urlDestino,
            String claveLogo,
            Instant desde,
            Instant hasta,
            int orden) {

        jdbc.sql(
                        """
                        INSERT INTO brand_sponsors
                            (id, name, target_url, logo_key, starts_at, ends_at, display_order)
                        VALUES
                            (:id, :nombre, :url, :logo, :desde, :hasta, :orden)
                        """)
                .param("id", id)
                .param("nombre", nombre)
                .param("url", urlDestino)
                .param("logo", claveLogo)
                .param("desde", Timestamp.from(desde))
                .param("hasta", Timestamp.from(hasta))
                .param("orden", orden)
                .update();
    }

    /** @return true si cambio algo; false si no existe o ya estaba aprobado */
    public boolean aprobar(UUID id) {
        return jdbc.sql("UPDATE brand_sponsors SET approved_at = now() WHERE id = :id AND approved_at IS NULL")
                        .param("id", id)
                        .update()
                > 0;
    }

    /**
     * Retira un patrocinador de la web.
     *
     * <p>Se marca, no se borra: puede tener un pedido asociado, y un patrocinio
     * que se cobro y luego se retiro es justo lo que hay que poder explicar.
     */
    public boolean cancelar(UUID id) {
        return jdbc.sql("UPDATE brand_sponsors SET cancelled_at = now() WHERE id = :id AND cancelled_at IS NULL")
                        .param("id", id)
                        .update()
                > 0;
    }

    private static Patrocinador mapear(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        Timestamp aprobado = rs.getTimestamp("approved_at");
        Timestamp cancelado = rs.getTimestamp("cancelled_at");

        return new Patrocinador(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("target_url"),
                rs.getString("logo_key"),
                rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(),
                rs.getInt("display_order"),
                aprobado == null ? null : aprobado.toInstant(),
                cancelado == null ? null : cancelado.toInstant());
    }

    /**
     * @param claveLogo clave en el almacen, no una URL. La URL publica la compone
     *     quien pinta, porque cambia segun donde este el almacen.
     * @param aprobadoEn null mientras no se haya aprobado; sin esto no se publica
     */
    public record Patrocinador(
            UUID id,
            String nombre,
            String urlDestino,
            String claveLogo,
            Instant desde,
            Instant hasta,
            int orden,
            Instant aprobadoEn,
            Instant canceladoEn) {}
}
