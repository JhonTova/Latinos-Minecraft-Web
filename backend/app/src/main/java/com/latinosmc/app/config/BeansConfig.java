package com.latinosmc.app.config;

import com.latinosmc.shared.identity.IdentityHasher;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BeansConfig {

    private static final String PEPPER_DESARROLLO = "pepper-solo-para-desarrollo-no-usar-en-produccion";

    /**
     * Reloj inyectable en vez de {@code Instant.now()} disperso por el codigo.
     *
     * <p>Sin esto, probar la ventana de voto exigiria esperar 24 horas reales. Con
     * un reloj inyectado, el test adelanta el tiempo y comprueba la regla en
     * milisegundos.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * @throws IllegalStateException si en produccion se dejo el pepper de desarrollo.
     *     Fallar al arrancar es incomodo; descubrir meses despues que todos los
     *     hashes de identidad se calcularon con una clave publica lo es mucho mas.
     */
    @Bean
    public IdentityHasher identityHasher(
            @Value("${latinosmc.identity-pepper}") String pepper, Environment entorno) {

        // La comprobacion se dispara en CUALQUIER despliegue, no solo en 'prod'.
        // Un entorno de pruebas en un servidor real acumula datos reales: si
        // arranca con la clave de desarrollo y luego se cambia, se pierden de
        // golpe la ventana de voto y el historial antifraude de todos.
        boolean esDesarrolloLocal = entorno.matchesProfiles("dev") || entorno.getActiveProfiles().length == 0;
        if (!esDesarrolloLocal && PEPPER_DESARROLLO.equals(pepper)) {
            throw new IllegalStateException(
                    "LATINOSMC_IDENTITY_PEPPER no esta configurado. En produccion es obligatorio "
                            + "un valor secreto y aleatorio. Generalo una vez y guardalo: si cambia, "
                            + "se pierde la ventana de voto y el historial antifraude de todos los usuarios.");
        }
        return new IdentityHasher(pepper);
    }
}
