import type { ServidorDestacado } from "@/lib/api";
import Link from "next/link";

/**
 * Tarjeta de servidor destacado.
 *
 * Distinta de la fila del ranking a propósito: aquí hay banner, icono grande y
 * botón propio, porque son seis y no cincuenta. Lo que se paga es exactamente
 * eso — sitio para respirar.
 *
 * **La cinta de arriba no es decoración: es la regla 14 del proyecto.** Lo
 * patrocinado no se mezcla nunca con lo orgánico, y no basta con separarlo de
 * bloque: cada tarjeta lo dice por escrito, con la palabra completa y sobre
 * color de marca, para que nadie la confunda con un puesto del ranking ni
 * siquiera mirando una sola tarjeta fuera de contexto.
 *
 * **Sin votos ni posición de ranking.** Un destacado se paga por visibilidad, no
 * por aparentar puesto. El número de la cinta es el espacio comprado (1 al 6);
 * enseñar aquí un contador de votos invitaría a confundir lo comprado con lo
 * ganado.
 */
export function TarjetaDestacado({
  posicion,
  servidor,
}: {
  posicion: number;
  servidor: ServidorDestacado;
}) {
  const versiones =
    servidor.versionMinima && servidor.versionMaxima
      ? `${servidor.versionMinima} – ${servidor.versionMaxima}`
      : (servidor.versionMinima ?? servidor.versionMaxima);

  return (
    <li className="tarjeta-viva group relative flex flex-col overflow-hidden rounded-xl border border-primary/30 bg-surface shadow-sm hover:border-primary/60">
      {/* Cinta. Texto real y no solo color: quien no distingue colores también
          tiene que poder saber que esto es un espacio comprado. */}
      <div className="flex items-center justify-between gap-2 bg-primary px-3.5 py-1.5 text-on-accent">
        <span className="nav-display flex items-center gap-1.5 text-[10px]">
          <svg viewBox="0 0 24 24" className="size-3" aria-hidden>
            <path
              d="m12 3 2.6 5.6 6.4.8-4.7 4.3 1.2 6.3-5.5-3-5.5 3 1.2-6.3L3 9.4l6.4-.8L12 3Z"
              fill="currentColor"
            />
          </svg>
          Patrocinado
        </span>
        <span className="cifras text-[10px] font-semibold opacity-75">
          Espacio {posicion}
        </span>
      </div>

      {/* Banner. Si no lo hay, la trama de bloques en lugar de un rectángulo
          gris: un hueco vacío en la parte más cara de la portada se lee como
          que el sitio está a medio hacer. */}
      <div className="relative h-28 overflow-hidden bg-primary-subtle">
        {servidor.bannerUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={servidor.bannerUrl}
            alt=""
            width={1200}
            height={300}
            className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
          />
        ) : (
          <div
            className="size-full"
            style={{
              background:
                "linear-gradient(135deg, color-mix(in oklch, var(--primary) 30%, transparent), color-mix(in oklch, var(--oro-400) 16%, transparent))",
            }}
          >
            <div className="trama-bloques size-full" aria-hidden />
          </div>
        )}

        {/* Degradado hacia el fondo de la tarjeta: sin él, el icono superpuesto
            se recorta contra un borde duro. */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-x-0 bottom-0 h-12"
          style={{
            background:
              "linear-gradient(to top, var(--surface), transparent)",
          }}
        />
      </div>

      <div className="px-4 pb-4">
        <div className="-mt-9 flex items-end gap-3">
          {servidor.iconoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={servidor.iconoUrl}
              alt=""
              width={72}
              height={72}
              className="size-18 shrink-0 rounded-xl object-cover shadow-md ring-4 ring-surface"
            />
          ) : (
            <span
              aria-hidden
              className="display grid size-18 shrink-0 place-items-center rounded-xl bg-primary-subtle text-2xl font-bold text-primary shadow-md ring-4 ring-surface"
            >
              {servidor.nombre.trim().charAt(0).toUpperCase()}
            </span>
          )}

          <h3 className="min-w-0 flex-1 truncate pb-1.5 text-lg font-semibold">
            <Link
              href={`/servidores/${servidor.slug}`}
              className="transition-colors after:absolute after:inset-0 group-hover:text-primary"
            >
              {servidor.nombre}
            </Link>
          </h3>
        </div>

        {servidor.modalidades.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1">
            {servidor.modalidades.slice(0, 3).map((m) => (
              <span
                key={m}
                className="rounded bg-primary-subtle px-1.5 py-0.5 text-[11px] font-medium text-primary"
              >
                {m}
              </span>
            ))}
          </div>
        )}

        <p className="mt-2 line-clamp-2 min-h-10 text-sm text-muted text-pretty">
          {servidor.descripcion ?? ""}
        </p>
      </div>

      <div className="mt-auto flex items-center gap-3 border-t border-border bg-surface-sunken/40 px-4 py-3">
        {/* La dirección y no un contador de jugadores: es el único dato que
            alguien copia de verdad, y la API de destacados no trae cifras a
            propósito para no invitar a compararlas con las del ranking. */}
        <span className="cifras min-w-0 truncate font-mono text-xs text-muted">
          {servidor.direccion}
        </span>

        {versiones && (
          <span className="cifras shrink-0 rounded border border-border px-1.5 py-0.5 font-mono text-[11px] text-subtle">
            {versiones}
          </span>
        )}

        <Link
          href={`/servidores/${servidor.slug}`}
          className="boton-vivo relative z-10 ml-auto shrink-0 rounded-md bg-primary px-3.5 py-1.5 text-sm font-semibold text-on-accent shadow-sm hover:bg-primary-hover"
        >
          Votar
        </Link>
      </div>
    </li>
  );
}
