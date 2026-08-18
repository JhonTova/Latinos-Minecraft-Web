import { BotonVotar } from "@/components/boton-votar";
import { resolverEnlaceDeVoto } from "@/lib/api";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Votar",
  // Un enlace de voto es personal y de un solo uso: no tiene sentido que un
  // buscador lo indexe, y menos que lo abra un rastreador y lo consuma.
  robots: { index: false, follow: false },
};

/**
 * Página a la que llega el jugador tras escribir /votar en el servidor.
 *
 * No hay formulario de nick: el token ya lo lleva (ADR-0016). Eso cierra el
 * agujero por el que cualquiera podría votar en nombre de cualquiera, y de paso
 * deja el flujo en un solo clic.
 */
export default async function PaginaVotar(props: PageProps<"/votar/[token]">) {
  const { token } = await props.params;
  const datos = await resolverEnlaceDeVoto(token);

  if (!datos) {
    return <EnlaceNoValido />;
  }

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-16">
      <div className="rounded-lg border border-border bg-surface p-6 shadow-sm">
        <p className="text-sm text-muted">Vas a votar por</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">
          {datos.servidorNombre}
        </h1>

        <div className="mt-5 flex items-center gap-3 rounded-md bg-surface-sunken p-3">
          <div className="grid size-9 shrink-0 place-items-center rounded bg-primary text-sm font-semibold text-on-accent">
            {datos.jugador.charAt(0).toUpperCase()}
          </div>
          <div className="min-w-0">
            <p className="truncate font-medium">{datos.jugador}</p>
            <p className="text-xs text-subtle">Identidad verificada</p>
          </div>
        </div>

        <div className="mt-6">
          <BotonVotar token={token} servidorNombre={datos.servidorNombre} />
        </div>
      </div>

      <p className="mt-4 text-center text-xs text-subtle text-pretty">
        Este enlace es solo tuyo y caduca a los 10 minutos.
      </p>
    </main>
  );
}

/**
 * Estado de enlace caducado o ya usado (convenciones del proyecto, regla 16).
 *
 * Se le dice exactamente qué hacer. Un &quot;enlace inválido&quot; a secas deja al
 * jugador sin salida y genera un ticket de soporte.
 */
function EnlaceNoValido() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-16 text-center">
      <h1 className="text-xl font-semibold">Este enlace ya no vale</h1>
      <p className="mt-2 text-muted text-pretty">
        Los enlaces de voto caducan a los 10 minutos y solo se pueden usar una
        vez.
      </p>
      <div className="mt-6 rounded-lg border border-border bg-surface-sunken p-4">
        <p className="text-sm text-muted">Vuelve a tu servidor y escribe</p>
        <p className="mt-1 font-mono text-lg font-medium">/votar</p>
      </div>
    </main>
  );
}
