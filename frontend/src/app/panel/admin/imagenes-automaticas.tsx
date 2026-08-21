"use client";

import { Motd } from "@/components/motd";
import { Aviso, Boton } from "@/components/ui";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useState } from "react";

/**
 * Revisión de las imágenes que cogemos del ping de los servidores (ADR-0027).
 *
 * Esta pantalla es la mitad que hace aceptable la otra. Adoptamos el icono y el
 * MOTD de un servidor ajeno al instante y los servimos desde nuestro dominio;
 * eso solo es defendible si hay un sitio donde verlos y un botón para quitarlos.
 * Sin esto, la política elegida sería "publicar" a secas.
 *
 * Bloquear **no suspende el servidor ni le toca los votos**: es una medida sobre
 * sus imágenes. El servidor sigue en el directorio con el hueco por defecto, y
 * su dueño puede subir las suyas. Para lo otro está la suspensión, que es una
 * decisión distinta y más grave.
 */
export interface ServidorConImagenAutomatica {
  id: string;
  slug: string;
  nombre: string;
  motd: string | null;
  iconoUrl: string | null;
  bannerUrl: string | null;
  bloqueado: boolean;
  motivoDelBloqueo: string | null;
  actualizadoEn: string | null;
}

export function ImagenesAutomaticas({
  servidores,
}: {
  servidores: ServidorConImagenAutomatica[];
}) {
  if (servidores.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border py-12 text-center text-sm text-muted">
        Todavía no hemos cogido imágenes de ningún servidor.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {servidores.map((servidor) => (
        <Fila key={servidor.id} servidor={servidor} />
      ))}
    </div>
  );
}

function Fila({ servidor }: { servidor: ServidorConImagenAutomatica }) {
  const router = useRouter();
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [motivo, setMotivo] = useState("");
  const [confirmando, setConfirmando] = useState(false);

  async function llamar(accion: "bloquear" | "desbloquear") {
    setTrabajando(true);
    setError(null);

    const respuesta = await fetch(
      `/api/backend/admin/servidores/${servidor.id}/imagenes-automaticas/${accion}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: accion === "bloquear" ? JSON.stringify({ motivo }) : undefined,
      },
    );

    if (!respuesta.ok) {
      setError("No se pudo aplicar el cambio.");
      setTrabajando(false);
      return;
    }

    setTrabajando(false);
    setConfirmando(false);
    setMotivo("");
    router.refresh();
  }

  return (
    <div
      className={`rounded-lg border p-4 ${
        servidor.bloqueado
          ? "border-danger/30 bg-danger/5"
          : "border-border bg-surface"
      }`}
    >
      <div className="flex items-start gap-4">
        {servidor.iconoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={servidor.iconoUrl}
            alt=""
            style={{ imageRendering: "pixelated" }}
            className="size-12 shrink-0 rounded-md"
          />
        ) : (
          <div
            className="size-12 shrink-0 rounded-md bg-surface-sunken"
            aria-hidden
          />
        )}

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-2">
            <Link
              href={`/servidores/${servidor.slug}`}
              target="_blank"
              className="font-medium hover:underline"
            >
              {servidor.nombre}
            </Link>
            {servidor.bloqueado && (
              <span className="rounded bg-danger/15 px-1.5 py-0.5 text-xs text-danger">
                Retiradas
              </span>
            )}
          </div>

          {servidor.motd && (
            <div className="mt-2 inline-block rounded-md bg-[#26262b] px-3 py-1.5">
              <Motd motd={servidor.motd} className="text-white" />
            </div>
          )}

          {servidor.bloqueado && servidor.motivoDelBloqueo && (
            <p className="mt-2 text-xs text-muted">
              Motivo: {servidor.motivoDelBloqueo}
            </p>
          )}
        </div>

        {/* El banner completo, pequeño: es lo que se ve en el directorio y es
            justo lo que hay que poder juzgar de un vistazo. */}
        {servidor.bannerUrl && !servidor.bloqueado && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={servidor.bannerUrl}
            alt=""
            style={{ imageRendering: "pixelated" }}
            className="hidden h-12 w-48 shrink-0 rounded-md object-cover sm:block"
          />
        )}
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {servidor.bloqueado ? (
          <Boton
            variante="secundario"
            className="w-auto px-3 py-1.5 text-sm"
            disabled={trabajando}
            onClick={() => llamar("desbloquear")}
          >
            Volver a permitirlas
          </Boton>
        ) : confirmando ? (
          <>
            <input
              type="text"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Motivo (queda registrado)"
              maxLength={300}
              className="min-w-0 flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-sm"
            />
            <Boton
              className="w-auto px-3 py-1.5 text-sm"
              disabled={trabajando}
              onClick={() => llamar("bloquear")}
            >
              Retirar
            </Boton>
            <Boton
              variante="secundario"
              className="w-auto px-3 py-1.5 text-sm"
              disabled={trabajando}
              onClick={() => setConfirmando(false)}
            >
              Cancelar
            </Boton>
          </>
        ) : (
          <Boton
            variante="secundario"
            className="w-auto px-3 py-1.5 text-sm"
            onClick={() => setConfirmando(true)}
          >
            Retirar imágenes
          </Boton>
        )}
      </div>

      {error && (
        <div className="mt-2">
          <Aviso tono="error">{error}</Aviso>
        </div>
      )}
    </div>
  );
}
