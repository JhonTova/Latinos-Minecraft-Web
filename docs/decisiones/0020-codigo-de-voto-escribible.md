# ADR-0020 — El token de voto pasa a ser un código escribible a mano

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-12
- **Decide:** el propietario del producto
- **Modifica:** [ADR-0016 — Token de voto](0016-token-de-voto.md) (el mecanismo, no la decisión)

---

## Contexto

ADR-0016 estableció que la identidad del votante viaja dentro de un token que
solo se obtiene desde dentro del servidor. Esa decisión sigue en pie y es la que
sostiene la credibilidad del ranking.

Lo que falla es el **formato**. El token eran 24 bytes en base64: 32 caracteres
sin sentido. Perfecto para pulsar un enlace, inservible para quien no puede
pulsarlo.

**Los jugadores de Bedrock no pueden pulsar enlaces en el chat de Minecraft.**
Móvil y consola son una parte enorme del público hispanohablante, y con el
formato anterior no tenían ninguna forma de votar: ni pulsando, ni copiando a
mano treinta y dos caracteres aleatorios de una línea de chat.

No era una molestia. Era una exclusión.

## Decisión

El token pasa a ser un **código de 12 caracteres en tres grupos de cuatro**:
`K7M2-9QX4-RT3F`.

### El alfabeto

32 símbolos, sin `I`, `L`, `O` ni `U`.

Las tres primeras se confunden con unos y ceros al leerlas del chat a doce
puntos. La `U` se quita para que no aparezcan palabras reconocibles por
accidente. Lo que se lee mal se escribe mal, y quien lo escribe mal concluye que
el sitio no funciona — no que se equivocó.

### Se es generoso al leer y estricto al guardar

Lo que escribe el jugador se normaliza antes de comprobarlo: mayúsculas, sin
guiones, sin espacios, y con `I`/`L` → `1`, `O` → `0`, `U` → `V`. Esas erratas no
son culpa suya: son consecuencia directa de haber elegido un código que se
escribe a mano, y absorberlas es parte del trato.

### Entropía

12 caracteres sobre 32 símbolos son **60 bits**. Frente a los 192 anteriores
parece un retroceso; no lo es, porque el token no protege un secreto permanente:

- Vive **10 minutos**.
- Sirve **una sola vez**.
- El límite de tráfico permite 10 intentos por minuto y por IP.

Adivinar uno exigiría acertar entre 10^18 combinaciones a diez intentos por
minuto. El factor limitante nunca fue la longitud.

## Consecuencias

**A favor**

- Los jugadores de Bedrock pueden votar. Antes, no.
- El enlace pinchable sigue existiendo: quien juega en PC no nota el cambio.
- Un código legible se puede dictar por voz en un Discord.

**En contra**

- Menos entropía. Aceptable por lo de arriba, y revisable si algún día se
  alargara la vigencia del código.
- Hay dos formas de llegar al mismo voto (enlace y código), así que la
  normalización tiene que ser idéntica en el backend y en la web. Están las dos
  probadas contra los mismos casos.

## Lo que NO cambia

**La web sigue sin preguntar el nick.** Fue la tentación durante un rato y se
descartó: si la ficha pública pidiera el nombre del jugador, cualquiera podría
votar en nombre de otro sin haber pisado nunca ese servidor. El código es lo que
prueba que estuviste dentro, y esa prueba es el producto.

## Cómo se sabe que funciona

`CodigoDeVotoTest`, 17 casos. Además de la forma y la unicidad, comprueba lo que
de verdad importa: que escribir el código en minúsculas, sin guiones, con
espacios de más o con una ele donde había un uno lleve al mismo sitio.
