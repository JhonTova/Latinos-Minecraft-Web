import { EncabezadoDeBloque } from "@/components/encabezado-de-bloque";
import { patrocinadores } from "@/lib/api";
import Link from "next/link";

/**
 * Marcas patrocinadoras: hostings, tiendas de clientes, servicios del sector.
 *
 * **Es la tercera cosa distinta de la portada** y por eso tiene su propio
 * bloque, su propio encabezado y su propia etiqueta:
 *
 *   - Ranking → orgánico, se gana votando
 *   - Servidores patrocinados → los compra un dueño de servidor
 *   - Marcas patrocinadoras → empresas ajenas al ranking
 *
 * Mezclar cualquiera de las tres confundiría al visitante sobre qué está
 * mirando, y eso es exactamente lo que hace que un directorio deje de ser fiable.
 *
 * Los enlaces llevan `rel="sponsored nofollow noopener"`:
 * - `sponsored` es lo que Google exige declarar en un enlace pagado. Omitirlo
 *   hunde el posicionamiento del sitio ENTERO, no solo el del enlace.
 * - `noopener` impide que la página de destino manipule la nuestra.
 */
export async function MarcasPatrocinadoras() {
  const marcas = await patrocinadores().catch(() => []);

  if (marcas.length === 0) {
    return null;
  }

  return (
    <section aria-labelledby="titulo-marcas">
      <EncabezadoDeBloque
        id="titulo-marcas"
        titulo="Marcas patrocinadoras"
        etiqueta="Publicidad"
        nota="Hostings y servicios que apoyan LatinosMC"
      />

      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {marcas.map((marca) => (
          <li key={marca.id}>
            <a
              href={marca.urlDestino}
              target="_blank"
              rel="sponsored nofollow noopener"
              className="tarjeta-viva flex h-full items-center gap-3 rounded-lg border border-border bg-surface px-3.5 py-3 hover:border-primary/40"
            >
              {marca.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={marca.logoUrl}
                  alt=""
                  width={36}
                  height={36}
                  className="size-9 shrink-0 rounded-md object-cover ring-1 ring-border"
                />
              ) : (
                <span
                  aria-hidden
                  className="grid size-9 shrink-0 place-items-center rounded-md bg-primary-subtle text-sm font-bold text-primary"
                >
                  {marca.nombre.trim().charAt(0).toUpperCase()}
                </span>
              )}
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium">
                  {marca.nombre}
                </span>
                <span className="block truncate text-xs text-subtle">
                  {dominioDe(marca.urlDestino)}
                </span>
              </span>
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * Versión estrecha, para la barra lateral.
 *
 * Sin encabezado propio ni etiqueta grande: en una columna de 290 píxeles, tres
 * niveles de título hacen que todo parezca igual de importante y nada destaque.
 */
export async function MarcasEnLateral() {
  const marcas = await patrocinadores().catch(() => []);

  if (marcas.length === 0) {
    return null;
  }

  return (
    <section className="rounded-lg border border-border bg-surface-sunken/50 p-4">
      <h2 className="display mb-1 text-sm font-semibold">
        Marcas patrocinadoras
      </h2>
      <p className="mb-3 text-xs text-subtle">Publicidad</p>

      <ul className="space-y-1.5">
        {marcas.slice(0, 5).map((marca) => (
          <li key={marca.id}>
            <a
              href={marca.urlDestino}
              target="_blank"
              rel="sponsored nofollow noopener"
              className="flex items-center gap-2.5 rounded-md px-2 py-1.5 text-sm text-muted transition-colors hover:bg-surface hover:text-text"
            >
              {marca.logoUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={marca.logoUrl}
                  alt=""
                  width={20}
                  height={20}
                  className="size-5 shrink-0 rounded"
                />
              )}
              <span className="truncate">{marca.nombre}</span>
            </a>
          </li>
        ))}
      </ul>

      <Link
        href="/panel/destacar"
        className="enlace-vivo mt-3 inline-block text-xs text-primary"
      >
        Destacar mi servidor
      </Link>
    </section>
  );
}

function dominioDe(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}
