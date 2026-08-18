package com.latinosmc.voting;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Codigo de voto: 8 caracteres que un jugador puede escribir a mano.
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
 * <p><strong>Por que 8 y no 12.</strong> Doce caracteres son tres grupos, y tres
 * grupos no caben de una vez en la memoria de trabajo de nadie: el jugador de
 * consola acaba mirando la pantalla, escribiendo cuatro, volviendo a mirar. Ocho
 * son dos grupos, que es justo lo que se retiene de una lectura. Es un tercio
 * menos de tecleo y, sobre todo, una mirada menos (ADR-0026).
 *
 * <p><strong>Entropia.</strong> 8 caracteres sobre 32 simbolos son 40 bits: un
 * billon largo de combinaciones. Lo que protege el codigo no es la longitud sino
 * su vida: 10 minutos, un solo uso y 10 intentos por minuto y por IP. Con diez
 * mil codigos vivos a la vez —muy por encima de lo realista— una botnet de mil
 * direcciones tardaria mas de una semana en acertar uno, y el premio serian los
 * votos de un dia de un jugador. El factor limitante nunca fue la longitud.
 */
public final class CodigoDeVoto {

    private static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final int LARGO = 8;
    private static final int TAMANO_GRUPO = 4;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoDeVoto() {}

    /** @return un codigo nuevo, ya agrupado: {@code K7M2-9QX4} */
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

    /** Agrupa un codigo canonico para enseñarlo: {@code K7M29QX4} → {@code K7M2-9QX4}. */
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
