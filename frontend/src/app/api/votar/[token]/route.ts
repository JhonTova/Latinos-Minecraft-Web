import type { NextRequest } from "next/server";

const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

/**
 * Puente entre el navegador y la API para emitir el voto.
 *
 * Existe por dos motivos:
 *
 * 1. **No exponer la API al navegador.** Si el navegador llamara directo, la
 *    dirección del backend quedaría en el código de la página, lista para que
 *    alguien la ataque saltándose esta capa.
 *
 * 2. **Reenviar la IP real del jugador.** El antifraude se apoya en ella. Si
 *    llegaran todas las peticiones con la IP del servidor de Next, todos los
 *    votantes parecerían la misma persona y las reglas por IP dejarían de
 *    funcionar por completo.
 */
export async function POST(
  peticion: NextRequest,
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;

  const respuesta = await fetch(
    `${API}/vote-tokens/${encodeURIComponent(token)}/vote`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Forwarded-For": ipDelCliente(peticion),
      },
      cache: "no-store",
    },
  );

  // El cuerpo se devuelve tal cual: la API ya habla Problem Details (RFC 9457) y
  // el cliente sabe leerlo. Reescribirlo aquí sería duplicar el contrato en dos
  // sitios que acabarían discrepando.
  const cuerpo = await respuesta.text();
  return new Response(cuerpo, {
    status: respuesta.status,
    headers: {
      "Content-Type":
        respuesta.headers.get("Content-Type") ?? "application/json",
    },
  });
}

/**
 * Primera entrada de `X-Forwarded-For`: es la del cliente original, antes de
 * pasar por ningún proxy.
 */
function ipDelCliente(peticion: NextRequest): string {
  const reenviada = peticion.headers.get("x-forwarded-for");
  if (reenviada) {
    return reenviada.split(",")[0].trim();
  }
  return peticion.headers.get("x-real-ip") ?? "0.0.0.0";
}
