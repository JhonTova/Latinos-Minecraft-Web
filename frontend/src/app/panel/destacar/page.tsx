import { Aviso, Tarjeta } from "@/components/ui";
import { slotsDestacados } from "@/lib/api";
import { misPedidos, misServidores } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { Comprar } from "./comprar";

export const metadata: Metadata = { title: "Destacar mi servidor" };

export default async function PaginaComprarDestacado(
  props: PageProps<"/panel/destacar">,
) {
  const parametros = await props.searchParams;
  const slotPedido = Number(parametros.slot);

  const [slots, servidores, pedidos] = await Promise.all([
    slotsDestacados(),
    misServidores(),
    misPedidos(),
  ]);

  // "PENDING" es el código de la tabla `order_statuses`, no una etiqueta: la
  // traducción a español se hace al pintarlo, nunca al compararlo.
  const pendientes = pedidos.filter((p) => p.estado === "PENDING");

  return (
    <main className="mx-auto w-full max-w-lg flex-1 px-4 py-8 sm:px-6">
      <Link
        href="/panel"
        className="mb-4 inline-block text-sm text-muted transition-colors hover:text-text"
      >
        ← Volver
      </Link>

      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">
          Destacar mi servidor
        </h1>
        <p className="mt-1 text-sm text-muted text-pretty">
          Cinco espacios en la portada, por encima del ranking. Precio cerrado y
          fecha confirmada antes de pagar.
        </p>
      </div>

      {pendientes.length > 0 && (
        <div className="mb-6">
          <Aviso tono="atencion">
            Tienes {pendientes.length === 1 ? "un pedido" : `${pendientes.length} pedidos`} sin
            pagar.{" "}
            <Link href={`/panel/pedidos/${pendientes[0].id}`} className="underline">
              Verlo
            </Link>{" "}
            antes de empezar otro.
          </Aviso>
        </div>
      )}

      <Tarjeta>
        <Comprar
          slots={slots}
          servidores={servidores}
          slotInicial={Number.isInteger(slotPedido) && slotPedido >= 1 && slotPedido <= 5 ? slotPedido : null}
        />
      </Tarjeta>

      <p className="mt-4 text-xs text-subtle text-pretty">
        Los destacados van en un bloque aparte y etiquetados, nunca dentro del
        ranking. Si las posiciones del ranking se pudieran comprar, dejaría de
        significar nada y el tráfico que te manda se iría con él.
      </p>
    </main>
  );
}
