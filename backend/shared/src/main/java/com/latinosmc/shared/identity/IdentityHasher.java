package com.latinosmc.shared.identity;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Calcula el hash estable de una identidad y de una direccion IP.
 *
 * <p>El hash es lo que se usa para la ventana de voto y el antifraude. Guardarlo
 * separado del valor en claro es lo que hace posible cumplir el derecho de
 * supresion (ADR-0010): al anonimizar se borra el nombre del jugador pero se
 * conserva el hash, asi que se siguen detectando duplicados historicos sin
 * conservar el dato personal.
 *
 * <p>Se usa HMAC-SHA256 con una clave secreta ("pepper") en lugar de SHA-256 a
 * secas. Motivo concreto: el espacio de nombres de Minecraft es pequeno y
 * publico. Un SHA-256 sin clave permitiria a cualquiera con una copia de la base
 * de datos revertir los hashes probando la lista de nombres conocidos. Con una
 * clave que no esta en la base de datos, esa tabla arcoiris no sirve.
 *
 * <p>La clave NO rota: el hash debe ser estable en el tiempo o se perderia la
 * capacidad de reconocer al mismo votante. Por eso vive en la configuracion de
 * secretos, nunca en el repositorio.
 */
public final class IdentityHasher {

    private static final String ALGORITMO = "HmacSHA256";

    private final SecretKeySpec clave;

    public IdentityHasher(String pepper) {
        if (pepper == null || pepper.length() < 32) {
            throw new IllegalArgumentException(
                    "El pepper de identidad debe tener al menos 32 caracteres. "
                            + "Configura LATINOSMC_IDENTITY_PEPPER con un valor aleatorio y guardalo: "
                            + "si cambia, se pierde la continuidad de todos los hashes existentes.");
        }
        this.clave = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), ALGORITMO);
    }

    /**
     * Hash de la identidad del votante. El tipo forma parte de la entrada para que
     * el UUID de Mojang de un jugador y su nombre offline nunca colisionen.
     */
    public byte[] hashIdentidad(PlayerIdentity identidad) {
        return hmac(identidad.type().name() + ":" + normalizar(identidad.value()));
    }

    /** Hash de una IP. Retencion 90 dias; nunca se guarda en claro (ADR-0010). */
    public byte[] hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        return hmac("ip:" + ip.trim());
    }

    /**
     * En Minecraft los nombres no distinguen mayusculas a efectos practicos:
     * "Steve" y "steve" son el mismo jugador para el dueno del servidor, y deben
     * serlo tambien para la ventana de voto. Sin esto, alternar mayusculas seria
     * una forma trivial de votar varias veces.
     */
    private String normalizar(String valor) {
        return valor.toLowerCase();
    }

    private byte[] hmac(String entrada) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            return mac.doFinal(entrada.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            // HmacSHA256 esta garantizado por la plataforma Java: si falla aqui,
            // el entorno esta roto y no tiene sentido continuar.
            throw new IllegalStateException("No se pudo calcular el HMAC", e);
        }
    }
}
