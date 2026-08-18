import type { EstadisticasPlataforma } from "@/lib/api";
import Link from "next/link";

const formatoNumero = new Intl.NumberFormat("es-ES");

/**
 * Franja de cifras.
 *
 * Todas se miden desde nuestra infraestructura (ADR-0009), nunca con lo que
 * reporte cada plugin. Es lo que separa un contador de un anuncio: si cada
 * servidor declarara sus jugadores, el total sería la suma de lo que a cada uno
 * le conviene decir.
 *
 * **No se pinta con la plataforma vacía.** Cuatro ceros enormes en la portada
 * dicen «esto no lo usa nadie» mucho más alto de lo que dicen «somos
 * transparentes», y esa es la primera impresión de quien llega el primer día.
 */
export function FranjaDeCifras({
  cifras,
}: {
  cifras: EstadisticasPlataforma | null;
}) {
  if (!cifras || cifras.servidores === 0) {
    return null;
  }

  return (
    <section className="rounded-xl border border-border bg-surface-sunken/60 px-4 py-5 sm:px-6">
      <dl className="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-4">
        <Cifra
          valor={formatoNumero.format(cifras.servidores)}
          etiqueta="Servidores registrados"
          icono={
            <path
              d="M4 6h16v4H4V6Zm0 8h16v4H4v-4Zm3-4h.01M7 18h.01"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          }
        />
        <Cifra
          valor={formatoNumero.format(cifras.votos24h)}
          etiqueta="Votos en 24 horas"
          icono={
            <path
              d="M7 11v9H4v-9h3Zm3 9V11l4-7a2 2 0 0 1 2 3l-1 4h5a2 2 0 0 1 2 2.4l-1.4 6A2 2 0 0 1 18.6 21H10Z"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinejoin="round"
            />
          }
        />
        <Cifra
          valor={formatoNumero.format(cifras.jugadores)}
          etiqueta="Jugando ahora mismo"
          destacada
          icono={
            <path
              d="M16 19v-2a4 4 0 0 0-8 0v2M12 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          }
        />
        <Cifra
          valor={`${formatoNumero.format(cifras.enLinea)}`}
          etiqueta="Servidores en línea"
          icono={
            <path
              d="M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Zm-9-9h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            />
          }
        />
      </dl>
    </section>
  );
}

function Cifra({
  valor,
  etiqueta,
  icono,
  destacada,
}: {
  valor: string;
  etiqueta: string;
  icono: React.ReactNode;
  destacada?: boolean;
}) {
  return (
    <div className="flex items-center gap-3">
      <span
        aria-hidden
        className={`grid size-10 shrink-0 place-items-center rounded-lg ${
          destacada ? "bg-success/15 text-success" : "bg-primary-subtle text-primary"
        }`}
      >
        <svg viewBox="0 0 24 24" className="size-5">
          {icono}
        </svg>
      </span>
      <div className="min-w-0">
        <dd className="cifras text-xl leading-tight font-bold">{valor}</dd>
        <dt className="truncate text-xs text-muted">{etiqueta}</dt>
      </div>
    </div>
  );
}

/**
 * Llamada final para dueños de servidor.
 *
 * Va al fondo de la portada, después de que el visitante haya visto la lista.
 * Antes de eso no significa nada: quien todavía no ha visto qué es esto no tiene
 * motivo para registrar nada.
 */
export function LlamadaParaDuenos() {
  return (
    <section className="overflow-hidden rounded-xl border border-primary/25 bg-primary-subtle/40">
      <div className="flex flex-wrap items-center gap-5 px-5 py-6 sm:px-7">
        <span
          aria-hidden
          className="grid size-12 shrink-0 place-items-center rounded-xl bg-primary/15 text-primary"
        >
          <svg viewBox="0 0 24 24" className="size-6">
            <path
              d="M8 21h8m-4-4v4m-5-17h10v4a5 5 0 0 1-10 0V4Zm10 1h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>

        <div className="min-w-0 flex-1">
          <h2 className="display text-lg font-semibold">
            ¿Tienes un servidor?
          </h2>
          <p className="mt-0.5 text-sm text-muted text-pretty">
            Añádelo gratis, instala el plugin y tus jugadores empezarán a votar
            y a llevarse recompensas dentro del juego.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Link
            href="/registro"
            className="boton-vivo rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
          >
            Registrar servidor
          </Link>
          <Link
            href="/plugin"
            className="boton-vivo rounded-lg border border-border bg-surface px-5 py-2.5 text-sm font-medium hover:bg-surface-sunken"
          >
            Ver el plugin
          </Link>
        </div>
      </div>
    </section>
  );
}
