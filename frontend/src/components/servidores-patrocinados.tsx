import { TarjetaDestacado } from "@/components/tarjeta-destacado";
import { destacadosDePortada, type SlotEnPortada } from "@/lib/api";
import Link from "next/link";
import { connection } from "next/server";

/**
 * Bloque de espacios comprados (ADR-0013).
 *
 * Va dentro de su propio marco tintado, con encabezado propio. Que sea un
 * recuadro entero y no unas tarjetas sueltas es lo que hace que la separación
 * respecto al ranking se vea de un vistazo, sin tener que leer nada.
 *
 * **Sale en la portada y en `/servidores`.** Son las dos páginas donde alguien
 * está eligiendo servidor, que es exactamente lo que se compra; enseñarlo solo
 * en la portada dejaría fuera a quien llega por un filtro o desde el buscador.
 *
 * Lo que cambia entre las dos es la invitación a comprar:
 *
 * @param variante `portada` enseña además algún espacio libre como oferta —es
 *   donde miran los dueños de servidor—. `listado` enseña solo lo vendido: quien
 *   está filtrando por modalidad viene a jugar, y un recuadro de «compra aquí»
 *   en medio de su búsqueda es ruido.
 */
export async function ServidoresPatrocinados({
  variante = "portada",
}: {
  variante?: "portada" | "listado";
}) {
  await connection();
  const slots = await destacadosDePortada().catch((): SlotEnPortada[] => []);
  // `!= null` con doble igual, no `!==`: descarta null Y undefined de una vez.
  // Es el caso que rompio la portada — un campo ausente no es null, y el filtro
  // dejaba pasar los huecos vacios para reventar dos lineas mas abajo.
  const ocupados = slots.filter((s) => s.servidor != null);
  const libres = slots.filter((s) => s.servidor == null);

  // Con la plataforma vacía se enseña UN solo hueco como oferta, no los seis.
  // Seis recuadros vacíos en lo primero que se ve son un cartel de «aquí no hay
  // nada»; uno solo se lee como una invitación.
  //
  // Con espacios vendidos se completan filas: como van de dos en dos, un hueco
  // impar dejaría la última fila coja, y eso se lee como un fallo de
  // maquetación en vez de como el espacio libre que es.
  const huecos =
    variante === "listado"
      ? 0
      : ocupados.length === 0
        ? Math.min(libres.length, 1)
        : Math.min(libres.length, ocupados.length % 2 === 1 ? 1 : 2);

  if (ocupados.length === 0 && huecos === 0) {
    return null;
  }

  return (
    <section
      aria-labelledby="titulo-destacados"
      className="rounded-2xl border border-primary/20 bg-primary-subtle/30 p-4 sm:p-5"
    >
      <header className="mb-4 flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        {/* El título ya dice que están patrocinados; una etiqueta de
            «Publicidad» al lado repite la misma palabra con otro tono. */}
        <h2 id="titulo-destacados" className="display text-lg font-semibold">
          Servidores patrocinados
        </h2>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
          <p className="text-xs text-subtle">
            Espacios comprados. No cuentan para el ranking.
          </p>
          {variante === "portada" && (
            <Link
              href="/panel/destacar"
              className="enlace-vivo text-sm font-medium text-primary"
            >
              Destacar el mío
            </Link>
          )}
        </div>
      </header>

      {/* Dos por fila y no tres: con tres, el banner y la descripción se quedan
          sin ancho y la tarjeta deja de valer más que una fila del ranking, que
          es justo por lo que se paga. */}
      <ul className="grid gap-4 sm:grid-cols-2">
        {ocupados.map((slot) => (
          <TarjetaDestacado
            key={slot.posicion}
            posicion={slot.posicion}
            servidor={slot.servidor!}
          />
        ))}

        {libres.slice(0, huecos).map((slot) => (
          <HuecoLibre key={slot.posicion} slot={slot.posicion} />
        ))}
      </ul>
    </section>
  );
}

/** Espacio sin vender. Se enseña como oferta, no como error. */
function HuecoLibre({ slot }: { slot: number }) {
  return (
    <li className="flex">
      <Link
        href={`/panel/destacar?slot=${slot}`}
        className="group flex min-h-[248px] flex-1 flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-primary/30 p-6 text-center transition-colors hover:border-primary hover:bg-primary-subtle/60"
      >
        <span className="grid size-11 place-items-center rounded-xl border border-dashed border-primary/40 text-xl text-primary/70 transition-colors group-hover:border-primary group-hover:text-primary">
          +
        </span>
        <span className="display mt-1 font-semibold transition-colors group-hover:text-primary">
          Espacio {slot} libre
        </span>
        <span className="max-w-[24ch] text-sm text-subtle text-pretty">
          Reserva por semanas. Ves el precio y las fechas antes de pagar
        </span>
      </Link>
    </li>
  );
}
