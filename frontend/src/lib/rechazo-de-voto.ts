import { formatearEspera } from "./espera";

export type RechazoDeVoto =
  | { tipo: "voto-diario"; espera: string | null }
  | { tipo: "ventana-servidor"; espera: string | null }
  | { tipo: "otro"; mensaje: string };

const RESPALDO = "No se pudo registrar tu voto. Inténtalo otra vez.";

/**
 * Decide qué clase de rechazo devolvió el backend al votar.
 *
 * Se compara `title` (no `type`) como hasta ahora. El cuerpo no es de fiar,
 * así que cualquier forma inesperada cae en «otro» con el texto de respaldo.
 */
export function clasificarRechazo(problema: unknown): RechazoDeVoto {
  if (typeof problema !== "object" || problema === null || Array.isArray(problema)) {
    return { tipo: "otro", mensaje: RESPALDO };
  }

  const cuerpo = problema as Record<string, unknown>;

  if (cuerpo.title === "vote.daily.used") {
    return { tipo: "voto-diario", espera: formatearEspera(cuerpo.segundosRestantes) };
  }
  if (cuerpo.title === "vote.window.open") {
    return { tipo: "ventana-servidor", espera: formatearEspera(cuerpo.segundosRestantes) };
  }

  const detalle = cuerpo.detail;
  return {
    tipo: "otro",
    mensaje: typeof detalle === "string" && detalle !== "" ? detalle : RESPALDO,
  };
}
