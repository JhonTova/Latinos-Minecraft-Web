import { TarjetaDestacado } from "@/components/tarjeta-destacado";
import { destacadosDePortada, type SlotEnPortada } from "@/lib/api";
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
 * **Aquí no se vende nada.** Ni espacios libres como reclamo ni enlace a la
 * tienda: quien está en la parte pública viene a encontrar un servidor donde
 * jugar, y ponerle delante un «compra este hueco» le enseña un producto que no
 * quiere y ensucia lo que sí. Los espacios se contratan desde el panel, que es
 * donde está el dueño de servidor. Sin nada vendido, el bloque no existe.
 */
export async function ServidoresPatrocinados() {
  await connection();
  const slots = await destacadosDePortada().catch((): SlotEnPortada[] => []);
  // `!= null` con doble igual, no `!==`: descarta null Y undefined de una vez.
  // Es el caso que rompio la portada — un campo ausente no es null, y el filtro
  // dejaba pasar los huecos vacios para reventar dos lineas mas abajo.
  const ocupados = slots.filter((s) => s.servidor != null);

  if (ocupados.length === 0) {
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

        <p className="text-xs text-subtle">
          Espacios comprados. No cuentan para el ranking.
        </p>
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
      </ul>
    </section>
  );
}
