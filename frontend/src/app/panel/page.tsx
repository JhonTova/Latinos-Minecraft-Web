import { Aviso } from "@/components/ui";
import {
  misOrganizaciones,
  misServidores,
  perfilActual,
  soyAdministrador,
} from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = { title: "Mis servidores" };

const ESTADOS: Record<string, { texto: string; clase: string }> = {
  ACTIVE: { texto: "Activo", clase: "bg-success" },
  PENDING_VERIFICATION: { texto: "Esperando al plugin", clase: "bg-warning" },
  SUSPENDED: { texto: "Suspendido", clase: "bg-danger" },
  DRAFT: { texto: "Borrador", clase: "bg-subtle" },
  ARCHIVED: { texto: "Archivado", clase: "bg-subtle" },
};

export default async function PaginaPanel() {
  const [perfil, organizaciones, servidores, administra] = await Promise.all([
    perfilActual(),
    misOrganizaciones(),
    misServidores(),
    soyAdministrador(),
  ]);

  const org = organizaciones[0];
  const sinLimite = org?.limiteServidores === null;
  const puedeCrear =
    sinLimite || (org && org.servidoresUsados < org.limiteServidores!);

  return (
    <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-8 sm:px-6">
      {!perfil?.emailVerificado && (
        <div className="mb-6">
          <Aviso tono="atencion">
            Tu correo aún no está verificado. No podrás registrar servidores
            hasta que lo confirmes.{" "}
            <Link href="/panel/verificar" className="font-medium underline">
              Verificar ahora
            </Link>
          </Aviso>
        </div>
      )}

      <div className="entra mb-6 flex items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Mis servidores
          </h1>
          {org && (
            <p className="mt-1 text-sm text-muted">
              Plan {org.plan === "FREE" ? "Gratuito" : "Plus"} ·{" "}
              {sinLimite
                ? `${org.servidoresUsados} servidores`
                : `${org.servidoresUsados} de ${org.limiteServidores}`}
            </p>
          )}
        </div>

        {puedeCrear && perfil?.emailVerificado && (
          <Link
            href="/panel/servidores/nuevo"
            className="boton-vivo shrink-0 rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent shadow-sm hover:bg-primary-hover hover:shadow-md"
          >
            Añadir servidor
          </Link>
        )}
      </div>

      {!puedeCrear && (
        <div className="mb-6">
          <Aviso tono="info">
            Has llegado al límite de {org?.limiteServidores} servidores del plan
            gratuito. Con Plus puedes tener los que quieras.
          </Aviso>
        </div>
      )}

      {servidores.length === 0 ? (
        <SinServidores verificado={perfil?.emailVerificado ?? false} />
      ) : (
        <ul className="entra entra-1 divide-y divide-border overflow-hidden rounded-lg border border-border bg-surface shadow-sm">
          {servidores.map((servidor) => {
            const estado = ESTADOS[servidor.estado] ?? {
              texto: servidor.estado,
              clase: "bg-subtle",
            };
            return (
              <li key={servidor.id}>
                <Link
                  href={`/panel/servidores/${servidor.id}`}
                  className="group flex items-center gap-4 px-4 py-3 transition-colors duration-200 hover:bg-surface-sunken"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium group-hover:text-primary">
                      {servidor.nombre}
                    </p>
                    <p className="truncate text-sm text-muted">
                      {servidor.descripcion || "Sin descripción"}
                    </p>
                  </div>
                  <span className="flex shrink-0 items-center gap-1.5 text-xs text-muted">
                    <span
                      className={`size-1.5 rounded-full ${estado.clase}`}
                      aria-hidden
                    />
                    {estado.texto}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}

      {/* Solo cuando ya hay algo que destacar. Ofrecérselo a quien todavía no ha
          registrado su primer servidor es ruido en la pantalla que más importa. */}
      {servidores.length > 0 && (
        <Link
          href="/panel/destacar"
          className="tarjeta-viva group mt-6 flex items-center gap-4 rounded-lg border border-border bg-surface px-4 py-3 shadow-sm hover:border-primary/40"
        >
          <div className="min-w-0 flex-1">
            <p className="font-medium">Destacar mi servidor</p>
            <p className="text-sm text-muted text-pretty">
              Uno de los cinco espacios de la portada, por encima del ranking.
            </p>
          </div>
          <span
            aria-hidden
            className="text-muted transition-transform duration-200 group-hover:translate-x-0.5"
          >
            →
          </span>
        </Link>
      )}

      {/* El enlace solo aparece para quien administra, pero esconderlo no es la
          protección: el backend rechaza cada petición por su cuenta. Esto es
          para no enseñar puertas cerradas a quien no las puede abrir. */}
      {administra && (
        <Link
          href="/panel/admin"
          className="tarjeta-viva group mt-3 flex items-center gap-4 rounded-lg border border-border bg-surface px-4 py-3 shadow-sm hover:border-primary/40"
        >
          <div className="min-w-0 flex-1">
            <p className="font-medium">Administración</p>
            <p className="text-sm text-muted text-pretty">
              Precios, periodos y pedidos de toda la plataforma.
            </p>
          </div>
          <span
            aria-hidden
            className="text-muted transition-transform duration-200 group-hover:translate-x-0.5"
          >
            →
          </span>
        </Link>
      )}
    </main>
  );
}

/**
 * Estado vacío. Es la primera pantalla que ve todo el mundo y la que decide si
 * se queda: por eso lleva una sola acción y ninguna decoración.
 */
function SinServidores({ verificado }: { verificado: boolean }) {
  return (
    <div className="rounded-lg border border-dashed border-border py-16 text-center">
      <h2 className="font-medium">Todavía no tienes servidores</h2>
      <p className="mx-auto mt-1 max-w-sm text-sm text-muted text-pretty">
        Añade tu primer servidor y te daremos la clave para conectar el plugin.
        Se tarda menos de dos minutos.
      </p>
      <Link
        href={verificado ? "/panel/servidores/nuevo" : "/panel/verificar"}
        className="mt-5 inline-block rounded-md bg-primary px-4 py-2 text-sm font-medium text-on-accent transition-colors hover:bg-primary-hover"
      >
        {verificado ? "Añadir mi primer servidor" : "Verificar mi correo"}
      </Link>
    </div>
  );
}
