# ADR-0005 — El antifraude es dominio core desde la fase 1

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03

## Contexto

El producto que se vende es un **ranking**. Un ranking solo tiene valor si es
creíble. Si un dueño de servidor puede comprar votos, el ranking deja de
correlacionar con la calidad, los jugadores dejan de confiar en el directorio,
el tráfico se desploma y la plataforma no vale nada.

El atacante no es un hacker externo: es **el propio cliente**, tiene incentivo
económico directo, dispone de tiempo ilimitado y opera en un ecosistema donde
las herramientas de fraude ya existen y se venden abiertamente.

## Decisión

`Trust & Abuse` es un **bounded context de primera clase, core**, con modelo
de dominio propio, almacenamiento propio y ciclo de vida propio, presente
**desde la fase 1**.

No es un middleware de rate limiting añadido más adelante.

## Estrategia multicapa

| Capa | Papel | Nota |
|---|---|---|
| Reglas + risk score | **Defensa principal** | Explicable y auditable. Defendible ante un cliente que reclama. |
| Análisis de comportamiento | Alto valor | Patrones temporales, secuencias imposibles |
| Análisis de grafo | Alto valor | Detecta colusión entre servidores y cuentas |
| Consistencia de identidad | Medio | Historial del votante |
| Reputación de IP (VPN/proxy/datacenter) | **Solo señal de apoyo** | Servicio de pago por consulta; los proxies residenciales la derrotan |
| Captcha adaptativo | Reactivo | Solo ante señales de riesgo |
| Machine Learning | ❌ **No al inicio** | Requiere datos etiquetados que no existen. Las reglas los generan. |

## Consecuencias

- El motor de votos se diseña **alrededor** del antifraude, no al revés
- Un voto rechazado no consume la ventana de voto (ADR-0011)
- Son obligatorias las herramientas de moderación con **apelaciones**: habrá
  falsos positivos y afectarán a clientes de pago
- El motor de reglas es configurable en caliente: se debe poder reaccionar a un
  patrón nuevo de fraude sin desplegar código
- La reputación de IP entra en la ruta crítica del voto → requiere caché,
  timeout y circuit breaker; su caída nunca puede tumbar la votación
