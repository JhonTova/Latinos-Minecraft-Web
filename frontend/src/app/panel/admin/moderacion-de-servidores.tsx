"use client";

import { Aviso, Boton } from "@/components/ui";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";

/**
 * Suspender y reactivar servidores (ADR-0028).
 *
 * Suspender saca al servidor del directorio, del ranking, de la búsqueda, del
 * sondeo y deja de aceptarle votos. **No borra nada**: al levantarla el servidor
 * vuelve exactamente como estaba.
 *
 * Y no caduca. Un temporizador de borrado sobre la suspensión destruiría la
 * prueba de por qué suspendimos a alguien justo cuando más falta hace —cuando
 * vuelve, o reclama— y le bastaría con esperar para que su historial se limpiara
 * solo.
 */
export interface ServidorAdministrable {
  id: string;
  slug: string;
  nombre: string;
  estado: string;
  motivoDeSuspension: string | null;
  iconoUrl: string | null;
  votosSemana: number;
}

const ESTADOS: Record<string, string> = {
  ACTIVE: "Activo",
  SUSPENDED: "Suspendido",
  ARCHIVED: "Archivado",
  DRAFT: "Borrador",
  PENDING_VERIFICATION: "Sin verificar",
};

export function ModeracionDeServidores({
  servidores,
}: {
  servidores: ServidorAdministrable[];
}) {
  const [busqueda, setBusqueda] = useState("");

  // Con trescientos servidores, una lista sin buscador obliga a bajar hasta
  // encontrar el que te acaban de reportar. El buscador es lo que hace que esta
  // pantalla sirva cuando hay prisa.
  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    if (!q) return servidores;
    return servidores.filter(
      (s) =>
        s.nombre.toLowerCase().includes(q) || s.slug.toLowerCase().includes(q),
    );
  }, [servidores, busqueda]);

  if (servidores.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border py-12 text-center text-sm text-muted">
        Todavía no hay ningún servidor registrado.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <input
        type="search"
        value={busqueda}
        onChange={(e) => setBusqueda(e.target.value)}
        placeholder="Buscar por nombre o dirección…"
        className="w-full rounded-md border border-border bg-surface px-3 py-2 text-sm"
      />

      {visibles.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border py-8 text-center text-sm text-muted">
          Ningún servidor coincide con «{busqueda}».
        </div>
      ) : (
        visibles.map((s) => <Fila key={s.id} servidor={s} />)
      )}
    </div>
  );
}

function Fila({ servidor }: { servidor: ServidorAdministrable }) {
  const router = useRouter();
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [motivo, setMotivo] = useState("");
  const [confirmando, setConfirmando] = useState(false);

  const suspendido = servidor.estado === "SUSPENDED";

  async function llamar(accion: "suspender" | "levantar-suspension") {
    if (accion === "suspender" && !motivo.trim()) {
      setError("El motivo es obligatorio: el dueño lo va a leer.");
      return;
    }

    setTrabajando(true);
    setError(null);

    const respuesta = await fetch(
      `/api/backend/admin/servidores/${servidor.id}/${accion}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: accion === "suspender" ? JSON.stringify({ motivo }) : undefined,
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
        suspendido ? "border-danger/30 bg-danger/5" : "border-border bg-surface"
      }`}
    >
      <div className="flex items-start gap-3">
        {servidor.iconoUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={servidor.iconoUrl}
            alt=""
            style={{ imageRendering: "pixelated" }}
            className="size-10 shrink-0 rounded-md"
          />
        ) : (
          <div className="size-10 shrink-0 rounded-md bg-surface-sunken" aria-hidden />
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
            <span
              className={`rounded px-1.5 py-0.5 text-xs ${
                suspendido
                  ? "bg-danger/15 text-danger"
                  : "bg-surface-sunken text-muted"
              }`}
            >
              {ESTADOS[servidor.estado] ?? servidor.estado}
            </span>
            <span className="text-xs text-subtle">
              {servidor.votosSemana} votos esta semana
            </span>
          </div>

          {suspendido && servidor.motivoDeSuspension && (
            <p className="mt-1 text-xs text-muted text-pretty">
              Motivo: {servidor.motivoDeSuspension}
            </p>
          )}
        </div>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {suspendido ? (
          <Boton
            variante="secundario"
            className="w-auto px-3 py-1.5 text-sm"
            disabled={trabajando}
            onClick={() => llamar("levantar-suspension")}
          >
            Reactivar
          </Boton>
        ) : confirmando ? (
          <>
            <input
              type="text"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Motivo — lo lee el dueño"
              maxLength={500}
              className="min-w-0 flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-sm"
            />
            <Boton
              className="w-auto px-3 py-1.5 text-sm"
              disabled={trabajando}
              onClick={() => llamar("suspender")}
            >
              Suspender
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
            Suspender
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
