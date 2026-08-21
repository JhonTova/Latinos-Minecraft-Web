# ADR-0030 — Dos rankings separados, y quién puede entrar como filtro

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-21
- **Decide:** el propietario del producto
- **Cierra:** la decisión 4.4 que abrió el [ADR-0029](0029-decisiones-de-marca-identidad-y-alcance.md)

## Contexto

Aceptar servidores offline-mode (ADR-0029) dejó una pregunta abierta: **cuánto
pesa un voto offline frente a uno premium**. Hasta ahora pesaban igual.

En offline-mode la identidad del votante no es débil: **no existe**. El servidor
no le pregunta nada a Mojang, así que el jugador escribe el nombre que quiera.
Para votar diez veces en premium hacen falta diez cuentas compradas; en offline,
diez nombres.

El límite por conexión acota el abuso pero no puede apretarse: en Latinoamérica
muchísima gente comparte IP con su operador móvil, y estrechar ese límite deja
fuera a votantes legítimos. Cualquiera con datos y wifi ya tiene dos IPs.

Resultado: un servidor offline tiene el camino más fácil al número uno. No por
hacer trampas — porque el sistema se lo permite. Y el producto real de esta
plataforma es la credibilidad del ranking.

## Decisión 1 — Dos rankings, no un peso inventado

Se evaluaron tres salidas:

| | Por qué no |
|---|---|
| Un ranking, el voto offline pesa menos | Cualquier número es arbitrario. El primer dueño que pierda un puesto preguntará por qué su gente vale medio voto, y la única respuesta honesta es «porque lo decidimos nosotros». Eso son quejas para siempre |
| Un ranking, todos iguales | Aguanta hasta el día que alguien abuse. Y quien lo note serán justo los servidores premium grandes, que son los que pagan los espacios destacados |

**Se separan en dos clasificaciones:** «Solo cuentas premium» y «Cuentas premium
y no premium». Cada servidor compite contra sus iguales y no hace falta defender
ningún número.

### El argumento que se le da al visitante no es el antifraude

Es más simple y más cierto: **están separados para que encuentres dónde puedes
jugar de verdad.**

Un jugador sin cuenta original que entra a un servidor premium no es que lo pase
peor: el servidor **le rechaza la conexión**. Antes, el directorio le mandaba
ahí sin avisar. Separarlos no es una medida antifraude disfrazada de función —
es que el directorio funcione mejor, y de paso el problema del ranking
desaparece solo.

Por eso la separación es visible y está explicada bajo cada título, no escondida
en un aviso legal.

## Decisión 2 — Separados en la portada, mezclados en el listado

- **Portada:** las dos clasificaciones, una al lado de la otra. Es donde el
  ranking *significa* algo, y donde comparar peras con manzanas haría daño.
- **`/servidores`:** todos mezclados, y el visitante filtra por lo que quiera —
  nombre, modalidad, tipo de cuenta, móvil. Obligarle a elegir un mundo antes de
  poder buscar sería ponerle una puerta delante de la puerta.

En móvil la portada no tiene sitio para dos columnas, así que hay un conmutador.
**Las dos listas se pintan igualmente en el servidor** y se esconde una con CSS:
cambiar de pestaña es instantáneo, no hay segunda petición, y Google ve los dos
rankings aunque rastree con pantalla estrecha.

Por defecto se abre por **«Premium y no premium»**. Es la lista donde puede
jugar cualquiera: quien tiene cuenta original entra en los dos sitios, quien no
la tiene solo en estos. Abrir por la lista que excluye a la mitad del público
sería empezar por el sitio equivocado.

## Decisión 3 — Bedrock es una casilla, no un tipo de juego

La petición decía «añadir Bedrock». Casi ningún servidor latino es Bedrock
**nativo**: lo que hay son servidores Java con **Geyser**, que aceptan jugadores
de móvil y consola.

Eso no es otro tipo de juego, es una propiedad del servidor Java. Un
`game_type` nuevo arrastraría otro protocolo de ping entero —Bedrock usa RakNet
sobre UDP, nada que ver con el Server List Ping de Java— y otro modelo de
identidad (gamertags de Xbox Live). Nada de eso hace falta para responder la
pregunta que de verdad se hace el jugador: **«¿puedo entrar desde el móvil?»**.

Es una columna, la declara el dueño y **no se verifica**, igual que la región:
detectar Geyser desde fuera no es fiable, y por ADR-0009 un dato declarado nunca
alimenta el ranking. Solo filtra.

Bedrock nativo, si algún día hace falta, es un proyecto aparte y este ADR no lo
cierra.

## Decisión 4 — La fila del ranking responde a su propio ancho

Detalle de implementación que merece quedar escrito porque se descubrió
rompiendo la pantalla.

Al partir el ranking en dos columnas de 540 px, las filas se descolocaron: las
etiquetas acababan encima de las cifras. La causa es que usaban puntos de corte
de **pantalla** (`sm:`), así que en un monitor de 1440 px la fila creía tener
sitio de sobra y se colocaba en horizontal dentro de una columna estrecha.

Se cambia a **consultas de contenedor** (`@container`): la fila mira su propio
ancho. El mismo componente sirve para la columna estrecha del top doble, para el
listado a lo ancho y para el móvil, sin una variante aparte que mantener y sin
que las tres puedan divergir.

## Consecuencias

- Ningún servidor compite contra otro que juega con reglas distintas.
- El visitante ve de un vistazo si puede entrar, antes de pulsar.
- La portada da el doble de visibilidad: dos primeros puestos en vez de uno.
- **Los votos siguen contando igual dentro de cada lista.** No se ha inventado
  ningún peso, y por tanto no hay ningún número que defender.
- Un servidor que cambia de offline a premium salta de clasificación. Es
  correcto —cambió las reglas de quién le vota— pero conviene decirlo en el
  panel antes de que alguien lo descubra por su cuenta.
- El filtro de móvil depende de que los dueños lo marquen con honestidad. Quien
  lo marque sin tenerlo gana un clic y pierde al jugador en cuanto lo intente;
  el texto del panel lo dice así.
