package com.latinosmc.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Quien puede administrar la plataforma entera.
 *
 * <p>Es un eje distinto de los roles de organizacion: alli se decide quien manda
 * en <em>su</em> servidor; aqui, quien puede cambiar los precios de todos y
 * mirar los pedidos de cualquiera.
 *
 * <p>No hay metodo para conceder el primer permiso. Se hace con SQL al
 * desplegar, a proposito: un endpoint de arranque —aunque solo funcionara con la
 * tabla vacia— es una puerta que hace falta una vez y queda abierta para
 * siempre.
 */
@Repository
public class PlatformAdminRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public PlatformAdminRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Si el usuario es administrador <strong>ahora mismo</strong>.
     *
     * <p>Se consulta en cada peticion administrativa y no se cachea. Cachearlo
     * significaria que revocar el permiso a alguien tarda en surtir efecto, y
     * revocar un permiso suele ser urgente justo cuando mas prisa corre.
     */
    public boolean esAdministrador(UUID usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*)
                          FROM platform_admins
                         WHERE user_id = :usuario
                           AND revoked_at IS NULL
                        """)
                .param("usuario", usuarioId)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    public List<Administrador> administradores() {
        return jdbc.sql(
                        """
                        SELECT a.user_id, u.email, u.display_name, a.granted_at, a.notes
                          FROM platform_admins a
                          JOIN users u ON u.id = a.user_id
                         WHERE a.revoked_at IS NULL
                         ORDER BY a.granted_at
                        """)
                .query((rs, fila) -> new Administrador(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getTimestamp("granted_at").toInstant(),
                        rs.getString("notes")))
                .list();
    }

    /**
     * Registra una accion administrativa en el diario de auditoria.
     *
     * <p>Cambiar un precio es una accion sobre dinero. Sin rastro, un cobro
     * inesperado no se puede explicar: nadie sabria si el precio era ese cuando
     * el cliente compro o si alguien lo cambio despues.
     *
     * <p>Va a {@code audit_log} (V6) y no a una tabla propia porque esa ya
     * existe y ademas seudonimiza al actor cuando vence su plazo de retencion
     * (ADR-0010). Una tabla nueva se saltaria ese plazo sin que nadie lo notara.
     */
    public void registrar(
            UUID eventoId, UUID actorId, String accion, String tipoRecurso, String recursoId, Object detalle) {

        String metadatos;
        try {
            metadatos = json.writeValueAsString(
                    java.util.Map.of("recurso", recursoId == null ? "" : recursoId, "cambio", detalle));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Que falle el serializado del detalle no puede impedir que quede
            // constancia de la accion. Se guarda sin detalle antes que nada.
            metadatos = "{}";
        }

        jdbc.sql(
                        """
                        INSERT INTO audit_log
                            (id, actor_type, actor_user_id, action, resource_type, resource_id, metadata)
                        VALUES
                            (:id, 'PLATFORM_ADMIN', :actor, :accion, :tipo, :recurso, CAST(:meta AS jsonb))
                        """)
                .param("id", eventoId)
                .param("actor", actorId)
                .param("accion", accion)
                .param("tipo", tipoRecurso)
                // resource_id es UUID en audit_log; los recursos con clave no-UUID
                // (un slot es el numero 1..5) viajan dentro de los metadatos.
                .param("recurso", (Object) null)
                .param("meta", metadatos)
                .update();
    }

    public record Administrador(UUID id, String email, String nombre, Instant desde, String notas) {}

    /**
     * Concede el permiso, o lo devuelve si estaba revocado.
     *
     * <p>{@code concedidoPor} es null solo para el primer administrador, el que
     * se crea con SQL al desplegar. A partir de ahi siempre hay alguien detras.
     */
    public void conceder(UUID usuarioId, UUID concedidoPor, String notas) {
        jdbc.sql(
                        """
                        INSERT INTO platform_admins (user_id, granted_by, notes)
                        VALUES (:usuario, :por, :notas)
                        ON CONFLICT (user_id) DO UPDATE
                            SET revoked_at = NULL, granted_at = :ahora, notes = :notas
                        """)
                .param("usuario", usuarioId)
                .param("por", concedidoPor)
                .param("notas", notas)
                .param("ahora", Timestamp.from(Instant.now()))
                .update();
    }
}
