import { BuscadorDeServidores } from "@/components/buscador";
import type { EstadisticasPlataforma } from "@/lib/api";
import Link from "next/link";

/**
 * Categorías del héroe.
 *
 * Cinco, no dieciséis. Aquí no se trata de listar el catálogo —para eso está el
 * filtro completo en `/servidores`— sino de dar cinco puertas grandes a lo que
 * busca la mayoría. Un menú de dieciséis atajos deja de ser un atajo.
 */
const ATAJOS = [
  { codigo: "SURVIVAL", nombre: "Survival", emoji: "🌳" },
  { codigo: "SKYBLOCK", nombre: "SkyBlock", emoji: "☁️" },
  { codigo: "PVP", nombre: "PvP", emoji: "⚔️" },
  { codigo: "PRISON", nombre: "Prison", emoji: "⛏️" },
  { codigo: "BEDWARS", nombre: "BedWars", emoji: "🛏️" },
];

/**
 * Héroe de la portada.
 *
 * Dos columnas: la promesa y el buscador a la izquierda, la marca a la derecha.
 * La imagen va a la derecha y no de fondo a propósito — de fondo obligaría a
 * oscurecerla para que el texto se leyera, y entonces no se vería la imagen; al
 * lado se ve entera y el texto no pelea con ella.
 *
 * Sin logotipo subido, la columna derecha desaparece y el texto ocupa el ancho
 * completo. No se deja un hueco esperando una imagen que quizá nunca llegue.
 */
export function Heroe({
  nombre,
  logoUrl,
  cifras,
}: {
  nombre: string;
  logoUrl: string | null;
  cifras: EstadisticasPlataforma | null;
}) {
  return (
    // Resplandor detrás de la marca: degradados radiales, no una imagen — cero
    // peticiones de red y se adaptan al tema claro y al oscuro.
    //
    // Ahora se mueven. Un ciclo de veinte segundos no se ve moverse: se nota que
    // la portada está viva cuando llevas unos segundos en ella, que es
    // exactamente la dosis que separa un acabado cuidado de un salvapantallas.
    // Con `prefers-reduced-motion` se queda quieto, como todo lo demás.
    <section className="aurora overflow-hidden">
      <div className="mx-auto grid w-full max-w-6xl items-center gap-10 px-4 py-12 sm:px-6 sm:py-16 lg:grid-cols-[1fr_auto]">
        <div className="min-w-0">
          {cifras && cifras.servidores > 0 && (
            <p className="entra mb-4 inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary-subtle px-3 py-1 text-xs font-semibold text-primary">
              <span className="late size-1.5 rounded-full bg-current" aria-hidden />
              {new Intl.NumberFormat("es-ES").format(cifras.jugadores)} jugando
              ahora mismo
            </p>
          )}

          {/*
            El H1 lleva "en español" a propósito. Es lo que Google lee como tema
            de la página, y la gente no busca la marca: busca "servidores de
            minecraft en español". Que la palabra esté solo en el <title> y en el
            párrafo de abajo desaprovecha la señal más fuerte de la portada.
          */}
          <h1 className="display entra entra-1 max-w-2xl text-4xl leading-[1.08] font-bold text-balance sm:text-5xl">
            Encuentra tu próximo servidor de{" "}
            <span className="text-primary">Minecraft en español</span>
          </h1>

          {/*
            Sin decir cada cuántas horas se vota: eso lo fija el administrador
            (ADR-0023) y un número escrito aquí quedaría mintiendo el día que se
            cambie. La ficha de cada servidor sí lo dice, y ahí sale del ajuste
            real.
          */}
          <p className="entra entra-2 mt-4 max-w-xl text-lg text-muted text-pretty">
            Servidores latinos e hispanos de survival, skyblock, factions y más.
            Vota por tus favoritos y ayúdalos a crecer.
          </p>

          <div className="entra entra-3 mt-7 max-w-xl">
            <BuscadorDeServidores />
          </div>

          <div className="entra entra-4 mt-4 flex flex-wrap gap-2">
            <Atajo href="/servidores">Todos</Atajo>
            {ATAJOS.map((a) => (
              <Atajo key={a.codigo} href={`/servidores?modalidad=${a.codigo}`}>
                <span aria-hidden>{a.emoji}</span> {a.nombre}
              </Atajo>
            ))}
          </div>
        </div>

        {logoUrl && (
          <div className="entra entra-2 hidden lg:block">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={logoUrl}
              alt={nombre}
              width={380}
              height={380}
              className="size-[340px] object-contain xl:size-[380px]"
            />
          </div>
        )}
      </div>
    </section>
  );
}

function Atajo({
  href,
  children,
}: {
  href: string;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      className="boton-vivo inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3.5 py-2 text-sm font-medium hover:border-primary hover:bg-primary-subtle hover:text-primary"
    >
      {children}
    </Link>
  );
}
