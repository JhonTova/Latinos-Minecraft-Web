# ADR-0022 — La identidad del jugador viaja con su tipo, también dentro del plugin

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-15
- **Decide:** ingeniería
- **Relacionado:** [ADR-0009 — El plugin no es fuente de verdad](0009-plugin-no-es-fuente-de-verdad.md),
  [ADR-0016 — Token de voto](0016-token-de-voto.md)

---

## Contexto

La plataforma modela la identidad del votante como un par `(tipo, valor)`:
`MOJANG_UUID` con un UUID en servidores premium, `OFFLINE_NAME` con un nombre en
los de offline-mode. Está así desde el primer día y por buenas razones: las dos
formas merecen reglas antifraude distintas.

**El plugin se quedaba solo con el valor y tiraba el tipo.** Después buscaba al
jugador por nombre:

```java
Player jugador = Bukkit.getPlayerExact(recompensa.identidadValor());
```

En un servidor premium ese valor es un UUID. Ningún jugador se llama
`f81d4fae-7dec-11d0-a765-00a0c91e6bf6`, así que la búsqueda devolvía `null`
**siempre**. El plugin concluía que el jugador estaba desconectado, guardaba la
recompensa como pendiente bajo la clave del UUID, y **le confirmaba la entrega a
la plataforma**. Al entrar el jugador, la deuda se buscaba por nombre y tampoco
coincidía nunca.

Resultado: en un servidor premium —la mayoría— **no se entregaba ni una sola
recompensa**, y la plataforma daba todas por entregadas.

El fallo no producía ningún error. El código "funcionaba": decidía que nadie
estaba conectado. Es la peor clase de fallo que existe en este bucle, porque
rompe exactamente lo que el producto promete y no deja rastro de que se rompió.

Los tests no lo vieron porque todos usaban nombres. El plugin nunca se probó
contra la forma en que un servidor premium identifica a la gente.

## Decisión

**El tipo de identidad viaja junto al valor por todo el plugin**, en un
`IdentidadDeJugador` que sabe dos cosas:

1. **Cómo buscar al jugador conectado.** `Bukkit.getPlayer(UUID)` para
   `MOJANG_UUID`, `getPlayerExact(nombre)` para `OFFLINE_NAME`.
2. **Con qué clave se guarda en disco**, con el tipo por delante
   (`MOJANG_UUID:f81d4fae-…`), para que un UUID y un nombre no puedan colisionar.

Y al entrar un jugador **se consultan sus dos identidades**, no una. El dueño
puede haber cambiado el servidor de premium a offline-mode entre el voto y la
vuelta del jugador; perder la recompensa por un cambio de configuración ajeno
sería un fallo nuestro.

### La migración no es opcional

Los servidores que ya tienen el plugin instalado guardan claves sin tipo. Al
cargar se deduce por la forma del valor: lo que parece un UUID se recupera como
`MOJANG_UUID`, el resto como `OFFLINE_NAME`.

Sin eso, la actualización que arregla el fallo **dejaría atrapado justo lo que el
fallo acumuló**: todas las recompensas de todos los votos que un servidor premium
recibió mientras tanto.

### Lo que no se pudo entregar vuelve a la deuda

Si al entregar falla el tipo de recompensa —no está en `config.yml`— la
recompensa vuelve a la lista de pendientes en lugar de descartarse. Ya se
confirmó a la plataforma, así que tirarla aquí la perdería para siempre.

Y la deuda deja de crecer sin límite: lo que lleve más de 90 días esperando a
alguien que no vuelve se descarta, y queda escrito en el log.

## Consecuencias

**A favor**

- Las recompensas llegan en servidores premium. Antes, ninguna.
- Un cambio de online-mode a offline-mode deja de perder recompensas.
- El archivo `entregas.yml` tiene un tamaño acotado.

**En contra**

- El formato de `entregas.yml` cambia. Se migra al cargar, pero un servidor que
  volviera a una versión anterior del plugin no entendería las claves nuevas.
  Aceptable: la versión anterior no entrega recompensas en premium de todas
  formas.

## Cómo se sabe que funciona

`AlmacenDeEntregasTest`, 12 casos. Los que cierran este fallo concretamente:

- **UUID y nombre son cajones distintos** — no se mezclan.
- **Al entrar se recoge la deuda esté guardada por UUID o por nombre** — el caso
  que no existía.
- **Las claves antiguas con forma de UUID se recuperan como UUID** — que la
  actualización devuelva lo que el fallo dejó atrapado.
- **Lo que no se pudo entregar vuelve a la deuda.**
- **Una recompensa que lleva años esperando se descarta.**

Lo que estos tests **no** cubren: la búsqueda del jugador (`Bukkit.getPlayer`)
necesita un servidor arrancado. La lógica de qué buscar está aislada en
`IdentidadDeJugador` y revisada; la llamada a Bukkit, no. Se comprueba a mano
con el paso «Voto» de la lista de `LANZAMIENTO.md`, **contra un servidor en
online-mode**, que es donde estaba el fallo.
