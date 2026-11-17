---
name: ruta-del-puente
description: Abre una ruta del backend al navegador a través del puente /api/backend del frontend de LatinosMC, de forma segura (lista blanca con la expresión regular más estrecha y su prueba). Úsala cuando un componente de cliente necesite llamar a un endpoint nuevo, cuando una llamada desde el navegador responda 404 con "Ruta no disponible", o cuando alguien pida "agregar una ruta al proxy" o "a la lista blanca".
---

# Abrir una ruta en el puente

El navegador nunca habla con el backend: llama a `/api/backend/<ruta>` y
`src/app/api/backend/[...ruta]/route.ts` la reenvía. Solo pasan las rutas de la
lista blanca `RUTAS_PERMITIDAS`, en `src/lib/puente.ts`. Esa lista es lo que
impide que la web sea un proxy abierto, así que cada entrada se trata como un
cambio de seguridad.

## Antes de tocar nada

1. **¿De verdad se llama desde el navegador?** Lo que se pide desde un Server
   Component o una Server Action va por `src/lib/api.ts` directo al backend y
   **no necesita** el puente. Solo los componentes con `"use client"` lo usan.
2. **Comprueba que el endpoint existe.** Búscalo en
   `backend/app/src/main/java/com/latinosmc/app/web/` y anota el método HTTP y
   la ruta exacta. No abras una ruta que supones.
3. **Nunca se abren:** `/plugin/*` (es del plugin, con su clave), `/pagos/webhook`
   (es de la pasarela) ni nada de diagnóstico.

## Pasos (la prueba va primero)

1. **Prueba en rojo.** En `src/lib/puente.test.ts` agrega la ruta nueva a la
   lista de «deja pasar» y, en la lista de «responde que no», al menos:
   - la misma ruta con un segmento de más (`.../extra`);
   - la ruta con un identificador mal formado, si lleva uno;
   - una ruta hermana que **no** debe abrirse.

   Ejecuta `npm run test` y confirma que falla solo el caso de «deja pasar».
2. **Patrón.** Agrega la expresión a `RUTAS_PERMITIDAS` con estas reglas:
   - Siempre anclada: empieza con `^` y termina con `$`.
   - Alternativas escritas una a una: `(aprobar|retirar)`, nunca `[a-z]+`.
   - Un UUID es `[0-9a-f-]{36}` con la bandera `i`; un slug es
     `[a-z0-9-]{1,64}`; un rango pequeño se escribe (`[1-6]`), no `\d+`.
   - Prohibido `.*`, `.+` y `[^/]+`.
   - Un comentario encima que diga para qué se abre.
3. **Verde.** `npm run test` pasa completo.
4. **Tipos.** Si la respuesta tiene una forma nueva, declárala en
   `src/lib/tipos.ts` copiando los nombres de los campos del `record` de Java.
   Ese archivo se escribe a mano: un campo inventado no lo detecta nadie.
5. **Comprueba** `npm run lint` y `npm run build`.

## Recuerda

- Que una ruta esté en la lista no autoriza a nadie: el permiso lo comprueba el
  backend en cada endpoint. La lista decide qué se alcanza, nunca quién.
- Una ruta de administración nueva no debe delatar que `/panel/admin` existe:
  el backend responde 404 a quien no es administrador.
- En el pull request, di qué ruta se abrió y con qué patrón.
