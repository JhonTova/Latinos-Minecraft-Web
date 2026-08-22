import { CopiarIp } from "@/components/copiar-ip";
import {
  EtiquetaBedrock,
  EtiquetaDeCuenta,
} from "@/components/etiquetas-de-cuenta";
import type { ServidorEnRanking } from "@/lib/tipos";
import Link from "next/link";

const formatoNumero = new Intl.NumberFormat("es-ES");

/**
 * Fila de servidor.
 *
 * **El banner es la fila.** Es la lección de los directorios que llevan años
 * funcionando: lo que llena visualmente la página no es el diseño del sitio, son
 * las imágenes que suben los servidores. Un listado de tarjetas con tipografía
 * bonita y sin banners se ve vacío por muchas capas de degradado que le eches.
 *
 * El reparto sigue el orden de las preguntas del jugador: quién es, a qué se
 * juega, cuánta gente hay, y cómo entro.
 */
export function FilaServidor({
  posicion,
  servidor,
  destacado = false,
}: {
  posicion: number;
  servidor: ServidorEnRanking;
  destacado?: boolean;
}) {
  const enLinea =
    servidor.estado !== "OFFLINE" && servidor.jugadoresConectados !== null;

  return (
    /* `@container`: la fila se adapta a SU PROPIO ancho, no al de la pantalla.
       Es lo que arregla el ranking partido en dos (ADR-0030). Con `sm:` y demás,
       la fila miraba el ancho del navegador: en una pantalla de 1440 creía tener
       sitio de sobra y se colocaba en horizontal dentro de una columna de 540,
       donde las etiquetas acababan encima de las cifras.

       El mismo componente sirve ahora para la columna estrecha del top doble y
       para el listado a lo ancho, sin una variante aparte que mantener. */
    <li
      className={`group relative @container overflow-hidden rounded-lg border bg-surface transition-colors ${
        destacado
          ? "border-primary/40 shadow-sm"
          : "border-border hover:border-border-strong"
      }`}
    >
      {destacado && (
        <span className="absolute top-0 right-0 z-10 rounded-bl-lg bg-primary px-2 py-0.5 text-[10px] font-semibold tracking-wide text-on-accent uppercase">
          Patrocinado
        </span>
      )}

      <div className="flex flex-col gap-3 p-3 @lg:flex-row @lg:items-center @lg:gap-4">
        {/* Puesto, icono y nombre van juntos en una sola línea cuando la fila es
            estrecha. Sin este envoltorio, el número y el icono se quedaban solos
            ocupando una línea entera en móvil y empujaban todo hacia abajo.

            `@lg:contents` hace que el envoltorio desaparezca del layout cuando
            hay sitio, así que a lo ancho la fila queda exactamente como estaba:
            un solo componente para los dos casos, sin duplicar nada. */}
        <div className="flex min-w-0 items-center gap-3 @lg:contents">
        {/* --- Puesto e icono ------------------------------------------- */}
        <div className="flex shrink-0 items-center gap-3">
          <span
            className={`cifras w-8 text-center font-mono ${
              posicion <= 3 && !destacado
                ? "text-xl font-bold text-primary"
                : "text-base text-subtle"
            }`}
          >
            {destacado ? "★" : posicion}
          </span>

          {servidor.iconoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={servidor.iconoUrl}
              alt=""
              width={48}
              height={48}
              className="size-12 shrink-0 rounded-md object-cover ring-1 ring-border"
            />
          ) : (
            <span
              aria-hidden
              className="grid size-12 shrink-0 place-items-center rounded-md bg-primary-subtle text-lg font-bold text-primary"
            >
              {servidor.nombre.trim().charAt(0).toUpperCase()}
            </span>
          )}
        </div>

        {/* --- Identidad ------------------------------------------------ */}
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <h3 className="truncate font-semibold">
              <Link
                href={`/servidores/${servidor.slug}`}
                className="transition-colors after:absolute after:inset-0 group-hover:text-primary"
              >
                {servidor.nombre}
              </Link>
            </h3>

            {!enLinea && (
              <span className="rounded bg-surface-sunken px-1.5 py-0.5 text-[10px] font-medium text-subtle">
                Sin conexión
              </span>
            )}
          </div>

          <p className="mt-0.5 line-clamp-1 text-sm text-muted">
            {servidor.descripcion}
          </p>

          <Etiquetas servidor={servidor} />
        </div>
        </div>

        {/* --- Banner ---------------------------------------------------- */}
        {/* El banner necesita 320 px suyos. Solo aparece cuando la fila tiene
            sitio de verdad: en la columna estrecha del top doble se esconde y
            el nombre se queda con el espacio, que es lo que importa ahí.

            El umbral es @4xl (896 px) y no @3xl (768). Con 768 la fila entraba
            justo pero, descontados el banner, las cifras y el botón, al nombre y
            la descripción les quedaban unos 120 px: el texto se partía letra a
            letra y parecía que el banner estuviera encima. Cabía por aritmética
            y no se leía, que es la única medida que cuenta. */}
        {servidor.bannerUrl && (
          <div className="hidden shrink-0 overflow-hidden rounded-md ring-1 ring-border @4xl:block">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={servidor.bannerUrl}
              alt=""
              width={320}
              height={60}
              className="h-[60px] w-[320px] object-cover"
            />
          </div>
        )}

        {/* --- Cifras y acción ------------------------------------------ */}
        <div className="flex shrink-0 items-center gap-4 @lg:gap-5">
          <div className="w-16 text-center">
            <div
              className={`cifras text-lg leading-tight font-bold ${enLinea ? "" : "text-subtle"}`}
            >
              {enLinea ? formatoNumero.format(servidor.jugadoresConectados!) : "—"}
            </div>
            <div className="text-[11px] text-subtle">
              {enLinea && servidor.jugadoresMaximo
                ? `/ ${formatoNumero.format(servidor.jugadoresMaximo)}`
                : "jugadores"}
            </div>
          </div>

          <div className="w-16 text-center">
            <div className="cifras text-lg leading-tight font-bold text-primary">
              {formatoNumero.format(servidor.votosSemana)}
            </div>
            <div className="text-[11px] text-subtle">votos</div>
          </div>

          {/* Copiar la IP al lado de las cifras solo cuando cabe sin apretar.
              Si no, baja a su propia línea abajo, que es donde de verdad se ve. */}
          <div className="relative z-10 hidden @2xl:block">
            <CopiarIp direccion={servidor.direccion} compacto />
          </div>
        </div>
      </div>

      {/* Cuando la fila es estrecha, la IP va abajo a lo ancho: es lo que viene
          a buscar el visitante y no puede quedarse escondida por falta de sitio.
          Vale igual para el móvil que para la columna del top doble. */}
      <div className="relative z-10 border-t border-border px-3 py-2 @2xl:hidden">
        <CopiarIp direccion={servidor.direccion} compacto />
      </div>
    </li>
  );
}

/**
 * Modalidades y versiones.
 *
 * Se cortan a tres. Un servidor que marca las dieciséis no está describiéndose,
 * está intentando salir en todos los filtros — y una fila con dieciséis
 * etiquetas deja de leerse.
 */
function Etiquetas({ servidor }: { servidor: ServidorEnRanking }) {
  const versiones =
    servidor.versionMinima && servidor.versionMaxima
      ? `${servidor.versionMinima} – ${servidor.versionMaxima}`
      : (servidor.versionMinima ?? servidor.versionMaxima);

  return (
    <div className="mt-1.5 flex flex-wrap items-center gap-1">
      {/* El tipo de cuenta va PRIMERO, antes que la modalidad y la versión.
          No es jerarquía visual por gusto: si el jugador no tiene cuenta
          original, un servidor premium no es que le convenga menos, es que no
          puede entrar. Es el dato que más le ahorra un clic perdido. */}
      <EtiquetaDeCuenta aceptaNoPremium={servidor.aceptaNoPremium} tamano="pequena" />

      {servidor.soportaBedrock && <EtiquetaBedrock tamano="pequena" />}

      {servidor.modalidades.slice(0, 3).map((modalidad) => (
        <span
          key={modalidad}
          className="rounded bg-surface-sunken px-1.5 py-0.5 text-[11px] text-muted"
        >
          {modalidad}
        </span>
      ))}

      {servidor.modalidades.length > 3 && (
        <span className="text-[11px] text-subtle">
          +{servidor.modalidades.length - 3}
        </span>
      )}

      {versiones && (
        <span className="cifras rounded bg-primary-subtle px-1.5 py-0.5 font-mono text-[11px] font-medium text-primary">
          {versiones}
        </span>
      )}

      {servidor.region && (
        <span className="text-[11px] text-subtle">· {servidor.region}</span>
      )}
    </div>
  );
}

/* -------------------------------------------------------------------------- */

export function FilasCargando({ cuantas = 8 }: { cuantas?: number }) {
  return (
    <ul
      aria-busy="true"
      aria-label="Cargando servidores"
      className="space-y-2"
    >
      {Array.from({ length: cuantas }).map((_, i) => (
        <li
          key={i}
          className="flex items-center gap-4 rounded-lg border border-border bg-surface p-3"
        >
          <div className="cargando size-12 shrink-0 rounded-md" />
          <div className="flex-1 space-y-2">
            <div className="cargando h-4 w-1/4 rounded" />
            <div className="cargando h-3 w-2/5 rounded" />
          </div>
          <div className="cargando hidden h-[60px] w-[320px] rounded-md lg:block" />
          <div className="cargando h-8 w-16 rounded" />
        </li>
      ))}
    </ul>
  );
}

export function SinServidores() {
  return (
    <div className="rounded-lg border border-dashed border-border py-16 text-center">
      <h2 className="font-semibold">Todavía no hay servidores</h2>
      <p className="mx-auto mt-1.5 max-w-sm text-sm text-muted text-pretty">
        Sé el primero en aparecer. Registrar tu servidor lleva dos minutos y no
        cuesta nada.
      </p>
      <Link
        href="/registro"
        className="boton-vivo mt-5 inline-block rounded-md bg-primary px-5 py-2.5 text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover"
      >
        Registrar mi servidor
      </Link>
    </div>
  );
}
