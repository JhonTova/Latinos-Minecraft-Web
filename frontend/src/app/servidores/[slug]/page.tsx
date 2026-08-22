import { Cabecera, PieDePagina } from "@/components/cabecera";
import {
  DatosEstructurados,
  esquemaDeServidor,
} from "@/components/datos-estructurados";
import { CopiarIp } from "@/components/copiar-ip";
import {
  EtiquetaBedrock,
  EtiquetaDeCuenta,
} from "@/components/etiquetas-de-cuenta";
import { Motd, textoPlanoDelMotd } from "@/components/motd";
import { Sparkline } from "@/components/sparkline";
import { PanelDeVoto } from "@/components/panel-de-voto";
import { fichaPublica } from "@/lib/api";
import type { Metadata } from "next";
import { notFound } from "next/navigation";

const formatoNumero = new Intl.NumberFormat("es-ES");

export async function generateMetadata(
  props: PageProps<"/servidores/[slug]">,
): Promise<Metadata> {
  const { slug } = await props.params;
  const servidor = await fichaPublica(slug);

  if (!servidor) {
    // `noindex` a propósito: sin esto, un slug mal escrito compartido por ahí
    // acabaría indexado como una página de error del sitio.
    return { title: "Servidor no encontrado", robots: { index: false } };
  }

  // El banner manda porque es apaisado y llena la tarjeta; el icono es cuadrado
  // y Discord lo recorta. Con icono se usa la tarjeta pequeña, que lo enseña
  // entero al lado del texto en vez de estirarlo.
  const imagen = servidor.bannerUrl ?? servidor.iconoUrl;
  const esBanner = Boolean(servidor.bannerUrl);

  // Lo que ve alguien en Discord antes de decidir si entra: quién es, cuánta
  // gente hay dentro y cómo conectarse. La IP va en la descripción porque es el
  // dato que se busca, y así se lee sin llegar a abrir el enlace.
  const jugadores =
    servidor.enLinea && servidor.jugadoresConectados !== null
      ? `${formatoNumero.format(servidor.jugadoresConectados)} jugadores conectados`
      : "Sin conexión ahora mismo";

  const descripcion = [
    // El MOTD delante de la descripción: es lo que el servidor dice de sí mismo
    // hoy, mientras que la descripción se escribió al darse de alta y casi nadie
    // vuelve a tocarla. Muchos servidores ni siquiera la rellenan, y para esos
    // era la diferencia entre un resultado de búsqueda con contenido y uno con
    // la IP a secas (ADR-0027).
    textoPlanoDelMotd(servidor.motd) || undefined,
    servidor.descripcion?.trim(),
    `${jugadores} · IP: ${servidor.direccion}`,
  ]
    .filter(Boolean)
    .join(" — ")
    .slice(0, 300);

  // El título lleva las palabras que la gente busca. Nadie escribe el nombre de
  // un servidor que todavía no conoce; escribe qué quiere.
  const titulo = `${servidor.nombre} — Servidor de Minecraft en español`;

  return {
    title: servidor.nombre,
    description: descripcion,
    alternates: { canonical: `/servidores/${servidor.slug}` },

    openGraph: {
      type: "website",
      locale: "es_ES",
      url: `/servidores/${servidor.slug}`,
      title: titulo,
      description: descripcion,
      // La clave `images` solo se pone cuando el servidor tiene imagen propia.
      // Si se pusiera siempre —aunque fuera vacía— Next la tomaría por una
      // decisión y dejaría de usar `opengraph-image.tsx`, que es lo que dibuja
      // la tarjeta de quien todavía no ha subido nada. Un diseño hecho por su
      // dueño gana al generado; no tener ninguno, no.
      ...(imagen
        ? {
            images: [
              {
                url: imagen,
                alt: servidor.nombre,
                ...(esBanner ? { width: 1200, height: 300 } : {}),
              },
            ],
          }
        : {}),
    },

    twitter: {
      // Sin imagen propia manda la generada, que es apaisada 1200×630: ahí la
      // tarjeta grande es la correcta.
      card: esBanner || !imagen ? "summary_large_image" : "summary",
      title: titulo,
      description: descripcion,
      ...(imagen ? { images: [imagen] } : {}),
    },
  };
}

export default async function PaginaServidor(
  props: PageProps<"/servidores/[slug]">,
) {
  const { slug } = await props.params;
  const servidor = await fichaPublica(slug);

  if (!servidor) {
    notFound();
  }

  return (
    <>
      <DatosEstructurados datos={esquemaDeServidor(servidor)} />

      <Cabecera />

      {/* El tema del dueño se aplica sobre un contenedor, no sobre <html>: así
          afecta a SU ficha y no a la cabecera ni al pie. Es la diferencia entre
          "personalizar tu página" y "repintar LatinosMC entero".

          Un código que no exista en el CSS hereda el tema del sitio, que es el
          fallo correcto: la ficha se ve normal en vez de romperse. */}
      <div data-tema={servidor.tema ?? "DEFECTO"} className="contents">

      {/* El banner rompe el ancho del contenido y llega hasta los bordes. Un
          banner encajonado dentro de la columna parece un anuncio pegado; a
          sangre completa, parece la portada del servidor. */}
      <div className="relative">
        {servidor.bannerUrl ? (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={servidor.bannerUrl}
              alt=""
              className="h-44 w-full object-cover sm:h-56"
            />
            {/* Degradado hacia el fondo: sin él, el borde inferior del banner
                corta en seco y el icono que se superpone se lee mal sobre
                imágenes claras. */}
            <div
              aria-hidden
              className="absolute inset-0"
              style={{
                background:
                  "linear-gradient(to bottom, transparent 30%, var(--bg))",
              }}
            />
          </>
        ) : (
          <div className="trama-bloques h-28 w-full sm:h-32" />
        )}
      </div>

      <main className="mx-auto -mt-12 w-full max-w-3xl flex-1 px-4 pb-24 sm:px-6">
        <div className="entra relative flex items-end gap-4">
          {servidor.iconoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={servidor.iconoUrl}
              alt=""
              className="size-20 shrink-0 rounded-xl border-2 border-bg bg-surface object-cover shadow-md sm:size-24"
            />
          ) : (
            <div
              aria-hidden
              className="grid size-20 shrink-0 place-items-center rounded-xl border-2 border-bg bg-primary-subtle text-3xl font-semibold text-primary shadow-md sm:size-24"
            >
              {servidor.nombre.trim().charAt(0).toUpperCase()}
            </div>
          )}

          <div className="min-w-0 flex-1 pb-1">
            <h1 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
              {servidor.nombre}
            </h1>
            <Estado
              enLinea={servidor.enLinea}
              jugadores={servidor.jugadoresConectados}
            />
          </div>
        </div>

        {/* Justo bajo el nombre, antes que nada más. Es lo primero que necesita
            saber quien llega: si puede entrar o no (ADR-0030). */}
        <div className="entra entra-1 mt-4 flex flex-wrap items-center gap-2">
          <EtiquetaDeCuenta aceptaNoPremium={servidor.aceptaOffline} />
          {servidor.soportaBedrock && <EtiquetaBedrock />}
        </div>

        {/* El MOTD va antes que la descripción: es lo que el servidor dice de sí
            mismo AHORA, y muchos lo cambian a diario para anunciar eventos. La
            descripción se escribió una vez al darse de alta y casi nadie vuelve
            a tocarla.

            Se pinta como texto además de ir dibujado en el banner. No es
            duplicarlo por gusto: un lector de pantalla no puede leer un PNG, y
            un buscador tampoco (ADR-0027). */}
        {servidor.motd && (
          <div className="entra entra-1 mt-5 rounded-lg border border-border bg-[#26262b] px-4 py-3">
            <Motd motd={servidor.motd} className="text-white" />
          </div>
        )}

        {servidor.descripcion && (
          <p className="entra entra-1 mt-5 text-lg leading-relaxed text-muted text-pretty">
            {servidor.descripcion}
          </p>
        )}

        <div className="entra entra-2 mt-6">
          <CopiarIp direccion={servidor.direccion} />
        </div>

        {/* Dos columnas en escritorio: los datos a la izquierda, el voto a la
            derecha y siempre visible. El voto es la acción por la que existe
            esta página; enterrarlo al final del scroll sería esconderlo. */}
        <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_320px] lg:items-start">
          <div className="order-2 space-y-6 lg:order-1">
            <section className="entra entra-3">
              <h2 className="mb-3 font-semibold">Datos del servidor</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                <Dato
                  etiqueta="Jugadores ahora"
                  valor={
                    servidor.enLinea && servidor.jugadoresConectados !== null
                      ? formatoNumero.format(servidor.jugadoresConectados)
                      : "—"
                  }
                  nota={
                    servidor.enLinea
                      ? "Medido por nosotros"
                      : "No responde al ping"
                  }
                />
                <Dato
                  etiqueta="Votos esta semana"
                  valor={formatoNumero.format(servidor.votosSemana)}
                  grafico={servidor.tendencia}
                />
                <Dato
                  etiqueta="Dirección"
                  valor={servidor.direccion}
                  monoespaciada
                />
                {/* El tipo de cuenta ya está arriba, en una etiqueta bajo el
                    nombre, que es donde se mira primero. Aquí se quedaba
                    "Premium ..." cortado en una tarjeta que valora números, y
                    repetir el dato en dos sitios solo hacía que uno de los dos
                    se viera mal. En su lugar, lo que aquí falta: si se puede
                    jugar desde el móvil. */}
                <Dato
                  etiqueta="Bedrock Edition"
                  valor={servidor.soportaBedrock ? "Sí" : "No"}
                  nota={
                    servidor.soportaBedrock
                      ? "Se entra desde móvil o consola"
                      : "Solo Java Edition"
                  }
                />
              </div>
            </section>

            <ComoFunciona
              servidor={servidor.nombre}
              ventanaHoras={servidor.ventanaHoras}
            />
          </div>

          <div className="entra entra-2 order-1 lg:sticky lg:top-20 lg:order-2">
            <PanelDeVoto servidorNombre={servidor.nombre} />
          </div>
        </div>
      </main>

      </div>

      <PieDePagina />
    </>
  );
}

/**
 * Estado del servidor.
 *
 * El número lo medimos NOSOTROS con ping directo (ADR-0009), nunca es el que
 * reporta el plugin: corre en hardware del cliente, que es justo quien tiene
 * incentivo para inflarlo. Decirlo en la interfaz es parte del producto.
 */
function Estado({
  enLinea,
  jugadores,
}: {
  enLinea: boolean;
  jugadores: number | null;
}) {
  if (!enLinea || jugadores === null) {
    return (
      <p className="mt-1 flex items-center gap-1.5 text-sm text-subtle">
        <span className="size-2 rounded-full bg-current" aria-hidden />
        Sin conexión ahora mismo
      </p>
    );
  }

  return (
    <p className="mt-1 flex items-center gap-1.5 text-sm text-muted">
      <span className="late size-2 rounded-full bg-success" aria-hidden />
      <span className="cifras font-medium text-text">
        {formatoNumero.format(jugadores)}
      </span>
      {jugadores === 1 ? "jugador conectado" : "jugadores conectados"}
    </p>
  );
}

function Dato({
  etiqueta,
  valor,
  nota,
  grafico,
  monoespaciada,
}: {
  etiqueta: string;
  valor: string;
  nota?: string;
  grafico?: number[];
  monoespaciada?: boolean;
}) {
  return (
    <div className="rounded-lg border border-border bg-surface p-4 shadow-sm">
      <div className="text-xs text-subtle">{etiqueta}</div>
      <div className="mt-1 flex items-end justify-between gap-2">
        <span
          className={`cifras truncate font-semibold ${monoespaciada ? "font-mono text-base" : "text-2xl"}`}
        >
          {valor}
        </span>
        {grafico && (
          <Sparkline valores={grafico} className="mb-1 shrink-0 text-primary" />
        )}
      </div>
      {nota && <div className="mt-1 text-xs text-muted">{nota}</div>}
    </div>
  );
}

/**
 * Explica las reglas antes de que el jugador choque con ellas.
 *
 * Un límite que aparece solo cuando lo incumples se lee como un fallo del sitio.
 * Contado de antemano, se lee como una garantía: es lo que hace que el ranking
 * valga algo.
 */
function ComoFunciona({
  servidor,
  ventanaHoras,
}: {
  servidor: string;
  ventanaHoras: number;
}) {
  return (
    <section className="rounded-lg border border-border bg-surface-sunken p-5">
      <h2 className="font-semibold">Cómo funciona el voto</h2>

      <ul className="mt-3 space-y-3 text-sm">
        <Regla titulo="El voto se pide dentro del juego">
          Escribe <code className="font-mono text-xs">/votar</code> en el chat
          del servidor. Te dará un código, y ese código ya sabe quién eres: por
          eso nadie puede votar en tu nombre.
        </Regla>
        <Regla titulo="Un voto al día, y solo a un servidor">
          El tuyo cuenta igual que el de cualquiera. Por eso el ranking refleja
          qué servidores gustan de verdad y no quién tiene más cuentas.
        </Regla>
        <Regla titulo={`Podrás repetir cada ${ventanaHoras} horas`}>
          Se cuenta desde tu último voto, no desde medianoche.
        </Regla>
        <Regla titulo="Las recompensas las da el servidor">
          Si {servidor} entrega premios por votar, entra al juego después de
          votar y los recibirás en menos de un minuto.
        </Regla>
        <Regla titulo="Nunca te pedimos tu contraseña">
          Solo el código que te da el servidor. Si alguna web te pide tu
          contraseña de Minecraft para votar, no es de fiar.
        </Regla>
      </ul>
    </section>
  );
}

function Regla({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <li className="flex gap-3">
      <span
        aria-hidden
        className="mt-1.5 size-1.5 shrink-0 rounded-full bg-primary"
      />
      <div className="min-w-0">
        <div className="font-medium">{titulo}</div>
        <p className="text-muted text-pretty">{children}</p>
      </div>
    </li>
  );
}
