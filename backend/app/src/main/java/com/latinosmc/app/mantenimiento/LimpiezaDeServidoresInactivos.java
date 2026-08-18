package com.latinosmc.app.mantenimiento;

import com.latinosmc.billing.BillingRepository;
import com.latinosmc.identity.IdentityRepository;
import com.latinosmc.organizations.OrganizationRepository;
import com.latinosmc.servers.ServerRepository;
import com.latinosmc.shared.ports.EmailSender;
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
 * Retira del directorio los servidores que dejaron de dar señales (ADR-0024).
 *
 * <p>Un directorio lleno de servidores muertos deja de servir para lo unico que
 * sirve un directorio: que quien entra encuentre donde jugar. Y el ranking deja
 * de significar nada cuando compite contra decenas de fichas que ya no existen.
 *
 * <p>La señal es la del plugin, que late cada minuto. Nunca lo que el dueño
 * declare: eso lo decide quien tiene el incentivo para mentir (ADR-0009).
 *
 * <p><strong>Se archiva, no se borra.</strong> Quince dias sin señal no prueban
 * que el servidor haya muerto, prueban que no hemos sabido de el. En cuanto el
 * plugin vuelve a latir, el servidor reaparece solo.
 */
@Component
public class LimpiezaDeServidoresInactivos {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDeServidoresInactivos.class);

    /**
     * Cuantos se revisan por ronda.
     *
     * <p>Acotado para que una noche mala —un corte de red que deje a medio
     * directorio sin latir— no dispare cientos de correos de golpe ni archive
     * medio catalogo en una sola pasada. Lo que sobre se recoge al dia
     * siguiente.
     */
    private static final int LOTE = 200;

    private final ServerRepository servidores;
    private final OrganizationRepository organizaciones;
    private final IdentityRepository identidades;
    private final BillingRepository facturacion;
    private final EmailSender correo;
    private final Clock reloj;
    private final String sitioUrl;

    private final int diasDeAviso;
    private final int diasParaRetirar;

    public LimpiezaDeServidoresInactivos(
            ServerRepository servidores,
            OrganizationRepository organizaciones,
            IdentityRepository identidades,
            BillingRepository facturacion,
            EmailSender correo,
            Clock reloj,
            @Value("${latinosmc.sitio-url}") String sitioUrl,
            @Value("${latinosmc.limpieza.dias-aviso:7}") int diasDeAviso,
            @Value("${latinosmc.limpieza.dias-retirada:15}") int diasParaRetirar) {

        this.servidores = servidores;
        this.organizaciones = organizaciones;
        this.identidades = identidades;
        this.facturacion = facturacion;
        this.correo = correo;
        this.reloj = reloj;
        this.sitioUrl = sitioUrl.endsWith("/") ? sitioUrl.substring(0, sitioUrl.length() - 1) : sitioUrl;
        this.diasDeAviso = diasDeAviso;
        this.diasParaRetirar = diasParaRetirar;
    }

    /**
     * Una ronda al dia, de madrugada.
     *
     * <p>Diaria y no cada hora a proposito: la unidad de esta regla son dias, y
     * una ronda por hora solo multiplicaria por 24 las oportunidades de mandar
     * un correo de mas.
     */
    @Scheduled(cron = "${latinosmc.limpieza.cron:0 30 4 * * *}")
    public void ronda() {
        List<ServerRepository.ServidorSinSenal> candidatos = servidores.sinSenalDelPlugin(diasDeAviso, LOTE);
        if (candidatos.isEmpty()) {
            return;
        }

        Instant ahora = Instant.now(reloj);
        int avisados = 0;
        int retirados = 0;

        for (var servidor : candidatos) {
            long dias = Duration.between(servidor.ultimaSenal(), ahora).toDays();

            if (dias >= diasParaRetirar) {
                if (retirar(servidor, dias)) {
                    retirados++;
                }
            } else if (servidor.avisadoEn() == null) {
                avisar(servidor, dias);
                avisados++;
            }
        }

        if (avisados > 0 || retirados > 0) {
            log.info("Limpieza: {} servidores avisados, {} retirados del directorio.", avisados, retirados);
        }
    }

    private boolean retirar(ServerRepository.ServidorSinSenal servidor, long dias) {
        // Un espacio destacado en curso es dinero ya cobrado. Retirar el servidor
        // dejaria la portada vendiendo un hueco vacio y a nosotros debiendo un
        // servicio que no prestamos. Se deja y se anota.
        var destacado = facturacion.destacadoVigenteDe(servidor.id(), Instant.now(reloj));
        if (destacado.isPresent()) {
            log.warn(
                    "El servidor {} lleva {} dias sin señales pero tiene un espacio destacado pagado hasta {}. "
                            + "No se retira: revisar a mano.",
                    servidor.slug(),
                    dias,
                    destacado.get());
            return false;
        }

        if (servidores.archivarPorInactividad(servidor.id()) == 0) {
            return false;
        }

        log.info("Servidor {} retirado del directorio tras {} dias sin señales del plugin.", servidor.slug(), dias);
        escribirAlDueno(
                servidor,
                "Hemos retirado " + servidor.nombre() + " del directorio",
                """
                Hola,

                Tu servidor "%s" lleva %d dias sin conectar con LatinosMC, asi que
                lo hemos retirado del directorio y del ranking.

                No hemos borrado nada. Sus votos, su historial y su configuracion
                siguen ahi, y vuelve solo: en cuanto el plugin conecte otra vez,
                el servidor reaparece donde estaba, sin que tengas que escribirnos
                ni volver a registrarlo.

                Lo habitual es que sea una de estas tres:

                  1. El servidor esta apagado.
                  2. El plugin ya no esta instalado, o el archivo se perdio en una
                     actualizacion.
                  3. La clave del config.yml ya no es valida.

                Puedes revisarlo en %s/panel

                — LatinosMC
                """
                        .formatted(servidor.nombre(), dias, sitioUrl));

        return true;
    }

    private void avisar(ServerRepository.ServidorSinSenal servidor, long dias) {
        servidores.marcarAvisoDeInactividad(servidor.id());

        escribirAlDueno(
                servidor,
                servidor.nombre() + " lleva " + dias + " dias sin conectar",
                """
                Hola,

                Tu servidor "%s" lleva %d dias sin dar señales de vida a LatinosMC.

                Si sigue asi, dentro de %d dias lo retiraremos del directorio para
                que quien busque donde jugar no acabe en una ficha que no lleva a
                ninguna parte. No se borra nada, y vuelve solo en cuanto el plugin
                conecte otra vez.

                Si el servidor esta funcionando, lo mas probable es que el plugin
                se haya quedado sin instalar tras una actualizacion, o que la clave
                del config.yml ya no valga.

                Puedes revisarlo en %s/panel

                — LatinosMC
                """
                        .formatted(servidor.nombre(), dias, diasParaRetirar - dias, sitioUrl));
    }

    /**
     * Escribe a quien pueda hacer algo al respecto.
     *
     * <p>Un fallo de correo no puede tumbar la ronda: el servidor ya se retiro o
     * ya se marco como avisado, y volver a intentarlo mañana repetiria la accion.
     * Se registra y se sigue.
     */
    private void escribirAlDueno(ServerRepository.ServidorSinSenal servidor, String asunto, String cuerpo) {
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
