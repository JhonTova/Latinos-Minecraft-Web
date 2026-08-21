/**
 * Pinta el MOTD de un servidor con sus colores y su formato.
 *
 * El banner ya lleva el MOTD dibujado, así que esto parece redundante y no lo
 * es: un lector de pantalla no puede leer un PNG y un buscador tampoco. Sin esta
 * versión en texto, lo que el servidor dice de sí mismo —que suele ser lo único
 * que dice— sería invisible para las dos cosas que más importan a un directorio.
 *
 * El formato es el que produce `MotdJson` en el backend (ADR-0027):
 *
 *   [[{ t: "Mi ", c: 16755200 }, { t: "Servidor", f: "b" }]]
 *
 *   t  el texto
 *   c  color RGB como entero; ausente si es blanco
 *   f  formato: b negrita, i cursiva, u subrayado, s tachado
 */

interface Tramo {
  t: string;
  c?: number;
  f?: string;
}

/**
 * Interpreta lo que venga de la API sin fiarse de su forma.
 *
 * Devuelve `null` ante cualquier cosa rara en vez de lanzar. Esto se pinta
 * dentro de la ficha pública y del ranking: reventar aquí tumbaría la página
 * entera por una fila mal guardada, y el MOTD es lo menos importante que hay en
 * ella.
 */
function interpretar(motd: string | null | undefined): Tramo[][] | null {
  if (!motd) return null;

  try {
    const datos: unknown = JSON.parse(motd);
    if (!Array.isArray(datos)) return null;

    const lineas = datos
      .filter(Array.isArray)
      .map((linea) =>
        (linea as unknown[]).filter(
          (tramo): tramo is Tramo =>
            typeof tramo === "object" &&
            tramo !== null &&
            typeof (tramo as Tramo).t === "string",
        ),
      )
      .filter((linea) => linea.length > 0);

    return lineas.length > 0 ? lineas : null;
  } catch {
    return null;
  }
}

function color(valor: number | undefined): string | undefined {
  if (valor === undefined) return undefined;
  return `#${(valor & 0xffffff).toString(16).padStart(6, "0")}`;
}

export function textoPlanoDelMotd(motd: string | null | undefined): string {
  const lineas = interpretar(motd);
  if (!lineas) return "";
  return lineas.map((l) => l.map((t) => t.t).join("")).join(" · ");
}

/**
 * @param motd el JSON tal como lo devuelve la API; si es nulo o ilegible, no se
 *   pinta nada — un hueco es mejor que un mensaje de error sobre algo que el
 *   visitante no sabía que existía
 */
export function Motd({
  motd,
  className = "",
}: {
  motd: string | null | undefined;
  className?: string;
}) {
  const lineas = interpretar(motd);
  if (!lineas) return null;

  return (
    <div className={`font-mono text-sm leading-relaxed ${className}`}>
      {lineas.map((linea, i) => (
        <p key={i} className="truncate">
          {linea.map((tramo, j) => (
            <span
              key={j}
              style={{
                color: color(tramo.c),
                fontWeight: tramo.f?.includes("b") ? 700 : undefined,
                fontStyle: tramo.f?.includes("i") ? "italic" : undefined,
                textDecoration:
                  [
                    tramo.f?.includes("u") ? "underline" : "",
                    tramo.f?.includes("s") ? "line-through" : "",
                  ]
                    .filter(Boolean)
                    .join(" ") || undefined,
              }}
            >
              {tramo.t}
            </span>
          ))}
        </p>
      ))}
    </div>
  );
}
