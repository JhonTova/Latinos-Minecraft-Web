package com.latinosmc.servers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generacion y verificacion de claves de API del plugin.
 *
 * <p>La clave se muestra <strong>una sola vez</strong> al crearla. En la base de
 * datos solo vive su hash: si la base se filtra, las claves no son utilizables.
 *
 * <p>Se usa SHA-256 y no Argon2, a diferencia de las contrasenas. El motivo es la
 * entropia: una contrasena humana es adivinable y hay que encarecer cada intento;
 * una clave de 256 bits aleatorios no se adivina por fuerza bruta ni con todo el
 * tiempo del universo, asi que un hash lento solo anadiria latencia a cada
 * peticion del plugin sin aportar seguridad.
 */
public final class ApiKeys {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String PREFIJO = "lmc_";
    private static final int BYTES_ENTROPIA = 32;

    private ApiKeys() {}

    /** Clave nueva en claro. Quien la reciba debe guardarla: no se puede recuperar. */
    public static String generar() {
        byte[] bytes = new byte[BYTES_ENTROPIA];
        ALEATORIO.nextBytes(bytes);
        return PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String claveEnClaro) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(claveEnClaro.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * Fragmento visible para que el usuario reconozca cual es cual en el panel sin
     * exponer la clave. Ejemplo: {@code lmc_A7x2...}
     */
    public static String prefijoVisible(String claveEnClaro) {
        int fin = Math.min(claveEnClaro.length(), PREFIJO.length() + 4);
        return claveEnClaro.substring(0, fin) + "...";
    }
}
