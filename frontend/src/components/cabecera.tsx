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
            {/* «Votar» y no «Destacar»: quien entra aquí viene a jugar, no a
                comprar publicidad. Este es el único enlace de la cabecera para
                el jugador que ya está dentro de un servidor y solo necesita un
                sitio donde escribir su código — el resto del bucle del producto
                depende de que ese paso sea fácil de encontrar. */}
            <EnlaceCentral href="/votar">Votar</EnlaceCentral>
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

/**
 * Pie de página.
 *
 * Tres decisiones que lo separan del típico muro de enlaces:
 *
 * 1. **La marca ocupa una columna ancha con su propia voz**, no un logotipo
 *    suelto sobre cuatro listas iguales. El pie es lo último que se lee: si
 *    todas las columnas pesan lo mismo, no queda nada en la cabeza.
 * 2. **Lleva las cifras vivas.** Un pie con un número que cambia dice que el
 *    sitio está vivo; uno con solo enlaces y un año, que lleva ahí desde 2019.
 * 3. **Las columnas están agrupadas por a quién sirven** —quien juega, quien
 *    tiene servidor— y no por dónde vive cada página. La estructura interna del
 *    sitio no le interesa a nadie que esté leyendo el pie.
 */
export async function PieDePagina() {
  const [sitio, cifras] = await Promise.all([ajustesDelSitio(), estadisticas()]);
  const año = new Date().getFullYear();
  const formato = new Intl.NumberFormat("es-ES");

  return (
    // El filete de marca que había aquí —una línea de un píxel que pasaba de
    // morado a dorado y se perdía en el fondo— cerraba la página con una raya de
    // tres colores. Ahora el cierre lo hace un resplandor ancho del acento sobre
    // el borde: mismo trabajo, sin una línea que mirar y sin moverse.
    <footer className="resplandor-superior mt-auto overflow-hidden border-t border-border bg-surface-sunken">
      <div className="mx-auto w-full max-w-6xl px-4 py-14 sm:px-6">
        <div className="grid gap-10 md:grid-cols-2 lg:grid-cols-[1.6fr_1fr_1fr]">
          <div className="min-w-0">
            <Link
              href="/"
              className="group inline-flex items-center gap-2.5"
            >
              <Logotipo url={sitio.logoUrl} nombre={sitio.nombre} />
              <span className="display text-lg font-semibold">
                {sitio.nombre}
              </span>
            </Link>

            <p className="mt-4 max-w-sm text-sm text-muted text-pretty">
              El directorio de servidores de Minecraft en español. Encuentra
              dónde jugar, vota por tu favorito y llévate recompensas dentro del
              juego.
            </p>

            {cifras && cifras.servidores > 0 && (
              <div className="mt-5 flex flex-wrap gap-2">
                <DatoDelPie
                  valor={formato.format(cifras.servidores)}
                  etiqueta="servidores"
                />
                <DatoDelPie
                  valor={formato.format(cifras.jugadores)}
                  etiqueta="jugando ahora"
                  viva
                />
                <DatoDelPie
                  valor={formato.format(cifras.votos24h)}
                  etiqueta="votos hoy"
                />
              </div>
            )}
          </div>

          <ColumnaDelPie titulo="Para jugar">
            <EnlaceDelPie href="/servidores">Todos los servidores</EnlaceDelPie>
            <EnlaceDelPie href="/votar">Votar con mi código</EnlaceDelPie>
            <EnlaceDelPie href="/servidores?modalidad=SURVIVAL">
              Survival
            </EnlaceDelPie>
            <EnlaceDelPie href="/servidores?modalidad=SKYBLOCK">
              SkyBlock
            </EnlaceDelPie>
          </ColumnaDelPie>

          <ColumnaDelPie titulo="Para tu servidor">
            <EnlaceDelPie href="/registro">Añadir mi servidor</EnlaceDelPie>
            <EnlaceDelPie href="/plugin">Cómo funciona el plugin</EnlaceDelPie>
            <EnlaceDelPie href="/panel">Mi panel</EnlaceDelPie>
            {/* Al panel y no a una página pública de venta: destacar es algo que
                se hace sobre un servidor propio ya registrado, así que la puerta
                de entrada es la sesión. */}
            <EnlaceDelPie href="/panel/destacar">
              Destacar mi servidor
            </EnlaceDelPie>
            {sitio.urlPlugin && (
              <li>
                <a
                  href={sitio.urlPlugin}
                  target="_blank"
                  rel="noopener"
                  className="enlace-vivo text-muted transition-colors hover:text-text"
                >
                  Descargar el plugin
                </a>
              </li>
            )}
          </ColumnaDelPie>
        </div>

        <div className="mt-12 flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-border pt-6 text-xs text-subtle">
          <span>
            © {año} {sitio.nombre}
          </span>
          <span className="hidden sm:inline" aria-hidden>
            ·
          </span>
          <span>Jugadores y estado medidos por nosotros con ping directo.</span>
          <span className="ml-auto">
            No estamos afiliados a Mojang ni a Microsoft.
          </span>
        </div>
      </div>
    </footer>
  );
}

function ColumnaDelPie({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <h2 className="nav-display mb-4 text-[10px] text-subtle">{titulo}</h2>
      <ul className="space-y-2.5 text-sm">{children}</ul>
    </div>
  );
}

function EnlaceDelPie({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) {
  return (
    <li>
      <Link
        href={href}
        className="enlace-vivo text-muted transition-colors hover:text-text"
      >
        {children}
      </Link>
    </li>
  );
}

/** Cifra del pie. El punto que late solo va en la que cambia sola. */
function DatoDelPie({
  valor,
  etiqueta,
  viva,
}: {
  valor: string;
  etiqueta: string;
  viva?: boolean;
}) {
  return (
    <div className="flex items-center gap-1.5 rounded-full border border-border bg-surface px-3 py-1 text-xs">
      {viva && (
        <span className="late size-1.5 rounded-full bg-success" aria-hidden />
      )}
      <span className="cifras font-semibold">{valor}</span>
      <span className="text-muted">{etiqueta}</span>
    </div>
  );
}
