/**
 * Piezas básicas de interfaz.
 *
 * Todo consume los tokens de `globals.css`: ningún color, radio ni sombra se
 * escribe aquí a mano. Cuando la marca esté decidida, cambia el archivo de
 * tokens y cambia toda la aplicación.
 */

export function Campo({
  etiqueta,
  ayuda,
  error,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & {
  etiqueta: string;
  ayuda?: string;
  error?: string;
}) {
  const id = props.id ?? props.name;
  const idAyuda = ayuda || error ? `${id}-ayuda` : undefined;

  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium">
        {etiqueta}
      </label>
      <input
        id={id}
        // Enlazar la ayuda y el error al campo es lo que hace que un lector de
        // pantalla los anuncie; sin esto, quien no ve la pantalla no se entera
        // de por qué el formulario no avanza.
        aria-describedby={idAyuda}
        aria-invalid={error ? true : undefined}
        className="w-full rounded-md border border-border bg-surface px-3 py-2 text-base outline-none transition-colors placeholder:text-subtle focus:border-primary"
        {...props}
      />
      {(ayuda || error) && (
        <p
          id={idAyuda}
          className={`text-xs ${error ? "text-danger" : "text-subtle"}`}
        >
          {error ?? ayuda}
        </p>
      )}
    </div>
  );
}

/**
 * Botón.
 *
 * El primario lleva sombra y se hunde al pulsar; el secundario no. Esa
 * diferencia física —uno tiene relieve, el otro está plano sobre la página— es
 * lo que hace que se distingan sin necesidad de mirar el color, y funciona
 * igual para quien no distingue el verde.
 *
 * Mientras carga se enseña un indicador que gira, no solo texto: un botón que
 * cambia a «Un momento…» y se queda quieto parece colgado.
 */
export function Boton({
  cargando,
  variante = "primario",
  children,
  className = "",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  cargando?: boolean;
  variante?: "primario" | "secundario";
}) {
  const estilos =
    variante === "primario"
      ? "bg-primary text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
      : "border border-border bg-surface hover:border-border-strong hover:bg-surface-sunken";

  return (
    <button
      className={`boton-vivo inline-flex w-full items-center justify-center gap-2 rounded-md px-4 py-2.5 font-medium disabled:pointer-events-none disabled:opacity-55 ${estilos} ${className}`}
      disabled={cargando || props.disabled}
      {...props}
    >
      {cargando && <Girando />}
      {cargando ? "Un momento…" : children}
    </button>
  );
}

/** Indicador de espera. `aria-hidden` porque el texto de al lado ya lo dice. */
function Girando() {
  return (
    <svg viewBox="0 0 24 24" className="size-4 shrink-0 animate-spin" aria-hidden>
      <circle
        cx="12"
        cy="12"
        r="9"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        opacity="0.25"
      />
      <path
        d="M21 12a9 9 0 0 0-9-9"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
      />
    </svg>
  );
}

export function Tarjeta({
  titulo,
  descripcion,
  className = "",
  children,
}: {
  titulo?: string;
  descripcion?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      className={`rounded-lg border border-border bg-surface p-6 shadow-sm ${className}`}
    >
      {titulo && (
        <h2 className="text-lg font-semibold tracking-tight">{titulo}</h2>
      )}
      {descripcion && (
        <p className="mt-1 text-sm text-muted text-pretty">{descripcion}</p>
      )}
      {(titulo || descripcion) && <div className="h-5" />}
      {children}
    </div>
  );
}

export function Aviso({
  tono = "info",
  children,
}: {
  tono?: "info" | "error" | "exito" | "atencion";
  children: React.ReactNode;
}) {
  // El color nunca es la única señal: cada tono lleva su icono, para quien no
  // distingue colores.
  const estilos = {
    info: { clase: "border-border bg-surface-sunken text-muted", icono: "i" },
    error: { clase: "border-danger/30 bg-danger/10 text-danger", icono: "!" },
    exito: {
      clase: "border-success/30 bg-success/10 text-success",
      icono: "✓",
    },
    atencion: {
      clase: "border-warning/30 bg-warning/10 text-warning",
      icono: "!",
    },
  }[tono];

  return (
    <div
      role={tono === "error" ? "alert" : undefined}
      className={`flex gap-2.5 rounded-md border p-3 text-sm ${estilos.clase}`}
    >
      <span aria-hidden className="font-semibold">
        {estilos.icono}
      </span>
      <div className="text-pretty">{children}</div>
    </div>
  );
}

/** Extrae el mensaje de un Problem Details (RFC 9457) devuelto por la API. */
export async function mensajeDeError(respuesta: Response): Promise<string> {
  try {
    const problema = await respuesta.json();
    return problema?.detail ?? "Algo ha fallado. Inténtalo de nuevo.";
  } catch {
    return "No hay conexión con el servidor.";
  }
}
