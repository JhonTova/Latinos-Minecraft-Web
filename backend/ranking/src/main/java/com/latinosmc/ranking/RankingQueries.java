package com.latinosmc.ranking;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas del ranking.
 *
 * <p>Es un read model: se lee, nunca se escribe. La posicion no se almacena — se
 * deriva de los votos aceptados. Guardar la posicion invitaria a que algun dia
 * alguien la editase a mano, y ahi se acaba la credibilidad del ranking.
 *
 * <p>Se apoya en el indice parcial {@code votes_por_servidor_aceptados}. Cuando
 * el volumen lo justifique, este calculo pasara a una vista materializada
 * refrescada por el planificador; la interfaz no notara el cambio.
 */
@Repository
public class RankingQueries {

    private final JdbcClient jdbc;

    public RankingQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ranking organico por votos aceptados en los ultimos dias.
     *
     * <p>Solo servidores activos y solo votos ACEPTADOS: un voto rechazado por el
     * antifraude no puede mover una posicion, que es justamente el objetivo.
     *
     * <p>Los slots patrocinados NO aparecen aqui (ADR-0013). Se consultan aparte y
     * se pintan en un bloque separado. Mezclarlos destruiria el activo que hace
     * valioso al ranking.
     */
    public List<FilaRanking> ranking(int dias, int limite) {
        return ranking(dias, limite, null);
    }

    /**
     * El mismo ranking, acotado a una modalidad de juego.
     *
     * <p>Es lo que sostiene las paginas por categoria. Se resuelve con la misma
     * consulta y no con un filtro en memoria porque el limite tiene que aplicarse
     * DESPUES de filtrar: traer los 50 mejores del total y quedarse con los de
     * skyblock devolveria tres, y la pagina de skyblock parecria vacia teniendo
     * cuarenta servidores.
     *
     * @param modalidad codigo del catalogo, o {@code null} para no filtrar
     */
    public List<FilaRanking> ranking(int dias, int limite, String modalidad) {
        return jdbc.sql(
                        """
                        SELECT s.id,
                               s.slug,
                               s.name          AS nombre,
                               s.description   AS descripcion,
                               -- Calculadas por la base de datos: resuelven solas si
                               -- la imagen es la que subio el dueño o la que sacamos
                               -- de su ping (ADR-0027).
                               s.display_icon_key   AS icono,
                               s.display_banner_key AS banner,
                               s.host          AS host,
                               s.port          AS puerto,
                               COUNT(v.id)     AS votos,
                               p.players_online,
                               p.is_online
                          FROM servers s
                          LEFT JOIN votes v
                                 ON v.server_id = s.id
                                AND v.status_code = 'ACCEPTED'
                                AND v.created_at >= now() - make_interval(days => :dias)
                          LEFT JOIN LATERAL (
                                SELECT players_online, players_max, is_online
                                  FROM server_pings sp
                                 WHERE sp.server_id = s.id
                                 ORDER BY sp.observed_at DESC
                                 LIMIT 1
                          ) p ON TRUE
                         WHERE s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                           AND (CAST(:modalidad AS TEXT) IS NULL
                                OR EXISTS (SELECT 1
                                             FROM server_game_modes m
                                            WHERE m.server_id = s.id
                                              AND m.mode_code = :modalidad))
                         GROUP BY s.id, s.slug, s.name, s.description, s.display_icon_key, s.display_banner_key,
                                  s.host, s.port, p.players_online, p.is_online
                         ORDER BY votos DESC, s.name ASC
                         LIMIT :limite
                        """)
                .param("dias", dias)
                .param("limite", limite)
                .param("modalidad", modalidad)
                .query((rs, fila) -> new FilaRanking(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("icono"),
                        rs.getString("banner"),
                        rs.getString("host"),
                        rs.getObject("puerto", Integer.class),
                        rs.getLong("votos"),
                        rs.getObject("players_online", Integer.class),
                        rs.getObject("is_online", Boolean.class)))
                .list();
    }

    /**
     * Cifras de la plataforma para la portada.
     *
     * <p>Vive en {@code ranking} y no en {@code servers} porque cruza servidores
     * con votos, y este modulo ya lee ambas tablas con la deuda registrada en el
     * ADR-0019. Ponerlo en {@code servers} habria añadido una lectura nueva entre
     * modulos sin apuntarla, que es justo lo prohibido.
     *
     * <p>Los jugadores solo se suman de pings recientes. Sin ese limite, un
     * servidor caido hace tres dias seguiria aportando los 300 jugadores que
     * tenia la ultima vez que respondio, y el contador de la portada seria una
     * cifra bonita que solo sube.
     */
    public Cifras cifras() {
        return jdbc.sql(
                        """
                        SELECT
                            (SELECT COUNT(*) FROM servers
                              WHERE status_code = 'ACTIVE' AND deleted_at IS NULL) AS servidores,

                            (SELECT COUNT(*) FROM servers s
                               JOIN LATERAL (
                                     SELECT is_online FROM server_pings sp
                                      WHERE sp.server_id = s.id
                                        AND sp.observed_at >= now() - interval '15 minutes'
                                      ORDER BY sp.observed_at DESC LIMIT 1
                               ) p ON TRUE
                              WHERE s.status_code = 'ACTIVE' AND s.deleted_at IS NULL
                                AND p.is_online)                                   AS en_linea,

                            (SELECT COALESCE(SUM(p.players_online), 0) FROM servers s
                               JOIN LATERAL (
                                     SELECT players_online, is_online FROM server_pings sp
                                      WHERE sp.server_id = s.id
                                        AND sp.observed_at >= now() - interval '15 minutes'
                                      ORDER BY sp.observed_at DESC LIMIT 1
                               ) p ON TRUE
                              WHERE s.status_code = 'ACTIVE' AND s.deleted_at IS NULL
                                AND p.is_online)                                   AS jugadores,

                            (SELECT COUNT(*) FROM votes
                              WHERE status_code = 'ACCEPTED'
                                AND created_at >= now() - interval '24 hours')      AS votos_24h
                        """)
                .query((rs, fila) -> new Cifras(
                        rs.getInt("servidores"),
                        rs.getInt("en_linea"),
                        rs.getLong("jugadores"),
                        rs.getLong("votos_24h")))
                .single();
    }

    /**
     * @param servidores todos los activos, respondan o no: un servidor caido
     *     sigue existiendo y sigue apareciendo en la lista
     * @param jugadores suma de conectados, medida por nosotros (ADR-0009)
     * @param votos24h solo los ACEPTADOS: contar los rechazados por el antifraude
     *     seria presumir de trafico que precisamente descartamos
     */
    public record Cifras(int servidores, int enLinea, long jugadores, long votos24h) {}

    /**
     * Votos aceptados por dia de un servidor, para el minigrafico de tendencia.
     * Devuelve una fila por dia con datos; los dias sin votos se rellenan arriba.
     */
    public List<Long> tendenciaDiaria(UUID servidorId, int dias) {
        return jdbc.sql(
                        """
                        WITH dias AS (
                            SELECT generate_series(
                                       date_trunc('day', now()) - make_interval(days => :dias - 1),
                                       date_trunc('day', now()),
                                       '1 day'
                                   ) AS dia
                        )
                        SELECT COUNT(v.id) AS votos
                          FROM dias d
                          LEFT JOIN votes v
                                 ON v.server_id = :servidor
                                AND v.status_code = 'ACCEPTED'
                                AND date_trunc('day', v.created_at) = d.dia
                         GROUP BY d.dia
                         ORDER BY d.dia
                        """)
                .param("servidor", servidorId)
                .param("dias", dias)
                .query(Long.class)
                .list();
    }

    /**
     * @param jugadoresConectados medido por NOSOTROS con ping directo (ADR-0009);
     *     null si aun no se ha medido
     */
    public record FilaRanking(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String claveIcono,
            String claveBanner,
            String host,
            Integer puerto,
            long votos,
            Integer jugadoresConectados,
            Boolean enLinea) {}
}
