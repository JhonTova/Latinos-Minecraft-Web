# ADR-0009 — El plugin nunca es fuente de verdad

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03
- **Corrige:** el requisito "protección contra plugins modificados" del documento 06

## Contexto

El documento 06 exigía "protección contra plugins modificados". Es un objetivo
inalcanzable:

1. El JAR se ejecuta en hardware del dueño del servidor
2. El dueño del servidor es quien tiene incentivo económico para hacer trampas
3. Java se decompila y recompila trivialmente
4. La API Key vive en su `config.yml` — se la entregamos nosotros
5. Un plugin modificado firma datos **falsos** con una clave **legítima**: la
   firma valida, el contenido miente

Ninguna ofuscación, checksum o atestación resuelve el problema del cliente no
confiable cuando el cliente es el adversario.

## Decisión

> El plugin es un **actuador** ("entrega esta recompensa"), nunca un **sensor**
> de confianza. Ningún dato reportado por el plugin puede influir en el
> ranking, en las recompensas ni en la facturación.

### Clasificación de la información

| Dato | ¿Confiable? | Tratamiento |
|---|---|---|
| "El jugador X votó" | ❌ Nunca | El voto nace y se valida en la plataforma |
| Jugadores conectados | ❌ Autorreportado | **Medir con ping directo** desde nuestra infraestructura |
| TPS, memoria, uptime | ❌ Autorreportado | Solo informativo, en el panel del propio dueño. Nunca en el ranking. |
| Versión del plugin | ⚠️ Autorreportado | Aceptable: mentir no da ventaja |
| "Recompensa entregada" | ⚠️ Autorreportado | Confirmación de entrega, no prueba |

### Regla derivada

Todo dato que influya en el ranking debe **medirlo la plataforma por sí misma**
o **derivarse de acciones ocurridas en la plataforma**.

## Consecuencias

- Se necesita un servicio de ping directo (protocolo de server list ping) como
  fuente autoritativa de jugadores conectados
- El heartbeat sigue siendo útil: prueba conectividad y permite alertas de
  caída, pero su contenido no es autoritativo
- Las firmas HMAC del heartbeat siguen siendo necesarias (evitan suplantación
  entre servidores), pero **no garantizan veracidad del contenido**
- Es una restricción permanente: cualquier funcionalidad futura que pretenda
  usar datos del plugin para ranking debe rechazarse o rediseñarse
