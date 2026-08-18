"use client";

import { useState } from "react";

type Estado =
  | { fase: "listo" }
  | { fase: "enviando" }
  | { fase: "hecho" }
  | { fase: "espera"; segundos: number }
  | { fase: "error"; mensaje: string };

/**
 * Botón de votar.
 *
 * El nick no se pide: viene dentro del token (ADR-0016). Aquí solo queda
 * confirmar.
 */
export function BotonVotar({
  token,
  servidorNombre,
}: {
  token: string;
  servidorNombre: string;
}) {
  const [estado, setEstado] = useState<Estado>({ fase: "listo" });

  async function votar() {
    setEstado({ fase: "enviando" });
    try {
      const respuesta = await fetch(`/api/votar/${encodeURIComponent(token)}`, {
        method: "POST",
      });

      if (respuesta.ok) {
        setEstado({ fase: "hecho" });
        return;
      }

      const problema = await respuesta.json().catch(() => null);

      // La API devuelve los segundos que faltan para que el jugador vea una
      // cuenta atrás en vez de una fecha: nadie debería tener que pensar en
      // husos horarios para saber cuándo puede volver a votar.
      if (problema?.title === "vote.window.open") {
        setEstado({
          fase: "espera",
          segundos: problema.segundosRestantes ?? 0,
        });
        return;
      }

      setEstado({
        fase: "error",
        mensaje:
          problema?.detail ?? "No se pudo registrar tu voto. Inténtalo otra vez.",
      });
    } catch {
      setEstado({
        fase: "error",
        mensaje: "No hay conexión con el servidor. Inténtalo otra vez.",
      });
    }
  }

  if (estado.fase === "hecho") {
    return (
      <div className="rounded-lg border border-border bg-primary-subtle p-6 text-center">
        <div className="text-2xl" aria-hidden>
          ✓
        </div>
        <p className="mt-2 font-medium">¡Voto registrado!</p>
        <p className="mt-1 text-sm text-muted text-pretty">
          Vuelve al servidor. Tu recompensa llegará en menos de un minuto.
        </p>
      </div>
    );
  }

  if (estado.fase === "espera") {
    return (
      <div className="rounded-lg border border-border bg-surface-sunken p-6 text-center">
        <p className="font-medium">Ya votaste hace poco</p>
        <p className="mt-1 text-sm text-muted">
          Puedes volver a votar en {formatearEspera(estado.segundos)}.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <button
        type="button"
        onClick={votar}
        disabled={estado.fase === "enviando"}
        className="w-full rounded-md bg-primary px-4 py-3 font-medium text-on-accent transition-colors hover:bg-primary-hover disabled:opacity-60"
      >
        {estado.fase === "enviando"
          ? "Registrando tu voto…"
          : `Votar por ${servidorNombre}`}
      </button>

      {estado.fase === "error" && (
        <p role="alert" className="text-center text-sm text-danger">
          {estado.mensaje}
        </p>
      )}
    </div>
  );
}

function formatearEspera(segundos: number): string {
  const horas = Math.floor(segundos / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  if (horas > 0) return `${horas} h ${minutos} min`;
  if (minutos > 0) return `${minutos} min`;
  return "menos de un minuto";
}
