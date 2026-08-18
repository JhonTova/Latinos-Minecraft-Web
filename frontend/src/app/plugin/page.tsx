import { BarraLateral } from "@/components/barra-lateral";
import { Cabecera, PieDePagina } from "@/components/cabecera";
import { EncabezadoDeBloque } from "@/components/encabezado-de-bloque";
import { ajustesDelSitio } from "@/lib/api";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "El plugin",
  description:
    "Instala el plugin de LatinosMC en tu servidor de Minecraft: un solo comando, recompensas automáticas y qué datos recoge exactamente.",
};

const API = "https://api.latinosmc.com";

export default async function PaginaPlugin() {
  const sitio = await ajustesDelSitio();

  return (
    <>
      <Cabecera />

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-20 sm:px-6">
        <div className="py-8 sm:py-10">
          <h1 className="display text-3xl font-semibold">El plugin</h1>
          <p className="mt-2 max-w-2xl text-muted text-pretty">
            Un solo comando. Tus jugadores escriben{" "}
            <Codigo>/votar</Codigo>, votan, y la recompensa llega sola. Ni
            escriben su nombre, ni repiten el comando para reclamar nada.
          </p>

          <div className="mt-5 flex flex-wrap items-center gap-3">
            {sitio.urlPlugin ? (
              <a
                href={sitio.urlPlugin}
                target="_blank"
                rel="noopener"
                className="boton-vivo inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 font-medium text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
              >
                <svg viewBox="0 0 24 24" className="size-5" aria-hidden>
                  <path
                    d="M12 3v12m0 0 4-4m-4 4-4-4M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                Descargar
              </a>
            ) : (
              <span className="rounded-lg border border-dashed border-border px-5 py-2.5 text-sm text-muted">
                La descarga estará disponible en breve
              </span>
            )}

            <span className="cifras rounded-md border border-border bg-surface px-3 py-2 font-mono text-sm text-muted">
              Paper 1.20+ · Java 21
            </span>
          </div>
        </div>

        <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_290px] lg:items-start">
          <div className="min-w-0 space-y-10">
            {/* --- Cómo funciona ---------------------------------------- */}
            <section aria-labelledby="titulo-como">
              <EncabezadoDeBloque
                id="titulo-como"
                titulo="Cómo funciona"
                nota="Tres pasos, un comando"
              />

              <ol className="grid gap-3 sm:grid-cols-3">
                <Paso numero={1} titulo="/votar en el chat">
                  El plugin devuelve un enlace y un código corto. El código lleva
                  dentro quién es el jugador, así que la web nunca le pregunta el
                  nombre.
                </Paso>
                <Paso numero={2} titulo="Vota en la web">
                  Si puede pulsar enlaces, pulsa. Si juega desde móvil o consola,
                  escribe el código: ocho caracteres.
                </Paso>
                <Paso numero={3} titulo="Recibe su recompensa">
                  Un minuto después, el plugin comprueba y entrega lo que hayas
                  configurado. Sin segundo comando.
                </Paso>
              </ol>
            </section>

            {/* --- Transparencia ---------------------------------------- */}
            <section aria-labelledby="titulo-datos">
              <EncabezadoDeBloque
                id="titulo-datos"
                titulo="Qué datos recoge"
                nota="Vas a instalar código nuestro en tu máquina"
              />

              <div className="grid gap-3 sm:grid-cols-2">
                <Panel titulo="Lo que envía" tono="neutro">
                  <Punto>Un latido cada minuto para confirmar que sigue vivo.</Punto>
                  <Punto>
                    El nombre o el UUID del jugador que escribe{" "}
                    <Codigo>/votar</Codigo>, para emitir su código.
                  </Punto>
                  <Punto>
                    Qué recompensas ha entregado, para no repetirlas.
                  </Punto>
                </Panel>

                <Panel titulo="Lo que NO envía" tono="bueno">
                  <Punto>Chats, coordenadas, inventarios ni construcciones.</Punto>
                  <Punto>
                    La lista de conectados. Eso lo medimos desde fuera con un
                    ping a tu IP pública — el mismo que puede hacer cualquiera.
                  </Punto>
                  <Punto>Nada de quien no ejecute el comando.</Punto>
                </Panel>
              </div>

              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Panel titulo="Nunca ejecutamos comandos en tu servidor" tono="bueno">
                  Enviamos un <em>tipo</em> de recompensa. Qué comando ejecuta
                  cada tipo lo decides tú en el <Codigo>config.yml</Codigo>. No
                  es una limitación: es la protección. Si pudiéramos mandar texto
                  de comando, un fallo nuestro se ejecutaría en tu máquina.
                </Panel>

                <Panel titulo="Lo que reporta tu servidor no sube tu posición" tono="neutro">
                  Los jugadores conectados del ranking los medimos nosotros. No
                  es desconfianza hacia ti: un ranking donde cada uno declara sus
                  propios números deja de significar algo para todos.
                </Panel>
              </div>

              <p className="mt-3 text-xs text-subtle text-pretty">
                El JAR es público y el código abierto: puedes abrirlo y comprobar
                cada línea de lo de arriba.
              </p>
            </section>

            {/* --- Para jugadores --------------------------------------- */}
            <section aria-labelledby="titulo-jugadores">
              <EncabezadoDeBloque
                id="titulo-jugadores"
                titulo="Qué tienen que hacer tus jugadores"
              />

              <div className="rounded-lg border border-border bg-surface p-5">
                <p className="text-pretty">
                  Escribir <Codigo grande>/votar</Codigo>, abrir el enlace y
                  votar. Nada más.
                </p>
                <ul className="mt-4 space-y-2 text-sm text-muted">
                  <Punto>
                    <strong className="text-text">No escriben su nombre</strong>{" "}
                    en ninguna parte. El código ya sabe quiénes son, y por eso
                    nadie puede votar suplantándolos.
                  </Punto>
                  <Punto>
                    <strong className="text-text">No repiten el comando.</strong>{" "}
                    Un minuto después de votar, la recompensa aparece sola.
                  </Punto>
                  <Punto>
                    <strong className="text-text">
                      Nunca les pedimos contraseñas.
                    </strong>{" "}
                    Si una web de votos se la pide, no es de fiar.
                  </Punto>
                  <Punto>
                    Los de <strong className="text-text">Bedrock</strong> —móvil
                    y consola— no pueden pulsar enlaces en el chat, así que
                    escriben el código. Funciona igual.
                  </Punto>
                </ul>
              </div>
            </section>

            {/* --- API --------------------------------------------------- */}
            <section aria-labelledby="titulo-api">
              <EncabezadoDeBloque
                id="titulo-api"
                titulo="API pública"
                etiqueta="Para desarrolladores"
                nota="Sin claves ni registro"
              />

              <div className="space-y-2">
                <Endpoint
                  ruta="/api/v1/ranking?dias=7&limite=50"
                  titulo="Ranking de servidores"
                >
                  Nombre, IP, modalidades, versiones, jugadores conectados y la
                  serie de votos por día para dibujar una gráfica.
                </Endpoint>
                <Endpoint ruta="/api/v1/directorio/{slug}" titulo="Ficha de un servidor">
                  Todo lo de un servidor concreto. El <Codigo>slug</Codigo> es lo
                  que aparece en la URL de su página.
                </Endpoint>
                <Endpoint ruta="/api/v1/estadisticas" titulo="Cifras de la plataforma">
                  Servidores totales, cuántos responden ahora y suma de jugadores
                  conectados.
                </Endpoint>
                <Endpoint ruta="/api/v1/destacados" titulo="Servidores patrocinados">
                  Los cinco espacios de la portada.
                </Endpoint>
                <Endpoint ruta="/api/v1/patrocinadores" titulo="Marcas patrocinadoras">
                  Las empresas que patrocinan el sitio, con logotipo y enlace.
                </Endpoint>
              </div>

              <details className="mt-3 rounded-lg border border-border bg-surface p-4">
                <summary className="cursor-pointer text-sm font-medium">
                  Ejemplo: el top 5 en tu web
                </summary>
                <pre className="mt-3 overflow-x-auto rounded-md bg-surface-sunken p-3 text-xs">
                  <code>{`const r = await fetch("${API}/api/v1/ranking?limite=5");
const servidores = await r.json();

for (const s of servidores) {
  console.log(s.nombre, s.direccion, s.votos, "votos");
}`}</code>
                </pre>
              </details>

              <p className="mt-3 text-xs text-subtle text-pretty">
                Base: <Codigo>{API}</Codigo> · Hay un límite de peticiones por IP
                y minuto, generoso para uso normal. Las respuestas ya vienen con
                sus cabeceras de caché: respetarlas te ahorra latencia a ti y
                carga a nosotros.
              </p>
            </section>

            {/* --- Instalación ------------------------------------------- */}
            <section aria-labelledby="titulo-instalar">
              <EncabezadoDeBloque id="titulo-instalar" titulo="Instalación" />

              <ol className="space-y-2">
                <PasoNumerado numero={1}>
                  Registra tu servidor en{" "}
                  <Link href="/registro" className="enlace-vivo text-primary">
                    latinosmc.com
                  </Link>{" "}
                  y guarda la clave — solo se muestra una vez.
                </PasoNumerado>
                <PasoNumerado numero={2}>
                  Copia el JAR en <Codigo>plugins/</Codigo> y arranca el servidor
                  una vez.
                </PasoNumerado>
                <PasoNumerado numero={3}>
                  Pega tu clave en{" "}
                  <Codigo>plugins/LatinosMC/config.yml</Codigo> y reinicia.
                </PasoNumerado>
                <PasoNumerado numero={4}>
                  Ajusta las recompensas y los mensajes en ese mismo archivo.
                </PasoNumerado>
              </ol>
            </section>
          </div>

          <div className="lg:sticky lg:top-28">
            <BarraLateral />
          </div>
        </div>
      </main>

      <PieDePagina />
    </>
  );
}

/* -------------------------------------------------------------------------- */

function Paso({
  numero,
  titulo,
  children,
}: {
  numero: number;
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <li className="rounded-lg border border-border bg-surface p-4">
      <span className="cifras grid size-7 place-items-center rounded-full bg-primary-subtle text-sm font-bold text-primary">
        {numero}
      </span>
      <h3 className="mt-2.5 font-semibold">{titulo}</h3>
      <p className="mt-1 text-sm text-muted text-pretty">{children}</p>
    </li>
  );
}

function PasoNumerado({
  numero,
  children,
}: {
  numero: number;
  children: React.ReactNode;
}) {
  return (
    <li className="flex gap-3 rounded-lg border border-border bg-surface px-4 py-3">
      <span className="cifras grid size-6 shrink-0 place-items-center rounded-full bg-primary-subtle text-xs font-bold text-primary">
        {numero}
      </span>
      <span className="text-sm text-pretty">{children}</span>
    </li>
  );
}

function Panel({
  titulo,
  tono,
  children,
}: {
  titulo: string;
  tono: "bueno" | "neutro";
  children: React.ReactNode;
}) {
  return (
    <div
      className={`rounded-lg border p-4 ${
        tono === "bueno"
          ? "border-success/30 bg-success/5"
          : "border-border bg-surface"
      }`}
    >
      <h3 className="text-sm font-semibold text-pretty">{titulo}</h3>
      <div className="mt-2 space-y-1.5 text-sm text-muted text-pretty">
        {children}
      </div>
    </div>
  );
}

function Punto({ children }: { children: React.ReactNode }) {
  return (
    <li className="flex gap-2 text-pretty">
      <span
        aria-hidden
        className="mt-1.5 size-1 shrink-0 rounded-full bg-current opacity-50"
      />
      <span>{children}</span>
    </li>
  );
}

function Endpoint({
  ruta,
  titulo,
  children,
}: {
  ruta: string;
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-lg border border-border bg-surface p-3.5">
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded bg-success/15 px-1.5 py-0.5 font-mono text-[11px] font-semibold text-success">
          GET
        </span>
        <code className="font-mono text-sm break-all">{ruta}</code>
      </div>
      <p className="mt-1.5 text-sm text-muted text-pretty">
        <span className="font-medium text-text">{titulo}.</span> {children}
      </p>
    </div>
  );
}

function Codigo({
  children,
  grande,
}: {
  children: React.ReactNode;
  grande?: boolean;
}) {
  return (
    <code
      className={`rounded bg-surface-sunken px-1.5 py-0.5 font-mono text-text ${
        grande ? "text-base" : "text-[0.9em]"
      }`}
    >
      {children}
    </code>
  );
}
