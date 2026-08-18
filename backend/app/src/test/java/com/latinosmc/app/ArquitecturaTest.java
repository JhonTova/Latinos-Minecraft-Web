package com.latinosmc.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica en CI las reglas del ADR-0001 (monolito modular).
 *
 * <p>Estas reglas son la diferencia entre "monolito modular" y "monolito a
 * secas". Sin una comprobacion automatica se erosionan solas: basta un import
 * puesto con prisa un martes por la tarde. Un humano no las mantiene; un test
 * si.
 */
class ArquitecturaTest {

    private static final String RAIZ = "com.latinosmc";

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);

    /**
     * Los modulos de dominio no se conocen entre si. Se comunican por eventos o
     * por interfaces publicas declaradas en 'shared'.
     *
     * <p>Sin esta regla, 'voting' acaba llamando a 'trust' directamente, luego
     * 'trust' necesita algo de 'servers', y en seis meses ya no hay modulos:
     * hay un solo bloque imposible de separar.
     */
    @Test
    @DisplayName("Los modulos de dominio no dependen unos de otros")
    void modulosDeDominioSonIndependientes() {
        String[] dominio = {
            "identity", "organizations", "servers", "voting", "trust", "ranking", "delivery", "billing"
        };

        for (String origen : dominio) {
            for (String destino : dominio) {
                if (origen.equals(destino)) {
                    continue;
                }
                noClasses()
                        .that()
                        .resideInAPackage(RAIZ + "." + origen + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage(RAIZ + "." + destino + "..")
                        .because("los modulos de dominio se comunican por eventos o por 'shared', "
                                + "nunca directamente (ADR-0001)")
                        // Los modulos aun estan vacios. ArchUnit falla por defecto cuando una
                        // regla no evalua ninguna clase, para que nadie confie en una
                        // comprobacion que en realidad no comprueba nada. Aqui el vacio es
                        // legitimo y temporal: se permite explicitamente.
                        .allowEmptyShould(true)
                        .check(CLASES);
            }
        }
    }

    /**
     * 'app' es el ensamblador: conoce a todos los modulos. Nadie conoce a 'app'.
     * Si un modulo de dominio dependiera de 'app', dejaria de ser extraible.
     */
    @Test
    @DisplayName("Ningun modulo depende del ensamblador 'app'")
    void nadieDependeDeApp() {
        noClasses()
                .that()
                .resideOutsideOfPackage(RAIZ + ".app..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(RAIZ + ".app..")
                .because("'app' ensambla los modulos; ningun modulo debe conocerlo (ADR-0001)")
                .check(CLASES);
    }

    /**
     * 'shared' es el nucleo comun: tipos base y contratos. Si 'shared' empezara
     * a depender de un modulo concreto, se convertiria en el "Core" gigante que
     * el ADR-0001 descarta explicitamente.
     */
    @Test
    @DisplayName("'shared' no depende de ningun modulo de dominio")
    void sharedNoDependeDeNadie() {
        noClasses()
                .that()
                .resideInAPackage(RAIZ + ".shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        RAIZ + ".identity..",
                        RAIZ + ".organizations..",
                        RAIZ + ".servers..",
                        RAIZ + ".voting..",
                        RAIZ + ".trust..",
                        RAIZ + ".ranking..",
                        RAIZ + ".delivery..",
                        RAIZ + ".billing..",
                        RAIZ + ".app..")
                .because("'shared' contiene tipos base y contratos; si acumula dependencias "
                        + "se convierte en el 'Core' gigante que descartamos (ADR-0001)")
                .check(CLASES);
    }

    /** Una dependencia circular entre modulos hace imposible extraer cualquiera de ellos. */
    @Test
    @DisplayName("No existen ciclos de dependencia entre modulos")
    void sinCiclos() {
        SlicesRuleDefinition.slices()
                .matching(RAIZ + ".(*)..")
                .should()
                .beFreeOfCycles()
                .check(CLASES);
    }
}
