"use client";

import { Aviso } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";

/**
 * Subida de icono o banner.
 *
 * El SVG ni siquiera aparece en el selector de archivos: el servidor lo rechaza
 * igualmente (ADR-0017), pero es mejor que el usuario no llegue a elegir algo
 * que se le va a rebotar.
 */
export function SubirImagen({
  servidorId,
  tipo,
  urlActual,
}: {
  servidorId: string;
  tipo: "icono" | "banner";
  urlActual: string | null;
}) {
  const router = useRouter();
  const entrada = useRef<HTMLInputElement>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [arrastrando, setArrastrando] = useState(false);
  const [vistaPrevia, setVistaPrevia] = useState(urlActual);

  const esIcono = tipo === "icono";

  async function subir(archivo: File) {
    setError(null);
    setSubiendo(true);

    const datos = new FormData();
    datos.append("archivo", archivo);

    const respuesta = await fetch(
      `/api/backend/servers/${servidorId}/imagenes/${tipo}`,
      { method: "POST", body: datos },
    );

    // 413 lo devuelve el proxy de delante, no la aplicación, así que no trae
    // explicación en JSON. Sin este caso, un límite del servidor web se ve como
    // un "no se pudo subir" que no apunta a ninguna parte.
    if (respuesta.status === 413) {
      setError(
        "La imagen pesa demasiado para el servidor. Prueba con una más ligera.",
      );
      setSubiendo(false);
      return;
    }

    if (!respuesta.ok) {
      const problema = await respuesta.json().catch(() => null);
      setError(problema?.detail ?? "No se pudo subir la imagen.");
      setSubiendo(false);
      return;
    }

    const { url } = await respuesta.json();
    setVistaPrevia(url);
    setSubiendo(false);
    router.refresh();
  }

  function alSoltar(evento: React.DragEvent) {
    evento.preventDefault();
    setArrastrando(false);
    const archivo = evento.dataTransfer.files?.[0];
    if (archivo) subir(archivo);
  }

  return (
    <div className="space-y-2">
      <div className="flex items-baseline justify-between">
        <span className="text-sm font-medium">
          {esIcono ? "Icono" : "Banner"}
        </span>
        <span className="text-xs text-subtle">
          {esIcono ? "PNG o JPG · máx. 2 MB" : "PNG, JPG o GIF · máx. 4 MB"}
        </span>
      </div>

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setArrastrando(true);
        }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={alSoltar}
        onClick={() => entrada.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          // Sin esto, quien navega con teclado no puede subir nada: un div con
          // onClick es invisible para el teclado.
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            entrada.current?.click();
          }
        }}
        className={`flex cursor-pointer items-center gap-4 rounded-lg border border-dashed p-4 transition-colors ${
          arrastrando
            ? "border-primary bg-primary-subtle"
            : "border-border hover:border-border-strong"
        }`}
      >
        {vistaPrevia ? (
          // Imagen normal y no next/image: la sirve nuestra propia API con
          // cabeceras que ya controlamos, y el optimizador no aporta nada sobre
          // un PNG de 128 píxeles que ya reprocesamos nosotros.
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={vistaPrevia}
            alt=""
            className={
              esIcono
                ? "size-16 shrink-0 rounded-md object-cover"
                : "h-16 w-40 shrink-0 rounded-md object-cover"
            }
          />
        ) : (
          <div
            className={`shrink-0 rounded-md bg-surface-sunken ${
              esIcono ? "size-16" : "h-16 w-40"
            }`}
            aria-hidden
          />
        )}

        <div className="min-w-0 text-sm">
          <p className="font-medium">
            {subiendo
              ? "Subiendo…"
              : vistaPrevia
                ? "Cambiar imagen"
                : "Arrastra una imagen o pulsa aquí"}
          </p>
          <p className="mt-0.5 text-muted text-pretty">
            {esIcono
              ? "Se recortará a un cuadrado de 128×128."
              : "Se ajustará a 1200×300 como máximo."}
          </p>
        </div>
      </div>

      <input
        ref={entrada}
        type="file"
        // Ni se ofrece SVG. El servidor lo rechaza igual, pero no tiene sentido
        // dejar que lo elija para luego rebotárselo.
        accept={esIcono ? "image/png,image/jpeg,image/webp" : "image/png,image/jpeg,image/webp,image/gif"}
        className="hidden"
        onChange={(e) => {
          const archivo = e.target.files?.[0];
          if (archivo) subir(archivo);
          e.target.value = "";
        }}
      />

      {error && <Aviso tono="error">{error}</Aviso>}
    </div>
  );
}
