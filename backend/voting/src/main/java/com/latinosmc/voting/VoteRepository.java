package com.latinosmc.voting;

import com.latinosmc.shared.ports.TrustEngine;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a los datos de votos. Solo el modulo {@code voting} toca estas tablas. */
@Repository
public class VoteRepository {

    private final JdbcClient jdbc;

    public VoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Busca un voto por su clave de idempotencia.
     *
     * <p>Un reintento del cliente (red inestable, plugin que reenvia) debe
     * devolver el mismo voto, no crear uno nuevo.
     */
    public Optional<Vote> buscarPorClaveIdempotencia(String clave) {
        return jdbc.sql(
                        """
                        SELECT id, server_id, status_code, identity_type_code, identity_value,
                               identity_hash, risk_score, rejection_reason, created_at
                          FROM votes
                         WHERE idempotency_key = :clave
                        """)
                .param("clave", clave)
                .query(VoteRepository::mapear)
                .optional();
    }

    /**
     * Instante del ultimo voto ACEPTADO de esta identidad a este servidor.
     *
     * <p>Es la consulta de la ventana deslizante (ADR-0011). Se apoya en el indice
     * parcial {@code votes_ventana}, que solo contiene votos aceptados y esta
     * ordenado descendente: responde leyendo una unica fila.
     *
     * <p>Los votos rechazados no cuentan a proposito: si un intento bloqueado por
     * el antifraude consumiera la ventana, un atacante podria impedir que un
     * jugador legitimo vote simplemente enviando basura en su nombre.
     */
    public Optional<Instant> ultimoVotoAceptado(UUID servidorId, byte[] identidadHash) {
        return jdbc.sql(
                        """
                        SELECT created_at
                          FROM votes
                         WHERE server_id = :servidor
                           AND identity_hash = :hash
                           AND status_code = 'ACCEPTED'
                         ORDER BY created_at DESC
                         LIMIT 1
                        """)
                .param("servidor", servidorId)
                .param("hash", identidadHash)
                .query(Instant.class)
                .optional();
    }

    /**
     * Ultimo voto aceptado de esta identidad en CUALQUIER servidor.
     *
     * <p>Es la regla global: un jugador vota a un servidor al dia, no a uno por
     * cada servidor. Cambia lo que mide el ranking —de "cuanta gente juega aqui"
     * a "cual es tu servidor favorito"— y por eso se puede desactivar desde
     * administracion.
     */
    public Optional<Instant> ultimoVotoAceptadoEnCualquierServidor(byte[] identidadHash) {
        return jdbc.sql(
                        """
                        SELECT created_at
                          FROM votes
                         WHERE identity_hash = :hash
                           AND status_code = 'ACCEPTED'
                         ORDER BY created_at DESC
                         LIMIT 1
                        """)
                .param("hash", identidadHash)
                .query(Instant.class)
                .optional();
    }

    /**
     * Cuantas identidades DISTINTAS han votado desde esta IP dentro de la ventana.
     *
     * <p>Se cuentan identidades y no votos: si se contaran votos, un jugador que
     * vota, se le revoca y vuelve a votar consumiria el cupo de su propia casa.
     *
     * <p>La identidad que pregunta se excluye del recuento. Sin esa exclusion, un
     * jugador que ya voto hoy desde su casa se bloquearia a si mismo al intentar
     * votar otra vez, y recibiria el mensaje equivocado: le diriamos que su IP
     * esta saturada cuando el problema es que ya voto.
     */
    public int identidadesDistintasDesdeIp(byte[] ipHash, byte[] identidadExcluida, Instant desde) {
        if (ipHash == null) {
            return 0;
        }
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(DISTINCT identity_hash)
                          FROM votes
                         WHERE ip_hash = :ip
                           AND status_code = 'ACCEPTED'
                           AND created_at >= :desde
                           AND identity_hash <> :yo
                        """)
                .param("ip", ipHash)
                .param("yo", identidadExcluida)
                .param("desde", java.sql.Timestamp.from(desde))
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }

    /**
     * Inserta el voto. Devuelve {@code true} si se creo, {@code false} si otra
     * peticion simultanea gano la carrera con la misma clave de idempotencia.
     *
     * <p>La proteccion es el indice unico, no una comprobacion previa en codigo:
     * dos peticiones concurrentes pueden pasar ambas un "SELECT antes de INSERT",
     * pero solo una gana contra el indice.
     */
    public boolean insertar(Vote voto, String claveIdempotencia, byte[] ipHash, String origen) {
        int filas = jdbc.sql(
                        """
                        INSERT INTO votes (id, server_id, status_code, identity_type_code, identity_value,
                                           identity_hash, ip_hash, source, idempotency_key,
                                           risk_score, rejection_reason, created_at, decided_at)
                        VALUES (:id, :servidor, :estado, :tipo, :valor,
                                :hash, :ipHash, :origen, :clave,
                                :riesgo, :motivo, :creado, :creado)
                        ON CONFLICT (idempotency_key) DO NOTHING
                        """)
                .param("id", voto.id())
                .param("servidor", voto.serverId())
                .param("estado", voto.estado().name())
                .param("tipo", voto.identidadTipo())
                .param("valor", voto.identidadValor())
                .param("hash", voto.identidadHash())
                .param("ipHash", ipHash)
                .param("origen", origen)
                .param("clave", claveIdempotencia)
                .param("riesgo", voto.puntuacionRiesgo())
                .param("motivo", voto.motivoRechazo())
                .param("creado", Timestamp.from(voto.creadoEn()))
                .update();
        return filas > 0;
    }

    /**
     * Guarda por que el motor de reglas puntuo como puntuo.
     *
     * <p>Sin esto, una suspension seria indefendible ante un cliente que reclama.
     */
    public void guardarSenales(UUID votoId, List<TrustEngine.Senal> senales) {
        for (TrustEngine.Senal senal : senales) {
            jdbc.sql(
                            """
                            INSERT INTO vote_risk_signals (id, vote_id, rule_code, score_delta, details)
                            VALUES (:id, :voto, :regla, :delta, CAST(:detalle AS jsonb))
                            """)
                    .param("id", com.latinosmc.shared.Ids.newId())
                    .param("voto", votoId)
                    .param("regla", senal.regla())
                    .param("delta", senal.delta())
                    .param("detalle", "{\"detalle\":" + comillas(senal.detalle()) + "}")
                    .update();
        }
    }

    /** Crea la recompensa pendiente de entrega asociada a un voto aceptado. */
    public void crearRecompensaPendiente(UUID votoId, UUID servidorId, String tipoRecompensa) {
        jdbc.sql(
                        """
                        INSERT INTO vote_rewards (id, vote_id, server_id, reward_type, status_code)
                        VALUES (:id, :voto, :servidor, :tipo, 'PENDING')
                        ON CONFLICT (vote_id) DO NOTHING
                        """)
                .param("id", com.latinosmc.shared.Ids.newId())
                .param("voto", votoId)
                .param("servidor", servidorId)
                .param("tipo", tipoRecompensa)
                .update();
    }

    private static Vote mapear(java.sql.ResultSet rs, int fila) throws java.sql.SQLException {
        Integer riesgo = rs.getObject("risk_score", Integer.class);
        return new Vote(
                rs.getObject("id", UUID.class),
                rs.getObject("server_id", UUID.class),
                VoteStatus.valueOf(rs.getString("status_code")),
                rs.getString("identity_type_code"),
                rs.getString("identity_value"),
                rs.getBytes("identity_hash"),
                riesgo,
                rs.getString("rejection_reason"),
                rs.getTimestamp("created_at").toInstant());
    }

    /** Escapa un texto para incrustarlo como cadena JSON. */
    private static String comillas(String valor) {
        if (valor == null) {
            return "null";
        }
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
