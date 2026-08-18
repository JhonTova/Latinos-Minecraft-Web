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
}

/** Lo que devuelve un enlace de voto válido (ADR-0016). */
export interface DatosParaVotar {
  servidorSlug: string;
  servidorNombre: string;
  /** Nick ya resuelto desde el token. Se muestra, nunca se pregunta. */
  jugador: string;
}
