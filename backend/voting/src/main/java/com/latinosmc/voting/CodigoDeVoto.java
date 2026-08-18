package com.latinosmc.voting;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Codigo de voto: 12 caracteres que un jugador puede escribir a mano.
 *
 * <p><strong>Por que no un token largo en base64.</strong> Los jugadores de
 * Bedrock —moviles y consolas, buena parte del publico hispanohablante— no
 * pueden pulsar enlaces en el chat de Minecraft. Con un token de 32 caracteres
 * aleatorios no tenian forma de votar: ni pulsando ni copiando a mano. El codigo
 * corto es lo que les abre la puerta, y al de PC no le quita nada porque sigue
 * teniendo su enlace.
 *
 * <p><strong>El alfabeto.</strong> 32 simbolos sin {@code I}, {@code L},
 * {@code O} ni {@code U}. Las tres primeras se confunden con unos y ceros al
 * leerlas de un chat; la {@code U} se quita para que no salgan palabras
 * reconocibles por accidente. Lo que se lee mal, se escribe mal, y quien lo
 * escribe mal cree que el sitio no funciona.
 *
 * <p><strong>Entropia.</strong> 12 caracteres sobre 32 simbolos son 60 bits. Con
 * 10 minutos de vigencia, un solo uso y el limite de trafico por IP, un atacante
 * tendria que acertar entre un trillon de combinaciones a diez intentos por
 * minuto. No es un secreto que dure eternamente, y no necesita serlo.
 */
public final class CodigoDeVoto {

    private static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LARGO = 12;
    private static final int TAMANO_GRUPO = 4;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoDeVoto() {}

    /** @return un codigo nuevo, ya agrupado: {@code K7M2-9QX4-RT3F} */
    public static String generar() {
        StringBuilder sb = new StringBuilder(LARGO + 2);

        for (int i = 0; i < LARGO; i++) {
            if (i > 0 && i % TAMANO_GRUPO == 0) {
                sb.append('-');
            }
            // nextInt y no un modulo sobre bytes aleatorios: el modulo sesga los
            // primeros simbolos del alfabeto en cuanto su tamano deja de ser
            // potencia de dos, y eso pasaria sin que nadie lo notara el dia que
            // alguien anada o quite un caracter.
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /**
     * Normaliza lo que escribe el jugador.
     *
     * <p>Se es generoso con la entrada y estricto con la salida, a proposito:
     * quien copia de un chat puede traerse espacios, escribir en minusculas,
     * olvidar los guiones o teclear una ele donde habia un uno. Ninguna de esas
     * cosas es culpa del jugador: son consecuencia de haber elegido un codigo que
     * se escribe a mano, y absorberlas es parte del trato.
     *
     * @return el codigo canonico sin guiones, o lo que se haya podido rescatar
     */
    public static String normalizar(String escrito) {
        if (escrito == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(LARGO);

        for (char c : escrito.trim().toUpperCase(Locale.ROOT).toCharArray()) {
            char equivalente = switch (c) {
                // Las que se excluyeron del alfabeto: si alguien las escribe es
                // porque leyo mal el simbolo parecido.
                case 'I', 'L' -> '1';
                case 'O' -> '0';
                case 'U' -> 'V';
                default -> c;
            };

            if (ALFABETO.indexOf(equivalente) >= 0) {
                sb.append(equivalente);
            }
            // Lo demas —guiones, espacios, lo que sea— se descarta en silencio en
            // lugar de rechazar el codigo entero.

            if (sb.length() == LARGO) {
                break;
            }
        }
        return sb.toString();
    }

    /** Si tiene la forma correcta. No dice nada sobre si existe. */
    public static boolean tieneForma(String normalizado) {
        return normalizado != null && normalizado.length() == LARGO;
    }

    /** Agrupa un codigo canonico para enseñarlo: {@code K7M29QX4RT3F} → {@code K7M2-9QX4-RT3F}. */
    public static String agrupar(String normalizado) {
        if (!tieneForma(normalizado)) {
            return normalizado;
        }
        StringBuilder sb = new StringBuilder(LARGO + 2);
        for (int i = 0; i < LARGO; i++) {
            if (i > 0 && i % TAMANO_GRUPO == 0) {
                sb.append('-');
            }
            sb.append(normalizado.charAt(i));
        }
        return sb.toString();
    }
}
