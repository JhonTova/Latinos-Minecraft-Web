import type { MetadataRoute } from "next";

const SITIO = process.env.SITIO_URL ?? "https://latinosmc.com";

/**
 * `robots.txt`.
 *
 * Lo importante no es lo que se permite, sino lo que se excluye:
 *
 * - `/panel` y `/api` son privados y no aportan nada en un buscador.
 * - `/votar/*` lleva un código de un solo uso. Si Google lo indexara, el enlace
 *   quedaría publicado — y aunque caduca en diez minutos, no hay ningún motivo
 *   para que un código personal aparezca en un resultado de búsqueda.
 *
 * El `sitemap` va aquí porque es donde lo buscan los rastreadores que no lo
 * tienen dado de alta en su panel.
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/panel", "/panel/", "/api/", "/votar/"],
    },
    sitemap: `${SITIO}/sitemap.xml`,
    host: SITIO,
  };
}
