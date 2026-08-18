package com.latinosmc.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada y unico artefacto desplegable (ADR-0001: monolito modular).
 *
 * <p>Este modulo ensambla los modulos de dominio. Es el unico que los conoce a
 * todos; ellos no se conocen entre si.
 */
@SpringBootApplication(scanBasePackages = "com.latinosmc")
public class LatinosMcApplication {

    public static void main(String[] args) {
        SpringApplication.run(LatinosMcApplication.class, args);
    }
}
