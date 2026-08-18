# ADR-0026 — El código de voto baja a ocho caracteres

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-18
- **Decide:** el propietario del producto
- **Modifica:** [ADR-0020 — Código de voto escribible a mano](0020-codigo-de-voto-escribible.md) (la longitud, no la decisión)

---

## Contexto

ADR-0020 convirtió el token en un código de 12 caracteres para que los jugadores
de Bedrock —móvil y consola, que no pueden pulsar enlaces en el chat de
Minecraft— pudieran votar escribiéndolo a mano. Esa decisión funcionó: pasaron de
no poder votar a poder hacerlo.

Lo que llega ahora es la queja del siguiente escalón: **el código es demasiado
largo para retenerlo de una lectura**.

Doce caracteres son tres grupos de cuatro. Nadie sostiene tres grupos en la
cabeza de una vez, así que el jugador de consola hace el viaje dos o tres veces:
mira la pantalla, escribe cuatro, vuelve a mirar, escribe otros cuatro. Cada uno
de esos viajes es una oportunidad de equivocarse y de abandonar — y hay un reloj
de diez minutos corriendo.

## Decisión

El código pasa a **8 caracteres en dos grupos de cuatro**: `K7M2-9QX4`.

El alfabeto no cambia (32 símbolos, sin `I`, `L`, `O` ni `U`) ni cambia la
tolerancia al escribirlo. Solo cambia la longitud.

Dos grupos es lo que se retiene de una lectura. Es un tercio menos de tecleo y,
lo que de verdad importa, **una mirada menos**.

### Entropía: por qué 8 y no menos

8 caracteres sobre 32 símbolos son **40 bits**: 1,1 billones de combinaciones
(10^12).

Lo que protege el código no es su longitud, sino su vida:

- Vive **10 minutos**.
- Sirve **una sola vez**.
- El límite de tráfico da **10 intentos por minuto y por IP** (`ControlDeTrafico`,
  categoría `VOTO`, que cubre `/vote-tokens`).

El ataque realista no es adivinar *un* código concreto, sino acertar *cualquiera*
de los que están vivos. Con 10 000 códigos vivos a la vez —muy por encima de lo
realista para esta plataforma— cada intento acierta con probabilidad 9×10⁻⁹. Una
botnet de mil direcciones distintas, agotando su cupo, tardaría **más de una
semana** en acertar uno. Y el premio de acertar es el voto de un día de un
jugador, que además va al servidor que ese jugador eligió.

Se descartó bajar a 6 caracteres (30 bits) precisamente por esta cuenta: ahí la
misma botnet acierta uno cada pocas horas, y menos aún según creciera la
plataforma. **8 es el suelo**, no un punto intermedio elegido al azar.

## Consecuencias

**A favor**

- Un tercio menos que teclear, y sobre todo una lectura en vez de dos o tres.
- La longitud vive en una sola constante por lado (`CodigoDeVoto.LARGO` en el
  backend, `LARGO_DEL_CODIGO` en la web). Volver atrás es cambiar un número.

**En contra**

- Menos entropía que antes. Aceptable por la cuenta de arriba, y **revisable si
  alguna vez se alargara la vigencia de los códigos**: es la vigencia, no la
  longitud, lo que sostiene el margen.
- Los códigos emitidos antes del despliegue dejan de resolverse. Duran diez
  minutos, así que la ventana de molestia es de diez minutos.

## Lo que se hizo además, y no es cosmético

- **El plugin distingue Bedrock de Java** por el UUID (Floodgate asigna a los de
  Bedrock uno cuyos primeros ocho bytes son cero, y ningún UUID de Java cae ahí).
  A los de móvil y consola **ya no se les enseña el botón** «PULSA AQUÍ»: para
  ellos era una promesa incumplida —lo pulsaban, no pasaba nada, y la conclusión
  era que el servidor estaba roto—. Ven dos datos y ninguno más: a dónde ir y qué
  escribir.
- **El chat da la dirección completa de la página** (`latinosmc.com/votar`) en
  lugar de solo el dominio. Antes aterrizaban en la portada a buscar dónde meter
  el código, con el reloj corriendo.
- **Existe `/votar`** como página propia y en la barra de navegación. Antes el
  campo para escribir el código solo estaba dentro de la ficha de cada servidor:
  quien no supiera a qué ficha ir no tenía dónde escribirlo.

## Cómo se sabe que funciona

`CodigoDeVotoTest` sigue cubriendo la forma, la unicidad, el uso del alfabeto
completo y —lo que de verdad importa— que escribirlo en minúsculas, sin guiones,
con espacios de más o con una ele donde había un uno lleve al mismo sitio. Los
casos se actualizaron a la longitud nueva; el que comprueba la forma falla si
alguien vuelve a poner tres grupos sin pasar por aquí.
