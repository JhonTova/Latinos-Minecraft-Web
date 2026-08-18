package com.latinosmc.voting;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.identity.PlayerIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tokens de voto de un solo uso (ADR-0016).
 *
 * <p>Resuelve el problema de fondo: sin token, el jugador escribe su nick en la
 * web y cualquiera puede votar en nombre de cualquiera sin haber pisado nunca
 * ese servidor. Con token, la identidad queda probada por construccion, porque
 * para obtener uno hay que estar dentro del servidor.
 *
 * <p>Efecto secundario agradable: la web deja de pedir el nick, asi que la medida
 * de seguridad <em>quita</em> un paso del flujo en vez de anadirlo.
 */
@Service
public class VoteTokenService {

    /**
     * Suficiente para hacer el alt-tab sin prisa; corto si alguien intercepta el
     * enlace. Con el codigo corto tambien cuenta el tiempo de teclearlo, que en
     * un movil no es despreciable.
     */
    private static final Duration VIGENCIA = Duration.ofMinutes(10);


    private final VoteTokenRepository repositorio;
    private final Clock reloj;

    public VoteTokenService(VoteTokenRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    /**
     * Emite un token para un jugador de un servidor.
     *
     * <p>Antes caduca cualquier token anterior sin usar del mismo jugador. Dos
     * motivos: evita que ejecutar {@code /vote} en bucle acumule tokens muertos, y
     * garantiza que solo hay un enlace valido a la vez, asi que uno reenviado por
     * error deja de funcionar en cuanto el jugador pide otro.
     *
     * <p>No se reutiliza el token anterior porque no se puede: en la base solo
     * vive su hash. Esa es exactamente la propiedad que se busca.
     *
     * @return el token en claro; es la unica vez que existe fuera de la memoria
     *     del proceso
     */
    @Transactional
    public TokenEmitido emitir(UUID servidorId, PlayerIdentity identidad) {
        Instant ahora = Instant.now(reloj);

        repositorio.caducarAnteriores(servidorId, identidad.value(), ahora);

        // El codigo se guarda agrupado para enseñarlo, pero se hashea normalizado:
        // asi da igual si el jugador escribe los guiones o no.
        String enClaro = CodigoDeVoto.generar();
        repositorio.guardar(
                Ids.newId(),
                servidorId,
                hash(CodigoDeVoto.normalizar(enClaro)),
                identidad.type().name(),
                identidad.value(),
                ahora.plus(VIGENCIA));

        return new TokenEmitido(enClaro, VIGENCIA);
    }

    /**
     * Resuelve un token sin consumirlo. Lo usa la web para saber a quien va a
     * votar antes de que pulse el boton.
     */
    @Transactional(readOnly = true)
    public DatosDelToken resolver(String enClaro) {
        return repositorio
                .buscarVigente(hash(CodigoDeVoto.normalizar(enClaro)), Instant.now(reloj))
                .orElseThrow(() -> new DomainException.NotFound(
                        "vote.token.invalid", "Este enlace ya no es valido. Vuelve a escribir /vote en el servidor."));
    }

    /**
     * Voto ya emitido con este token, si se uso antes.
     *
     * <p>Permite que un doble clic muestre "ya votaste" en lugar de "enlace no
     * valido": para el jugador son mensajes muy distintos, y el segundo parece un
     * error del sitio cuando en realidad su voto se registro perfectamente.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Vote> votoYaEmitido(String enClaro) {
        return repositorio.votoDeTokenConsumido(hash(CodigoDeVoto.normalizar(enClaro)));
    }

    /**
     * Marca el token como usado.
     *
     * <p>Devuelve {@code false} si otra peticion lo consumio primero: la condicion
     * viaja en el propio UPDATE, asi que dos pulsaciones simultaneas del boton no
     * pueden producir dos votos.
     */
    @Transactional
    public boolean consumir(String enClaro, UUID votoId) {
        return repositorio.consumir(hash(CodigoDeVoto.normalizar(enClaro)), votoId, Instant.now(reloj)) > 0;
    }



    /**
     * SHA-256 y no Argon2: el token son 192 bits aleatorios, no una contrasena
     * humana. No hay nada que adivinar por fuerza bruta, y un hash lento solo
     * anadiria latencia a cada apertura del enlace.
     */
    private String hash(String enClaro) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(enClaro.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * @param token valor en claro, para incluir en el enlace
     * @param vigencia cuanto dura
     */
    public record TokenEmitido(String token, Duration vigencia) {}

    /**
     * @param servidorSlug servidor al que se va a votar
     * @param identidadTipo tipo de identidad ya resuelta
     * @param identidadValor nick del jugador; la web lo muestra, no lo pregunta
     */
    public record DatosDelToken(UUID servidorId, String servidorSlug, String servidorNombre, String identidadTipo, String identidadValor) {}
}
