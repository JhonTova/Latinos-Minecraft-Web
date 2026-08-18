package com.latinosmc.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a las cuentas externas vinculadas (Google, Discord). */
@Repository
public class OAuthRepository {

    private final JdbcClient jdbc;

    public OAuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Usuario de la plataforma asociado a una cuenta externa.
     *
     * <p>Se busca por el identificador del proveedor, nunca por el correo. En
     * algunos proveedores el correo se puede cambiar; si se usara como enlace,
     * cambiarlo en Google permitiria apropiarse de la cuenta de otro aqui.
     */
    public Optional<UUID> buscarUsuario(String proveedor, String idEnProveedor) {
        return jdbc.sql(
                        """
                        SELECT user_id FROM oauth_accounts
                         WHERE provider = :proveedor AND provider_user_id = :id
                        """)
                .param("proveedor", proveedor)
                .param("id", idEnProveedor)
                .query(UUID.class)
                .optional();
    }

    public void vincular(UUID id, UUID usuarioId, String proveedor, String idEnProveedor, String correo) {
        jdbc.sql(
                        """
                        INSERT INTO oauth_accounts (id, user_id, provider, provider_user_id,
                                                    email_at_provider, last_login_at)
                        VALUES (:id, :usuario, :proveedor, :idProveedor, :correo, now())
                        ON CONFLICT (provider, provider_user_id) DO NOTHING
                        """)
                .param("id", id)
                .param("usuario", usuarioId)
                .param("proveedor", proveedor)
                .param("idProveedor", idEnProveedor)
                .param("correo", correo)
                .update();
    }

    public void registrarAcceso(String proveedor, String idEnProveedor) {
        jdbc.sql(
                        """
                        UPDATE oauth_accounts SET last_login_at = now()
                         WHERE provider = :proveedor AND provider_user_id = :id
                        """)
                .param("proveedor", proveedor)
                .param("id", idEnProveedor)
                .update();
    }
}
