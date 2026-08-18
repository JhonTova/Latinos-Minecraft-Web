import { BarraLateral } from "@/components/barra-lateral";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import { FilaServidor, FilasCargando } from "@/components/fila-servidor";
import { catalogoDeModalidades, obtenerRankingDe } from "@/lib/api";
import { categoriaDe } from "@/lib/categorias";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";

/**
 * Una página por modalidad, con su propia dirección y su propio texto.
 *
 * Ya existía el filtro `?modalidad=SKYBLOCK`, pero para un buscador eso no es
 * una página: es `/servidores` con un parámetro. Mismo título, misma
 * descripción, mismo contenido salvo la lista — y Google la descarta por
 * duplicada, así que no aparecía por «servidores de skyblock en español» aunque
 * fuera exactamente lo que enseñaba.
 *
 * Lo que la convierte en una página que puede posicionar son tres cosas: una
 * dirección propia, un título y una descripción escritos para lo que la gente
 * busca, y texto de verdad sobre la modalidad. Las tres están aquí.
 *
 * La ruta lleva `/modalidad/` en medio a propósito. `/servidores/skyblock`
 * chocaría con la ficha de un servidor cuyo identificador fuera «skyblock», y
 * resolver esa ambigüedad con una lista de palabras reservadas es una trampa que
 * salta el día que alguien registra el servidor equivocado.
 */

async function modalidad(codigo: string) {
  const catalogo = await catalogoDeModalidades().catch(() => []);
  const encontrada = catalogo.find(
    (m) => m.codigo.toLowerCase() === codigo.toLowerCase(),
  );
  return encontrada ?? null;
}

export async function generateMetadata(
  props: PageProps<"/servidores/modalidad/[codigo]">,
): Promise<Metadata> {
  const { codigo } = await props.params;
  const encontrada = await modalidad(codigo);

  if (!encontrada) {
    return { title: "Modalidad no encontrada", robots: { index: false } };
  }

  const categoria = categoriaDe(encontrada.codigo, encontrada.nombre);

  return {
    title: categoria.titulo,
    description: categoria.descripcion,
    alternates: {
      canonical: `/servidores/modalidad/${encontrada.codigo.toLowerCase()}`,
    },
    openGraph: {
      type: "website",
      locale: "es_ES",
      title: categoria.titulo,
      description: categoria.descripcion,
      url: `/servidores/modalidad/${encontrada.codigo.toLowerCase()}`,
    },
  };
}

export default async function PaginaDeModalidad(
  props: PageProps<"/servidores/modalidad/[codigo]">,
) {
  const { codigo } = await props.params;
  const encontrada = await modalidad(codigo);

  // 404 y no una lista vacía: una dirección inventada no puede devolver algo que
  // parezca una página real, o Google acabaría indexando basura.
  if (!encontrada) {
    notFound();
  }

  const categoria = categoriaDe(encontrada.codigo, encontrada.nombre);

  return (
    <>
      <Cabecera />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-20 sm:px-6">
        <nav
          aria-label="Migas de pan"
          className="pt-8 text-sm text-subtle sm:pt-10"
        >
          <Link href="/servidores" className="transition-colors hover:text-text">
            Servidores
          </Link>
          <span className="mx-1.5">/</span>
          <span className="text-muted">{encontrada.nombre}</span>
        </nav>

        <div className="py-5">
          <h1 className="display text-3xl font-semibold text-balance">
            {categoria.h1} <span className="text-muted">en español</span>
          </h1>

          <div className="mt-3 max-w-2xl space-y-3">
            {categoria.intro.map((parrafo) => (
              <p key={parrafo.slice(0, 24)} className="text-muted text-pretty">
                {parrafo}
              </p>
            ))}
          </div>
        </div>

        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_290px] lg:items-start">
          <div className="min-w-0">
            <Suspense fallback={<OtrasCargando />}>
              <Otras activa={encontrada.codigo} />
            </Suspense>

            <Suspense fallback={<FilasCargando cuantas={10} />}>
              <Listado codigo={encontrada.codigo} nombre={encontrada.nombre} />
            </Suspense>
          </div>

          <div className="lg:sticky lg:top-28">
            <BarraLateral />
          </div>
        </div>
      </main>

      <PieDePagina />
    </>
  );
}

/**
 * Las demás modalidades, como enlaces.
 *
 * Además de servir para navegar, es lo que hace que un buscador llegue a todas:
 * una página a la que no apunta ningún enlace es una página que Google tarda
 * meses en encontrar, o no encuentra.
 */
async function Otras({ activa }: { activa: string }) {
  const catalogo = await catalogoDeModalidades().catch(() => []);
  if (catalogo.length === 0) return null;

  return (
    <div className="mb-5 flex flex-wrap gap-1.5">
      <Enlace href="/servidores" activo={false}>
        Todas
      </Enlace>
      {catalogo.map((m) => (
        <Enlace
          key={m.codigo}
          href={`/servidores/modalidad/${m.codigo.toLowerCase()}`}
          activo={m.codigo === activa}
        >
          {m.nombre}
        </Enlace>
      ))}
    </div>
  );
}

function Enlace({
  href,
  activo,
  children,
}: {
  href: string;
  activo: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-current={activo ? "page" : undefined}
      className={`rounded-full border px-3 py-1.5 text-sm transition-colors ${
        activo
          ? "border-primary bg-primary text-on-accent"
          : "border-border bg-surface text-muted hover:border-border-strong hover:bg-surface-sunken hover:text-text"
      }`}
    >
      {children}
    </Link>
  );
}

function OtrasCargando() {
  return (
    <div className="mb-5 flex flex-wrap gap-1.5">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="cargando h-8 w-20 rounded-full" />
      ))}
    </div>
  );
}

async function Listado({ codigo, nombre }: { codigo: string; nombre: string }) {
  await connection();

  // El filtro se pide a la API, no se aplica sobre el ranking completo. Filtrar
  // en memoria devolvería solo los de esta modalidad que ya estuvieran entre los
  // 50 mejores del total: una modalidad con cuarenta servidores, ninguno en el
  // top general, se vería vacía.
  const servidores = await obtenerRankingDe(codigo).catch(() => []);

  if (servidores.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border px-6 py-14 text-center">
        <p className="font-medium">
          Todavía no hay servidores de {nombre} en el directorio
        </p>
        <p className="mx-auto mt-1 max-w-md text-sm text-muted text-pretty">
          Si tienes uno, es un buen momento para darlo de alta: serás el primero
          de la lista.
        </p>
        <Link
          href="/panel/servidores/nuevo"
          className="mt-5 inline-block rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent transition-colors hover:bg-primary-hover"
        >
          Añadir mi servidor
        </Link>
      </div>
    );
  }

  return (
    <>
      <p className="mb-3 text-sm text-subtle">
        {servidores.length} {servidores.length === 1 ? "servidor" : "servidores"}{" "}
        de {nombre}, por votos de los últimos 7 días
      </p>

      <ul className="space-y-2">
        {servidores.map((servidor, i) => (
          <FilaServidor key={servidor.id} posicion={i + 1} servidor={servidor} />
        ))}
      </ul>
    </>
  );
}
