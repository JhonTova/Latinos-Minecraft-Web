import { ThemeToggle } from "@/components/theme-toggle";
import {
  ajustesDelSitio,
  estadisticas,
  type EstadisticasPlataforma,
} from "@/lib/api";
import Link from "next/link";

/**
 * Cabecera pública.
 *
 * El reparto es deliberado: la marca a la izquierda, **Servidores** en el centro
 * porque es lo que viene a hacer el 90 % de quien entra, y a la derecha lo que
 * solo interesa a los dueños de servidor. Poner el listado en el centro y no
 * junto al resto de enlaces es lo que lo convierte en destino en lugar de en una
 * opción más de un menú.
 */
export async function Cabecera() {
  const [sitio, cifras] = await Promise.all([ajustesDelSitio(), estadisticas()]);

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-bg/85 backdrop-blur-md">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-4 px-4 sm:px-6">
        {/* Izquierda: marca. Siempre lleva a la portada, desde cualquier
            pantalla — también desde el panel. */}
        <Link
          href="/"
          className="group flex shrink-0 items-center gap-2.5 font-semibold"
        >
          <Logotipo url={sitio.logoUrl} nombre={sitio.nombre} />
          <span className="hidden sm:inline">{sitio.nombre}</span>
        </Link>

        {/* Centro: el listado. Absoluto para que quede centrado respecto a la
            página y no respecto al hueco que dejen los lados, que cambian de
            ancho según el nombre de la marca y si hay botón de plugin. */}
        <div className="pointer-events-none absolute left-1/2 hidden -translate-x-1/2 md:block">
          <div className="pointer-events-auto flex items-center gap-6">
            <EnlaceCentral href="/">Inicio</EnlaceCentral>
            <EnlaceCentral href="/servidores">Servidores</EnlaceCentral>
            <EnlaceCentral href="/destacar">Destacar</EnlaceCentral>
            <EnlaceCentral href="/plugin">Plugin</EnlaceCentral>
          </div>
        </div>

        <div className="flex flex-1 items-center justify-end gap-1.5">
          {/* En móvil el enlace central no cabe, así que aquí aparece uno
              normal. Sin esto, en móvil no habría forma de llegar al listado. */}
          <Link
            href="/servidores"
            className="rounded-md px-3 py-1.5 text-sm text-muted transition-colors hover:bg-surface-sunken hover:text-text md:hidden"
          >
            Servidores
          </Link>

          {/* Píldora de jugadores en vivo. Es la información que hace que una
              cabecera se sienta viva en lugar de vacía: un número que cambia.
              Se esconde en móvil, donde el sitio lo necesitan los botones. */}
          {cifras && cifras.jugadores > 0 && (
            <span className="mr-1 hidden items-center gap-1.5 rounded-full border border-border bg-surface px-2.5 py-1 text-xs md:inline-flex">
              <span className="late size-1.5 rounded-full bg-success" aria-hidden />
              <span className="cifras font-semibold">
                {new Intl.NumberFormat("es-ES").format(cifras.jugadores)}
              </span>
              <span className="text-muted">jugando</span>
            </span>
          )}

          <ThemeToggle />

          {/* «Entrar» y no «Panel»: quien no tiene cuenta no sabe qué es un
              panel, y quien la tiene entiende igual las dos. El destino es el
              mismo — /panel redirige a /login si no hay sesión. */}
          <Link
            href="/panel"
            className="boton-vivo inline-flex shrink-0 items-center gap-1.5 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
          >
            <svg viewBox="0 0 24 24" className="size-4" aria-hidden>
              <path
                d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <span className="hidden sm:inline">Entrar</span>
          </Link>
        </div>
      </div>


      {cifras && <BarraDeCifras cifras={cifras} />}
    </header>
  );
}

/** Enlace del bloque central, en versalitas. */
function EnlaceCentral({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      className="nav-display enlace-vivo text-[13px] transition-colors hover:text-primary"
    >
      {children}
    </Link>
  );
}

/**
 * Cifras vivas de la plataforma.
 *
 * Todas se miden con ping directo desde nuestra infraestructura (ADR-0009),
 * nunca con lo que reporte cada plugin. Es lo que separa un contador de un
 * anuncio: si cada servidor declarara sus jugadores, el total sería la suma de
 * lo que a cada uno le conviene decir.
 *
 * Se esconde en móvil. Es información agradable, no necesaria, y en una pantalla
 * estrecha le quitaría sitio a lo que sí importa.
 */
function BarraDeCifras({ cifras }: { cifras: EstadisticasPlataforma }) {
  const formato = new Intl.NumberFormat("es-ES");

  return (
    <div className="hidden border-b border-border bg-surface-sunken/60 sm:block">
      <div className="mx-auto flex w-full max-w-6xl items-center gap-6 px-4 py-1.5 text-xs sm:px-6">
        <span className="text-muted">
          <span className="cifras font-semibold text-text">
            {formato.format(cifras.enLinea)}
          </span>{" "}
          de {formato.format(cifras.servidores)} servidores en línea
        </span>

        <span className="text-border" aria-hidden>
          ·
        </span>

        <span className="text-muted">
          <span className="cifras font-semibold text-text">
            {formato.format(cifras.votos24h)}
          </span>{" "}
          votos en las últimas 24 h
        </span>

        <span className="ml-auto hidden text-subtle md:inline">
          Jugadores y estado medidos por nosotros, no declarados por cada servidor
        </span>
      </div>
    </div>
  );
}

/**
 * Marca.
 *
 * Si hay logotipo subido se usa. Si no, las iniciales sobre un degradado de la
 * marca — no un icono genérico ni un hueco vacío.
 *
 * La alternativa dibujada importa más de lo que parece: recién instalado, sin
 * que nadie haya subido nada, la cabecera ya tiene identidad. Un sitio que
 * arranca con un cuadro gris parece a medio hacer desde el primer minuto.
 */
function Logotipo({ url, nombre }: { url: string | null; nombre: string }) {
  if (url) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={url}
        alt=""
        width={32}
        height={32}
        className="size-8 shrink-0 rounded-lg object-cover shadow-sm transition-transform duration-200 group-hover:scale-105"
      />
    );
  }

  return (
    <span
      aria-hidden
      className="display grid size-8 shrink-0 place-items-center rounded-lg text-[13px] font-bold text-on-accent shadow-sm transition-transform duration-200 group-hover:scale-105"
      style={{
        // Degradado en lugar de color plano: es lo que diferencia un logotipo
        // improvisado de un cuadrado de color.
        background:
          "linear-gradient(135deg, var(--accent-500), var(--accent-700))",
      }}
    >
      {iniciales(nombre)}
    </span>
  );
}

/** «LatinosMC» → «LM». Dos letras: con tres dejan de leerse a 32 píxeles. */
function iniciales(nombre: string): string {
  const limpio = nombre.trim();
  const palabras = limpio.split(/\s+/).filter(Boolean);

  if (palabras.length >= 2) {
    return (palabras[0][0] + palabras[1][0]).toUpperCase();
  }
  // Una sola palabra: se aprovechan sus mayúsculas internas («LatinosMC» → LM).
  const mayusculas = limpio.replace(/[^A-ZÁÉÍÓÚÑ]/g, "");
  return (
    mayusculas.length >= 2 ? mayusculas.slice(0, 2) : limpio.slice(0, 2)
  ).toUpperCase();
}

export async function PieDePagina() {
  const sitio = await ajustesDelSitio();
  const año = new Date().getFullYear();

  return (
    <footer className="mt-auto border-t border-border bg-surface-sunken">
      <div className="mx-auto w-full max-w-6xl px-4 py-12 sm:px-6">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <div className="flex items-center gap-2.5 font-semibold">
              <Logotipo url={sitio.logoUrl} nombre={sitio.nombre} />
              {sitio.nombre}
            </div>
            <p className="mt-3 max-w-sm text-sm text-muted text-pretty">
              El directorio de servidores de Minecraft en español. Encuentra
              dónde jugar, vota por tu favorito y llévate recompensas dentro del
              juego.
            </p>
          </div>

          <div>
            <h2 className="mb-3 text-sm font-semibold">Jugadores</h2>
            <ul className="space-y-2 text-sm text-muted">
              <li>
                <Link href="/servidores" className="enlace-vivo">
                  Todos los servidores
                </Link>
              </li>
              <li>
                <Link href="/" className="enlace-vivo">
                  Destacados
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h2 className="mb-3 text-sm font-semibold">Dueños de servidor</h2>
            <ul className="space-y-2 text-sm text-muted">
              <li>
                <Link href="/registro" className="enlace-vivo">
                  Añadir mi servidor
                </Link>
              </li>
              <li>
                <Link href="/panel" className="enlace-vivo">
                  Mi panel
                </Link>
              </li>
              <li>
                <Link href="/destacar" className="enlace-vivo">
                  Destacar mi servidor
                </Link>
              </li>
              {sitio.urlPlugin && (
                <li>
                  <a
                    href={sitio.urlPlugin}
                    target="_blank"
                    rel="noopener"
                    className="enlace-vivo"
                  >
                    Descargar el plugin
                  </a>
                </li>
              )}
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-6 text-xs text-subtle">
          <span>
            © {año} {sitio.nombre}
          </span>
          <span>No estamos afiliados a Mojang ni a Microsoft.</span>
        </div>
      </div>
    </footer>
  );
}
