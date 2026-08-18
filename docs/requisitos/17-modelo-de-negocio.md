# Requisito 17 — Business Platform / Monetization / Growth Strategy

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** modelo de negocio — sin código

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 17

BUSINESS PLATFORM

MONETIZATION

GROWTH STRATEGY

BUSINESS MODEL

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define el modelo de negocio completo de la plataforma.

NO escribir código.

NO diseñar interfaces.

NO implementar pagos.

NO crear campañas.

Tu misión consiste en diseñar una empresa sostenible y escalable.

Piensa como:

Chief Product Officer. Chief Revenue Officer. Chief Strategy Officer.
Principal Product Manager.

================================================================================
VISIÓN
================================================================================

La plataforma debe convertirse en la infraestructura principal para la administración de servidores de Minecraft.

No depender de una única fuente de ingresos.

Diversificar. Escalar. Crear efectos de red.

================================================================================
FUENTES DE INGRESOS
================================================================================

Analiza y diseña.

Suscripciones. Subastas de posiciones. Publicidad. Marketplace.
Venta de extensiones. Comisiones. Planes Enterprise. API Premium.
Analytics Premium. White Label. Licencias. Partners.
Programa de afiliados. Patrocinios. Créditos prepago.

================================================================================
MODELO FREEMIUM
================================================================================

Define claramente.

Free. Starter. Pro. Business. Enterprise.

Para cada plan define.

Límites. Características. Soporte. SLA. Capacidad.

================================================================================
SUBASTAS
================================================================================

Diseña completamente.

Modelo económico. Incremento mínimo. Reglas. Pagos. Incumplimiento.
Sanciones. Empates. Calendario. Reservas. KPIs.

================================================================================
PUBLICIDAD
================================================================================

Diseña espacios.

Ranking. Banner. Patrocinios. Promociones. Featured Servers.
Eventos especiales.

================================================================================
MARKETPLACE
================================================================================

Modelo económico.

Revenue Share. Comisiones. Promociones. Verificados. Reembolsos.
Resolución de disputas.

================================================================================
PARTNER PROGRAM
================================================================================

Diseña.

Partners. Resellers. Hosting Partners. Agencias. Influencers.
Desarrolladores.

================================================================================
AFILIADOS
================================================================================

Programa completo.

Tracking. Cookies. Comisiones. Fraude. Pagos.

================================================================================
FACTURACIÓN
================================================================================

Diseña.

Facturas. Impuestos. IVA. Monedas. Regiones. Renovaciones. Prorrateo.

================================================================================
WALLET
================================================================================

Diseña un sistema de saldo.

Créditos. Bonificaciones. Promociones. Gift Cards.

================================================================================
PRECIOS
================================================================================

Diseña una estrategia.

Psicología de precios. Regionalización. Descuentos. Cupones. Promociones.
Bundles.

================================================================================
RETENCIÓN
================================================================================

Diseña estrategias.

Onboarding. Gamificación. Logros. Insignias. Misiones. Niveles.
Programas de fidelidad.

================================================================================
NETWORK EFFECTS
================================================================================

¿Cómo hacer que cada nuevo servidor incremente el valor de la plataforma?

¿Cómo hacer que los desarrolladores quieran crear extensiones?

¿Cómo incentivar a los jugadores?

================================================================================
MÉTRICAS
================================================================================

Define.

MRR. ARR. LTV. CAC. Churn. ARPU. NPS. North Star Metric.
Activation Rate. Conversion Rate.

================================================================================
ANALYTICS
================================================================================

Define dashboards ejecutivos.

Ingresos. Conversión. Funnel. Retención. Marketplace. Partners.

================================================================================
GO TO MARKET
================================================================================

Diseña el lanzamiento.

Alpha. Beta. Early Access. Open Launch. Enterprise.

================================================================================
EXPANSIÓN
================================================================================

Analiza.

Otras regiones. Otros idiomas. Otros juegos. Otras plataformas.

================================================================================
RIESGOS
================================================================================

Analiza.

Dependencia de ingresos. Competencia. Fraude. Regulación. Pasarelas.

================================================================================
ROADMAP
================================================================================

Diseña una estrategia de crecimiento.

12 meses. 24 meses. 36 meses. 60 meses.

================================================================================
OUTPUT
================================================================================

No escribir código.

Quiero un documento equivalente al plan estratégico de una startup respaldada por inversión.

Debe incluir:

Business Model Canvas. Lean Canvas. SWOT. Porter's Five Forces.
Unit Economics. Pricing Strategy. Revenue Forecast. Growth Loops.
Roadmap. KPIs. Riesgos.

Y recomendaciones para construir una empresa rentable y sostenible.

---

## 🔴 Tensión central del negocio (no identificada en ningún documento previo)

### Vender posiciones destruye el activo que se está vendiendo

En el documento de arranque, el CTO estableció que **el producto real es la
credibilidad del ranking**: si el ranking no correlaciona con la calidad del
servidor, los jugadores dejan de usar el directorio y la plataforma pierde
todo su valor.

Este documento propone monetizar exactamente eso:

- Subastas de **posiciones**
- Publicidad en el **ranking**
- **Featured Servers**

Es la contradicción estructural del negocio: **el ranking vale porque es
honesto, y la forma más directa de monetizarlo es dejar de serlo.**

Consecuencia si se mezcla:

```
Se venden los primeros puestos → el ranking deja de reflejar calidad
→ los jugadores dejan de confiar → cae el tráfico
→ las posiciones dejan de valer → el ingreso desaparece
```

El activo se consume a sí mismo.

### Resolución: separación visual estricta

Es un problema resuelto en la industria (buscadores, tiendas de aplicaciones,
marketplaces). La regla es una sola:

> **Lo patrocinado nunca se mezcla con lo orgánico. Va en una zona propia,
> visualmente distinta y etiquetada como patrocinado.**

| Modelo | Efecto |
|---|---|
| Posiciones compradas dentro del ranking orgánico | 🔴 Destruye la confianza. Ingresos decrecientes. |
| **Bloque de destacados separado + ranking orgánico intacto** | ✅ Ambos activos conservan su valor |

El ranking orgánico es lo que atrae al jugador; el bloque patrocinado es lo
que se vende al dueño del servidor. Uno alimenta al otro **solo si están
separados**.

---

## Notas del CTO sobre este requisito

### Objeción 1 — Quince fuentes de ingresos para cero clientes

El documento pide diseñar 15 vías de ingreso simultáneas. Diversificar antes
de tener el primer euro invierte el orden correcto: **la diversificación es
una defensa de un negocio que ya funciona, no un método para arrancarlo.**

Cada fuente de ingresos tiene coste fijo permanente: lógica de facturación,
impuestos, soporte, fraude, disputas y reembolsos. Quince fuentes con cero
clientes son quince sistemas que mantener y ningún ingreso.

**Secuencia recomendada:**

| Fase | Fuente | Motivo |
|---|---|---|
| 1 | **Destacados / patrocinios** (precio fijo) | El cliente ya está motivado: quiere visibilidad. Venta más simple. |
| 2 | **Suscripción** (estadísticas avanzadas, más servidores, alertas) | Ingreso recurrente y predecible |
| 3 | Subastas de posiciones destacadas | Solo cuando haya demanda que supere la oferta de espacios |
| 4+ | Marketplace, afiliados, white label, enterprise | Cuando exista base de clientes |

### Objeción 2 — El Wallet es un riesgo regulatorio, no una funcionalidad

"Créditos prepago", "wallet", "gift cards" significan **retener dinero de los
clientes**. Implicaciones:

| Aspecto | Consecuencia |
|---|---|
| Regulatorio | Retener saldo de terceros puede caer bajo normativa de dinero electrónico según jurisdicción |
| Contable | El saldo no consumido es **pasivo diferido**, no ingreso |
| Operativo | Reembolsos, caducidad de saldo, saldos huérfanos, disputas |
| Fiscal | El momento de devengo del ingreso deja de ser evidente |

**Recomendación:** sin wallet. Cobro por transacción o suscripción vía
pasarela. El dinero nunca se queda en la plataforma.

Mismo razonamiento para **marketplace con revenue share** y **programa de
afiliados**: ambos implican **pagar dinero a muchas personas físicas en
distintos países**, lo que arrastra verificación de identidad, retenciones e
informes fiscales. Aplazados.

### Elementos de alto valor del documento

| Elemento | Valoración |
|---|---|
| **Unit Economics** (CAC, LTV, ARPU) | ✅ Correcto y necesario. Se conecta directamente con el "coste por servidor" del Requisito 16: sin ese dato no se puede fijar el precio ni el límite del plan gratuito. |
| **Activation Rate** | ✅ Es la métrica que más importa al inicio, coherente con el embudo del Requisito 11 |
| **North Star Metric** | ✅ Propuesta del CTO: *votos legítimos entregados con éxito por semana*. Combina uso real, integridad y funcionamiento técnico en un solo número. |
| **Regionalización de precios** | ✅ **Crítico en este mercado.** Un precio pensado para Estados Unidos es inviable en gran parte de Latinoamérica. Refuerza la decisión del Requisito 15 sobre pasarelas locales (Mercado Pago, PSE). |
| Go To Market por fases (alpha cerrada → beta → abierto) | ✅ Correcto y ya alineado con el Requisito 09 |

### Elemento con consecuencia arquitectónica: "otros juegos"

La sección EXPANSIÓN menciona expandirse a otros juegos. Es la idea de mayor
recorrido del documento, y tiene una consecuencia técnica **barata hoy y cara
mañana**:

> No incrustar "Minecraft" en el modelo de dominio.

Conceptos como versión del servidor, identidad del jugador, protocolo de ping
y tipo de recompensa deben modelarse como propiedades de un **tipo de juego**,
no como campos fijos. El coste hoy es casi nulo; hacerlo después implica
migrar el esquema completo.

⚠️ Advertencia asociada: la compatibilidad con **Votifier** (Requisito 13) es
específica de Minecraft. Debe vivir en un adaptador aislado, no filtrarse al
núcleo del dominio.

### Riesgo no mencionado en el documento

La sección RIESGOS omite el más relevante: **las directrices comerciales de
Mojang/Microsoft**, que limitan qué pueden vender los servidores de Minecraft.
Afectan directamente al marketplace y a las recompensas monetizables. Ya
señalado en el documento de arranque del CTO y todavía sin resolver.
