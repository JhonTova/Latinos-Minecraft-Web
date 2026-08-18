# ADR-0014 — Compatibilidad con Votifier como vía de entrada sin fricción

- **Estado:** 🟡 Propuesto — pendiente de verificación técnica y de licencias
- **Fecha:** 2026-08-03

## Contexto

En el requisito 11 se identificó que **la instalación del plugin es el punto
donde se pierde a la mayoría de usuarios**: es el único paso que ocurre fuera
de la plataforma, en infraestructura ajena, sin feedback.

**Votifier** es el protocolo de facto del ecosistema Minecraft para que una
lista de servidores notifique un voto a un servidor. La mayoría de servidores
ya lo tienen instalado, porque es lo que exigen las listas existentes.

## Decisión propuesta

Soportar **dos vías de integración**:

| Vía | Fricción | Funcionalidad |
|---|---|---|
| **Votifier** (compatibilidad) | 🟢 **Cero** — ya está instalado | Entrega de votos |
| **Plugin propio** | 🟡 Media | Votos + heartbeat + estadísticas + alertas de caída + recompensas avanzadas |

El servidor se activa en minutos sin instalar nada; el plugin propio es una
mejora opcional que desbloquea lo diferencial.

## Justificación

Es la diferencia entre pedir al cliente que instale algo **antes** de ver
valor, y darle valor **antes** de pedirle que instale nada. Ataca directamente
la métrica que más importa al inicio: la tasa de activación.

## Pendiente antes de pasar a Vigente

1. Verificar el estado actual del protocolo (v1 basado en RSA, v2 basado en
   token/HMAC) y de las implementaciones vigentes
2. Revisar licencias para asegurar compatibilidad legal con una implementación
   propia del lado emisor
3. Confirmar que la vía Votifier no permite eludir controles antifraude

## Consecuencias

- El código específico de Votifier vive en un **adaptador aislado**, nunca en
  el núcleo del dominio (coherente con la regla de no incrustar Minecraft en el
  modelo — ver las convenciones del proyecto)
- Los servidores integrados solo por Votifier no aportan heartbeat: los
  jugadores conectados se miden igualmente por ping directo (ADR-0009)
- Las funciones que requieren el plugin propio deben estar claramente marcadas
  en el panel, como incentivo natural a instalarlo
