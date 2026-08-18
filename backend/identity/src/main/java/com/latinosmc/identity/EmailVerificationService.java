package com.latinosmc.identity;

import com.latinosmc.shared.DomainException;
import com.latinosmc.shared.Ids;
import com.latinosmc.shared.ports.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verificacion de correo por codigo de 6 digitos.
 *
 * <p>Codigo y no enlace: el publico de esta plataforma abre el correo en el movil
 * y suele haberse registrado en el ordenador. Un enlace obliga a saltar de
 * dispositivo; un codigo se teclea donde ya estas.
 *
 * <p>Un codigo de 6 digitos son un millon de combinaciones. Suena poco, pero con
 * caducidad de 15 minutos, maximo 5 intentos y control de trafico por IP, la
 * probabilidad de acertar es despreciable. Lo que no puede faltar es ninguno de
 * los tres limites.
 */
@Service
public class EmailVerificationService {

    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final int MAX_INTENTOS = 5;

    /** Evita que alguien use el reenvio para bombardear de correo a un tercero. */
    private static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofSeconds(60);

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final VerificationRepository repositorio;
    private final EmailSender correo;
    private final Clock reloj;

    public EmailVerificationService(VerificationRepository repositorio, EmailSender correo, Clock reloj) {
        this.repositorio = repositorio;
        this.correo = correo;
        this.reloj = reloj;
    }

    /** Genera un codigo y lo envia. Invalida cualquier codigo anterior sin usar. */
    @Transactional
    public void enviarCodigo(UUID usuarioId, String email, String nombre) {
        Instant ahora = Instant.now(reloj);

        repositorio
                .ultimoEnvio(usuarioId)
                .filter(ultimo -> ultimo.plus(ESPERA_ENTRE_ENVIOS).isAfter(ahora))
                .ifPresent(ultimo -> {
                    throw new DomainException.Conflict(
                            "verification.too_soon",
                            "Espera un momento antes de pedir otro codigo. Revisa tu bandeja de spam.");
                });

        repositorio.invalidarAnteriores(usuarioId, ahora);

        String codigo = generarCodigo();
        repositorio.guardar(Ids.newId(), usuarioId, hash(codigo), email, ahora.plus(VIGENCIA));

        correo.enviar(
                email,
                "Tu codigo de LatinosMC: " + codigo,
                """
                Hola %s:

                Tu codigo de verificacion es:

                    %s

                Caduca en 15 minutos.

                Si no has creado ninguna cuenta en LatinosMC, ignora este mensaje:
                sin el codigo, nadie puede usar tu correo.
                """
                        .formatted(nombre, codigo),
                null);
    }

    /**
     * Comprueba el codigo y marca el correo como verificado.
     *
     * <p>Cada intento fallido cuenta. Agotados los cinco, el codigo muere y hay que
     * pedir otro: sin eso, un millon de combinaciones se prueban en un rato.
     */
    @Transactional
    public void verificar(UUID usuarioId, String codigo) {
        Instant ahora = Instant.now(reloj);

        var vigente = repositorio
                .codigoVigente(usuarioId, ahora)
                .orElseThrow(() -> new DomainException.Conflict(
                        "verification.expired", "El codigo ha caducado. Pide uno nuevo."));

        if (vigente.intentos() >= MAX_INTENTOS) {
            throw new DomainException.Conflict(
                    "verification.too_many_attempts", "Demasiados intentos fallidos. Pide un codigo nuevo.");
        }

        if (!vigente.codeHash().equals(hash(normalizar(codigo)))) {
            repositorio.sumarIntento(vigente.id());
            throw new DomainException.Conflict("verification.invalid_code", "El codigo no es correcto.");
        }

        repositorio.marcarConsumido(vigente.id(), ahora);
        repositorio.marcarEmailVerificado(usuarioId, vigente.email(), ahora);
    }

    /**
     * Marca el correo como verificado sin pedir codigo.
     *
     * <p>Se usa al entrar con Google o Discord: el proveedor ya verifico esa
     * direccion, y pedir un segundo codigo seria exigir al usuario que demuestre
     * algo ya demostrado.
     */
    @Transactional
    public void marcarVerificadoPorProveedor(UUID usuarioId, String email) {
        repositorio.marcarEmailVerificado(usuarioId, email, Instant.now(reloj));
    }

    /** Codigo de 6 digitos, con ceros a la izquierda si hace falta. */
    private String generarCodigo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }

    /** Se quitan espacios y guiones: la gente los pega tal cual desde el correo. */
    private String normalizar(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[\\s-]", "");
    }

    private String hash(String codigo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
