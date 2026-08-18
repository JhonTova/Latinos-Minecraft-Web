# Requisito 11 — UX Flows, Product Journeys & Service Blueprint

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** experiencia de usuario — sin pantallas ni código

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 11

UX FLOWS
PRODUCT JOURNEYS
&
SERVICE BLUEPRINT

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define toda la experiencia funcional del producto.

NO diseñes interfaces.

NO escribas código.

NO implementes páginas.

NO crees componentes.

Tu única misión consiste en diseñar cómo interactúan las personas con la plataforma.

Piensa como un Principal UX Architect de:

Stripe

Notion

Linear

GitHub

Atlassian

Figma

Slack

Cada flujo debe minimizar:

Tiempo

Errores

Fricción

Confusión

Clicks innecesarios

Carga cognitiva

================================================================================
FILOSOFÍA
================================================================================

Todo usuario debe poder completar una tarea importante sin leer documentación.

La plataforma debe sentirse:

Intuitiva

Predecible

Rápida

Consistente

Profesional

================================================================================
TIPOS DE USUARIO
================================================================================

Analiza todos los perfiles.

Visitante

Jugador

Administrador de servidor

Propietario de organización

Moderador

Soporte

Administrador global

Partner

Developer

Enterprise Customer

Para cada uno define:

Objetivos

Necesidades

Problemas

Frecuencia de uso

Nivel técnico

================================================================================
PERSONAS
================================================================================

Construye personas completas.

Nombre. Contexto. Conocimientos. Motivaciones. Frustraciones. Objetivos.
KPIs personales.

================================================================================
USER JOURNEY MAPS
================================================================================

Diseña journeys completos para:

Registro. Login. Creación de organización. Invitar miembros.
Registrar servidor. Verificar servidor. Instalar plugin.
Configurar recompensas. Generar API Key. Activar Webhooks.
Participar en subastas. Pagar. Renovar plan. Consultar analytics.
Solicitar soporte. Eliminar organización. Migrar servidor.

================================================================================
SERVICE BLUEPRINT
================================================================================

Para cada Journey genera un Service Blueprint completo.

Acciones del usuario. Acciones visibles del sistema. Procesos internos.
Procesos automáticos. Dependencias. Eventos. Integraciones.

================================================================================
ONBOARDING
================================================================================

Diseña un onboarding profesional.

Primer Login. Checklist. Tutorial interactivo. Configuración inicial.
Servidor de prueba. Buenas prácticas. Centro de ayuda.

================================================================================
INFORMACIÓN
================================================================================

Diseña la Information Architecture.

Navegación. Jerarquía. Agrupación. Etiquetas. Búsqueda. Accesos rápidos.

================================================================================
TASK FLOWS
================================================================================

Diseña Task Flows para:

Crear servidor. Editar servidor. Eliminar servidor. Ver estadísticas.
Configurar votos. Gestionar pagos. Administrar organización.
Gestionar miembros. Gestionar API. Configurar plugin. Configurar Discord.
Gestionar subastas.

================================================================================
ERROR FLOWS
================================================================================

Diseña flujos para:

Pago fallido. Plugin desconectado. Servidor offline. OAuth expirado.
API Key revocada. Subasta cancelada. Webhook fallido. Error interno.

================================================================================
EMPTY STATES
================================================================================

Diseña experiencias para.

Sin servidores. Sin votos. Sin pagos. Sin analytics. Sin miembros.
Sin organizaciones. Sin actividad.

================================================================================
NOTIFICACIONES
================================================================================

Diseña el sistema.

Centro de notificaciones. Correo. Discord. Web Push. In-App. Prioridades.
Agrupación. Acciones rápidas.

================================================================================
SEARCH EXPERIENCE
================================================================================

Diseña una búsqueda global.

Comandos. Filtros. Recientes. Favoritos. Resultados rápidos.

================================================================================
COMMAND PALETTE
================================================================================

Diseña una Command Palette estilo Linear.

Acciones. Navegación. Configuración. Búsqueda. Atajos.

================================================================================
KEYBOARD FIRST
================================================================================

Diseña atajos globales.

Navegación. Edición. Búsqueda. Guardado.

================================================================================
MOBILE EXPERIENCE
================================================================================

Analiza.

Qué funciones deben estar disponibles.

Qué funciones deben simplificarse.

Qué funciones no deberían existir.

================================================================================
DASHBOARD EXPERIENCE
================================================================================

Diseña la experiencia.

Widgets. Resumen. Actividad. Alertas. Acciones rápidas. KPIs.

================================================================================
SETTINGS EXPERIENCE
================================================================================

Diseña un sistema de configuración consistente.

General. Seguridad. Facturación. API. Plugin. Notificaciones. Organización.

================================================================================
BILLING EXPERIENCE
================================================================================

Diseña.

Planes. Facturas. Métodos de pago. Historial. Renovaciones. Cupones.

================================================================================
AUCTION EXPERIENCE
================================================================================

Diseña toda la experiencia.

Registro. Participación. Pujas. Tiempo real. Ganador. Pago. Cancelación.

================================================================================
SUPPORT EXPERIENCE
================================================================================

Diseña.

Centro de ayuda. FAQ. Documentación. Tickets. Estado. Incidencias.

================================================================================
ADMIN EXPERIENCE
================================================================================

Diseña la experiencia para administradores globales.

Moderación. Auditoría. Fraude. Analytics. Soporte.

================================================================================
DEVELOPER EXPERIENCE
================================================================================

Diseña el recorrido de un desarrollador.

Crear API Key. Leer documentación. Probar API. Instalar SDK.
Webhook Tester. Sandbox.

================================================================================
EDGE CASES
================================================================================

Analiza escenarios extremos.

Miles de organizaciones. Usuarios bloqueados. Pagos duplicados.
Servidor eliminado. Cambio de propietario. Fallo durante una subasta.

================================================================================
METRICS
================================================================================

Define métricas UX.

Tiempo para registrar servidor. Tiempo para configurar plugin.
Tiempo para reclamar voto. Tiempo para comprar plan.
Tiempo para integrar API.

================================================================================
DIAGRAMAS
================================================================================

Genera.

User Journey Maps. Service Blueprints. Task Flows. Sequence Diagrams. Mermaid.

================================================================================
ENTREGA
================================================================================

No diseñar pantallas.

No implementar frontend.

No escribir código.

Quiero un documento completo que defina la experiencia de usuario de toda la plataforma.

Cada flujo debe incluir:

Objetivo. Actores. Precondiciones. Pasos. Alternativas. Errores.
Recuperación. Eventos. Integraciones. KPIs.

Recomendaciones de mejora.

Si detectas oportunidades para reducir pasos, automatizar tareas o mejorar la experiencia, intégralas y justifícalas.

---

## Notas del CTO sobre este requisito

### Valoración

Documento sólido. Dos aciertos destacables:

1. **Los flujos de error tienen categoría propia.** La mayoría de las
   especificaciones de producto documentan solo el camino feliz. Aquí se
   exigen explícitamente: pago fallido, plugin desconectado, servidor caído,
   OAuth caducado, webhook fallido. Es señal de madurez.

2. **Las métricas de UX son métricas de activación reales.** "Tiempo para
   configurar el plugin" no es una métrica cosmética: es el predictor
   principal de si un servidor se queda o abandona.

### El hallazgo más importante: el embudo de activación decide el negocio

De los 17 journeys listados, cinco forman una sola secuencia que determina si
la plataforma tiene clientes:

```
registrarse → crear organización → registrar servidor →
verificar propiedad → instalar plugin → configurar recompensas →
primer voto real recibido
```

**Esa secuencia es el producto.** Todo lo demás (subastas, marketplace,
facturación, soporte) solo importa si esta se completa.

#### Dónde se pierde a los usuarios: la instalación del plugin

Es el único paso que ocurre **fuera de la plataforma**, en infraestructura
ajena, sin visibilidad ni feedback:

| Paso | Dónde ocurre | Riesgo |
|---|---|---|
| Descargar el JAR | Navegador | Bajo |
| Subirlo al servidor (FTP/panel) | Infraestructura del cliente | 🔴 Alto — muchos no saben hacerlo |
| Pegar la API Key en `config.yml` | Editor de texto remoto | 🔴 Alto — errores de formato YAML |
| Reiniciar el servidor | Consola del cliente | 🟡 Medio — implica downtime para sus jugadores |
| Comprobar que funciona | Ninguna parte | 🔴 **Sin feedback: aquí abandonan** |

**Recomendación de diseño (ventaja competitiva real):** fusionar verificación
de propiedad e instalación del plugin en un solo paso, y dar **confirmación en
vivo**. Es decir: el panel muestra "esperando conexión del plugin…" y cambia
solo a "✅ Plugin conectado — servidor verificado" en cuanto llega el primer
heartbeat.

Ese único detalle —cerrar el bucle de feedback en el momento exacto de máxima
incertidumbre del usuario— convierte más que cualquier tutorial. Además
resuelve dos journeys con un solo flujo: la conexión del plugin **es** la
prueba de propiedad del servidor.

### Corrección 1 — Priorización de flujos

El documento pide ~40 flujos detallados (17 journeys + 12 task flows + 8 error
flows + un service blueprint por cada journey).

| Prioridad | Flujos | Justificación |
|---|---|---|
| 🔴 **Crítica** | Embudo de activación completo (7 pasos) + votar y reclamar recompensa | Es el producto |
| 🟡 **Alta** | Errores: plugin desconectado, servidor caído, webhook fallido | Ocurren a diario |
| 🟢 **Media** | Organizaciones, miembros, API Keys, analytics | Necesarios, no bloqueantes |
| ⚪ **Aplazada** | Subastas, marketplace, facturación, tickets | Funcionalidades aún no construidas |

### Corrección 2 — Las personas inventadas no aportan información

Construir personas ficticias ("Carlos, 24 años, administrador de servidor")
sin haber hablado con ningún dueño de servidor real produce una descripción de
lo que ya se suponía. No añade conocimiento nuevo; lo formaliza.

**Sustituto de mayor valor:** conversaciones con 5 dueños de servidores
reales. Preguntas concretas: qué plataforma de votos usan hoy, qué les
frustra, cuánto tardaron en configurarla, si han sufrido fraude de votos, qué
pagarían. Eso sí genera información que ningún documento puede inventar.

### Corrección 3 — Command Palette y "Keyboard First" están mal priorizados para esta audiencia

La Command Palette estilo Linear es excelente… para usuarios profesionales que
viven dentro de una herramienta 8 horas al día. La audiencia real de esta
plataforma son dueños de servidores de Minecraft que entrarán al panel unos
minutos a la semana, muchos desde el móvil, y con perfil técnico muy variable.

| Función | Valor para esta audiencia |
|---|---|
| **Experiencia móvil impecable** | 🔴 Alto — consultarán estadísticas desde el teléfono |
| Estados vacíos y de error claros | 🔴 Alto |
| Notificaciones por **Discord** | 🔴 Alto — Discord es el canal nativo de esta comunidad, más que el email |
| Command Palette | 🟢 Bajo — pulido, no esencial |
| Atajos de teclado globales | 🟢 Bajo |

Observación adicional: el documento lista Discord entre los canales de
notificación, y es probablemente **el más importante de todos** en este
ecosistema. Merece tratamiento de primera clase, no de canal secundario.
