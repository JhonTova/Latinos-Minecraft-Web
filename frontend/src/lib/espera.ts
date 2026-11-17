/**
 * Convierte los segundos que faltan para volver a votar en un texto legible.
 *
 * Devuelve `null` si el dato no es un número finito: el llamador decide no
 * mostrar nada en vez de inventar un tiempo.
 *
 * Todo valor positivo se redondea hacia arriba al minuto: es peor decirle al
 * jugador que vuelva antes de tiempo (y que reciba otra vez el rechazo) que un
 * minuto después.
 */
export function formatearEspera(segundos: unknown): string | null {
  if (typeof segundos !== "number" || !Number.isFinite(segundos)) return null;
  if (segundos <= 0) return "menos de un minuto";

  const totalMinutos = Math.ceil(segundos / 60);
  const horas = Math.floor(totalMinutos / 60);
  const minutos = totalMinutos % 60;

  if (horas > 0 && minutos > 0) return `${horas} h ${minutos} min`;
  if (horas > 0) return `${horas} h`;
  return `${minutos} min`;
}
