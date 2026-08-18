"use server";

import { updateTag } from "next/cache";

/**
 * Invalida la caché de los ajustes del sitio.
 *
 * Existe porque `ajustesDelSitio()` se cachea cinco minutos —aparece en la
 * cabecera de todas las páginas y cambia como mucho una vez al mes— y eso
 * chocaba con el panel de administración, que espera ver el cambio al instante.
 *
 * `router.refresh()` no basta: vuelve a renderizar el componente de servidor,
 * pero el `fetch` sigue sirviendo la respuesta guardada. El síntoma era el peor
 * posible: la subida funcionaba, el archivo se guardaba, y la pantalla seguía
 * diciendo «sin imagen» sin ningún error.
 *
 * **`updateTag` y no `revalidateTag`.** El segundo sirve contenido viejo
 * mientras refresca por detrás, que está bien para un catálogo pero no aquí:
 * quien acaba de subir su logotipo tiene que verlo, no ver el de antes «un
 * ratito más». Solo funciona dentro de una Server Action, que es justo lo que
 * es este archivo.
 */
export async function refrescarSitio() {
  updateTag("sitio");
}

/** Lo mismo para los patrocinadores, que también se cachean cinco minutos. */
export async function refrescarPatrocinadores() {
  updateTag("patrocinadores");
}
