package com.latinosmc.app.web;

import com.latinosmc.ranking.RankingQueries;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.voting.VoteSettingsRepository;
import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.ports.FileStorage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Directorio publico de servidores.
 *
 * <p>Sin autenticacion: es la parte que ve cualquier jugador. Devuelve
 * unicamente informacion publica — nunca el host real, el puerto, la clave del
 * plugin ni la organizacion propietaria.
 */
@RestController
@RequestMapping("/api/v1/directorio")
public class DirectorioController {

    private final ServerRepository servidores;
    private final RankingQueries ranking;
    private final FileStorage almacen;
    private final VoteSettingsRepository reglasDeVoto;

    public DirectorioController(
            ServerRepository servidores,
            RankingQueries ranking,
            FileStorage almacen,
            VoteSettingsRepository reglasDeVoto) {
        this.reglasDeVoto = reglasDeVoto;
        this.servidores = servidores;
        this.ranking = ranking;
        this.almacen = almacen;
    }

    /**
     * Ficha publica de un servidor.
     *
     * <p>Se cachea un minuto: es la pagina mas visitada despues del ranking y su
     * contenido apenas cambia.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<FichaPublica> ficha(@PathVariable String slug) {
        var servidor = servidores
                .buscarPublicoPorSlug(slug)
                .orElseThrow(() -> new DomainException.NotFound("server.not_found", "Ese servidor no existe."));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(new FichaPublica(
                        servidor.id(),
                        servidor.slug(),
                        servidor.nombre(),
                        servidor.descripcion(),
                        servidor.direccionPublica(),
                        servidor.aceptaOffline(),
                        // La misma para todos los servidores: la fija la
                        // plataforma, no su dueño (ADR-0023).
                        (int) reglasDeVoto.actuales().ventana().toHours(),
                        servidor.jugadoresConectados(),
                        servidor.jugadoresMaximo(),
                        servidor.enLinea(),
                        servidor.votosSemana(),
                        urlDe(servidor.claveIcono()),
                        urlDe(servidor.claveBanner()),
                        servidor.versionMinima(),
                        servidor.versionMaxima(),
                        servidor.region(),
                        servidor.tema(),
                        servidores.modalidadesDe(servidor.id()).stream()
                                .map(com.latinosmc.servers.ServerRepository.Modalidad::nombre)
                                .toList(),
                        ranking.tendenciaDiaria(servidor.id(), 7)));
    }

    private String urlDe(String clave) {
        return clave == null ? null : almacen.urlPublica(clave);
    }

    /**
     * @param direccion la que los jugadores escriben para conectarse. Es publica
     *     por definicion: sin ella el directorio no sirve de nada.
     * @param jugadoresConectados medido por nosotros con ping directo (ADR-0009)
     */
    public record FichaPublica(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String direccion,
            boolean aceptaOffline,
            int ventanaHoras,
            Integer jugadoresConectados,
            Integer jugadoresMaximo,
            boolean enLinea,
            long votosSemana,
            String iconoUrl,
            String bannerUrl,
            String versionMinima,
            String versionMaxima,
            String region,
            String tema,
            List<String> modalidades,
            List<Long> tendencia) {}
}
