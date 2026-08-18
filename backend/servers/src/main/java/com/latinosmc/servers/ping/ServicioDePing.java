package com.latinosmc.servers.ping;

import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.Ids;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sondea periodicamente todos los servidores activos.
 *
 * <p>Es la fuente autoritativa de "jugadores conectados" y de si un servidor esta
 * caido (ADR-0009). El plugin no interviene: lo que reporta es informativo y
 * jamas alimenta el ranking.
 *
 * <p>Los sondeos van en paralelo sobre hilos virtuales. Cada uno es E/S pura que
 * pasa casi todo su tiempo esperando, y con hilos virtuales mil servidores no
 * cuestan mil hilos del sistema: es exactamente el caso para el que existen
 * (ADR-0008).
 */
@Component
public class ServicioDePing {

    private static final Logger log = LoggerFactory.getLogger(ServicioDePing.class);

    /**
     * Cuantos servidores se sondean por ronda.
     *
     * <p>Acotado para que una ronda no se alargue indefinidamente segun crece el
     * directorio. Con rondas cada minuto y este lote, 300 servidores se cubren en
     * unos 3 minutos — suficiente para un dato que solo tiene que estar "al dia",
     * no al segundo.
     */
    private static final int LOTE = 100;

    private final ServerRepository servidores;
    private final PingMinecraft ping;
    private final DireccionesPermitidas direcciones;

    public ServicioDePing(
            ServerRepository servidores, PingMinecraft ping, DireccionesPermitidas direcciones) {
        this.servidores = servidores;
        this.ping = ping;
        this.direcciones = direcciones;
    }

    @Scheduled(fixedDelayString = "${latinosmc.ping.intervalo-ms:60000}")
    public void rondaDeSondeo() {
        List<ServerRepository.ServidorParaSondear> lote = servidores.siguientesParaSondear(LOTE);
        if (lote.isEmpty()) {
            return;
        }

        long inicio = System.currentTimeMillis();

        // Un hilo virtual por sondeo. Cada uno es E/S pura que pasa casi todo su
        // tiempo esperando una respuesta de red: con hilos del sistema, cien
        // sondeos serian cien hilos bloqueados; con virtuales, casi nada.
        //
        // El try-with-resources espera a que terminen todos antes de cerrar, asi
        // que ninguna tarea queda huerfana cuando la ronda acaba.
        try (ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var servidor : lote) {
                ejecutor.submit(() -> sondearUno(servidor));
            }
            ejecutor.shutdown();
            // Techo por si algun servidor se queda colgado pese al timeout del
            // socket: una ronda nunca puede bloquear a la siguiente para siempre.
            if (!ejecutor.awaitTermination(2, TimeUnit.MINUTES)) {
                log.warn("La ronda de sondeo no termino a tiempo; se cortan las pendientes.");
                ejecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        log.debug("Ronda de sondeo: {} servidores en {} ms", lote.size(), System.currentTimeMillis() - inicio);
    }

    private void sondearUno(ServerRepository.ServidorParaSondear servidor) {
        // La comprobacion de direccion va ANTES de abrir ningun socket: es lo que
        // impide que alguien use nuestro sondeador para explorar redes internas
        // registrando un servidor con host 127.0.0.1 o una IP privada.
        var direccion = direcciones.resolverSiEsSegura(servidor.host());

        PingMinecraft.Resultado resultado = direccion == null
                ? new PingMinecraft.Resultado(false, null, null, null, null, "direccion no sondeable")
                : ping.sondear(direccion, servidor.puerto(), servidor.host());

        try {
            servidores.registrarPing(
                    Ids.newId(),
                    servidor.id(),
                    resultado.enLinea(),
                    resultado.jugadoresConectados(),
                    resultado.jugadoresMaximos(),
                    resultado.latenciaMs(),
                    resultado.version(),
                    resultado.error());
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el sondeo de {}: {}", servidor.host(), e.getMessage());
        }
    }
}
