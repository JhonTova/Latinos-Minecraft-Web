"use client";

import { Aviso, Boton } from "@/components/ui";
import type { ReglasDeVoto } from "@/lib/sesion";
import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Reglas de voto de toda la plataforma.
 *
 * Es la palanca más delicada del sitio, y por eso lleva los avisos que lleva:
 * quien la toca tiene que entender la consecuencia ANTES de pulsar, no cuando
 * los votos se desplomen y nadie sepa por qué.
 */
export function ReglasDeVotoEditor({ reglas }: { reglas: ReglasDeVoto }) {
  const router = useRouter();

  const [unVotoAlDia, setUnVotoAlDia] = useState(reglas.unVotoAlDia);
  const [porIp, setPorIp] = useState(String(reglas.maxJugadoresPorIp));
  const [horas, setHoras] = useState(String(Math.round(reglas.ventana / 3600)));

  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<"ok" | string | null>(null);

  const ipEstricto = Number(porIp) <= 2;

  async function guardar() {
    setGuardando(true);
    setResultado(null);

    try {
      const respuesta = await fetch("/api/backend/admin/voto", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          unVotoAlDia,
          maxJugadoresPorIp: Number(porIp),
          ventanaHoras: Number(horas),
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
    <div className="space-y-4 rounded-lg border border-border bg-surface p-5 shadow-sm">
      <label className="flex items-start gap-3">
        <input
          type="checkbox"
          checked={unVotoAlDia}
          onChange={(e) => setUnVotoAlDia(e.target.checked)}
          className="mt-1 size-4 shrink-0"
        />
        <span>
          <span className="font-medium">Un voto al día por jugador</span>
          <span className="mt-0.5 block text-sm text-muted text-pretty">
            Activado, cada jugador vota a <strong>un solo servidor</strong> al
            día. Desactivado, puede votar a uno por cada servidor.
          </span>
        </span>
      </label>

      {unVotoAlDia && (
        <Aviso tono="info">
          Con esto activado el ranking mide <em>cuál es tu servidor favorito</em>
          , no cuánta gente juega en cada uno. Es más difícil de manipular, pero
          también baja el total de votos: quien juega en tres servidores ya solo
          suma a uno.
        </Aviso>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label
            htmlFor="por-ip"
            className="mb-1 block text-sm font-medium"
          >
            Jugadores por conexión al día
          </label>
          <input
            id="por-ip"
            inputMode="numeric"
            value={porIp}
            onChange={(e) => setPorIp(e.target.value)}
            className="w-full rounded-md border border-border bg-surface px-3 py-2"
          />
        </div>

        <div>
          <label htmlFor="horas" className="mb-1 block text-sm font-medium">
            Horas entre votos
          </label>
          <input
            id="horas"
            inputMode="numeric"
            value={horas}
            onChange={(e) => setHoras(e.target.value)}
            className="w-full rounded-md border border-border bg-surface px-3 py-2"
          />
          <p className="mt-1 text-xs text-subtle text-pretty">
            La misma para todos los servidores, y solo se toca desde aquí. Si
            cada dueño pudiera elegir la suya, bajarla le multiplicaría los votos
            sin que sus jugadores hicieran nada distinto.
          </p>
        </div>
      </div>

      {ipEstricto && (
        <Aviso tono="atencion">
          Con este valor tan bajo vas a bloquear a gente legítima. Los operadores
          móviles de Latinoamérica meten miles de clientes detrás de una sola IP
          (CGNAT), y también comparten conexión los hermanos de una casa o los
          equipos de un ciber. El primero que vote deja fuera al resto ese día,
          sin que entiendan por qué. Si empiezan las quejas, sube esto a 5 o 10.
        </Aviso>
      )}

      <div className="flex items-center gap-3">
        <div className="w-36">
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

      <p className="text-xs text-subtle text-pretty">
        Los cambios se aplican al instante, sin desplegar, y quedan registrados
        con quién los hizo.
      </p>
    </div>
  );
}
