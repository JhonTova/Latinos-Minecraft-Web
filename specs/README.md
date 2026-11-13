# Specs

Aquí vive el desarrollo guiado por especificaciones (SDD) de LatinosMC. Cada
funcionalidad tiene una carpeta `NNN-nombre-corto/` con tres archivos:

| Archivo | Qué contiene |
|---|---|
| `spec.md` | Qué se construye y por qué, con criterios de aceptación verificables |
| `plan.md` | Cómo se construye: decisiones técnicas, archivos que se tocan y la matriz criterio → prueba |
| `tareas.md` | El trabajo en pasos marcados como completados, la evidencia y lo que hubo que corregirle a la IA |

Cada spec se desarrolla en una rama propia (`feat/NNN-nombre-corto`) y entra a
`main` por un pull request.

## Por qué no Spec Kit

El proyecto ya tenía dónde vivir sus principios y sus decisiones antes de
empezar a usar SDD: las reglas no negociables están en `AGENTS.md` y las
decisiones de arquitectura en los ADR de `docs/decisiones/`. Spec Kit habría
traído una `constitution.md` que repetiría lo primero y un juego de comandos
pensado para otra forma de orquestar. Se usa esta estructura propia, más
pequeña, con una skill que la conoce:

- la skill [`spec-sdd`](../.claude/skills/spec-sdd/SKILL.md) crea la carpeta a
  partir de sus plantillas y explica el orden;
- la skill [`revision-de-pruebas`](../.claude/skills/revision-de-pruebas/SKILL.md)
  comprueba que cada criterio tenga una prueba que pueda fallar.

Como no se usa Spec Kit, no hay `constitution.md`.
