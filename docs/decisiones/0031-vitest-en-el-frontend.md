# ADR-0031 — Vitest para la lógica del frontend

- **Estado:** ✅ Vigente
- **Fecha:** 2026-11-12
- **Relacionado:** ADR-0007 (deuda técnica), `docs/agentes/README.md` (sistema de agentes)

## Contexto

El frontend no tenía ninguna prueba automática. Un cambio se daba por bueno con
`npm run lint`, `npm run build` y una revisión a ojo en el navegador. Eso alcanza
para lo visual, pero hay dos piezas del frontend donde un error no se ve:

- **El puente `/api/backend`.** Su lista blanca es lo único que impide que la web
  sea un proxy abierto hacia el backend. Un patrón demasiado ancho no rompe
  nada a la vista: simplemente deja alcanzar rutas que no debían salir.
- **La IP que se reenvía al backend.** Con ella se decide si un voto cuenta. Si
  se lee mal, el cupo por IP deja de filtrar.

Además, el sistema de agentes separa quien escribe las pruebas de quien escribe
el código. En el frontend esa separación no podía existir sin un ejecutor de
pruebas.

`frontend/AGENTS.md` prohíbe agregar dependencias sin decidirlo, así que esta
decisión se registra aquí.

## Decisión

Se agrega **Vitest** como dependencia de desarrollo del frontend, con este
alcance:

- Se prueba la **lógica sin interfaz**: `src/lib/**` y el puente. Los archivos
  de prueba son `src/**/*.test.ts` y viven junto al archivo que prueban.
- El entorno es `node`. No se agrega `jsdom`, Testing Library ni un ejecutor de
  navegador.
- `npm run test` ejecuta `vitest run` (una pasada, sin modo observador) y la CI
  lo corre en el trabajo `frontend`, entre el lint y la compilación.
- No se fija un umbral de cobertura en el frontend.

Para que el puente se pueda probar sin levantar Next, la lista blanca y la
lectura de la IP salen de `route.ts` a `src/lib/puente.ts` como funciones puras.
El comportamiento no cambia.

Vitest 5 pide `@types/node` 22 o superior, así que `@types/node` sube de `^20` a
`^22`. Es la versión de Node que ya usan `frontend/Dockerfile` y la CI.

## Alternativas descartadas

- **Jest.** Necesita configuración aparte para TypeScript y módulos ES. Vitest
  lee TypeScript sin transformadores adicionales.
- **El ejecutor de pruebas de Node (`node --test`).** No agrega dependencias,
  pero no resuelve el alias `@/` ni TypeScript con la sintaxis que usa el
  proyecto sin un cargador extra.
- **Pruebas de componentes con Testing Library.** Los paneles cambian seguido y
  esas pruebas se romperían con cada rediseño. La interfaz se verifica mirando
  la pantalla real (agente `verificador-ui`).
- **Playwright de extremo a extremo.** Necesita el backend y una base de datos
  en marcha; ninguna prueba del proyecto los tiene todavía (ADR-0021).

## Consecuencias

- El frontend tiene por primera vez una puerta automática para su parte de
  seguridad. Una ruta nueva en la lista blanca debe llegar con su prueba.
- Los componentes React siguen sin pruebas automáticas. Es un límite conocido.
- Una dependencia de desarrollo más que mantener al día. No llega a producción:
  la imagen del frontend solo copia el resultado de `next build`.
