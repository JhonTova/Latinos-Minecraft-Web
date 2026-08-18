import { CopiarIp } from "@/components/copiar-ip";
import { DarDeBaja } from "@/components/dar-de-baja";
import { EditorDeFicha } from "@/components/editor-de-ficha";
import { SubirImagen } from "@/components/subir-imagen";
import { Aviso } from "@/components/ui";
import { catalogoDeModalidades, fichaServidor } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

export const metadata: Metadata = { title: "Servidor" };

export default async function PaginaServidor(
  props: PageProps<"/panel/servidores/[servidorId]">,
) {
  const { servidorId } = await props.params;
  const [servidor, catalogo] = await Promise.all([
    fichaServidor(servidorId),
    catalogoDeModalidades(),
  ]);

  if (!servidor) {
    notFound();
  }

  const direccion =
    servidor.puerto === 25565
      ? servidor.host
      : `${servidor.host}:${servidor.puerto}`;

  return (
    <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-8 sm:px-6">
      <Link
        href="/panel"
        className="mb-5 inline-flex items-center gap-1.5 text-sm text-muted transition-colors hover:text-text"
      >
        <span aria-hidden>←</span> Mis servidores
      </Link>

      {/* --- Encabezado: quién es este servidor ------------------------- */}
      <header className="mb-6 flex flex-wrap items-start gap-4">
        {servidor.iconoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={servidor.iconoUrl}
            alt=""
            width={64}
            height={64}
            className="size-16 shrink-0 rounded-xl object-cover ring-1 ring-border"
          />
        ) : (
          <span
            aria-hidden
            className="grid size-16 shrink-0 place-items-center rounded-xl bg-primary-subtle text-2xl font-bold text-primary"
          >
            {servidor.nombre.trim().charAt(0).toUpperCase()}
          </span>
        )}

        <div className="min-w-0 flex-1">
          <h1 className="display text-2xl font-semibold">{servidor.nombre}</h1>

          <div className="mt-2 flex flex-wrap items-center gap-3">
            <CopiarIp direccion={direccion} compacto />

            {servidor.verificado ? (
              <Link
                href={`/servidores/${servidor.slug}`}
                className="enlace-vivo text-sm text-primary"
              >
                Ver ficha pública
              </Link>
            ) : (
              <span className="rounded bg-surface-sunken px-2 py-1 text-xs text-muted">
                Aún no publicado
              </span>
            )}
          </div>
        </div>
      </header>

      {!servidor.verificado && (
        <div className="mb-6">
          <Aviso tono="atencion">
            Tu servidor aún no aparece en el ranking. Se activará solo en cuanto
            el plugin se conecte por primera vez — esa conexión es la prueba de
            que el servidor es tuyo.
          </Aviso>
        </div>
      )}

      {/* --- Contenido -------------------------------------------------- */}
      <div className="space-y-6">
        <Seccion
          titulo="Imágenes"
          descripcion="El icono se ve en el ranking; el banner, en tu ficha y al compartir el enlace."
        >
          <div className="grid gap-6 sm:grid-cols-2">
            <SubirImagen
              servidorId={servidor.id}
              tipo="icono"
              urlActual={servidor.iconoUrl}
            />
            <SubirImagen
              servidorId={servidor.id}
              tipo="banner"
              urlActual={servidor.bannerUrl}
            />
          </div>

          <p className="mt-5 text-xs text-subtle text-pretty">
            Formatos aceptados: PNG, JPG, WebP y GIF. Reprocesamos todo lo que
            subes, así que se eliminan los metadatos — incluida la ubicación que
            guardan las fotos hechas con el móvil.
          </p>
        </Seccion>

        <Seccion
          titulo="Ficha pública"
          descripcion="Lo que ve un jugador antes de decidir si entra."
        >
          <EditorDeFicha
            servidorId={servidor.id}
            ficha={{
              descripcion: servidor.descripcion,
              versionMinima: servidor.versionMinima,
              versionMaxima: servidor.versionMaxima,
              region: servidor.region,
              tema: servidor.tema,
              modalidades: servidor.modalidades,
            }}
            catalogo={catalogo}
          />
        </Seccion>

        <DarDeBaja servidorId={servidor.id} nombre={servidor.nombre} />
      </div>
    </main>
  );
}

/**
 * Sección del panel.
 *
 * El título va FUERA de la caja, no dentro. Así el ojo recorre los títulos de
 * una pasada sin entrar en cada bloque, que es como se lee una página de ajustes
 * cuando ya sabes lo que vienes a cambiar.
 */
function Seccion({
  titulo,
  descripcion,
  children,
}: {
  titulo: string;
  descripcion: string;
  children: React.ReactNode;
}) {
  return (
    <section>
      <h2 className="display text-lg font-semibold">{titulo}</h2>
      <p className="mt-0.5 mb-3 text-sm text-muted text-pretty">{descripcion}</p>
      <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        {children}
      </div>
    </section>
  );
}
