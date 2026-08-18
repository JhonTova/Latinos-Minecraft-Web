"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Botón para copiar la dirección del servidor.
 *
 * Es la acción más usada de todo el sitio: el jugador viene a copiar la IP y
 * pegarla en Minecraft. Por eso ocupa el sitio que ocupa y aparece también en
 * las tarjetas del listado, no solo en la ficha.
 *
 * La variante `compacto` es la de las tarjetas: una sola línea.
 */
export function CopiarIp({
  direccion,
  compacto = false,
}: {
  direccion: string;
  compacto?: boolean;
}) {
  const [copiada, setCopiada] = useState(false);
  const [fallo, setFallo] = useState(false);
  const temporizador = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Sin esto, salir de la página con el aviso "¡Copiada!" en pantalla deja un
  // temporizador intentando actualizar un componente que ya no existe.
  useEffect(() => {
    return () => {
      if (temporizador.current) clearTimeout(temporizador.current);
    };
  }, []);

  async function copiar() {
    try {
      // El portapapeles falla en contextos sin HTTPS y si el usuario deniega el
      // permiso. Avisar es mejor que no hacer nada: al menos sabe que tiene que
      // seleccionar el texto a mano.
      await navigator.clipboard.writeText(direccion);
      setCopiada(true);
      setFallo(false);
    } catch {
      setFallo(true);
    }

    if (temporizador.current) clearTimeout(temporizador.current);
    temporizador.current = setTimeout(() => {
      setCopiada(false);
      setFallo(false);
    }, 2000);
  }

  if (compacto) {
    return (
      <button
        type="button"
        onClick={copiar}
        // aria-live para que un lector de pantalla anuncie el cambio: sin esto,
        // quien no ve la pantalla no sabe si la copia funcionó.
        aria-live="polite"
        className="boton-vivo flex w-full items-center justify-between gap-2 rounded-md border border-border bg-surface-sunken px-3 py-2 text-left hover:border-primary/50"
      >
        <span className="truncate font-mono text-sm">{direccion}</span>
        <span
          className={`shrink-0 text-xs font-medium ${copiada ? "text-success" : fallo ? "text-danger" : "text-muted"}`}
        >
          {copiada ? "¡Copiada!" : fallo ? "Cópiala a mano" : "Copiar"}
        </span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={copiar}
      aria-live="polite"
      className="boton-vivo group flex w-full items-center justify-between gap-3 rounded-lg border border-border bg-surface px-4 py-3 text-left shadow-sm hover:border-primary"
    >
      <div className="min-w-0">
        <div className="text-xs text-subtle">Dirección del servidor</div>
        <div className="truncate font-mono text-lg font-medium">
          {direccion}
        </div>
      </div>
      <span
        className={`shrink-0 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
          copiada
            ? "bg-success text-on-accent"
            : fallo
              ? "bg-danger/15 text-danger"
              : "bg-surface-sunken group-hover:bg-primary group-hover:text-on-accent"
        }`}
      >
        {copiada ? "¡Copiada!" : fallo ? "Cópiala a mano" : "Copiar"}
      </span>
    </button>
  );
}
