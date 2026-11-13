# Spec 002 — Decir cuánto falta cuando el voto del día ya se usó

- **Estado:** Implementada, pendiente de la verificación visual y de la fusión (aprobada el 13/11/2026, con los ajustes de las respuestas P-1 a P-6 del plan)
- **Rama:** `feat/002-espera-voto-diario`
- **Parte:** frontend
- **Riesgo:** 🟢 ligero (interfaz y textos; no toca el motor de votos ni el antifraude)

## Problema

Un jugador solo puede votar a un servidor al día. Cuando ya votó hoy a otro
servidor e intenta votar a este, el backend responde `409` con
`title: "vote.daily.used"`, `detail: "Ya has votado hoy. Solo se puede votar a
un servidor al dia."` y la propiedad `segundosRestantes`
(`ManejadorDeErrores.votoDiarioConsumido`, en
`backend/app/src/main/java/com/latinosmc/app/web/ManejadorDeErrores.java`).

`frontend/src/components/boton-votar.tsx` solo reconoce `vote.window.open`.
Para `vote.daily.used` cae en el error genérico: pinta el `detail` en rojo,
como si algo hubiera fallado, deja el botón de votar activo y **no dice cuándo
podrá volver a votar**, aunque el dato llegó.

Caso concreto: Steve vota a las 9:00 a «Servidor A». A las 17:18 entra a votar
a «Servidor B» y ve en rojo «Ya has votado hoy…». No sabe si es un fallo, vuelve
a pulsar, recibe lo mismo, y no sabe que podrá votar dentro de 6 h 42 min. Un
mensaje que parece un error del sitio resta confianza en la plataforma, y el
producto real es la credibilidad.

Además, el formateo del tiempo («6 h 42 min») vive dentro del componente, y los
componentes no tienen pruebas (ADR-0031): hoy nadie comprueba sus bordes. Por
ejemplo, si `segundosRestantes` no llega, el componente muestra «menos de un
minuto», que es falso.

## Qué se construye

Cuando el voto del día ya se usó, el jugador ve un aviso neutro (no un error)
que dice la verdad —ya usó su voto de hoy, no que ya votó a este servidor— y
cuánto falta para poder votar de nuevo. El formateo del tiempo de espera pasa a
una función pura en `frontend/src/lib/`, con sus pruebas de Vitest.

## Criterios de aceptación

Notación: `f(x)` es la función pura de formateo del tiempo de espera aplicada a
`x` segundos.

**Regla de redondeo:** todo valor positivo se redondea **hacia arriba al minuto**
(`M = ⌈x / 60⌉`) y luego se parte en horas y minutos. Es peor decirle al jugador
que vuelva antes de tiempo —y que reciba otra vez el rechazo— que un minuto
después.

| ID | Dado / cuando / entonces |
|---|---|
| CA-1 | Dado un número de segundos cuyo redondeo hacia arriba da 61 min o más y no es una hora exacta, cuando se formatea, entonces devuelve `«H h M min»`: `f(3601)` = «1 h 1 min», `f(24120)` = «6 h 42 min», `f(24061)` = «6 h 42 min», `f(24060)` = «6 h 41 min», `f(86340)` = «23 h 59 min». |
| CA-2 | Dado un número de segundos cuyo redondeo hacia arriba da horas exactas, cuando se formatea, entonces devuelve `«H h»` sin «0 min»: `f(3600)` = «1 h», `f(3541)` = «1 h», `f(7200)` = «2 h», `f(86399)` = «24 h»; sin días: `f(90000)` = «25 h». |
| CA-3 | Dado un número de segundos entre 60 y 3540, cuando se formatea, entonces devuelve `«M min»` redondeando hacia arriba: `f(60)` = «1 min», `f(61)` = «2 min», `f(119)` = «2 min», `f(120)` = «2 min», `f(720)` = «12 min», `f(3540)` = «59 min». |
| CA-4 | Dado un número de segundos mayor que 0 y menor que 60 (incluidas fracciones como 0,5 y 59,9), cuando se formatea, entonces devuelve «1 min»: `f(0.5)`, `f(1)`, `f(59)`, `f(59.9)`. Es el borde de la regla de redondeo: el jugador no vuelve antes de tiempo. |
| CA-5 | Dado 0 o un número negativo (`0`, `-0.5`, `-1`, `-3600`), cuando se formatea, entonces devuelve «menos de un minuto»: la espera ya terminó o está por terminar, y nunca sale un texto con un signo menos ni con cifras negativas. |
| CA-6 | Dado un valor que no es un número finito (`undefined`, `null`, `NaN`, `Infinity`, `"3600"`, `{}`), cuando se formatea, entonces la función indica que no hay tiempo que mostrar (no devuelve un texto inventado como «menos de un minuto» ni «NaN h»). |
| CA-7 | Dada una respuesta `409` con `title: "vote.daily.used"` y `segundosRestantes: 24120`, cuando el jugador pulsa votar, entonces ve el aviso «Ya usaste tu voto de hoy», la frase «Solo se puede votar a un servidor al día.» y «Podrás volver a votar en 6 h 42 min.»; el texto no dice «ya votaste a este servidor» ni «hace poco», y no muestra el `detail` del backend. |
| CA-8 | Dada la respuesta de CA-7, cuando se muestra el aviso, entonces no se pinta como error (no usa `text-danger` ni `role="alert"`), el botón de votar desaparece como en la espera por servidor, y el aviso se anuncia a los lectores de pantalla (`role="status"`). |
| CA-9 | Dada una respuesta `409` con `title: "vote.daily.used"` sin `segundosRestantes` o con un valor no válido, cuando el jugador pulsa votar, entonces ve el mismo aviso de CA-7 sin la frase del tiempo (no muestra «menos de un minuto» ni «NaN»). |
| CA-10 | Dada una respuesta `409` con `title: "vote.window.open"` y `segundosRestantes`, cuando el jugador pulsa votar, entonces sigue viendo «Ya votaste hace poco» y «Puedes volver a votar en …», ahora formateado con la función de `src/lib/` (con `segundosRestantes: 3600`, «Puedes volver a votar en 1 h.»); si falta `segundosRestantes`, la frase del tiempo no se muestra. |
| CA-11 | Dado cualquier otro error (otro `title`, un cuerpo que no es JSON o un fallo de red), cuando el jugador pulsa votar, entonces el comportamiento es el de hoy: el mensaje en rojo con `role="alert"` y el botón disponible. |
| CA-12 | Dado el diff de la rama, entonces solo cambia `frontend/src/` y `specs/002-espera-voto-diario/`: ningún endpoint, ni `src/lib/puente.ts`, ni `src/app/api/votar/[token]/route.ts`, ni `package.json`; y `npm run lint`, `npm run test` y `npm run build` pasan. |

## Fuera de alcance

- Cambiar el backend: los textos del `detail`, los códigos, el cálculo de
  `segundosRestantes` o el contrato `/api/v1/plugin/*`.
- Una cuenta atrás que se actualice sola en pantalla: el tiempo se calcula una
  vez, al recibir la respuesta.
- Mostrar a qué servidor votó hoy el jugador (el backend no lo manda).
- Los demás rechazos con mensaje propio (`CupoDeIpAgotado`, antifraude): siguen
  en el error genérico.
- Días en el formato («1 d 2 h»): la espera diaria nunca pasa de 24 h; un valor
  mayor se muestra en horas.
- Tocar el puente `/api/backend` o la ruta `/api/votar/[token]`.
- Tocar `src/components/ui.tsx` (el aviso es un bloque propio de
  `boton-votar.tsx`).
- **Posible mejora: comparar `type` en vez de `title`.** El Javadoc de
  `ManejadorDeErrores` llama a `type` «el contrato»; hoy `type` y `title`
  llevan el mismo código. Se sigue comparando `title`, como hasta ahora; pasar
  a `type` es otro cambio, con su propia spec.

## Paradas

- [ ] Cambia el contrato `/api/v1/plugin/*`
- [ ] Necesita una migración Flyway
- [ ] Escribe en el ledger
- [ ] Agrega una dependencia

Ninguna aplica: solo cambia `frontend/src/` y Vitest ya es dependencia del
proyecto (ADR-0031).

## Decisiones del desarrollador

Las preguntas P-1 a P-6 de [plan.md](plan.md) se respondieron el 13/11/2026.
La que cambió criterios es P-4: redondeo hacia arriba al minuto (CA-1 a CA-5
se reescribieron con esa regla antes de aprobar la spec).

## Cambios durante la implementación

- **`role="status"` también en «Ya votaste hace poco».** CA-8 solo lo pide
  para el aviso del voto diario, pero el orquestador se lo encargó a
  `dev-frontend` también para el bloque de la espera por servidor, para que los
  dos avisos se anuncien igual a los lectores de pantalla. No estaba en la spec
  ni en el plan; lo detectó el `revisor` (hallazgo bajo). No cambia ningún
  texto ni ningún otro criterio. Queda pendiente que el desarrollador confirme
  si lo conserva.
