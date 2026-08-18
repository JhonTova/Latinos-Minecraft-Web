package com.latinosmc.identity;

/**
 * Configuracion de un proveedor de identidad externo.
 *
 * <p>Discord antes que Facebook, deliberadamente: en el ecosistema de Minecraft
 * la comunidad entera vive en Discord, y Facebook no lo usa practicamente nadie
 * de este publico.
 */
public record ProveedorOAuth(
        String codigo,
        String nombre,
        String clienteId,
        String clienteSecreto,
        String urlAutorizacion,
        String urlToken,
        String urlPerfil,
        String scopes) {

    /** Un proveedor sin credenciales no se ofrece: el boton ni siquiera aparece. */
    public boolean estaConfigurado() {
        return clienteId != null && !clienteId.isBlank() && clienteSecreto != null && !clienteSecreto.isBlank();
    }

    public static ProveedorOAuth google(String clienteId, String secreto) {
        return new ProveedorOAuth(
                "GOOGLE",
                "Google",
                clienteId,
                secreto,
                "https://accounts.google.com/o/oauth2/v2/auth",
                "https://oauth2.googleapis.com/token",
                "https://openidconnect.googleapis.com/v1/userinfo",
                "openid email profile");
    }

    public static ProveedorOAuth discord(String clienteId, String secreto) {
        return new ProveedorOAuth(
                "DISCORD",
                "Discord",
                clienteId,
                secreto,
                "https://discord.com/oauth2/authorize",
                "https://discord.com/api/oauth2/token",
                "https://discord.com/api/users/@me",
                // Solo lo imprescindible: quien es y su correo. Pedir mas permisos
                // de los necesarios asusta al usuario en la pantalla de consentimiento
                // y amplia el dano si algun dia se filtra un token.
                "identify email");
    }
}
