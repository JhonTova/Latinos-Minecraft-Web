import { patrocinadores, type Patrocinador } from "@/lib/api";

/**
 * Marcas patrocinadoras: hostings, tiendas de clientes, servicios del sector.
 *
 * **Es la tercera cosa distinta de la portada** y por eso nunca comparte
 * contenedor con las otras dos:
 *
 *   - Ranking → orgánico, se gana votando
 *   - Servidores patrocinados → los compra un dueño de servidor
 *   - Marcas patrocinadoras → empresas ajenas al ranking
 *
 * Mezclar cualquiera de las tres confundiría al visitante sobre qué está
 * mirando, y eso es exactamente lo que hace que un directorio deje de ser fiable.
 *
 * **Dónde se pintan.** En pantalla ancha, en los raíles laterales, a la altura
 * de las listas de servidores y no antes: quien acaba de entrar todavía está
 * buscando dónde jugar, y un anuncio por delante de eso estorba. Cuando no caben
 * los raíles, bajan a una tira compacta bajo el ranking. La misma información,
 * el sitio que permita cada pantalla.
 *
 * Los enlaces llevan `rel="sponsored nofollow noopener"`:
 * - `sponsored` es lo que Google exige declarar en un enlace pagado. Omitirlo
 *   hunde el posicionamiento del sitio ENTERO, no solo el del enlace.
 * - `noopener` impide que la página de destino manipule la nuestra.
 */

/** Cuántas marcas caben en un raíl sin que haya que hacer scroll para verlas. */
const POR_RAIL = 3;

/**
 * Reparte las marcas entre los dos raíles, alternando.
 *
 * Alterna en lugar de cortar por la mitad para que el reparto sea justo: con un
 * corte, las tres primeras irían siempre al lado izquierdo y las últimas al
 * derecho, y el orden de contratación acabaría decidiendo quién queda arriba en
 * la columna que primero se mira.
 */
export function repartirEnRailes(marcas: Patrocinador[]): {
  izquierda: Patrocinador[];
  derecha: Patrocinador[];
} {
  return {
    izquierda: marcas.filter((_, i) => i % 2 === 0).slice(0, POR_RAIL),
    derecha: marcas.filter((_, i) => i % 2 === 1).slice(0, POR_RAIL),
  };
}

/**
 * Raíl lateral de banners verticales.
 *
 * `sticky` para que acompañe al visitante mientras recorre el ranking, que es
 * justo lo que se vende: un banner que se pierde al primer scroll se ve dos
 * segundos y se cobra una semana.
 *
 * Marcado como `aria-hidden` **no**: es contenido real y quien navega con lector
 * de pantalla tiene derecho a saber que ahí hay publicidad. Lo que hace es
 * anunciarse como tal, con un encabezado que lo dice.
 */
export function RailDeMarcas({
  marcas,
  lado,
}: {
  marcas: Patrocinador[];
  lado: "izquierda" | "derecha";
}) {
  if (marcas.length === 0) {
    return <div className="hidden 2xl:block" aria-hidden />;
  }

  return (
    <aside
      aria-label={`Publicidad (${lado})`}
      // 2xl y no xl: a 1280 px los raíles le quitan 240 al centro, y ahí las
      // filas del ranking dejan de caber en una línea. La publicidad no puede
      // estropear la lista que la gente ha venido a leer.
      className="hidden 2xl:block"
    >
      {/* top-28: la cabecera es sticky y mide 4rem más la barra de cifras. Sin
          este margen, el primer banner se metería debajo de ella al bajar. */}
      <div className="sticky top-28 space-y-3">
        <p className="nav-display text-[9px] text-subtle">Publicidad</p>

        {marcas.map((marca) => (
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
 * No se deja el hueco vacío ni se esconde el patrocinador: se compone una pieza
 * vertical con lo que sí tenemos —logotipo, nombre y dominio—. Una marca que
 * paga tiene que verse aunque nos haya mandado solo un logotipo.
 */
function SinBanner({ marca }: { marca: Patrocinador }) {
  return (
    <div
      // Alto mínimo para que un raíl de logotipos no quede raquítico al lado del
      // otro cuando una marca sí trajo banner y la de enfrente no.
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
 * Tira compacta, para cuando no hay sitio a los lados.
 *
 * Va después del ranking, nunca antes: la publicidad no se pone por delante de
 * aquello que la gente ha venido a ver.
 */
export function TiraDeMarcas({ marcas }: { marcas: Patrocinador[] }) {
  if (marcas.length === 0) {
    return null;
  }

  return (
    <section
      aria-labelledby="titulo-marcas"
      className="rounded-xl border border-border bg-surface-sunken/50 p-4 2xl:hidden"
    >
      <div className="mb-3 flex items-baseline justify-between gap-3">
        <h2 id="titulo-marcas" className="display text-sm font-semibold">
          Marcas que apoyan LatinosMC
        </h2>
        <span className="nav-display text-[9px] text-subtle">Publicidad</span>
      </div>

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
 * Versión estrecha, para la barra lateral de las páginas que la usan como
 * columna de verdad (`/servidores`, `/plugin`).
 *
 * Sin encabezado grande ni banner: en una columna de 290 píxeles, tres niveles
 * de título hacen que todo parezca igual de importante y nada destaque. Los
 * banners verticales se reservan para los raíles de la portada, que es donde hay
 * sitio para que se vean.
 *
 * Se pide los datos ella misma en lugar de recibirlos: así las páginas que la
 * usan no tienen que saber que existen los patrocinadores.
 */
export async function MarcasEnLateral() {
  const marcas = await patrocinadores().catch(() => []);

  if (marcas.length === 0) {
    return null;
  }

  return (
    <section className="rounded-lg border border-border bg-surface-sunken/50 p-4">
      <div className="mb-3 flex items-baseline justify-between gap-2">
        <h2 className="display text-sm font-semibold">Marcas que apoyan</h2>
        <span className="nav-display text-[9px] text-subtle">Publicidad</span>
      </div>

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
