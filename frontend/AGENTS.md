<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Frontend de LatinosMC — memoria para agentes de código

Lo general del proyecto (reglas, despliegue, commits) está en el `AGENTS.md` de
la raíz. Aquí va solo lo propio del frontend. El bloque de arriba lo escribe
`next dev`: déjalo como está.

Web pública y paneles de LatinosMC en **Next.js 16 (App Router) + React 19 +
TypeScript + Tailwind 4**: directorio y ranking, ficha de cada servidor, página
de votación, panel del dueño (`/panel`) y panel de administración
(`/panel/admin`).

## Comandos (PowerShell, desde `frontend/`)

```powershell
npm ci             # instalar exactamente lo del package-lock.json
npm run dev        # desarrollo en http://localhost:3000 (necesita el backend en :8080)
npm run lint       # ESLint
npm run build      # compilación de producción: también revisa los tipos
```

No hay pruebas automáticas en el frontend: un cambio se da por bueno cuando
`npm run lint` y `npm run build` pasan sin errores **y** se revisó la pantalla en
el navegador, en tema claro y oscuro y en ancho de móvil.

## Organización

- `src/app/`: rutas del App Router. `(auth)` agrupa login y registro sin cambiar
  la URL. `error.tsx` es el estado de error global.
- `src/components/`: componentes, **un archivo por componente, en kebab-case y
  en español** (`fila-servidor.tsx`, `panel-de-voto.tsx`; `theme-toggle.tsx` es
  una excepción antigua). Las piezas básicas
  (campos, botones) están en `ui.tsx`: úsalas antes de crear otras.
- `src/lib/`: `api.ts` (llamadas al backend desde el servidor), `tipos.ts`,
  `sesion.ts`, `acciones.ts` (Server Actions) y `categorias.ts`.
- `src/app/api/backend/[...ruta]/route.ts`: el **único puente** entre el
  navegador y la API. Tiene una lista blanca de rutas (`RUTAS_PERMITIDAS`); una
  ruta nueva del backend que se llame desde el navegador hay que agregarla ahí,
  con la expresión regular más estrecha posible. No la conviertas en un proxy
  abierto.
- `src/proxy.ts`: redirección temprana del panel cuando no hay cookie. Es solo
  experiencia de usuario: la autorización real la hace el backend.

## Convenciones del frontend

- **Colores, radios y sombras solo con los tokens de `src/app/globals.css`**
  (`bg-primary`, `text-muted`, `border-border`…). Nunca escribas un color a mano:
  hay tema claro y oscuro, y la paleta de la marca es morada (tono OKLCH 300).
- Todo texto visible va en español, con tuteo («Inténtalo de nuevo»).
- **Toda pantalla necesita sus estados**: cargando, vacío, error y sin permisos.
- Accesibilidad: cada campo enlazado a su etiqueta y a su error, foco visible,
  contraste WCAG AA.
- Los mensajes de error no muestran detalles técnicos al visitante (ver el
  comentario de `src/app/error.tsx`).
- `/panel/admin` responde **404 a quien no es administrador**, a propósito:
  nada en la interfaz debe revelar que esa ruta existe.
- Lo patrocinado se pinta en un bloque aparte, etiquetado y visualmente
  distinto del ranking orgánico.

## Lo que no debes hacer aquí

- No agregues dependencias sin pedirlo; el proyecto usa solo Next, React y
  Tailwind.
- No llames al backend directamente desde el navegador: pasa por
  `/api/backend/...`.
- No guardes la sesión ni datos sensibles en `localStorage`.
