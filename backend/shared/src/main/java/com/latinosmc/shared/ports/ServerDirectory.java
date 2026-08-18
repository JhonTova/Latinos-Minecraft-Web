package com.latinosmc.shared.ports;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que el resto de la plataforma necesita saber de un servidor.
 *
 * <p>Es un puerto publico, no el modelo interno del modulo {@code servers}. El
 * modulo de votos depende de esta interfaz, no del modulo de servidores: asi se
 * cumple la regla de ADR-0001 (los modulos de dominio no se conocen entre si) y
 * el compilador lo verifica.
 *
 * <p>Se expone deliberadamente poco. Cada campo que se anade aqui es un acoplamiento
 * nuevo entre modulos que habra que mantener para siempre.
 */
public interface ServerDirectory {

    /** Devuelve el servidor si existe, esta activo y admite votos. */
    Optional<ServerInfo> buscarActivoPorSlug(String slug);

    /**
     * @param id identificador del servidor
     * @param slug identificador legible usado en URLs
     * @param nombre nombre publico
     * @param aceptaIdentidadesNoVerificables si admite votantes de servidores offline-mode
     */
    record ServerInfo(UUID id, String slug, String nombre, boolean aceptaIdentidadesNoVerificables) {}
}
