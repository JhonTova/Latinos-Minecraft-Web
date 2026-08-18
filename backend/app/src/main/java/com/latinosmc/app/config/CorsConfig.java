package com.latinosmc.app.config;

import com.latinosmc.app.web.UsuarioActualResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para el frontend.
 *
 * <p>Los origenes se declaran explicitamente, nunca con comodin. Un
 * {@code allowedOrigins("*")} en una API que maneja votos y pagos convierte
 * cualquier web del mundo en cliente autorizado.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;
    private final UsuarioActualResolver usuarioActual;

    public CorsConfig(
            @Value("${latinosmc.cors.origenes:http://localhost:3000}") String origenes,
            UsuarioActualResolver usuarioActual) {
        this.origenesPermitidos = origenes.split(",");
        this.usuarioActual = usuarioActual;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .allowedHeaders("Content-Type", "Idempotency-Key", "X-Api-Key")
                // Necesario para que la cookie de sesion viaje en las peticiones.
                // Solo funciona con origenes explicitos, nunca con comodin: es
                // justamente lo que impide que cualquier web actue en nombre del
                // usuario con su sesion abierta.
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(usuarioActual);
    }
}
