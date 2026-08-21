package com.latinosmc.servers;

import com.latinosmc.shared.ports.ServerDirectory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Acceso a los datos de servidores. Solo el modulo {@code servers} toca estas tablas. */
@Repository
public class ServerRepository {

    private final JdbcClient jdbc;

    public ServerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ServerDirectory.ServerInfo> buscarActivoPorSlug(String slug) {
        return jdbc.sql(
                        """
                        SELECT id, slug, name, accepts_unverifiable_identities
                          FROM servers
                         WHERE slug = :slug
                           AND status_code = 'ACTIVE'
                           AND deleted_at IS NULL
                        """)
                .param("slug", slug)
                .query((rs, fila) -> new ServerDirectory.ServerInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getBoolean("accepts_unverifiable_identities")))
                .optional();
    }

    public boolean existeSlug(String slug) {
        Integer n = jdbc.sql("SELECT COUNT(*) FROM servers WHERE slug = :slug AND deleted_at IS NULL")
                .param("slug", slug)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    public void insertar(ServidorNuevo servidor) {
        jdbc.sql(
                        """
                        INSERT INTO servers (id, organization_id, game_type_code, status_code, slug, name,
                                             description, host, port,
                                             accepts_unverifiable_identities)
                        VALUES (:id, :org, :juego, :estado, :slug, :nombre,
                                :descripcion, :host, :puerto, :offline)
                        """)
                .param("id", servidor.id())
                .param("org", servidor.organizacionId())
                .param("juego", servidor.tipoJuego())
                .param("estado", servidor.estado())
                .param("slug", servidor.slug())
                .param("nombre", servidor.nombre())
                .param("descripcion", servidor.descripcion())
                .param("host", servidor.host())
                .param("puerto", servidor.puerto())
                .param("offline", servidor.aceptaOffline())
                .update();
    }

    /** Guarda el hash de una clave de API del plugin. El valor en claro no se persiste nunca. */
    public void guardarClaveApi(UUID id, UUID servidorId, String hash, String prefijo) {
        jdbc.sql(
                        """
                        INSERT INTO server_api_keys (id, server_id, key_hash, key_prefix)
                        VALUES (:id, :servidor, :hash, :prefijo)
                        """)
                .param("id", id)
                .param("servidor", servidorId)
                .param("hash", hash)
                .param("prefijo", prefijo)
                .update();
    }

    /**
     * Marca el servidor como verificado y activo.
     *
     * <p>La verificacion es la primera conexion del plugin: si puede autenticarse
     * con la clave que solo ve el dueno, es que la puso en su servidor. Eso ES la
     * prueba de propiedad, y ahorra un paso entero del embudo de activacion.
     */
    public int activarPorPrimeraConexion(UUID servidorId) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET status_code = 'ACTIVE',
                               verified_at = COALESCE(verified_at, now()),
                               updated_at = now()
                         WHERE id = :id
                           AND status_code = 'PENDING_VERIFICATION'
                        """)
                .param("id", servidorId)
                .update();
    }

    /** Busca el servidor dueno de una clave de API, si la clave sigue vigente. */
    public Optional<UUID> servidorPorClaveApi(String hash) {
        return jdbc.sql(
                        """
                        SELECT server_id
                          FROM server_api_keys
                         WHERE key_hash = :hash
                           AND revoked_at IS NULL
                           AND (expires_at IS NULL OR expires_at > now())
                        """)
                .param("hash", hash)
                .query(UUID.class)
                .optional();
    }

    public void registrarUsoDeClave(String hash) {
        jdbc.sql("UPDATE server_api_keys SET last_used_at = now() WHERE key_hash = :hash")
                .param("hash", hash)
                .update();
    }

    /** Servidores vivos de una organizacion. Se usa para aplicar el limite del plan. */
    public int contarDe(UUID organizacionId) {
        Integer n = jdbc.sql(
                        """
                        SELECT COUNT(*) FROM servers
                         WHERE organization_id = :org
                           AND deleted_at IS NULL
                           AND status_code <> 'ARCHIVED'
                        """)
                .param("org", organizacionId)
                .query(Integer.class)
                .single();
        return n == null ? 0 : n;
    }

    /** Servidores de una organizacion, para la pantalla "mis servidores". */
    public List<ResumenServidor> listarDe(UUID organizacionId) {
        return jdbc.sql(
                        """
                        SELECT id, slug, name, description, status_code
                          FROM servers
                         WHERE organization_id = :org AND deleted_at IS NULL
                         ORDER BY created_at DESC
                        """)
                .param("org", organizacionId)
                .query((rs, fila) -> new ResumenServidor(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status_code")))
                .list();
    }

    // --- Imagenes (ADR-0017) -------------------------------------------------

    public String claveIcono(UUID servidorId) {
        return jdbc.sql("SELECT icon_key FROM servers WHERE id = :id")
                .param("id", servidorId)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    public String claveBanner(UUID servidorId) {
        return jdbc.sql("SELECT banner_key FROM servers WHERE id = :id")
                .param("id", servidorId)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    /**
     * Actualiza el icono.
     *
     * <p>La condicion sobre {@code organization_id} viaja dentro del UPDATE, no en
     * una comprobacion previa. Asi la propia base de datos garantiza que nadie
     * modifica un servidor ajeno, aunque quien llame se olvide de comprobarlo.
     *
     * @return filas afectadas; 0 significa que el servidor no es de esa organizacion
     */
    public int actualizarIcono(UUID servidorId, UUID organizacionId, String clave) {
        return jdbc.sql(
                        """
                        UPDATE servers SET icon_key = :clave, updated_at = now()
                         WHERE id = :id AND organization_id = :org AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .param("org", organizacionId)
                .param("clave", clave)
                .update();
    }

    public int actualizarBanner(UUID servidorId, UUID organizacionId, String clave) {
        return jdbc.sql(
                        """
                        UPDATE servers SET banner_key = :clave, updated_at = now()
                         WHERE id = :id AND organization_id = :org AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .param("org", organizacionId)
                .param("clave", clave)
                .update();
    }

    public void registrarSubida(
            UUID id,
            UUID organizacionId,
            UUID usuarioId,
            String clave,
            String contentType,
            long tamanoBytes,
            Integer ancho,
            Integer alto,
            String proposito) {

        jdbc.sql(
                        """
                        INSERT INTO uploads (id, organization_id, uploaded_by, storage_key,
                                             content_type, size_bytes, width, height, purpose)
                        VALUES (:id, :org, :usuario, :clave, :tipo, :tamano, :ancho, :alto, :proposito)
                        """)
                .param("id", id)
                .param("org", organizacionId)
                .param("usuario", usuarioId)
                .param("clave", clave)
                .param("tipo", contentType)
                .param("tamano", tamanoBytes)
                .param("ancho", ancho)
                .param("alto", alto)
                .param("proposito", proposito)
                .update();
    }

    /** Marca una subida como reemplazada, para que la limpieza la borre del almacen. */
    public void marcarSubidaHuerfana(String clave) {
        jdbc.sql("UPDATE uploads SET deleted_at = now() WHERE storage_key = :clave AND deleted_at IS NULL")
                .param("clave", clave)
                .update();
    }

    // --- Imagenes automaticas (ADR-0027) -------------------------------------

    /**
     * Lo que hace falta saber, tras un sondeo, para decidir si se readoptan las
     * imagenes de un servidor.
     */
    public Optional<EstadoDeImagenesAutomaticas> estadoDeImagenesAutomaticas(UUID servidorId) {
        return jdbc.sql(
                        """
                        SELECT organization_id, name, host,
                               icon_source, banner_source,
                               motd_hash, favicon_hash,
                               icon_key_auto, banner_key_auto,
                               auto_images_updated_at,
                               auto_images_blocked_at
                          FROM servers
                         WHERE id = :id AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .query((rs, fila) -> new EstadoDeImagenesAutomaticas(
                        rs.getObject("organization_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("host"),
                        "AUTO".equals(rs.getString("icon_source")),
                        "AUTO".equals(rs.getString("banner_source")),
                        rs.getString("motd_hash"),
                        rs.getString("favicon_hash"),
                        rs.getString("icon_key_auto"),
                        rs.getString("banner_key_auto"),
                        rs.getTimestamp("auto_images_updated_at") == null
                                ? null
                                : rs.getTimestamp("auto_images_updated_at").toInstant(),
                        rs.getTimestamp("auto_images_blocked_at") != null))
                .optional();
    }

    /**
     * Guarda lo adoptado del ping.
     *
     * <p>Las claves se pasan siempre, tambien cuando no cambian: el llamador ya
     * sabe cual es la vigente de cada una y mandar dos UPDATE distintos segun que
     * haya cambiado solo abriria la puerta a dejar la fila a medias.
     */
    public int guardarImagenesAutomaticas(
            UUID servidorId,
            String claveIcono,
            String claveBanner,
            String motdJson,
            String hashMotd,
            String hashFavicon) {

        return jdbc.sql(
                        """
                        UPDATE servers
                           SET icon_key_auto          = :icono,
                               banner_key_auto        = :banner,
                               motd_json              = :motd,
                               motd_hash              = :hashMotd,
                               favicon_hash           = :hashFavicon,
                               auto_images_updated_at = now(),
                               updated_at             = now()
                         WHERE id = :id
                           AND deleted_at IS NULL
                           AND auto_images_blocked_at IS NULL
                        """)
                .param("id", servidorId)
                .param("icono", claveIcono)
                .param("banner", claveBanner)
                .param("motd", motdJson)
                .param("hashMotd", hashMotd)
                .param("hashFavicon", hashFavicon)
                .update();
    }

    /** Cambia el origen de una imagen entre automatico y subido por el dueño. */
    public int cambiarOrigenDeImagen(
            UUID servidorId, UUID organizacionId, String cual, boolean automatico) {

        String columna = "ICON".equals(cual) ? "icon_source" : "banner_source";

        // El nombre de columna no viene de fuera: se elige aqui entre dos literales
        // segun un valor que el llamador ya ha validado. Interpolar lo que llegue
        // por la API en una consulta seria inyeccion de SQL, y ni con lista blanca
        // conviene acostumbrarse; por eso la eleccion es de este metodo y no del
        // que llama.
        return jdbc.sql(
                        """
                        UPDATE servers SET %s = :origen, updated_at = now()
                         WHERE id = :id AND organization_id = :org AND deleted_at IS NULL
                        """
                                .formatted(columna))
                .param("id", servidorId)
                .param("org", organizacionId)
                .param("origen", automatico ? "AUTO" : "MANUAL")
                .update();
    }

    /**
     * Bloquea las imagenes automaticas de un servidor y borra las que hubiera.
     *
     * <p>Es la retirada del ADR-0027: adoptamos contenido de terceros al instante,
     * y esto es lo que lo deshace cuando alguien pone algo que no debe. Deja las
     * claves a nulo ademas de marcar el bloqueo, para que ninguna consulta del
     * directorio tenga que acordarse de mirar si esta bloqueado.
     */
    public int bloquearImagenesAutomaticas(UUID servidorId, String motivo) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET auto_images_blocked_at     = now(),
                               auto_images_blocked_reason = :motivo,
                               icon_key_auto              = NULL,
                               banner_key_auto            = NULL,
                               motd_json                  = NULL,
                               motd_hash                  = NULL,
                               favicon_hash               = NULL,
                               updated_at                 = now()
                         WHERE id = :id AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .param("motivo", motivo)
                .update();
    }

    public int desbloquearImagenesAutomaticas(UUID servidorId) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET auto_images_blocked_at     = NULL,
                               auto_images_blocked_reason = NULL,
                               auto_images_updated_at     = NULL,
                               updated_at                 = now()
                         WHERE id = :id AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .update();
    }

    /**
     * @param iconoAutomatico si el icono sale del ping o lo subio el dueño
     * @param hashMotd hash del MOTD ya saneado la ultima vez que se adopto
     * @param actualizadoEn null si nunca se ha adoptado nada
     */
    public record EstadoDeImagenesAutomaticas(
            UUID organizacionId,
            String nombre,
            String host,
            boolean iconoAutomatico,
            boolean bannerAutomatico,
            String hashMotd,
            String hashFavicon,
            String claveIconoAuto,
            String claveBannerAuto,
            Instant actualizadoEn,
            boolean bloqueado) {}

    // --- Sondeo directo (ADR-0009) -------------------------------------------

    /**
     * Servidores que llevan mas tiempo sin sondear.
     *
     * <p>Ordenar por "el mas antiguo primero" reparte los sondeos de forma justa:
     * ninguno se queda sin medir porque siempre entren otros antes.
     */
    public List<ServidorParaSondear> siguientesParaSondear(int limite) {
        return jdbc.sql(
                        """
                        SELECT s.id, s.host, s.port
                          FROM servers s
                          LEFT JOIN LATERAL (
                                SELECT observed_at
                                  FROM server_pings sp
                                 WHERE sp.server_id = s.id
                                 ORDER BY sp.observed_at DESC
                                 LIMIT 1
                          ) p ON TRUE
                         WHERE s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                         ORDER BY p.observed_at ASC NULLS FIRST
                         LIMIT :limite
                        """)
                .param("limite", limite)
                .query((rs, fila) -> new ServidorParaSondear(
                        rs.getObject("id", UUID.class), rs.getString("host"), rs.getInt("port")))
                .list();
    }

    public void registrarPing(
            UUID id,
            UUID servidorId,
            boolean enLinea,
            Integer jugadores,
            Integer maximos,
            Integer latenciaMs,
            String version,
            String error) {

        jdbc.sql(
                        """
                        INSERT INTO server_pings (id, server_id, is_online, players_online,
                                                  players_max, latency_ms, game_version, error_message)
                        VALUES (:id, :servidor, :online, :jugadores, :maximos, :latencia, :version, :error)
                        """)
                .param("id", id)
                .param("servidor", servidorId)
                .param("online", enLinea)
                .param("jugadores", jugadores)
                .param("maximos", maximos)
                .param("latencia", latenciaMs)
                .param("version", version)
                .param("error", error)
                .update();
    }

    /** Borra sondeos antiguos. Sin esto la tabla crece sin parar. */
    public int purgarPingsAnterioresA(java.time.Instant limite) {
        return jdbc.sql("DELETE FROM server_pings WHERE observed_at < :limite")
                .param("limite", java.sql.Timestamp.from(limite))
                .update();
    }

    public record ServidorParaSondear(UUID id, String host, int puerto) {}

    /**
     * Ficha publica de un servidor, por slug.
     *
     * <p>Solo devuelve lo que puede ver cualquiera. La consulta NO selecciona
     * {@code organization_id}: si estuviera en el resultado, tarde o temprano
     * alguien lo expondria en una respuesta sin darse cuenta.
     *
     * <p>Los jugadores conectados salen de nuestro propio ping, no de lo que
     * reporte el plugin (ADR-0009).
     */
    public Optional<PublicoServidor> buscarPublicoPorSlug(String slug) {
        return jdbc.sql(
                        """
                        SELECT s.id, s.slug, s.name, s.description, s.host, s.port,
                               s.accepts_unverifiable_identities,
                               -- Columnas calculadas: la regla de que imagen se
                               -- enseña vive en la base de datos (ADR-0027).
                               s.display_icon_key, s.display_banner_key, s.motd_json,
                               s.version_min, s.version_max, s.hosting_region, s.theme_code,
                               p.players_online, p.players_max, p.is_online,
                               (SELECT COUNT(*) FROM votes v
                                 WHERE v.server_id = s.id
                                   AND v.status_code = 'ACCEPTED'
                                   AND v.created_at >= now() - interval '7 days') AS votos
                          FROM servers s
                          LEFT JOIN LATERAL (
                                SELECT players_online, players_max, is_online
                                  FROM server_pings sp
                                 WHERE sp.server_id = s.id
                                 ORDER BY sp.observed_at DESC
                                 LIMIT 1
                          ) p ON TRUE
                         WHERE s.slug = :slug
                           AND s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                        """)
                .param("slug", slug)
                .query((rs, fila) -> {
                    int puerto = rs.getInt("port");
                    // El puerto 25565 es el de Minecraft por defecto: escribirlo
                    // solo confunde al jugador que copia la direccion.
                    String direccion = puerto == 25565
                            ? rs.getString("host")
                            : rs.getString("host") + ":" + puerto;

                    return new PublicoServidor(
                            rs.getObject("id", UUID.class),
                            rs.getString("slug"),
                            rs.getString("name"),
                            rs.getString("description"),
                            direccion,
                            rs.getBoolean("accepts_unverifiable_identities"),
                            rs.getString("display_icon_key"),
                            rs.getString("display_banner_key"),
                            rs.getString("motd_json"),
                            rs.getObject("players_online", Integer.class),
                            rs.getObject("players_max", Integer.class),
                            rs.getBoolean("is_online"),
                            rs.getLong("votos"),
                            rs.getString("version_min"),
                            rs.getString("version_max"),
                            rs.getString("hosting_region"),
                            rs.getString("theme_code"));
                })
                .optional();
    }

    public record PublicoServidor(
            UUID id,
            String slug,
            String nombre,
            String descripcion,
            String direccionPublica,
            boolean aceptaOffline,
            String claveIcono,
            String claveBanner,
            /** MOTD leido del ping, ya saneado. Null si el servidor no anuncia ninguno. */
            String motdJson,
            Integer jugadoresConectados,
            Integer jugadoresMaximo,
            boolean enLinea,
            long votosSemana,
            String versionMinima,
            String versionMaxima,
            String region,
            String tema) {}

    /**
     * Da de baja un servidor.
     *
     * <p><strong>Baja logica, no borrado.</strong> Los votos apuntan al servidor
     * con {@code ON DELETE RESTRICT}: borrarlo de verdad exigiria borrar antes el
     * historial de votos de todo el que voto ahi, y eso destruiria las señales
     * que el antifraude usa para detectar patrones. Ademas, si el dueño vuelve
     * mañana, un servidor archivado se recupera y uno borrado no.
     *
     * <p>La condicion por organizacion viaja en el WHERE. Es lo que impide dar de
     * baja el servidor de otro pasando su identificador: comprobarlo antes en
     * codigo dejaria una ventana entre la comprobacion y la escritura.
     *
     * @return numero de filas afectadas; cero si no existe, ya estaba de baja o
     *     no es de esa organizacion
     */
    public int darDeBaja(UUID servidorId, UUID organizacionId) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET deleted_at  = now(),
                               status_code = 'ARCHIVED',
                               updated_at  = now()
                         WHERE id = :id
                           AND organization_id = :org
                           AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .param("org", organizacionId)
                .update();
    }

    /**
     * Revoca las claves de API de un servidor.
     *
     * <p>Se llama al darlo de baja. Sin esto, el plugin de un servidor archivado
     * seguiria autenticandose contra la API para siempre: el dueño lo dio de baja
     * en la web pero el JAR sigue instalado en su maquina, latiendo cada minuto.
     */
    public int revocarClaves(UUID servidorId) {
        return jdbc.sql(
                        """
                        UPDATE server_api_keys
                           SET revoked_at = now()
                         WHERE server_id = :id AND revoked_at IS NULL
                        """)
                .param("id", servidorId)
                .update();
    }

    /**
     * Cifras de la plataforma para la cabecera.
     *
     * <p>Los jugadores se suman del ultimo ping de cada servidor, y solo si ese
     * ping es reciente. Sin el limite de frescura, un servidor caido hace tres
     * dias seguiria aportando los 300 jugadores que tenia la ultima vez que
     * respondio, y el total de la portada seria una cifra inventada que solo
     * sube.
     *
     * <p>Quince minutos es holgado respecto al intervalo con el que medimos:
     * cubre un ping perdido sin dejar fuera a un servidor que si esta vivo.
     */
    public Estadisticas estadisticas() {
        return jdbc.sql(
                        """
                        SELECT
                            COUNT(*)                                                  AS servidores,
                            COUNT(*) FILTER (WHERE p.is_online)                        AS en_linea,
                            COALESCE(SUM(p.players_online) FILTER (WHERE p.is_online), 0) AS jugadores
                          FROM servers s
                          LEFT JOIN LATERAL (
                                SELECT players_online, is_online
                                  FROM server_pings sp
                                 WHERE sp.server_id = s.id
                                   AND sp.observed_at >= now() - interval '15 minutes'
                                 ORDER BY sp.observed_at DESC
                                 LIMIT 1
                          ) p ON TRUE
                         WHERE s.status_code = 'ACTIVE'
                           AND s.deleted_at IS NULL
                        """)
                .query((rs, fila) -> new Estadisticas(
                        rs.getInt("servidores"), rs.getInt("en_linea"), rs.getLong("jugadores")))
                .single();
    }

    /**
     * @param servidores todos los activos, respondan o no: un servidor caido
     *     sigue existiendo y sigue apareciendo en la lista
     * @param enLinea cuantos responden al ping ahora mismo
     * @param jugadores suma de conectados, medida por nosotros (ADR-0009)
     */
    public record Estadisticas(int servidores, int enLinea, long jugadores) {}

    /**
     * Si el servidor responde al ping ahora mismo.
     *
     * <p>Se usa para no aceptar votos a servidores caidos: votar a algo donde no
     * se puede entrar no mide nada, y ademas la recompensa nunca se entregaria.
     *
     * <p>Devuelve {@code true} si no hay ningun ping reciente. Es deliberado: la
     * duda beneficia al servidor. Un fallo de NUESTRA medicion no puede costarle
     * los votos del dia a alguien que si esta funcionando.
     */
    public boolean respondeAlPing(UUID servidorId) {
        Boolean enLinea = jdbc.sql(
                        """
                        SELECT is_online
                          FROM server_pings
                         WHERE server_id = :servidor
                           AND observed_at >= now() - interval '15 minutes'
                         ORDER BY observed_at DESC
                         LIMIT 1
                        """)
                .param("servidor", servidorId)
                .query(Boolean.class)
                .optional()
                .orElse(null);

        return enLinea == null || enLinea;
    }

    /** Catalogo de modalidades, para el selector del panel. */
    public List<Modalidad> modalidades() {
        return jdbc.sql("SELECT code, name FROM game_modes ORDER BY sort_order, name")
                .query((rs, fila) -> new Modalidad(rs.getString("code"), rs.getString("name")))
                .list();
    }

    /** Modalidades de un servidor, en el orden del catalogo. */
    public List<Modalidad> modalidadesDe(UUID servidorId) {
        return jdbc.sql(
                        """
                        SELECT m.code, m.name
                          FROM server_game_modes sm
                          JOIN game_modes m ON m.code = sm.mode_code
                         WHERE sm.server_id = :servidor
                         ORDER BY m.sort_order, m.name
                        """)
                .param("servidor", servidorId)
                .query((rs, fila) -> new Modalidad(rs.getString("code"), rs.getString("name")))
                .list();
    }

    /**
     * Modalidades de varios servidores de una vez.
     *
     * <p>Existe para pintar el listado. Pidiendolas una a una harian falta
     * cincuenta consultas para pintar cincuenta tarjetas — el problema N+1, que
     * no se nota en desarrollo con tres servidores y tumba la portada con
     * quinientos.
     */
    public java.util.Map<UUID, List<Modalidad>> modalidadesDeVarios(List<UUID> servidorIds) {
        if (servidorIds == null || servidorIds.isEmpty()) {
            return java.util.Map.of();
        }

        // IN con una lista y no `= ANY(array)`: con parametros con nombre, Spring
        // expande una lista a (?, ?, ?) pero NO sabe convertir un array de Java
        // al tipo array de PostgreSQL. Con ANY la consulta falla en tiempo de
        // ejecucion, y como esto se ejecuta al pintar el ranking, tumbaba la
        // portada entera.
        //
        // El mapeador devuelve una fila de verdad y el agrupado se hace despues.
        // Antes acumulaba en un mapa y devolvia null por fila: un RowMapper que
        // devuelve null es justo lo que Spring no garantiza que acepte, y era el
        // segundo sospechoso del mismo fallo.
        record Fila(UUID servidorId, Modalidad modalidad) {}

        List<Fila> filas = jdbc.sql(
                        """
                        SELECT sm.server_id, m.code, m.name
                          FROM server_game_modes sm
                          JOIN game_modes m ON m.code = sm.mode_code
                         WHERE sm.server_id IN (:servidores)
                         ORDER BY m.sort_order, m.name
                        """)
                .param("servidores", servidorIds)
                .query((rs, i) -> new Fila(
                        rs.getObject("server_id", UUID.class),
                        new Modalidad(rs.getString("code"), rs.getString("name"))))
                .list();

        var porServidor = new java.util.HashMap<UUID, List<Modalidad>>();
        for (Fila fila : filas) {
            porServidor
                    .computeIfAbsent(fila.servidorId(), k -> new java.util.ArrayList<>())
                    .add(fila.modalidad());
        }
        return porServidor;
    }

    /**
     * Reemplaza las modalidades de un servidor.
     *
     * <p>Borra y vuelve a insertar en lugar de calcular diferencias: son cinco
     * filas como mucho, y el codigo de "cual quitar y cual añadir" es donde se
     * cuelan los errores por un puñado de microsegundos que no importan.
     */
    public void reemplazarModalidades(UUID servidorId, List<String> codigos) {
        jdbc.sql("DELETE FROM server_game_modes WHERE server_id = :servidor")
                .param("servidor", servidorId)
                .update();

        for (String codigo : codigos.stream().distinct().limit(8).toList()) {
            // ON CONFLICT por si llegan repetidos con distinta caja; el codigo
            // inexistente lo rechaza la clave ajena, que es donde debe rechazarse.
            jdbc.sql(
                            """
                            INSERT INTO server_game_modes (server_id, mode_code)
                            VALUES (:servidor, :codigo)
                            ON CONFLICT DO NOTHING
                            """)
                    .param("servidor", servidorId)
                    .param("codigo", codigo)
                    .update();
        }
    }

    public record Modalidad(String codigo, String nombre) {}

    /** Ficha completa de un servidor para su propietario. */
    public Optional<DetalleServidor> detalle(UUID servidorId) {
        return jdbc.sql(
                        """
                        SELECT id, organization_id, slug, name, description, status_code,
                               host, port, accepts_unverifiable_identities,
                               display_icon_key, display_banner_key, verified_at,
                               version_min, version_max, hosting_region, theme_code,
                               -- El panel, a diferencia del directorio, si necesita
                               -- ver los dos origenes por separado: el dueño tiene
                               -- que poder comparar lo automatico con lo que subio
                               -- antes de decidir con cual se queda (ADR-0027).
                               icon_source, banner_source,
                               icon_key, banner_key,
                               icon_key_auto, banner_key_auto,
                               motd_json, auto_images_blocked_at
                          FROM servers
                         WHERE id = :id AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .query((rs, fila) -> new DetalleServidor(
                        rs.getObject("id", UUID.class),
                        rs.getObject("organization_id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status_code"),
                        rs.getString("host"),
                        rs.getInt("port"),
                        rs.getBoolean("accepts_unverifiable_identities"),
                        rs.getString("display_icon_key"),
                        rs.getString("display_banner_key"),
                        rs.getTimestamp("verified_at") != null,
                        rs.getString("version_min"),
                        rs.getString("version_max"),
                        rs.getString("hosting_region"),
                        rs.getString("theme_code"),
                        "AUTO".equals(rs.getString("icon_source")),
                        "AUTO".equals(rs.getString("banner_source")),
                        rs.getString("icon_key"),
                        rs.getString("banner_key"),
                        rs.getString("icon_key_auto"),
                        rs.getString("banner_key_auto"),
                        rs.getString("motd_json"),
                        rs.getTimestamp("auto_images_blocked_at") != null))
                .optional();
    }

    /**
     * @param claveIcono la que se enseña, ya resuelta segun el origen
     * @param iconoAutomatico si el icono sale del ping o lo subio el dueño
     * @param claveIconoSubido lo que subio el dueño, exista o no; el panel lo
     *     necesita para poder ofrecer "volver a mi imagen" sin haberla perdido
     */
    public record DetalleServidor(
            UUID id,
            UUID organizacionId,
            String slug,
            String nombre,
            String descripcion,
            String estado,
            String host,
            int puerto,
            boolean aceptaOffline,
            String claveIcono,
            String claveBanner,
            boolean verificado,
            String versionMinima,
            String versionMaxima,
            String region,
            String tema,
            boolean iconoAutomatico,
            boolean bannerAutomatico,
            String claveIconoSubido,
            String claveBannerSubido,
            String claveIconoAuto,
            String claveBannerAuto,
            String motdJson,
            boolean imagenesAutomaticasBloqueadas) {}

    /**
     * Actualiza lo que el dueño puede cambiar de su ficha.
     *
     * <p>Deliberadamente NO incluye el slug ni la direccion. El slug es la URL
     * publica y cambiarlo rompe los enlaces que ya circulan por Discord; la
     * direccion es lo que medimos por ping, y permitir cambiarla libremente
     * dejaria apuntar el ping a un servidor ajeno con mas jugadores.
     *
     * <p>La condicion por organizacion viaja en el WHERE. Es lo que impide que
     * alguien edite la ficha de otro pasando su identificador: si se comprobara
     * antes en codigo, quedaria una ventana entre la comprobacion y la escritura.
     */
    public int actualizarFicha(
            UUID servidorId,
            UUID organizacionId,
            String descripcion,
            String versionMinima,
            String versionMaxima,
            String region,
            String tema) {

        return jdbc.sql(
                        """
                        UPDATE servers
                           SET description     = :descripcion,
                               version_min     = :vmin,
                               version_max     = :vmax,
                               hosting_region  = :region,
                               theme_code      = :tema,
                               updated_at      = now()
                         WHERE id = :id AND organization_id = :org AND deleted_at IS NULL
                        """)
                .param("id", servidorId)
                .param("org", organizacionId)
                .param("descripcion", descripcion)
                .param("vmin", versionMinima)
                .param("vmax", versionMaxima)
                .param("region", region)
                .param("tema", tema)
                .update();
    }

    /** Organizacion dueña de un servidor. Devuelve vacio si no existe. */
    public Optional<UUID> organizacionDe(UUID servidorId) {
        return jdbc.sql("SELECT organization_id FROM servers WHERE id = :id AND deleted_at IS NULL")
                .param("id", servidorId)
                .query(UUID.class)
                .optional();
    }

    public List<ResumenServidor> listarActivos() {
        return jdbc.sql(
                        """
                        SELECT id, slug, name, description, status_code
                          FROM servers
                         WHERE deleted_at IS NULL
                           AND status_code = 'ACTIVE'
                         ORDER BY name
                        """)
                .query((rs, fila) -> new ResumenServidor(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status_code")))
                .list();
    }

    // --- Inactividad (ADR-0024) ----------------------------------------------

    /**
     * Servidores vivos que llevan dias sin dar señales del plugin.
     *
     * <p>La señal es {@code last_used_at} de sus claves: el plugin la refresca en
     * cada llamada autenticada, y late cada minuto. Se toma la mas reciente de
     * todas sus claves —un servidor puede tener varias— y, si nunca se uso
     * ninguna, la fecha de alta: un servidor que se registro y jamas conecto
     * tambien es un servidor que no esta.
     *
     * <p>Se excluyen los que el dueño ya dio de baja ({@code deleted_at}): esos
     * no vuelven solos y no hay a quien avisar.
     */
    public List<ServidorSinSenal> sinSenalDelPlugin(int diasMinimos, int limite) {
        return jdbc.sql(
                        """
                        SELECT s.id, s.organization_id, s.slug, s.name, s.status_code,
                               s.inactivity_warned_at,
                               COALESCE(MAX(k.last_used_at), s.created_at) AS ultima_senal
                          FROM servers s
                          LEFT JOIN server_api_keys k
                                 ON k.server_id = s.id AND k.revoked_at IS NULL
                         WHERE s.deleted_at IS NULL
                           AND s.status_code IN ('ACTIVE', 'PENDING_VERIFICATION')
                         GROUP BY s.id
                        HAVING COALESCE(MAX(k.last_used_at), s.created_at)
                                   < now() - make_interval(days => :dias)
                         ORDER BY ultima_senal
                         LIMIT :limite
                        """)
                .param("dias", diasMinimos)
                .param("limite", limite)
                .query((rs, fila) -> new ServidorSinSenal(
                        rs.getObject("id", UUID.class),
                        rs.getObject("organization_id", UUID.class),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getTimestamp("ultima_senal").toInstant(),
                        rs.getTimestamp("inactivity_warned_at") == null
                                ? null
                                : rs.getTimestamp("inactivity_warned_at").toInstant()))
                .list();
    }

    /** Deja constancia de que ya se aviso, para no repetirlo cada noche. */
    public int marcarAvisoDeInactividad(UUID servidorId) {
        return jdbc.sql("UPDATE servers SET inactivity_warned_at = now() WHERE id = :id")
                .param("id", servidorId)
                .update();
    }

    /**
     * Retira el servidor del directorio por inactividad.
     *
     * <p>No toca {@code deleted_at}: esto se deshace solo en cuanto el plugin
     * vuelva a latir. Las claves tampoco se revocan, justamente para que pueda
     * volver.
     */
    public int archivarPorInactividad(UUID servidorId) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET status_code = 'ARCHIVED',
                               archived_at = now(),
                               updated_at  = now()
                         WHERE id = :id
                           AND deleted_at IS NULL
                           AND status_code <> 'ARCHIVED'
                        """)
                .param("id", servidorId)
                .update();
    }

    /**
     * Devuelve al directorio un servidor que se habia retirado por inactividad.
     *
     * <p>Lo llama el latido del plugin. Solo revive los archivados por nosotros
     * ({@code archived_at} puesto): un servidor que su dueño dio de baja no
     * puede volver porque el JAR siga instalado en su maquina.
     */
    public int reactivarTrasInactividad(UUID servidorId) {
        return jdbc.sql(
                        """
                        UPDATE servers
                           SET status_code          = 'ACTIVE',
                               archived_at          = NULL,
                               inactivity_warned_at = NULL,
                               updated_at           = now()
                         WHERE id = :id
                           AND deleted_at IS NULL
                           AND archived_at IS NOT NULL
                        """)
                .param("id", servidorId)
                .update();
    }

    /**
     * @param ultimaSenal ultima vez que su plugin dio señales, o el alta si nunca
     * @param avisadoEn null si todavia no se le ha avisado
     */
    public record ServidorSinSenal(
            UUID id,
            UUID organizacionId,
            String slug,
            String nombre,
            Instant ultimaSenal,
            Instant avisadoEn) {}

    public record ServidorNuevo(
            UUID id,
            UUID organizacionId,
            String tipoJuego,
            String estado,
            String slug,
            String nombre,
            String descripcion,
            String host,
            int puerto,
            boolean aceptaOffline) {}

    public record ResumenServidor(UUID id, String slug, String nombre, String descripcion, String estado) {}
}
