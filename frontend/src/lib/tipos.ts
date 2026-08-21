/** Tipos del dominio tal y como los consume la interfaz. */

export type EstadoServidor = "ACTIVE" | "OFFLINE";

export interface ServidorEnRanking {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string;
  version: string;
  /** Medido por NOSOTROS con ping directo, nunca reportado por el plugin (ADR-0009). */
  jugadoresConectados: number | null;
  estado: EstadoServidor;
  votosSemana: number;
  /** null si el dueño no ha subido ninguno. */
  iconoUrl: string | null;
  /** Lo que el jugador copia para conectarse. */
  direccion: string;
  versionMinima: string | null;
  versionMaxima: string | null;
  /** Dónde está alojado, declarado por el dueño. Sirve para estimar el ping. */
  region: string | null;
  modalidades: string[];
  /** Aforo del servidor, medido por nosotros con el ping. */
  jugadoresMaximo: number | null;
  bannerUrl: string | null;
  /** Votos por dia de la ultima semana, para el minigrafico. */
  tendencia: number[];
  /**
   * Si admite cuentas no premium (ADR-0030).
   *
   * Decide en cuál de las dos clasificaciones sale. Para el jugador es el dato
   * más útil después de la dirección: sin cuenta original, un servidor premium
   * no es que le convenga menos — es que no puede entrar.
   */
  aceptaNoPremium: boolean;
  /** Si se puede jugar desde móvil o consola. Lo declara el dueño. */
  soportaBedrock: boolean;
}

/** Lo que devuelve un enlace de voto válido (ADR-0016). */
export interface DatosParaVotar {
  servidorSlug: string;
  servidorNombre: string;
  /** Nick ya resuelto desde el token. Se muestra, nunca se pregunta. */
  jugador: string;
}
