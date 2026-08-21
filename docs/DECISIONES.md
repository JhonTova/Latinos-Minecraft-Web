# Decisiones vigentes — LatinosMC

> Consolidación de los 19 documentos de `docs/requisitos/` en un único
> conjunto de decisiones sin contradicciones.
>
> **Última actualización:** 2026-08-03

Los documentos de requisitos son **material de entrada**, no la especificación
final. Se escribieron de forma independiente y contienen solapamientos y
contradicciones (documentadas abajo). Este archivo es lo que manda.

---

## 1. Índice de ADRs

| ADR | Decisión | Estado |
|---|---|---|
| [0001](decisiones/0001-monolito-modular.md) | Monolito modular, despliegue único | ✅ Vigente |
| [0002](decisiones/0002-postgresql.md) | PostgreSQL como almacén primario | ✅ Vigente |
| [0003](decisiones/0003-postgres-como-cola.md) | PostgreSQL como cola (Outbox), sin Kafka ni RabbitMQ | ✅ Vigente |
| [0004](decisiones/0004-pagos-sin-tarjetas.md) | Nunca tocar datos de tarjeta | ✅ Vigente |
| [0005](decisiones/0005-antifraude-core.md) | Antifraude es dominio core desde fase 1 | ✅ Vigente |
| [0006](decisiones/0006-openapi-fuente-de-verdad.md) | OpenAPI + AsyncAPI como fuente de verdad | ✅ Vigente |
| [0007](decisiones/0007-deuda-tecnica.md) | Política de deuda técnica | ✅ Vigente |
| [0008](decisiones/0008-java-21.md) | Java 21 LTS para backend y plugin | ✅ Vigente |
| [0009](decisiones/0009-plugin-no-es-fuente-de-verdad.md) | El plugin no es fuente de verdad | ✅ Vigente |
| [0010](decisiones/0010-retencion-y-borrado.md) | Retención de datos y derecho de supresión | ✅ Vigente |
| [0011](decisiones/0011-ventana-de-voto.md) | Ventana deslizante de 24 h en UTC | ✅ Vigente |
| [0012](decisiones/0012-comandos-sin-concatenacion.md) | Comandos de Minecraft con lista blanca | ✅ Vigente |
| [0013](decisiones/0013-slots-destacados.md) | Venta de slots a precio fijo (sustituye subastas) | ✅ Vigente |
| [0014](decisiones/0014-votifier.md) | Compatibilidad con Votifier | 🟡 Propuesto |
| [0015](decisiones/0015-antifraude-lee-tabla-votos.md) | Deuda deliberada: el antifraude lee la tabla de votos | ✅ Vigente |
| [0016](decisiones/0016-token-de-voto.md) | El jugador nunca escribe su nick: token de un solo uso | 🟡 Propuesto |
| [0017](decisiones/0017-imagenes-subidas.md) | Imágenes subidas: nada de SVG, todo reprocesado | ✅ Vigente |
| [0018](decisiones/0018-paypal-como-pasarela-principal.md) | PayPal como pasarela principal, Stripe como alternativa | ✅ Vigente |
| [0019](decisiones/0019-lecturas-sql-entre-modulos.md) | Deuda deliberada: lecturas SQL de solo lectura entre módulos | ✅ Vigente |
| [0020](decisiones/0020-codigo-de-voto-escribible.md) | El token de voto pasa a ser un código escribible a mano | ✅ Vigente (longitud modificada por [0026](decisiones/0026-codigo-de-voto-de-ocho-caracteres.md)) |
| [0021](decisiones/0021-reintento-de-eventos-de-pago.md) | Un evento de pago solo se da por hecho cuando lo está | ✅ Vigente |
| [0022](decisiones/0022-identidad-del-jugador-en-el-plugin.md) | La identidad del jugador viaja con su tipo, también en el plugin | ✅ Vigente |
| [0023](decisiones/0023-la-ventana-de-voto-es-de-la-plataforma.md) | La ventana entre votos la fija la plataforma, no el dueño del servidor | ✅ Vigente |
| [0024](decisiones/0024-retirada-de-servidores-inactivos.md) | Los servidores que dejan de dar señales se retiran del directorio | ✅ Vigente |
| [0025](decisiones/0025-paginas-por-categoria.md) | Una página propia por modalidad, no un filtro | ✅ Vigente |
| [0026](decisiones/0026-codigo-de-voto-de-ocho-caracteres.md) | El código de voto baja a ocho caracteres | ✅ Vigente |
| [0027](decisiones/0027-imagenes-automaticas-desde-el-ping.md) | Icono y banner automáticos, leídos del ping del servidor | ✅ Vigente |

---

## 2. Alcance de la v1

### Se construye

| Módulo | Nivel |
|---|---|
| Identidad y acceso (registro, login, sesiones, MFA) | 🔴 Crítico |
| Organizaciones, miembros, roles y permisos | 🔴 Crítico |
| Directorio de servidores (registro, verificación, edición) | 🟡 Estándar |
| **Motor de votos** | 🔴 Crítico |
| **Antifraude (Trust & Abuse)** | 🔴 Crítico |
| Motor de ranking | 🟡 Estándar |
| Entrega de recompensas (webhooks + plugin) | 🔴 Crítico |
| Plugin Paper/Velocity | 🔴 Crítico |
| Auditoría | 🔴 Crítico |
| Analíticas básicas | 🟢 Ligero |
| Notificaciones (Discord + email) | 🟢 Ligero |
| Slots destacados de pago | 🟡 Estándar |
| Panel del propietario | 🟢 Ligero |
| Panel de administración | 🟢 Ligero |
| API pública + OpenAPI | 🟡 Estándar |

### No se construye en v1

Subastas · Marketplace · Motor de automatizaciones (Zapier) · Framework de
extensiones con sandbox · Wallet y créditos prepago · Afiliados · Reparto de
ingresos · SDKs más allá de Java · Kubernetes · Kafka/RabbitMQ · SOC/SIEM ·
Chaos engineering · Certificaciones SOC 2 / ISO 27001 · Aplicación móvil ·
CLI · White label · LDAP/SAML

---

## 3. Contradicciones detectadas y cómo se resuelven

| # | Contradicción | Origen | Resolución |
|---|---|---|---|
| 1 | "Nunca eliminar información" vs. derecho al olvido | Docs 04, 06 | ADR-0010: se conserva el hecho, se anonimiza a la persona |
| 2 | "Protección contra plugins modificados" (imposible) | Doc 06 | ADR-0009: el plugin no es fuente de verdad |
| 3 | 11 APIs que son 3 | Doc 05 | Pública, Plugin y Admin. Webhooks es tráfico saliente. |
| 4 | 32 "módulos" | Doc 03 | 10-12 módulos de dominio reales |
| 5 | "Boring Technology First" + 60 tecnologías | Doc 08 | Filtro: "¿sin esto, esta semana, algo se rompe?" |
| 6 | 20 secciones en el panel vs. "3 clics" | Doc 12 | 7 entradas de navegación |
| 7 | Subastas/Automatización "Core" tras haberse aplazado | Doc 15 | Fuera de v1. Slots a precio fijo (ADR-0013). |
| 8 | Runbooks de RabbitMQ y Redis, descartados en el 08 | Doc 16 | Se eliminan |
| 9 | Vender posiciones destruye el ranking | Doc 17 | ADR-0013: separación estricta patrocinado/orgánico |
| 10 | 4 documentos de estándares de ingeniería (07, 08, 19, 20) | — | Fusionados en las convenciones del proyecto |
| 11 | 99,99% de disponibilidad con 1 persona | Doc 16 | SLO interno 99,5%, sin SLA público |
| 12 | "UI Last" vs. objetivo declarado del usuario | Doc 09 | Rebanadas verticales, la UI entra desde el hito 1 |
| 13 | Escala (20M usuarios, 500M votos) sin fundamento | Docs 03, 04, 13 | Diseñar para 10× el objetivo realista a 24 meses |
| 14 | Java 25 backend + Java 21 plugin | Doc 08 | ADR-0008: Java 21 en todo |

---

## 4. Decisiones pendientes (bloquean diseño)

### 4.1 🔴 ¿Se aceptan servidores en offline-mode (pirata)?

**Por qué bloquea:** en offline-mode el UUID del jugador es un hash de un
nombre que cualquiera puede escribir. La identidad del votante es
criptográficamente inexistente, y los nombres pueden contener caracteres
arbitrarios (vector de inyección — ver ADR-0012).

**Supuesto de trabajo mientras no se resuelva:**
Se soportan ambos, pero con **modelos de confianza separados**: la identidad se
modela como `(tipo_de_identidad, valor)` en lugar de asumir UUID de Mojang, y
el antifraude aplica reglas distintas por tipo. Los rankings podrán segregarse
sin cambios de esquema.

### 4.2 🔴 Nombre definitivo, dominio y paleta de marca

**Por qué bloquea:** los tokens de color son el ~90% del resultado visual.

**Supuesto de trabajo:** se implementan los tokens como variables CSS con una
paleta provisional neutra. Cambiar la marca después será modificar un archivo
de tokens, no rehacer pantallas.

### 4.3 🟡 ¿La plataforma es el sitio de votación o agrega votos de otros?

**Supuesto de trabajo:** la plataforma **es** el sitio de votación (lectura A).
Es lo que indican los documentos 02, 11 y 12. El modelo de datos incluye
`vote_source` para no cerrar la puerta a la agregación futura.

### 4.4 🟡 Directrices comerciales de Mojang/Microsoft

Afectan a qué pueden vender los servidores y, por tanto, al marketplace y a las
recompensas monetizables. **Debe leerse antes** de diseñar cualquier
funcionalidad que involucre venta de contenido de juego. No bloquea la v1.

---

## 5. Objetivos de escala (revisados)

Sustituyen a las cifras de los documentos 03, 04 y 13.

| Métrica | Objetivo 24 meses | Diseñar para (10×) |
|---|---|---|
| Servidores registrados | 500 | 5.000 |
| Servidores con plugin conectado | 300 | 3.000 |
| Votos / día | 20.000 | 200.000 |
| Usuarios registrados | 50.000 | 500.000 |

**Nota crítica de coste:** los heartbeats dominan el tráfico. Con 3.000
servidores latiendo cada 60 s son 4,3 M peticiones/día, frente a 200.000 votos.
El intervalo de heartbeat es la decisión que más afecta al coste de
infraestructura → intervalo adaptativo y payload mínimo.

---

## 6. Métricas oficiales

**North Star Metric:** votos legítimos entregados con éxito por semana.

| Métrica | Por qué |
|---|---|
| Tasa de activación (registro → primer voto entregado) | Predice todo lo demás |
| Tiempo hasta conectar el servidor | El punto donde se pierde a la gente |
| % de votos rechazados por antifraude | Salud del ranking |
| Coste por servidor / mes | Decide el precio y los límites del plan gratuito |
| MRR | Ingreso recurrente |

**Descartadas por no ser medibles con un equipo de una persona:** velocity,
developer happiness.

---

## 7. Fiabilidad

| Fase | SLO interno | SLA público |
|---|---|---|
| VPS único, 1 persona | **99,5%** | **Ninguno** |
| Con redundancia básica | 99,9% | Ninguno |
| Con equipo y guardias | 99,9% | Solo plan Enterprise |

Principio operativo con un solo operador: el objetivo no es recuperarse rápido,
sino que **los fallos sean sobrevivibles sin intervención humana inmediata**
(reinicio automático, degradación elegante, colas que absorben caídas).
