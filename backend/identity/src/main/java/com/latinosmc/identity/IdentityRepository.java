package com.latinosmc.identity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a usuarios y sesiones. Solo el modulo {@code identity} toca estas tablas. */
@Repository
public class IdentityRepository {

    private final JdbcClient jdbc;

    public IdentityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // --- Usuarios ------------------------------------------------------------

    public boolean existeEmail(String email) {
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*) FROM users
                         WHERE email = :email AND deleted_at IS NULL AND anonymized_at IS NULL
                        """)
                .param("email", email)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    public void crearUsuario(UUID id, String email, String nombre, String passwordHash) {
        jdbc.sql(
                        """
                        INSERT INTO users (id, email, display_name, password_hash)
                        VALUES (:id, :email, :nombre, :hash)
                        """)
                .param("id", id)
                .param("email", email)
                .param("nombre", nombre)
                .param("hash", passwordHash)
                .update();
    }

    public Optional<UsuarioConCredencial> buscarPorEmail(String email) {
        return jdbc.sql(
                        """
                        SELECT id, email, display_name, password_hash
                          FROM users
                         WHERE email = :email AND deleted_at IS NULL AND anonymized_at IS NULL
                        """)
                .param("email", email)
                .query((rs, fila) -> new UsuarioConCredencial(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("password_hash")))
                .optional();
    }

    public Optional<Usuario> buscarPorId(UUID id) {
        return jdbc.sql(
                        """
                        SELECT id, email, display_name
                          FROM users
                         WHERE id = :id AND deleted_at IS NULL AND anonymized_at IS NULL
                        """)
                .param("id", id)
                .query((rs, fila) -> new Usuario(
                        rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("display_name")))
                .optional();
    }

    // --- Sesiones ------------------------------------------------------------

    public void crearSesion(
            UUID id, UUID usuarioId, String tokenHash, String userAgent, byte[] ipHash, Instant caduca) {

        jdbc.sql(
                        """
                        INSERT INTO sessions (id, user_id, token_hash, user_agent, ip_hash, expires_at)
                        VALUES (:id, :usuario, :hash, :agente, :ip, :caduca)
                        """)
                .param("id", id)
                .param("usuario", usuarioId)
                .param("hash", tokenHash)
                .param("agente", recortar(userAgent))
                .param("ip", ipHash)
                .param("caduca", Timestamp.from(caduca))
                .update();
    }

    /** Usuario dueno de una sesion viva. Es la consulta de cada peticion autenticada. */
    public Optional<Usuario> usuarioDeSesion(String tokenHash, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT u.id, u.email, u.display_name
                          FROM sessions s
                          JOIN users u ON u.id = s.user_id
                         WHERE s.token_hash = :hash
                           AND s.revoked_at IS NULL
                           AND s.expires_at > :ahora
                           AND u.deleted_at IS NULL
                           AND u.anonymized_at IS NULL
                        """)
                .param("hash", tokenHash)
                .param("ahora", Timestamp.from(ahora))
                .query((rs, fila) -> new Usuario(
                        rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("display_name")))
                .optional();
    }

    /**
     * Actualiza la marca de ultimo uso.
     *
     * <p>Se hace en una sentencia aparte y sin bloquear la respuesta: es
     * informativo para el panel de sesiones, no forma parte de la autenticacion.
     */
    public void tocarSesion(String tokenHash, Instant ahora) {
        jdbc.sql("UPDATE sessions SET last_seen_at = :ahora WHERE token_hash = :hash")
                .param("hash", tokenHash)
                .param("ahora", Timestamp.from(ahora))
                .update();
    }

    public int revocarSesion(String tokenHash, Instant ahora, String motivo) {
        return jdbc.sql(
                        """
                        UPDATE sessions
                           SET revoked_at = :ahora, revoked_reason = :motivo
                         WHERE token_hash = :hash AND revoked_at IS NULL
                        """)
                .param("hash", tokenHash)
                .param("ahora", Timestamp.from(ahora))
                .param("motivo", motivo)
                .update();
    }

    /** Cierra todas las sesiones de un usuario. Se usa al cambiar la contrasena. */
    public int revocarTodasLasSesiones(UUID usuarioId, Instant ahora, String motivo) {
        return jdbc.sql(
                        """
                        UPDATE sessions
                           SET revoked_at = :ahora, revoked_reason = :motivo
                         WHERE user_id = :usuario AND revoked_at IS NULL
                        """)
                .param("usuario", usuarioId)
                .param("ahora", Timestamp.from(ahora))
                .param("motivo", motivo)
                .update();
    }

    public List<SesionActiva> sesionesDe(UUID usuarioId, Instant ahora) {
        return jdbc.sql(
                        """
                        SELECT id, user_agent, created_at, last_seen_at
                          FROM sessions
                         WHERE user_id = :usuario AND revoked_at IS NULL AND expires_at > :ahora
                         ORDER BY last_seen_at DESC
                        """)
                .param("usuario", usuarioId)
                .param("ahora", Timestamp.from(ahora))
                .query((rs, fila) -> new SesionActiva(
                        rs.getObject("id", UUID.class),
                        rs.getString("user_agent"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_seen_at").toInstant()))
                .list();
    }

    private String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.length() > 255 ? valor.substring(0, 255) : valor;
    }

    public record Usuario(UUID id, String email, String nombre) {}

    public record UsuarioConCredencial(UUID id, String email, String nombre, String passwordHash) {}

    public record SesionActiva(UUID id, String userAgent, Instant creada, Instant ultimoUso) {}
}
