import { BarraLateral } from "@/components/barra-lateral";
import { EncabezadoDeBloque } from "@/components/encabezado-de-bloque";
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
import { MarcasPatrocinadoras } from "@/components/patrocinadores";
import { Heroe } from "@/components/heroe";
import { TarjetaDestacado } from "@/components/tarjeta-destacado";
import {
  ajustesDelSitio,
  destacadosDePortada,
  estadisticas,
  obtenerRanking,
  type SlotEnPortada,
} from "@/lib/api";
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
 *   1. Destacados — pagados, etiquetados, separados
 *   2. Ranking    — orgánico, empieza en el #1, independiente de lo anterior
 *
 * Si algún día alguien propone meter los destacados dentro del top 5 «para que
 * se vean más», la respuesta es no: el ranking vale porque es honesto, y esa
 * honestidad es el producto.
 */
export default async function Page() {
  const [sitio, cifras] = await Promise.all([ajustesDelSitio(), estadisticas()]);

  return (
    <>
      <DatosEstructurados
        datos={esquemaDelSitio(sitio.nombre, sitio.descripcion)}
      />

      <Cabecera />

      <Heroe nombre={sitio.nombre} logoUrl={sitio.logoUrl} cifras={cifras} />

      <main className="mx-auto w-full max-w-6xl flex-1 space-y-10 px-4 pb-20 sm:px-6">
        <Suspense fallback={null}>
          <Destacados />
        </Suspense>

        <FranjaDeCifras cifras={cifras} />

        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_290px] lg:items-start">
          <div className="min-w-0">
            <Suspense fallback={<RankingCargando />}>
              <Ranking />
            </Suspense>
          </div>

          <div className="lg:sticky lg:top-28">
            <BarraLateral />
          </div>
        </div>

        <Suspense fallback={null}>
          <MarcasPatrocinadoras />
        </Suspense>

        <LlamadaParaDuenos />
      </main>

      <PieDePagina />
    </>
  );
}


/* ========================================================================== */

async function Destacados() {
  await connection();
  const slots = await destacadosDePortada().catch((): SlotEnPortada[] => []);
  // `!= null` con doble igual, no `!==`: descarta null Y undefined de una vez.
  // Es el caso que rompio la portada — un campo ausente no es null, y el filtro
  // dejaba pasar los huecos vacios para reventar dos lineas mas abajo.
  const ocupados = slots.filter((s) => s.servidor != null);

  // Con la plataforma vacía se enseña UN solo hueco como oferta, no los cinco.
  // Cinco recuadros vacíos en lo primero que se ve son un cartel de «aquí no hay
  // nada»; uno solo se lee como una invitación.
  const huecos = ocupados.length === 0 ? 1 : Math.min(3 - ocupados.length, 2);

  return (
    <section aria-labelledby="titulo-destacados">
      <EncabezadoDeBloque
        id="titulo-destacados"
        titulo="Servidores patrocinados"
        etiqueta="Patrocinado"
        enlace={{ href: "/panel/destacar", texto: "Destacar el mío" }}
      />

      {/* Tarjetas y no filas: son cinco, no cincuenta, y hay sitio para una
          imagen grande. Ese sitio para respirar es exactamente lo que se paga. */}
      <ul className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {ocupados.map((slot) => (
          <TarjetaDestacado
            key={slot.posicion}
            posicion={slot.posicion}
            servidor={slot.servidor!}
            jugadores={null}
          />
        ))}

        {huecos > 0 &&
          slots
            .filter((s) => s.servidor == null)
            .slice(0, huecos)
            .map((slot) => <HuecoLibre key={slot.posicion} slot={slot.posicion} />)}
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
        className="group flex min-h-[168px] flex-1 flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-border p-5 text-center transition-colors hover:border-primary hover:bg-primary-subtle"
      >
        <span className="grid size-9 place-items-center rounded-full border border-dashed border-border text-lg text-subtle transition-colors group-hover:border-primary group-hover:text-primary">
          +
        </span>
        <span className="mt-1 text-sm font-medium transition-colors group-hover:text-primary">
          Espacio {slot} libre
        </span>
        <span className="text-xs text-subtle text-pretty">
          Coloca aquí tu servidor
        </span>
      </Link>
    </li>
  );
}

/* ========================================================================== */

async function Ranking() {
  await connection();
  const servidores = await obtenerRanking();

  if (servidores.length === 0) {
    return <SinServidores />;
  }

  return (
    <section aria-labelledby="titulo-ranking">
      {/* Sin esto Google ve cincuenta enlaces sueltos; con esto entiende que es
          un ranking y que el orden significa algo. */}
      <DatosEstructurados datos={esquemaDeRanking(servidores)} />

      <EncabezadoDeBloque
        id="titulo-ranking"
        titulo="Ranking"
        nota="Votos verificados de los últimos 7 días"
      />

      <ul className="space-y-2">
        {servidores.slice(0, 25).map((servidor, i) => (
          <FilaServidor key={servidor.id} posicion={i + 1} servidor={servidor} />
        ))}
      </ul>

      {servidores.length > 25 && (
        <div className="mt-5 text-center">
          <Link
            href="/servidores"
            className="boton-vivo inline-block rounded-md border border-border bg-surface px-5 py-2.5 text-sm font-medium hover:border-border-strong hover:bg-surface-sunken"
          >
            Ver los {servidores.length} servidores
          </Link>
        </div>
      )}
    </section>
  );
}

function RankingCargando() {
  return (
    <section aria-label="Cargando ranking">
      <div className="cargando mb-4 h-6 w-28 rounded" />
      <FilasCargando cuantas={8} />
    </section>
  );
}

