/**
 * Las etiquetas que dicen quién puede entrar a un servidor.
 *
 * Es el dato más útil de una ficha después de la dirección, y no por
 * conveniencia: un jugador **sin** cuenta original que entra a un servidor
 * premium no es que lo pase peor, es que **no puede jugar**. El servidor le
 * rechaza la conexión. Antes de esto, el directorio le mandaba ahí sin avisar.
 *
 * Por eso el color nunca va solo: cada etiqueta lleva su texto completo y su
 * `title`, para quien no distingue colores y para quien navega con lector de
 * pantalla.
 */

/**
 * Texto largo, el que se usa en la ficha y como título accesible.
 *
 * La etiqueta corta dice la edición («Java Edition Premium») porque es como lo
 * nombra el jugador; el texto largo dice qué significa, que es lo que de verdad
 * necesita quien no conoce la jerga.
 */
export function textoDeCuenta(aceptaNoPremium: boolean): string {
  return aceptaNoPremium
    ? "Se puede entrar con cuenta original o sin ella"
    : "Hace falta una cuenta original de Minecraft para entrar";
}

export function EtiquetaDeCuenta({
  aceptaNoPremium,
  tamano = "normal",
}: {
  aceptaNoPremium: boolean;
  tamano?: "normal" | "pequena";
}) {
  const texto = textoDeCuenta(aceptaNoPremium);

  // Corto en las listas, completo en la ficha. En una tarjeta de ranking la
  // frase entera partiría la línea y empujaría todo lo demás.
  const etiqueta = aceptaNoPremium
    ? "Java Edition No Premium"
    : "Java Edition Premium";

  return (
    <span
      title={texto}
      className={`inline-flex shrink-0 items-center gap-1 rounded-full border font-medium ${
        tamano === "pequena" ? "px-1.5 py-0.5 text-[11px]" : "px-2 py-0.5 text-xs"
      } ${
        aceptaNoPremium
          ? "border-success/30 bg-success/10 text-success"
          : "border-info/30 bg-info/10 text-info"
      }`}
    >
      <span aria-hidden>{aceptaNoPremium ? "◈" : "◆"}</span>
      {etiqueta}
      <span className="sr-only">. {texto}</span>
    </span>
  );
}

export function EtiquetaBedrock({
  tamano = "normal",
}: {
  tamano?: "normal" | "pequena";
}) {
  return (
    <span
      title="Se puede jugar desde móvil o consola (Bedrock)"
      className={`inline-flex shrink-0 items-center gap-1 rounded-full border border-secundario/40 bg-secundario/10 font-medium text-secundario ${
        tamano === "pequena" ? "px-1.5 py-0.5 text-[11px]" : "px-2 py-0.5 text-xs"
      }`}
    >
      <span aria-hidden>▣</span>
      Bedrock Edition
      <span className="sr-only">. Se puede jugar desde móvil o consola</span>
    </span>
  );
}
