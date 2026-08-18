package com.latinosmc.app.web;

import com.latinosmc.ranking.RankingQueries;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/ranking")
public class RankingController {

    private static final int DIAS_POR_DEFECTO = 7;
    private static final int LIMITE_MAXIMO = 100;

    private final RankingQueries consultas;

    /**
     * Deja pasar solo codigos que existen en el catalogo.
     *
     * <p>No es por seguridad —el parametro va como valor, nunca concatenado— sino
     * para que una modalidad mal escrita devuelva el ranking completo en lugar de
     * una lista vacia que parece un fallo del sitio.
     */
    private String modalidadValida(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        String normalizado = codigo.trim().toUpperCase(java.util.Locale.ROOT);
        boolean existe = servidores.modalidades().stream()
                .anyMatch(m -> m.codigo().equals(normalizado));

        return existe ? normalizado : null;
    }

    /** Compone la URL publica del icono: cambia segun donde este el almacen. */
    private final com.latinosmc.shared.ports.FileStorage almacen;

    private final com.latinosmc.servers.ServerRepository servidores;

    public RankingController(
            RankingQueries consultas,
            com.latinosmc.shared.ports.FileStorage almacen,
            com.latinosmc.servers.ServerRepository servidores) {
        this.consultas = consultas;
        this.almacen = almacen;
        this.servidores = servidores;
    }

    /**
     * Ranking organico. No incluye slots patrocinados: se piden aparte (ADR-0013).
     *
     * <p>Se cachea 60 segundos. El ranking no necesita ser instantaneo y es la
     * consulta mas pedida del sitio: sin cache, cada visita agrega todos los votos
     * de la semana.
     */
    @GetMapping
    public ResponseEntity<List<ServidorEnRanking>> ranking(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(defaultValue = "50") int limite,
            @RequestParam(required = false) String modalidad) {

        int diasSeguros = Math.clamp(dias, 1, 30);
        int limiteSeguro = Math.clamp(limite, 1, LIMITE_MAXIMO);

        var crudas = consultas.ranking(diasSeguros, limiteSeguro, modalidadValida(modalidad));

        // Las modalidades de todos de una vez. Pedirlas por servidor serian
        // cincuenta consultas para pintar cincuenta tarjetas: el problema N+1,
        // que no se nota con tres servidores y tumba la portada con quinientos.
        var modalidades = servidores.modalidadesDeVarios(
                crudas.stream().map(RankingQueries.FilaRanking::id).toList());

        List<ServidorEnRanking> filas = crudas.stream()
                .map(f -> new ServidorEnRanking(
                        f.id(),
                        f.slug(),
                        f.nombre(),
                        f.descripcion(),
                        f.claveIcono() == null ? null : almacen.urlPublica(f.claveIcono()),
                        f.claveBanner() == null ? null : almacen.urlPublica(f.claveBanner()),
                        direccionDe(f.host(), f.puerto()),
                        f.votos(),
                        f.jugadoresConectados(),
                        Boolean.TRUE.equals(f.enLinea()),
                        consultas.tendenciaDiaria(f.id(), DIAS_POR_DEFECTO)))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(filas);
    }

    /**
     * Direccion tal como la escribe el jugador para conectarse.
     *
     * <p>El puerto se omite cuando es el de Minecraft por defecto: nadie escribe
     * ":25565", y ensenarlo hace parecer complicado lo que no lo es.
     */
    private static String direccionDe(String host, Integer puerto) {
        if (host == null) {
            return null;
        }
        return puerto == null || puerto == 25565 ? host : host + ":" + puerto;
    }

    /**
     * @param jugadoresConectados medido con ping directo desde nuestra
     *     infraestructura, nunca reportado por el plugin (ADR-0009)
     */
    public record ServidorEnRanking(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String iconoUrl,
            String bannerUrl,
            String direccion,
            long votos,
            Integer jugadoresConectados,
            boolean enLinea,
            List<Long> tendencia) {}
}
