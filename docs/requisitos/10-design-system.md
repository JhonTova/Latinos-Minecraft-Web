# Requisito 10 — Design System & Frontend Platform Specification

> **Origen:** documento de entrada del usuario
> **Recibido:** 2026-08-03
> **Estado:** guardado, pendiente de ejecutar
> **Tipo:** sistema de diseño — sin código, CSS ni componentes
> **Valoración del CTO:** ✅ Es el documento más cercano al objetivo declarado por el usuario ("una página bonita, funcional y profesional")

---

## Texto original del prompt (verbatim)

# MINECRAFT PLATFORM

DOCUMENT 10

DESIGN SYSTEM
&
FRONTEND PLATFORM SPECIFICATION

VERSION 1.0

================================================================================
OBJETIVO
================================================================================

Este documento define toda la experiencia visual y de interacción de la plataforma.

NO debes escribir código.

NO debes crear componentes React.

NO debes implementar páginas.

NO debes generar CSS.

NO debes escribir HTML.

Tu misión consiste en diseñar el sistema visual completo de la plataforma.

Piensa como un Staff Product Designer de:

Stripe

Linear

GitHub

Vercel

Cloudflare

Notion

Atlassian

Figma

Todo el frontend deberá construirse utilizando este documento como única fuente de verdad.

================================================================================
FILOSOFÍA
================================================================================

No estamos diseñando una web.

Estamos diseñando una plataforma SaaS profesional.

Debe transmitir:

Rapidez. Confiabilidad. Elegancia. Minimalismo. Escalabilidad. Claridad.
Consistencia.

Toda decisión visual deberá facilitar el trabajo del usuario.

Nunca distraerlo.

================================================================================
DESIGN PRINCIPLES
================================================================================

Define principios oficiales.

Consistency First. Accessibility First. Content First. Motion with Purpose.
Performance by Default. Responsive First. Dark Mode Native. Keyboard First.
Touch Friendly. Developer Friendly. AI Friendly.

================================================================================
DESIGN TOKENS
================================================================================

Diseña completamente.

Colores. Espaciados. Border Radius. Sombras. Tipografía. Alturas. Anchos.
Animaciones. Duraciones. Elevaciones. Opacidad. Gradientes. Iconografía.
Estados.

Todo deberá representarse mediante Design Tokens.

================================================================================
TEMAS
================================================================================

Diseña el sistema de temas.

Light. Dark. System. Brand Themes. High Contrast. Enterprise Themes.

Explica cómo cambiar dinámicamente de tema.

================================================================================
TIPOGRAFÍA
================================================================================

Selecciona tipografías.

Escalas. Pesos. Jerarquía. Line Heights. Letter Spacing.
Responsive Typography.

================================================================================
GRID SYSTEM
================================================================================

Diseña.

Containers. Breakpoints. Spacing. Columns. Responsive Behavior.

================================================================================
ICONOGRAFÍA
================================================================================

Selecciona un sistema.

Heroicons. Lucide. Phosphor. Tabler.

Analiza. Define convenciones.

================================================================================
COMPONENT LIBRARY
================================================================================

Diseña absolutamente todos los componentes.

Buttons. Inputs. Selects. Tables. Cards. Tabs. Badges. Dialogs. Modals.
Dropdowns. Tooltips. Toasts. Alerts. Skeletons. Progress. Charts. Calendar.
Date Picker. Code Block. Markdown Viewer. Avatar. Breadcrumb. Sidebar.
Navbar. Pagination. Search. Filters. Command Palette. File Upload.
Drag & Drop. Empty States. Error States. Loading States. Success States.
Warning States. Notification Center. Activity Feed.

Todo.

================================================================================
PATTERNS
================================================================================

Diseña patrones reutilizables.

CRUD. Dashboards. Analytics. Tables. Wizard. Settings. Billing.
Authentication. Onboarding. Marketplace. Forms. Search. Infinite Scroll.
Virtual Lists.

================================================================================
TABLAS
================================================================================

Diseña una tabla empresarial.

Sorting. Filtering. Grouping. Bulk Actions. Column Visibility. Export.
Selection. Pinned Columns. Virtualization. Responsive.

================================================================================
FORMULARIOS
================================================================================

Diseña completamente.

Validation. Async Validation. Autosave. Wizard. Sections. Undo.
Inline Errors. Optimistic UI.

================================================================================
DASHBOARDS
================================================================================

Diseña principios.

Widgets. KPIs. Cards. Metrics. Charts. Activity. Recent Events.
Quick Actions.

================================================================================
DATA VISUALIZATION
================================================================================

Selecciona estándares.

Charts. Heatmaps. Timelines. Treemaps. Pie. Line. Bar. Area. Radar. Geo.
Sparklines.

Explica cuándo utilizar cada uno.

================================================================================
MOTION
================================================================================

Diseña.

Transitions. Animations. Page Transitions. Hover. Focus. Loading. Skeletons.
Motion Guidelines.

================================================================================
ACCESIBILIDAD
================================================================================

Toda la plataforma deberá cumplir WCAG 2.2 AA como mínimo.

Analiza.

Keyboard Navigation. Screen Readers. Focus Management. Contrast. ARIA.
Reduced Motion.

================================================================================
RESPONSIVE
================================================================================

Diseña la estrategia.

Desktop. Tablet. Mobile. Ultra Wide.

================================================================================
MICROINTERACCIONES
================================================================================

Diseña.

Hover. Success. Error. Loading. Drag. Drop. Copy. Save. Delete.

================================================================================
ESTADOS
================================================================================

Toda pantalla deberá contemplar.

Loading. Empty. Error. Offline. Unauthorized. Forbidden. Maintenance.
No Results.

================================================================================
UX WRITING
================================================================================

Define reglas.

Mensajes. Errores. Confirmaciones. Tooltips. Ayuda. Feedback.

================================================================================
NAVEGACIÓN
================================================================================

Diseña.

Sidebar. Topbar. Breadcrumb. Search. Command Palette. Recent. Favorites.

================================================================================
PERFORMANCE
================================================================================

Diseña estrategias.

Lazy Loading. Prefetch. Streaming. Virtualization. Suspense. Memoization.

================================================================================
INTERNACIONALIZACIÓN
================================================================================

Diseña.

Idiomas. RTL. Pluralización. Fechas. Monedas. Zonas Horarias.

================================================================================
DESIGN QA
================================================================================

Antes de aprobar cualquier pantalla verificar.

Consistencia. Accesibilidad. Responsive. Performance. Legibilidad.
Contraste. Estados. Errores.

================================================================================
FIGMA
================================================================================

Define cómo debe organizarse el archivo.

Pages. Libraries. Variables. Components. Variants. Auto Layout. Dev Mode.

================================================================================
DOCUMENTACIÓN
================================================================================

Todo componente deberá incluir.

Objetivo. Props. Estados. Accesibilidad. Ejemplos. Buenas prácticas.
Casos de uso.

================================================================================
DESIGN GOVERNANCE
================================================================================

Diseña un proceso para aprobar nuevos componentes.

RFC. Review. Versionado. Deprecación. Migraciones.

================================================================================
OUTPUT
================================================================================

No crear componentes.

No crear CSS.

No implementar React.

Quiero una especificación completa del Design System.

Debe sentirse como el sistema de diseño oficial de una empresa como Stripe, GitHub o Atlassian.

Incluye:

Design Tokens. Diagramas. Wireframes conceptuales. Tablas. Matrices.
Buenas prácticas. Checklist. Roadmap del Design System.

Y cualquier recomendación que mejore la consistencia y escalabilidad del frontend.

---

## Notas del CTO sobre este requisito

### Valoración

Es el documento que **conecta directamente con el objetivo que el usuario
declaró en sus propias palabras**: una página bonita, funcional y profesional.
Junto con la elección de shadcn/ui + Tailwind del Requisito 08, es lo que
determinará si la plataforma parece profesional o parece amateur.

Elementos correctos sin reservas:

| Elemento | Valoración |
|---|---|
| Design tokens como base | ✅ Es la decisión que hace que todo se vea coherente |
| Dark mode nativo (no añadido después) | ✅ Correcto. Retrofitear dark mode es doloroso. |
| WCAG 2.2 AA | ✅ Correcto y exigible |
| Estados obligatorios por pantalla (loading, vacío, error, sin permisos) | ✅ **Muy buena práctica.** Es exactamente lo que separa una web amateur de una profesional: los estados vacíos y de error suelen olvidarse. |
| Motion with Purpose | ✅ Correcto |
| UX Writing como disciplina | ✅ Poco común y muy valioso |

### Corrección 1 — Los 37 componentes ya existen; no hay que diseñarlos

El documento pide diseñar "absolutamente todos los componentes" y lista 37.
Pero en el Requisito 08 ya se seleccionó **shadcn/ui**, que proporciona la
mayoría de ellos ya construidos, accesibles y probados.

Especificar de cero Button, Input, Select, Dialog, Tooltip, Tabs, Toast y
Pagination es rehacer trabajo terminado — y peor: hacerlo **sin implementarlo**
produce una especificación que nadie leerá porque el componente real ya viene
con su propia API.

**Dónde está el valor real (regla 90/10):**

| Esfuerzo | Impacto en "parecer profesional" |
|---|---|
| **Tokens: paleta, tipografía, escala de espaciado, radios, sombras** | 🔴 **~90% del resultado visual.** Es lo que hace que no parezca una plantilla genérica. |
| Estados vacíos, de error y de carga bien diseñados | 🔴 Alto |
| Densidad y ritmo vertical consistentes | 🔴 Alto |
| Especificación textual de 37 componentes | 🟢 Bajo — ya resuelto por la librería |

**Propuesta:** invertir el peso del documento. Tokens y personalidad visual
a fondo; componentes, solo los que shadcn **no** trae (tarjeta de servidor,
fila de ranking, widget de estado del servidor, tarjeta de recompensa,
selector de versión de Minecraft).

### Corrección 2 — Figma no aporta valor en este proyecto

El documento pide definir la organización del archivo de Figma (páginas,
librerías, variables, variants, Dev Mode).

No hay diseñador en el proyecto. Con shadcn/ui + Tailwind, los tokens **son**
código (variables CSS), y el flujo Figma → código introduce un paso de
sincronización manual permanente que nadie va a mantener.

**Recomendación:** omitir Figma. Definir los tokens directamente como
variables CSS y usar el propio navegador como herramienta de diseño. Si en el
futuro entra un diseñador, se genera la librería de Figma **desde** los tokens
existentes, no al revés.

### Corrección 3 — Los tipos de gráfico listados no encajan con el producto

Se piden 11 tipos: Charts, Heatmaps, Timelines, Treemaps, Pie, Line, Bar,
Area, Radar, Geo, Sparklines.

Lo que este producto realmente necesita:

| Gráfico | ¿Aplica? | Uso |
|---|---|---|
| **Línea / Área** | ✅ Sí | Votos a lo largo del tiempo, jugadores conectados |
| **Barras** | ✅ Sí | Comparativas entre servidores o periodos |
| **Sparkline** | ✅ Sí | Mini-tendencia dentro de la fila del ranking |
| **Heatmap** | ✅ Sí | Franjas horarias de votación — muy útil para el dueño |
| Geo | 🟡 Quizá | Origen de los votantes (además es señal antifraude) |
| Pie | ⚠️ Evitar | Difícil de leer con más de 3 categorías |
| Radar / Treemap / Timeline | ❌ No | Sin caso de uso real aquí |

Nota: al construir gráficos deberá aplicarse la guía de visualización de datos
del entorno antes de escribir la primera línea de código de un chart.

### Corrección 4 — RTL no tiene audiencia en este producto

El documento pide soporte RTL (idiomas de derecha a izquierda: árabe, hebreo).
El mercado objetivo, según el nombre del proyecto y el contexto, es
hispanohablante.

**Recomendación:** arquitectura preparada para i18n desde el día 1 (ningún
texto incrustado en el código, todo en archivos de traducción) con **español
e inglés**. RTL no se implementa: duplica el trabajo de maquetación de cada
pantalla para una audiencia inexistente.

### Decisión pendiente que este documento reabre

El documento habla de "Brand Themes" y "Enterprise Themes" pero **no existe
todavía una identidad de marca**: no hay nombre definitivo, ni logo, ni
paleta. El Requisito 01 pedía definir el nombre y no se ha ejecutado.

Sin esa decisión, los tokens de color no pueden fijarse — y son justamente el
90% del resultado visual.
