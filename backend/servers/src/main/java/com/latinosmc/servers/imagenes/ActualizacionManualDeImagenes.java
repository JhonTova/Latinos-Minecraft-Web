package com.latinosmc.servers.imagenes;

import com.latinosmc.servers.ServerRepository;
import com.latinosmc.servers.ping.DireccionesPermitidas;
import com.latinosmc.servers.ping.PingMinecraft;
import com.latinosmc.shared.DomainException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * El boton "actualizar ahora" del panel del dueño.
 *
 * <p>Sondea el servidor en el momento y readopta lo que anuncie, sin esperar a la
 * ronda automatica. Existe porque quien acaba de cambiar el icono de su servidor
 * quiere verlo ya, y decirle que vuelva en seis horas convierte una funcion util
 * en un ticket de soporte.
 *
 * <p>Clase aparte de {@link ImagenesAutomaticasService} por un motivo concreto: el
 * sondeo es una conexion de red que puede tardar sus segundos, y tiene que ocurrir
 * <strong>fuera</strong> de la transaccion. Si estuviera dentro, cada pulsacion
 * mantendria una conexion de base de datos ocupada esperando a un socket que
 * quiza no responda nunca, y con unas cuantas a la vez se agota el grupo de
 * conexiones y cae toda la aplicacion, no solo esta funcion.
 */
@Service
public class ActualizacionManualDeImagenes {

    private final ServerRepository servidores;
    private final PingMinecraft ping;
    private final DireccionesPermitidas direcciones;
    private final ImagenesAutomaticasService imagenes;

    public ActualizacionManualDeImagenes(
            ServerRepository servidores,
            PingMinecraft ping,
            DireccionesPermitidas direcciones,
            ImagenesAutomaticasService imagenes) {
        this.servidores = servidores;
        this.ping = ping;
        this.direcciones = direcciones;
        this.imagenes = imagenes;
    }

    public ImagenesAutomaticasService.Resultado ejecutar(UUID servidorId, UUID organizacionId) {
        var detalle = servidores
                .detalle(servidorId)
                .filter(d -> d.organizacionId().equals(organizacionId))
                .orElseThrow(() -> new DomainException.NotFound(
                        "server.not_found", "Ese servidor no existe o no puedes gestionarlo."));

        // La comprobacion de direccion va antes de abrir ningun socket, igual que
        // en la ronda automatica: es lo que impide que alguien use este boton para
        // explorar redes internas registrando un servidor con una IP privada.
        var direccion = direcciones.resolverSiEsSegura(detalle.host());
        if (direccion == null) {
            return ImagenesAutomaticasService.Resultado.SERVIDOR_CAIDO;
        }

        PingMinecraft.Resultado sondeo = ping.sondear(direccion, detalle.puerto(), detalle.host());
        return imagenes.actualizar(servidorId, sondeo, true);
    }
}
