package com.latinosmc.trust;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas de senales para el motor antifraude.
 *
 * <p>Nota sobre fronteras: estas consultas leen la tabla {@code votes}, que
 * pertenece al modulo {@code voting}. Es una deuda deliberada y prudente
 * (ADR-0007) registrada en ADR-0015: el antifraude necesita el historico completo
 * de votos y exponerlo por eventos duplicaria todo el volumen sin beneficio real
 * en esta fase. Se paga extrayendo un read model propio cuando el antifraude
 * crezca en reglas.
 */
@Repository
public class TrustQueries {

    private final JdbcClient jdbc;

    public TrustQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Cuantas identidades distintas han votado desde esta IP en una ventana.
     *
     * <p>Es la senal mas util contra granjas de cuentas: una casa compartida da 2
     * o 3 jugadores; treinta identidades desde la misma IP no es una familia.
     */
    public int identidadesDistintasPorIp(byte[] ipHash, Instant desde) {
        if (ipHash == null) {
            return 0;
        }
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(DISTINCT identity_hash)
                          FROM votes
                         WHERE ip_hash = :ip
                           AND created_at >= :desde
                        """)
                .param("ip", ipHash)
                .param("desde", Timestamp.from(desde))
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }

    /** Votos totales desde esta IP en una ventana corta. Detecta rafagas automatizadas. */
    public int votosPorIp(byte[] ipHash, Instant desde) {
        if (ipHash == null) {
            return 0;
        }
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*)
                          FROM votes
                         WHERE ip_hash = :ip
                           AND created_at >= :desde
                        """)
                .param("ip", ipHash)
                .param("desde", Timestamp.from(desde))
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }

    /** Votos aceptados historicos de esta identidad. Cero significa votante nuevo. */
    public int historialIdentidad(byte[] identidadHash) {
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*)
                          FROM votes
                         WHERE identity_hash = :hash
                           AND status_code = 'ACCEPTED'
                        """)
                .param("hash", identidadHash)
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }
}
