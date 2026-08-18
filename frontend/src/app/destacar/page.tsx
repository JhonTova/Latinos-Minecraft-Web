import { Cabecera, PieDePagina } from "@/components/cabecera";
import { precio, slotsDestacados, type SlotDestacado } from "@/lib/api";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Destacar tu servidor",
  description: "Aparece en el bloque de destacados de la portada.",
};

/**
 * Venta de slots destacados (ADR-0013).
 *
 * Precio fijo, no subasta: el cliente sabe exactamente qué paga y cuándo lo
 * recibe. Y los slots van en un bloque separado del ranking — nunca dentro —
 * porque el ranking vale precisamente porque no se puede comprar.
 */
export default async function PaginaDestacar() {
  // Si la API no responde, la página sigue siendo útil: se explica el producto
  // y se invita a registrarse. Caerse entera por no poder pintar cinco precios
  // sería perder al visitante por nada.
  const slots = await slotsDestacados().catch((): SlotDestacado[] => []);

  return (
    <>
      <Cabecera />

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 pb-20 sm:px-6">
        <div className="py-10">
          <h1 className="text-3xl font-semibold tracking-tight text-balance">
            Destaca tu servidor
          </h1>
          <p className="mt-2 max-w-xl text-lg text-muted text-pretty">
            Cinco espacios en la portada, por encima del ranking. Precio fijo,
            sin pujas.
          </p>
        </div>

        <section className="grid gap-3 sm:grid-cols-2">
          <Ventaja titulo="Por encima del ranking">
            Tu servidor es lo primero que ve quien entra, sin depender de cuántos
            votos lleves.
          </Ventaja>
          <Ventaja titulo="Semanas completas">
            Cada periodo dura 7 días. Sabes la fecha exacta antes de pagar.
          </Ventaja>
          <Ventaja titulo="Sin pujas ni sorpresas">
            Precio cerrado. Nadie te supera la oferta a última hora.
          </Ventaja>
          <Ventaja titulo="Cola por orden de compra">
            Si el espacio está ocupado, entras en la siguiente rotación con fecha
            confirmada.
          </Ventaja>
        </section>

        <section className="mt-10">
          <h2 className="font-semibold">Los cinco espacios</h2>
          <div className="mt-3 grid gap-3 sm:grid-cols-5">
            {(slots.length > 0 ? slots : PLACEHOLDER).map((slot) => (
              <Link
                key={slot.posicion}
                href={
                  slot.disponible
                    ? `/panel/destacar?slot=${slot.posicion}`
                    : "/panel/destacar"
                }
                className="rounded-lg border border-border bg-surface p-4 text-center transition-colors hover:border-primary hover:bg-surface-sunken"
              >
                <div className="text-xs text-subtle">Espacio</div>
                <div className="mt-0.5 text-2xl font-semibold">
                  {slot.posicion}
                </div>
                <div className="mt-2 text-sm font-medium">
                  {slot.precioCentimos > 0
                    ? precio(slot.precioCentimos, slot.moneda)
                    : "—"}
                </div>
                <div className="mt-0.5 text-xs text-muted">
                  {slot.disponible ? "Libre" : "Ocupado"}
                </div>
              </Link>
            ))}
          </div>

          <div className="mt-5">
            <Link
              href="/panel/destacar"
              className="inline-block rounded-md bg-primary px-4 py-2.5 font-medium text-on-accent transition-colors hover:bg-primary-hover"
            >
              Comprar un espacio
            </Link>
          </div>
          <p className="mt-3 text-xs text-subtle text-pretty">
            El espacio 1 aparece primero. Todos llevan la etiqueta
            «Patrocinado», porque mezclarlos con el ranking haría que el ranking
            dejara de significar nada.
          </p>
        </section>


        <div className="mt-10 flex flex-wrap gap-3">
          <Link
            href="/registro"
            className="rounded-md bg-primary px-4 py-2.5 font-medium text-on-accent transition-colors hover:bg-primary-hover"
          >
            Registrar mi servidor
          </Link>
          <Link
            href="/servidores"
            className="rounded-md border border-border bg-surface px-4 py-2.5 font-medium transition-colors hover:bg-surface-sunken"
          >
            Ver el ranking
          </Link>
        </div>
      </main>

      <PieDePagina />
    </>
  );
}

/**
 * Estado vacío para cuando la API no responde.
 *
 * Cinco huecos sin precio en vez de una parrilla en blanco: se sigue viendo qué
 * se vende, aunque no se pueda decir a cuánto.
 */
const PLACEHOLDER: SlotDestacado[] = [1, 2, 3, 4, 5].map((posicion) => ({
  posicion,
  precioCentimos: 0,
  moneda: "EUR",
  diasPeriodo: 7,
  disponible: true,
  proximoDesde: "",
  proximoHasta: "",
  periodosEnCola: 0,
}));

function Ventaja({
  titulo,
  children,
}: {
  titulo: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-lg border border-border bg-surface p-4">
      <h3 className="font-medium">{titulo}</h3>
      <p className="mt-1 text-sm text-muted text-pretty">{children}</p>
    </div>
  );
}
