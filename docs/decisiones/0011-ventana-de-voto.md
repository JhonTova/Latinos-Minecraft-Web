# ADR-0011 — Ventana de voto deslizante de 24 h en UTC

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Contexto

El documento 13 pedía ventanas configurables de 12/24/48 h, "timezone aware",
con horario local y UTC. Esa combinación es la mayor fuente de incidencias y
reclamaciones del sector ("¿por qué no puedo votar todavía?").

Dos estrategias posibles:

| | Reinicio a medianoche | **Ventana deslizante** |
|---|---|---|
| Cambio de horario de verano | 🔴 Días de 23 y 25 h | 🟢 No aplica |
| Zona horaria del jugador vs. del servidor | 🔴 Ambigua | 🟢 Irrelevante |
| Carga | 🔴 **Pico masivo a las 00:00** | 🟢 Distribuida |
| Cálculo | Calendario + zona horaria | Una resta de instantes |

## Decisión

**Ventana deslizante:** un jugador puede volver a votar por un servidor cuando
hayan pasado N horas desde su último voto **aceptado** a ese servidor.

- Todos los instantes se almacenan y comparan en **UTC** (`timestamptz`)
- Duración por defecto: **24 horas**, configurable por servidor dentro de
  límites definidos por la plataforma
- La ventana se cuenta desde el voto **aceptado**, no desde el intento
- En la interfaz se muestra **siempre una cuenta atrás** ("puedes volver a
  votar en 6 h 42 min"), nunca una fecha absoluta

## Justificación principal

El argumento decisivo no es la corrección horaria, sino la **carga**: con
reinicio a medianoche todos los jugadores elegibles llegan en el mismo
instante. Es un pico de tráfico autoinfligido y evitable — y coincide con el
momento en que también se concentra el fraude automatizado.

## Consecuencias

- No existe concepto de "día natural" en el dominio de votos
- Un voto rechazado por antifraude **no** consume la ventana
- La configuración por servidor se limita a un rango (p. ej. 12–48 h) para
  impedir que un servidor fije 1 hora e infle su ranking
- El cálculo de elegibilidad es un simple `ultimo_voto_aceptado + ventana <= now()`,
  indexable y barato
