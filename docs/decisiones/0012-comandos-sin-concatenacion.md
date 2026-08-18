# ADR-0012 — Comandos de Minecraft con lista blanca y parámetros tipados

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03
- **Origen:** hallazgo de seguridad crítico en el documento 14

## Contexto

El documento 14 proponía una acción "Ejecutar comando Minecraft" combinada con
variables interpoladas por el usuario y reglas escritas por el cliente. El
resultado es un sistema de **ejecución remota de comandos en servidores de
terceros dirigido por plantillas de usuario**.

Escenario de explotación con una plantilla típica:

```
Plantilla:  give {player} diamond 5

{player} = "Steve"            → give Steve diamond 5              ✅
{player} = "Steve; op Steve"  → give Steve; op Steve diamond 5    🔴
```

El riesgo es real en este proyecto porque:

- En servidores **offline-mode** los nombres de jugador son texto arbitrario
- Otras variables (nombre de servidor, organización, texto de recompensa) son
  texto libre en cualquier caso
- La víctima no es la plataforma: **es el servidor del cliente**. Un incidente
  así destruye la confianza del ecosistema de forma irreversible.

## Decisión

**Prohibida la construcción de comandos por concatenación de texto.**

| Regla | Detalle |
|---|---|
| Lista blanca | Solo comandos previamente aprobados por la plataforma. Nunca texto libre del cliente. |
| Parámetros tipados | `{player}` es un identificador validado contra su tipo, no una cadena |
| Estructura, no texto | El comando viaja como estructura de datos (comando + parámetros), se ensambla en destino |
| Validación en destino | El plugin rechaza lo que no cumpla el contrato, sin confiar en el emisor |
| Saneado obligatorio | Toda variable interpolada se valida contra su gramática antes de usarse |
| Auditoría | Cada comando ejecutado queda registrado con su origen, plantilla y parámetros |

### Consecuencia sobre el diseño de recompensas

Las recompensas no se modelan como "una cadena de comando", sino como un
**tipo de recompensa** con parámetros validados (dar objeto, dar moneda,
otorgar rango, ejecutar comando de lista blanca). El plugin traduce cada tipo
a la acción correspondiente.

## Consecuencias

- Se pierde la flexibilidad de "escribe cualquier comando". Es una pérdida
  aceptada y deliberada.
- Ampliar el catálogo de recompensas requiere una versión del plugin, no un
  cambio de configuración del cliente
- Si en el futuro se permite comando libre, deberá ser una opción
  explícitamente activada por el dueño, con advertencia clara y aislada del
  motor de reglas
