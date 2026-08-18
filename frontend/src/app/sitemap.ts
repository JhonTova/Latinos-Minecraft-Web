import { catalogoDeModalidades, obtenerRanking } from "@/lib/api";
import type { MetadataRoute } from "next";

const SITIO = process.env.SITIO_URL ?? "https://latinosmc.com";

/**
 * Mapa del sitio.
 *
 * Sin él, Google descubre las fichas de servidor solo si llega a ellas siguiendo
 * enlaces desde la portada — y la portada enseña cincuenta, así que el servidor
 * número ochenta podría no indexarse nunca. Con el mapa, todos entran de golpe.
 *
 * `prioridad` es una pista, no una orden: Google la usa para decidir el orden de
 * rastreo entre páginas del MISMO sitio, no para posicionar mejor. Por eso la
 * portada y el listado van por delante de las fichas.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const fijas: MetadataRoute.Sitemap = [
    { url: SITIO, changeFrequency: "hourly", priority: 1 },
    { url: `${SITIO}/servidores`, changeFrequency: "hourly", priority: 0.9 },
    { url: `${SITIO}/destacar`, changeFrequency: "monthly", priority: 0.5 },
    { url: `${SITIO}/registro`, changeFrequency: "monthly", priority: 0.4 },
  ];

  // Si la API no responde se devuelve solo lo fijo. Un sitemap incompleto es
  // mucho mejor que un 500: ante un error, algunos rastreadores dejan de pedirlo
  // durante días.
  const servidores = await obtenerRanking().catch(() => []);

  // Las páginas por categoría. Van con prioridad alta porque son las que pueden
  // posicionar por lo que la gente busca de verdad —«servidores de skyblock en
  // español»— y hay pocas, así que no diluyen nada.
  const categorias = await catalogoDeModalidades().catch(() => []);

  return [
    ...fijas,
    ...categorias.map((m) => ({
      url: `${SITIO}/servidores/modalidad/${m.codigo.toLowerCase()}`,
      changeFrequency: "daily" as const,
      priority: 0.8,
    })),
    ...servidores.map((servidor) => ({
      url: `${SITIO}/servidores/${servidor.slug}`,
      changeFrequency: "daily" as const,
      priority: 0.7,
    })),
  ];
}
