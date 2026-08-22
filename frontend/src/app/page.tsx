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
  BandaDePatrocinadores,
  RailDePatrocinadores,
} from "@/components/patrocinadores";
import { Heroe } from "@/components/heroe";
import { ServidoresPatrocinados } from "@/components/servidores-patrocinados";
import {
  ajustesDelSitio,
  estadisticas,
  obtenerTopsSeparados,
  patrocinadores,
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
 * **Los patrocinadores no tocan la forma de la página.** En pantallas de 1600 px
 * o más van en un raíl colgado del margen derecho; por debajo, en una banda tras
 * el ranking. La columna de contenido mide lo mismo en los dos casos, se hayan
 * vendido banners o no.
 */
export default async function Page() {
  const [sitio, cifras, marcas] = await Promise.all([
    ajustesDelSitio(),
    estadisticas(),
    patrocinadores().catch(() => []),
  ]);

  return (
    <>
      <DatosEstructurados
        datos={esquemaDelSitio(sitio.nombre, sitio.descripcion)}
      />

      <Cabecera />

      <Heroe nombre={sitio.nombre} logoUrl={sitio.logoUrl} cifras={cifras} />

      {/* Mismo ancho que el héroe de arriba, haya patrocinadores o no: la
          portada no puede cambiar de forma según lo que se haya vendido.
          `relative` es lo que ancla el raíl a este margen. */}
      <div className="relative mx-auto w-full max-w-6xl flex-1 px-4 pb-20 sm:px-6">
        <RailDePatrocinadores marcas={marcas} />

        <main className="w-full min-w-0 space-y-10">
          <Suspense fallback={null}>
            <ServidoresPatrocinados />
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

          <BandaDePatrocinadores marcas={marcas} />

          <LlamadaParaDuenos />
        </main>
      </div>

      <PieDePagina />
    </>
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
            titulo="Java Edition Premium"
            explicacion="Hace falta una cuenta original de Minecraft para entrar."
            servidores={premium}
            vacio="Todavía no hay servidores en esta lista."
          />
        }
        mixto={
          <ListaDelTop
            titulo="Java Edition No Premium"
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
            Por votos verificados de los últimos 7 días.
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
