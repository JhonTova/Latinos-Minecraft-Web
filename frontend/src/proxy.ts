import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const COOKIE_SESION = "lmc_sesion";

/**
 * Redirección temprana del panel.
 *
 * Se ejecuta ANTES de renderizar nada. Sin esto, la página empieza a
 * transmitirse, el layout descubre a medias que no hay sesión y Next tiene que
 * recurrir a un `meta refresh` con un segundo de retraso: el visitante ve un
 * esqueleto de carga que no lleva a ninguna parte y luego un salto.
 *
 * **Esto es solo experiencia de usuario, no seguridad.** Aquí únicamente se mira
 * si existe la cookie, no si vale: comprobarlo de verdad exigiría una llamada al
 * backend en cada petición, y la documentación desaconseja expresamente usar el
 * proxy para gestión de sesiones.
 *
 * La comprobación real sigue estando en el layout del panel, que pregunta al
 * backend. Una cookie inventada pasa por aquí y se estrella allí.
 */
export function proxy(peticion: NextRequest) {
  const tieneCookie = peticion.cookies.has(COOKIE_SESION);

  if (!tieneCookie) {
    const destino = new URL("/login", peticion.url);
    // Se recuerda a dónde iba para devolverlo ahí tras entrar, en vez de dejarlo
    // en un panel vacío preguntándose qué pasó con el enlace que abrió.
    destino.searchParams.set("volver", peticion.nextUrl.pathname);
    return NextResponse.redirect(destino);
  }

  return NextResponse.next();
}

export const config = {
  matcher: "/panel/:path*",
};
