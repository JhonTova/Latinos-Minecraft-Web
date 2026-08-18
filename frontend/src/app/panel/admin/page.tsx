import { Tarjeta } from "@/components/ui";
import {
  ajustesDelSitioAdmin,
  ingresos,
  patrocinadoresAdmin,
  reglasDeVoto,
  slotsConfigurables,
  soyAdministrador,
  todosLosPedidos,
} from "@/lib/sesion";
import { ajustesDelSitio } from "@/lib/api";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AvisoDeCambioDePrecio, EditorDeSlots } from "./editor-de-slots";
import { EditorDeMetadatos } from "./editor-de-metadatos";
import { GestionDePatrocinadores } from "./patrocinadores";
import { ReglasDeVotoEditor } from "./reglas-de-voto";

export const metadata: Metadata = { title: "Administración" };

const ESTADOS: Record<string, string> = {
  PENDING: "Pendiente",
  PAID: "Pagado",
  CANCELLED: "Cancelado",
  REFUNDED: "Reembolsado",
  FAILED: "Fallido",
};

export default async function PaginaAdministracion() {
  // 404 y no 403: quien no administra no tiene por qué saber que esta pantalla
  // existe. El backend igualmente rechaza cada petición por su cuenta, así que
  // esto es presentación, no seguridad.
  if (!(await soyAdministrador())) {
    notFound();
  }

  const [slots, pedidos, totales, marcas, voto, metadatos, sitioPublico] =
    await Promise.all([
      slotsConfigurables(),
      todosLosPedidos(),
      ingresos(),
      patrocinadoresAdmin(),
      reglasDeVoto(),
      ajustesDelSitioAdmin(),
      ajustesDelSitio(),
    ]);

  const pagados = pedidos.filter((p) => p.estado === "PAID").length;

  return (
    <main className="mx-auto w-full max-w-2xl flex-1 px-4 py-8 sm:px-6">
      <Link
        href="/panel"
        className="mb-4 inline-block text-sm text-muted transition-colors hover:text-text"
      >
        ← Volver
      </Link>

      <h1 className="mb-6 text-2xl font-semibold tracking-tight">
        Administración
      </h1>

      <section className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {totales.map((t) => (
          <Cifra
            key={t.moneda}
            etiqueta={`Ingresos (${t.moneda})`}
            valor={(t.totalCentimos / 100).toLocaleString("es-ES", {
              style: "currency",
              currency: t.moneda || "EUR",
            })}
          />
        ))}
        <Cifra etiqueta="Pedidos pagados" valor={String(pagados)} />
        <Cifra etiqueta="Pedidos totales" valor={String(pedidos.length)} />
      </section>

      <section className="mb-8">
        <h2 className="mb-1 font-semibold">Espacios destacados</h2>
        <p className="mb-3 text-sm text-muted text-pretty">
          Precio, duración del periodo y cuántos se pueden vender por adelantado.
        </p>
        <div className="mb-3">
          <AvisoDeCambioDePrecio />
        </div>
        <EditorDeSlots slots={slots} />
      </section>

      <section className="mb-8">
        <h2 className="mb-1 font-semibold">Reglas de voto</h2>
        <p className="mb-3 text-sm text-muted text-pretty">
          Cuántas veces puede votar un jugador y desde cuántas conexiones. Es lo
          que decide qué mide el ranking.
        </p>
        <ReglasDeVotoEditor reglas={voto} />
      </section>

      <section className="mb-8">
        <h2 className="mb-1 font-semibold">Cómo se ve la web al compartirla</h2>
        <p className="mb-3 text-sm text-muted text-pretty">
          El título y la descripción que salen en Google, y la imagen que
          aparece al pegar el enlace en Discord o WhatsApp.
        </p>
        <EditorDeMetadatos
          ajustes={metadatos}
          ogImageUrl={sitioPublico.ogImageUrl}
          logoUrl={sitioPublico.logoUrl}
        />
      </section>

      <section className="mb-8">
        <h2 className="mb-1 font-semibold">Patrocinadores de marca</h2>
        <p className="mb-3 text-sm text-muted text-pretty">
          Hostings, VPS y empresas del sector. Se venden hablando, no por la web,
          y por eso se dan de alta aquí.
        </p>
        <GestionDePatrocinadores
          patrocinadores={marcas.marcas}
          ahora={marcas.leidoEn}
        />
      </section>

      <section>
        <h2 className="mb-3 font-semibold">Últimos pedidos</h2>

        {pedidos.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border py-12 text-center text-sm text-muted">
            Todavía no hay ningún pedido.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full text-sm">
              <thead className="bg-surface-sunken text-left text-xs text-muted">
                <tr>
                  <th className="px-3 py-2 font-medium">Estado</th>
                  <th className="px-3 py-2 font-medium">Importe</th>
                  <th className="px-3 py-2 font-medium">Pasarela</th>
                  <th className="px-3 py-2 font-medium">Referencia</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border bg-surface">
                {pedidos.map((p) => (
                  <tr key={p.id}>
                    <td className="px-3 py-2">{ESTADOS[p.estado] ?? p.estado}</td>
                    <td className="px-3 py-2">
                      {(p.importeCentimos / 100).toLocaleString("es-ES", {
                        style: "currency",
                        currency: p.moneda || "EUR",
                      })}
                    </td>
                    <td className="px-3 py-2 text-muted">{p.pasarela ?? "—"}</td>
                    <td className="px-3 py-2 font-mono text-xs text-subtle">
                      {p.referencia ?? "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <p className="mt-6 text-xs text-subtle text-pretty">
        Los cambios de precio quedan registrados con quién los hizo y cuándo. Es
        lo que permite explicar un cobro meses después.
      </p>
    </main>
  );
}

function Cifra({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <Tarjeta>
      <div className="text-xs text-muted">{etiqueta}</div>
      <div className="mt-1 text-xl font-semibold">{valor}</div>
    </Tarjeta>
  );
}
