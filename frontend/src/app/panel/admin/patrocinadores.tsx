"use client";

import { Aviso, Boton, Campo } from "@/components/ui";
import { refrescarPatrocinadores } from "@/lib/acciones";
import { useRouter } from "next/navigation";
import { useState } from "react";

import type { PatrocinadorAdmin } from "@/lib/sesion";

/**
 * Alta y aprobación de patrocinadores.
 *
 * Un patrocinador **nace sin aprobar** y hay que aprobarlo en un segundo paso,
 * incluso habiéndolo creado tú. No es burocracia: un enlace saliente desde
 * nuestro dominio es nuestra reputación, y separar el alta de la publicación
 * evita que una URL mal tecleada salga directa a la portada.
 */
export function GestionDePatrocinadores({
  patrocinadores,
  ahora,
}: {
  patrocinadores: PatrocinadorAdmin[];
  /**
   * Instante con el que se comparan las fechas, medido en el servidor al cargar
   * la página. Se recibe en vez de leerlo aquí porque el reloj es impuro: leerlo
   * durante el pintado daría un estado distinto en cada renderizado.
   */
  ahora: number;
}) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState<string | null>(null);

  async function accion(id: string, que: "aprobar" | "retirar") {
    setOcupado(id);
    setError(null);
    try {
      const respuesta = await fetch(`/api/backend/admin/patrocinadores/${id}/${que}`, {
        method: "POST",
      });
      if (!respuesta.ok) throw new Error("No se pudo completar la acción.");
      await refrescarPatrocinadores();
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo completar la acción.");
    } finally {
      setOcupado(null);
    }
  }

  return (
    <div className="space-y-4">
      <Alta onCreado={() => router.refresh()} />

      {error && <Aviso tono="error">{error}</Aviso>}

      {patrocinadores.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border py-10 text-center text-sm text-muted">
          Todavía no hay ningún patrocinador.
        </div>
      ) : (
        <ul className="divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface">
          {patrocinadores.map((p) => (
            <li key={p.id} className="flex items-center gap-3 px-4 py-3">
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">{p.nombre}</p>
                <p className="truncate text-xs text-subtle">{p.urlDestino}</p>
                <p className="mt-0.5 text-xs text-muted">
                  {fecha(p.desde)} → {fecha(p.hasta)} · orden {p.orden}
                </p>
              </div>

              <Estado patrocinador={p} ahora={ahora} />

              {!p.canceladoEn && (
                <div className="flex shrink-0 gap-2">
                  {!p.aprobadoEn && (
                    <button
                      onClick={() => accion(p.id, "aprobar")}
                      disabled={ocupado === p.id}
                      className="rounded-md bg-primary px-3 py-1.5 text-sm font-medium text-on-accent transition-colors hover:bg-primary-hover disabled:opacity-60"
                    >
                      Aprobar
                    </button>
                  )}
                  <button
                    onClick={() => accion(p.id, "retirar")}
                    disabled={ocupado === p.id}
                    className="rounded-md border border-border px-3 py-1.5 text-sm transition-colors hover:bg-surface-sunken disabled:opacity-60"
                  >
                    Retirar
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Estado({
  patrocinador,
  ahora,
}: {
  patrocinador: PatrocinadorAdmin;
  ahora: number;
}) {
  const { texto, clase } = patrocinador.canceladoEn
    ? { texto: "Retirado", clase: "text-subtle" }
    : !patrocinador.aprobadoEn
      ? { texto: "Sin aprobar", clase: "text-warning" }
      : new Date(patrocinador.hasta).getTime() < ahora
        ? { texto: "Caducado", clase: "text-subtle" }
        : new Date(patrocinador.desde).getTime() > ahora
          ? { texto: "Programado", clase: "text-muted" }
          : { texto: "Visible", clase: "text-success" };

  return <span className={`shrink-0 text-xs ${clase}`}>{texto}</span>;
}

function Alta({ onCreado }: { onCreado: () => void }) {
  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState("");
  const [url, setUrl] = useState("");
  const [semanas, setSemanas] = useState("4");
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function crear() {
    setGuardando(true);
    setError(null);

    const desde = new Date();
    const hasta = new Date(desde.getTime() + Number(semanas) * 7 * 24 * 60 * 60 * 1000);

    try {
      const respuesta = await fetch("/api/backend/admin/patrocinadores", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          nombre,
          urlDestino: url,
          claveLogo: null,
          desde: desde.toISOString(),
          hasta: hasta.toISOString(),
          orden: 100,
        }),
      });

      if (!respuesta.ok) {
        const detalle = await respuesta.json().catch(() => null);
        // El backend explica por qué rechaza un enlace ("tiene que empezar por
        // http://…"). Ese mensaje es más útil que uno genérico nuestro.
        throw new Error(detalle?.detail ?? "No se pudo crear.");
      }

      setNombre("");
      setUrl("");
      setAbierto(false);
      await refrescarPatrocinadores();
      onCreado();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo crear.");
    } finally {
      setGuardando(false);
    }
  }

  if (!abierto) {
    return (
      <button
        onClick={() => setAbierto(true)}
        className="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium transition-colors hover:bg-surface-sunken"
      >
        Añadir patrocinador
      </button>
    );
  }

  return (
    <div className="space-y-3 rounded-lg border border-border bg-surface p-4">
      <Campo
        id="patrocinador-nombre"
        etiqueta="Nombre"
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        placeholder="Hosting Ejemplo"
      />
      <Campo
        id="patrocinador-url"
        etiqueta="Enlace"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        placeholder="https://hosting-ejemplo.com"
      />
      <Campo
        id="patrocinador-semanas"
        etiqueta="Duración (semanas)"
        value={semanas}
        onChange={(e) => setSemanas(e.target.value)}
        inputMode="numeric"
      />

      {error && <Aviso tono="error">{error}</Aviso>}

      <div className="flex gap-2">
        <Boton onClick={crear} cargando={guardando} disabled={!nombre || !url}>
          Crear sin publicar
        </Boton>
        <Boton onClick={() => setAbierto(false)} variante="secundario">
          Cancelar
        </Boton>
      </div>

      <p className="text-xs text-subtle text-pretty">
        Se crea oculto. Aparecerá en la web cuando lo apruebes, en un segundo
        paso deliberado.
      </p>
    </div>
  );
}

function fecha(iso: string): string {
  return new Date(iso).toLocaleDateString("es-ES", {
    day: "numeric",
    month: "short",
    year: "2-digit",
  });
}
