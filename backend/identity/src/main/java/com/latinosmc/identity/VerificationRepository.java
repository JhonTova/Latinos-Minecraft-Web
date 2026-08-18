package com.latinosmc.identity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a los codigos de verificacion de correo. */
@Repository
public class VerificationRepository {

    private final JdbcClient jdbc;

    public VerificationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void guardar(UUID id, UUID usuarioId, String codeHash, String email, Instant caduca) {
        jdbc.sql(
                        """
                        INSERT INTO email_verification_codes (id, user_id, code_hash, email, expires_at)
                        VALUES (:id, :usuario, :hash, :email, :caduca)
                        """)
                .param("id", id)
                .param("usuario", usuarioId)
                .param("hash", codeHash)
                .param("email", email)
                .param("caduca", Timestamp.from(caduca))
                .update();
    }

    /** Instante del ultimo codigo enviado, para respetar la espera entre envios. */
    public Optional<Instant> ultimoEnvio(UUID usuarioId) {
        return jdbc.sql(
                        """
                        SELECT created_at FROM email_verification_codes
                         WHERE user_id = :usuario
                         ORDER BY created_at DESC
                         LIMIT 1
                        """)
                .param("usuario", usuarioId)
                .query(Instant.class)
                .optional();
    }

    /**
     * Caduca los codigos anteriores sin usar.
     *
     * <p>Solo puede haber uno vivo a la vez: si convivieran varios, agotar los
     * intentos de uno no serviria de nada porque quedarian los otros.
     */
    public int invalidarAnteriores(UUID usuarioId, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE email_verification_codes
                           SET expires_at = :ahora
                         WHERE user_id = :usuario
                           AND consumed_at IS NULL
                           AND expires_at > :ahora
                        """)
                .param("usuario", usuarioId)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    public Optional<CodigoVigente> codigoVigente(UUID usuarioId, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT id, code_hash, email, attempts
                          FROM email_verification_codes
                         WHERE user_id = :usuario
                           AND consumed_at IS NULL
                           AND expires_at > :ahora
                         ORDER BY created_at DESC
                         LIMIT 1
                        """)
                .param("usuario", usuarioId)
                .param("ahora", Timestamp.from(ahora))
                .query((rs, fila) -> new CodigoVigente(
                        rs.getObject("id", UUID.class),
                        rs.getString("code_hash"),
                        rs.getString("email"),
                        rs.getInt("attempts")))
                .optional();
    }

    public void sumarIntento(UUID codigoId) {
        jdbc.sql("UPDATE email_verification_codes SET attempts = attempts + 1 WHERE id = :id")
                .param("id", codigoId)
                .update();
    }

    public void marcarConsumido(UUID codigoId, Instant ahora) {
        jdbc.sql("UPDATE email_verification_codes SET consumed_at = :ahora WHERE id = :id")
                .param("id", codigoId)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    /**
     * Marca el correo como verificado.
     *
     * <p>La condicion sobre {@code email} no es decorativa: si el usuario cambio de
     * direccion despues de pedir el codigo, verificar la antigua no prueba nada
     * sobre la nueva.
     */
    public int marcarEmailVerificado(UUID usuarioId, String email, Instant ahora) {
        return jdbc.sql(
                        """
                        UPDATE users
                           SET email_verified_at = :ahora, updated_at = :ahora
                         WHERE id = :usuario
                           AND email = :email
                           AND email_verified_at IS NULL
                        """)
                .param("usuario", usuarioId)
                .param("email", email)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    public boolean tieneEmailVerificado(UUID usuarioId) {
        Integer n = jdbc.sql(
                        "SELECT COUNT(*) FROM users WHERE id = :usuario AND email_verified_at IS NOT NULL")
                .param("usuario", usuarioId)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    /** Purga los codigos viejos. Sin esto la tabla solo crece. */
    public int purgarAnterioresA(Instant limite) {
        return jdbc.sql("DELETE FROM email_verification_codes WHERE created_at < :limite")
                .param("limite", Timestamp.from(limite))
                .update();
    }

    public record CodigoVigente(UUID id, String codeHash, String email, int intentos) {}
}
