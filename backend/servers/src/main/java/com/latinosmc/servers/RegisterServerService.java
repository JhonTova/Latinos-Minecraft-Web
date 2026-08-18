package com.latinosmc.servers;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de un servidor en el directorio. */
@Service
public class RegisterServerService {

    private static final Pattern SLUG_VALIDO = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final ServerRepository repositorio;

    public RegisterServerService(ServerRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Registra el servidor y devuelve la clave del plugin <strong>en claro</strong>.
     * Es la unica vez que existe fuera de la memoria del proceso.
     *
     * <p>El servidor nace en {@code PENDING_VERIFICATION}. Se activa solo cuando el
     * plugin se conecta por primera vez con esta clave: esa conexion es la prueba
     * de propiedad, porque solo quien administra el servidor puede instalarla alli.
     * Verificacion e instalacion son el mismo paso, no dos.
     */
    @Transactional
    public Resultado registrar(Peticion peticion) {
        String slug = normalizarSlug(peticion.slug());

        if (!SLUG_VALIDO.matcher(slug).matches()) {
            throw new DomainException.Conflict(
                    "server.slug.invalid", "El identificador solo admite minusculas, numeros y guiones.");
        }
        if (repositorio.existeSlug(slug)) {
            throw new DomainException.Conflict("server.slug.taken", "Ese identificador ya esta en uso.");
        }
        if (peticion.puerto() < 1 || peticion.puerto() > 65535) {
            throw new DomainException.Conflict("server.port.invalid", "El puerto debe estar entre 1 y 65535.");
        }

        // La ventana entre votos NO se pide aqui: es una sola para toda la
        // plataforma y la fija su administrador (ADR-0023). Cuando cada dueño
        // elegia la suya, acotarla entre 12 y 48 horas no bastaba — bajarla al
        // minimo seguia doblando sus votos frente a quien la dejara en 24.

        UUID servidorId = Ids.newId();
        repositorio.insertar(new ServerRepository.ServidorNuevo(
                servidorId,
                peticion.organizacionId(),
                "MINECRAFT_JAVA",
                "PENDING_VERIFICATION",
                slug,
                peticion.nombre(),
                peticion.descripcion(),
                peticion.host(),
                peticion.puerto(),
                peticion.aceptaOffline()));

        String claveEnClaro = ApiKeys.generar();
        repositorio.guardarClaveApi(
                Ids.newId(), servidorId, ApiKeys.hash(claveEnClaro), ApiKeys.prefijoVisible(claveEnClaro));

        return new Resultado(servidorId, slug, claveEnClaro);
    }

    private String normalizarSlug(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    public record Peticion(
            UUID organizacionId,
            String slug,
            String nombre,
            String descripcion,
            String host,
            int puerto,
            boolean aceptaOffline) {}

    /**
     * @param claveApi clave en claro. No se vuelve a mostrar nunca.
     */
    public record Resultado(UUID servidorId, String slug, String claveApi) {}
}
