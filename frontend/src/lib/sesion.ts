import type { ImagenesServidor } from "@/components/imagenes-del-servidor";
import type { ServidorConImagenAutomatica } from "@/app/panel/admin/imagenes-automaticas";
import type { ServidorAdministrable } from "@/app/panel/admin/moderacion-de-servidores";
import { cookies } from "next/headers";

const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

export interface PerfilActual {
  id: string;
  email: string;
  nombre: string;
  emailVerificado: boolean;
}

export interface OrganizacionPropia {
  id: string;
  nombre: string;
  plan: string;
  rol: string;
  servidoresUsados: number;
  /** null = sin límite (plan Plus). */
  limiteServidores: number | null;
}

export interface ServidorPropio {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  estado: string;
  organizacion: string;
  plan: string;
}

/**
 * Usuario de la sesión actual, leído en el servidor.
 *
 * Se resuelve aquí y no en el navegador porque así el panel nunca llega a
 * pintarse para alguien sin sesión: no hay un parpadeo en el que se vea contenido
 * privado antes de la redirección.
 *
 * Devuelve `null` en lugar de lanzar: no tener sesión es un estado normal, no un
 * error.
 */
export async function perfilActual(): Promise<PerfilActual | null> {
  const cookie = (await cookies()).toString();
  if (!cookie) return null;

  const respuesta = await fetch(`${API}/auth/yo`, {
    headers: { Cookie: cookie },
    cache: "no-store",
  });

  if (!respuesta.ok) return null;
  return respuesta.json();
}

export async function misOrganizaciones(): Promise<OrganizacionPropia[]> {
  return traer("/servers/mis-organizaciones");
}

export async function misServidores(): Promise<ServidorPropio[]> {
  return traer("/servers/mios");
}

export interface FichaServidor {
  id: string;
  slug: string;
  nombre: string;
  descripcion: string | null;
  estado: string;
  host: string;
  puerto: number;
  aceptaOffline: boolean;
  verificado: boolean;
  iconoUrl: string | null;
  bannerUrl: string | null;
  versionMinima: string | null;
  versionMaxima: string | null;
  region: string | null;
  tema: string;
  /** Códigos, no nombres: es lo que espera el formulario. */
  modalidades: string[];
  /** Si acepta jugadores de móvil y consola, vía Geyser (ADR-0030). */
  soportaBedrock: boolean;
  /** De dónde salen el icono y el banner, y qué hay de cada uno (ADR-0027). */
  imagenes: ImagenesServidor;
  /**
   * Por qué está suspendido, si lo está (ADR-0028).
   *
   * Su dueño tiene que poder leerlo. Un servidor que desaparece del directorio
   * sin explicación es un ticket de soporte seguro.
   */
  motivoDeSuspension: string | null;
}

export interface Modalidad {
  codigo: string;
  nombre: string;
}

/**
 * Ficha de un servidor propio.
 *
 * Devuelve `null` si no existe o no es del usuario — la API responde lo mismo en
 * ambos casos a propósito, para que nadie pueda averiguar qué servidores hay
 * probando identificadores.
 */
export async function fichaServidor(id: string): Promise<FichaServidor | null> {
  const cookie = (await cookies()).toString();
  const respuesta = await fetch(`${API}/servers/${encodeURIComponent(id)}`, {
    headers: { Cookie: cookie },
    cache: "no-store",
  });

  if (respuesta.status === 404) return null;
  if (!respuesta.ok) {
    throw new Error(`No se pudo cargar el servidor (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

/** Catálogo de modalidades. Es una lista fija, así que se cachea. */
export async function catalogoDeModalidades(): Promise<Modalidad[]> {
  const respuesta = await fetch(`${API}/servers/modalidades`, {
    next: { revalidate: 3600 },
  });
  if (!respuesta.ok) return [];
  return respuesta.json();
}

export interface Pedido {
  id: string;
  organizacionId: string;
  producto: string;
  /**
   * Código de la tabla `order_statuses`, en inglés y en mayúsculas: PENDING,
   * PAID, CANCELLED, REFUNDED o FAILED. Se traduce al pintarlo, nunca al
   * compararlo.
   */
  estado: string;
  importeCentimos: number;
  moneda: string;
  pasarela: string | null;
  referencia: string | null;
}

export async function misPedidos(): Promise<Pedido[]> {
  return traer("/pagos/pedidos");
}

async function traer<T>(ruta: string): Promise<T> {
  const cookie = (await cookies()).toString();
  const respuesta = await fetch(`${API}${ruta}`, {
    headers: { Cookie: cookie },
    cache: "no-store",
  });
  if (!respuesta.ok) {
    throw new Error(`No se pudo cargar ${ruta} (HTTP ${respuesta.status})`);
  }
  return respuesta.json();
}

export interface SlotConfigurable {
  posicion: number;
  precioCentimos: number;
  moneda: string;
  diasPeriodo: number;
  maxAdelanto: number;
  activo: boolean;
}

export interface Ingresos {
  moneda: string;
  /** Neto: los reembolsos son asientos negativos y restan. */
  totalCentimos: number;
  asientos: number;
}

/**
 * Si el usuario de la sesión administra la plataforma.
 *
 * Sirve solo para decidir si se pinta el enlace de administración. No concede
 * nada: cada endpoint del backend vuelve a comprobarlo por su cuenta, así que
 * falsear esta respuesta desde el navegador no abre ninguna puerta.
 */
export async function soyAdministrador(): Promise<boolean> {
  const cookie = (await cookies()).toString();
  if (!cookie) return false;

  const respuesta = await fetch(`${API}/admin/soy`, {
    headers: { Cookie: cookie },
    cache: "no-store",
  });

  if (!respuesta.ok) return false;
  const datos: { administrador: boolean } = await respuesta.json();
  return datos.administrador;
}

export async function slotsConfigurables(): Promise<SlotConfigurable[]> {
  return traer("/admin/slots");
}

export async function todosLosPedidos(): Promise<Pedido[]> {
  return traer("/admin/pedidos");
}

export async function ingresos(): Promise<Ingresos[]> {
  return traer("/admin/ingresos");
}

export interface PatrocinadorAdmin {
  id: string;
  nombre: string;
  urlDestino: string;
  claveLogo: string | null;
  /** Banner vertical de los laterales de la portada. */
  claveBanner: string | null;
  desde: string;
  hasta: string;
  orden: number;
  /** null mientras no se haya aprobado. Sin esto no se publica. */
  aprobadoEn: string | null;
  canceladoEn: string | null;
}

/**
 * Devuelve tambien el instante de la lectura. Los estados que se pintan
 * (visible, programado, caducado) son fechas comparadas con "ahora", y ese
 * "ahora" tiene que quedar fijado aqui: leerlo mientras se pinta convierte el
 * renderizado en impuro y el resultado cambia solo al re-renderizar.
 */
export async function patrocinadoresAdmin(): Promise<{
  marcas: PatrocinadorAdmin[];
  leidoEn: number;
}> {
  const marcas: PatrocinadorAdmin[] = await traer("/admin/patrocinadores");
  return { marcas, leidoEn: Date.now() };
}

/**
 * Servidores cuyas imágenes hemos cogido de su ping, para revisarlas (ADR-0027).
 *
 * Solo devuelve los que tienen algo adoptado o están retirados: son los únicos
 * sobre los que se puede actuar, y una pantalla de moderación que no se recorre
 * de un vistazo no se usa.
 */
export async function imagenesAutomaticasAdmin(): Promise<
  ServidorConImagenAutomatica[]
> {
  return traer("/admin/servidores/imagenes-automaticas");
}

/**
 * Todos los servidores vivos, para moderar (ADR-0028).
 *
 * Los suspendidos vienen primero: son los que necesitan una decisión.
 */
export async function servidoresAdmin(): Promise<ServidorAdministrable[]> {
  return traer("/admin/servidores");
}

export interface ReglasDeVoto {
  unVotoAlDia: boolean;
  maxJugadoresPorIp: number;
  /** Segundos. Jackson serializa las duraciones de Java así. */
  ventana: number;
}

export async function reglasDeVoto(): Promise<ReglasDeVoto> {
  return traer("/admin/voto");
}

export interface AjustesDelSitioAdmin {
  titulo: string;
  descripcion: string;
  claveOgImage: string | null;
  nombre: string;
  claveLogo: string | null;
  /** null oculta el botón de descarga en vez de dejarlo roto. */
  urlPlugin: string | null;
}

export async function ajustesDelSitioAdmin(): Promise<AjustesDelSitioAdmin> {
  return traer("/admin/sitio");
}
