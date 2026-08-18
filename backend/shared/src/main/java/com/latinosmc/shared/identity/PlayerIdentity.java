package com.latinosmc.shared.identity;

import com.latinosmc.shared.DomainException;
import java.util.regex.Pattern;

/**
 * Identidad de un votante, modelada como (tipo, valor).
 *
 * <p>No se asume que sea un UUID de Mojang. La decision sobre si se aceptan
 * servidores en offline-mode sigue abierta (DECISIONES.md 4.1), y en offline-mode
 * la identidad es texto arbitrario sin ninguna garantia. Modelarlo asi permite
 * soportar ambos con reglas antifraude distintas sin migrar el esquema.
 *
 * <p><strong>Seguridad:</strong> {@code value} es entrada no confiable. Se valida
 * aqui contra la gramatica de su tipo y NUNCA se concatena para formar un comando
 * de Minecraft (ADR-0012).
 *
 * @param type tipo de identidad
 * @param value valor ya validado
 */
public record PlayerIdentity(IdentityType type, String value) {

    /** Nombres de Minecraft: 3-16 caracteres, letras ASCII, digitos y guion bajo. */
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final Pattern UUID_VALIDO =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public PlayerIdentity {
        if (value == null || value.isBlank()) {
            throw new DomainException.Conflict("identity.value.missing", "La identidad del votante es obligatoria.");
        }
        if (!type.esValida(value)) {
            throw new DomainException.Conflict(
                    "identity.value.invalid", "La identidad del votante no tiene un formato valido.");
        }
    }

    public static PlayerIdentity mojangUuid(String uuid) {
        return new PlayerIdentity(IdentityType.MOJANG_UUID, uuid.toLowerCase());
    }

    public static PlayerIdentity offlineName(String nombre) {
        return new PlayerIdentity(IdentityType.OFFLINE_NAME, nombre);
    }

    public enum IdentityType {
        /** Verificado contra Mojang. Confianza alta. */
        MOJANG_UUID(true) {
            @Override
            boolean esValida(String valor) {
                return UUID_VALIDO.matcher(valor).matches();
            }
        },

        /**
         * Servidor en offline-mode: el nombre no lo respalda nadie. Confianza nula.
         *
         * <p>Se valida igualmente contra la gramatica de nombres de Minecraft. No
         * porque impida el fraude (no lo impide), sino porque cierra el vector de
         * inyeccion: un "jugador" llamado {@code "Steve; op Steve"} no llega
         * siquiera a construirse.
         */
        OFFLINE_NAME(false) {
            @Override
            boolean esValida(String valor) {
                return NOMBRE_VALIDO.matcher(valor).matches();
            }
        };

        private final boolean verificable;

        IdentityType(boolean verificable) {
            this.verificable = verificable;
        }

        abstract boolean esValida(String valor);

        /** Si un tercero de confianza respalda esta identidad. */
        public boolean esVerificable() {
            return verificable;
        }
    }
}
