# ADR-0028 — Suspensión, purga por inactividad y limpieza de archivos

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-21
- **Relacionado:** [ADR-0024 — Retirada de servidores inactivos](0024-retirada-de-servidores-inactivos.md),
  [ADR-0017 — Imágenes subidas](0017-imagenes-subidas.md),
  [ADR-0010 — Retención y borrado](0010-retencion-y-borrado.md)

## Contexto

Tres huecos que se descubrieron juntos y se cierran juntos:

1. **`SUSPENDED` existía en la base de datos desde la V3 y ninguna línea de
   código lo usaba.** No había forma de moderar un servidor: si alguien ponía
   algo grave, lo único disponible era quitarle las imágenes.
2. **Nada se limpiaba nunca.** Un servidor archivado por inactividad se quedaba
   archivado para siempre, con sus imágenes ocupando espacio y sus claves de API
   vivas.
3. **La limpieza de huérfanos del ADR-0017 nunca se construyó.**
   `marcarSubidaHuerfana` marcaba la fila y nadie la leía jamás. Cada icono o
   banner reemplazado seguía en el almacén desde el primer día, pagándose por
   bytes que nadie mira. El ADR-0027 empeora el ritmo: genera imágenes solo.

La petición original era «si tras 30 días suspendido no vuelve a recibir ping,
elimínalo». Se cumple lo que busca, pero no como estaba planteado.

## Decisión 1 — Suspender e inactividad son cosas distintas, y no se mezclan

Suspender es **una decisión nuestra sobre alguien que hizo algo**. Archivar por
inactividad es **constatar que no hay nadie en casa**. Colgar un temporizador de
borrado de la suspensión junta las dos y produce dos efectos malos:

- **Destruye la prueba.** El registro de por qué suspendimos a alguien
  desaparece justo cuando más falta hace: cuando esa persona vuelve, o reclama,
  o abre otra cuenta.
- **Premia al infractor.** A quien suspendemos le basta con no hacer nada
  durante treinta días para que su historial se limpie solo.

Así que:

- **Una suspensión no caduca y no purga nunca.** La levanta una persona, o se
  queda.
- **La purga automática cuelga de la inactividad**, que es donde tiene sentido.
- Un servidor suspendido queda fuera del ciclo de inactividad por completo.

## Decisión 2 — Qué hace una suspensión

`status_code = 'SUSPENDED'`, más quién la puso, cuándo y por qué.

Todas las consultas públicas ya filtran por `status_code = 'ACTIVE'` —el
directorio, el ranking, la búsqueda, la ronda de ping y la aceptación de votos—
así que cambiar el estado lo saca de todo a la vez. No hace falta acordarse de
nada en ninguna consulta, que es justo el tipo de cosa que se olvida.

**El dueño ve el motivo en su panel.** Un servidor que desaparece sin
explicación genera un ticket de soporte garantizado y la sensación de que la
plataforma es arbitraria. Se le dice qué pasó y cómo escribirnos.

Sus votos, su historial y sus imágenes se quedan intactos: una suspensión puede
levantarse, y al levantarla el servidor vuelve exactamente como estaba.

## Decisión 3 — La purga no borra la fila, porque no puede

La petición decía «elimínalo». Un `DELETE` real es **imposible**, y no por
diseño nuestro sino por el esquema:

| Tabla | Restricción | Qué pasaría |
|---|---|---|
| `votes` | `ON DELETE RESTRICT` | PostgreSQL rechaza el borrado |
| `vote_rewards` | `ON DELETE RESTRICT` | PostgreSQL rechaza el borrado |
| `slot_bookings` | `ON DELETE CASCADE` | **Se borrarían en silencio espacios destacados ya pagados** |

Las dos primeras se podrían sortear borrando antes todos los votos del servidor.
No se hace: esos votos son las señales con las que el antifraude detecta
patrones, y el producto real es la credibilidad del ranking (CLAUDE.md). Vaciar
el historial de un servidor deja agujeros en los datos con los que se pilla a
quien hace trampas en *otros* servidores.

La tercera es peor y es silenciosa. Un `CASCADE` sobre `slot_bookings` borraría
el registro de qué compró un pedido pagado, dejando un cobro sin nada que lo
explique. Va en contra de la regla 6 de CLAUDE.md.

**Se purga en lugar de borrar.** Se elimina todo lo que cuesta dinero o estorba,
y se conserva lo que el resto de la plataforma necesita:

| Se elimina | Se conserva |
|---|---|
| Las imágenes, del almacén de verdad | La fila del servidor |
| Las claves de API | Sus votos y su historial |
| El MOTD y los hashes del ping | Los pedidos y espacios pagados |
| La descripción y los enlaces | |

El servidor queda marcado con `purged_at` y `deleted_at`, así que desaparece de
todas partes y **deja de volver solo**. Su `slug` queda libre: el índice único
es parcial sobre `deleted_at IS NULL`, así que alguien puede registrar ese
nombre otra vez.

Esto consigue lo que la petición perseguía —que el directorio y el almacén no
crezcan para siempre— sin destruir nada que haga falta.

## Decisión 4 — El calendario

Contando desde la última señal del plugin:

| Día | Qué pasa | Reversible |
|---|---|---|
| 7 | Correo de aviso | — |
| 15 | Sale del directorio (`ARCHIVED`) | Sí, vuelve solo al latir |
| 38 | Segundo correo: «en 7 días borramos tus imágenes» | — |
| **45** | **Purga** | No |

Los 45 días son los **30 que pediste contados desde que se retira**, que es
cuando el reloj empieza a tener sentido: antes del día 15 el servidor todavía
está publicado.

Dos frenos, los mismos que ya tenía el archivado:

- **Un espacio destacado pagado en curso no se purga.** Es dinero cobrado.
- **Un servidor suspendido no se purga.** Otro carril (Decisión 1).

## Decisión 5 — Los archivos huérfanos se borran de verdad

Lo que el ADR-0017 dio por hecho y nunca existió.

Cada vez que se reemplaza una imagen, la anterior se marca en `uploads`. A los
**7 días** de estar marcada, se borra del almacén.

Siete y no cero: si se borrara al instante, una petición en vuelo que ya estaba
sirviendo el archivo anterior se quedaría a medias, y un error al actualizar la
base de datos dejaría al servidor sin imagen y sin forma de recuperarla. Una
semana es tiempo de sobra para detectar un fallo así y volver atrás.

La fila de `uploads` **se conserva** aunque el archivo se borre: es el registro
de quién subió qué, y sirve para auditar y para detectar abuso.

## Consecuencias

- Existe moderación de servidores, que antes no existía.
- El almacén deja de crecer sin techo. Con el ADR-0027 generando imágenes solo,
  esto pasaba de conveniente a necesario.
- Un servidor de temporada que se apaga siete meses pierde sus imágenes y sus
  claves. Vuelve a estar, pero tiene que volver a configurarlo. Es aceptable:
  se le avisa dos veces y tiene cuarenta y cinco días.
- **Deuda deliberada (ADR-0007):** la purga no toca los datos personales que
  puedan quedar asociados. El derecho de supresión del ADR-0010 va por su propia
  vía, sobre la persona y no sobre el servidor. Se registra aquí para que no se
  confundan las dos cosas.
- La ronda nocturna hace ahora tres trabajos. Se mantienen separados en clases
  distintas para que un fallo en uno no impida los otros.
