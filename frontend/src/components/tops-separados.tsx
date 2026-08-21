"use client";

import { useState } from "react";

/**
 * Las dos clasificaciones de la portada (ADR-0030).
 *
 * En escritorio salen las dos, una al lado de la otra. En móvil no caben, así
 * que hay un conmutador y se ve una cada vez.
 *
 * **Las dos listas se pintan siempre en el servidor**, y en móvil se esconde una
 * con CSS en lugar de no renderizarla. Cuesta unos kilobytes de HTML y a cambio:
 * cambiar de pestaña es instantáneo, no hay una segunda petición, y Google ve
 * los dos rankings completos aunque rastree con una pantalla estrecha. Cargar la
 * otra al pulsar habría sido más "eficiente" y peor en las tres cosas.
 *
 * Por defecto se enseña **«Premium y no premium»**. Es la lista donde cualquiera
 * puede jugar: quien tiene cuenta original entra en los dos sitios, quien no la
 * tiene solo en estos. Abrir por la lista que excluye a la mitad del público
 * sería empezar por el sitio equivocado.
 */
export function TopsSeparados({
  premium,
  mixto,
}: {
  premium: React.ReactNode;
  mixto: React.ReactNode;
}) {
  const [activo, setActivo] = useState<"premium" | "mixto">("mixto");

  return (
    <div>
      {/* Conmutador: solo en móvil. En escritorio sobra, porque se ven los dos. */}
      <div
        role="tablist"
        aria-label="Tipo de servidores"
        className="mb-4 flex rounded-lg border border-border bg-surface-sunken p-1 lg:hidden"
      >
        <Pestana
          activo={activo === "mixto"}
          onClick={() => setActivo("mixto")}
          id="pestana-mixto"
          controla="lista-mixto"
        >
          Premium y no premium
        </Pestana>
        <Pestana
          activo={activo === "premium"}
          onClick={() => setActivo("premium")}
          id="pestana-premium"
          controla="lista-premium"
        >
          Solo premium
        </Pestana>
      </div>

      <div className="grid gap-8 lg:grid-cols-2 lg:gap-6">
        {/* Premium a la izquierda en escritorio; en móvil el orden lo decide el
            conmutador, así que da igual cuál venga primero en el HTML. */}
        <div
          id="lista-premium"
          role="tabpanel"
          aria-labelledby="pestana-premium"
          className={activo === "premium" ? "min-w-0" : "hidden min-w-0 lg:block"}
        >
          {premium}
        </div>

        <div
          id="lista-mixto"
          role="tabpanel"
          aria-labelledby="pestana-mixto"
          className={activo === "mixto" ? "min-w-0" : "hidden min-w-0 lg:block"}
        >
          {mixto}
        </div>
      </div>
    </div>
  );
}

function Pestana({
  activo,
  onClick,
  children,
  id,
  controla,
}: {
  activo: boolean;
  onClick: () => void;
  children: React.ReactNode;
  id: string;
  controla: string;
}) {
  return (
    <button
      type="button"
      role="tab"
      id={id}
      aria-selected={activo}
      aria-controls={controla}
      onClick={onClick}
      className={`flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
        activo
          ? "bg-surface text-text shadow-sm"
          : "text-muted hover:text-text"
      }`}
    >
      {children}
    </button>
  );
}
