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
          Elige espacio y semana. Ves el precio y las fechas antes de pagar.
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
          slotInicial={
            // El tope tiene que seguir al CHECK de featured_slots (V24). Es solo
            // qué espacio viene preseleccionado: la compra la valida el backend.
            Number.isInteger(slotPedido) && slotPedido >= 1 && slotPedido <= 6
              ? slotPedido
              : null
          }
        />
      </Tarjeta>

      {/* Esto no es relleno: es lo que el comprador tiene que saber antes de
          pagar. Su espacio va etiquetado como patrocinado y fuera del ranking. */}
      <p className="mt-4 text-xs text-subtle text-pretty">
        Tu servidor aparece en un bloque aparte, etiquetado como patrocinado, y
        nunca dentro del ranking.
      </p>
    </main>
  );
}
