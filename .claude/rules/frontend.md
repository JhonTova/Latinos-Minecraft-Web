---
paths:
  - "frontend/src/**/*.{ts,tsx,css}"
---

# Código del frontend

Se carga al leer o editar código de `frontend/src/`. Lo general está en
`frontend/AGENTS.md`.

- **Next.js 16 no es el que conoces.** Antes de usar una API de Next (rutas,
  `params`, caché, `proxy.ts`, metadatos), lee su guía en
  `frontend/node_modules/next/dist/docs/`.
- **El navegador nunca llama al backend directamente.** Todo pasa por
  `/api/backend/...`. Una ruta nueva se agrega a la lista blanca del puente con
  la expresión regular más estrecha posible (identificadores acotados, sin
  `.*`).
- **La autorización es del backend.** Lo que hace el frontend (ocultar un
  enlace, redirigir en `proxy.ts`) es solo experiencia de usuario.
- **Colores, radios y sombras solo con los tokens de `globals.css`**
  (`bg-primary`, `text-muted`, `border-border`). Ningún color escrito a mano:
  hay tema claro y oscuro.
- **Toda pantalla tiene sus estados**: cargando, vacío, error y sin permisos.
- Texto visible en español y con tuteo. Los errores no muestran detalles
  técnicos al visitante.
- Accesibilidad: cada campo con su etiqueta y su error enlazados, foco visible
  y contraste AA.
- `/panel/admin` responde 404 a quien no es administrador: nada en la interfaz
  revela que existe.
- Lo patrocinado va en un bloque aparte y etiquetado; nunca dentro del ranking
  orgánico.
- Un componente por archivo, en kebab-case y en español. Antes de crear un
  botón o un campo, mira `src/components/ui.tsx`.
- **No agregues dependencias** a `package.json` sin que el desarrollador lo
  decida.
