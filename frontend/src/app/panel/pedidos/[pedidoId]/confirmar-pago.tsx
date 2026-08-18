"use client";

import { Aviso } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

type Estado = "confirmando" | "pagado" | "pendiente" | "error";

/**
 * Cierra el cobro cuando el usuario vuelve de PayPal.
 *
 * PayPal aprueba primero y solo mueve el dinero cuando se le pide capturar. Ese
 * paso lo dispara esta pantalla: sin él, el usuario habría dicho que sí, volvería
 * aquí, y no pasaría nada hasta que PayPal anulase la orden a los tres días.
 *
 * Lo que se envía es la referencia que PayPal pone en la URL, no el importe ni el
 * pedido: quién ha pagado y cuánto lo decide el backend preguntándole a PayPal.
 */
export function ConfirmarPago({ referencia }: { referencia: string }) {
  const [estado, setEstado] = useState<Estado>("confirmando");
  const router = useRouter();

  // React monta dos veces en desarrollo; sin este candado se dispararían dos
  // capturas para el mismo pago.
  const yaLanzado = useRef(false);

  useEffect(() => {
    if (yaLanzado.current) return;
    yaLanzado.current = true;

    const controlador = new AbortController();

    fetch("/api/backend/pagos/confirmar", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ referencia }),
      signal: controlador.signal,
    })
      .then(async (respuesta) => {
        if (!respuesta.ok) throw new Error(String(respuesta.status));
        const datos: { estado: string } = await respuesta.json();

        if (datos.estado === "pagado") {
          setEstado("pagado");
          // Recarga los datos del servidor para que el pedido aparezca ya
          // como pagado sin que haga falta refrescar a mano.
          router.refresh();
        } else {
          setEstado("pendiente");
        }
      })
      .catch((e: unknown) => {
        if (e instanceof Error && e.name === "AbortError") return;
        setEstado("error");
      });

    return () => controlador.abort();
  }, [referencia, router]);

  if (estado === "confirmando") {
    return <Aviso tono="info">Confirmando el pago con PayPal…</Aviso>;
  }

  if (estado === "pagado") {
    return (
      <Aviso tono="exito">
        Pago confirmado. Tu servidor ocupará el espacio destacado en la próxima
        rotación.
      </Aviso>
    );
  }

  // Ni "pendiente" ni "error" son un pago perdido: si PayPal llegó a cobrar, su
  // webhook lo confirmará solo. Se dice para que nadie vuelva a pagar por miedo.
  return (
    <Aviso tono="atencion">
      Todavía no hemos podido confirmar el pago. Si PayPal te lo cobró, se
      registrará solo en unos minutos — no vuelvas a pagar. Si en una hora sigue
      sin aparecer, escríbenos con el número de pedido.
    </Aviso>
  );
}
