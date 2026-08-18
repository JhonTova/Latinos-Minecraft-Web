package com.latinosmc.voting;

import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lectura y escritura de las reglas de voto. */
@Repository
public class VoteSettingsRepository {

    private static final Logger log = LoggerFactory.getLogger(VoteSettingsRepository.class);

    private final JdbcClient jdbc;

    public VoteSettingsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Las reglas vigentes.
     *
     * <p>Se lee en cada voto, sin cache. Es una fila unica con clave primaria: a
     * PostgreSQL le cuesta menos resolverlo que a nosotros invalidar una cache, y
     * cachearlo significaria que un cambio en administracion tarda en aplicarse
     * justo cuando se cambia por urgencia.
     */
    public VoteSettings actuales() {
        try {
            return jdbc.sql(
                            """
                            SELECT one_vote_per_day, max_players_per_ip, window_hours
                              FROM vote_settings
                             WHERE id IS TRUE
                            """)
                    .query((rs, fila) -> new VoteSettings(
                            rs.getBoolean("one_vote_per_day"),
                            rs.getInt("max_players_per_ip"),
                            Duration.ofHours(rs.getInt("window_hours"))))
                    .single();

        } catch (RuntimeException e) {
            // Si la configuracion no se puede leer, se sigue con los valores
            // permisivos en vez de tumbar la funcion principal del producto.
            log.error("No se pudieron leer los ajustes de voto; se usan los permisivos", e);
            return VoteSettings.porDefecto();
        }
    }

    public void guardar(VoteSettings ajustes, UUID actorId) {
        jdbc.sql(
                        """
                        UPDATE vote_settings
                           SET one_vote_per_day   = :global,
                               max_players_per_ip = :porIp,
                               window_hours       = :horas,
                               updated_at         = now(),
                               updated_by         = :actor
                         WHERE id IS TRUE
                        """)
                .param("global", ajustes.unVotoAlDia())
                .param("porIp", ajustes.maxJugadoresPorIp())
                .param("horas", (int) ajustes.ventana().toHours())
                .param("actor", actorId)
                .update();
    }
}
