import Link from "next/link";

/**
 * Encabezado común de los bloques de la portada.
 *
 * Existe para que todos se vean iguales. Cuando cada sección se maqueta por su
 * cuenta acaban con tamaños y márgenes ligeramente distintos, y esa
 * irregularidad es lo que hace que una página parezca improvisada aunque nadie
 * sepa señalar el motivo.
 *
 * **Vive aquí y no dentro de `page.tsx` por un motivo técnico, no de orden.** Un
 * archivo de página de Next solo puede exportar el componente por defecto y unas
 * pocas cosas concretas —`metadata`, `generateMetadata`, `revalidate`…—. Exportar
 * cualquier otra función de un `page.tsx` rompe la ruta entera en tiempo de
 * ejecución, y el error que sale no menciona el export por ningún lado.
 */
export function EncabezadoDeBloque({
  id,
  titulo,
  etiqueta,
  nota,
  enlace,
}: {
  id: string;
  titulo: string;
  etiqueta?: string;
  nota?: string;
  enlace?: { href: string; texto: string };
}) {
  return (
    <div className="mb-3 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 border-b border-border pb-2">
      <h2
        id={id}
        className="display flex items-center gap-2 text-lg font-semibold"
      >
        {titulo}
        {etiqueta && (
          // La etiqueta es texto real, no solo un color: quien no distingue
          // colores también tiene que poder saber que esto es publicidad.
          <span className="rounded border border-border px-1.5 py-0.5 text-[10px] font-normal tracking-wide text-subtle uppercase">
            {etiqueta}
          </span>
        )}
      </h2>

      {nota && <span className="text-xs text-subtle">{nota}</span>}

      {enlace && (
        <Link href={enlace.href} className="enlace-vivo text-sm text-primary">
          {enlace.texto}
        </Link>
      )}
    </div>
  );
}
