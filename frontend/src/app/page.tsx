import { BarraLateral } from "@/components/barra-lateral";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import {
  DatosEstructurados,
  esquemaDeRanking,
  esquemaDelSitio,
} from "@/components/datos-estructurados";
import {
  FilaServidor,
  FilasCargando,
  SinServidores,
} from "@/components/fila-servidor";
import { FranjaDeCifras, LlamadaParaDuenos } from "@/components/franjas";
import {
  RailDeMarcas,
  TiraDeMarcas,
  repartirEnRailes,
} from "@/components/patrocinadores";
import { Heroe } from "@/components/heroe";
import { TarjetaDestacado } from "@/components/tarjeta-destacado";
import {
  ajustesDelSitio,
  destacadosDePortada,
  estadisticas,
  obtenerTopsSeparados,
  patrocinadores,
  type SlotEnPortada,
} from "@/lib/api";
import type { ServidorEnRanking } from "@/lib/tipos";
import { TopsSeparados } from "@/components/tops-separados";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";

/**
 * Portada.
 *
 * **La lista va arriba.** Es la corrección más importante respecto al diseño
 * anterior, que tenía una portada a pantalla completa antes de enseñar un solo
 * servidor. Quien entra a un directorio no viene a leer una promesa: viene a ver
 * dónde jugar. Los directorios que llevan años funcionando ponen una línea de
 * presentación y, justo debajo, los servidores.
 *
 * El orden vertical es el argumento del producto:
 *
 *   1. Destacados — pagados, etiquetados, en su propio marco
 *   2. Ranking    — orgánico, empieza en el #1, independiente de lo anterior
 *
 * Si algún día alguien propone meter los destacados dentro del top 5 «para que
 * se vean más», la respuesta es no: el ranking vale porque no se puede comprar,
 * y eso es el producto.
 *
 * **Las marcas patrocinadoras van a los lados**, no en una fila perdida al
 * final. En pantalla ancha sobra sitio a izquierda y derecha, y un banner
 * vertical ahí acompaña al visitante mientras recorre el ranking. Empiezan a la
 * altura de los servidores y no del titular: la publicidad no se pone por
 * delante de lo que la gente ha venido a ver.
 */
export default async function Page() {
  const [sitio, cifras, marcas] = await Promise.all([
    ajustesDelSitio(),
    estadisticas(),
    // Se pide aquí y no dentro de un Suspense porque decide la FORMA de la
    // página: con marcas hay tres columnas, sin ellas una. Resolverlo después
    // significaría mover el contenido cuando ya se está leyendo.
    patrocinadores().catch(() => []),
  ]);

  const { izquierda, derecha } = repartirEnRailes(marcas);
  const conRailes = marcas.length > 0;

  return (
    <>
      <DatosEstructurados
        datos={esquemaDelSitio(sitio.nombre, sitio.descripcion)}
      />

      <Cabecera />

      <Heroe nombre={sitio.nombre} logoUrl={sitio.logoUrl} cifras={cifras} />

      {/* 96rem = los 72rem del contenido más los dos raíles con su separación.
          Así, en pantallas grandes, la columna central mide exactamente lo mismo
          que el héroe de arriba y nada se desalinea. */}
      <div
        className={`mx-auto w-full flex-1 px-4 pb-20 sm:px-6 ${
          conRailes ? "max-w-[96rem]" : "max-w-6xl"
        }`}
      >
        <div
          className={
            conRailes
              ? "grid gap-6 2xl:grid-cols-[9rem_minmax(0,1fr)_9rem]"
              : ""
          }
        >
          {conRailes && <RailDeMarcas marcas={izquierda} lado="izquierda" />}

          <main className="mx-auto w-full max-w-6xl min-w-0 space-y-10">
            <Suspense fallback={null}>
              <Destacados />
            </Suspense>

            <FranjaDeCifras cifras={cifras} />

            {/* Los dos rankings ocupan el ancho completo de la columna. Antes la
                barra lateral compartía fila con ellos; con DOS listas al lado,
                cada una se quedaba en unos 380 píxeles y las filas —icono,
                nombre, etiquetas, votos— dejan de leerse ahí. */}
            <Suspense fallback={<RankingCargando />}>
              <Ranking />
            </Suspense>

            <BarraLateral disposicion="fila" llamadaParaDuenos={false} />

            {/* Solo cuando no caben los raíles; se esconde con 2xl:hidden. */}
            <TiraDeMarcas marcas={marcas} />

            <LlamadaParaDuenos />
          </main>

          {conRailes && <RailDeMarcas marcas={derecha} lado="derecha" />}
        </div>
      </div>

      <PieDePagina />
    </>
  );
}

/* ========================================================================== */

/**
 * Bloque de espacios comprados (ADR-0013).
 *
 * Va dentro de su propio marco tintado, con encabezado y etiqueta de publicidad.
 * Que sea un recuadro entero y no unas tarjetas sueltas es lo que hace que la
 * separación respecto al ranking se vea de un vistazo, sin tener que leer nada.
 */
async function Destacados() {
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
    ocupados.length === 0
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
        <div className="flex items-center gap-2.5">
          <h2 id="titulo-destacados" className="display text-lg font-semibold">
            Servidores patrocinados
          </h2>
          {/* La etiqueta es texto real, no solo un color: quien no distingue
              colores también tiene que poder saber que esto es publicidad. */}
          <span className="nav-display rounded-full border border-primary/40 bg-primary/10 px-2 py-0.5 text-[9px] text-primary">
            Publicidad
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
          <p className="text-xs text-subtle">
            Espacios comprados. No cuentan para el ranking.
          </p>
          <Link
            href="/panel/destacar"
            className="enlace-vivo text-sm font-medium text-primary"
          >
            Destacar el mío
          </Link>
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
          Coloca aquí tu servidor y sale en lo primero que se ve
        </span>
      </Link>
    </li>
  );
}

/* ========================================================================== */

/**
 * Los dos rankings separados (ADR-0030).
 *
 * No se mezclan porque en offline-mode la identidad del votante no es débil: no
 * existe. Se vota escribiendo un nombre. Comparar los dos mundos en una sola
 * tabla haría que el ranking midiera lo fácil que es falsificar votos, y el
 * producto es justo lo contrario.
 *
 * Pero el argumento que se le da al visitante no es ese, es más simple y más
 * cierto: **están separados para que encuentres dónde puedes jugar de verdad.**
 * Quien no tiene cuenta original no es que lo pase peor en un servidor premium,
 * es que el servidor le rechaza la conexión.
 */
async function Ranking() {
  await connection();
  const { premium, mixto } = await obtenerTopsSeparados();

  if (premium.length === 0 && mixto.length === 0) {
    return <SinServidores />;
  }

  return (
    <section aria-labelledby="titulo-ranking">
      {/* Sin esto Google ve enlaces sueltos; con esto entiende que es un ranking
          y que el orden significa algo. */}
      <DatosEstructurados datos={esquemaDeRanking([...premium, ...mixto])} />

      <CabeceraDelRanking />

      <TopsSeparados
        premium={
          <ListaDelTop
            titulo="Solo cuentas premium"
            explicacion="Hace falta una cuenta original de Minecraft para entrar."
            servidores={premium}
            vacio="Todavía no hay servidores en esta lista."
          />
        }
        mixto={
          <ListaDelTop
            titulo="Cuentas premium y no premium"
            explicacion="Se puede entrar con cuenta original o sin ella."
            servidores={mixto}
            vacio="Todavía no hay servidores en esta lista."
          />
        }
      />
    </section>
  );
}

/**
 * Encabezado del ranking.
 *
 * Un `<h2>` a secas encima de dos listas quedaba flotando: no se veía dónde
 * empezaba la sección ni por qué esos servidores están en ese orden. Aquí el
 * título va con su explicación dentro de una pieza cerrada, que es lo que hace
 * que se lea como el bloque principal de la página y no como una etiqueta.
 */
function CabeceraDelRanking() {
  return (
    <div className="mb-5 overflow-hidden rounded-xl border border-border bg-surface-sunken/60">
      {/* Filete de marca: media docena de píxeles que separan esta sección de la
          de publicidad de arriba sin necesidad de más texto. */}
      <div
        aria-hidden
        className="h-0.5 w-full"
        style={{
          background:
            "linear-gradient(90deg, var(--primary), var(--oro-400) 70%, transparent)",
        }}
      />

      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-4">
        <span
          aria-hidden
          className="grid size-10 shrink-0 place-items-center rounded-lg bg-primary-subtle text-primary"
        >
          <svg viewBox="0 0 24 24" className="size-5">
            <path
              d="M8 21h8m-4-4v4m-5-17h10v4a5 5 0 0 1-10 0V4Zm10 1h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>

        <div className="min-w-0 flex-1">
          <h2 id="titulo-ranking" className="display text-xl font-semibold">
            Ranking de servidores
          </h2>
          <p className="mt-0.5 text-sm text-muted text-pretty">
            Por votos verificados de los últimos 7 días. Nadie compra un puesto
            aquí.
          </p>
        </div>

        <Link
          href="/servidores"
          className="boton-vivo shrink-0 rounded-md border border-border bg-surface px-3.5 py-2 text-sm font-medium hover:border-primary hover:text-primary"
        >
          Ver todos
        </Link>
      </div>
    </div>
  );
}

function ListaDelTop({
  titulo,
  explicacion,
  servidores,
  vacio,
}: {
  titulo: string;
  explicacion: string;
  servidores: ServidorEnRanking[];
  vacio: string;
}) {
  return (
    <div>
      <div className="mb-3">
        <h3 className="font-semibold">{titulo}</h3>
        {/* La explicación va siempre, no solo en un tooltip. Es la diferencia
            entre que alguien entienda la separación y que le parezca arbitraria. */}
        <p className="mt-0.5 text-sm text-muted text-pretty">{explicacion}</p>
      </div>

      {servidores.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border py-10 text-center text-sm text-muted">
          {vacio}
        </div>
      ) : (
        <ul className="space-y-2">
          {servidores.slice(0, 10).map((servidor, i) => (
            <FilaServidor
              key={servidor.id}
              posicion={i + 1}
              servidor={servidor}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

function RankingCargando() {
  return (
    <section aria-label="Cargando ranking">
      <div className="cargando mb-5 h-20 w-full rounded-xl" />
      <FilasCargando cuantas={8} />
    </section>
  );
}
