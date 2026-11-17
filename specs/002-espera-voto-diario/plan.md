# Plan 002 — Decir cuánto falta cuando el voto del día ya se usó

Spec: [spec.md](spec.md)

## Mapa del cambio

Lo encontró el `analista` (informe del 13/11/2026):

- **Parte:** solo frontend, 🟢 ligero. El backend (`voting`, 🔴) solo se lee:
  `segundosRestantes` sale de `ManejadorDeErrores.java:34` (ventana por
  servidor) y `:48` (voto diario); los códigos, de `CastVoteService.java:262`
  (`vote.window.open`) y `:283` (`vote.daily.used`).
- **Endpoint consumido, sin cambios:** `POST /api/v1/vote-tokens/{token}/vote`
  (`VoteTokenController.java:62`), al que el navegador llega por
  `frontend/src/app/api/votar/[token]/route.ts`, que devuelve el cuerpo del
  backend tal cual. El puente `/api/backend` (`src/lib/puente.ts`) no
  interviene.
- **Otros usos:** `formatearEspera`, `segundosRestantes`, `vote.window.open` y
  `vote.daily.used` solo aparecen en `boton-votar.tsx`. `BotonVotar` tiene un
  único consumidor: `frontend/src/app/votar/[token]/page.tsx:47`.
- **Vitest:** `frontend/vitest.config.ts`, entorno `node`, sin jsdom,
  `include: ["src/**/*.test.ts"]`. Un `.test.tsx` no se ejecutaría.
- **Paradas:** ninguna. No cambia `/api/v1/plugin/*`, no hay migración, no toca
  el ledger, no agrega dependencias.
- **ADR que aplican:**
  - [ADR-0031](../../docs/decisiones/0031-vitest-en-el-frontend.md): pruebas
    solo de lógica sin interfaz, en `src/**/*.test.ts`, junto al archivo; los
    componentes no se prueban. Por eso la lógica sale a `src/lib/`.
  - [ADR-0006](../../docs/decisiones/0006-openapi-fuente-de-verdad.md): errores
    en Problem Details (RFC 9457). No fija si el código estable es `type` o
    `title`; el Javadoc de `ManejadorDeErrores` dice que es `type`, pero
    `crear(...)` pone el mismo código en los dos (ver P-3).
  - [ADR-0016](../../docs/decisiones/0016-token-de-voto.md): el nick viene en
    el token; no cambia nada aquí.
- **Origen del texto falso:** `problema.segundosRestantes ?? 0`
  (`boton-votar.tsx:47`) convierte un dato ausente en «menos de un minuto».

## Decisiones técnicas

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| `formatearEspera(segundos: unknown): string \| null` en `src/lib/espera.ts`; `null` si no es un número finito | Recibir `number` y que el componente valide antes | Con `unknown`, CA-6, CA-9 y CA-10 se resuelven en un solo sitio con prueba; el componente deja de inventar un `0` |
| Redondeo **hacia arriba al minuto** (`Math.ceil(segundos / 60)`) y luego horas y minutos; de 0 exclusive a 59,9 s → «1 min»; 0 y negativos → «menos de un minuto» | Truncar (`Math.floor`), como hacía el componente | Decisión del desarrollador (P-4): es peor decirle que vuelva antes de tiempo que un minuto después. Con el truncado, 119 s decían «1 min» |
| Horas exactas sin «0 min» («1 h») | Mantener «1 h 0 min» | Lo pidió el desarrollador como borde («exactamente una hora») y se lee mejor |
| Sin días: 90000 s → «25 h» | «1 d 1 h» | La espera diaria no pasa de 24 h; un formato de días sería código sin uso |
| **Clasificador puro** `clasificarRechazo(problema: unknown)` en `src/lib/rechazo-de-voto.ts`, que devuelve `{ tipo: "voto-diario", espera }`, `{ tipo: "ventana-servidor", espera }` u `{ tipo: "otro", mensaje }` | Dejar el `if` en el componente | Sin él, CA-7, CA-9, CA-10 y CA-11 solo se verifican a ojo (ADR-0031 no prueba componentes). Aprobado por el desarrollador (P-1) |
| Se compara `title`, como hoy | Pasar a `type` (`https://latinosmc.com/errores/<código>`) | Hoy son equivalentes y `title` es el cambio mínimo; pasar a `type` queda fuera de alcance como posible mejora (P-3) |
| Los textos visibles se quedan en `boton-votar.tsx`; el clasificador solo decide el caso y formatea el tiempo | Textos en `src/lib/` | Los textos son interfaz; la lógica, lo que se prueba |
| El aviso de voto diario es un bloque propio en `boton-votar.tsx` con `role="status"` y los tokens del bloque de espera (`border-border`, `bg-surface-sunken`, `text-muted`) | Reutilizar `Aviso` de `ui.tsx` | `Aviso` solo pone `role="alert"` en el tono error y no acepta `role`; ampliarlo toca `ui.tsx` sin necesidad (P-5) |
| `mensaje` del caso «otro»: el `detail` si es una cadena no vacía; si no, «No se pudo registrar tu voto. Inténtalo otra vez.» | Pintar `detail` sea lo que sea | Hoy un `detail` que no es cadena se pintaría tal cual; el texto de respaldo es el actual |
| El fallo de red sigue en el `catch` del componente | Meterlo en el clasificador | No hay cuerpo que clasificar; el texto actual no cambia (CA-11) |

Textos exactos (CA-7 a CA-10):

| Caso | Título | Cuerpo |
|---|---|---|
| `vote.daily.used` con tiempo | «Ya usaste tu voto de hoy» | «Solo se puede votar a un servidor al día.» + «Podrás volver a votar en {espera}.» |
| `vote.daily.used` sin tiempo | «Ya usaste tu voto de hoy» | «Solo se puede votar a un servidor al día.» |
| `vote.window.open` con tiempo | «Ya votaste hace poco» | «Puedes volver a votar en {espera}.» (como hoy) |
| `vote.window.open` sin tiempo | «Ya votaste hace poco» | (sin frase de tiempo) |

## Archivos que se tocan

| Archivo | Cambio | Agente |
|---|---|---|
| `frontend/src/lib/espera.test.ts` | nuevo: CA-1 a CA-6 | `pruebas-frontend` |
| `frontend/src/lib/rechazo-de-voto.test.ts` | nuevo: parte lógica de CA-7, CA-9, CA-10, CA-11 | `pruebas-frontend` |
| `frontend/src/lib/espera.ts` | nuevo: `formatearEspera` | `dev-frontend` |
| `frontend/src/lib/rechazo-de-voto.ts` | nuevo: `clasificarRechazo` | `dev-frontend` |
| `frontend/src/components/boton-votar.tsx` | modifica: usa `clasificarRechazo`, fase nueva para el voto diario, quita su `formatearEspera` y el `?? 0` | `dev-frontend` |
| `specs/002-espera-voto-diario/*` | spec, plan, tareas, pr | `orquestador` |

No se tocan: `src/lib/puente.ts`, `src/app/api/votar/[token]/route.ts`,
`src/lib/api.ts`, `src/lib/tipos.ts`, `src/components/ui.tsx`, `package.json`
ni nada de `backend/`.

**El rojo es por módulo que no existe.** `pruebas-frontend` solo puede editar
`*.test.ts`, así que no puede dejar un `espera.ts` vacío. El rojo será «no se
encuentra el módulo `./espera`» (y `./rechazo-de-voto`), como se hizo con
`puente.test.ts`; el desarrollador lo aceptó (P-6). `pruebas-frontend` deja
copiado el motivo del fallo en su informe y se pega en «Evidencia».

## Matriz criterio → prueba

| Criterio | Prueba | Tipo |
|---|---|---|
| CA-1 | `espera.test.ts › horas y minutos redondeando hacia arriba` (`it.each`: 3601, 24120, 24061, 24060, 86340) | unitaria |
| CA-2 | `espera.test.ts › horas exactas sin «0 min»` (3600, 3541, 7200, 86399, 90000) | unitaria |
| CA-3 | `espera.test.ts › solo minutos redondeando hacia arriba` (60, 61, 119, 120, 720, 3540) | unitaria |
| CA-4 | `espera.test.ts › menos de un minuto positivo da «1 min»` (0.5, 1, 59, 59.9) | unitaria |
| CA-5 | `espera.test.ts › cero y negativos` (0, -0.5, -1, -3600 → «menos de un minuto»; sin «-») | unitaria |
| CA-6 | `espera.test.ts › no es un número finito` (`undefined`, `null`, `NaN`, `Infinity`, `-Infinity`, `"3600"`, `{}` → `null`) | unitaria |
| CA-7 | `rechazo-de-voto.test.ts › voto diario con tiempo` (`{ tipo: "voto-diario", espera: "6 h 42 min" }`) + textos y ausencia de «hace poco» / `detail` en pantalla | unitaria + verificación visual |
| CA-8 | `verificador-ui`: aviso sin `text-danger` ni `role="alert"`, con `role="status"`, sin botón; móvil y escritorio, claro y oscuro | verificación visual |
| CA-9 | `rechazo-de-voto.test.ts › voto diario sin tiempo` (ausente, `null`, `"abc"` → `espera: null`) + pantalla sin frase de tiempo | unitaria + verificación visual |
| CA-10 | `rechazo-de-voto.test.ts › ventana por servidor` (con 3600 → `"1 h"`; sin dato → `null`) + pantalla «Ya votaste hace poco» | unitaria + verificación visual |
| CA-11 | `rechazo-de-voto.test.ts › otros rechazos` (otro `title`, `null`, array, cadena, `detail` no cadena → texto de respaldo) + fallo de red en pantalla | unitaria + verificación visual |
| CA-12 | `git diff --stat` de la rama contra su base + `npm run lint`, `npm run test`, `npm run build` en verde (revisor) | revisión |

Para la verificación visual, `verificador-ui` necesita provocar las respuestas
`409`: con el backend en marcha y dos votos del mismo jugador (diario y por
servidor), o interceptando `/api/votar/*` en el navegador. Lo decide él y lo
documenta en su informe.

## Riesgos y cómo se comprueban

- **Se rompe el voto normal (200).** Lo que llega a `main` llega a producción.
  El camino feliz no pasa por el clasificador; `verificador-ui` comprueba
  también «¡Voto registrado!».
- **El backend cambia `title`.** El aviso cae en «otro» y se ve el `detail` en
  rojo, como hoy: degradación sin mentir. La prueba del clasificador fija los
  dos códigos.
- **`next build` o `next dev` reescriben `frontend/AGENTS.md` o
  `next-env.d.ts`.** El revisor comprueba en el diff (CA-12) que solo cambian
  los archivos de la tabla.
- **El estado `espera` cambia de forma** (`segundos: number` pasa a
  `espera: string | null`). `npm run build` revisa los tipos.

## Preguntas para el desarrollador

**Respondidas el 13/11/2026:** P-1 sí (clasificador); P-2 textos aprobados;
P-3 se sigue comparando `title` (pasar a `type`, fuera de alcance); P-4
redondeo hacia arriba al minuto (CA-1 a CA-5 reescritos; el borde de 1 a 59 s
da «1 min»); P-5 bloque propio, sin tocar `ui.tsx`; P-6 vale el rojo por
módulo inexistente, con el motivo copiado.

- **P-1 (alcance).** ¿Se crea el clasificador `clasificarRechazo` en
  `src/lib/rechazo-de-voto.ts`? Es más de lo pedido, pero sin él CA-7, CA-9,
  CA-10 y CA-11 solo tienen verificación visual. Si no, esas filas de la matriz
  quedan solo como «verificación visual».
- **P-2 (texto).** ¿Aprueba los textos de la tabla de arriba, en especial
  «Ya usaste tu voto de hoy» / «Solo se puede votar a un servidor al día.» /
  «Podrás volver a votar en …»?
- **P-3 (contrato).** ¿Se sigue comparando `title` o se pasa a `type`, que el
  Javadoc de `ManejadorDeErrores` llama «el contrato»? Hoy llevan el mismo
  código.
- **P-4 (redondeo).** Truncar hace que 119 s se muestren como «1 min»: el
  jugador puede volver antes de tiempo y recibir otra vez el aviso. ¿Se
  mantiene el truncado actual (CA-1, CA-3) o se redondea hacia arriba?
- **P-5 (componente).** ¿Bloque propio con `role="status"` (propuesto) o se
  amplía `Aviso` de `ui.tsx` con una prop `role`?
- **P-6 (rojo).** ¿Vale un rojo por «módulo no encontrado», o prefiere que
  `dev-frontend` deje antes los módulos vacíos para que el rojo sea por
  aserción?
