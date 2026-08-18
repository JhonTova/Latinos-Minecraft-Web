import { Aviso, Tarjeta } from "@/components/ui";
import { misPedidos } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ConfirmarPago } from "./confirmar-pago";

export const metadata: Metadata = { title: "Pedido" };

/**
 * Los códigos vienen de la tabla `order_statuses` y son los que manda la API.
 * Aquí solo se traducen para enseñarlos.
 */
const ESTADOS: Record<string, string> = {
  PENDING: "Pendiente de pago",
  PAID: "Pagado",
  CANCELLED: "Cancelado",
  REFUNDED: "Reembolsado",
  FAILED: "El pago no se completó",
};

const PRODUCTOS: Record<string, string> = {
  FEATURED_SLOT: "Espacio destacado",
  PLUS_PLAN: "Plan Plus",
};

export default async function PaginaPedido(
  props: PageProps<"/panel/pedidos/[pedidoId]">,
) {
  const { pedidoId } = await props.params;
  const parametros = await props.searchParams;

  // PayPal devuelve al usuario con `?token=`. Es la referencia de la orden que
  // hay que capturar para que el dinero se mueva de verdad.
  const referencia = typeof parametros.token === "string" ? parametros.token : null;

  // El listado ya viene filtrado por las organizaciones del usuario, así que un
  // pedido ajeno simplemente no aparece y acaba en 404 — sin decir si existe.
  const pedido = (await misPedidos()).find((p) => p.id === pedidoId);
  if (!pedido) {
    notFound();
  }

  const sinPagar = pedido.estado === "PENDING";
  const importe = (pedido.importeCentimos / 100).toLocaleString("es-ES", {
    style: "currency",
    currency: pedido.moneda ?? "EUR",
  });

  return (
    <main className="mx-auto w-full max-w-lg flex-1 px-4 py-8 sm:px-6">
      <Link
        href="/panel"
        className="mb-4 inline-block text-sm text-muted transition-colors hover:text-text"
      >
        ← Volver
      </Link>

      <h1 className="mb-6 text-2xl font-semibold tracking-tight">Tu pedido</h1>

      {/* Solo se intenta capturar si el pedido sigue pendiente: si el webhook ya
          llegó antes que el usuario, no hay nada que hacer. */}
      {referencia && sinPagar && (
        <div className="mb-6">
          <ConfirmarPago referencia={referencia} />
        </div>
      )}

      {!referencia && sinPagar && (
        <div className="mb-6">
          <Aviso tono="atencion">
            Este pedido está sin pagar. Si cancelaste el pago, puedes volver a
            empezar desde{" "}
            <Link href="/panel/destacar" className="underline">
              Destacar mi servidor
            </Link>
            .
          </Aviso>
        </div>
      )}

      <Tarjeta>
        <dl className="grid grid-cols-2 gap-y-3 text-sm">
          <dt className="text-muted">Producto</dt>
          <dd className="text-right">
            {PRODUCTOS[pedido.producto] ?? pedido.producto}
          </dd>

          <dt className="text-muted">Importe</dt>
          <dd className="text-right font-medium">{importe}</dd>

          <dt className="text-muted">Estado</dt>
          <dd className="text-right">
            {ESTADOS[pedido.estado] ?? pedido.estado}
          </dd>

          <dt className="text-muted">Número</dt>
          <dd className="text-right font-mono text-xs break-all">{pedido.id}</dd>
        </dl>
      </Tarjeta>

      <p className="mt-4 text-xs text-muted text-pretty">
        Guarda este número: es lo que necesitamos si tienes que preguntarnos algo
        sobre el pago.
      </p>
    </main>
  );
}
