"use client";

import { Boton, Aviso } from "@/components/ui";
import { precio, type SlotDestacado } from "@/lib/api";
import type { ServidorPropio } from "@/lib/sesion";
import { useState } from "react";

/**
 * Compra de un espacio destacado.
 *
 * El importe **no se envía**: solo el espacio y el servidor. El precio lo pone
 * el backend leyéndolo de la base de datos. Si lo aceptara del navegador,
 * cualquiera podría comprar un espacio de 25 € por un céntimo cambiando un
 * número en las herramientas del desarrollador — es el error clásico de las
 * integraciones de pago y sigue apareciendo constantemente.
 */
export function Comprar({
  slots,
  servidores,
  slotInicial,
}: {
  slots: SlotDestacado[];
  servidores: ServidorPropio[];
  slotInicial: number | null;
}) {
  const disponibles = slots.filter((s) => s.disponible);

  const [slot, setSlot] = useState<number>(
    slotInicial && disponibles.some((s) => s.posicion === slotInicial)
      ? slotInicial
      : (disponibles[0]?.posicion ?? 0),
  );
  const [servidorId, setServidorId] = useState(servidores[0]?.id ?? "");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const elegido = slots.find((s) => s.posicion === slot);

  if (servidores.length === 0) {
    return (
      <Aviso tono="atencion">
        Primero registra un servidor. Los espacios destacados se compran para un
        servidor concreto.
      </Aviso>
    );
  }

  if (disponibles.length === 0) {
    return (
      <Aviso tono="atencion">
        Ahora mismo no hay espacios libres. Los periodos son de una semana, así
        que vuelve a mirar en unos días.
      </Aviso>
    );
  }

  async function comprar() {
    setEnviando(true);
    setError(null);

    try {
      const respuesta = await fetch("/api/backend/pagos/slots", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          // Si el usuario pulsa dos veces o se le va la conexión y reintenta,
          // el backend reconoce la misma clave y devuelve el pedido que ya
          // existía en lugar de crear otro.
          "Idempotency-Key": crypto.randomUUID(),
        },
        body: JSON.stringify({ slot, servidorId }),
      });

      if (!respuesta.ok) {
        const detalle = await respuesta.json().catch(() => null);
        throw new Error(detalle?.detail ?? "No se pudo iniciar el pago.");
      }

      const { urlPago }: { urlPago: string } = await respuesta.json();
      window.location.href = urlPago;
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo iniciar el pago.");
      setEnviando(false);
    }
  }

  const fechas = elegido
    ? `${formatoFecha(elegido.proximoDesde)} → ${formatoFecha(elegido.proximoHasta)}`
    : "";

  return (
    <div className="space-y-5">
      {/* Con un solo servidor no hay nada que elegir: un desplegable de una
          opción es una decisión falsa que sugiere que falta algo por hacer. Se
          enseña cuál es y se sigue. Con varios, se elige. */}
      {servidores.length === 1 ? (
        <div>
          <span className="mb-1.5 block text-sm font-medium">Servidor</span>
          <p className="rounded-md border border-border bg-surface-sunken px-3 py-2.5">
            {servidores[0].nombre}
          </p>
        </div>
      ) : (
        <div>
          <label htmlFor="servidor" className="mb-1.5 block text-sm font-medium">
            Servidor
          </label>
          <select
            id="servidor"
            value={servidorId}
            onChange={(e) => setServidorId(e.target.value)}
            className="w-full rounded-md border border-border bg-surface px-3 py-2.5"
          >
            {servidores.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </select>
        </div>
      )}

      <fieldset>
        <legend className="mb-1.5 text-sm font-medium">Espacio</legend>
        <div className="grid grid-cols-5 gap-2">
          {slots.map((s) => (
            <button
              key={s.posicion}
              type="button"
              disabled={!s.disponible}
              onClick={() => setSlot(s.posicion)}
              aria-pressed={s.posicion === slot}
              className={`rounded-lg border p-3 text-center transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                s.posicion === slot
                  ? "border-primary bg-primary/10"
                  : "border-border bg-surface hover:bg-surface-sunken"
              }`}
            >
              <div className="text-lg font-semibold">{s.posicion}</div>
              <div className="mt-0.5 text-[11px] text-muted">
                {s.disponible ? precio(s.precioCentimos, s.moneda) : "lleno"}
              </div>
            </button>
          ))}
        </div>
      </fieldset>

      {elegido && (
        <div className="rounded-lg border border-border bg-surface-sunken p-4 text-sm">
          <div className="flex justify-between">
            <span className="text-muted">Periodo</span>
            <span>{fechas}</span>
          </div>
          <div className="mt-2 flex justify-between">
            <span className="text-muted">Duración</span>
            <span>{elegido.diasPeriodo} días</span>
          </div>
          <div className="mt-2 flex justify-between border-t border-border pt-2 font-medium">
            <span>Total</span>
            <span>{precio(elegido.precioCentimos, elegido.moneda)}</span>
          </div>

          {elegido.periodosEnCola > 0 && (
            <p className="mt-3 text-xs text-muted text-pretty">
              Este espacio está ocupado. Tu servidor entra en la rotación de la
              fecha de arriba, que ya está confirmada — no es una estimación.
            </p>
          )}
        </div>
      )}

      {error && <Aviso tono="error">{error}</Aviso>}

      <Boton onClick={comprar} cargando={enviando} disabled={!elegido || !servidorId}>
        Pagar con PayPal
      </Boton>

      <p className="text-xs text-subtle text-pretty">
        Te llevamos a PayPal para pagar. No guardamos ningún dato de tu tarjeta:
        nunca pasan por aquí.
      </p>
    </div>
  );
}

function formatoFecha(iso: string): string {
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
  });
}
