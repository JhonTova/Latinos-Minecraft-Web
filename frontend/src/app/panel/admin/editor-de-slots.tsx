"use client";

import { Aviso, Boton } from "@/components/ui";
import type { SlotConfigurable } from "@/lib/sesion";
import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Edición de los cinco espacios destacados.
 *
 * Cambiar un precio **no toca lo ya vendido**: cada periodo reservado guarda sus
 * propias fechas y cada pedido su propio importe. Eso es lo que permite tocar el
 * precio sin miedo — y es lo que dice el aviso de abajo, porque quien administra
 * necesita saberlo antes de pulsar, no después.
 */
export function EditorDeSlots({ slots }: { slots: SlotConfigurable[] }) {
  return (
    <div className="space-y-3">
      {slots.map((slot) => (
        <FilaDeSlot key={slot.posicion} slot={slot} />
      ))}
    </div>
  );
}

function FilaDeSlot({ slot }: { slot: SlotConfigurable }) {
  const router = useRouter();

  // Los euros se editan como texto y se convierten a céntimos al guardar. Un
  // input numérico en euros invita a los decimales flotantes, y el dinero no se
  // guarda nunca en coma flotante.
  const [euros, setEuros] = useState((slot.precioCentimos / 100).toFixed(2));
  const [dias, setDias] = useState(String(slot.diasPeriodo));
  const [adelanto, setAdelanto] = useState(String(slot.maxAdelanto));
  const [activo, setActivo] = useState(slot.activo);

  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<"ok" | string | null>(null);

  async function guardar() {
    setGuardando(true);
    setResultado(null);

    // Se redondea al céntimo entero más cercano en vez de truncar: escribir
    // "9,999" debe costar 10,00 €, no 9,99 €.
    const centimos = Math.round(Number(euros.replace(",", ".")) * 100);

    if (!Number.isFinite(centimos) || centimos < 0) {
      setResultado("Ese precio no es válido.");
      setGuardando(false);
      return;
    }

    try {
      const respuesta = await fetch(`/api/backend/admin/slots/${slot.posicion}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          precioCentimos: centimos,
          diasPeriodo: Number(dias),
          maxAdelanto: Number(adelanto),
          activo,
        }),
      });

      if (!respuesta.ok) {
        const detalle = await respuesta.json().catch(() => null);
        throw new Error(detalle?.detail ?? "No se pudo guardar.");
      }

      setResultado("ok");
      router.refresh();
    } catch (e) {
      setResultado(e instanceof Error ? e.message : "No se pudo guardar.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="rounded-lg border border-border bg-surface p-4">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="font-medium">Espacio {slot.posicion}</h3>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={activo}
            onChange={(e) => setActivo(e.target.checked)}
            className="size-4"
          />
          A la venta
        </label>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <Numero
          etiqueta={`Precio (${slot.moneda})`}
          valor={euros}
          onChange={setEuros}
          id={`precio-${slot.posicion}`}
        />
        <Numero
          etiqueta="Días"
          valor={dias}
          onChange={setDias}
          id={`dias-${slot.posicion}`}
        />
        <Numero
          etiqueta="Periodos por adelantado"
          valor={adelanto}
          onChange={setAdelanto}
          id={`adelanto-${slot.posicion}`}
        />
      </div>

      <div className="mt-3 flex items-center gap-3">
        <div className="w-32">
          <Boton onClick={guardar} cargando={guardando} variante="secundario">
            Guardar
          </Boton>
        </div>
        {resultado === "ok" && (
          <span className="text-sm text-success">Guardado</span>
        )}
        {resultado && resultado !== "ok" && (
          <span className="text-sm text-danger">{resultado}</span>
        )}
      </div>
    </div>
  );
}

function Numero({
  etiqueta,
  valor,
  onChange,
  id,
}: {
  etiqueta: string;
  valor: string;
  onChange: (v: string) => void;
  id: string;
}) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-xs text-muted">
        {etiqueta}
      </label>
      <input
        id={id}
        inputMode="decimal"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-md border border-border bg-surface px-3 py-2"
      />
    </div>
  );
}

export function AvisoDeCambioDePrecio() {
  return (
    <Aviso tono="info">
      Cambiar un precio afecta solo a las compras futuras. Lo ya vendido conserva
      su importe y sus fechas.
    </Aviso>
  );
}
