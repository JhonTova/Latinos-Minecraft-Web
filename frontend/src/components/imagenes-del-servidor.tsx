"use client";

import { Motd } from "@/components/motd";
import { SubirImagen } from "@/components/subir-imagen";
import { Aviso, Boton } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";

/**
 * Elige de dónde salen el icono y el banner de un servidor (ADR-0027).
 *
 * Dos opciones por imagen, y por separado: icono automático con banner subido es
 * una combinación normal y con un solo interruptor sería imposible.
 *
 * La regla que hace esto usable es que cambiar de modo **no destruye nada**. Lo
 * automático y lo subido viven en sitios distintos, así que probar el automático
 * y volver es un clic de ida y otro de vuelta. Si el cambio borrara el banner que
 * alguien pagó a un diseñador, nadie se atrevería a probarlo.
 */
export interface ImagenesServidor {
  iconoAutomatico: boolean;
  bannerAutomatico: boolean;
  iconoAutoUrl: string | null;
  bannerAutoUrl: string | null;
  iconoSubidoUrl: string | null;
  bannerSubidoUrl: string | null;
  motd: string | null;
  bloqueadas: boolean;
}

export function ImagenesDelServidor({
  servidorId,
  imagenes,
}: {
  servidorId: string;
  imagenes: ImagenesServidor;
}) {
  const router = useRouter();
  const [actualizando, empezarActualizacion] = useTransition();
  const [mensaje, setMensaje] = useState<{
    tono: "exito" | "atencion" | "error";
    texto: string;
  } | null>(null);

  async function actualizarAhora() {
    setMensaje(null);

    const respuesta = await fetch(
      `/api/backend/servers/${servidorId}/imagenes/automaticas/actualizar`,
      { method: "POST" },
    );

    if (!respuesta.ok) {
      const problema = await respuesta.json().catch(() => null);
      setMensaje({
        tono: "error",
        texto: problema?.detail ?? "No hemos podido leer tu servidor ahora mismo.",
      });
      return;
    }

    const { resultado, mensaje: texto } = await respuesta.json();
    setMensaje({
      tono:
        resultado === "ACTUALIZADO"
          ? "exito"
          : resultado === "SERVIDOR_CAIDO" || resultado === "BLOQUEADO"
            ? "error"
            : "atencion",
      texto,
    });

    empezarActualizacion(() => router.refresh());
  }

  if (imagenes.bloqueadas) {
    return (
      <Aviso tono="error">
        Hemos retirado las imágenes automáticas de este servidor. Puedes seguir
        subiendo las tuyas. Si crees que es un error, escríbenos.
      </Aviso>
    );
  }

  return (
    <div className="space-y-8">
      <BloqueDeImagen
        servidorId={servidorId}
        tipo="icono"
        automatico={imagenes.iconoAutomatico}
        urlAuto={imagenes.iconoAutoUrl}
        urlSubida={imagenes.iconoSubidoUrl}
        explicacionAutomatica="El mismo icono que ve un jugador en su lista de servidores."
      />

      <BloqueDeImagen
        servidorId={servidorId}
        tipo="banner"
        automatico={imagenes.bannerAutomatico}
        urlAuto={imagenes.bannerAutoUrl}
        urlSubida={imagenes.bannerSubidoUrl}
        explicacionAutomatica="Lo dibujamos con tu icono y tu MOTD, igual que el menú del juego."
      />

      {/* --- Lo que leemos de tu servidor ------------------------------- */}
      <div className="rounded-lg border border-border bg-surface-sunken p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-sm font-medium">Lo que anuncia tu servidor</p>
            {imagenes.motd ? (
              <div className="mt-2 rounded-md bg-[#26262b] px-3 py-2">
                <Motd motd={imagenes.motd} className="text-white" />
              </div>
            ) : (
              <p className="mt-1 text-sm text-muted text-pretty">
                Todavía no hemos podido leer tu MOTD. Lo intentamos cada pocos
                minutos; si tu servidor está encendido, aparecerá solo.
              </p>
            )}
          </div>

          <Boton
            variante="secundario"
            className="w-auto shrink-0"
            onClick={actualizarAhora}
            disabled={actualizando}
          >
            {actualizando ? "Leyendo…" : "Actualizar ahora"}
          </Boton>
        </div>

        <p className="mt-3 text-xs text-subtle text-pretty">
          Lo leemos solos cada pocas horas. Si acabas de cambiar tu icono o tu
          MOTD, pulsa el botón para verlo ya.
        </p>

        {mensaje && (
          <div className="mt-3">
            <Aviso tono={mensaje.tono}>{mensaje.texto}</Aviso>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Un bloque por imagen: el selector de modo y, debajo, lo que corresponda.
 *
 * Las dos vistas previas se enseñan a la vez a propósito. Elegir entre
 * "automático" y "mi imagen" leyendo la descripción de dos botones es adivinar;
 * viendo las dos, una al lado de la otra, es mirar.
 */
function BloqueDeImagen({
  servidorId,
  tipo,
  automatico,
  urlAuto,
  urlSubida,
  explicacionAutomatica,
}: {
  servidorId: string;
  tipo: "icono" | "banner";
  automatico: boolean;
  urlAuto: string | null;
  urlSubida: string | null;
  explicacionAutomatica: string;
}) {
  const router = useRouter();
  const [cambiando, setCambiando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const esIcono = tipo === "icono";

  async function cambiarModo(aAutomatico: boolean) {
    if (aAutomatico === automatico) return;

    setCambiando(true);
    setError(null);

    const respuesta = await fetch(
      `/api/backend/servers/${servidorId}/imagenes/${tipo}/origen?origen=${
        aAutomatico ? "auto" : "manual"
      }`,
      { method: "PUT" },
    );

    if (!respuesta.ok) {
      setError("No se pudo cambiar el modo. Inténtalo otra vez.");
      setCambiando(false);
      return;
    }

    setCambiando(false);
    router.refresh();
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <span className="text-sm font-medium">
          {esIcono ? "Icono" : "Banner"}
        </span>

        <div
          role="radiogroup"
          aria-label={`Origen del ${tipo}`}
          className="inline-flex rounded-md border border-border p-0.5"
        >
          <BotonDeModo
            activo={automatico}
            disabled={cambiando}
            onClick={() => cambiarModo(true)}
          >
            Automático
          </BotonDeModo>
          <BotonDeModo
            activo={!automatico}
            disabled={cambiando}
            onClick={() => cambiarModo(false)}
          >
            Mi imagen
          </BotonDeModo>
        </div>
      </div>

      {automatico ? (
        <div className="flex items-center gap-4 rounded-lg border border-border p-4">
          {urlAuto ? (
            // Imagen normal y no next/image: la sirve nuestra propia API con las
            // cabeceras que ya controlamos, y el optimizador no aporta nada
            // sobre un PNG que hemos generado nosotros.
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={urlAuto}
              alt=""
              // Píxeles duros: la imagen es de estética de píxel y suavizarla al
              // escalar es justo lo que la haría dejar de parecerse al juego.
              style={{ imageRendering: "pixelated" }}
              className={
                esIcono
                  ? "size-16 shrink-0 rounded-md"
                  : "h-16 w-40 shrink-0 rounded-md object-cover"
              }
            />
          ) : (
            <div
              className={`shrink-0 animate-pulse rounded-md bg-surface-sunken ${
                esIcono ? "size-16" : "h-16 w-40"
              }`}
              aria-hidden
            />
          )}

          <div className="min-w-0 text-sm">
            {urlAuto ? (
              <>
                <p className="font-medium">Cogida de tu servidor</p>
                <p className="mt-0.5 text-muted text-pretty">
                  {explicacionAutomatica}
                </p>
              </>
            ) : (
              <>
                <p className="font-medium">Todavía no la tenemos</p>
                <p className="mt-0.5 text-muted text-pretty">
                  {esIcono
                    ? "Tu servidor no anuncia icono, o aún no hemos podido leerlo."
                    : "En cuanto leamos tu servidor, lo dibujamos."}
                </p>
              </>
            )}
          </div>
        </div>
      ) : (
        <SubirImagen servidorId={servidorId} tipo={tipo} urlActual={urlSubida} />
      )}

      {error && <Aviso tono="error">{error}</Aviso>}
    </div>
  );
}

function BotonDeModo({
  activo,
  disabled,
  onClick,
  children,
}: {
  activo: boolean;
  disabled: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={activo}
      disabled={disabled}
      onClick={onClick}
      className={`rounded px-3 py-1 text-sm transition-colors disabled:opacity-50 ${
        activo
          ? "bg-primary text-on-accent"
          : "text-muted hover:text-foreground"
      }`}
    >
      {children}
    </button>
  );
}
