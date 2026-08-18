# ADR-0016 — El jugador nunca escribe su nick: token de un solo uso

- **Estado:** 🟡 Propuesto — corrige el flujo descrito por el usuario
- **Fecha:** 2026-08-04

## Contexto

El flujo propuesto era:

```
/vote en el servidor → el jugador recibe una URL → la web le PREGUNTA el nick
→ vota → recibe la recompensa
```

El paso «la web le pregunta el nick» abre el agujero más grande del sistema.

### Por qué es grave

Si el nick lo escribe el visitante, **cualquiera puede votar en nombre de
cualquiera, sin haber pisado nunca ese servidor**. Consecuencias directas:

| Ataque | Coste para el atacante |
|---|---|
| Inflar el ranking de mi servidor inventando nicks | Escribir texto |
| Gastarle la ventana de voto a un jugador legítimo | Escribir su nick antes que él |
| Farmear recompensas con nombres al azar | Un bucle de diez líneas |

El antifraude por IP mitiga el volumen, pero no arregla el fondo: **la
plataforma no tiene ninguna prueba de que quien vota sea quien dice ser.** Y el
producto entero es la credibilidad del ranking.

## Decisión

El comando `/vote` genera un **token de un solo uso** ligado a ese jugador y a
ese servidor. La web no pregunta el nick: ya lo sabe.

```
Jugador escribe /vote
   ↓
El plugin pide a la plataforma un token para (servidor, jugador)
   ↓
La plataforma devuelve un token corto y opaco, válido 10 minutos, un solo uso
   ↓
El jugador recibe un enlace pinchable en el chat
   ↓
La web abre ya con el nick resuelto: solo queda pulsar "Votar"
   ↓
Se consume el token, se registra el voto, la recompensa se entrega
```

### Propiedades

| | |
|---|---|
| **Un solo uso** | Consumido en la primera votación; un enlace reenviado no sirve |
| **Caducidad de 10 min** | Tiempo de sobra para alt-tab; ventana corta si alguien lo intercepta |
| **Ligado a servidor + jugador** | Un token de un servidor no vale en otro |
| **Opaco** | No contiene el nick: no se puede fabricar uno adivinando |
| **Emitido por el plugin autenticado** | Solo un servidor con clave válida puede pedirlo |

### La identidad queda probada por construcción

Para conseguir un token hay que **estar dentro del servidor**. Eso es
exactamente la prueba que faltaba, y sale gratis: el jugador ya estaba ahí.

## Beneficio inesperado: menos pasos, no más

Se elimina un formulario. El flujo pasa de:

```
abrir web → escribir nick → resolver captcha → votar
```

a:

```
abrir web → votar
```

La medida de seguridad **mejora** la conversión en lugar de empeorarla, que es
lo contrario de lo habitual.

## Votar sin estar en el servidor

Un visitante que descubre un servidor navegando por el directorio también debe
poder votar. Se mantienen dos caminos con distinto nivel de confianza:

| Camino | Identidad | Fricción | Tratamiento antifraude |
|---|---|---|---|
| **Con token** (desde `/vote`) | Probada | Ninguna | Riesgo base bajo |
| **Sin token** (desde la web) | Declarada | Escribir el nick + captcha | Penalización de riesgo; sujeto a todas las reglas |

Los votos con token deberían ser la inmensa mayoría, y son los que sostienen la
credibilidad del ranking.

## Consecuencias

- Nueva tabla `vote_tokens`: token hasheado, servidor, identidad, caducidad,
  instante de consumo
- Nuevo endpoint en la API del plugin para emitir tokens (sujeto al límite de
  tráfico por clave)
- El comando `/vote` es la primera funcionalidad del plugin visible para el
  jugador; el mensaje debe ser un enlace pinchable
- El token se guarda **hasheado**, igual que las claves de API: si la base se
  filtra, los tokens vivos no son utilizables

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Confiar en el nick escrito | El agujero descrito arriba |
| Verificar con la API de Mojang | No funciona en offline-mode, que es buena parte del mercado |
| Código numérico que el jugador copia a mano | Misma seguridad, mucha más fricción |
| Vincular cuenta de la web con cuenta de Minecraft | Correcto a largo plazo, pero exige el módulo de cuentas y frena la adopción |
