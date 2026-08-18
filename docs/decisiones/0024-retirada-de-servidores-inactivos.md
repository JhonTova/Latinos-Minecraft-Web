# ADR-0024 — Los servidores que dejan de dar señales se retiran del directorio

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-15
- **Decide:** el propietario del producto
- **Relacionado:** [ADR-0009 — El plugin no es fuente de verdad](0009-plugin-no-es-fuente-de-verdad.md)

---

## Contexto

Un directorio no se estropea de golpe: se va llenando. Servidores que se
probaron un fin de semana, altas de gente que nunca instaló el plugin, proyectos
que cerraron hace un año y siguen ocupando su sitio en el ranking.

El daño es doble. El visitante pulsa fichas que no llevan a ninguna parte y
concluye que el sitio no está mantenido. Y el dueño de un servidor vivo compite
contra decenas que ya no existen, así que su posición dice menos de lo que
debería. El producto es la credibilidad del ranking; un ranking con muertos
dentro no la tiene.

## Decisión

**A los 15 días sin señales del plugin, el servidor sale del directorio.** Con
aviso a los 7.

### La señal es el latido del plugin

`server_api_keys.last_used_at`, que se refresca en cada llamada autenticada. El
plugin late cada minuto, así que quince días de silencio son quince días de
silencio de verdad.

Nunca lo que el dueño declare, ni un campo de «sigo activo» en el panel: eso lo
rellenaría precisamente quien tiene el incentivo para rellenarlo (ADR-0009). Un
servidor que se registró y jamás conectó cuenta desde su fecha de alta — también
es un servidor que no está.

### Se archiva, no se borra

Ésta es la parte que importa, y va en contra de la lectura literal de «se
elimina automáticamente».

Quince días sin señal **no prueban que el servidor haya muerto**. Prueban que no
hemos sabido de él. Una migración de hosting, unas vacaciones, un mes con la
tarjeta caducada o un plugin que se perdió en una actualización dan exactamente
la misma señal que un cierre definitivo, y desde fuera no se distinguen.

Así que el servidor pasa a `ARCHIVED`:

- **Desaparece** del directorio, del ranking, de la búsqueda y del ping. Que es
  todo lo que hacía falta para que la web no se llene de muertos.
- **La fila se queda.** Sus votos y su historial son de la plataforma, no solo
  suyos: borrarlos deja huecos en datos que usa el antifraude.
- **Vuelve solo.** En cuanto el plugin late otra vez, el servidor reaparece donde
  estaba. Sin escribir a soporte, sin registrarse de nuevo, sin perder su
  identificador.

Un borrado irreversible por dos semanas de silencio convierte un problema
pasajero del cliente en una pérdida definitiva, y la reclamación la acabamos
atendiendo nosotros. Archivar consigue el objetivo completo sin ninguna de esas
consecuencias.

### Se avisa antes

A los 7 días sale un correo a quien pueda hacer algo: dice cuántos días quedan y
las tres causas habituales (servidor apagado, plugin desinstalado, clave del
`config.yml` caducada). **Una sola vez** — repetirlo cada noche durante ocho días
es la forma más rápida de acabar en la carpeta de spam, y entonces tampoco se lee
el que importa.

Al retirarlo sale un segundo correo que explica que no se ha borrado nada y que
vuelve solo.

### Un espacio destacado pagado no se retira

Es dinero ya cobrado. Retirar ese servidor dejaría la portada vendiendo un hueco
vacío y a nosotros debiendo un servicio que no prestamos. Se deja, y se registra
en el log para mirarlo a mano.

## Consecuencias

**A favor**

- El directorio se mantiene solo. No hay una tarea de limpieza manual que alguien
  tenga que acordarse de hacer.
- El ranking compara servidores que existen.
- Se dejan de sondear servidores muertos: la ronda de ping deja de gastar
  intentos en direcciones que no responden.

**En contra**

- Un servidor que se apaga por temporada (eventos, servidores de temporada)
  desaparece y vuelve. Es el comportamiento correcto —mientras está apagado no
  hay dónde jugar— pero conviene decirlo en la documentación del panel.
- Depende de que el correo funcione. Si el proveedor está caído, el servidor se
  retira igual y su dueño no se entera hasta que lo mira. Se registra en el log;
  el fallo de correo no interrumpe la ronda, porque abandonar a medias dejaría el
  resto del directorio sin revisar.

**Configurable sin desplegar:** `latinosmc.limpieza.dias-aviso` (7),
`dias-retirada` (15) y `cron` (4:30). Los valores por defecto van en el código,
así que un `.env` existente sigue siendo válido tal cual.

## Cómo se sabe que funciona

`LimpiezaDeServidoresInactivosTest`, 9 casos. Los que importan no son los que
comprueban que retira, sino los que comprueban que **no retira de más**:

- Con 14 días todavía no se retira.
- Un servidor con espacio destacado pagado no se retira.
- El aviso no se repite.
- Si el correo falla, la ronda sigue y el servidor se retira igual.
- Un servidor sin dueño localizable se retira sin intentar escribir a nadie.

Lo que **no** cubren: la consulta que decide quién lleva días sin señales es SQL
—con `GROUP BY` y `HAVING` sobre las claves del plugin— y el proyecto todavía no
tiene tests contra una base de datos real (ADR-0021 lo registra como deuda). Está
razonada y revisada, no ejecutada por una máquina. Antes de confiar en la primera
ronda conviene mirarla a mano:

```sql
SELECT s.slug, s.status_code,
       COALESCE(MAX(k.last_used_at), s.created_at) AS ultima_senal,
       now() - COALESCE(MAX(k.last_used_at), s.created_at) AS silencio
  FROM servers s
  LEFT JOIN server_api_keys k ON k.server_id = s.id AND k.revoked_at IS NULL
 WHERE s.deleted_at IS NULL
   AND s.status_code IN ('ACTIVE', 'PENDING_VERIFICATION')
 GROUP BY s.id
 ORDER BY ultima_senal;
```
