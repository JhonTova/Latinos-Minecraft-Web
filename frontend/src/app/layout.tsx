import { ajustesDelSitio } from "@/lib/api";
import type { Metadata } from "next";
import { Geist, Geist_Mono, Sora } from "next/font/google";
import "./globals.css";

const sans = Geist({
  variable: "--font-sans",
  subsets: ["latin"],
});

const mono = Geist_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
});

/**
 * Fuente de titulares.
 *
 * Sora tiene más carácter que la de texto sin caer en lo decorativo: sus formas
 * son geométricas y algo anchas, que es lo que hace que un título en versalitas
 * se lea como una marca y no como una etiqueta de menú.
 *
 * Solo se cargan tres pesos. Cada peso es un archivo que el visitante descarga
 * antes de ver nada, y con siete pesos "por si acaso" el sitio tarda medio
 * segundo más en pintar para usar dos.
 */
const display = Sora({
  variable: "--font-display",
  subsets: ["latin"],
  weight: ["500", "600", "700"],
});

/**
 * Dominio público.
 *
 * Sin esto, las URLs relativas de los metadatos salen rotas al compartir: un
 * `/servidores/x` se resuelve contra el servidor de quien lee el enlace, no
 * contra el nuestro.
 *
 * La variable NO lleva el prefijo `NEXT_PUBLIC_` a propósito. Next congela esas
 * al compilar, y la imagen de Docker se construye antes de saber en qué dominio
 * va a correr. Sin prefijo se lee en tiempo de ejecución, que es cuando el
 * dominio ya se conoce.
 */
const SITIO = new URL(process.env.SITIO_URL ?? "https://latinosmc.com");

/**
 * Metadatos del sitio, leídos de la base de datos.
 *
 * **Por qué esto se genera en el servidor.** Los rastreadores de Discord,
 * WhatsApp, Twitter y Facebook *no ejecutan JavaScript*: piden el HTML, leen las
 * etiquetas que ya vienen dentro y se van. Una web que inyecta sus metadatos
 * desde React —con Helmet o similar— se queda sin vista previa, porque el
 * rastreador nunca llega a ver esas etiquetas.
 *
 * Aquí viajan en el HTML inicial, así que las ven.
 */
export async function generateMetadata(): Promise<Metadata> {
  const sitio = await ajustesDelSitio();

  return {
    metadataBase: SITIO,

    title: {
      default: sitio.titulo,
      // Las páginas internas ponen su nombre y heredan la marca. En Google
      // compite el principio del título, así que lo específico va primero.
      template: `%s · ${sitio.nombre}`,
    },
    description: sitio.descripcion,

    applicationName: sitio.nombre,
    // Google ignora la etiqueta keywords desde 2009. Se declara igual porque
    // algunos buscadores menores la siguen leyendo y no cuesta nada; lo que de
    // verdad posiciona son el título, la descripción y el contenido.
    keywords: [
      "servidores de minecraft",
      "servidores minecraft español",
      "servidores minecraft latinos",
      "servidores minecraft hispanos",
      "minecraft latino",
      "lista de servidores minecraft",
      "votar servidor minecraft",
      "servidores survival",
      "servidores skyblock",
      "minecraft bedrock español",
    ],

    alternates: { canonical: "/" },

    // El favicon sale del logotipo que se sube en administración. Si no hay
    // ninguno se usa el archivo del proyecto, así que la pestaña nunca queda con
    // el icono genérico del navegador.
    icons: sitio.logoUrl
      ? { icon: sitio.logoUrl, apple: sitio.logoUrl }
      : undefined,

    openGraph: {
      type: "website",
      locale: "es_ES",
      url: SITIO.toString(),
      siteName: sitio.nombre,
      title: sitio.titulo,
      description: sitio.descripcion,
      images: sitio.ogImageUrl
        ? [
            {
              url: sitio.ogImageUrl,
              width: 1200,
              height: 630,
              alt: sitio.nombre,
            },
          ]
        : undefined,
    },

    twitter: {
      // Sin imagen, `summary_large_image` deja una tarjeta con un hueco vacío.
      card: sitio.ogImageUrl ? "summary_large_image" : "summary",
      title: sitio.titulo,
      description: sitio.descripcion,
      images: sitio.ogImageUrl ? [sitio.ogImageUrl] : undefined,
    },

    robots: {
      index: true,
      follow: true,
      googleBot: {
        index: true,
        follow: true,
        // Sin esto, Google recorta las vistas previas de texto e imagen por su
        // cuenta y el resultado en el buscador queda más pobre.
        "max-image-preview": "large",
        "max-snippet": -1,
      },
    },
  };
}

/**
 * Evita el parpadeo blanco al cargar en tema oscuro.
 *
 * El tema se resuelve en CSS, pero si el usuario ha elegido uno explicitamente
 * hay que aplicarlo ANTES del primer pintado. Este script es sincrono y va en
 * <head> a proposito: cualquier alternativa (efecto de React, cookie leida en
 * servidor) llega tarde y el usuario ve un destello blanco.
 */
const scriptTema = `
(function () {
  try {
    var t = localStorage.getItem("tema");
    if (t === "dark" || t === "light") {
      document.documentElement.setAttribute("data-theme", t);
    }
  } catch (e) {}
})();
`;

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="es"
      className={`${sans.variable} ${mono.variable} ${display.variable} h-full`}
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: scriptTema }} />
      </head>
      <body className="flex min-h-full flex-col font-sans">{children}</body>
    </html>
  );
}
