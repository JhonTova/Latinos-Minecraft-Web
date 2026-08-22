"use client";

import { Aviso, Boton, Campo } from "@/components/ui";
import { refrescarSitio } from "@/lib/acciones";
import { useRouter } from "next/navigation";
import { useRef, useState } from "react";

import type { AjustesDelSitioAdmin } from "@/lib/sesion";

/**
 * Textos e imagen que se ven al compartir la portada.
 *
 * Va acompañado de una vista previa porque estos campos no se juzgan leyéndolos
 * sueltos: se juzgan viendo cómo quedan en la tarjeta de Discord. Sin la vista
 * previa, la única forma de saber si un título entra es publicarlo y pegarse el
 * enlace a uno mismo.
 */
export function EditorDeMetadatos({
  ajustes,
  ogImageUrl,
  logoUrl,
}: {
  ajustes: AjustesDelSitioAdmin;
  ogImageUrl: string | null;
  logoUrl: string | null;
}) {
  const router = useRouter();

  const [titulo, setTitulo] = useState(ajustes.titulo);
  const [descripcion, setDescripcion] = useState(ajustes.descripcion);
  const [nombre, setNombre] = useState(ajustes.nombre);
  const [urlPlugin, setUrlPlugin] = useState(ajustes.urlPlugin ?? "");

  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<"ok" | string | null>(null);

  async function guardar() {
    setGuardando(true);
    setResultado(null);
    try {
      const respuesta = await fetch("/api/backend/admin/sitio", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ titulo, descripcion, nombre, urlPlugin }),
      });
      if (!respuesta.ok) {
        const detalle = await respuesta.json().catch(() => null);
        throw new Error(detalle?.detail ?? "No se pudo guardar.");
      }
      setResultado("ok");
      // Primero la caché, luego el re-render. Al revés, el componente se vuelve
      // a pintar con los datos viejos que siguen guardados.
      await refrescarSitio();
      router.refresh();
    } catch (e) {
      setResultado(e instanceof Error ? e.message : "No se pudo guardar.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="space-y-5">
      <VistaPrevia
        titulo={titulo}
        descripcion={descripcion}
        imagen={ogImageUrl}
      />

      <Campo
        id="sitio-titulo"
        etiqueta="Título"
        value={titulo}
        onChange={(e) => setTitulo(e.target.value)}
        ayuda={`${titulo.length} caracteres. Google corta alrededor de 60 — pon lo que la gente busca al principio, no la marca.`}
        error={
          titulo.length < 3 || titulo.length > 70
            ? "Entre 3 y 70 caracteres."
            : undefined
        }
      />

      <div className="space-y-1.5">
        <label htmlFor="sitio-desc" className="block text-sm font-medium">
          Descripción
        </label>
        <textarea
          id="sitio-desc"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          rows={3}
          aria-describedby="sitio-desc-ayuda"
          className="w-full rounded-md border border-border bg-surface px-3 py-2 text-base outline-none transition-colors focus:border-primary"
        />
        <p
          id="sitio-desc-ayuda"
          className={`text-xs ${descripcion.length < 20 || descripcion.length > 300 ? "text-danger" : "text-subtle"}`}
        >
          {descripcion.length} caracteres. Entre 120 y 160 funciona mejor: por
          debajo desaprovechas espacio, por encima Google la corta a mitad de
          frase.
        </p>
      </div>

      <Campo
        id="sitio-nombre"
        etiqueta="Nombre de la marca"
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
        ayuda="Se añade al final de los títulos de las páginas internas."
      />

      <Campo
        id="sitio-plugin"
        etiqueta="Enlace de descarga del plugin"
        value={urlPlugin}
        onChange={(e) => setUrlPlugin(e.target.value)}
        placeholder="https://github.com/tu-usuario/latinosmc-plugin/releases/latest"
        ayuda="Sin esto, el botón de descarga no aparece. Mejor que dejarlo llevando a ningún sitio."
      />

      <SubirImagen
        titulo="Logotipo"
        descripcion="Se ve en la cabecera, en grande en la portada y como icono de la pestaña del navegador. Hasta 512 px; se conserva la proporción, no se recorta."
        actual={logoUrl}
        ruta="/api/backend/admin/sitio/logo"
        vistaPrevia="logo"
      />

      <SubirImagen
        titulo="Imagen al compartir"
        descripcion="1200 × 630 px. Es la proporción que esperan Discord, WhatsApp y Twitter; con otra forma recortan por el centro, que es justo donde suele estar el logo."
        actual={ogImageUrl}
        ruta="/api/backend/admin/sitio/imagen"
        vistaPrevia="social"
      />

      {resultado && resultado !== "ok" && (
        <Aviso tono="error">{resultado}</Aviso>
      )}
      {resultado === "ok" && (
        <Aviso tono="exito">
          Guardado. Discord y WhatsApp guardan la vista previa en caché unas
          horas, así que un enlace ya compartido puede seguir enseñando la
          anterior un rato.
        </Aviso>
      )}

      <div className="w-40">
        <Boton onClick={guardar} cargando={guardando}>
          Guardar
        </Boton>
      </div>
    </div>
  );
}

/**
 * Cómo se verá en Discord.
 *
 * Es una aproximación, no un clon: cada red maqueta la suya. Sirve para lo que
 * importa —ver si el título cabe y si la imagen tiene la proporción correcta—
 * sin tener que publicar para comprobarlo.
 */
function VistaPrevia({
  titulo,
  descripcion,
  imagen,
}: {
  titulo: string;
  descripcion: string;
  imagen: string | null;
}) {
  return (
    <div>
      <p className="mb-2 text-xs text-subtle">Así se verá al compartirlo</p>
      <div className="max-w-md overflow-hidden rounded-lg border-l-4 border-primary bg-surface-sunken">
        {imagen && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={imagen}
            alt=""
            className="aspect-[1200/630] w-full object-cover"
          />
        )}
        <div className="p-3">
          <p className="line-clamp-2 font-medium text-info">
            {titulo || "Sin título"}
          </p>
          <p className="mt-1 line-clamp-3 text-sm text-muted text-pretty">
            {descripcion || "Sin descripción"}
          </p>
        </div>
      </div>
    </div>
  );
}

/**
 * Subida de una imagen del sitio.
 *
 * Sirve para el logotipo y para la imagen social: son la misma interacción con
 * distinto destino, y tener dos componentes casi iguales garantiza que uno de
 * los dos se quede sin un arreglo tarde o temprano.
 */
function SubirImagen({
  titulo,
  descripcion,
  actual,
  ruta,
  vistaPrevia,
}: {
  titulo: string;
  descripcion: string;
  actual: string | null;
  ruta: string;
  vistaPrevia: "logo" | "social";
}) {
  const router = useRouter();
  const entrada = useRef<HTMLInputElement>(null);
  const [subiendo, setSubiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function subir(archivo: File) {
    setSubiendo(true);
    setError(null);
    try {
      const cuerpo = new FormData();
      cuerpo.append("archivo", archivo);

      const respuesta = await fetch(ruta, { method: "POST", body: cuerpo });
      if (!respuesta.ok) {
        // 413 lo devuelve el proxy de delante, no la aplicación, así que no
        // trae explicación en JSON. Sin este caso, un límite del servidor web
        // se ve como un "no se pudo subir" que no apunta a ninguna parte —y
        // eso costó dos rondas de diagnóstico.
        if (respuesta.status === 413) {
          throw new Error(
            "La imagen pesa demasiado para el servidor. Prueba con una más ligera o avisa al administrador.",
          );
        }
        const detalle = await respuesta.json().catch(() => null);
        throw new Error(detalle?.detail ?? "No se pudo subir la imagen.");
      }

      await refrescarSitio();
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo subir la imagen.");
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <div>
      <p className="mb-1 text-sm font-medium">{titulo}</p>
      <p className="mb-3 text-xs text-subtle text-pretty">{descripcion}</p>

      <div className="flex flex-wrap items-center gap-4">
        {actual ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={actual}
            alt=""
            className={
              vistaPrevia === "logo"
                ? "size-16 rounded-lg object-contain ring-1 ring-border"
                : "aspect-[1200/630] w-40 rounded-lg object-cover ring-1 ring-border"
            }
          />
        ) : (
          <span
            aria-hidden
            className={`grid place-items-center rounded-lg border border-dashed border-border text-xs text-subtle ${
              vistaPrevia === "logo" ? "size-16" : "aspect-[1200/630] w-40"
            }`}
          >
            Sin imagen
          </span>
        )}

        <input
          ref={entrada}
          type="file"
          accept="image/png,image/jpeg,image/webp"
          className="hidden"
          onChange={(e) => {
            const archivo = e.target.files?.[0];
            if (archivo) subir(archivo);
          }}
        />

        <button
          type="button"
          onClick={() => entrada.current?.click()}
          disabled={subiendo}
          className="boton-vivo rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium hover:bg-surface-sunken disabled:opacity-60"
        >
          {subiendo ? "Subiendo…" : actual ? "Cambiar" : "Subir"}
        </button>
      </div>

      {error && <p className="mt-2 text-xs text-danger">{error}</p>}
    </div>
  );
}
