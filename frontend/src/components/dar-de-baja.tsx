"use client";

import { Aviso, Boton } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Zona de peligro: dar de baja un servidor.
 *
 * Pide escribir el nombre exacto. La confirmación **también se verifica en el
 * backend**, no solo aquí: una comprobación que vive solo en el navegador es
 * teatro — basta con llamar a la API directamente para saltársela. Lo de aquí
 * existe para obligar a mirar *qué* se está borrando, no para proteger nada.
 *
 * Va en un desplegable cerrado. Una acción destructiva permanentemente visible
 * acaba pulsada por error; una que hay que abrir a propósito, no.
 */
export function DarDeBaja({
  servidorId,
  nombre,
}: {
  servidorId: string;
  nombre: string;
}) {
  const router = useRouter();
  const [escrito, setEscrito] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const coincide = escrito.trim().toLowerCase() === nombre.trim().toLowerCase();

  async function darDeBaja() {
    setEnviando(true);
    setError(null);
    try {
      const respuesta = await fetch(`/api/backend/servers/${servidorId}`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nombre: escrito }),
      });

      if (!respuesta.ok) {
        const detalle = await respuesta.json().catch(() => null);
        throw new Error(detalle?.detail ?? "No se pudo dar de baja.");
      }

      // `replace` y no `push`: si el usuario pulsa atrás no debe volver a la
      // ficha de un servidor que ya no existe.
      router.replace("/panel");
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo dar de baja.");
      setEnviando(false);
    }
  }

  return (
    <details className="group rounded-xl border border-danger/30 bg-danger/5">
      <summary className="flex cursor-pointer items-center justify-between gap-3 px-5 py-4">
        <span>
          <span className="font-semibold text-danger">Dar de baja</span>
          <span className="mt-0.5 block text-sm text-muted text-pretty">
            Tu servidor desaparece del ranking y la clave del plugin deja de
            funcionar.
          </span>
        </span>
        <span
          aria-hidden
          className="shrink-0 text-muted transition-transform group-open:rotate-180"
        >
          ▾
        </span>
      </summary>

      <div className="space-y-4 border-t border-danger/20 px-5 py-4">
        <ul className="space-y-1.5 text-sm text-muted">
          <Punto>
            Deja de aparecer en el ranking y en las búsquedas al instante.
          </Punto>
          <Punto>
            La clave de API se revoca: el plugin dejará de conectarse aunque siga
            instalado.
          </Punto>
          <Punto>
            El historial de votos <strong className="text-text">se conserva</strong>.
            No se borra a nadie de la base de datos por dar de baja un servidor.
          </Punto>
          <Punto>
            Si te arrepientes, escríbenos: se puede recuperar mientras no pase
            demasiado tiempo.
          </Punto>
        </ul>

        <div className="space-y-1.5">
          <label htmlFor="confirmar-nombre" className="block text-sm font-medium">
            Escribe{" "}
            <span className="font-mono text-danger">{nombre}</span> para
            confirmar
          </label>
          <input
            id="confirmar-nombre"
            value={escrito}
            onChange={(e) => {
              setEscrito(e.target.value);
              setError(null);
            }}
            autoComplete="off"
            spellCheck={false}
            placeholder={nombre}
            className="w-full rounded-md border border-border bg-surface px-3 py-2.5 font-mono outline-none transition-colors placeholder:font-sans placeholder:text-subtle focus:border-danger"
          />
        </div>

        {error && <Aviso tono="error">{error}</Aviso>}

        <Boton
          onClick={darDeBaja}
          cargando={enviando}
          disabled={!coincide}
          className="!bg-danger hover:!bg-danger"
        >
          Dar de baja definitivamente
        </Boton>
      </div>
    </details>
  );
}

function Punto({ children }: { children: React.ReactNode }) {
  return (
    <li className="flex gap-2 text-pretty">
      <span
        aria-hidden
        className="mt-1.5 size-1 shrink-0 rounded-full bg-current opacity-50"
      />
      <span>{children}</span>
    </li>
  );
}
