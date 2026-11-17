import type { NextRequest } from "next/server";

import { ipDelCliente, rutaPermitida } from "@/lib/puente";

const API = process.env.API_URL ?? "http://localhost:8080/api/v1";

/**
 * Puente entre el navegador y la API.
 *
 * Todo pasa por aquí en lugar de que el navegador llame al backend directamente,
 * por dos motivos:
 *
 * 1. **La cookie de sesión vive en un solo dominio.** Si el navegador hablara con
 *    `api.dominio.com` y la web fuera `dominio.com`, habría que compartir la
 *    cookie entre subdominios — más superficie y más formas de equivocarse. Así
 *    todo es del mismo origen.
 *
 * 2. **La dirección del backend no se publica.** No aparece en el código que
 *    descarga el navegador.
 *
 * La lista blanca (`rutaPermitida`) es lo que impide que esto sea un proxy
 * abierto. Vive en `src/lib/puente.ts`, con sus pruebas.
 */
async function proxy(peticion: NextRequest, ruta: string[]) {
  const camino = ruta.join("/");

  if (!rutaPermitida(camino)) {
    return Response.json(
      { title: "not_found", status: 404, detail: "Ruta no disponible." },
      { status: 404 },
    );
  }

  // arrayBuffer y no text(): el cuerpo puede ser binario (una imagen dentro de
  // un multipart). Leerlo como texto lo pasaría por UTF-8 y destrozaría los
  // bytes — el archivo llegaría corrupto al servidor y fallaría al decodificarse
  // con un error que no apunta a ninguna parte.
  const cuerpo =
    peticion.method === "GET" || peticion.method === "HEAD"
      ? undefined
      : await peticion.arrayBuffer();

  const respuesta = await fetch(`${API}/${camino}${peticion.nextUrl.search}`, {
    method: peticion.method,
    headers: {
      // Se reenvía tal cual porque en multipart lleva el 'boundary', y sin él el
      // servidor no sabe dónde empieza y acaba cada parte.
      "Content-Type":
        peticion.headers.get("content-type") ?? "application/json",
      // La sesión viaja hacia el backend...
      Cookie: peticion.headers.get("cookie") ?? "",
      // ...y la IP real del usuario también: el antifraude y el control de
      // tráfico la necesitan. Sin esto, todos los usuarios parecerían la misma
      // persona y ambos mecanismos dejarían de funcionar.
      "X-Forwarded-For": ipDelCliente(peticion.headers.get("x-forwarded-for")),
    },
    body: cuerpo,
    cache: "no-store",
    redirect: "manual",
  });

  const cabeceras = new Headers();

  // Set-Cookie de vuelta al navegador: es lo que abre y cierra la sesión.
  const cookies = respuesta.headers.getSetCookie?.() ?? [];
  for (const cookie of cookies) {
    cabeceras.append("Set-Cookie", cookie);
  }

  // Sin esto, una redirección del backend llegaría al navegador sin destino: el
  // usuario vería una página en blanco. Es lo que mueve el acceso con Google y
  // Discord, que es una cadena de redirecciones de principio a fin.
  const destino = respuesta.headers.get("location");
  if (destino) {
    cabeceras.set("Location", destino);
  }

  // 204 y 304 no pueden llevar cuerpo. Devolver uno —aunque sea vacío— hace que
  // el runtime rechace la respuesta y el cliente reciba un 500 por una operación
  // que en realidad salió bien. Es un fallo especialmente traicionero: la acción
  // se ejecuta y aun así el usuario ve un error.
  if (respuesta.status === 204 || respuesta.status === 304) {
    return new Response(null, { status: respuesta.status, headers: cabeceras });
  }

  const tipo = respuesta.headers.get("content-type");
  if (tipo) {
    cabeceras.set("Content-Type", tipo);
  }

  return new Response(await respuesta.text(), {
    status: respuesta.status,
    headers: cabeceras,
  });
}

type Contexto = { params: Promise<{ ruta: string[] }> };

export async function GET(peticion: NextRequest, { params }: Contexto) {
  return proxy(peticion, (await params).ruta);
}

export async function POST(peticion: NextRequest, { params }: Contexto) {
  return proxy(peticion, (await params).ruta);
}

export async function PUT(peticion: NextRequest, { params }: Contexto) {
  return proxy(peticion, (await params).ruta);
}

export async function PATCH(peticion: NextRequest, { params }: Contexto) {
  return proxy(peticion, (await params).ruta);
}

export async function DELETE(peticion: NextRequest, { params }: Contexto) {
  return proxy(peticion, (await params).ruta);
}
