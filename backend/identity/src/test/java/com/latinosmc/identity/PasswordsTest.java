package com.latinosmc.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.latinosmc.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Hasheo de contrasenas.
 *
 * <p>Un fallo aqui no se nota: la aplicacion sigue dejando entrar a quien pone
 * bien su contrasena. Solo se descubre el dia que alguien se lleva la tabla de
 * usuarios, y entonces ya no hay nada que hacer.
 */
class PasswordsTest {

    private final Passwords passwords = new Passwords();

    /**
     * Longitud minima y no reglas de mayusculas y simbolos: esas reglas empujan
     * a "Password1!" y a reutilizarla en todas partes. La longitud aporta mas
     * entropia real y molesta menos.
     */
    @Test
    @DisplayName("una contrasena de menos de diez caracteres se rechaza")
    void demasiadoCorta() {
        assertThatThrownBy(() -> passwords.hash("123456789"))
                .isInstanceOf(DomainException.Conflict.class)
                .hasFieldOrPropertyWithValue("code", "password.too_short");
    }

    @Test
    @DisplayName("una contrasena nula se rechaza igual, sin reventar")
    void nula() {
        assertThatThrownBy(() -> passwords.hash(null))
                .isInstanceOf(DomainException.Conflict.class)
                .hasFieldOrPropertyWithValue("code", "password.too_short");
    }

    @Test
    @DisplayName("con diez caracteres justos ya vale")
    void longitudJusta() {
        assertThatCode(() -> passwords.hash("1234567890")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el hash es Argon2id y no contiene la contrasena")
    void formatoDelHash() {
        String hash = passwords.hash("una-contrasena-larga");

        assertThat(hash).startsWith("$argon2id$").doesNotContain("una-contrasena-larga");
    }

    /**
     * Dos cuentas con la misma contrasena tienen que dar hashes distintos. Si no,
     * quien mirase la tabla sabria de un vistazo que usuarios comparten
     * contrasena, y romper una las rompe todas.
     */
    @Test
    @DisplayName("la misma contrasena da hashes distintos cada vez")
    void salAleatoria() {
        assertThat(passwords.hash("una-contrasena-larga")).isNotEqualTo(passwords.hash("una-contrasena-larga"));
    }

    @Test
    @DisplayName("la contrasena correcta coincide y la incorrecta no")
    void verificacion() {
        String hash = passwords.hash("una-contrasena-larga");

        assertThat(passwords.coincide("una-contrasena-larga", hash)).isTrue();
        assertThat(passwords.coincide("una-contrasena-largo", hash)).isFalse();
        assertThat(passwords.coincide("", hash)).isFalse();
    }

    /**
     * Quien se registro solo con Google no tiene contrasena guardada. Ese caso
     * devuelve false; lanzar una excepcion distinta revelaria hacia fuera que la
     * cuenta existe, que es justo lo que el login evita decir.
     */
    @Test
    @DisplayName("sin contrasena guardada no coincide nada, y no lanza")
    void cuentaSinPassword() {
        assertThat(passwords.coincide("loquesea", null)).isFalse();
        assertThat(passwords.coincide(null, passwords.hash("una-contrasena-larga"))).isFalse();
    }
}
