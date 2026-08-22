import { patrocinadores, type Patrocinador } from "@/lib/api";

/**
 * Marcas patrocinadoras: hostings, tiendas de clientes, servicios del sector.
 *
 * **Es la tercera cosa distinta de la portada** y por eso nunca comparte
 * contenedor con las otras dos:
 *
 *   - Ranking → orgánico, se gana votando
 *   - Servidores patrocinados → los compra un dueño de servidor
 *   - Patrocinadores → empresas ajenas al ranking
 *
 * Mezclar cualquiera de las tres confundiría al visitante sobre qué está
 * mirando, y eso es exactamente lo que hace que un directorio deje de ser fiable.
 *
 * **Dónde se pintan.** Dos formas, nunca las dos a la vez:
 *
 *   - **Raíl derecho** con banners verticales, a partir de 1600 px de ancho.
 *     Es lo que se vende como banner 160×600 y el único sitio donde ese formato
 *     cabe entero. Va *fuera* de la columna de contenido, colgado del margen
 *     derecho: así el ranking sigue midiendo lo mismo y alineado con el héroe
 *     haya patrocinadores o no. Antes había dos raíles y la portada cambiaba de
 *     forma según lo vendido; con uno solo, el contenido no se entera.
 *   - **Banda** bajo el ranking en todo lo demás, con tarjetas de logotipo. Por
 *     debajo de 1600 px no hay margen físico donde meter un raíl sin robarle
 *     ancho a la lista, que es a lo que la gente ha venido.
 *
 * En ambos casos después del ranking o a su altura, nunca por delante.
 *
 * Los enlaces llevan `rel="sponsored nofollow noopener"`:
 * - `sponsored` es lo que Google exige declarar en un enlace pagado. Omitirlo
 *   hunde el posicionamiento del sitio ENTERO, no solo el del enlace.
 * - `noopener` impide que la página de destino manipule la nuestra.
 */

/** Cuántas marcas caben en el raíl sin que haya que hacer scroll para verlas. */
const POR_RAIL = 4;

/**
 * Raíl derecho de banners verticales, para la portada.
 *
 * Va posicionado en absoluto sobre el margen del contenedor y no como una
 * columna más de la rejilla: una columna empujaría al ranking y lo dejaría
 * descentrado respecto al héroe justo en las pantallas donde más se nota.
 *
 * `sticky` para que acompañe al visitante mientras recorre el ranking, que es
 * justo lo que se vende: un banner que se pierde al primer scroll se ve dos
 * segundos y se cobra una semana.
 *
 * No se esconde de los lectores de pantalla: es contenido real, y quien navega
 * con uno tiene derecho a saber que ahí hay patrocinadores. Lo que hace es
 * anunciarse como tales.
 */
export function RailDePatrocinadores({ marcas }: { marcas: Patrocinador[] }) {
  if (marcas.length === 0) {
    return null;
  }

  return (
    <aside
      aria-label="Patrocinadores"
      // 1600 px y no 2xl (1536): a 1536 el raíl se come el aire del contenedor
      // y queda pegado al borde de la ventana. Por debajo de eso manda la banda.
      className="absolute inset-y-0 left-full ml-6 hidden w-40 min-[1600px]:block"
    >
      {/* top-28: la cabecera es sticky y mide 4rem más la barra de cifras. Sin
          este margen, el primer banner se metería debajo de ella al bajar. */}
      <div className="sticky top-28 space-y-3">
        <p className="nav-display text-[9px] text-subtle">Patrocinadores</p>

        {marcas.slice(0, POR_RAIL).map((marca) => (
          <BannerVertical key={marca.id} marca={marca} />
        ))}
      </div>
    </aside>
  );
}

function BannerVertical({ marca }: { marca: Patrocinador }) {
  return (
    <a
      href={marca.urlDestino}
      target="_blank"
      rel="sponsored nofollow noopener"
      className="tarjeta-viva group block overflow-hidden rounded-lg border border-border bg-surface shadow-sm hover:border-primary/40"
    >
      {marca.bannerUrl ? (
        // 160x600 es la proporción del rascacielos clásico, que es la que la
        // gente que vende publicidad ya tiene diseñada.
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={marca.bannerUrl}
          alt={marca.nombre}
          width={160}
          height={600}
          className="aspect-[160/600] w-full object-cover"
        />
      ) : (
        <SinBanner marca={marca} />
      )}
    </a>
  );
}

/**
 * Alternativa cuando la marca no ha subido banner.
 *
 * No se deja el hueco vacío ni se esconde al patrocinador: se compone una pieza
 * vertical con lo que sí tenemos —logotipo, nombre y dominio—. Una marca que
 * paga tiene que verse aunque nos haya mandado solo un logotipo.
 */
function SinBanner({ marca }: { marca: Patrocinador }) {
  return (
    <div
      // Alto mínimo para que el raíl no quede a saltos cuando una marca trae
      // banner y la siguiente solo logotipo.
      className="flex min-h-[260px] flex-col items-center justify-center gap-3 px-3 py-6 text-center"
      style={{
        background:
          "linear-gradient(180deg, color-mix(in oklch, var(--primary) 10%, transparent), transparent 70%)",
      }}
    >
      {marca.logoUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={marca.logoUrl}
          alt=""
          width={56}
          height={56}
          className="size-14 rounded-lg object-cover ring-1 ring-border"
        />
      ) : (
        <span
          aria-hidden
          className="display grid size-14 place-items-center rounded-lg bg-primary-subtle text-xl font-bold text-primary"
        >
          {inicial(marca.nombre)}
        </span>
      )}

      <span className="min-w-0">
        <span className="block text-sm leading-tight font-semibold text-balance">
          {marca.nombre}
        </span>
        <span className="mt-1 block truncate text-[11px] text-subtle">
          {dominioDe(marca.urlDestino)}
        </span>
      </span>

      <span className="mt-1 rounded-md border border-border px-2.5 py-1 text-[11px] font-medium transition-colors group-hover:border-primary group-hover:text-primary">
        Visitar
      </span>
    </div>
  );
}

/**
 * Banda de patrocinadores, para cuando no hay margen donde colgar el raíl.
 *
 * Tarjetas con logotipo, nombre y dominio: es lo que toda marca tiene, mientras
 * que un banner diseñado a medida lo manda una de cada tres. Aquí caben todas,
 * también las que en el raíl no habrían entrado.
 */
export function BandaDePatrocinadores({ marcas }: { marcas: Patrocinador[] }) {
  if (marcas.length === 0) {
    return null;
  }

  return (
    <section
      aria-labelledby="titulo-marcas"
      // Se apaga justo donde aparece el raíl: enseñar las mismas marcas dos
      // veces en la misma pantalla las devalúa a las dos.
      className="rounded-xl border border-border bg-surface-sunken/50 p-4 sm:p-5 min-[1600px]:hidden"
    >
      <h2 id="titulo-marcas" className="display mb-3 text-sm font-semibold">
        Patrocinadores
      </h2>

      <ul className="grid grid-cols-2 gap-2.5 sm:grid-cols-3 lg:grid-cols-4">
        {marcas.map((marca) => (
          <li key={marca.id}>
            <a
              href={marca.urlDestino}
              target="_blank"
              rel="sponsored nofollow noopener"
              className="tarjeta-viva flex h-full items-center gap-3 rounded-lg border border-border bg-surface px-3 py-2.5 hover:border-primary/40"
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
                  {inicial(marca.nombre)}
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
 * Banda para las páginas que tienen columna lateral (`/servidores` y las de
 * modalidad).
 *
 * Existe solo por debajo de `lg`: ahí la columna lateral deja de ir al lado y
 * cae al fondo de la página, así que las marcas acabarían por detrás de la
 * lista entera. Con esta banda arriba se ven igual que en escritorio, sin
 * repetirse: a partir de `lg` se apaga y manda la columna.
 */
export async function BandaEnListado() {
  const marcas = await patrocinadores().catch(() => []);

  if (marcas.length === 0) {
    return null;
  }

  return (
    <div className="mb-6 lg:hidden">
      <BandaDePatrocinadores marcas={marcas} />
    </div>
  );
}

/** Banners que se enseñan en la columna lateral antes de pasar a la lista. */
const BANNERS_EN_LATERAL = 2;

/**
 * Versión para la barra lateral de las páginas que la usan como columna de
 * verdad (`/servidores`, `/plugin`).
 *
 * Aquí no hace falta colgar nada del margen: la columna ya existe, mide 290 px
 * y está a la derecha de la lista. Es el equivalente del raíl de la portada, y
 * por eso las marcas con banner lo enseñan igual —a su ancho natural de 160 px,
 * centrado, sin estirarlo: un rascacielos escalado a 290 mediría más de mil
 * píxeles de alto y echaría de la pantalla a todo lo demás—.
 *
 * Las que no trajeron banner van debajo como lista de logotipo y nombre. Se
 * enseñan las dos primeras con banner y no todas para que la columna no se
 * convierta en una torre de anuncios por delante de «cómo se vota».
 *
 * Se pide los datos ella misma en lugar de recibirlos: así las páginas que la
 * usan no tienen que saber que existen los patrocinadores.
 */
export async function MarcasEnLateral() {
  const marcas = await patrocinadores().catch(() => []);

  if (marcas.length === 0) {
    return null;
  }

  const conBanner = marcas
    .filter((m) => m.bannerUrl)
    .slice(0, BANNERS_EN_LATERAL);
  const restantes = marcas.filter((m) => !conBanner.includes(m)).slice(0, 5);

  return (
    <section className="rounded-lg border border-border bg-surface-sunken/50 p-4">
      <h2 className="display mb-3 text-sm font-semibold">Patrocinadores</h2>

      {conBanner.length > 0 && (
        <div className="mb-3 space-y-3">
          {conBanner.map((marca) => (
            <a
              key={marca.id}
              href={marca.urlDestino}
              target="_blank"
              rel="sponsored nofollow noopener"
              className="tarjeta-viva mx-auto block w-40 overflow-hidden rounded-lg border border-border bg-surface shadow-sm hover:border-primary/40"
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={marca.bannerUrl!}
                alt={marca.nombre}
                width={160}
                height={600}
                className="aspect-[160/600] w-full object-cover"
              />
            </a>
          ))}
        </div>
      )}

      {restantes.length > 0 && (
        <ul className="space-y-1.5">
          {restantes.map((marca) => (
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
      )}
    </section>
  );
}

function inicial(nombre: string): string {
  return nombre.trim().charAt(0).toUpperCase();
}

function dominioDe(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}
