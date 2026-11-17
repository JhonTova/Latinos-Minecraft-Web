/**
 * Las dos decisiones del puente `/api/backend` que no se ven en pantalla: qué
 * rutas del backend se pueden alcanzar desde el navegador y qué IP se le
 * reenvía.
 *
 * Están aquí, y no dentro de `route.ts`, para poder probarlas sin levantar Next
 * (ADR-0031). Son funciones puras: no leen la petición ni el entorno.
 */

/**
 * La lista blanca es lo que impide que el puente sea un proxy abierto: sin ella,
 * cualquiera podría alcanzar *cualquier* ruta interna a través de la web.
 *
 * Una ruta nueva entra con la expresión más estrecha posible y con su caso en
 * `puente.test.ts`.
 */
const RUTAS_PERMITIDAS: RegExp[] = [
  /^auth\/(registro|login|logout|yo|verificar|reenviar-codigo|sesiones)$/,
  // Acceso con Google y Discord. Pasa por aquí, y no directamente al backend,
  // porque la cookie de sesión se guarda para el dominio que responde a la vuelta
  // del proveedor: si esa vuelta aterrizara en la API, la sesión quedaría en otro
  // dominio y el usuario volvería a la web sin haber entrado.
  /^auth\/oauth\/(google|discord)$/,
  /^auth\/oauth\/(google|discord)\/callback$/,
  /^servers$/,
  /^servers\/(mios|mis-organizaciones|modalidades)$/,
  // Ficha y subida de imágenes de un servidor concreto. El UUID se acota en el
  // patrón: sin eso, este comodín abriría la puerta a cualquier subruta.
  /^servers\/[0-9a-f-]{36}$/i,
  /^servers\/[0-9a-f-]{36}\/imagenes\/(icono|banner)$/i,
  // Elegir entre la imagen automática y la subida, y pedir una lectura del ping
  // en el momento (ADR-0027).
  /^servers\/[0-9a-f-]{36}\/imagenes\/(icono|banner)\/origen$/i,
  /^servers\/[0-9a-f-]{36}\/imagenes\/automaticas\/actualizar$/i,
  // Compra de espacios destacados. `confirmar` es el paso de captura que PayPal
  // exige cuando el usuario vuelve de pagar.
  /^pagos\/(slots|pedidos|confirmar)$/,
  // Voto desde la web escribiendo el nick. El límite de tráfico y el antifraude
  // viven en el backend; esto solo abre la ruta.
  /^servers\/[a-z0-9-]{1,64}\/votes$/,
  // Administración de la plataforma. Que la ruta esté permitida aquí no da
  // acceso a nada: el backend comprueba el permiso en cada endpoint. Esta lista
  // solo decide qué se puede alcanzar, nunca quién.
  /^admin\/(soy|slots|pedidos|ingresos|administradores|patrocinadores|voto|sitio)$/,
  /^admin\/sitio\/(imagen|logo)$/,
  // Seis espacios desde V24. El rango va escrito y no como `\d+` a propósito:
  // así una ruta inventada no llega siquiera al backend.
  /^admin\/slots\/[1-6]$/,
  /^admin\/patrocinadores\/[0-9a-f-]{36}\/(aprobar|retirar|banner)$/i,
  // Retirada de imágenes automáticas. Es la mitad que hace aceptable adoptar
  // contenido de terceros al instante (ADR-0027): sin una forma de quitarlo, la
  // política elegida no existe.
  /^admin\/servidores\/imagenes-automaticas$/,
  /^admin\/servidores\/[0-9a-f-]{36}\/imagenes-automaticas\/(bloquear|desbloquear)$/i,
  // Moderación de servidores (ADR-0028). Que la ruta esté aquí no da acceso a
  // nada: el backend comprueba el permiso en cada endpoint.
  /^admin\/servidores$/,
  /^admin\/servidores\/[0-9a-f-]{36}\/(suspender|levantar-suspension)$/i,
];

/** Si `camino` (sin la barra inicial ni `/api/backend/`) puede ir al backend. */
export function rutaPermitida(camino: string): boolean {
  return RUTAS_PERMITIDAS.some((patron) => patron.test(camino));
}

/**
 * IP real del visitante, a partir de la cabecera `X-Forwarded-For`.
 *
 * **Se lee una sola cabecera**, la misma que el backend, y por el mismo motivo.
 * Antes se probaban tres en orden quedándose con la primera que viniera, y esa
 * cadena de alternativas era el agujero: ninguna cabecera prueba nada —las
 * escribe quien envía la petición— y lo único que las hace fiables es que
 * nuestro proxy las reescriba. Con tres opciones, a quien quisiera falsificar su
 * IP le bastaba con elegir la que el proxy no estuviera limpiando.
 *
 * El proxy de delante DEBE reescribir `X-Forwarded-For` con la IP real
 * (`LANZAMIENTO.md` § 7). Si no lo hace, nada de lo que llegue aquí vale.
 *
 * Esto no es un detalle: la IP decide si un voto se acepta. Si aquí llegara
 * siempre la misma, todos los votantes parecerían la misma persona y solo
 * votaría uno al día en todo el sitio; si cada cual pudiera elegir la suya, el
 * límite no filtraría nada.
 */
export function ipDelCliente(reenviada: string | null): string {
  if (reenviada) {
    const primera = reenviada.split(",")[0].trim();
    if (primera) return primera;
  }
  return "0.0.0.0";
}
