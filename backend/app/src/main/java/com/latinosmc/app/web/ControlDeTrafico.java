package com.latinosmc.app.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Control de trafico por IP.
 *
 * <p>Sin esto, cualquiera que descubra la direccion de la API puede lanzar
 * peticiones sin limite: no para robar nada, sino para tumbar el servicio o
 * llenar la base de datos de basura. Es el ataque mas barato que existe contra
 * una API publica y no requiere ninguna habilidad.
 *
 * <p><b>Limites distintos por tipo de ruta</b>, porque cuestan cosas distintas:
 *
 * <ul>
 *   <li><b>Votar</b> escribe en la base de datos y ejecuta el antifraude. Es lo
 *       mas caro y lo mas atacado: limite estricto.
 *   <li><b>Ranking</b> es de lectura y va cacheado: limite generoso.
 *   <li><b>Plugin</b> son servidores legitimos consultando cada 30 s. Aqui el
 *       limite se aplica por clave, no por IP: un hosting compartido puede tener
 *       cien servidores tras la misma IP y no deben castigarse entre ellos.
 * </ul>
 *
 * <p>Es una primera barrera dentro de la aplicacion, no la unica. Un ataque de
 * volumen real se frena antes, en el proxy o en Cloudflare: cuando la peticion
 * llega hasta aqui, ya ha consumido un hilo y una conexion.
 */
@Component
@Order(1)
public class ControlDeTrafico extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ControlDeTrafico.class);

    /**
     * Categorias de trafico. Cada una lleva su propio contador.
     *
     * <p>Es importante que no compartan cupo: si lecturas y escrituras contaran
     * juntas, un usuario navegando por el ranking se quedaria sin poder crear un
     * servidor. Y si el login compartiera cupo con el resto, equivocarse dos veces
     * de contrasena dejaria la cuenta bloqueada un minuto.
     */
    private enum Categoria {
        /** Escribe en BD y ejecuta el antifraude. Un humano no vota 10 veces por minuto. */
        VOTO(10),

        /**
         * Login y registro. Suficiente para un humano que se equivoca varias veces;
         * insuficiente para probar contrasenas por fuerza bruta.
         */
        AUTENTICACION(12),

        /** Un plugin normal consulta 2 veces por minuto. 60 deja margen de sobra. */
        PLUGIN(60),

        /** Crear o modificar recursos: operaciones puntuales, no rutinarias. */
        ESCRITURA(20),

        /** Lectura cacheada: puede ser generosa sin riesgo. */
        LECTURA(120);

        private final int porMinuto;

        Categoria(int porMinuto) {
            this.porMinuto = porMinuto;
        }
    }

    /**
     * Los contadores caducan solos al minuto. Se acota el tamano para que un
     * atacante que rote millones de IPs no consuma toda la memoria: el propio
     * mecanismo de defensa seria entonces el vector de ataque.
     */
    /**
     * Tope de peticiones de plugin por IP y minuto, sumando todas las claves.
     *
     * <p>Muy por encima del uso legitimo —cien servidores en un hosting compartido
     * consultando dos veces por minuto son 200— y muy por debajo de lo que hace
     * falta para saturar nada.
     */
    private static final int TECHO_PLUGIN_POR_IP = 600;

    private final Cache<String, AtomicInteger> contadores = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(100_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {

        String ruta = peticion.getRequestURI();
        Categoria categoria = categoriaDe(ruta, peticion.getMethod());
        int limite = categoria.porMinuto;
        String clave = claveDe(peticion, ruta, categoria);

        AtomicInteger usos = contadores.get(clave, k -> new AtomicInteger());
        int actual = usos.incrementAndGet();

        // Techo por IP para el trafico de plugin.
        //
        // Sin esto, el limite por clave se saltaba entero: bastaba con mandar una
        // clave INVENTADA DISTINTA en cada peticion para estrenar un contador
        // nuevo cada vez. Cada una de esas peticiones llega hasta la consulta de
        // autenticacion en base de datos, asi que era una via directa para
        // saturar la API sin tener ninguna credencial.
        //
        // El techo es generoso a proposito: en un hosting compartido pueden salir
        // cien servidores legitimos por la misma direccion.
        if (categoria == Categoria.PLUGIN) {
            String claveIp = "plugin-ip:" + IpDelCliente.de(peticion);
            int desdeEsaIp = contadores.get(claveIp, k -> new AtomicInteger()).incrementAndGet();

            if (desdeEsaIp > TECHO_PLUGIN_POR_IP) {
                if (desdeEsaIp == TECHO_PLUGIN_POR_IP + 1) {
                    log.warn("Techo de trafico de plugin superado desde {}", claveIp);
                }
                rechazar(respuesta);
                return;
            }
        }

        if (actual > limite) {
            // Se registra solo al superar el limite por primera vez: si se
            // registrara cada peticion bloqueada, un ataque llenaria el disco de
            // logs, que es otra forma de tumbar el servicio.
            if (actual == limite + 1) {
                log.warn("Limite de trafico superado en {} para {}", ruta, clave);
            }
            respuesta.setStatus(429);
            respuesta.setHeader("Retry-After", "60");
            respuesta.setContentType("application/problem+json");
            respuesta.getWriter()
                    .write(
                            """
                            {"type":"https://latinosmc.com/errores/rate_limit",\
                            "title":"rate_limit",\
                            "status":429,\
                            "detail":"Demasiadas peticiones. Intentalo de nuevo en un minuto."}""");
            return;
        }

        respuesta.setHeader("X-RateLimit-Limit", String.valueOf(limite));
        respuesta.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limite - actual)));

        cadena.doFilter(peticion, respuesta);
    }

    private void rechazar(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(429);
        respuesta.setHeader("Retry-After", "60");
        respuesta.setContentType("application/problem+json");
        respuesta.getWriter()
                .write(
                        """
                        {"type":"https://latinosmc.com/errores/rate_limit",                        "title":"rate_limit",                        "status":429,                        "detail":"Demasiadas peticiones. Intentalo de nuevo en un minuto."}""");
    }

    private Categoria categoriaDe(String ruta, String metodo) {
        if (ruta.contains("/votes") || ruta.contains("/vote-tokens")) {
            return Categoria.VOTO;
        }
        if (ruta.startsWith("/api/v1/plugin")) {
            return Categoria.PLUGIN;
        }
        if (ruta.startsWith("/api/v1/auth/login") || ruta.startsWith("/api/v1/auth/registro")) {
            return Categoria.AUTENTICACION;
        }
        if ("POST".equals(metodo) || "PATCH".equals(metodo) || "DELETE".equals(metodo)) {
            return Categoria.ESCRITURA;
        }
        return Categoria.LECTURA;
    }

    /**
     * La categoria forma parte de la clave: cada tipo de trafico lleva su propio
     * contador y no se gastan el cupo entre ellos.
     *
     * <p>El plugin se limita por clave y no por IP: en un hosting compartido, cien
     * servidores distintos salen por la misma direccion y no deben castigarse
     * mutuamente.
     */
    private String claveDe(HttpServletRequest peticion, String ruta, Categoria categoria) {
        if (categoria == Categoria.PLUGIN) {
            String clave = peticion.getHeader("X-Api-Key");
            if (clave != null && !clave.isBlank()) {
                return "k:" + huella(clave);
            }
        }
        return "ip:" + IpDelCliente.de(peticion) + ":" + categoria.name();
    }

    /**
     * Huella de la clave para agrupar peticiones, nunca la clave en claro.
     *
     * <p>SHA-256 y no {@code String.hashCode()}. El hashCode de Java tiene
     * colisiones triviales de construir: con el, alguien podia fabricar una clave
     * inventada que cayera en el MISMO contador que un servidor legitimo y
     * agotarle el cupo desde fuera. El servidor honesto empezaria a recibir 429
     * sin haber hecho nada y sin forma de averiguar por que.
     *
     * <p>Se recorta a 128 bits: de sobra contra colisiones y ocupa la mitad en la
     * cache de contadores.
     */
    private String huella(String clave) {
        try {
            byte[] resumen = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(clave.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(resumen, 0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 deberia existir siempre", e);
        }
    }

    /** Health y metricas quedan fuera: las consulta el propio Docker cada 30 s. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return peticion.getRequestURI().startsWith("/actuator");
    }
}
