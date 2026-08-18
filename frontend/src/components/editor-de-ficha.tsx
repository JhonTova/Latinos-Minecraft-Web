"use client";

import { Aviso, Boton, Campo } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Los seis temas. El código vive aquí y en `globals.css`, y tienen que coincidir:
 * el backend guarda el código tal cual y la web pinta `data-tema` con él, así que
 * un código que no exista en el CSS simplemente hereda el tema del sitio.
 *
 * Ese comportamiento es el correcto: un tema desconocido se ve como el de por
 * defecto, no rompe la ficha.
 */
export const TEMAS = [
  { codigo: "DEFECTO", nombre: "LatinosMC", muestra: "oklch(0.575 0.225 300)" },
  { codigo: "ESMERALDA", nombre: "Esmeralda", muestra: "oklch(0.585 0.138 145)" },
  { codigo: "ROSA", nombre: "Rosa", muestra: "oklch(0.6 0.19 355)" },
  { codigo: "LAVA", nombre: "Lava", muestra: "oklch(0.585 0.2 30)" },
  { codigo: "OCEANO", nombre: "Océano", muestra: "oklch(0.56 0.15 245)" },
  { codigo: "AMATISTA", nombre: "Amatista", muestra: "oklch(0.55 0.18 300)" },
  { codigo: "ORO", nombre: "Oro", muestra: "oklch(0.66 0.145 75)" },
] as const;

export interface Modalidad {
  codigo: string;
  nombre: string;
}

export interface FichaEditable {
  descripcion: string | null;
  versionMinima: string | null;
  versionMaxima: string | null;
  region: string | null;
  tema: string;
  modalidades: string[];
}

/**
 * Edición de la ficha pública de un servidor.
 *
 * Todo lo de aquí lo declara el dueño y **no afecta al ranking**. Es deliberado:
 * en cuanto un dato declarado por el cliente puntúa, deja de describir el
 * servidor y pasa a describir lo que le conviene poner.
 */
export function EditorDeFicha({
  servidorId,
  ficha,
  catalogo,
}: {
  servidorId: string;
  ficha: FichaEditable;
  catalogo: Modalidad[];
}) {
  const router = useRouter();

  const [descripcion, setDescripcion] = useState(ficha.descripcion ?? "");
  const [vMin, setVMin] = useState(ficha.versionMinima ?? "");
  const [vMax, setVMax] = useState(ficha.versionMaxima ?? "");
  const [region, setRegion] = useState(ficha.region ?? "");
  const [tema, setTema] = useState(ficha.tema ?? "DEFECTO");
  const [modalidades, setModalidades] = useState<string[]>(ficha.modalidades);

  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<"ok" | string | null>(null);

  const TOPE_MODALIDADES = 8;

  function alternar(codigo: string) {
    setModalidades((actuales) =>
      actuales.includes(codigo)
        ? actuales.filter((c) => c !== codigo)
        : actuales.length >= TOPE_MODALIDADES
          ? actuales
          : [...actuales, codigo],
    );
  }

  async function guardar() {
    setGuardando(true);
    setResultado(null);
    try {
      const respuesta = await fetch(`/api/backend/servers/${servidorId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          descripcion,
          versionMinima: vMin,
          versionMaxima: vMax,
          region,
          tema,
          modalidades,
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
    <div className="space-y-6">
      <div className="space-y-1.5">
        <label htmlFor="descripcion" className="block text-sm font-medium">
          Descripción
        </label>
        <textarea
          id="descripcion"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          rows={4}
          maxLength={500}
          placeholder="Qué hace especial a tu servidor, qué van a encontrar…"
          className="w-full rounded-md border border-border bg-surface px-3 py-2 text-base outline-none transition-colors placeholder:text-subtle focus:border-primary"
        />
        <p className="text-xs text-subtle">
          {descripcion.length} de 500. Las primeras palabras son las que se ven
          en el listado.
        </p>
      </div>

      <fieldset>
        <legend className="mb-2 text-sm font-medium">
          Modalidades{" "}
          <span className="font-normal text-subtle">
            ({modalidades.length} de {TOPE_MODALIDADES})
          </span>
        </legend>
        <div className="flex flex-wrap gap-2">
          {catalogo.map((m) => {
            const activa = modalidades.includes(m.codigo);
            return (
              <button
                key={m.codigo}
                type="button"
                onClick={() => alternar(m.codigo)}
                aria-pressed={activa}
                disabled={!activa && modalidades.length >= TOPE_MODALIDADES}
                className={`rounded-full border px-3 py-1.5 text-sm transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                  activa
                    ? "border-primary bg-primary text-on-accent"
                    : "border-border bg-surface hover:border-border-strong hover:bg-surface-sunken"
                }`}
              >
                {m.nombre}
              </button>
            );
          })}
        </div>
        <p className="mt-2 text-xs text-subtle text-pretty">
          Marca solo las que de verdad tengas. Marcarlas todas para salir en
          todos los filtros hace que el jugador entre, no encuentre lo que
          buscaba y se vaya.
        </p>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-2">
        <Campo
          id="version-min"
          etiqueta="Versión mínima"
          value={vMin}
          onChange={(e) => setVMin(e.target.value)}
          placeholder="1.8"
          maxLength={12}
        />
        <Campo
          id="version-max"
          etiqueta="Versión máxima"
          value={vMax}
          onChange={(e) => setVMax(e.target.value)}
          placeholder="1.21.11"
          maxLength={12}
        />
      </div>

      <Campo
        id="region"
        etiqueta="Dónde está alojado"
        value={region}
        onChange={(e) => setRegion(e.target.value)}
        placeholder="Brasil · São Paulo"
        maxLength={40}
        ayuda="Ayuda al jugador a saber si le irá bien de ping antes de entrar."
      />

      <fieldset>
        <legend className="mb-2 text-sm font-medium">Tema de tu ficha</legend>
        <div className="flex flex-wrap gap-2">
          {TEMAS.map((t) => (
            <button
              key={t.codigo}
              type="button"
              onClick={() => setTema(t.codigo)}
              aria-pressed={tema === t.codigo}
              className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm transition-colors ${
                tema === t.codigo
                  ? "border-primary bg-primary-subtle"
                  : "border-border bg-surface hover:bg-surface-sunken"
              }`}
            >
              <span
                aria-hidden
                className="size-4 rounded-full ring-1 ring-border"
                style={{ backgroundColor: t.muestra }}
              />
              {t.nombre}
            </button>
          ))}
        </div>
        <p className="mt-2 text-xs text-subtle text-pretty">
          Cambia el color de los títulos y del botón de votar en tu página. Los
          fondos y el texto no se tocan, para que siempre se lea bien.
        </p>
      </fieldset>

      {resultado && resultado !== "ok" && <Aviso tono="error">{resultado}</Aviso>}
      {resultado === "ok" && <Aviso tono="exito">Ficha actualizada.</Aviso>}

      <div className="w-40">
        <Boton onClick={guardar} cargando={guardando}>
          Guardar ficha
        </Boton>
      </div>
    </div>
  );
}
