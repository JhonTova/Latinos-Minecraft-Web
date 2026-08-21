# ADR-0029 — Se cierran las tres decisiones abiertas: offline-mode, marca y alcance

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-21
- **Decide:** el propietario del producto
- **Reemplaza:** las secciones 4.1, 4.2 y 4.3 de `docs/DECISIONES.md`
- **Relacionado:** [ADR-0005](0005-antifraude-core.md), [ADR-0022](0022-identidad-del-jugador-en-el-plugin.md),
  [ADR-0009](0009-plugin-no-es-fuente-de-verdad.md)

## Contexto

Tres decisiones llevaban abiertas desde el principio y bloqueaban diseño. Se
trabajaba con supuestos. Ya están decididas, y los tres supuestos se confirman.

## Decisión 1 — Se aceptan servidores offline-mode y premium

**Ambos.** Un directorio hispanohablante que solo admita servidores premium
deja fuera a una parte enorme del mercado al que se dirige.

El supuesto de trabajo se confirma tal cual: la identidad se modela como
`(tipo, valor)` y no como UUID de Mojang, y el antifraude aplica reglas
distintas por tipo. Eso ya está construido — `RuleBasedTrustEngine` suma **+25
de riesgo** a toda identidad no verificable— así que esta decisión no obliga a
cambiar el esquema ni el motor.

### Lo que esta decisión NO resuelve, y hay que decidir aparte

En offline-mode la identidad del votante es un nombre que cualquiera puede
escribir. No es que sea débil: **no existe**. Nada impide votar como `Pedro1`,
`Pedro2` y `Pedro3` desde el mismo teclado.

El +25 de riesgo acota el abuso pero no cambia el hecho de fondo: **un voto
offline y un voto premium no valen lo mismo, y hoy en el ranking pesan igual.**
El producto real de esta plataforma es la credibilidad del ranking (CLAUDE.md);
si un servidor offline puede escalar más fácil que uno premium, el ranking mide
qué tan fácil es falsificar votos, no dónde apetece jugar.

Las salidas razonables son tres, y la decisión es del propietario:

| Salida | A favor | En contra |
|---|---|---|
| **Rankings separados** (premium y offline, en pestañas) | Cada uno compite contra sus iguales. Nadie puede acusar de nada. | Parte el directorio en dos y hay que explicarlo |
| **Un ranking, votos offline con menos peso** | Un solo sitio, un solo número | Cualquier peso que se elija es arbitrario y habrá que defenderlo |
| **Un ranking, todos iguales** | Lo más simple | Es la que rompe la credibilidad si alguien abusa |

Queda **pendiente y registrado en `DECISIONES.md`**. La plataforma funciona hoy
con la tercera; el esquema soporta las tres sin migración, que era justo el
objetivo del supuesto de trabajo.

## Decisión 2 — La marca es LatinosMC, con la paleta morada actual

Nombre confirmado: **LatinosMC**.

La paleta provisional deja de ser provisional. Es un sistema en OKLCH construido
en torno al **tono 300** (morado-magenta), con los neutros teñidos del mismo tono
—por eso el gris del sitio no se ve gris de ordenador— y **oro** como secundario
con cuentagotas, para el primer puesto y poco más.

Se midió antes de confirmarla, no después. **El morado estaba bien**: los
enlaces dan 4,74:1 y el texto blanco sobre el botón primario 4,88:1, los dos por
encima del 4,5:1 que exige WCAG AA.

Lo que **no** estaba bien eran los colores de estado, y fallaban en el peor sitio
posible: texto pequeño (`text-xs`) dentro del componente `Aviso`.

| Color | Antes | Ahora |
|---|---|---|
| Aviso (`warning`) | **2,19:1** | 4,51:1 |
| Éxito (`success`) | **2,92:1** | 4,53:1 |
| Error (`danger`) | 4,28:1 | 4,50:1 |
| Texto terciario | 4,43:1 | 4,54:1 |

Los de estado pasan a **definirse por tema**. Un mismo valor no puede servir
para las dos cosas: los vivos se leen bien sobre fondo oscuro y se pierden sobre
blanco; los oscuros hacen lo contrario, y aplicados en modo oscuro caen a 3,3:1.
Con los dos temas separados, los ocho colores pasan AA.

El tono, la saturación y el carácter de la marca no se han tocado. Solo la
luminosidad, y solo donde había un fallo medido.

## Decisión 3 — La plataforma ES el sitio de votación; el plugin va en el pack

Se confirma la lectura A. **No se agregan votos de otros directorios.**

El jugador vota aquí, en LatinosMC, y el plugin entrega la recompensa en su
servidor. Los dos son el mismo producto: sin el plugin, un voto es un número que
no le llega a nadie; sin la plataforma, el plugin no tiene qué entregar.

Consecuencias de que sean un pack y no dos cosas:

- **El bucle está cerrado y es nuestro de punta a punta.** Se puede garantizar
  que un voto aceptado acaba en una recompensa, porque los dos extremos son
  nuestros. Eso es exactamente lo que el Outbox existe para asegurar (ADR-0011).
- **La instalación del plugin ES la prueba de propiedad del servidor.** Ya está
  aprovechado: la primera conexión autenticada activa el servidor, y eso ahorra
  un paso entero del embudo de alta.
- **No hay que confiar en votos de terceros.** Un voto agregado de otro
  directorio sería un dato que no medimos nosotros, con las mismas objeciones
  que el ADR-0009 le pone al plugin.

La columna `vote_source` se conserva. No es indecisión: distingue el voto hecho
desde la web del hecho por Votifier (ADR-0014), que ya existen los dos, y deja
la puerta abierta sin costar nada.

## Consecuencias

- `DECISIONES.md` deja de tener decisiones bloqueantes abiertas. Queda la 4.4
  (directrices de Mojang), que no bloquea la v1, y la nueva sobre cómo puntúan
  los votos offline.
- La marca deja de ser un marcador de posición: `LatinosMC` y el morado son
  definitivos y se pueden usar en material que no sea fácil de cambiar.
- **Sigue pendiente lo del peso de los votos offline.** Es la única de las tres
  que abre una pregunta nueva en lugar de cerrarla, y conviene resolverla antes
  de que haya suficientes servidores como para que alguien note el desequilibrio.
