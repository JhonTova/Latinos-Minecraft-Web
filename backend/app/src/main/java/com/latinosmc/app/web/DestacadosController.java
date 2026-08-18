package com.latinosmc.app.web;

import com.latinosmc.billing.BillingRepository;
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
 * Bloque de destacados de la portada.
 *
 * <p>Es lo que compra el cliente y, por tanto, lo unico de esta plataforma por lo
 * que alguien paga directamente. Si esto no se pinta, el cobro fue una estafa
 * involuntaria.
 *
 * <p><strong>Endpoint aparte del ranking, a proposito.</strong> Podrian ir
 * juntos, y seria una respuesta menos. No van: lo patrocinado no se mezcla nunca
 * con lo organico (ADR-0013). Mantenerlos separados en la API hace que mezclarlos
 * en la interfaz requiera un esfuerzo deliberado en lugar de ser lo que sale por
 * defecto un dia de prisa.
 */
@RestController
@RequestMapping("/api/v1/destacados")
public class DestacadosController {

    private final BillingRepository facturacion;
    private final FileStorage almacen;

    private final com.latinosmc.servers.ServerRepository servidores;

    public DestacadosController(
            BillingRepository facturacion,
            FileStorage almacen,
            com.latinosmc.servers.ServerRepository servidores) {
        this.facturacion = facturacion;
        this.almacen = almacen;
        this.servidores = servidores;
    }

    /**
     * Los cinco espacios, ocupados o no.
     *
     * <p>Se devuelven siempre los cinco —con {@code servidor} a null los libres—
     * para que la portada no cambie de forma segun quien haya comprado. Un bloque
     * que salta de tres a cinco huecos mueve todo lo de abajo y se nota.
     *
     * <p>Cache de 60 segundos: una compra tarda como mucho un minuto en verse, y a
     * cambio la consulta mas visitada del sitio no toca la base de datos en cada
     * visita.
     */
    @GetMapping
    public ResponseEntity<List<SlotEnPortada>> destacados() {
        var ocupados = facturacion.destacadosVigentes(Instant.now());

        List<SlotEnPortada> slots = java.util.stream.IntStream.rangeClosed(1, 5)
                .mapToObj(posicion -> ocupados.stream()
                        .filter(o -> o.posicion() == posicion)
                        .findFirst()
                        .map(o -> new SlotEnPortada(
                                posicion,
                                new ServidorDestacado(
                                        o.servidorId(),
                                        o.slug(),
                                        o.nombre(),
                                        o.descripcion(),
                                        o.claveIcono() == null ? null : almacen.urlPublica(o.claveIcono()),
                                        o.claveBanner() == null ? null : almacen.urlPublica(o.claveBanner()),
                                        o.puerto() == null || o.puerto() == 25565
                                                ? o.host()
                                                : o.host() + ":" + o.puerto(),
                                        o.versionMinima(),
                                        o.versionMaxima(),
                                        o.region(),
                                        servidores.modalidadesDe(o.servidorId()).stream()
                                                .map(com.latinosmc.servers.ServerRepository.Modalidad::nombre)
                                                .toList())))
                        .orElse(new SlotEnPortada(posicion, null)))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(slots);
    }

    /** @param servidor null si el espacio esta libre */
    public record SlotEnPortada(int posicion, ServidorDestacado servidor) {}

    /**
     * Deliberadamente <strong>sin votos ni jugadores conectados</strong>. Un
     * destacado no compite con el ranking: se paga por visibilidad, no por
     * aparentar posicion. Enseñar aqui las mismas cifras que en el ranking
     * invitaria a confundir una cosa con la otra, que es justo lo que ADR-0013
     * evita.
     */
    public record ServidorDestacado(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String iconoUrl,
            String bannerUrl,
            String direccion,
            String versionMinima,
            String versionMaxima,
            String region,
            java.util.List<String> modalidades) {}
}
