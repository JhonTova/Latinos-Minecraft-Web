package com.latinosmc.app.mantenimiento;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.EmailSender;
import com.latinosmc.shared.ports.FileStorage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Quita sus imagenes y sus claves a los servidores que llevan meses fuera
 * (ADR-0028).
 *
 * <p>El ADR-0024 los saca del directorio a los 15 dias, y ahi se quedaban para
 * siempre: sus imagenes ocupando espacio que se paga y sus claves de API
 * vivas. Esto cierra ese ciclo.
 *
 * <p><strong>No borra la fila del servidor.</strong> No es una preferencia:
 * {@code votes} y {@code vote_rewards} apuntan a {@code servers} con
 * {@code ON DELETE RESTRICT}, asi que PostgreSQL rechazaria el borrado; y
 * {@code slot_bookings} apunta con {@code ON DELETE CASCADE}, asi que un borrado
 * se llevaria por delante y en silencio el registro de espacios destacados ya
 * pagados. Ademas, esos votos son las señales con las que el antifraude detecta
 * patrones en OTROS servidores: vaciarlos deja agujeros donde se pilla a quien
 * hace trampas.
 *
 * <p>Lo que se elimina es lo que cuesta dinero o estorba. Lo que se conserva es
 * lo que el resto de la plataforma necesita.
 */
@Component
public class PurgaDeServidoresArchivados {

    private static final Logger log = LoggerFactory.getLogger(PurgaDeServidoresArchivados.class);

    /** Acotado por ronda, por el mismo motivo que la limpieza: no vaciar medio catalogo de golpe. */
    private static final int LOTE = 100;

    private final ServerRepository servidores;
    private final OrganizationRepository organizaciones;
    private final IdentityRepository identidades;
    private final BillingRepository facturacion;
    private final FileStorage almacen;
    private final EmailSender correo;
    private final Clock reloj;
    private final String sitioUrl;

    private final int diasParaPurgar;
    private final int diasDeAviso;

    public PurgaDeServidoresArchivados(
            ServerRepository servidores,
            OrganizationRepository organizaciones,
            IdentityRepository identidades,
            BillingRepository facturacion,
            FileStorage almacen,
            EmailSender correo,
            Clock reloj,
            @Value("${latinosmc.sitio-url}") String sitioUrl,
            @Value("${latinosmc.purga.dias-archivado:30}") int diasParaPurgar,
            @Value("${latinosmc.purga.dias-aviso:7}") int diasDeAviso) {

        this.servidores = servidores;
        this.organizaciones = organizaciones;
        this.identidades = identidades;
        this.facturacion = facturacion;
        this.almacen = almacen;
        this.correo = correo;
        this.reloj = reloj;
        this.sitioUrl = sitioUrl.endsWith("/") ? sitioUrl.substring(0, sitioUrl.length() - 1) : sitioUrl;
        this.diasParaPurgar = diasParaPurgar;
        this.diasDeAviso = diasDeAviso;
    }

    /**
     * Una ronda al dia, despues de la de inactividad.
     *
     * <p>Media hora mas tarde a proposito: asi el servidor que se acaba de
     * archivar esta noche ya tiene su {@code archived_at} puesto, y el contador
     * de esta empieza donde debe.
     */
    @Scheduled(cron = "${latinosmc.purga.cron:0 0 5 * * *}")
    public void ronda() {
        // Se piden los que ya pasaron el umbral del AVISO, no el de la purga: en
        // esta misma consulta caben las dos fases, y asi el aviso llega antes de
        // que nadie pierda nada.
        int desdeCuando = Math.max(0, diasParaPurgar - diasDeAviso);
        List<ServerRepository.ServidorParaPurgar> candidatos =
                servidores.archivadosDesdeHace(desdeCuando, LOTE);

        if (candidatos.isEmpty()) {
            return;
        }

        Instant ahora = Instant.now(reloj);
        int avisados = 0;
        int purgados = 0;

        for (var servidor : candidatos) {
            long dias = Duration.between(servidor.archivadoEn(), ahora).toDays();

            if (dias >= diasParaPurgar) {
                if (purgar(servidor, dias)) {
                    purgados++;
                }
            } else if (servidor.avisadoEn() == null) {
                avisar(servidor, dias);
                avisados++;
            }
        }

        if (avisados > 0 || purgados > 0) {
            log.info("Purga: {} servidores avisados, {} purgados.", avisados, purgados);
        }
    }

    private boolean purgar(ServerRepository.ServidorParaPurgar servidor, long dias) {
        // Mismo freno que el archivado: un espacio destacado pagado es dinero
        // cobrado, y no se toca por muy archivado que este el servidor.
        var destacado = facturacion.destacadoVigenteDe(servidor.id(), Instant.now(reloj));
        if (destacado.isPresent()) {
            log.warn(
                    "El servidor {} lleva {} dias archivado pero tiene un espacio destacado pagado hasta {}. "
                            + "No se purga: revisar a mano.",
                    servidor.slug(),
                    dias,
                    destacado.get());
            return false;
        }

        // La fila se actualiza ANTES de borrar los archivos. Si el orden fuera el
        // contrario y algo fallara entre medias, quedaria un servidor apuntando a
        // imagenes que ya no existen: fichas rotas a la vista de todos. Al reves,
        // lo peor que puede pasar es que sobren unos archivos que ya no referencia
        // nadie, y de esos se encarga LimpiezaDeArchivosHuerfanos.
        if (servidores.purgar(servidor.id()) == 0) {
            return false;
        }

        servidores.revocarClaves(servidor.id());
        servidores.marcarArchivosDeServidorHuerfanos(servidor.claves());

        int borrados = 0;
        for (String clave : servidor.claves()) {
            try {
                almacen.borrar(clave);
                servidores.marcarHuerfanoBorrado(clave);
                borrados++;
            } catch (RuntimeException e) {
                // El archivo queda marcado como huerfano igualmente, asi que la
                // ronda de limpieza volvera a intentarlo. No merece cortar la purga.
                log.warn("No se pudo borrar {} del almacen: {}", clave, e.getMessage());
            }
        }

        log.info(
                "Servidor {} purgado tras {} dias archivado: {} archivos borrados, claves revocadas.",
                servidor.slug(),
                dias,
                borrados);

        escribirAlDueno(
                servidor,
                "Hemos liberado los datos de " + servidor.nombre(),
                """
                Hola,

                Tu servidor "%s" llevaba %d dias retirado del directorio sin volver
                a conectar, asi que hemos liberado lo que ocupaba: sus imagenes y
                sus claves de acceso.

                Sus votos y su historial NO se han borrado. Siguen ahi.

                Si quieres volver, escribenos y lo recuperamos: habra que dar de
                alta el servidor otra vez y volver a subir el icono y el banner,
                pero no pierdes lo que ya conseguiste.

                %s

                — LatinosMC
                """
                        .formatted(servidor.nombre(), dias, sitioUrl));

        return true;
    }

    private void avisar(ServerRepository.ServidorParaPurgar servidor, long dias) {
        servidores.marcarAvisoDePurga(servidor.id());

        escribirAlDueno(
                servidor,
                "Ultimo aviso sobre " + servidor.nombre(),
                """
                Hola,

                Tu servidor "%s" lleva %d dias retirado del directorio porque el
                plugin dejo de conectar.

                Dentro de %d dias liberaremos lo que ocupa: sus imagenes y sus
                claves de acceso. Sus votos y su historial se conservan, pero
                tendras que volver a configurarlo si quieres recuperarlo.

                Para evitarlo solo hace falta que el plugin vuelva a conectar. En
                cuanto lo haga, el servidor reaparece donde estaba.

                Puedes revisarlo en %s/panel

                — LatinosMC
                """
                        .formatted(servidor.nombre(), dias, diasParaPurgar - dias, sitioUrl));
    }

    /**
     * Escribe a quien pueda hacer algo.
     *
     * <p>Un fallo de correo no interrumpe la ronda: la accion ya esta hecha y
     * reintentarla mañana la repetiria. Se registra y se sigue.
     */
    private void escribirAlDueno(ServerRepository.ServidorParaPurgar servidor, String asunto, String cuerpo) {
        try {
            for (var usuarioId : organizaciones.administradoresDe(servidor.organizacionId())) {
                identidades
                        .buscarPorId(usuarioId)
                        .ifPresent(usuario -> correo.enviar(usuario.email(), asunto, cuerpo, null));
            }
        } catch (RuntimeException e) {
            log.error("No se pudo avisar al dueño del servidor {}: {}", servidor.slug(), e.getMessage());
        }
    }
}
