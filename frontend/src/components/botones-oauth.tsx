const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

const PROVEEDORES: Record<string, { nombre: string; icono: React.ReactNode }> = {
  GOOGLE: {
    nombre: "Google",
    icono: (
      <svg viewBox="0 0 24 24" className="size-[18px]" aria-hidden>
        <path
          fill="#4285F4"
          d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.76h3.57c2.08-1.92 3.28-4.74 3.28-8.09Z"
        />
        <path
          fill="#34A853"
          d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.76c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z"
        />
        <path
          fill="#FBBC05"
          d="M5.84 14.11a6.6 6.6 0 0 1 0-4.22V7.05H2.18a11 11 0 0 0 0 9.9l3.66-2.84Z"
        />
        <path
          fill="#EA4335"
          d="M12 4.75c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 1.46 14.97.5 12 .5a11 11 0 0 0-9.82 6.05l3.66 2.84c.87-2.6 3.3-4.64 6.16-4.64Z"
        />
      </svg>
    ),
  },
  DISCORD: {
    nombre: "Discord",
    icono: (
      <svg viewBox="0 0 24 24" fill="#5865F2" className="size-[18px]" aria-hidden>
        <path d="M20.3 4.4A19.8 19.8 0 0 0 15.4 3l-.24.44a18.3 18.3 0 0 1 4.35 1.4A15.6 15.6 0 0 0 12 3.9a15.6 15.6 0 0 0-7.5 .94 18.3 18.3 0 0 1 4.34-1.4L8.6 3a19.8 19.8 0 0 0-4.9 1.4C.9 8.5.16 12.5.53 16.4a19.9 19.9 0 0 0 6.03 3.05l1.2-1.7a13 13 0 0 1-1.94-.93l.48-.37a14.2 14.2 0 0 0 11.4 0l.48.37a13 13 0 0 1-1.94.94l1.2 1.69a19.9 19.9 0 0 0 6.03-3.05c.44-4.5-.74-8.47-3.17-12ZM8.4 14.2c-1.17 0-2.13-1.08-2.13-2.4 0-1.33.94-2.4 2.13-2.4s2.15 1.08 2.13 2.4c0 1.32-.94 2.4-2.13 2.4Zm7.2 0c-1.17 0-2.13-1.08-2.13-2.4 0-1.33.94-2.4 2.13-2.4s2.14 1.08 2.12 2.4c0 1.32-.93 2.4-2.12 2.4Z" />
      </svg>
    ),
  },
};

/**
 * Botones de acceso con Google y Discord.
 *
 * Solo aparecen los proveedores que tienen credenciales configuradas. Si no hay
 * ninguno, este componente no pinta nada — mejor eso que un botón que lleva a un
 * error.
 */
export async function BotonesOAuth() {
  let disponibles: string[] = [];

  try {
    const respuesta = await fetch(`${API}/auth/oauth`, { cache: "no-store" });
    if (respuesta.ok) {
      disponibles = await respuesta.json();
    }
  } catch {
    // Si la API no responde, el formulario de correo sigue funcionando. No tiene
    // sentido romper toda la página de login por unos botones opcionales.
    return null;
  }

  if (disponibles.length === 0) {
    return null;
  }

  return (
    <div className="space-y-3">
      <div className="grid gap-2">
        {disponibles.map((codigo) => {
          const proveedor = PROVEEDORES[codigo];
          if (!proveedor) return null;
          return (
            <a
              key={codigo}
              // Ruta relativa a propósito: la dirección del backend no se publica
              // y el enlace vale igual en local, en pruebas y en producción sin
              // depender de ninguna variable. Antes se construía con una variable
              // que nadie definía, así que el botón llevaba al usuario a su propia
              // máquina.
              href={`/api/backend/auth/oauth/${codigo.toLowerCase()}`}
              className="flex items-center justify-center gap-2.5 rounded-md border border-border bg-surface px-4 py-2.5 font-medium transition-colors hover:bg-surface-sunken"
            >
              {proveedor.icono}
              Continuar con {proveedor.nombre}
            </a>
          );
        })}
      </div>

      <div className="flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <span className="text-xs text-subtle">o con tu correo</span>
        <div className="h-px flex-1 bg-border" />
      </div>
    </div>
  );
}
