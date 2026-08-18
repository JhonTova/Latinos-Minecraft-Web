package com.latinosmc.app.web;

import com.latinosmc.ranking.RankingQueries;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cifras de toda la plataforma, para la cabecera.
 *
 * <p>Salen del <strong>ping directo</strong> (ADR-0009), nunca de lo que
 * reporten los plugins. Es la diferencia entre un contador y un anuncio: si cada
 * servidor pudiera declarar sus jugadores, el numero total seria la suma de lo
 * que a cada uno le conviene decir.
 *
 * <p>Solo cuentan los pings recientes. Un servidor que lleva tres dias caido
 * tiene su ultimo ping guardado con 300 jugadores dentro, y sin el limite de
 * frescura ese 300 seguiria sumando indefinidamente.
 */
@RestController
@RequestMapping("/api/v1/estadisticas")
public class EstadisticasController {

    private final RankingQueries consultas;

    public EstadisticasController(RankingQueries consultas) {
        this.consultas = consultas;
    }

    /**
     * Cache de un minuto: aparece en todas las paginas y el dato se mide cada
     * pocos minutos de todos modos, asi que refrescarlo mas a menudo no lo haria
     * mas cierto.
     */
    @GetMapping
    public ResponseEntity<RankingQueries.Cifras> estadisticas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(consultas.cifras());
    }
}
