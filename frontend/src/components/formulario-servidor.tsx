"use client";

import { Aviso, Boton, Campo, mensajeDeError } from "@/components/ui";
import Link from "next/link";
import { useState } from "react";

interface ServidorCreado {
  servidorId: string;
  slug: string;
  claveApi: string;
}

export function FormularioServidor() {
  const [creado, setCreado] = useState<ServidorCreado | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setCargando(true);

    const datos = new FormData(evento.currentTarget);
    const respuesta = await fetch("/api/backend/servers", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        slug: datos.get("slug"),
        nombre: datos.get("nombre"),
        descripcion: datos.get("descripcion"),
        host: datos.get("host"),
        puerto: Number(datos.get("puerto")),
        aceptaOffline: datos.get("aceptaOffline") === "on",
      }),
    });

    if (!respuesta.ok) {
      setError(await mensajeDeError(respuesta));
      setCargando(false);
      return;
    }

    setCreado(await respuesta.json());
    setCargando(false);
  }

  if (creado) {
    return <ClaveGenerada servidor={creado} />;
  }

  return (
    <form onSubmit={enviar} className="space-y-4">
      {error && <Aviso tono="error">{error}</Aviso>}

      <Campo
        etiqueta="Nombre del servidor"
        name="nombre"
        required
        maxLength={120}
        placeholder="CraftLatino"
      />

      <Campo
        etiqueta="Identificador en la URL"
        name="slug"
        required
        maxLength={64}
        pattern="[a-z0-9]+(-[a-z0-9]+)*"
        placeholder="craftlatino"
        ayuda="Solo minúsculas, números y guiones. Aparecerá como /servidores/craftlatino"
      />

      <Campo
        etiqueta="Descripción"
        name="descripcion"
        maxLength={500}
        placeholder="Survival · Skyblock · Sin pay to win"
        ayuda="Lo primero que lee un jugador. Sé concreto."
      />

      <div className="grid grid-cols-3 gap-3">
        <div className="col-span-2">
          <Campo
            etiqueta="Dirección (IP o dominio)"
            name="host"
            required
            maxLength={255}
            placeholder="mc.craftlatino.com"
            ayuda="La usamos para comprobar si tu servidor está en línea."
          />
        </div>
        <Campo
          etiqueta="Puerto"
          name="puerto"
          type="number"
          required
          min={1}
          max={65535}
          defaultValue={25565}
        />
      </div>

      <label className="flex cursor-pointer items-start gap-3 rounded-md border border-border p-3">
        <input
          type="checkbox"
          name="aceptaOffline"
          className="mt-0.5 size-4 accent-[var(--primary)]"
        />
        <span className="text-sm">
          <span className="font-medium">Mi servidor es offline-mode</span>
          <span className="mt-0.5 block text-muted text-pretty">
            Acepta votos de jugadores sin cuenta premium. Esos votos puntúan más
            bajo en el antifraude, porque su identidad no la respalda nadie.
          </span>
        </span>
      </label>

      <Boton type="submit" cargando={cargando}>
        Crear servidor y generar clave
      </Boton>
    </form>
  );
}

/**
 * La clave se muestra UNA vez. En la base de datos solo vive su hash, así que ni
 * nosotros podemos recuperarla: si se pierde, hay que generar otra.
 *
 * Por eso esta pantalla no se parece al resto: tiene que quedar claro que este
 * momento no se repite.
 */
function ClaveGenerada({ servidor }: { servidor: ServidorCreado }) {
  const [copiada, setCopiada] = useState(false);

  async function copiar() {
    await navigator.clipboard.writeText(servidor.claveApi);
    setCopiada(true);
    setTimeout(() => setCopiada(false), 2000);
  }

  return (
    <div className="space-y-5">
      <Aviso tono="exito">
        Servidor <strong>{servidor.slug}</strong> creado.
      </Aviso>

      <div>
        <p className="mb-2 text-sm font-medium">
          Tu clave del plugin — cópiala ahora
        </p>
        <div className="flex gap-2">
          <code className="min-w-0 flex-1 truncate rounded-md border border-border bg-surface-sunken px-3 py-2.5 font-mono text-sm">
            {servidor.claveApi}
          </code>
          <button
            type="button"
            onClick={copiar}
            className="shrink-0 rounded-md bg-primary px-4 py-2.5 text-sm font-medium text-on-accent transition-colors hover:bg-primary-hover"
          >
            {copiada ? "¡Copiada!" : "Copiar"}
          </button>
        </div>
      </div>

      <Aviso tono="atencion">
        <strong>No se volverá a mostrar.</strong> Guardamos solo una huella suya,
        así que ni nosotros podemos recuperarla. Si la pierdes, tendrás que
        generar una nueva.
      </Aviso>

      <div className="rounded-lg border border-border bg-surface-sunken p-4">
        <p className="mb-3 text-sm font-medium">Ya solo quedan 3 pasos</p>
        <ol className="space-y-2.5 text-sm text-muted">
          <li className="flex gap-2.5">
            <span className="font-mono text-subtle">1.</span>
            <span>
              Descarga el plugin y ponlo en la carpeta{" "}
              <code className="rounded bg-surface px-1 py-0.5 font-mono text-xs">
                plugins/
              </code>{" "}
              de tu servidor.
            </span>
          </li>
          <li className="flex gap-2.5">
            <span className="font-mono text-subtle">2.</span>
            <span>
              Arranca el servidor una vez y pega la clave en{" "}
              <code className="rounded bg-surface px-1 py-0.5 font-mono text-xs">
                plugins/LatinosMC/config.yml
              </code>
            </span>
          </li>
          <li className="flex gap-2.5">
            <span className="font-mono text-subtle">3.</span>
            <span>
              Reinicia. En cuanto el plugin se conecte, tu servidor pasa a activo
              automáticamente — esa conexión es la prueba de que es tuyo.
            </span>
          </li>
        </ol>
      </div>

      <Link
        href="/panel"
        className="block w-full rounded-md border border-border bg-surface px-4 py-2.5 text-center font-medium transition-colors hover:bg-surface-sunken"
      >
        Ya la he guardado, volver al panel
      </Link>
    </div>
  );
}
