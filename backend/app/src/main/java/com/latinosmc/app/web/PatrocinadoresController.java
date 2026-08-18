package com.latinosmc.app.web;

import com.latinosmc.billing.PatrocinadoresRepository;
import com.latinosmc.shared.ports.FileStorage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Patrocinadores de marca visibles en la web.
 *
 * <p>Publico y sin sesion. Solo salen los aprobados, sin cancelar y dentro de su
 * periodo: las tres condiciones se comprueban en la consulta, no aqui, para que
 * no exista ninguna forma de pedir "todos" desde fuera.
 *
 * <p>Van en su propio bloque, separados del ranking y tambien de los espacios
 * destacados. Son tres cosas distintas y mezclarlas confundiria al visitante
 * sobre que es publicidad y que no.
 */
@RestController
@RequestMapping("/api/v1/patrocinadores")
public class PatrocinadoresController {

    private final PatrocinadoresRepository patrocinadores;
    private final FileStorage almacen;

    public PatrocinadoresController(PatrocinadoresRepository patrocinadores, FileStorage almacen) {
        this.patrocinadores = patrocinadores;
        this.almacen = almacen;
    }

    /**
     * Cache de cinco minutos: cambian como mucho una vez a la semana y aparecen en
     * todas las paginas. Es la consulta con mejor relacion entre lo que se ahorra
     * y lo poco que importa que tarde unos minutos en reflejar un cambio.
     */
    @GetMapping
    public ResponseEntity<List<Patrocinador>> patrocinadores() {
        List<Patrocinador> visibles = patrocinadores.vigentes(Instant.now()).stream()
                .map(p -> new Patrocinador(
                        p.id(),
                        p.nombre(),
                        p.urlDestino(),
                        p.claveLogo() == null ? null : almacen.urlPublica(p.claveLogo())))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(visibles);
    }

    /**
     * Sin fechas ni estado: al visitante no le importan y publicarlas diria a
     * quien mire la API cuando caduca cada contrato.
     */
    public record Patrocinador(UUID id, String nombre, String urlDestino, String logoUrl) {}
}
