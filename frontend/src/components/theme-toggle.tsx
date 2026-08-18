"use client";

import { useSyncExternalStore } from "react";

type Tema = "light" | "dark";

const oyentes = new Set<() => void>();

/**
 * El tema no vive en React: lo escribe el script sincrono del layout antes del
 * primer pintado y lo lee el CSS. Este componente lo observa desde fuera en vez
 * de guardarse una copia, que es lo que evita el efecto que sincronizaba estado
 * despues de montar —y con el, el renderizado en cascada.
 */
function suscribir(avisar: () => void): () => void {
  oyentes.add(avisar);

  // Sin eleccion explicita manda el sistema, y puede cambiar con la tarde.
  const consulta = window.matchMedia("(prefers-color-scheme: dark)");
  consulta.addEventListener("change", avisar);

  return () => {
    oyentes.delete(avisar);
    consulta.removeEventListener("change", avisar);
  };
}

function temaEnCliente(): Tema {
  const elegido = document.documentElement.getAttribute("data-theme");
  if (elegido === "dark" || elegido === "light") return elegido;

  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}

// En el servidor no hay tema que leer. Devolver null hace que el primer pintado
// reserve el hueco y no adivine un icono que luego tendria que corregir.
function temaEnServidor(): Tema | null {
  return null;
}

/**
 * Conmutador de tema.
 *
 * Guarda la eleccion en localStorage y la aplica como atributo en <html>. El
 * valor inicial lo pinta el script sincrono del layout, no este componente:
 * asi no hay parpadeo.
 */
export function ThemeToggle() {
  const tema = useSyncExternalStore(suscribir, temaEnCliente, temaEnServidor);

  function alternar() {
    const siguiente: Tema = tema === "dark" ? "light" : "dark";
    localStorage.setItem("tema", siguiente);
    document.documentElement.setAttribute("data-theme", siguiente);
    oyentes.forEach((avisar) => avisar());
  }

  // Hasta que el efecto resuelve el tema no sabemos que icono toca. Se reserva
  // el espacio para que la cabecera no de un salto al hidratar.
  if (tema === null) {
    return <div className="size-9" aria-hidden />;
  }

  const oscuro = tema === "dark";

  return (
    <button
      type="button"
      onClick={alternar}
      aria-label={oscuro ? "Cambiar a tema claro" : "Cambiar a tema oscuro"}
      className="grid size-9 place-items-center rounded-md border border-border text-muted transition-colors hover:bg-surface-sunken hover:text-text"
    >
      {oscuro ? <IconoSol /> : <IconoLuna />}
    </button>
  );
}

function IconoSol() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      className="size-[18px]"
      aria-hidden
    >
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  );
}

function IconoLuna() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="size-[18px]"
      aria-hidden
    >
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
    </svg>
  );
}
