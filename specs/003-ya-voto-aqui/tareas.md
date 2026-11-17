# Tareas 003 — Avisar al plugin de que el jugador ya votó a este servidor

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md` — informe «completo» del 13/11/2026
- [x] T2 · `orquestador` · Aprobación de spec y plan, y respuestas P-1 a P-7. Se comprueba con la respuesta del desarrollador y el commit `docs(specs): ...` con los tres documentos — aprobados el 17/11/2026
- [x] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-5 y CA-7 en `CastVoteServiceTest`, en rojo. Se comprueba con `cd backend && mvn -B -pl voting -am test`: fallan las de CA-1 y CA-2 («un segundo antes»), porque hoy devuelve `puede = true`, y no por un error de compilación ni de configuración.
- [x] T4 · `pruebas-backend` · Pruebas de regresión de CA-6 (regla global) y `PluginControllerTest` para CA-8 (aprobada en P-2). Se comprueba con que pasan en verde ya con el código actual; se explica en el informe por qué es lo correcto.
- [x] T5 · `orquestador` · Commit `test(voting): ...` con T3 y T4.
- [ ] T6 · `dev-backend` · `puedeVotar` comprueba la ventana por servidor con la regla global desactivada, y se reemplaza el comentario citando ADR-0023. Se comprueba con `cd backend && mvn -B -pl voting -am test` en verde y `mvn -B -f backend/pom.xml verify` en verde (incluye `ArquitecturaTest` y el suelo de JaCoCo).
- [ ] T7 · `orquestador` · Commit `feat(voting): ...`.
- [ ] T8 · `revisor` · Revisión del diff completo de la rama, CA-9 y mutación con `mvn -B -Pmutacion -pl voting -am verify`. Clasifica los supervivientes de `puedeVotar` y lee la cobertura de `voting` contra el suelo `0.36`.
- [ ] T9 · `orquestador` · Cierre: Evidencia, correcciones y `pr.md` con la parada del contrato `/api/v1/plugin/*` en la primera línea. Sin push ni PR en esta sesión.

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo (voting) | `cd backend && mvn -B -pl voting -am test` (lo ejecutó `pruebas-backend`) | `Tests run: 46, Failures: 3, Errors: 0`, BUILD FAILURE. Fallan por aserción: CA-1 (esperado `[false, YA_VOTO_AQUI, PT18H]`, obtenido `[true, null, PT0S]`), CA-2 un segundo antes (esperado `[false, YA_VOTO_AQUI, PT1S]`, obtenido `[true, null, PT0S]`) y CA-5 consulta correcta (`Expecting value to be false but was true`). Las demás pruebas nuevas pasan, como se esperaba. |
| CA-8 (contrato) | `cd backend && mvn -B -pl app -am test -Dtest=PluginControllerTest -Dsurefire.failIfNoSpecifiedTests=false` | `Tests run: 1, Failures: 0`, BUILD SUCCESS; verde desde el principio a propósito, porque fija el contrato que no cambia |
| Verde | | |
| Suite completa | | |
| Mutación | | |
| Revisión | | |

## Qué hubo que corregirle a la IA

Nada en el trabajo hecho hasta ahora (spec, mapa, plan).

## Observaciones sobre el sistema de agentes

- **La sesión se cortó por el límite de uso** después de crear la rama, escribir
  `spec.md` y recibir el informe del `analista`, y antes de escribir el plan. Al
  reanudar se comprobó el estado con `git status` y `git branch --show-current`
  y se reutilizó el informe del analista, que estaba completo. No se repitió
  ningún paso.
- **El analista no marcó su mapa como incompleto** por la falta de `plan.md`.
  En el encargo se le avisó explícitamente, siguiendo lo aprendido en la 002, y
  `spec.md` no enlaza a `plan.md` hasta que existe.
- **El hook no deja a ningún rol editar `backend/*/pom.xml`.** La política
  (`.claude/hooks/politica.mjs`) limita `dev-backend` a `backend/*/src/main/` y
  `pruebas-backend` a `backend/*/src/test/`. ADR-0032 y `backend/AGENTS.md`
  piden subir `cobertura.suelo` en el mismo PR cuando la cobertura sube, y
  ningún agente puede hacerlo. Ver P-6 del plan.
