"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Estado de "acabo de copiar esto".
 *
 * Vive en un hook y no dentro del botón porque en la ficha hay tres copiables
 * —la dirección de Java, la IP de Bedrock y su puerto— y cada uno tiene que
 * poder decir «¡Copiada!» sin apagar el aviso de los otros.
 */
function useCopiado() {
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

  async function copiar(texto: string) {
    try {
      // El portapapeles falla en contextos sin HTTPS y si el usuario deniega el
      // permiso. Avisar es mejor que no hacer nada: al menos sabe que tiene que
      // seleccionar el texto a mano.
      await navigator.clipboard.writeText(texto);
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

  const etiqueta = copiada ? "¡Copiada!" : fallo ? "Cópiala a mano" : "Copiar";

  return { copiar, copiada, fallo, etiqueta };
}

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
  const { copiar, copiada, fallo, etiqueta } = useCopiado();

  if (compacto) {
    return (
      <button
        type="button"
        onClick={() => copiar(direccion)}
        // aria-live para que un lector de pantalla anuncie el cambio: sin esto,
        // quien no ve la pantalla no sabe si la copia funcionó.
        aria-live="polite"
        className="boton-vivo flex w-full items-center justify-between gap-2 rounded-md border border-border bg-surface-sunken px-3 py-2 text-left hover:border-primary/50"
      >
        <span className="truncate font-mono text-sm">{direccion}</span>
        <span
          className={`shrink-0 text-xs font-medium ${copiada ? "text-success" : fallo ? "text-danger" : "text-muted"}`}
        >
          {etiqueta}
        </span>
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={() => copiar(direccion)}
      aria-live="polite"
      className="boton-vivo group borde-tema superficie-tema flex w-full items-center justify-between gap-3 rounded-lg border px-4 py-3 text-left shadow-sm hover:border-primary"
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
        {etiqueta}
      </span>
    </button>
  );
}

/**
 * Dirección de Bedrock: IP y puerto, cada uno copiable por separado.
 *
 * **Los dos campos van separados a propósito.** El cliente de Bedrock —móvil,
 * consola, Windows 10— no pide una cadena `host:puerto` como el de Java: pide la
 * dirección en una casilla y el puerto en otra. Ofrecer aquí un solo botón con
 * las dos cosas juntas obligaría al jugador a partirlo a mano en una pantalla
 * táctil, que es justo donde peor se hace.
 *
 * El puerto se enseña siempre aunque sea el 19132 por defecto. En Java se
 * esconde el 25565 porque el cliente lo rellena solo; el de Bedrock no, y quien
 * no lo escribe no entra.
 */
export function DireccionBedrock({
  host,
  puerto,
}: {
  host: string;
  puerto: number;
}) {
  return (
    <div className="borde-tema hundido-tema rounded-lg border px-4 py-3">
      <div className="mb-2 flex items-center gap-1.5 text-xs font-medium text-secundario">
        <span aria-hidden>▣</span>
        Desde móvil o consola (Bedrock)
      </div>

      <div className="grid gap-2 sm:grid-cols-[1fr_9rem]">
        <CampoCopiable etiqueta="Dirección" valor={host} />
        <CampoCopiable etiqueta="Puerto" valor={String(puerto)} />
      </div>

      <p className="mt-2 text-xs text-subtle text-pretty">
        En Bedrock la dirección y el puerto van en casillas distintas. Cópialos
        por separado.
      </p>
    </div>
  );
}

function CampoCopiable({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string;
}) {
  const { copiar, copiada, fallo, etiqueta: estado } = useCopiado();

  return (
    <button
      type="button"
      onClick={() => copiar(valor)}
      aria-live="polite"
      aria-label={`Copiar ${etiqueta.toLowerCase()}: ${valor}`}
      className="boton-vivo group flex items-center justify-between gap-3 rounded-md border border-border bg-surface px-3 py-2 text-left hover:border-primary"
    >
      <span className="min-w-0">
        <span className="block text-[11px] text-subtle">{etiqueta}</span>
        <span className="block truncate font-mono text-sm font-medium">
          {valor}
        </span>
      </span>
      <span
        className={`shrink-0 text-xs font-medium ${copiada ? "text-success" : fallo ? "text-danger" : "text-muted group-hover:text-primary"}`}
      >
        {estado}
      </span>
    </button>
  );
}
