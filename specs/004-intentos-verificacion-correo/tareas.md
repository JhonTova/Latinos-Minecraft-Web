# Tareas 004 — Decir cuántos intentos quedan al verificar el correo

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código. El backend va antes que el frontend, porque la
pantalla consume la propiedad nueva.

- [x] T1 · `analista` · Mapa del cambio en `plan.md`. Se comprueba con la sección «Mapa del cambio».
- [x] T2 · `pruebas-backend` · `EmailVerificationServiceTest` (CA-1, CA-2, CA-5 a CA-8) y `ManejadorDeErroresTest` (CA-1, CA-2, CA-4), en rojo. Se comprueba con `cd backend && mvn -B -pl app -am test`: deben fallar por la ausencia de `IntentoRechazadoException` o de `intentosRestantes`. Las de los límites que ya se cumplen hoy (CA-5, CA-7, CA-8) pasan desde el principio, y hay que decirlo así.
- [x] T3 · `pruebas-backend` · `EmailVerificationServiceTransaccionTest` (CA-3), en rojo. Se comprueba con `cd backend && mvn -B -pl identity -am test`: debe fallar porque hoy hay `rollback` y no `commit`.
- [x] T4 · `orquestador` · Commit `test(identity): ...` con T2 y T3.
- [x] T5 · `dev-backend` · `IntentoRechazadoException`, `noRollbackFor` y el manejador en `ManejadorDeErrores` (D1 a D4). Se comprueba con `mvn -B -f backend/pom.xml verify` en verde, incluido `ArquitecturaTest`, y con la cobertura de identity medida.
- [x] T6 · `pruebas-frontend` · `intentos-de-verificacion.test.ts` (CA-9 a CA-13), en rojo. Se comprueba con `cd frontend && npm run test`: debe fallar porque el módulo no existe.
- [x] T7 · `orquestador` · Commit `test(frontend): ...`.
- [x] T8 · `dev-frontend` · `intentos-de-verificacion.ts` y `formulario-verificar.tsx` (D6, D7). Se comprueba con `npm run lint`, `npm run test` y `npm run build` en verde.
- [ ] T9 · `verificador-ui` · `/verificar` con 2, 1 y 0 intentos, `too_many_attempts`, error genérico y fallo de red; en móvil y escritorio, con tema claro y oscuro (CA-12, CA-13).
- [ ] T10 · `orquestador` · Commits `fix(identity): ...` y `feat(frontend): ...`.
- [ ] T11 · `revisor` · Diff completo de la rama (CA-14) y mutación con `mvn -B -Pmutacion -pl identity -am verify`, con los mutantes supervivientes clasificados abajo.
- [ ] T12 · `orquestador` · Evidencia, `pr.md` con la primera línea sobre el fallo de seguridad, y anotar la cobertura medida de identity para que se suba `cobertura.suelo` en un commit aparte.

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo backend, fase 1 | `cd backend && mvn -B -pl identity -am test` | `Tests run: 43, Failures: 1, Errors: 0`. Falla `EmailVerificationServiceTransaccionTest.codigoIncorrectoHaceCommitYNoRollback`: `Wanted but not invoked: platformTransactionManager.commit(...)`; hubo `getTransaction` y `rollback(...)` desde `TransactionAspectSupport.completeTransactionAfterThrowing`. Confirma el fallo de seguridad. El control `caducadoHaceRollback` pasa, y CA-5, CA-7, CA-8 y el correcto con 4 fallos (CA-6) pasan desde hoy. |
| Rojo backend, fase 2 | `cd backend && mvn -B -pl app -am test` | `testCompile` falla en `identity`: 7 × `cannot find symbol: class IntentoRechazadoException` en `EmailVerificationServiceTest.java`. `app` queda `SKIPPED`. |
| Rojo frontend | `cd frontend && npm run test` | `FAIL src/lib/intentos-de-verificacion.test.ts`: `Cannot find module './intentos-de-verificacion'`. `Test Files 1 failed \| 3 passed (4)`, `Tests 110 passed (110)`. Es un rojo por módulo ausente, no por aserción. |
| Verde backend | | |
| Verde frontend | | |
| Cobertura identity | | |
| Mutación identity | | |
| Revisión | | |

## Qué hubo que corregirle a la IA

- El mapa del `analista` asignaba a `dev-backend` editar
  `backend/identity/pom.xml` (subir `cobertura.suelo`). Ningún rol puede
  editar `pom.xml` en esta sesión, así que el orquestador lo sacó del plan: el
  valor medido se anota aquí y lo sube la sesión que lanzó al orquestador.
- El `analista` dijo que no revisó si `CastVoteService.votar` registra algo
  antes de lanzar, que sería el mismo patrón de rollback. El orquestador lo
  miró (`CastVoteService.java:112-160`): las excepciones de ese tramo se lanzan
  antes de escribir. No se revisó el resto del método ni `AuthService`.
- El `analista` recomendó pedir la revisión del plan al agente `arquitecto`,
  pero ese agente no está entre los tipos disponibles para el orquestador.

## Observaciones sobre el sistema de agentes

- **La sesión se cortó** por el límite de uso con `spec.md` y `plan.md` ya
  escritos y sin commit. Al reanudar, el orquestador leyó `git status` y
  `specs/004-…/` y continuó sin repetir el mapa del `analista`. El informe del
  analista seguía disponible en la conversación; si no lo estuviera, el mapa
  solo sobreviviría en la sección «Mapa del cambio» de `plan.md`.
- La skill `spec-sdd` numera con el mayor `NNN` más uno. Aquí habría dado 003,
  pero la solicitud pedía 004. Se siguió la solicitud y se preguntó (P-6). La
  003 se fusionó en `main` (PR #17) después de crear esta rama, así que la
  regla «mayor + 1» solo mira la rama local y no ve una spec que esté abierta
  o recién fusionada en otra parte.
- Ningún hook bloqueó nada en esta etapa.
