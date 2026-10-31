import { BuscadorDeServidores } from "@/components/buscador";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Página no encontrada",
  // Next ya añade un `noindex`, pero el layout declara `index, follow` para todo
  // el sitio y la página salía con las dos etiquetas, contradiciéndose. Esta
  // sustituye a la del layout y deja una sola.
  robots: { index: false, follow: true },
};

/**
 * Página «no encontrada».
 *
 * Quien llega aquí casi siempre venía buscando un servidor: un enlace viejo a
 * uno que se retiró o se purgó, o un slug mal escrito. Por eso la salida
 * principal es el buscador y el directorio, no solo «volver al inicio».
 *
 * **El texto es el mismo para cualquier ruta, a propósito.** `/panel/admin`
 * responde 404 a quien no es administrador para no revelar que existe. Si esta
 * página mostrara la URL pedida o cambiara según la sección, un 404 de admin se
 * podría distinguir de uno normal. Al vivir en la raíz se renderiza sin el
 * layout del panel, así que los dos salen idénticos.
 *
 * Next añade solo el `noindex`. El código HTTP es 404 mientras `notFound()` se
 * llame antes de cualquier `<Suspense>` de la página; después, la respuesta ya
 * se está transmitiendo y sale como 200 (con `noindex`).
 */
export default function NoEncontrada() {
  return (
    <>
      <Cabecera />

      <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col items-center justify-center px-4 py-24 text-center sm:px-6">
        <p
          aria-hidden="true"
          className="display text-6xl font-bold text-subtle sm:text-7xl"
        >
          404
        </p>
        <h1 className="display mt-4 text-xl font-semibold">
          No encontramos esta página
        </h1>
        <p className="mt-2 max-w-sm text-muted text-pretty">
          Puede que el servidor ya no esté en el directorio o que el enlace esté
          mal escrito. Búscalo por su nombre:
        </p>

        <div className="mt-6 w-full max-w-md text-left">
          <BuscadorDeServidores />
        </div>

        <div className="mt-6 flex flex-wrap items-center justify-center gap-x-6 gap-y-3">
          <Link
            href="/servidores"
            className="boton-vivo rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover"
          >
            Ver todos los servidores
          </Link>
          <Link
            href="/"
            className="text-sm font-medium text-muted underline-offset-4 hover:text-text hover:underline"
          >
            Volver al inicio
          </Link>
        </div>
      </main>

      <PieDePagina />
    </>
  );
}
