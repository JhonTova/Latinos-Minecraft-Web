package com.latinosmc.identity;

import com.latinosmc.shared.DomainException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hasheo de contrasenas con Argon2id.
 *
 * <p>Argon2id y no bcrypt para desarrollo nuevo: bcrypt se defiende bien de la
 * fuerza bruta en CPU, pero una GPU moderna lo ataca en paralelo con mucha
 * ventaja. Argon2id ademas consume memoria a proposito, y la memoria es
 * justamente lo caro de paralelizar en una GPU.
 *
 * <p>Contraste deliberado con {@code ApiKeys}, que usa SHA-256: alli el secreto
 * son 256 bits aleatorios que no se adivinan ni con todo el tiempo del universo,
 * asi que un hash lento solo anadiria latencia. Aqui el secreto lo elige un
 * humano y probablemente sea adivinable, asi que encarecer cada intento es
 * exactamente el objetivo.
 */
@Component
public class Passwords {

    /**
     * Parametros: 16 bytes de sal, 32 de hash, 1 hilo, 19 MiB de memoria,
     * 2 iteraciones. Son los valores recomendados por OWASP para Argon2id.
     */
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);

    private static final int LONGITUD_MINIMA = 10;

    /**
     * @throws DomainException.Conflict si la contrasena es demasiado corta
     */
    public String hash(String enClaro) {
        if (enClaro == null || enClaro.length() < LONGITUD_MINIMA) {
            // Longitud minima y no reglas de "una mayuscula, un simbolo": esas
            // reglas empujan a la gente hacia contrasenas predecibles del tipo
            // "Password1!" y a reutilizarlas. La longitud aporta mucha mas
            // entropia real.
            throw new DomainException.Conflict(
                    "password.too_short", "La contrasena debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
        }
        return encoder.encode(enClaro);
    }

    /**
     * Verifica una contrasena.
     *
     * <p>Devuelve false sin lanzar cuando el usuario no tiene contrasena (se
     * registro solo con Google o Discord). Distinguir ese caso hacia fuera
     * revelaria que la cuenta existe.
     */
    public boolean coincide(String enClaro, String hashGuardado) {
        if (hashGuardado == null || enClaro == null) {
            return false;
        }
        return encoder.matches(enClaro, hashGuardado);
    }
}
