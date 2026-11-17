# Tareas 004 — Decir cuántos intentos quedan al verificar el correo

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código. El backend va antes que el frontend, porque la
pantalla consume la propiedad nueva.

- [x] T1 · `analista` · Mapa del cambio en `plan.md`. Se comprueba en la sección «Mapa del cambio».
- [x] T2 · `pruebas-backend` · `EmailVerificationServiceTest` (CA-1, CA-2, CA-5 a CA-8) y `ManejadorDeErroresTest` (CA-1, CA-2, CA-4), en rojo. Se comprueba con `cd backend && mvn -B -pl app -am test`: deben fallar porque no existe `IntentoRechazadoException` o falta `intentosRestantes`. Las de los límites que ya se cumplen hoy (CA-5, CA-7, CA-8) pasan desde el principio, y hay que decirlo así.
- [x] T3 · `pruebas-backend` · `EmailVerificationServiceTransaccionTest` (CA-3), en rojo. Se comprueba con `cd backend && mvn -B -pl identity -am test`: debe fallar porque hoy hay `rollback` y no `commit`.
- [x] T4 · `orquestador` · Commit `test(identity): ...` con T2 y T3 (`e84b2b1`).
- [x] T5 · `dev-backend` · `IntentoRechazadoException`, `noRollbackFor` y el manejador en `ManejadorDeErrores` (D1 a D4). Se comprueba con `mvn -B -f backend/pom.xml verify` en verde, incluido `ArquitecturaTest`, y con la cobertura de identity medida.
- [x] T6 · `pruebas-frontend` · `intentos-de-verificacion.test.ts` (CA-9 a CA-13), en rojo. Se comprueba con `cd frontend && npm run test`: debe fallar porque el módulo no existe.
- [x] T7 · `orquestador` · Commit `test(frontend): ...` (`d856036`).
- [x] T8 · `dev-frontend` · `intentos-de-verificacion.ts` y `formulario-verificar.tsx` (D6, D7). Se comprueba con `npm run lint`, `npm run test` y `npm run build` en verde.
- [ ] T9 · `verificador-ui` · `/panel/verificar` con 2, 1 y 0 intentos, `too_many_attempts`, error genérico y fallo de red, en móvil y escritorio, con tema claro y oscuro (CA-12, CA-13). **No se pudo hacer:** en este entorno el rol no tiene navegador y el hook le bloquea las peticiones HTTP (ver «Observaciones»). Queda pendiente de que el desarrollador revise la pantalla en el navegador antes de fusionar.
- [x] T10 · `orquestador` · Commits `test(identity)` de corrección (`e750132`), `fix(identity)` (`d602b54`) y `feat(frontend)` (`c1184c8`).
- [x] T11 · `revisor` · Diff completo de la rama (CA-14) y mutación con `mvn -B -Pmutacion -pl identity -am verify`. Sin hallazgos altos. El hallazgo bajo 5 volvió a `pruebas-backend` (`c2e1f66`), y los 3, 4 y 6 los corrigió el orquestador en los documentos.
- [x] T12 · `orquestador` · Evidencia, `pr.md` con la primera línea sobre el fallo de seguridad, y la cobertura medida de identity (0,253) para que la sesión que lanzó al orquestador suba `cobertura.suelo` en un commit aparte.

## Evidencia

Salida real de los comandos, tal como la pegaron los subagentes en sus informes.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo backend, fase 1 | `cd backend && mvn -B -pl identity -am test` (`pruebas-backend`) | `Tests run: 43, Failures: 1, Errors: 0`. Falla `EmailVerificationServiceTransaccionTest.codigoIncorrectoHaceCommitYNoRollback` con `Wanted but not invoked: platformTransactionManager.commit(...)`: hubo `getTransaction` y `rollback(...)`, este último desde `TransactionAspectSupport.completeTransactionAfterThrowing`. Confirma el fallo de seguridad. El control `caducadoHaceRollback` pasa. CA-5, CA-7, CA-8 y el caso del código correcto con 4 fallos (CA-6) ya pasan, porque esos límites se cumplen hoy. |
| Rojo backend, fase 2 | `cd backend && mvn -B -pl app -am test` (`pruebas-backend`) | `testCompile` falla en `identity`: 7 × `cannot find symbol: class IntentoRechazadoException` en `EmailVerificationServiceTest.java`. `app` queda `SKIPPED`. |
| Rojo frontend | `cd frontend && npm run test` (`pruebas-frontend`) | `FAIL src/lib/intentos-de-verificacion.test.ts: Cannot find module './intentos-de-verificacion'`. `Test Files 1 failed \| 3 passed (4)`, `Tests 110 passed (110)`. El rojo es por módulo ausente, no por aserción. |
| Primer verde backend | `cd backend && mvn -B verify` (`dev-backend`) | `BUILD FAILURE` en identity: `Tests run: 52, Failures: 1`, en `nuloCuentaComoIncorrecto` (`expected: 2 but was: 3`). Era un error de la prueba (ver abajo). Se corrigió en `e750132` y después `mvn -B -pl identity -am test` dio `Tests run: 52, Failures: 0` y `BUILD SUCCESS`. |
| Verde backend (limpio) | `cd backend && mvn -B verify` (`revisor`) | Todo sin fallos: shared 50, identity 52, servers 79, voting 37, trust 35, billing 22, app 112. `ArquitecturaTest` 4/4. `All coverage checks have been met`. Los 11 módulos en `SUCCESS`, `BUILD SUCCESS`. |
| Prueba añadida tras la revisión | `cd backend && mvn -B -pl app -am test` (`pruebas-backend`) | app: `Tests run: 114, Failures: 0, Errors: 0`, `BUILD SUCCESS`. |
| Verde frontend | `cd frontend && npm run lint`, `npm run test`, `npm run build` (`dev-frontend`) | lint sin errores ni avisos. `4` archivos y `155` pruebas pasan. Build compilado con Next.js 16.3.1, TypeScript sin errores y 15 de 15 páginas, `/panel/verificar` incluida. El `revisor` repitió `lint` y `test` (155/155). El hook no le dejó ejecutar `build`. |
| Cobertura identity | JaCoCo, contador LINE (`revisor` y `dev-backend`) | **106/419 = 0,253** (antes de la rama, ~0,157). Instrucciones: 551/1666, ~0,331, pero el umbral no usa esa cifra. El `cobertura.suelo` sigue en 0,15 en `backend/identity/pom.xml:23`: lo sube la sesión que lanzó al orquestador, propuesto a 0,25. |
| Mutación identity | `cd backend && mvn -B -Pmutacion -pl identity -am verify` (`revisor`) | `EmailVerificationService`: 17/17 mutantes muertos. `IntentoRechazadoException`: 1/1. **Ningún superviviente en las clases tocadas.** En todo el módulo: 144 generados, 52 muertos, 91 sin cobertura y 1 superviviente en `AuthService.registrar:55`, que esta rama no toca. Test strength 98 %. |
| Verificación visual | `verificador-ui`, dos vueltas | **No se hizo.** Ver T9 y «Observaciones». |
| Revisión | `revisor` | Sin hallazgos altos. 2 medios: el suelo de cobertura y que falten el build y la verificación visual en la revisión. 4 bajos, todos atendidos o anotados. |

### Clasificación de mutantes supervivientes

No sobrevive ninguno en `EmailVerificationService` ni en
`IntentoRechazadoException`. El que sobrevive en `AuthService.registrar:55`
está fuera de esta spec.

Límites de lo que mide PIT aquí, según el `revisor`:
- PIT no muta anotaciones. Lo único que protege `noRollbackFor` es
  `EmailVerificationServiceTransaccionTest`.
- Las llamadas que devuelven `int` (`invalidarAnteriores` y
  `marcarEmailVerificado`) no generan mutantes con los mutadores por
  defecto. Las pruebas sí las comprueban con `verify`.

### Qué no demuestran estas pruebas

- **CA-3 no demuestra que el `UPDATE` llegue a PostgreSQL.** La prueba
  demuestra que el proxy transaccional de Spring hace `commit` y no `rollback`
  con un gestor simulado (decisión P-4). Que la fila se escriba de verdad
  depende de la base de datos, y las pruebas del backend no usan ninguna
  (ADR-0021).
- La carrera de peticiones simultáneas (P-3, fuera de alcance) tampoco se
  puede demostrar sin PostgreSQL.
- CA-12 y CA-13 están probados en `src/lib` con Vitest, y la conexión con
  `formulario-verificar.tsx` solo se ha comprobado leyendo el código, porque no
  hubo verificación visual.

## Qué hubo que corregirle a la IA

- **`pruebas-backend` escribió un valor esperado mal calculado.**
  `nuloCuentaComoIncorrecto` esperaba 2 intentos restantes con 1 fallo previo,
  pero la fórmula de CA-1 da 5 − 2 = 3, y `restaN` del mismo archivo ya lo
  fijaba así. Lo detectó `dev-backend` al ponerla en verde, que la señaló sin
  adaptar el código. El orquestador lo comprobó y la devolvió a
  `pruebas-backend` (vuelta 1), que la corrigió en `e750132` y no encontró otros
  errores de cálculo. Como el rojo de esa prueba era de compilación, el error no
  se vio en la fase de rojo.
- **`verificador-ui` no siguió la forma de comando que se le dio.** En la
  vuelta 1 lanzó `cd frontend` y `npm run dev` en dos llamadas separadas. El
  `cd` no persiste, así que `npm run dev` falló en la raíz. En la vuelta 2 sí
  arrancó el servidor, pero probó `python -c`, `curl` e `Invoke-WebRequest`
  después de que el hook lo bloqueara, en contra de la instrucción de no buscar
  otro camino. Él mismo lo reconoció en su informe.
- El mapa del `analista` asignaba a `dev-backend` editar
  `backend/identity/pom.xml` para subir `cobertura.suelo`. Ningún rol puede
  editar `pom.xml` en esta sesión, así que el orquestador lo sacó del plan.
- El `analista` dijo que no había revisado si `CastVoteService.votar` registra
  algo antes de lanzar, que sería el mismo patrón de rollback. El orquestador lo
  miró en `CastVoteService.java:112-160`: las excepciones de ese tramo se lanzan
  antes de escribir. No se revisó el resto del método ni `AuthService`.
- La matriz del plan nombraba pruebas que acabaron llamándose de otra forma
  (`conEspaciosYGuiones` resultó en `conEspacios` y `conGuion`), y D9 citaba
  `Clock.offset`, que no se usa. Lo detectó el `revisor` y se corrigió en
  `plan.md`.
- CA-13 describía mal el fallo de red. Lo detectó el `revisor` y se corrigió en
  la spec (ver «Cambios durante la implementación»).

## Observaciones sobre el sistema de agentes

- **La sesión se cortó** por el límite de uso con `spec.md` y `plan.md` ya
  escritos y sin commit. Al reanudar, el orquestador leyó `git status` y
  `specs/004-…/` y continuó sin repetir el mapa del `analista`. El informe del
  analista seguía disponible en la conversación; si no lo estuviera, el mapa
  solo sobreviviría en la sección «Mapa del cambio» de `plan.md`.
- La skill `spec-sdd` numera con el mayor `NNN` más uno. Aquí habría dado 003,
  pero la solicitud pedía 004. Se siguió la solicitud y se preguntó (P-6). La
  003 se fusionó en `main` (PR #17) después de crear esta rama: la regla «mayor +
  1» solo mira la rama local y no ve una spec que esté abierta o recién
  fusionada en otra parte.
- **El hook bloqueó al orquestador** al escribir
  `specs/004-…/evidencia/backend-simulado.mjs`, con el mensaje «está fuera de las
  carpetas del rol 'orquestador'». En la spec 002 existe un archivo igual en
  `specs/002-…/evidencia/`, así que la política no admite ya lo que se hizo
  entonces, o lo escribió otro rol. El orquestador no buscó otra vía.
- **`verificador-ui` no puede verificar en este entorno.** No tiene ninguna
  herramienta de navegador (buscó con ToolSearch y no apareció ninguna), y el
  hook le bloquea `python -c`, `curl` e `Invoke-WebRequest`. Solo consiguió
  arrancar `next dev` («Ready in 389ms») y pararlo. Su documentación dice que
  comprueba «en un navegador real», pero aquí no había navegador. Tras dos
  vueltas sin avance, el orquestador no lanzó la tercera.
- **El `revisor` no puede ejecutar `npm run build`.** El hook se lo niega («el
  rol 'revisor' no puede ejecutar: npm run build»), aunque `AGENTS.md` lo pone
  entre las condiciones de «terminado». El build verde solo lo ejecutó
  `dev-frontend`.
- `next dev` no modificó `frontend/AGENTS.md` ni dejó archivos sueltos: el
  orquestador lo comprobó con `git status` después.
- `revision-de-pruebas` y la mutación funcionaron como dice la documentación.
