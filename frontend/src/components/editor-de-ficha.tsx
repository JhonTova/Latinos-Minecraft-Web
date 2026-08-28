"use client";

import { Aviso, Boton, Campo } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

/**
 * Los siete temas. El código vive aquí y en `globals.css`, y tienen que
 * coincidir: el backend guarda el código tal cual y la web pinta `data-tema`
 * con él, así que un código que no exista en el CSS simplemente hereda el tema
 * del sitio.
 *
 * Ese comportamiento es el correcto: un tema desconocido se ve como el de por
 * defecto, no rompe la ficha.
 *
 * **Aquí ya no se repite el color.** Antes cada entrada llevaba su valor OKLCH
 * copiado del CSS para pintar el puntito de la píldora, y eran siete valores
 * duplicados que había que acordarse de cambiar en dos sitios — y que además
 * mentían en tema oscuro, donde el acento real es más claro. Ahora la muestra se
 * marca con `data-tema` y lee el color del propio CSS.
 */
export const TEMAS = [
  { codigo: "DEFECTO", nombre: "LatinosMC" },
  { codigo: "ESMERALDA", nombre: "Esmeralda" },
  { codigo: "ROSA", nombre: "Rosa" },
  { codigo: "LAVA", nombre: "Lava" },
  { codigo: "OCEANO", nombre: "Océano" },
  { codigo: "AMATISTA", nombre: "Amatista" },
  { codigo: "ORO", nombre: "Oro" },
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
  /** Si acepta jugadores de móvil y consola, normalmente vía Geyser (ADR-0030). */
  soportaBedrock: boolean;
  /** IP de Geyser. null mientras no se haya puesto. */
  hostBedrock: string | null;
  /** Puerto de Geyser. Bedrock lo pide aparte de la IP. */
  puertoBedrock: number | null;
}

/** El de Geyser recién instalado. Es lo que tiene la inmensa mayoría. */
const PUERTO_BEDROCK_POR_DEFECTO = "19132";

/**
 * Edición de la ficha pública de un servidor.
 *
 * Todo lo de aquí lo declara el dueño y **no afecta al ranking**. Es deliberado:
 * en cuanto un dato declarado por el cliente puntúa, deja de describir el
 * servidor y pasa a describir lo que le conviene poner.
 */
export function EditorDeFicha({
  servidorId,
  nombre,
  ficha,
  catalogo,
}: {
  servidorId: string;
  /** Solo para la vista previa del color: enseñarla con el nombre real es lo
      que hace que se lea como «mi ficha» y no como un ejemplo genérico. */
  nombre: string;
  ficha: FichaEditable;
  catalogo: Modalidad[];
}) {
  const router = useRouter();

  const [descripcion, setDescripcion] = useState(ficha.descripcion ?? "");
  const [vMin, setVMin] = useState(ficha.versionMinima ?? "");
  const [vMax, setVMax] = useState(ficha.versionMaxima ?? "");
  const [region, setRegion] = useState(ficha.region ?? "");
  const [tema, setTema] = useState(ficha.tema ?? "DEFECTO");
  const [bedrock, setBedrock] = useState(ficha.soportaBedrock ?? false);
  const [hostBedrock, setHostBedrock] = useState(ficha.hostBedrock ?? "");
  // El puerto arranca ya relleno con el de Geyser por defecto. Casi nadie lo
  // cambia, y un campo vacío obliga a ir a buscar un dato que ya sabemos.
  const [puertoBedrock, setPuertoBedrock] = useState(
    ficha.puertoBedrock ? String(ficha.puertoBedrock) : PUERTO_BEDROCK_POR_DEFECTO,
  );
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

    // El puerto se valida aquí además de en el backend. No es la protección
    // —esa está en el servidor— sino la diferencia entre corregir un dedazo al
    // instante y esperar a que vuelva un 400 sin saber qué campo era.
    const puerto = Number.parseInt(puertoBedrock, 10);
    if (bedrock && hostBedrock.trim() && !(puerto >= 1 && puerto <= 65535)) {
      setGuardando(false);
      setResultado("El puerto de Bedrock tiene que estar entre 1 y 65535.");
      return;
    }

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
          soportaBedrock: bedrock,
          // Sin IP no se manda puerto: el backend guarda los dos o ninguno, y
          // un puerto suelto no dice nada.
          hostBedrock: bedrock ? hostBedrock.trim() : "",
          puertoBedrock:
            bedrock && hostBedrock.trim() ? puerto : null,
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

      {/* Bedrock (ADR-0030).
          Una casilla y no un tipo de servidor aparte: casi ningún servidor
          latino es Bedrock nativo, lo que hay son servidores Java con Geyser.
          La pregunta que se hace el jugador es «¿puedo entrar desde el móvil?»,
          y eso es una propiedad de este servidor, no otro juego. */}
      <div className="rounded-lg border border-border">
        <label className="flex cursor-pointer items-start gap-3 p-4">
          <input
            type="checkbox"
            checked={bedrock}
            onChange={(e) => setBedrock(e.target.checked)}
            className="mt-0.5 size-4 shrink-0 accent-[var(--primary)]"
          />
          <span className="text-sm">
            <span className="font-medium">
              Se puede jugar desde móvil o consola
            </span>
            <span className="mt-0.5 block text-muted text-pretty">
              Márcalo si tienes Geyser o similar y los jugadores de Bedrock
              pueden entrar. Aparecerás en el filtro «Bedrock Edition», que es de
              los más usados. No lo marques si no funciona: el jugador lo
              descubre al intentar entrar y se va.
            </span>
          </span>
        </label>

        {/* La dirección de Geyser solo aparece con la casilla marcada.
            Es lo que faltaba: sin estos dos campos, el jugador de móvil veía la
            etiqueta «Bedrock Edition» en la ficha, copiaba la dirección de Java
            —otro puerto y muchas veces otro dominio— y no podía entrar.

            Van dentro del mismo bloque y no sueltos más abajo porque son parte
            de la misma decisión: si dices que se puede entrar desde el móvil,
            aquí mismo se dice por dónde. */}
        {bedrock && (
          <div className="entra border-t border-border bg-surface-sunken/50 p-4">
            <div className="grid gap-4 sm:grid-cols-[1fr_9rem]">
              <Campo
                id="bedrock-host"
                etiqueta="IP de Geyser"
                value={hostBedrock}
                onChange={(e) => setHostBedrock(e.target.value)}
                placeholder="bedrock.tuservidor.com"
                maxLength={255}
                autoComplete="off"
                spellCheck={false}
              />
              <Campo
                id="bedrock-puerto"
                etiqueta="Puerto"
                value={puertoBedrock}
                onChange={(e) =>
                  setPuertoBedrock(e.target.value.replace(/\D/g, "").slice(0, 5))
                }
                // `inputMode` y no `type="number"`: el numérico trae flechas de
                // incremento que aquí no sirven para nada y, en móvil, un teclado
                // con coma decimal. Esto da el teclado de números y ya.
                inputMode="numeric"
                placeholder={PUERTO_BEDROCK_POR_DEFECTO}
                autoComplete="off"
              />
            </div>

            <p className="mt-2 text-xs text-subtle text-pretty">
              Bedrock pide la dirección y el puerto en dos casillas separadas, así
              que en tu ficha se enseñan por separado. Suele ser el{" "}
              <span className="font-mono">{PUERTO_BEDROCK_POR_DEFECTO}</span>{" "}
              salvo que lo hayas cambiado en el <span className="font-mono">config.yml</span>{" "}
              de Geyser. Si dejas la IP vacía se enseña solo la etiqueta, sin
              dirección.
            </p>
          </div>
        )}
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

      {/* Selector de color.
          Antes eran siete píldoras con un punto de color: se parecían más a un
          filtro que a una elección de identidad, y como el tema apenas se notaba
          en la ficha, elegir uno parecía no hacer nada.

          Ahora cada opción enseña su propia muestra grande y debajo hay una
          vista previa de la ficha de verdad. La muestra no lleva el color
          escrito a mano: la tarjeta se marca con `data-tema`, así que hereda
          exactamente los mismos valores que usará la página —los del tema claro
          o los del oscuro, según el que tenga puesto quien mira. */}
      <fieldset>
        <legend className="text-sm font-medium">Color de tu ficha</legend>
        <p className="mt-0.5 mb-3 text-xs text-subtle text-pretty">
          Tiñe la portada, las tarjetas de datos y el botón de votar de tu
          página. Los fondos y el texto siguen siendo los del sitio, así que se
          lee bien con cualquiera de los siete.
        </p>

        <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
          {TEMAS.map((t) => {
            const activo = tema === t.codigo;
            return (
              <button
                key={t.codigo}
                type="button"
                data-tema={t.codigo}
                onClick={() => setTema(t.codigo)}
                aria-pressed={activo}
                className={`boton-vivo overflow-hidden rounded-lg border text-left ${
                  activo
                    ? "border-primary ring-2 ring-primary/40"
                    : "border-border hover:border-border-strong"
                }`}
              >
                <span
                  aria-hidden
                  className="block h-11 w-full"
                  style={{
                    background:
                      "linear-gradient(135deg, var(--primary), color-mix(in oklch, var(--primary) 30%, var(--surface)))",
                  }}
                />
                <span className="superficie-tema flex items-center justify-between gap-1 px-2.5 py-2">
                  <span className="truncate text-xs font-medium">
                    {t.nombre}
                  </span>
                  <span
                    aria-hidden
                    className={`shrink-0 text-xs text-primary ${activo ? "" : "invisible"}`}
                  >
                    ✓
                  </span>
                </span>
              </button>
            );
          })}
        </div>

        <VistaPreviaDelTema tema={tema} nombre={nombre} />
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

/**
 * Vista previa de la ficha con el color elegido.
 *
 * **Es la mitad del cambio, no un adorno.** El problema del selector anterior no
 * era que las píldoras fueran feas: era que elegir un color no enseñaba
 * consecuencia ninguna. Hay que guardar, salir del panel, abrir la ficha
 * pública y volver para saber si acertaste — y a la tercera vuelta nadie
 * vuelve.
 *
 * Reproduce las piezas reales de la ficha (portada con trama, icono con su
 * anillo, dirección y botón de votar) con las mismas clases, así que lo que se
 * ve aquí es lo que se verá allí. Si algún día una de esas piezas deja de
 * teñirse, esta previa deja de mentir sola.
 */
function VistaPreviaDelTema({
  tema,
  nombre,
}: {
  tema: string;
  nombre: string;
}) {
  const inicial = (nombre.trim().charAt(0) || "S").toUpperCase();

  return (
    <div className="mt-4">
      <p className="mb-2 text-xs font-medium text-subtle">
        Así se verá tu ficha
      </p>

      <div
        data-tema={tema}
        className="tema-ficha borde-tema superficie-tema overflow-hidden rounded-lg border"
      >
        <div
          className="h-14 w-full"
          style={{
            background:
              "linear-gradient(to bottom, color-mix(in oklch, var(--primary) 22%, transparent), transparent)",
          }}
        >
          <div className="trama-bloques size-full" aria-hidden />
        </div>

        <div className="-mt-6 flex items-end gap-3 px-4">
          <span
            aria-hidden
            className="grid size-12 shrink-0 place-items-center rounded-lg border-2 border-bg bg-primary-subtle font-bold text-primary ring-2 ring-primary/35"
          >
            {inicial}
          </span>
          <span className="min-w-0 flex-1 truncate pb-1 text-sm font-semibold">
            {nombre}
          </span>
        </div>

        <div className="px-4 pt-3 pb-4">
          <div className="borde-tema flex items-center justify-between gap-2 rounded-md border bg-surface px-3 py-2">
            <span className="truncate font-mono text-xs">
              play.tuservidor.com
            </span>
            <span className="shrink-0 text-[11px] text-muted">Copiar</span>
          </div>

          <div className="mt-2 rounded-md bg-primary px-3 py-2 text-center text-xs font-semibold text-on-accent">
            Votar por {nombre}
          </div>
        </div>
      </div>
    </div>
  );
}
