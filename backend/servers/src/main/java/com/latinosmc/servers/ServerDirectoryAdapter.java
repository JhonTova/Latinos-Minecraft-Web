package com.latinosmc.servers;

import com.latinosmc.shared.ports.ServerDirectory;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementa el puerto publico {@link ServerDirectory}.
 *
 * <p>Es la unica puerta por la que otros modulos acceden a informacion de
 * servidores. Mantenerla fina es intencionado: cada campo que se anade al puerto
 * es acoplamiento permanente entre modulos.
 */
@Component
public class ServerDirectoryAdapter implements ServerDirectory {

    private final ServerRepository repositorio;

    public ServerDirectoryAdapter(ServerRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<ServerInfo> buscarActivoPorSlug(String slug) {
        return repositorio.buscarActivoPorSlug(slug);
    }
}
