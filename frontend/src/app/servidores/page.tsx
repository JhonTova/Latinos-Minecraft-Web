import { BarraLateral } from "@/components/barra-lateral";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import {
  FilaServidor,
  FilasCargando,
  SinServidores,
} from "@/components/fila-servidor";
import { BuscadorDeServidores } from "@/components/buscador";
import { catalogoDeModalidades, obtenerRanking } from "@/lib/api";
import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";

/**
 * El canónico depende del filtro que haya puesto.
 *
 * `/servidores?modalidad=SKYBLOCK` enseña lo mismo que
 * `/servidores/modalidad/skyblock`, que sí es una página con su título y su
 * texto. Sin decirlo, Google ve dos direcciones con el mismo contenido, elige
 * una por su cuenta y reparte la fuerza entre las dos. Con el canónico apuntando
 * a la buena, todo lo que llegue aquí con un filtro empuja a la que puede
 * posicionar.
 */
export async function generateMetadata(
  props: PageProps<"/servidores">,
): Promise<Metadata> {
  const parametros = await props.searchParams;
  const modalidad =
    typeof parametros.modalidad === "string" ? parametros.modalidad : null;

  return {
    title: "Todos los servidores",
    description:
      "Listado completo de servidores de Minecraft en español: survival, skyblock, PvP, factions y más. Con IP, versión y jugadores conectados.",
    alternates: {
      canonical: modalidad
        ? `/servidores/modalidad/${modalidad.toLowerCase()}`
        : "/servidores",
    },
    // Una búsqueda no es una página: hay infinitas y ninguna aporta nada en un
    // buscador. Se enseñan igual, pero no se indexan.
    robots:
      typeof parametros.q === "string" && parametros.q
        ? { index: false, follow: true }
        : undefined,
  };
}

export default async function PaginaServidores(
  props: PageProps<"/servidores">,
) {
  const parametros = await props.searchParams;
  const modalidad =
    typeof parametros.modalidad === "string" ? parametros.modalidad : null;
  const busqueda = typeof parametros.q === "string" ? parametros.q : null;
  const cuentas =
    parametros.cuentas === "premium" || parametros.cuentas === "todas"
      ? parametros.cuentas
      : null;
  const bedrock = parametros.bedrock === "1";

  return (
    <>
      <Cabecera />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-20 sm:px-6">
        <div className="py-8 sm:py-10">
          <h1 className="display text-3xl font-semibold">Servidores</h1>
          <p className="mt-2 text-muted text-pretty">
            Ordenados por votos verificados de los últimos 7 días.
          </p>

          <div className="mt-5 max-w-xl">
            <BuscadorDeServidores valorInicial={busqueda ?? ""} />
          </div>
        </div>

        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_290px] lg:items-start">
          <div className="min-w-0">
            <Suspense fallback={<FiltrosCargando />}>
              <Filtros
                activa={modalidad}
                cuentas={cuentas}
                bedrock={bedrock}
                busqueda={busqueda}
              />
            </Suspense>

            <Suspense fallback={<FilasCargando cuantas={10} />}>
              <Listado
                modalidad={modalidad}
                busqueda={busqueda}
                cuentas={cuentas}
                bedrock={bedrock}
              />
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
 * Filtro por modalidad.
 *
 * Enlaces y no un desplegable con JavaScript: cada filtro tiene su propia URL,
 * así que se puede compartir, guardar en marcadores y —lo que más importa—
 * Google lo indexa. «Servidores de skyblock en español» es exactamente lo que la
 * gente escribe en el buscador, y una página por modalidad es la forma de
 * aparecer ahí.
 */
async function Filtros({
  activa,
  cuentas,
  bedrock,
  busqueda,
}: {
  activa: string | null;
  cuentas: string | null;
  bedrock: boolean;
  busqueda: string | null;
}) {
  const catalogo = await catalogoDeModalidades().catch(() => []);

  /**
   * Construye la URL cambiando un filtro y conservando los demás.
   *
   * Sin esto, elegir "solo premium" borraría la modalidad que ya estaba puesta,
   * y quien quiera "skyblock premium desde el móvil" no puede llegar nunca:
   * cada clic desharía el anterior.
   */
  const con = (cambios: Record<string, string | null>) => {
    const p = new URLSearchParams();
    if (busqueda) p.set("q", busqueda);
    if (activa) p.set("modalidad", activa);
    if (cuentas) p.set("cuentas", cuentas);
    if (bedrock) p.set("bedrock", "1");

    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor === null) p.delete(clave);
      else p.set(clave, valor);
    }
    const cadena = p.toString();
    return cadena ? `/servidores?${cadena}` : "/servidores";
  };

  return (
    <div className="mb-5 space-y-3">
      {/* --- Quién puede entrar ------------------------------------------
          Va primero porque es el filtro que más recortes hace y el que más le
          importa al jugador: sin cuenta original, un servidor premium no es que
          le convenga menos, es que no puede entrar (ADR-0030). */}
      <div>
        <p className="mb-1.5 text-xs font-medium text-subtle">Quién puede entrar</p>
        <div className="flex flex-wrap gap-1.5">
          <Filtro href={con({ cuentas: null })} activo={!cuentas}>
            Todos
          </Filtro>
          <Filtro href={con({ cuentas: "premium" })} activo={cuentas === "premium"}>
            Solo premium
          </Filtro>
          <Filtro href={con({ cuentas: "todas" })} activo={cuentas === "todas"}>
            Premium y no premium
          </Filtro>
          <Filtro
            href={con({ bedrock: bedrock ? null : "1" })}
            activo={bedrock}
          >
            Móvil y consola
          </Filtro>
        </div>
      </div>

      {/* --- Modalidad -------------------------------------------------- */}
      {catalogo.length > 0 && (
        <div>
          <p className="mb-1.5 text-xs font-medium text-subtle">Modalidad</p>
          <div className="flex flex-wrap gap-1.5">
            <Filtro href={con({ modalidad: null })} activo={!activa}>
              Todas
            </Filtro>
            {catalogo.map((m) => (
              <Filtro
                key={m.codigo}
                // Sin otros filtros puestos se va a la página propia de la
                // modalidad, que sí tiene título y texto y es la que Google
                // puede posicionar. Con filtros, a la URL con parámetros: esa
                // combinación no es una página, es una búsqueda.
                href={
                  !cuentas && !bedrock && !busqueda
                    ? `/servidores/modalidad/${m.codigo.toLowerCase()}`
                    : con({ modalidad: m.codigo })
                }
                activo={activa === m.codigo}
              >
                {m.nombre}
              </Filtro>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function Filtro({
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

function FiltrosCargando() {
  return (
    <div className="mb-5 flex flex-wrap gap-1.5">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="cargando h-8 w-20 rounded-full" />
      ))}
    </div>
  );
}

async function Listado({
  modalidad,
  busqueda,
  cuentas,
  bedrock,
}: {
  modalidad: string | null;
  busqueda: string | null;
  cuentas: string | null;
  bedrock: boolean;
}) {
  await connection();
  const todos = await obtenerRanking();

  // El filtro se aplica aquí y no en la consulta porque el ranking ya viene
  // cacheado y completo: filtrar en memoria evita una consulta distinta por cada
  // modalidad, que multiplicaría por dieciséis la carga de la base de datos para
  // devolver un subconjunto de algo que ya teníamos.
  //
  // Deja de ser buena idea cuando haya miles de servidores; entonces el filtro
  // baja a SQL con su índice, que ya existe.
  const catalogo = modalidad ? await catalogoDeModalidades().catch(() => []) : [];
  const nombre = catalogo.find((m) => m.codigo === modalidad)?.nombre;

  let servidores = nombre
    ? todos.filter((s) => s.modalidades.includes(nombre))
    : todos;

  // Aquí los servidores salen MEZCLADOS y es el jugador quien decide (ADR-0030).
  // La separación estricta es cosa de los dos rankings de la portada; en el
  // listado completo, obligarle a elegir un mundo antes de poder buscar sería
  // ponerle una puerta delante de la puerta.
  if (cuentas === "premium") {
    servidores = servidores.filter((s) => !s.aceptaNoPremium);
  } else if (cuentas === "todas") {
    servidores = servidores.filter((s) => s.aceptaNoPremium);
  }

  if (bedrock) {
    servidores = servidores.filter((s) => s.soportaBedrock);
  }

  if (busqueda) {
    // Se normaliza quitando acentos: quien busca "anarquia" espera encontrar
    // "Anarquía", y obligarle a escribir la tilde es una forma tonta de no
    // devolver resultados que sí existen.
    const aguja = normalizar(busqueda);
    servidores = servidores.filter(
      (s) =>
        normalizar(s.nombre).includes(aguja) ||
        normalizar(s.descripcion).includes(aguja) ||
        normalizar(s.direccion).includes(aguja),
    );
  }

  if (servidores.length === 0) {
    if (busqueda) return <SinCoincidencias busqueda={busqueda} />;
    return modalidad ? <SinResultados modalidad={nombre ?? modalidad} /> : <SinServidores />;
  }

  return (
    <>
      <p className="mb-3 text-sm text-subtle">
        {servidores.length}{" "}
        {servidores.length === 1 ? "servidor" : "servidores"}
        {nombre && ` de ${nombre}`}
        {busqueda && ` para «${busqueda}»`}
      </p>

      <ul className="space-y-2">
        {servidores.map((servidor, i) => (
          <FilaServidor key={servidor.id} posicion={i + 1} servidor={servidor} />
        ))}
      </ul>
    </>
  );
}

/** Quitar acentos: "anarquia" tiene que encontrar "Anarquía". */
function normalizar(texto: string): string {
  return texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "");
}

function SinCoincidencias({ busqueda }: { busqueda: string }) {
  return (
    <div className="rounded-lg border border-dashed border-border py-16 text-center">
      <h2 className="font-semibold">Sin resultados para «{busqueda}»</h2>
      <p className="mx-auto mt-1.5 max-w-sm text-sm text-muted text-pretty">
        Prueba con menos palabras, o mira el listado completo.
      </p>
      <Link
        href="/servidores"
        className="boton-vivo mt-5 inline-block rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium hover:bg-surface-sunken"
      >
        Ver todos los servidores
      </Link>
    </div>
  );
}

function SinResultados({ modalidad }: { modalidad: string }) {
  return (
    <div className="rounded-lg border border-dashed border-border py-16 text-center">
      <h2 className="font-semibold">Todavía no hay servidores de {modalidad}</h2>
      <p className="mx-auto mt-1.5 max-w-sm text-sm text-muted text-pretty">
        Si tienes uno, sé el primero en aparecer aquí.
      </p>
      <div className="mt-5 flex flex-wrap justify-center gap-2">
        <Link
          href="/registro"
          className="boton-vivo rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover"
        >
          Añadir mi servidor
        </Link>
        <Link
          href="/servidores"
          className="boton-vivo rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium hover:bg-surface-sunken"
        >
          Ver todos
        </Link>
      </div>
    </div>
  );
}
