import { fichaPublica } from "@/lib/api";
import { ImageResponse } from "next/og";

/**
 * Tarjeta que se ve al pegar el enlace de un servidor en Discord o WhatsApp.
 *
 * Existe por el caso feo: un servidor que todavía no ha subido banner ni icono
 * compartía una tarjeta de solo texto, gris y sin identidad. Y es justo el
 * servidor recién dado de alta el que más está compartiendo su enlace, así que
 * la primera impresión de la plataforma era la peor versión de sí misma.
 *
 * Aquí se dibuja una imagen con lo que ya sabemos de él: su nombre, su icono si
 * lo tiene, cuánta gente hay dentro ahora mismo, sus votos y su IP. Se genera al
 * pedirla, así que los números están al día sin que nadie suba nada.
 *
 * Next la usa automáticamente para `/servidores/[slug]`. Si el servidor tiene
 * banner propio, el `openGraph.images` de `page.tsx` manda sobre esto — un
 * diseño hecho por su dueño siempre gana a uno generado.
 */
export const alt = "Servidor de Minecraft en LatinosMC";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/**
 * Trae el icono y lo incrusta como datos.
 *
 * No se pone la URL directamente en el `<img>` a propósito. La imagen se dibuja
 * en streaming: si el almacén tarda o no responde mientras se está dibujando, no
 * hay forma de capturar ese fallo y el enlace se queda **sin ninguna tarjeta**.
 * Trayéndolo antes, un almacén lento solo cuesta una tarjeta sin icono.
 */
async function iconoIncrustado(url: string | null): Promise<string | null> {
  if (!url || !/^https?:\/\//i.test(url)) return null;

  try {
    const respuesta = await fetch(url, {
      signal: AbortSignal.timeout(2500),
      next: { revalidate: 3600 },
    });
    if (!respuesta.ok) return null;

    const tipo = respuesta.headers.get("content-type") ?? "";
    if (!tipo.startsWith("image/")) return null;

    const bytes = Buffer.from(await respuesta.arrayBuffer());
    // Un icono nuestro pesa unos pocos KB. Si llega algo mucho mayor, algo no
    // cuadra y no merece la pena meterlo en cada tarjeta que se comparta.
    if (bytes.byteLength > 500_000) return null;

    return `data:${tipo};base64,${bytes.toString("base64")}`;
  } catch {
    return null;
  }
}

export default async function Imagen(props: PageProps<"/servidores/[slug]">) {
  const { slug } = await props.params;
  const servidor = await fichaPublica(slug).catch(() => null);
  const icono = await iconoIncrustado(servidor?.iconoUrl ?? null);

  // Sin servidor no se puede dibujar nada suyo, pero devolver un error dejaría
  // el enlace sin ninguna imagen. Una tarjeta con la marca es mejor que un hueco.
  const nombre = servidor?.nombre ?? "LatinosMC";
  const direccion = servidor?.direccion ?? "latinosmc.com";

  const jugadores =
    servidor?.enLinea && servidor.jugadoresConectados !== null
      ? `${servidor.jugadoresConectados} conectados`
      : "Sin conexión";

  const votos = servidor ? `${servidor.votosSemana} votos esta semana` : "";
  const modalidades = servidor?.modalidades.slice(0, 3).join(" · ") ?? "";

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: 64,
          background: "linear-gradient(135deg, #0b1120 0%, #172554 100%)",
          color: "#f8fafc",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 28 }}>
          {/*
            Dentro de ImageResponse solo existe <img>: esto no lo pinta un
            navegador, lo dibuja Satori en el servidor para producir un PNG.
            `next/image` no tiene nada que optimizar aquí y no funciona.
          */}
          {icono ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={icono}
              width={140}
              height={140}
              alt=""
              style={{ borderRadius: 24, objectFit: "cover" }}
            />
          ) : null}

          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            <div
              style={{
                fontSize: 68,
                fontWeight: 700,
                lineHeight: 1.05,
                // El nombre puede ser largo. Sin tope, empuja todo lo demás
                // fuera de la imagen y la tarjeta queda cortada.
                maxWidth: 820,
                overflow: "hidden",
              }}
            >
              {nombre.slice(0, 32)}
            </div>
            {modalidades ? (
              <div style={{ fontSize: 30, color: "#93c5fd" }}>{modalidades}</div>
            ) : null}
          </div>
        </div>

        {servidor?.descripcion ? (
          <div style={{ fontSize: 30, color: "#cbd5e1", maxWidth: 1000 }}>
            {servidor.descripcion.slice(0, 120)}
          </div>
        ) : null}

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div style={{ fontSize: 40, fontWeight: 600 }}>{direccion}</div>
            <div style={{ fontSize: 28, color: "#94a3b8" }}>
              {[jugadores, votos].filter(Boolean).join("  ·  ")}
            </div>
          </div>

          <div
            style={{
              fontSize: 26,
              fontWeight: 600,
              color: "#0b1120",
              background: "#60a5fa",
              padding: "14px 26px",
              borderRadius: 999,
            }}
          >
            latinosmc.com
          </div>
        </div>
      </div>
    ),
    size,
  );
}
