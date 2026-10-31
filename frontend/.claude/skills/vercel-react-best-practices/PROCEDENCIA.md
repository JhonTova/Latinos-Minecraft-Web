# Procedencia de esta skill

Este archivo no es parte de la skill original: lo agregó el equipo de LatinosMC
para saber de dónde salió y cómo se revisó. El resto de la carpeta está copiado
sin cambios.

| | |
|---|---|
| Origen | https://github.com/vercel-labs/agent-skills/tree/1a2c79d4e6c4a2283dc2c614b6538cbe1ed5ca62/skills/react-best-practices |
| Commit | `1a2c79d4e6c4a2283dc2c614b6538cbe1ed5ca62` |
| Autor | Vercel (organización verificada en GitHub) |
| Licencia | MIT, según el campo `license` de `SKILL.md`. El repositorio de origen no tiene archivo LICENSE |
| Fecha de copia | 31/10/2026 |
| Instalada por | PR #2 |

## Revisión de seguridad hecha antes de instalarla

- 76 archivos: todo Markdown salvo `metadata.json`. **Ningún script.**
- Búsquedas: comandos que descargan, ejecutan o borran (`curl`, `wget`,
  `rm -rf`, `Invoke-WebRequest`, `child_process`, `eval(`), credenciales y
  secretos, comentarios HTML ocultos y caracteres Unicode invisibles. Solo
  aparecieron `npx svgo` (ejemplo de optimización de SVG) y `pnpm install` (en el
  README, para quien mantiene la skill).
- Las URLs son documentación (react.dev, nextjs.org, vercel.com) y ejemplos
  (`example.com`).

## Para actualizarla

Copiar de nuevo la carpeta desde un commit concreto del repositorio de origen
(no desde `main` sin fijar), repetir la revisión y actualizar este archivo.
