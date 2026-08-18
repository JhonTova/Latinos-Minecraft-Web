# Salir en Google

Qué está hecho, qué decide de verdad la posición y qué hacer por orden.

---

## Lo primero, sin rodeos

**La parte técnica ya está hecha.** Título, descripción, mapa del sitio,
`robots.txt`, datos estructurados, enlaces canónicos, imagen al compartir,
velocidad. Todo eso está y funciona.

Y no es lo que decide si sales el primero.

Google ya sabe de qué va tu web. Lo que aún no sabe es **por qué debería
enseñarla antes que a otra que lleva ocho años haciendo lo mismo**. Eso no se
arregla con más etiquetas: se arregla con contenido que responda a lo que la
gente busca y con otros sitios enlazándote.

**Cuánto tarda:** un dominio nuevo no compite por «servidores minecraft» en
semanas. Lo realista son **de tres a seis meses** para empezar a aparecer en
términos con competencia, y antes en los más específicos. Cualquiera que te
prometa la primera posición en un mes te está vendiendo humo o enlaces
comprados — y los enlaces comprados hoy son una penalización mañana.

---

## Lo que ya está hecho

| Qué | Dónde |
|---|---|
| Título y descripción pensados para búsquedas, no para la marca | `/panel/admin`, editables |
| Mapa del sitio con todas las fichas de servidor | `/sitemap.xml`, se genera solo |
| `robots.txt` que excluye panel, API y códigos de voto | `/robots.txt` |
| Datos estructurados: buscador interno, lista de servidores, ficha de cada uno | En el HTML de cada página |
| Enlaces canónicos y tarjeta al compartir en Discord/WhatsApp | `layout.tsx` |
| H1 con «servidores de Minecraft en español» | Portada |

Lo único que falta de la parte técnica está en `LANZAMIENTO.md` § 10: **dar de
alta el dominio en Google Search Console y enviar el sitemap**. Sin eso Google
tarda semanas en encontrarte; con eso, días. Hazlo el primer día.

---

## Lo que de verdad mueve la posición

Por orden de lo que más rinde para el esfuerzo que cuesta.

### 1. Que los dueños de servidores te enlacen

**Es la palanca más fuerte que tienes, y ya viene incluida en tu producto.**

Un servidor quiere votos. Para tener votos necesita mandar a sus jugadores a tu
web. Va a poner tu enlace en su Discord, en su web y en su tienda **porque le
conviene a él**. Eso es exactamente lo que Google mide para decidir si una web
importa, y tú lo consigues sin pedir favores.

Para que ocurra a lo grande, dales el enlace ya hecho:

- Un **banner o botón «Vótanos»** con su código HTML listo para copiar, en el
  panel de cada servidor.
- Una **imagen dinámica** con su posición y sus votos (`/servidores/x/banner.png`)
  que puedan pegar en su web. Se actualiza sola, así que la dejan puesta.

Un servidor con banner es un enlace permanente. Cien servidores con banner son
cien enlaces desde webs del mismo tema, que es precisamente la señal más difícil
de conseguir y la que más cuesta falsificar.

> **Esto es lo siguiente que construiría.** No está hecho.

### 2. Páginas por categoría — ✅ hecho

Nadie que sepa lo que quiere busca «servidores de minecraft» a secas. Busca
«servidores de skyblock español», «servidores de bedwars latinos», «servidores
survival en español».

Esas búsquedas tienen **mucha menos competencia** y traen gente que ya sabe a qué
va a jugar. Una web nueva puede ganarlas; la genérica, no todavía.

Hay dieciséis, una por modalidad, en `/servidores/modalidad/{codigo}`. Cada una
con su título, su descripción y **dos párrafos escritos a mano** sobre esa
modalidad — no plantilla rellenada con el nombre, que produce dieciséis páginas
idénticas y eso Google lo penaliza (ADR-0025).

El texto vive en `frontend/src/lib/categorias.ts`. **Reescríbelo cuando quieras:**
lo que hay es un punto de partida decente, pero tú conoces a tu público mejor que
yo, y una descripción que suene a alguien que juega vale más que una correcta.

### 3. Que el ranking cambie todos los días

Esto ya lo tienes: los votos entran a diario y el orden se mueve. Una página que
cambia sola es una página que Google vuelve a rastrear a menudo, y eso ayuda a
que lo nuevo se indexe rápido.

La retirada de servidores muertos (ADR-0024) juega a favor: un directorio donde
todas las fichas llevan a algo vivo es exactamente lo que Google intenta premiar.

### 4. Discord, YouTube y foros

El público de Minecraft en español vive ahí, no en Google. Y el tráfico que
llega desde esos sitios acaba mejorando también la posición en Google, porque la
gente que te conoce te busca por el nombre — y eso Google lo cuenta.

No hace falta ser un experto en marketing: entra en los Discord de servidores que
ya existen y ofréceles darse de alta. Es su interés tanto como el tuyo.

---

## Lo que NO hay que hacer

- **Comprar enlaces.** Funciona unos meses y luego te hunde. No hay vuelta atrás
  rápida.
- **Repetir palabras clave** en el título o el texto. Google lo detecta desde
  hace quince años y penaliza.
- **Crear páginas casi iguales** para cada variante de búsqueda
  («servidores latinos», «servidores hispanos», «servidores en español», todas
  con el mismo contenido). Se llaman *doorway pages* y son penalización directa.
- **Tocar la etiqueta `keywords`** esperando algo. Google la ignora desde 2009.
  Está puesta porque no cuesta nada, no porque sirva.

---

## En qué orden

1. **Hoy:** Search Console + enviar el sitemap (`LANZAMIENTO.md` § 10).
2. **Primera semana:** conseguir los primeros diez servidores reales. Sin
   servidores no hay nada que posicionar, y da igual lo bien escrito que esté el
   título — una categoría vacía no sube por mucho texto que tenga.
3. **Primer mes:** el banner «Vótanos» para que ellos te enlacen. Es lo único
   importante de esta lista que todavía no está construido.
4. **Cuando tengas datos:** reescribir el texto de las categorías que más
   visitas reciban, con tus palabras.
5. **Siempre:** mirar en Search Console **qué búsquedas** te traen gente. No lo
   que tú crees que buscan: lo que buscan. Esa lista es la que dice qué páginas
   escribir después.

El paso 2 es el que casi todo el mundo se salta, y es el único imprescindible.
Un directorio con tres servidores no sale en Google por muy bien hecho que esté,
porque no hay nada que enseñar.
