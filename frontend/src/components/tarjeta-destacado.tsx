import type { ServidorDestacado } from "@/lib/api";
import Link from "next/link";

const formatoNumero = new Intl.NumberFormat("es-ES");

/**
 * Colores de medalla por puesto.
 *
 * Oro, plata y bronce solo para los tres primeros; del cuarto en adelante, un
 * color neutro. Inventar un cuarto y un quinto metal haría que la escala dejara
 * de leerse de un vistazo, que es lo único que aporta.
 */
const MEDALLAS = [
  // Oro: el token de la marca, no un color suelto de Tailwind. Si el secundario
  // cambia algún día, la medalla cambia con él en lugar de quedarse huérfana.
  { fondo: "var(--oro-400)", texto: "oklch(0.25 0.06 75)" },
  { fondo: "oklch(0.82 0.01 300)", texto: "oklch(0.25 0.02 300)" },
  { fondo: "oklch(0.7 0.11 45)", texto: "oklch(0.99 0 0)" },
];

/**
 * Tarjeta de servidor destacado.
 *
 * Distinta de la fila del ranking a propósito: aquí hay espacio para una imagen
 * grande y un botón de votar, porque son cinco y no cincuenta. Lo que se paga es
 * exactamente eso — sitio para respirar.
 *
 * **Sin votos ni posición.** Un destacado se paga por visibilidad, no por
 * aparentar puesto. El número de la medalla es el espacio comprado (1 al 5), no
 * un ranking, y por eso las tarjetas no llevan contador de votos: enseñarlo
 * invitaría a confundir lo comprado con lo ganado.
 */
export function TarjetaDestacado({
  posicion,
  servidor,
  jugadores,
}: {
  posicion: number;
  servidor: ServidorDestacado;
  jugadores: number | null;
}) {
  const medalla = MEDALLAS[posicion - 1];

  const versiones =
    servidor.versionMinima && servidor.versionMaxima
      ? `${servidor.versionMinima} – ${servidor.versionMaxima}`
      : (servidor.versionMinima ?? servidor.versionMaxima);

  return (
    <li
      className={`tarjeta-viva group relative flex flex-col overflow-hidden rounded-xl border bg-surface shadow-sm ${
        posicion === 1
          ? "border-primary/50 ring-1 ring-primary/20"
          : "border-border hover:border-primary/40"
      }`}
    >
      <div className="flex gap-3.5 p-4">
        {/* Imagen: banner recortado a cuadrado, o icono, o la inicial. Los tres
            ocupan exactamente el mismo espacio para que las tarjetas de la fila
            queden alineadas pase lo que pase. */}
        <div className="relative shrink-0">
          {servidor.bannerUrl || servidor.iconoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={servidor.bannerUrl ?? servidor.iconoUrl!}
              alt=""
              width={112}
              height={112}
              className="size-28 rounded-lg object-cover ring-1 ring-border transition-transform duration-500 group-hover:scale-[1.04]"
            />
          ) : (
            <span
              aria-hidden
              className="grid size-28 place-items-center rounded-lg bg-primary-subtle text-4xl font-bold text-primary"
            >
              {servidor.nombre.trim().charAt(0).toUpperCase()}
            </span>
          )}

          <span
            aria-hidden
            className="cifras absolute -top-1.5 -left-1.5 grid size-7 place-items-center rounded-full text-xs font-bold shadow-sm ring-2 ring-surface"
            style={
              medalla
                ? { backgroundColor: medalla.fondo, color: medalla.texto }
                : { backgroundColor: "var(--surface-sunken)", color: "var(--text-muted)" }
            }
          >
            {posicion}
          </span>
        </div>

        <div className="min-w-0 flex-1">
          <h3 className="truncate font-semibold">
            <Link
              href={`/servidores/${servidor.slug}`}
              className="transition-colors after:absolute after:inset-0 group-hover:text-primary"
            >
              {servidor.nombre}
            </Link>
          </h3>

          {servidor.modalidades.length > 0 && (
            <div className="mt-1.5 flex flex-wrap gap-1">
              {servidor.modalidades.slice(0, 2).map((m) => (
                <span
                  key={m}
                  className="rounded bg-primary-subtle px-1.5 py-0.5 text-[11px] font-medium text-primary"
                >
                  {m}
                </span>
              ))}
            </div>
          )}

          <p className="mt-2 line-clamp-2 text-sm text-muted text-pretty">
            {servidor.descripcion ?? ""}
          </p>
        </div>
      </div>

      <div className="mt-auto flex items-center gap-3 border-t border-border px-4 py-3">
        <span className="flex items-center gap-1.5 text-sm">
          <svg viewBox="0 0 24 24" className="size-4 text-subtle" aria-hidden>
            <path
              d="M16 19v-2a4 4 0 0 0-8 0v2M12 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          </svg>
          <span className="cifras font-semibold">
            {jugadores !== null ? formatoNumero.format(jugadores) : "—"}
          </span>
        </span>

        {versiones && (
          <span className="cifras rounded border border-border px-1.5 py-0.5 font-mono text-[11px] text-muted">
            {versiones}
          </span>
        )}

        <Link
          href={`/servidores/${servidor.slug}`}
          className="boton-vivo relative z-10 ml-auto rounded-md bg-primary px-3.5 py-1.5 text-sm font-semibold text-on-accent shadow-sm hover:bg-primary-hover"
        >
          Votar
        </Link>
      </div>
    </li>
  );
}
