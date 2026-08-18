# ADR-0025 — Una página propia por modalidad, no un filtro

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-15
- **Decide:** ingeniería
- **Amplía:** [ADR-0019](0019-lecturas-sql-entre-modulos.md) (una tabla más en el
  inventario)

---

## Contexto

El listado ya permitía filtrar por modalidad con `/servidores?modalidad=SKYBLOCK`.
El comentario del código incluso decía que era para posicionar en Google.

No lo era. Esa dirección tenía **el mismo título, la misma descripción y el mismo
canónico** que `/servidores`. Para un buscador eso no es una página nueva: es la
misma con un parámetro. Google la descarta por duplicada, o peor, reparte la
fuerza entre las dos y ninguna sube.

Y el filtro se aplicaba **en memoria sobre el ranking de 50**, así que una
modalidad con cuarenta servidores, ninguno en el top general, se veía vacía.

Es una pena, porque «servidores de skyblock en español» es exactamente lo que la
gente escribe, tiene mucha menos competencia que el término genérico, y es de lo
poco que una web nueva puede ganar de verdad.

## Decisión

**Cada modalidad tiene su página**, en `/servidores/modalidad/{codigo}`, con:

- **Título y descripción propios**, escritos con las palabras de la búsqueda, no
  con el nombre interno. Nadie busca «modalidad SKYBLOCK».
- **Texto de verdad** sobre esa modalidad: dos párrafos escritos a mano por
  categoría, en `lib/categorias.ts`. No plantilla rellenada con el nombre — una
  plantilla produce dieciséis páginas idénticas salvo una palabra, que es
  exactamente lo que Google penaliza como *doorway pages*.
- **Canónico propio**, y `/servidores?modalidad=X` apunta a ella. Todo lo que
  llegue con el parámetro antiguo empuja a la página buena en vez de competir
  con ella.
- **Enlaces desde el listado y desde las demás categorías.** Una página a la que
  no apunta nadie tarda meses en indexarse.
- **Está en el sitemap**, con prioridad por encima de las fichas.

### El filtro baja a SQL

`RankingQueries.ranking(dias, limite, modalidad)`. El límite tiene que aplicarse
**después** de filtrar, no antes: es la diferencia entre una categoría con
cuarenta servidores y una que parece vacía.

Añade una lectura de `server_game_modes` desde el módulo `ranking`, apuntada en
el inventario del ADR-0019. Es una tabla más dentro de una lectura que ya estaba
registrada, no una frontera nueva.

### La ruta lleva `/modalidad/` en medio

`/servidores/skyblock` se lee mejor, pero chocaría con la ficha de un servidor
cuyo identificador fuera «skyblock». Resolver esa ambigüedad con una lista de
palabras reservadas es una trampa que salta el día que alguien registra el
servidor equivocado — y salta en producción, con un cliente de por medio.

Una búsqueda (`?q=`) sí queda con `noindex`: hay infinitas y ninguna aporta nada
en un buscador.

## Consecuencias

**A favor**

- Dieciséis páginas que pueden posicionar por búsquedas que la web genérica no
  gana en su primer año.
- Cada categoría enseña todos sus servidores, no solo los que estaban en el top
  general.
- Enlaces internos entre categorías: Google recorre el sitio entero desde
  cualquier página.

**En contra**

- Dieciséis textos que alguien tiene que mantener cuando cambie lo que ofrece la
  plataforma. Es trabajo de contenido, no de código, y no se puede automatizar
  sin volver a caer en el problema que esto resuelve.
- Una modalidad nueva en `game_modes` sin texto propio funciona, pero con una
  descripción genérica: la página existe y no rompe nada, simplemente no
  posiciona hasta que se le escriba lo suyo.
- Una consulta por categoría en lugar de una para todo. Se cachea 60 segundos
  igual que el ranking, y el índice `idx_server_game_modes_por_modalidad` ya
  existía desde V17.

## Cómo se sabe que funciona

Lo que se puede comprobar automáticamente está cubierto por la compilación: la
página existe, tipa y se genera. Lo demás es de mirar:

- `/servidores/modalidad/skyblock` responde y enseña solo skyblock.
- Un código inventado devuelve **404**, no una lista vacía.
- El código fuente de la página lleva su propio `<title>`, su `description` y su
  `<link rel="canonical">`, distintos de los de `/servidores`.
- `/sitemap.xml` incluye las dieciséis.

Si sube o no en Google no se comprueba con un test: se mira en Search Console
dentro de unas semanas. `docs/POSICIONAMIENTO.md` explica qué esperar y cuándo.
