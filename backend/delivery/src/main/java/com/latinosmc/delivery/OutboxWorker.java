package com.latinosmc.delivery;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consume la tabla Outbox y entrega los eventos.
 *
 * <p>Usa {@code FOR UPDATE SKIP LOCKED}: varias instancias pueden consumir la
 * misma cola sin pisarse ni bloquearse entre ellas. Cada una coge filas que
 * ninguna otra tiene tomadas y sigue adelante.
 *
 * <p>Los reintentos usan backoff exponencial. El motivo no es tecnico sino de
 * cortesia: si el destino de un cliente esta caido, martillearlo cada segundo
 * empeora su situacion y no acelera la entrega.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private static final int LOTE = 50;
    private static final int MAX_INTENTOS = 8;

    private final JdbcClient jdbc;

    public OutboxWorker(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${latinosmc.outbox.intervalo-ms:2000}")
    @Transactional
    public void procesar() {
        List<Pendiente> lote = tomarLote();
        if (lote.isEmpty()) {
            return;
        }
        for (Pendiente evento : lote) {
            try {
                entregar(evento);
                marcarPublicado(evento.id());
            } catch (RuntimeException e) {
                registrarFallo(evento, e);
            }
        }
    }

    /**
     * Toma un lote de eventos pendientes bloqueandolos para esta transaccion.
     * SKIP LOCKED hace que otras instancias salten estas filas en vez de esperar.
     */
    private List<Pendiente> tomarLote() {
        return jdbc.sql(
                        """
                        SELECT id, event_type, payload::text AS payload, attempts
                          FROM outbox
                         WHERE status_code IN ('PENDING', 'FAILED')
                           AND next_attempt_at <= now()
                         ORDER BY next_attempt_at, created_at
                         LIMIT :lote
                           FOR UPDATE SKIP LOCKED
                        """)
                .param("lote", LOTE)
                .query((rs, fila) -> new Pendiente(
                        rs.getObject("id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getInt("attempts")))
                .list();
    }

    /**
     * Entrega el evento a sus destinos externos.
     *
     * <p>De momento no hay webhooks salientes configurados: las recompensas las
     * recoge el plugin consultando (ver {@code PluginController}). Se hizo asi a
     * proposito y no por simplicidad: la mayoria de servidores de Minecraft estan
     * detras de NAT o cortafuegos y no son alcanzables desde internet. Un modelo
     * de envio dejaria fuera a buena parte del mercado; uno de consulta funciona
     * en todos los casos.
     *
     * <p>Los webhooks salientes se anadiran aqui para quien si tenga endpoint
     * publico, sin cambiar nada del resto del flujo.
     */
    private void entregar(Pendiente evento) {
        log.debug("Evento {} listo para consumo: {}", evento.tipoEvento(), evento.id());
    }

    private void marcarPublicado(UUID id) {
        jdbc.sql(
                        """
                        UPDATE outbox
                           SET status_code = 'PUBLISHED', published_at = now()
                         WHERE id = :id
                        """)
                .param("id", id)
                .update();
    }

    private void registrarFallo(Pendiente evento, RuntimeException error) {
        int intentos = evento.intentos() + 1;
        boolean agotado = intentos >= MAX_INTENTOS;

        if (agotado) {
            log.error("Evento {} agotado tras {} intentos, va a DLQ", evento.id(), intentos, error);
        } else {
            log.warn("Fallo entregando evento {} (intento {}): {}", evento.id(), intentos, error.getMessage());
        }

        jdbc.sql(
                        """
                        UPDATE outbox
                           SET status_code = :estado,
                               attempts = :intentos,
                               next_attempt_at = :proximo,
                               last_error = :error
                         WHERE id = :id
                        """)
                .param("id", evento.id())
                .param("estado", agotado ? "DEAD" : "FAILED")
                .param("intentos", intentos)
                .param("proximo", Timestamp.from(proximoIntento(intentos)))
                .param("error", recortar(error.getMessage()))
                .update();
    }

    /** Backoff exponencial con techo: 2s, 4s, 8s... hasta 10 minutos. */
    private Instant proximoIntento(int intentos) {
        long segundos = Math.min(600L, (long) Math.pow(2, Math.min(intentos, 10)));
        return Instant.now().plus(Duration.ofSeconds(segundos));
    }

    private String recortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() > 500 ? mensaje.substring(0, 500) : mensaje;
    }

    private record Pendiente(UUID id, String tipoEvento, String payload, int intentos) {}
}
