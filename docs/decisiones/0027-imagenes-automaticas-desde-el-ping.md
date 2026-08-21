# ADR-0027 — Icono y banner automáticos desde el ping

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-20
- **Relacionado:** ADR-0009 (el plugin no es fuente de verdad), ADR-0017 (imágenes subidas)

## Contexto

El usuario del día 1 tiene un servidor, cero votos y cinco minutos de paciencia
(CLAUDE.md, regla 15). Hoy, al terminar el alta, su ficha es un rectángulo gris
con el nombre en texto plano. Para que deje de serlo tiene que abrir un editor
de imágenes, diseñar un banner de 1200×300 y subirlo. La mayoría no lo hace, y
un directorio lleno de fichas grises no manda tráfico a nadie.

Sin embargo, **ese servidor ya tiene una identidad visual**: el icono y el MOTD
que enseña el menú multijugador de Minecraft. Están diseñados, tienen los
colores de la marca del servidor, y el jugador los reconoce. Y ya los podemos
leer: el Server List Ping que ejecutamos cada minuto para medir jugadores
conectados (ADR-0009) devuelve ambos campos en la misma respuesta que ya
recibimos. Los estábamos tirando a la basura.

## Decisión 1 — Esto es del backend, no del plugin

La petición original decía "que el plugin traiga el icono". **No.**

El icono y el MOTD llegan en la respuesta del ping, que ejecutamos nosotros
desde nuestra infraestructura. El plugin no interviene, y no debe: corre en
hardware del cliente y por ADR-0009 no es fuente de verdad de nada. Que un dato
sea cosmético no es motivo para relajar la regla — es motivo para no necesitar
relajarla, porque ya tenemos una vía que no pasa por el cliente.

Consecuencia práctica: **esta funcionalidad no toca el plugin.** No hay versión
nueva que distribuir, no hay que pedirle a nadie que actualice el JAR, y
funciona desde el minuto uno en los servidores que ya están dados de alta.

## Decisión 2 — Fuente por imagen: `AUTO` o `MANUAL`

No un interruptor global "modo automático", sino un origen por cada imagen:

| | `AUTO` | `MANUAL` |
|---|---|---|
| Icono | El favicon del ping, reprocesado | El PNG que subió el dueño |
| Banner | Generado a partir de icono + MOTD | El archivo que subió el dueño |

Reglas:

- **Se nace en `AUTO`.** Un servidor recién dado de alta tiene ficha decente
  antes de que su dueño toque nada.
- **Subir una imagen conmuta esa imagen a `MANUAL`.** Es lo que quiere decir
  alguien que sube un banner. No hace falta un segundo clic para explicarlo.
- **Volver a `AUTO` no destruye lo subido.** Las claves se guardan en columnas
  distintas (`icon_key` / `icon_key_auto`). Alguien que pagó a un diseñador y
  prueba el modo automático por curiosidad tiene que poder volver con un clic.
- Los dos orígenes son independientes: icono manual con banner automático es una
  combinación normal y frecuente.

## Decisión 3 — El favicon del ping es entrada hostil, y se trata como tal

Lo que llega por ese socket lo controla el dueño del servidor, que es
exactamente quien tendría el incentivo para abusar. Pasa por el mismo
`ProcesadorDeImagenes` que las subidas (ADR-0017): se decodifica a píxeles y se
vuelve a codificar. Ni una excepción por venir "de un ping" y no "de un
formulario".

Además, tres límites que la ruta de subida no tenía y ahora tienen las dos:

1. **Tope de dimensiones declaradas antes de decodificar.** Un PNG de 30 KB
   puede declarar 30000×30000 píxeles y ser un rectángulo de un solo color. Al
   decodificarlo, Java reserva la trama completa: 3,6 GB, y el proceso muere. La
   cabecera se lee primero y se rechaza por dimensiones sin llegar a decodificar.
   *Esto era un fallo real de la ruta de subida, no solo un riesgo de la nueva.*
2. **El favicon se limita a 64×64**, que es lo que manda el protocolo. Un
   servidor que anuncie otra cosa está fuera de especificación.
3. **Límite de bytes propio**, más estrecho que el de una subida: un favicon
   legítimo no pasa de 24 KB en base64.

## Decisión 4 — El banner se genera como PNG en el servidor, no como HTML

Se evaluó pintarlo con HTML y CSS en el frontend: sale nítido a cualquier
densidad de pantalla y no cuesta almacenamiento. Se descarta por dos motivos.

El primero es que **un `<div>` no se puede compartir**. La vista previa que sale
al pegar un enlace en Discord o en WhatsApp —que es por donde se comparte un
servidor de Minecraft— necesita un PNG en una URL. Sin él, el enlace se ve como
texto pelado justo en el sitio donde se decide si entrar.

El segundo es que la alternativa "HTML en la web, PNG para redes" obliga a
mantener **dos implementaciones del mismo dibujo** que tienen que salir
idénticas. Eso no se sostiene: divergen a la tercera modificación, y el bug es
invisible hasta que alguien compara.

Se genera **a 2×** (2400×600) y se muestra escalado con `image-rendering:
pixelated`. En una estética de píxel eso no es una concesión: es exactamente el
aspecto que se busca, y en pantallas de alta densidad se ve mejor que el vector.

## Decisión 5 — La fuente es Monocraft (OFL), no la de Mojang

El aspecto de "menú multijugador" depende de una tipografía de píxel. La fuente
del juego es un asset propiedad de Mojang y **no se puede empaquetar en nuestro
backend ni servir desde nuestro dominio**, ni siquiera extrayéndola del `.jar`
del juego.

Se usa **Monocraft** (SIL Open Font License 1.1), que es una recreación libre y
redistribuible. Diferencia visible: Monocraft es monoespaciada y la del juego es
proporcional, así que el espaciado no es idéntico carácter a carácter. A la
distancia a la que se mira un banner, no se nota; y es el precio de no tener un
problema legal.

La fuente vive en `backend/servers/src/main/resources/fuentes/`, con su licencia
al lado. Si no carga, el generador cae a una monoespaciada del sistema en lugar
de fallar: un banner feo es mejor que una ficha sin banner.

## Decisión 6 — El MOTD se sanea antes de pintarse

El MOTD es texto libre que escribe el dueño. Lo que se quita:

- **URLs y dominios.** Es lo más importante y lo menos evidente: el MOTD de
  medio Minecraft dice "vota en otrositio.com". Publicarlo tal cual sería pagar
  hosting para anunciar a la competencia en nuestra propia portada.
- **Caracteres de control**, marcas de dirección bidireccional (que pueden
  reordenar visualmente el texto de alrededor) y espacios de ancho cero.
- **`§k` (obfuscated).** Es una animación de glifos aleatorios; en una imagen
  estática no existe. Se pinta el texto de debajo, sin más.

Lo que se conserva: los colores (los 16 clásicos y los hexadecimales modernos),
negrita, cursiva, subrayado, tachado, los símbolos Unicode y las dos líneas.

**La línea que llevaba una dirección ajena se va entera, no recortada.** La
primera versión quitaba solo el dominio, y el resultado era peor que el
problema: `Vota en otro.net y en 51.161.24.7` quedaba en `Vota en y en`. Eso, en
un banner que firmamos nosotros, se lee como que la plataforma está rota, no como
que el servidor anunciaba a otro sitio. Y una línea cuyo contenido *era* la
dirección que hemos borrado ya no dice nada: no hay nada que salvar recortándola.

Si se van todas las líneas, el banner cae al nombre del servidor. Feo, pero
íntegro.

## Decisión 7 — Se regenera por cambio, con freno

Regenerar en cada ronda de ping sería regenerar cada minuto por servidor para
siempre. Y muchos MOTD llevan un contador de jugadores dentro, así que
*cambian* cada minuto: detectar el cambio no basta como freno.

Por eso:

- Se guarda el hash del MOTD saneado y el del favicon. Sin cambio, no se hace
  nada.
- Con cambio, **como mucho una regeneración cada 6 horas** por servidor.
- El dueño tiene un botón "actualizar ahora" en su panel que se salta el freno,
  con su propio límite. Es lo que necesita quien acaba de cambiar su icono y
  quiere verlo.

## Decisión 8 — Se publica y se puede retirar

Adoptar automáticamente contenido de terceros y servirlo desde nuestro dominio
tiene un riesgo evidente: alguien pone porno o un símbolo nazi de favicon y
aparece en nuestra portada.

La política elegida es publicar al instante y poder retirar:

- Botón de reportar en la ficha pública.
- El panel de administración puede **bloquear las imágenes automáticas de un
  servidor concreto** (`auto_images_blocked_at`). Bloqueado, vuelve al
  marcador de posición y no se readopta nunca más, aunque cambie el favicon.
- Queda registro en `uploads` igual que una subida, con `purpose` propio, así
  que la auditoría y la limpieza de huérfanos funcionan sin código nuevo.

Se descartó exigir revisión previa: rompe la promesa de "se configura solo", que
es la razón de ser de la funcionalidad, y no escala más allá de unas decenas de
servidores.

## Consecuencias

- Un servidor recién dado de alta tiene ficha con imagen en cuanto responde al
  primer ping, sin que su dueño haga nada.
- El directorio deja de estar lleno de rectángulos grises, que es lo que más
  daña la sensación de que la plataforma está viva.
- Aparece una dependencia nueva: una fuente empaquetada, con su licencia que hay
  que conservar en cualquier redistribución.
- El almacenamiento crece con imágenes que nadie subió. Acotado por el freno de
  6 horas y por la limpieza de huérfanos que ya existe.
- Java2D en contenedor exige modo *headless*. Ya lo está por defecto en Spring
  Boot, pero es un supuesto que hay que dejar escrito.
- **Bedrock queda fuera:** su protocolo de ping devuelve MOTD pero no favicon. El
  día que se soporten, el banner automático saldrá sin icono. Es una degradación
  aceptable, no un bloqueo.
