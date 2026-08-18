package com.latinosmc.voting;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a los tokens de voto (ADR-0016). */
@Repository
public class VoteTokenRepository {

    private final JdbcClient jdbc;

    public VoteTokenRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void guardar(
            UUID id,
            UUID servidorId,
            String tokenHash,
            String identidadTipo,
            String identidadValor,
            Instant caduca) {

        jdbc.sql(
                        """
                        INSERT INTO vote_tokens (id, server_id, token_hash, identity_type_code,
                                                 identity_value, expires_at)
                        VALUES (:id, :servidor, :hash, :tipo, :valor, :caduca)
                        """)
                .param("id", id)
                .param("servidor", servidorId)
                .param("hash", tokenHash)
                .param("tipo", identidadTipo)
                .param("valor", identidadValor)
                .param("caduca", Timestamp.from(caduca))
                .update();
    }

    /**
     * Caduca los tokens sin usar que ese jugador tuviera en ese servidor.
     *
     * <p>Se marcan como caducados en vez de borrarlos: si mas adelante hace falta
     * investigar un patron raro de peticiones, el rastro sigue ahi.
     */
    public int caducarAnteriores(UUID servidorId, String identidadValor, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE vote_tokens
                           SET expires_at = :ahora
                         WHERE server_id = :servidor
                           AND identity_value = :valor
                           AND consumed_at IS NULL
                           AND expires_at > :ahora
                        """)
                .param("servidor", servidorId)
                .param("valor", identidadValor)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    /** Datos del token si sigue vivo y sin usar. */
    public Optional<VoteTokenService.DatosDelToken> buscarVigente(String tokenHash, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT t.server_id, s.slug, s.name, t.identity_type_code, t.identity_value
                          FROM vote_tokens t
                          JOIN servers s ON s.id = t.server_id
                         WHERE t.token_hash = :hash
                           AND t.consumed_at IS NULL
                           AND t.expires_at > :ahora
                           AND s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                        """)
                .param("hash", tokenHash)
                .param("ahora", Timestamp.from(ahora))
                .query((rs, fila) -> new VoteTokenService.DatosDelToken(
                        rs.getObject("server_id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("identity_type_code"),
                        rs.getString("identity_value")))
                .optional();
    }

    /**
     * Marca el token como usado.
     *
     * <p>Las condiciones {@code consumed_at IS NULL} y {@code expires_at} viajan
     * dentro del propio UPDATE. Es lo que hace que dos pulsaciones simultaneas del
     * boton no puedan producir dos votos: la base decide, no el codigo.
     */
    public int consumir(String tokenHash, UUID votoId, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE vote_tokens
                           SET consumed_at = :ahora, consumed_vote_id = :voto
                         WHERE token_hash = :hash
                           AND consumed_at IS NULL
                           AND expires_at > :ahora
                        """)
                .param("hash", tokenHash)
                .param("voto", votoId)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    /**
     * Voto que se creo con este token, si ya se uso.
     *
     * <p>Sirve para que un doble clic o un "volver atras y repetir" muestren
     * "ya votaste" en vez de "enlace no valido". Son cosas distintas para el
     * jugador: la primera es tranquilizadora, la segunda parece un fallo.
     */
    public Optional<Vote> votoDeTokenConsumido(String tokenHash) {
        return jdbc.sql(
                        """
                        SELECT v.id, v.server_id, v.status_code, v.identity_type_code, v.identity_value,
                               v.identity_hash, v.risk_score, v.rejection_reason, v.created_at
                          FROM vote_tokens t
                          JOIN votes v ON v.id = t.consumed_vote_id
                         WHERE t.token_hash = :hash
                        """)
                .param("hash", tokenHash)
                .query((rs, fila) -> new Vote(
                        rs.getObject("id", UUID.class),
                        rs.getObject("server_id", UUID.class),
                        VoteStatus.valueOf(rs.getString("status_code")),
                        rs.getString("identity_type_code"),
                        rs.getString("identity_value"),
                        rs.getBytes("identity_hash"),
                        rs.getObject("risk_score", Integer.class),
                        rs.getString("rejection_reason"),
                        rs.getTimestamp("created_at").toInstant()))
                .optional();
    }

    /** Purga los tokens viejos. Sin esto, la tabla solo crece. */
    public int purgarAnterioresA(Instant limite) {
        return jdbc.sql("DELETE FROM vote_tokens WHERE created_at < :limite")
                .param("limite", Timestamp.from(limite))
                .update();
    }
}
