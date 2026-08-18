package com.latinosmc.app.config;

import com.latinosmc.identity.ProveedorOAuth;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proveedores de acceso externo.
 *
 * <p>Sin credenciales, el proveedor simplemente no se ofrece: el boton no
 * aparece y el endpoint responde que no esta disponible. Asi el proyecto arranca
 * recien clonado sin exigir dar de alta aplicaciones en Google y Discord solo
 * para poder verlo funcionar.
 */
@Configuration
public class OAuthConfig {

    @Bean
    public Map<String, ProveedorOAuth> proveedoresOAuth(
            @Value("${latinosmc.oauth.google.client-id:}") String googleId,
            @Value("${latinosmc.oauth.google.client-secret:}") String googleSecreto,
            @Value("${latinosmc.oauth.discord.client-id:}") String discordId,
            @Value("${latinosmc.oauth.discord.client-secret:}") String discordSecreto) {

        Map<String, ProveedorOAuth> proveedores = new LinkedHashMap<>();
        proveedores.put("GOOGLE", ProveedorOAuth.google(googleId, googleSecreto));
        proveedores.put("DISCORD", ProveedorOAuth.discord(discordId, discordSecreto));
        return proveedores;
    }
}
