Solo frontend: no toca `/api/v1/plugin/*`, ni migraciones, ni el ledger, ni agrega dependencias. **Falta la verificación visual: hay que ver la pantalla antes de fusionar** (ver abajo).

## Qué cambia y por qué

Cuando un jugador ya usó su voto del día en otro servidor, el backend responde
`409` con `title: "vote.daily.used"` y `segundosRestantes`. La web solo
reconocía `vote.window.open`, así que este caso caía en el error genérico:
texto en rojo, botón activo y ningún dato de cuánto falta.

Ahora:

- **Aviso neutro para el voto diario** (`boton-votar.tsx`): «Ya usaste tu voto
  de hoy», «Solo se puede votar a un servidor al día.» y «Podrás volver a
  votar en 6 h 42 min.». Va con `role="status"`, no en rojo y sin el botón. No
  dice «ya votaste a este servidor», porque no es lo que pasó.
- **`formatearEspera`** sale del componente a `src/lib/espera.ts`, como función
  pura. Redondea **hacia arriba al minuto** (decisión del desarrollador): es
  peor decirle al jugador que vuelva antes de tiempo que un minuto después.
  - 119 s → «2 min».
  - 3600 s → «1 h».
  - 3601 s → «1 h 1 min».
  - 1 a 59 s → «1 min».
  - 0 y negativos → «menos de un minuto».
  - Un valor que no es un número finito → `null`.
- **`clasificarRechazo`** (`src/lib/rechazo-de-voto.ts`): decide si el cuerpo
  del rechazo es voto diario, espera por servidor u otro error. Como los
  componentes no tienen pruebas (ADR-0031), esta función es lo que permite
  probar los criterios de pantalla.
- **Arreglo de paso:** si `segundosRestantes` no llegaba, el componente hacía
  `?? 0` y decía «menos de un minuto», que es falso. Ahora la frase del tiempo
  simplemente no aparece.
- El aviso «Ya votaste hace poco» también lleva ahora `role="status"`. No
  estaba en la spec y está anotado en «Cambios durante la implementación».

Se sigue comparando `title`, como hasta ahora. Pasar a `type` (que el Javadoc
de `ManejadorDeErrores` llama «el contrato») queda fuera de alcance como
posible mejora.

Spec: [`specs/002-espera-voto-diario/spec.md`](spec.md) · Plan:
[`plan.md`](plan.md) · Tareas: [`tareas.md`](tareas.md)

## Matriz criterio → prueba

| Criterio | Prueba | Tipo |
|---|---|---|
| CA-1 | `espera.test.ts` › CA-1 (3601, 24120, 24061, 24060, 86340) | unitaria |
| CA-2 | `espera.test.ts` › CA-2 (3600, 3541, 7200, 86399, 90000) | unitaria |
| CA-3 | `espera.test.ts` › CA-3 (60, 61, 119, 120, 720, 3540) | unitaria |
| CA-4 | `espera.test.ts` › CA-4 (0.5, 1, 59, 59.9 → «1 min») | unitaria |
| CA-5 | `espera.test.ts` › CA-5 (0, −0, −0.5, −1, −3600) | unitaria |
| CA-6 | `espera.test.ts` › CA-6 (`undefined`, `null`, `NaN`, ±`Infinity`, `"3600"`, `{}` → `null`) | unitaria |
| CA-7 | `rechazo-de-voto.test.ts` › CA-7 (objeto completo, sin `detail`) + textos de `boton-votar.tsx` | unitaria + revisión de código; **visual pendiente** |
| CA-8 | `role="status"`, sin `text-danger` ni `role="alert"`, sin botón | revisión de código; **visual pendiente** |
| CA-9 | `rechazo-de-voto.test.ts` › CA-9 (ausente, `null`, `"abc"`) | unitaria; **visual pendiente** |
| CA-10 | `rechazo-de-voto.test.ts` › CA-10 (3600 → «1 h»; sin dato o `"3600"` → `null`) | unitaria; **visual pendiente** |
| CA-11 | `rechazo-de-voto.test.ts` › CA-11 (otro `title`, mayúsculas, solo `type`, cuerpos y `detail` no válidos) | unitaria; **visual pendiente** |
| CA-12 | `git log --stat a4a381d..HEAD`: solo `frontend/src/` y `specs/002-…/`; lint, test y build en verde | revisión |

## Evidencia

- **Rojo** (commit `test(frontend)`, antes del `feat`). Salida de `npm run test`:
  `Error: Cannot find module './espera'` y
  `Error: Cannot find module './rechazo-de-voto'`. El resumen fue
  `Test Files 2 failed | 1 passed (3)`. Los módulos no existían todavía, el
  mismo tipo de rojo que con `puente.test.ts`.
- **Verde.** `dev-frontend` ejecutó los tres comandos:
  - `npm run lint`: sin errores ni avisos.
  - `npm run test`: `Test Files 3 passed (3)`, `Tests 110 passed (110)`.
  - `npm run build`: Next.js 16.3.1 compila y TypeScript pasa.
- **Revisión** (`revisor`). Volvió a ejecutar `lint` y `test` en verde. No hay
  ningún hallazgo alto ni pruebas débiles: las pruebas distinguen `ceil` de
  `round` y de `floor`. La mutación no aplica, porque es frontend (ADR-0032).
- **Visual: no se hizo.** El `verificador-ui` no tenía navegador en esta
  sesión. Además, `/votar/[token]` consulta el backend desde el servidor antes
  de pintar el botón, y el backend no estaba levantado.

## Antes de fusionar

Con el backend en marcha, abre `/votar/<token>` en móvil (≈375 px) y en
escritorio, en tema claro y oscuro, y comprueba:

1. El voto correcto sigue mostrando «¡Voto registrado!».
2. Con el voto del día ya usado en otro servidor, aparece «Ya usaste tu voto de
   hoy» con el tiempo, sin texto rojo y sin el botón.
3. Al volver a votar al mismo servidor aparece «Ya votaste hace poco» con el
   tiempo.
4. Cualquier otro rechazo sale en rojo y deja el botón disponible.
5. La consola queda limpia y el contraste se lee bien en los dos temas.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
