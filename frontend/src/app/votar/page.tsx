import { CampoDeCodigo } from "@/components/campo-de-codigo";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Votar",
  description:
    "Escribe el código que te da el servidor y vota en un paso. Sin registrarte y sin escribir tu nombre.",
};

/**
 * Entrada manual del código de voto.
 *
 * <p>Existe por los jugadores de consola y móvil, que son buena parte del
 * público en español: en Bedrock no se pueden pulsar los enlaces del chat, así
 * que sin un sitio donde escribir el código a mano se quedan fuera del bucle
 * entero —votar, recompensa, ranking— por un detalle de la interfaz de su
 * plataforma.
 *
 * <p>No se pregunta el nombre del jugador en ningún momento. Va dentro del
 * código (ADR-0016): preguntarlo permitiría votar en nombre de cualquiera.
 */
export default function PaginaEscribirCodigo() {
  return (
    <>
      <Cabecera />

      <main className="mx-auto w-full max-w-md flex-1 px-4 py-12 sm:px-6">
        <h1 className="text-2xl font-semibold tracking-tight text-balance">
          Votar por tu servidor
        </h1>
        <p className="mt-2 text-muted text-pretty">
          Escribe <span className="cifras font-medium text-text">/votar</span> en
          el chat del servidor y te daremos un código. Escríbelo aquí.
        </p>

        <div className="mt-6 rounded-lg border border-border bg-surface p-6 shadow-sm">
          <CampoDeCodigo autoFocus />
        </div>

        <p className="mt-4 text-center text-xs text-subtle text-pretty">
          El código dura 10 minutos y solo sirve una vez. Si juegas en un
          ordenador, el enlace del chat te trae aquí directamente.
        </p>

        <div className="mt-8 border-t border-border pt-6 text-center">
          <p className="text-sm text-muted text-pretty">
            ¿Todavía no sabes dónde jugar?
          </p>
          <Link
            href="/servidores"
            className="enlace-vivo mt-1 inline-block text-sm font-medium text-primary"
          >
            Ver el ranking de servidores
          </Link>
        </div>
      </main>

      <PieDePagina />
    </>
  );
}
