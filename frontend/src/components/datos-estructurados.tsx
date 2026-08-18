/**
 * Datos estructurados (JSON-LD, schema.org).
 *
 * Es la forma de decirle a Google qué *es* una página, no solo qué palabras
 * tiene. Con esto puede enseñar el buscador interno del sitio directamente en
 * los resultados, y entiende que las fichas de servidor son elementos de una
 * lista y no páginas sueltas repetidas.
 *
 * No posiciona por sí mismo. Lo que hace es que el resultado ocupe más espacio y
 * se entienda mejor, y eso sí cambia cuánta gente pulsa.
 *
 * **Sobre `dangerouslySetInnerHTML`:** aquí es la forma correcta y no un atajo.
 * Un `<script type="application/ld+json">` tiene que llevar JSON crudo; si React
 * lo escapara como texto, Google leería `&quot;` en vez de comillas y lo
 * descartaría. El contenido lo generamos nosotros con `JSON.stringify`, así que
 * no hay entrada de terceros sin escapar — salvo los nombres de servidor, que
 * `stringify` ya escapa.
 */
export function DatosEstructurados({ datos }: { datos: object }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(datos) }}
    />
  );
}

const SITIO = process.env.SITIO_URL ?? "https://latinosmc.com";

/** El sitio en sí, con su buscador. */
export function esquemaDelSitio(nombre: string, descripcion: string) {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: nombre,
    description: descripcion,
    url: SITIO,
    inLanguage: "es",
    potentialAction: {
      "@type": "SearchAction",
      target: {
        "@type": "EntryPoint",
        urlTemplate: `${SITIO}/servidores?q={search_term_string}`,
      },
      "query-input": "required name=search_term_string",
    },
  };
}

/**
 * El ranking como lista ordenada.
 *
 * Sin esto, Google ve cincuenta enlaces sueltos. Con esto entiende que es un
 * ranking y que el orden significa algo.
 */
export function esquemaDeRanking(
  servidores: { slug: string; nombre: string }[],
) {
  return {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: "Ranking de servidores de Minecraft en español",
    numberOfItems: servidores.length,
    itemListElement: servidores.slice(0, 25).map((servidor, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: servidor.nombre,
      url: `${SITIO}/servidores/${servidor.slug}`,
    })),
  };
}

/**
 * Una ficha de servidor.
 *
 * Se declara como `Product` en lugar de `WebPage` porque es lo que permite
 * enseñar la valoración —los votos— junto al resultado. `Game` sería más exacto
 * semánticamente, pero Google no le saca ningún partido visual.
 */
export function esquemaDeServidor(servidor: {
  slug: string;
  nombre: string;
  descripcion: string | null;
  direccion: string;
  iconoUrl: string | null;
  bannerUrl: string | null;
  votosSemana: number;
}) {
  return {
    "@context": "https://schema.org",
    "@type": "Product",
    name: servidor.nombre,
    description: servidor.descripcion ?? `Servidor de Minecraft ${servidor.nombre}`,
    url: `${SITIO}/servidores/${servidor.slug}`,
    image: [servidor.bannerUrl, servidor.iconoUrl].filter(Boolean),
    category: "Servidor de Minecraft",
    // Los votos son interacciones reales y verificadas, así que se declaran como
    // tales. NO se inventa una valoración de estrellas: marcar como "reseñas"
    // algo que no lo es está expresamente prohibido por Google y se penaliza.
    interactionStatistic: {
      "@type": "InteractionCounter",
      interactionType: "https://schema.org/VoteAction",
      userInteractionCount: servidor.votosSemana,
    },
  };
}
