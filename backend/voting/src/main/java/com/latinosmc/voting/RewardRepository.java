package com.latinosmc.voting;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Recompensas pendientes de entrega. */
@Repository
public class RewardRepository {

    private static final int MAX_INTENTOS = 10;

    private final JdbcClient jdbc;

    public RewardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Recompensas que el plugin de un servidor todavia no ha confirmado.
     *
     * <p>Se devuelve tambien la identidad del jugador porque el plugin la necesita
     * para entregar. Va como dato tipado, no incrustada en ningun comando
     * (ADR-0012).
     */
    public List<RecompensaPendiente> pendientesDe(UUID servidorId, int limite) {
        return jdbc.sql(
                        """
                        SELECT r.id, r.reward_type, v.identity_type_code, v.identity_value, r.attempts
                          FROM vote_rewards r
                          JOIN votes v ON v.id = r.vote_id
                         WHERE r.server_id = :servidor
                           AND r.status_code IN ('PENDING', 'FAILED')
                           AND r.attempts < :maxIntentos
                         ORDER BY r.created_at
                         LIMIT :limite
                        """)
                .param("servidor", servidorId)
                .param("maxIntentos", MAX_INTENTOS)
                .param("limite", limite)
                .query((rs, fila) -> new RecompensaPendiente(
                        rs.getObject("id", UUID.class),
                        rs.getString("reward_type"),
                        rs.getString("identity_type_code"),
                        rs.getString("identity_value"),
                        rs.getInt("attempts")))
                .list();
    }

    /**
     * Marca recompensas como entregadas.
     *
     * <p>La condicion sobre {@code server_id} no es decorativa: sin ella, un
     * servidor con una clave valida podria confirmar recompensas de OTRO servidor
     * enviando identificadores ajenos. Es el control de acceso a nivel de objeto
     * que OWASP llama BOLA, y es el fallo numero uno en APIs multi-tenant.
     */
    public int marcarEntregadas(UUID servidorId, List<UUID> recompensaIds) {
        if (recompensaIds.isEmpty()) {
            return 0;
        }
        return jdbc.sql(
                        """
                        UPDATE vote_rewards
                           SET status_code = 'DELIVERED',
                               delivered_at = now(),
                               updated_at = now()
                         WHERE server_id = :servidor
                           AND id = ANY(:ids)
                           AND status_code IN ('PENDING', 'FAILED')
                        """)
                .param("servidor", servidorId)
                .param("ids", recompensaIds.toArray(new UUID[0]))
                .update();
    }

    /** Suma un intento a las recompensas entregadas al plugin, para no reintentar eternamente. */
    public void registrarIntento(UUID servidorId, List<UUID> recompensaIds) {
        if (recompensaIds.isEmpty()) {
            return;
        }
        jdbc.sql(
                        """
                        UPDATE vote_rewards
                           SET attempts = attempts + 1,
                               updated_at = now()
                         WHERE server_id = :servidor
                           AND id = ANY(:ids)
                        """)
                .param("servidor", servidorId)
                .param("ids", recompensaIds.toArray(new UUID[0]))
                .update();
    }

    /**
     * @param tipo tipo de recompensa; el plugin lo traduce a la accion concreta
     * @param identidadTipo como identificar al jugador
     * @param identidadValor identidad del jugador
     */
    public record RecompensaPendiente(
            UUID id, String tipo, String identidadTipo, String identidadValor, int intentos) {}
}
