import type { NextRequest } from "next/server";

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
 * La lista blanca es lo que impide que esto sea un proxy abierto: sin ella,
 * cualquiera podría alcanzar *cualquier* ruta interna a través de la web.
 */
const RUTAS_PERMITIDAS: RegExp[] = [
  /^auth\/(registro|login|logout|yo|verificar|reenviar-codigo|sesiones)$/,
  /^servers$/,
  /^servers\/(mios|mis-organizaciones|modalidades)$/,
  // Ficha y subida de imágenes de un servidor concreto. El UUID se acota en el
  // patrón: sin eso, este comodín abriría la puerta a cualquier subruta.
  /^servers\/[0-9a-f-]{36}$/i,
  /^servers\/[0-9a-f-]{36}\/imagenes\/(icono|banner)$/i,
  // Compra de espacios destacados. `confirmar` es el paso de captura que PayPal
  // exige cuando el usuario vuelve de pagar.
  /^pagos\/(slots|pedidos|confirmar)$/,
  // Voto desde la web escribiendo el nick. El límite de tráfico y el antifraude
  // viven en el backend; esto solo abre la ruta.
  /^servers\/[a-z0-9-]{1,64}\/votes$/,
  // Administración de la plataforma. Que la ruta esté permitida aquí no da
  // acceso a nada: el backend comprueba el permiso en cada endpoint. Esta lista
  // solo decide qué se puede alcanzar, nunca quién.
  /^admin\/(soy|slots|pedidos|ingresos|administradores|patrocinadores|voto|sitio)$/,
  /^admin\/sitio\/(imagen|logo)$/,
  /^admin\/slots\/[1-5]$/,
  /^admin\/patrocinadores\/[0-9a-f-]{36}\/(aprobar|retirar)$/i,
];

async function proxy(peticion: NextRequest, ruta: string[]) {
  const camino = ruta.join("/");

  if (!RUTAS_PERMITIDAS.some((patron) => patron.test(camino))) {
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
      "X-Forwarded-For": ipDelCliente(peticion),
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

/**
 * IP real del visitante.
 *
 * **Se lee una sola cabecera**, la misma que el backend, y por el mismo motivo.
 * Antes se probaban tres en orden quedándose con la primera que viniera, y esa
 * cadena de alternativas era el agujero: ninguna cabecera prueba nada —las
 * escribe quien envía la petición— y lo único que las hace fiables es que
 * nuestro proxy las reescriba. Con tres opciones, a quien quisiera falsificar su
 * IP le bastaba con elegir la que el proxy no estuviera limpiando.
 *
 * El proxy de delante DEBE reescribir `X-Forwarded-For` con la IP real
 * (`LANZAMIENTO.md` § 7). Si no lo hace, nada de lo que llegue aquí vale.
 *
 * Esto no es un detalle: la IP decide si un voto se acepta. Si aquí llegara
 * siempre la misma, todos los votantes parecerían la misma persona y solo
 * votaría uno al día en todo el sitio; si cada cual pudiera elegir la suya, el
 * límite no filtraría nada.
 */
function ipDelCliente(peticion: NextRequest): string {
  const reenviada = peticion.headers.get("x-forwarded-for");
  if (reenviada) {
    const primera = reenviada.split(",")[0].trim();
    if (primera) return primera;
  }
  return "0.0.0.0";
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
