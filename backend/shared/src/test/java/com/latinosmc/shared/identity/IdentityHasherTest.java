package com.latinosmc.shared.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentityHasherTest {

    private static final String PEPPER = "un-pepper-de-pruebas-suficientemente-largo-para-pasar";

    private final IdentityHasher hasher = new IdentityHasher(PEPPER);

    @Test
    @DisplayName("el mismo jugador produce siempre el mismo hash")
    void hashEstable() {
        var identidad = PlayerIdentity.offlineName("Steve");
        assertThat(hasher.hashIdentidad(identidad)).isEqualTo(hasher.hashIdentidad(identidad));
    }

    /**
     * Sin esto, alternar mayusculas seria la forma mas barata de saltarse la
     * ventana de voto: "Steve", "steve" y "STEVE" contarian como tres jugadores.
     */
    @Test
    @DisplayName("las mayusculas no crean identidades distintas")
    void insensibleAMayusculas() {
        assertThat(hasher.hashIdentidad(PlayerIdentity.offlineName("Steve")))
                .isEqualTo(hasher.hashIdentidad(PlayerIdentity.offlineName("STEVE")))
                .isEqualTo(hasher.hashIdentidad(PlayerIdentity.offlineName("steve")));
    }

    @Test
    void jugadoresDistintosProducenHashesDistintos() {
        assertThat(hasher.hashIdentidad(PlayerIdentity.offlineName("Steve")))
                .isNotEqualTo(hasher.hashIdentidad(PlayerIdentity.offlineName("Alex")));
    }

    /**
     * Un UUID de Mojang y un nombre offline nunca deben colisionar: son niveles de
     * confianza distintos y confundirlos permitiria suplantar a un jugador
     * verificado desde un servidor pirata.
     */
    @Test
    @DisplayName("el tipo de identidad forma parte del hash")
    void tiposDistintosNoColisionan() {
        var comoNombre = hasher.hashIdentidad(PlayerIdentity.offlineName("Steve"));
        var comoUuid = hasher.hashIdentidad(PlayerIdentity.mojangUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertThat(comoNombre).isNotEqualTo(comoUuid);
    }

    /**
     * El pepper es lo que impide revertir los hashes con una lista de nombres
     * conocidos. Con otra clave, los mismos datos dan otro resultado.
     */
    @Test
    @DisplayName("el pepper cambia el resultado")
    void pepperDistintoCambiaElHash() {
        var otro = new IdentityHasher("otro-pepper-distinto-igualmente-largo-de-sobra");
        assertThat(hasher.hashIdentidad(PlayerIdentity.offlineName("Steve")))
                .isNotEqualTo(otro.hashIdentidad(PlayerIdentity.offlineName("Steve")));
    }

    @Test
    void hashDeIpNuloCuandoNoHayIp() {
        assertThat(hasher.hashIp(null)).isNull();
        assertThat(hasher.hashIp("  ")).isNull();
    }

    @Test
    @DisplayName("rechaza un pepper corto en el arranque")
    void exigePepperLargo() {
        assertThatThrownBy(() -> new IdentityHasher("corto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 caracteres");
    }
}
