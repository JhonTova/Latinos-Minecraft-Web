import { BotonSalir } from "@/components/boton-salir";
import { ThemeToggle } from "@/components/theme-toggle";
import { ajustesDelSitio } from "@/lib/api";
import { perfilActual } from "@/lib/sesion";
import Link from "next/link";
import { redirect } from "next/navigation";

/**
 * Marco del panel.
 *
 * La sesión se comprueba aquí, en el layout, y no en cada página: así proteger
 * una pantalla nueva es ponerla dentro de esta carpeta, y *olvidarse* de
 * protegerla exige sacarla a propósito. Es lo contrario de tener que acordarse
 * cada vez.
 */
export default async function LayoutPanel({
  children,
}: {
  children: React.ReactNode;
}) {
  const [perfil, sitio] = await Promise.all([perfilActual(), ajustesDelSitio()]);
  if (!perfil) {
    redirect("/login");
  }

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-border bg-bg/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 w-full max-w-5xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-5">
            {/* La marca lleva a la PORTADA, no al panel. Antes apuntaba a
                /panel, así que desde dentro no había forma de volver a la web
                pública salvo editando la URL a mano. */}
            <Link
              href="/"
              className="group flex items-center gap-2.5 font-semibold"
            >
              <LogotipoPanel url={sitio.logoUrl} nombre={sitio.nombre} />
              <span className="hidden sm:inline">{sitio.nombre}</span>
            </Link>

            <Link
              href="/panel"
              className="text-sm text-muted transition-colors hover:text-text"
            >
              Panel
            </Link>
          </div>

          <div className="flex items-center gap-2">
            <ThemeToggle />
            <span className="hidden text-sm text-muted sm:inline">
              {perfil.nombre}
            </span>
            <BotonSalir />
          </div>
        </div>
      </header>

      {children}
    </>
  );
}

/** Igual que en la cabecera pública: logotipo subido, o las iniciales. */
function LogotipoPanel({ url, nombre }: { url: string | null; nombre: string }) {
  if (url) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={url}
        alt=""
        width={32}
        height={32}
        className="size-8 shrink-0 rounded-lg object-cover shadow-sm transition-transform duration-200 group-hover:scale-105"
      />
    );
  }

  const iniciales =
    nombre.replace(/[^A-Z]/g, "").slice(0, 2) ||
    nombre.slice(0, 2).toUpperCase();

  return (
    <span
      aria-hidden
      className="display grid size-8 shrink-0 place-items-center rounded-lg text-[13px] font-bold text-on-accent shadow-sm transition-transform duration-200 group-hover:scale-105"
      style={{
        background:
          "linear-gradient(135deg, var(--accent-500), var(--accent-700))",
      }}
    >
      {iniciales}
    </span>
  );
}
