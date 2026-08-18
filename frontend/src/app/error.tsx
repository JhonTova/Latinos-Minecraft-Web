"use client";

/**
 * Estado de error (convenciones del proyecto, regla 16).
 *
 * No se muestra el mensaje técnico: al visitante no le dice nada y a un atacante
 * le dice demasiado. Se le ofrece la única acción útil —reintentar— y el detalle
 * queda en la consola para desarrollo.
 *
 * **El texto es genérico a propósito.** Antes decía «No pudimos cargar el
 * ranking» pasara lo que pasara, y esto salta ante CUALQUIER fallo de la
 * aplicación. Ese mensaje costó dos rondas de diagnóstico buscando un problema
 * en el ranking que no existía: la API respondía perfectamente y lo que fallaba
 * estaba en otro sitio. Un error que nombra el sitio equivocado es peor que uno
 * que no nombra ninguno.
 */
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  if (process.env.NODE_ENV === "development") {
    console.error(error);
  }

  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col items-center justify-center px-4 py-24 text-center sm:px-6">
      <h1 className="display text-xl font-semibold">Algo ha ido mal</h1>
      <p className="mt-2 max-w-sm text-muted text-pretty">
        Puede ser un problema temporal. Inténtalo de nuevo en unos segundos.
      </p>
      <button
        type="button"
        onClick={reset}
        className="boton-vivo mt-6 rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover"
      >
        Reintentar
      </button>
      {error.digest && (
        <p className="mt-6 font-mono text-xs text-subtle">
          Referencia: {error.digest}
        </p>
      )}
    </main>
  );
}
