package com.latinosmc.app.mantenimiento;

import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.FileStorage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra del almacen los archivos que ya no referencia nadie.
 *
 * <p>Es la limpieza que el ADR-0017 daba por hecha y que <strong>nunca se
 * construyo</strong>. {@code marcarSubidaHuerfana} marcaba la fila desde el
 * primer dia y nadie la leia jamas: cada icono o banner reemplazado seguia
 * ocupando sitio, y pagandose, indefinidamente.
 *
 * <p>El ADR-0027 convirtio eso de conveniente en necesario. Antes los archivos
 * huerfanos aparecian cuando una persona subia una imagen nueva —unas pocas al
 * dia en toda la plataforma—; ahora los genera la propia plataforma cada vez que
 * un servidor cambia su MOTD, y eso escala con el numero de servidores.
 */
@Component
public class LimpiezaDeArchivosHuerfanos {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDeArchivosHuerfanos.class);

    /**
     * Cuantos se borran por ronda.
     *
     * <p>Cada borrado es una llamada de red al almacen. Con un lote sin techo,
     * una acumulacion de meses convertiria la primera ronda en miles de llamadas
     * seguidas contra R2. Lo que sobre se recoge mañana.
     */
    private static final int LOTE = 500;

    private final ServerRepository servidores;
    private final FileStorage almacen;
    private final int diasDeGracia;

    public LimpiezaDeArchivosHuerfanos(
            ServerRepository servidores,
            FileStorage almacen,
            @Value("${latinosmc.limpieza.dias-huerfanos:7}") int diasDeGracia) {
        this.servidores = servidores;
        this.almacen = almacen;
        this.diasDeGracia = diasDeGracia;
    }

    @Scheduled(cron = "${latinosmc.limpieza.cron-huerfanos:0 0 3 * * *}")
    public void ronda() {
        List<String> claves = servidores.huerfanosParaBorrar(diasDeGracia, LOTE);
        if (claves.isEmpty()) {
            return;
        }

        int borrados = 0;
        int fallos = 0;

        for (String clave : claves) {
            try {
                almacen.borrar(clave);
                // Se marca DESPUES de borrar. Al reves, un fallo del almacen
                // dejaria la fila diciendo que el archivo ya no esta cuando si
                // esta, y entonces nadie volveria a intentarlo nunca: seria un
                // archivo pagado para siempre e invisible.
                servidores.marcarHuerfanoBorrado(clave);
                borrados++;
            } catch (RuntimeException e) {
                // Se deja sin marcar a proposito, para reintentarlo mañana. Un
                // corte del almacen no puede perder la referencia.
                fallos++;
                log.warn("No se pudo borrar el archivo huerfano {}: {}", clave, e.getMessage());
            }
        }

        log.info("Limpieza de archivos: {} borrados, {} fallidos (se reintentan).", borrados, fallos);
    }
}
