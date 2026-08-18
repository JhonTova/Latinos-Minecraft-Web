package com.latinosmc.app.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Averigua la IP real de quien hace la peticion.
 *
 * <p>Estaba repetida en cinco controladores. Ademas de duplicacion, era un riesgo
 * real: el dia que haya que cambiar como se detecta la IP —justo lo que pasa al
 * poner Cloudflare delante— habria que acordarse de los cinco sitios, y bastaria
 * olvidar uno para que el antifraude viera a todos los votantes como la misma
 * persona.
 *
 * <p><strong>Se lee UNA sola cabecera, y es a proposito.</strong> Antes se
 * probaban tres en orden ({@code CF-Connecting-IP}, {@code X-Forwarded-For},
 * {@code X-Real-IP}) quedandose con la primera que viniera. Esa cadena de
 * alternativas <em>era</em> el agujero: ninguna cabecera es una prueba de nada
 * —las escribe quien envia la peticion— y lo unico que las hace fiables es que
 * <em>nuestro</em> proxy las sobrescriba. Con tres opciones, a quien quisiera
 * falsificar su IP le bastaba con elegir la que el proxy no estuviera limpiando.
 *
 * <p>Y no era teorico: el proxy documentado sobrescribia {@code X-Forwarded-For}
 * pero no tocaba {@code CF-Connecting-IP}, que era justo la que se miraba
 * primero. Bastaba con enviarla a mano —saltandose Cloudflare, directo a la IP
 * del servidor— para aparecer como un visitante distinto en cada voto, y con eso
 * el cupo por IP y las reglas de antifraude por IP dejaban de existir.
 *
 * <p>Ahora se lee {@code X-Forwarded-For} y nada mas. El proxy DEBE reescribirla
 * con la IP real del visitante (la configuracion exacta esta en
 * {@code LANZAMIENTO.md} § 7). Si no lo hace, lo que llegue aqui no vale nada, y
 * es mejor que eso se decida en un solo sitio que se puede revisar.
 *
 * <p>Aun asi ninguna de estas señales es una prueba. Por eso la reputacion de IP
 * es apoyo del antifraude y nunca su defensa principal (ADR-0005).
 */
public final class IpDelCliente {

    /**
     * La unica cabecera que se cree.
     *
     * <p>Si algun dia hay que cambiarla, se cambia aqui y en la configuracion del
     * proxy — en ese orden, y nunca solo en uno de los dos sitios.
     */
    static final String CABECERA = "X-Forwarded-For";

    private IpDelCliente() {}

    public static String de(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader(CABECERA);

        if (esUtil(reenviada)) {
            // Con el proxy bien configurado hay una sola entrada. Si hay varias
            // es que alguien mando la suya y el proxy la conservo: se toma la
            // primera igualmente, porque cambiar de criterio segun el numero de
            // entradas seria una regla que nadie recuerda al leer un log.
            String primera = reenviada.split(",")[0].trim();
            if (esUtil(primera)) {
                return primera;
            }
        }

        // Sin proxy delante —desarrollo local, o una peticion que llego directa—
        // la direccion del socket es la verdad y ademas no se puede falsificar.
        return peticion.getRemoteAddr();
    }

    /**
     * Algunos proxies mal configurados envian la cabecera con el texto literal
     * "unknown" en lugar de omitirla. Tratarlo como una IP agruparia bajo el mismo
     * valor a visitantes que no tienen nada que ver.
     */
    private static boolean esUtil(String valor) {
        return valor != null && !valor.isBlank() && !"unknown".equalsIgnoreCase(valor.trim());
    }
}
