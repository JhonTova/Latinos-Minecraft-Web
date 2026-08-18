"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Buscador de servidores.
 *
 * Navega a `/servidores?q=...` en lugar de filtrar aquí mismo. Así la búsqueda
 * tiene URL propia: se comparte, se guarda en marcadores y funciona con el botón
 * de atrás. Un buscador que solo filtra en memoria pierde las tres cosas.
 *
 * Es un `<form>` de verdad, no un input con un `onClick`: eso hace que la tecla
 * Enter funcione sin escribir nada, y que siga funcionando si el JavaScript
 * falla o tarda en cargar.
 */
export function BuscadorDeServidores({
  valorInicial = "",
}: {
  valorInicial?: string;
}) {
  const router = useRouter();
  const [texto, setTexto] = useState(valorInicial);

  return (
    <form
      role="search"
      onSubmit={(e) => {
        e.preventDefault();
        const limpio = texto.trim();
        router.push(
          limpio ? `/servidores?q=${encodeURIComponent(limpio)}` : "/servidores",
        );
      }}
      className="flex gap-2"
    >
      <div className="relative flex-1">
        <svg
          viewBox="0 0 24 24"
          className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-subtle"
          aria-hidden
        >
          <path
            d="M21 21l-4.3-4.3M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0Z"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
        <input
          type="search"
          name="q"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Buscar servidores…"
          aria-label="Buscar servidores"
          className="w-full rounded-lg border border-border bg-surface py-3 pr-4 pl-11 text-base outline-none transition-colors placeholder:text-subtle focus:border-primary"
        />
      </div>

      <button
        type="submit"
        aria-label="Buscar"
        className="boton-vivo grid w-12 shrink-0 place-items-center rounded-lg bg-primary text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
      >
        <svg viewBox="0 0 24 24" className="size-5" aria-hidden>
          <path
            d="M21 21l-4.3-4.3M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0Z"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      </button>
    </form>
  );
}
