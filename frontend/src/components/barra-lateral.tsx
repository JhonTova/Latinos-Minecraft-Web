import { MarcasEnLateral } from "@/components/patrocinadores";
import Link from "next/link";

/**
 * Modalidades más buscadas.
 *
 * No es decoración: es la segunda forma de navegar el sitio. Mucha gente no
 * llega buscando «un servidor», llega buscando «un skyblock», y sin estos
 * atajos la única salida es leerse la lista entera.
 *
 * Son un subconjunto del catálogo, elegido por lo que la gente busca de verdad.
 * Poner las dieciséis convertiría el atajo en otra lista que hay que leer.
 */
const CATEGORIAS = [
  { codigo: "SURVIVAL", nombre: "Survival", emoji: "🌳" },
  { codigo: "SKYBLOCK", nombre: "SkyBlock", emoji: "☁️" },
  { codigo: "PVP", nombre: "PvP", emoji: "⚔️" },
  { codigo: "FACTIONS", nombre: "Factions", emoji: "🏰" },
  { codigo: "BEDWARS", nombre: "BedWars", emoji: "🛏️" },
  { codigo: "MINIGAMES", nombre: "Minijuegos", emoji: "🎮" },
  { codigo: "PRISON", nombre: "Prison", emoji: "⛏️" },
  { codigo: "ANARCHY", nombre: "Anarquía", emoji: "💀" },
];

/**
 * Bloques secundarios: navegar por modalidad y cómo se vota.
 *
 * @param disposicion `columna` en las páginas que la usan como barra lateral de
 *   verdad; `fila` en la portada, donde ocupa el ancho completo y dos bloques
 *   apilados dejarían una tira de aire vacío a la derecha.
 * @param llamadaParaDuenos la invitación a registrar un servidor. Se apaga en la
 *   portada porque allí ya está la franja grande del final, y la misma oferta
 *   dos veces en la misma pantalla resta fuerza a las dos.
 */
export function BarraLateral({
  disposicion = "columna",
  llamadaParaDuenos = true,
}: {
  disposicion?: "columna" | "fila";
  llamadaParaDuenos?: boolean;
}) {
  const enFila = disposicion === "fila";

  return (
    <aside
      className={
        enFila
          ? "grid gap-4 md:grid-cols-2"
          : "space-y-5"
      }
    >
      <Bloque titulo="Explorar por modalidad">
        <div className="grid grid-cols-2 gap-1.5">
          {CATEGORIAS.map((categoria) => (
            <Link
              key={categoria.codigo}
              href={`/servidores?modalidad=${categoria.codigo}`}
              className="flex items-center gap-2 rounded-md border border-border bg-surface px-2.5 py-2 text-sm transition-colors hover:border-primary hover:bg-primary-subtle hover:text-primary"
            >
              <span aria-hidden>{categoria.emoji}</span>
              <span className="truncate">{categoria.nombre}</span>
            </Link>
          ))}
        </div>
      </Bloque>

      <Bloque titulo="Cómo se vota">
        <ol className="space-y-2.5 text-sm text-muted">
          <Paso numero={1}>
            Entra al servidor y escribe{" "}
            <code className="rounded bg-surface-sunken px-1 py-0.5 font-mono text-xs text-text">
              /votar
            </code>
          </Paso>
          <Paso numero={2}>Abre el enlace que te dé, o escribe el código</Paso>
          <Paso numero={3}>
            Vuelve al juego. Tu recompensa llega en menos de un minuto
          </Paso>
        </ol>
        <p className="mt-3 text-xs text-subtle text-pretty">
          Un voto al día por jugador. Nunca te pedimos contraseñas.
        </p>
      </Bloque>

      {llamadaParaDuenos && (
        <Bloque titulo="¿Tienes un servidor?">
          <p className="text-sm text-muted text-pretty">
            Añádelo gratis. Instala el plugin y tus jugadores podrán votar y
            llevarse recompensas dentro del juego.
          </p>
          <div className="mt-3 flex flex-col gap-2">
            <Link
              href="/registro"
              className="boton-vivo rounded-md bg-primary px-3 py-2 text-center text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover"
            >
              Añadir mi servidor
            </Link>
            <Link
              href="/plugin"
              className="boton-vivo rounded-md border border-border bg-surface px-3 py-2 text-center text-sm font-medium hover:bg-surface-sunken"
            >
              Ver cómo funciona
            </Link>
          </div>
        </Bloque>
      )}

      {/* Solo en columna. En la portada las marcas ya salen en los raíles
          laterales o en su propia tira, y repetirlas aquí sería enseñar la misma
          publicidad dos veces en la misma pantalla. */}
      {!enFila && <MarcasEnLateral />}
    </aside>
  );
}

function Bloque({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-lg border border-border bg-surface-sunken/50 p-4">
      <h2 className="display mb-3 text-sm font-semibold">{titulo}</h2>
      {children}
    </section>
  );
}

function Paso({
  numero,
  children,
}: {
  numero: number;
  children: React.ReactNode;
}) {
  return (
    <li className="flex gap-2.5 text-pretty">
      <span className="cifras grid size-5 shrink-0 place-items-center rounded-full bg-primary-subtle text-[11px] font-bold text-primary">
        {numero}
      </span>
      <span>{children}</span>
    </li>
  );
}
