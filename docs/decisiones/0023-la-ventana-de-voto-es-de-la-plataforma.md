# ADR-0023 — La ventana entre votos la fija la plataforma, no el dueño del servidor

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-15
- **Decide:** el propietario del producto
- **Modifica:** [ADR-0011 — Ventana de voto](0011-ventana-de-voto.md) (quién la
  decide, no cómo se calcula)

---

## Contexto

Cada servidor elegía cada cuántas horas podía volver a votar el mismo jugador.
Lo elegía **su dueño**, al registrarlo, con un margen de 12 a 48 horas y un
`CHECK` en la base de datos que lo mantenía dentro.

El margen era la defensa, y no defendía nada. Un servidor con la ventana en 12
horas recibe **el doble de votos** que uno idéntico con la ventana en 24, sin que
sus jugadores hagan nada distinto: el mismo jugador simplemente puede votar dos
veces al día en vez de una.

No hace falta mala fe para que esto se rompa. Basta con que uno lo baje al mínimo
para que a todos los demás les convenga bajarlo también, y a partir de ahí el
ranking deja de comparar servidores y pasa a comparar quién leyó mejor el
formulario de registro. El producto es la credibilidad del ranking: una posición
que se consigue rellenando un campo no es una posición que valga nada.

El acotado a 12–48 horas era la señal de que el problema se veía venir — el
comentario del código decía literalmente *«acotada para que nadie fije una
ventana de 1 hora e infle su ranking»*. Acotar la palanca no la quita: solo
limita cuánto se puede empujar.

## Decisión

**La ventana entre votos es una sola para toda la plataforma y solo la toca su
administrador.** Por defecto, **24 horas**.

Vive donde ya vivían las demás reglas de voto: la tabla `vote_settings` (V14),
editable desde `/panel/admin` sin desplegar. La columna `vote_window_hours` de
`servers` se **borra** (V19).

Se borra en lugar de dejarla sin usar a propósito. Una columna que nadie lee pero
sigue en el esquema es una invitación a volver a conectarla dentro de seis meses
sin recordar por qué se desconectó.

El dueño deja de verla: desaparece del formulario de alta y de la ficha que
recibe su panel. La página pública del servidor sigue diciendo cada cuánto se
puede votar, pero ahora ese número sale de la configuración de la plataforma, y
es el mismo en todas las fichas.

### Lo que NO cambia

**Cómo se calcula.** Sigue siendo una ventana deslizante desde el último voto
aceptado, en UTC, sin días naturales (ADR-0011). Lo que cambia es quién decide su
duración.

**Que sea configurable.** Es una decisión de producto, no de ingeniería:
revisarla no debería exigir un despliegue. Solo que ahora se revisa en un sitio
en lugar de en cada servidor.

## Consecuencias

**A favor**

- Todos los servidores compiten con la misma regla. El ranking vuelve a comparar
  servidores.
- Una palanca menos en manos de quien tiene incentivo para moverla — la misma
  idea que ADR-0009 aplicada a la configuración en lugar de a los datos.
- Un formulario de alta más corto. El dueño no tenía forma de decidir bien ese
  número: solo podía elegir el que más le convenía.

**En contra**

- Los servidores existentes que hubieran elegido 12 o 48 horas pasan a la ventana
  global. Es el objetivo, pero es un cambio en las reglas bajo sus pies y
  conviene avisarlo si ya hubiera servidores registrados.
- La ventana global admite de 1 a 168 horas, un margen mucho más ancho que el de
  12–48 que tenían los dueños. Es deliberado: el administrador de la plataforma
  puede querer una campaña de 12 horas o una ventana semanal, y cualquier valor
  que ponga **se aplica a todos por igual**, así que no distorsiona la
  comparación.

## Cómo se sabe que funciona

`CastVoteServiceTest`, 20 casos. Los tres que fijan esta decisión:

- **La ventana sale de la configuración de la plataforma, no del servidor** — se
  cambia a 48 h en los ajustes y un voto que con 24 h ya estaría permitido se
  rechaza, con las horas restantes correctas.
- **Bajar la ventana en la configuración deja votar antes.**
- **La ventana por defecto es de 24 horas.**

Además, el puerto `ServerDirectory.ServerInfo` ya no tiene el campo: devolver una
ventana por servidor no compila. Si alguien intentara reintroducirla tendría que
tocar la frontera entre módulos, que es exactamente donde se quiere que pare a
pensar.
