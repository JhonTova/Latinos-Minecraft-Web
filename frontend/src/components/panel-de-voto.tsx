import { CampoDeCodigo } from "@/components/campo-de-codigo";

/**
 * Panel de voto de la ficha de un servidor.
 *
 * El campo de código existe sobre todo por los jugadores de **Bedrock** —móvil y
 * consola—, que no pueden pulsar enlaces en el chat de Minecraft. Para ellos
 * escribirlo a mano es la única forma de votar. Los de PC pulsan el enlace y no
 * pasan por aquí.
 */
export function PanelDeVoto({ servidorNombre }: { servidorNombre: string }) {
  return (
    <div className="rounded-xl border border-primary/25 bg-primary-subtle/40 p-5 shadow-sm">
      <h3 className="font-semibold">Votar por {servidorNombre}</h3>

      <ol className="mt-3 space-y-2 text-sm text-muted">
        <li className="flex gap-2">
          <span className="cifras font-semibold text-primary">1.</span>
          <span className="text-pretty">
            Entra al servidor y escribe{" "}
            <code className="rounded bg-surface px-1.5 py-0.5 font-mono text-xs text-text">
              /votar
            </code>{" "}
            en el chat.
          </span>
        </li>
        <li className="flex gap-2">
          <span className="cifras font-semibold text-primary">2.</span>
          <span className="text-pretty">
            Pulsa el enlace que te dé, o escribe aquí el código.
          </span>
        </li>
        <li className="flex gap-2">
          <span className="cifras font-semibold text-primary">3.</span>
          <span className="text-pretty">
            Vuelve al juego. Tu recompensa llega sola en menos de un minuto.
          </span>
        </li>
      </ol>

      <div className="mt-5">
        <CampoDeCodigo
          etiqueta="¿Tienes un código?"
          ayuda="Para jugadores de móvil y consola, que no pueden pulsar enlaces en el chat."
        />
      </div>
    </div>
  );
}
