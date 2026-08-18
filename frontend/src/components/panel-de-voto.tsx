"use client";

import { Boton } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

/** El mismo alfabeto que genera el backend, sin I, L, O ni U. */
const ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
const LARGO = 12;

/**
 * Panel de voto de la ficha de un servidor.
 *
 * **Aquí no se pide el nick.** El código que da el servidor ya lleva dentro
 * quién eres (ADR-0016): si la web preguntara el nombre, cualquiera podría votar
 * en nombre de otro sin haber pisado nunca ese servidor, y el ranking dejaría de
 * significar algo.
 *
 * El campo de código existe sobre todo por los jugadores de **Bedrock** —móvil y
 * consola—, que no pueden pulsar enlaces en el chat de Minecraft. Para ellos
 * escribir doce caracteres es la única forma de votar. Los de PC pulsan el
 * enlace y no pasan por aquí.
 */
export function PanelDeVoto({ servidorNombre }: { servidorNombre: string }) {
  const router = useRouter();
  const [codigo, setCodigo] = useState("");
  const [error, setError] = useState<string | null>(null);

  const limpio = normalizar(codigo);
  const completo = limpio.length === LARGO;

  function continuar() {
    if (!completo) {
      setError("El código tiene 12 caracteres. Revísalo.");
      return;
    }
    // Se navega a la página del código en lugar de votar desde aquí: ahí se
    // resuelve a quién pertenece y se le enseña antes de confirmar. Votar sin
    // que el jugador vea su propio nombre invita a pensar que se ha votado por
    // otro.
    router.push(`/votar/${limpio}`);
  }

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
        <label htmlFor="codigo" className="mb-1.5 block text-sm font-medium">
          ¿Tienes un código?
        </label>
        <input
          id="codigo"
          value={codigo}
          onChange={(e) => {
            setCodigo(agrupar(normalizar(e.target.value)));
            setError(null);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") continuar();
          }}
          placeholder="K7M2-9QX4-RT3F"
          autoComplete="off"
          autoCapitalize="characters"
          spellCheck={false}
          // 14 = 12 caracteres + los dos guiones que se insertan solos.
          maxLength={14}
          aria-describedby="codigo-ayuda"
          aria-invalid={error ? true : undefined}
          className="w-full rounded-md border border-border bg-surface px-3 py-2.5 text-center font-mono text-lg tracking-widest uppercase outline-none transition-colors placeholder:tracking-normal placeholder:text-subtle focus:border-primary"
        />
        <p
          id="codigo-ayuda"
          className={`mt-1.5 text-xs ${error ? "text-danger" : "text-subtle"}`}
        >
          {error ??
            "Para jugadores de móvil y consola, que no pueden pulsar enlaces en el chat."}
        </p>
      </div>

      <div className="mt-3">
        <Boton onClick={continuar} disabled={!completo}>
          Continuar
        </Boton>
      </div>
    </div>
  );
}

/**
 * Deja solo caracteres del alfabeto y corrige las confusiones de lectura.
 *
 * Es la misma tolerancia que aplica el backend, repetida aquí para que el campo
 * reaccione bien mientras se escribe. Nadie distingue una I de un 1 leyendo el
 * chat de Minecraft, y rechazar por eso haría pensar que el sitio está roto.
 */
function normalizar(escrito: string): string {
  let salida = "";

  for (const caracter of escrito.trim().toUpperCase()) {
    const equivalente =
      caracter === "I" || caracter === "L"
        ? "1"
        : caracter === "O"
          ? "0"
          : caracter === "U"
            ? "V"
            : caracter;

    if (ALFABETO.includes(equivalente)) salida += equivalente;
    if (salida.length === LARGO) break;
  }
  return salida;
}

/** Mete los guiones mientras se escribe, para que se lea igual que en el chat. */
function agrupar(limpio: string): string {
  return limpio.replace(/(.{4})(?=.)/g, "$1-");
}
