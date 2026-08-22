import type { DatosParaVotar, ServidorEnRanking } from "./tipos";

const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

export interface FichaPublica {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  /** Lo que el jugador copia para conectarse. */
  direccion: string;
  aceptaOffline: boolean;
  ventanaHoras: number;
  jugadoresConectados: number | null;
  enLinea: boolean;
  votosSemana: number;
  iconoUrl: string | null;
  bannerUrl: string | null;
  versionMinima: string | null;
  versionMaxima: string | null;
  /** Dónde está alojado, declarado por el dueño. Sirve para estimar el ping. */
  region: string | null;
  /** Uno de los seis temas. Uno desconocido hereda el del sitio. */
  tema: string;
  modalidades: string[];
  /** Aforo, medido por nosotros con el ping. */
  jugadoresMaximo: number | null;
  tendencia: number[];
  /**
   * El MOTD que anuncia el servidor, leído de su ping y saneado (ADR-0027).
   *
   * En el formato de `MotdJson`; lo pinta el componente `Motd`. Va además del
   * banner porque un lector de pantalla no puede leer un PNG y un buscador
   * tampoco.
   */
  motd: string | null;
  /** Si se puede jugar desde móvil o consola. Lo declara el dueño (ADR-0030). */
  soportaBedrock: boolean;
}

/** Ficha pública de un servidor. Devuelve `null` si no existe o no está activo. */
export async function fichaPublica(slug: string): Promise<FichaPublica | null> {
  const respuesta = await fetch(
    `${API}/directorio/${encodeURIComponent(slug)}`,
    { next: { revalidate: 60 } },
  );

  if (respuesta.status === 404) return null;
  if (!respuesta.ok) {
    throw new Error(`No se pudo cargar el servidor (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

/**
 * Resuelve un enlace de voto (ADR-0016).
 *
 * El nick viene dentro del token: la web nunca lo pregunta. Si lo preguntara,
 * cualquiera podría votar en nombre de cualquiera sin haber pisado el servidor.
 *
 * Devuelve `null` si el enlace caducó o ya se usó, para que la página muestre un
 * mensaje útil en lugar de un error.
 */
export async function resolverEnlaceDeVoto(
  token: string,
): Promise<DatosParaVotar | null> {
  const respuesta = await fetch(
    `${API}/vote-tokens/${encodeURIComponent(token)}`,
    { cache: "no-store" },
  );

  if (respuesta.status === 404 || respuesta.status === 409) {
    return null;
  }
  if (!respuesta.ok) {
    throw new Error(`No se pudo comprobar el enlace (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

/** Respuesta cruda del backend. Se traduce a los tipos de la interfaz abajo. */
interface RankingDto {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  iconoUrl: string | null;
  bannerUrl: string | null;
  direccion: string;
  versionMinima: string | null;
  versionMaxima: string | null;
  region: string | null;
  modalidades: string[];
  votos: number;
  jugadoresConectados: number | null;
  jugadoresMaximo: number | null;
  enLinea: boolean;
  tendencia: number[];
  aceptaNoPremium: boolean;
  soportaBedrock: boolean;
}

/**
 * Ranking orgánico.
 *
 * En esta versión de Next.js `fetch` no cachea por defecto, así que se pide el
 * caché de forma explícita. 60 segundos es suficiente: el ranking no necesita
 * ser instantáneo y es la consulta más pedida del sitio.
 */
export async function obtenerRanking(): Promise<ServidorEnRanking[]> {
  return pedirRanking();
}

/**
 * Las dos clasificaciones separadas de la portada (ADR-0030).
 *
 * `premium` son los que **solo** admiten cuentas originales; `mixto`, los que
 * admiten también cuentas no premium.
 *
 * Se piden en paralelo: son dos consultas independientes y encadenarlas
 * duplicaría el tiempo de la portada sin ganar nada.
 */
export async function obtenerTopsSeparados(): Promise<{
  premium: ServidorEnRanking[];
  mixto: ServidorEnRanking[];
}> {
  const [premium, mixto] = await Promise.all([
    pedirRanking(undefined, { cuentas: "premium", limite: 15 }),
    pedirRanking(undefined, { cuentas: "todas", limite: 15 }),
  ]);
  return { premium, mixto };
}

/**
 * El ranking de una modalidad concreta. Lo usan las páginas por categoría.
 *
 * El filtro va a la API y no se aplica sobre el ranking completo: quedarse con
 * los de una modalidad entre los 50 mejores del total dejaría casi vacía a
 * cualquier categoría cuyos servidores no estén arriba del general.
 */
export async function obtenerRankingDe(
  modalidad: string,
): Promise<ServidorEnRanking[]> {
  return pedirRanking(modalidad);
}

async function pedirRanking(
  modalidad?: string,
  opciones?: { cuentas?: "premium" | "todas"; bedrock?: boolean; limite?: number },
): Promise<ServidorEnRanking[]> {
  const parametros = new URLSearchParams({
    dias: "7",
    limite: String(opciones?.limite ?? 50),
  });
  if (modalidad) parametros.set("modalidad", modalidad);
  if (opciones?.cuentas) parametros.set("cuentas", opciones.cuentas);
  if (opciones?.bedrock) parametros.set("bedrock", "true");

  const respuesta = await fetch(`${API}/ranking?${parametros}`, {
    next: { revalidate: 60 },
  });

  if (!respuesta.ok) {
    throw new Error(`El ranking no está disponible (HTTP ${respuesta.status})`);
  }

  const datos: RankingDto[] = await respuesta.json();

  return datos.map((d) => ({
    id: d.id,
    slug: d.slug,
    nombre: d.nombre,
    descripcion: d.descripcion ?? "",
    // La versión que MIDE el ping se sigue sin exponer; lo que hay aquí es el
    // rango que declara el dueño, que es otra cosa y va en sus propios campos.
    version: "",
    iconoUrl: d.iconoUrl,
    bannerUrl: d.bannerUrl,
    direccion: d.direccion,
    versionMinima: d.versionMinima,
    versionMaxima: d.versionMaxima,
    region: d.region,
    modalidades: d.modalidades ?? [],
    jugadoresMaximo: d.jugadoresMaximo,
    jugadoresConectados: d.enLinea ? d.jugadoresConectados : null,
    estado: d.enLinea ? "ACTIVE" : "OFFLINE",
    votosSemana: d.votos,
    tendencia: d.tendencia,
    aceptaNoPremium: d.aceptaNoPremium,
    soportaBedrock: d.soportaBedrock,
  }));
}

export interface SlotDestacado {
  posicion: number;
  precioCentimos: number;
  moneda: string;
  diasPeriodo: number;
  disponible: boolean;
  /** Cuándo empezaría el periodo que compres ahora mismo. */
  proximoDesde: string;
  proximoHasta: string;
  /** Cuántos periodos hay ya vendidos por delante. */
  periodosEnCola: number;
}

/**
 * Precios y disponibilidad de los cinco espacios destacados.
 *
 * Sin caché: el precio y sobre todo las fechas cambian en cuanto alguien compra.
 * Enseñar una fecha vieja aquí significa prometer una semana que ya no existe.
 */
export async function slotsDestacados(): Promise<SlotDestacado[]> {
  const respuesta = await fetch(`${API}/pagos/slots`, { cache: "no-store" });

  if (!respuesta.ok) {
    throw new Error(`No se pudieron cargar los espacios (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

/** Formatea céntimos enteros como precio. Nunca se pasa por coma flotante. */
export function precio(centimos: number, moneda: string): string {
  return (centimos / 100).toLocaleString("es-ES", {
    style: "currency",
    currency: moneda || "EUR",
    minimumFractionDigits: centimos % 100 === 0 ? 0 : 2,
  });
}

export interface ServidorDestacado {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  iconoUrl: string | null;
  bannerUrl: string | null;
  direccion: string;
  versionMinima: string | null;
  versionMaxima: string | null;
  region: string | null;
  modalidades: string[];
}

/** `servidor` es null cuando el espacio está libre. */
export interface SlotEnPortada {
  posicion: number;
  servidor: ServidorDestacado | null;
}

/**
 * Los cinco espacios de la portada.
 *
 * Endpoint aparte del ranking a propósito (ADR-0013): lo patrocinado no se
 * mezcla con lo orgánico. Que vengan de sitios distintos hace que mezclarlos
 * requiera un esfuerzo deliberado en vez de ser lo que sale por defecto.
 */
export async function destacadosDePortada(): Promise<SlotEnPortada[]> {
  const respuesta = await fetch(`${API}/destacados`, {
    next: { revalidate: 60 },
  });

  if (!respuesta.ok) {
    throw new Error(`No se pudieron cargar los destacados (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

export interface Patrocinador {
  id: string;
  nombre: string;
  urlDestino: string;
  logoUrl: string | null;
  /** Banner vertical de los laterales. null: se pinta el logotipo y el nombre. */
  bannerUrl: string | null;
}

/**
 * Patrocinadores de marca: hostings, VPS y similares.
 *
 * Solo devuelve los aprobados y vigentes; el filtro está en el backend, no aquí.
 * Caché de cinco minutos porque aparecen en todas las páginas y cambian como
 * mucho una vez a la semana.
 */
export async function patrocinadores(): Promise<Patrocinador[]> {
  const respuesta = await fetch(`${API}/patrocinadores`, {
    next: { revalidate: 300, tags: ["patrocinadores"] },
  });

  if (!respuesta.ok) {
    throw new Error(`No se pudieron cargar los patrocinadores (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

export interface AjustesDelSitio {
  titulo: string;
  descripcion: string;
  nombre: string;
  ogImageUrl: string | null;
  logoUrl: string | null;
  /** null oculta el botón de descarga en vez de dejarlo roto. */
  urlPlugin: string | null;
}

/**
 * Textos e imagen social de la portada, configurados desde administración.
 *
 * Nunca lanza. Se pide al construir los `<meta>` de cada página y una excepción
 * aquí dejaría la web entera sin renderizar: un fallo al leer un título no puede
 * tumbar el sitio. Si la API no responde, se usan los valores de reserva.
 */
export async function ajustesDelSitio(): Promise<AjustesDelSitio> {
  try {
    // La etiqueta permite invalidar esta caché desde el panel: sin ella, subir
    // un logotipo no se vería hasta cinco minutos después y parecería que la
    // subida no ha funcionado.
    const respuesta = await fetch(`${API}/sitio`, {
      next: { revalidate: 300, tags: ["sitio"] },
    });
    if (!respuesta.ok) return SITIO_POR_DEFECTO;
    return await respuesta.json();
  } catch {
    return SITIO_POR_DEFECTO;
  }
}

/**
 * Valores de reserva. Existen sobre todo por el `build` de Docker, que se
 * ejecuta sin backend: sin ellos, la portada se compilaría sin título ni
 * descripción y Google podría indexarla así.
 */
const SITIO_POR_DEFECTO: AjustesDelSitio = {
  titulo: "Servidores de Minecraft en español — Ranking de servidores latinos",
  descripcion:
    "Los mejores servidores de Minecraft en español y latinos. Ranking por votos verificados.",
  nombre: "LatinosMC",
  ogImageUrl: null,
  logoUrl: null,
  urlPlugin: null,
};

export interface EstadisticasPlataforma {
  /** Todos los activos, respondan o no: un servidor caído sigue existiendo. */
  servidores: number;
  enLinea: number;
  /** Suma de conectados, medida por nosotros con ping directo (ADR-0009). */
  jugadores: number;
  /** Solo los ACEPTADOS: contar los rechazados sería presumir de tráfico falso. */
  votos24h: number;
}

/**
 * Cifras de la plataforma.
 *
 * Nunca lanza: aparecen en la cabecera de todas las páginas y un fallo aquí
 * dejaría el sitio entero sin renderizar por un contador.
 */
export async function estadisticas(): Promise<EstadisticasPlataforma | null> {
  try {
    const respuesta = await fetch(`${API}/estadisticas`, {
      next: { revalidate: 60 },
    });
    if (!respuesta.ok) return null;
    return await respuesta.json();
  } catch {
    return null;
  }
}

export interface Modalidad {
  codigo: string;
  nombre: string;
}

/** Catálogo de modalidades. Lista fija, así que se cachea una hora. */
export async function catalogoDeModalidades(): Promise<Modalidad[]> {
  const respuesta = await fetch(`${API}/servers/modalidades`, {
    next: { revalidate: 3600 },
  });
  if (!respuesta.ok) return [];
  return respuesta.json();
}
