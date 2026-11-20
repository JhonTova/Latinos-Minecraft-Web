# Tareas 005 — El reintento de un voto aceptado dice cuánto falta para el próximo

Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md)

Cada tarea se marca en el commit que la completa. El orden es TDD: la prueba en
rojo va antes que el código.

- [x] T1 · `analista` · Mapa del cambio en `plan.md`. Comprobado: informe con
  `ESTADO: completo`, sin paradas obligatorias.
- [x] T2 · `orquestador` · Aprobación de spec y plan (P-1 a P-6) y commit
  `docs(specs): ...` con los tres documentos.
- [x] T3 · `pruebas-backend` · Pruebas de CA-1 a CA-9 en
  `CastVoteServiceTest` (`@Nested` «Reintento idempotente»), en rojo.
  - Se comprueba con `cd backend && mvn -B -pl voting -am test`.
  - Las de CA-1 a CA-4, CA-7 (aceptado) y CA-9 tienen que fallar **por
    aserción**: hoy sale `null`, no el valor esperado.
  - CA-5, CA-6 y CA-8 pueden pasar ya, porque fijan lo que no cambia. Se
    declara cuáles pasan.
  - Antes de aceptar el rojo, el `orquestador` comprueba que cada valor
    esperado sale de la fórmula de la spec (tabla del plan).
- [x] T4 · `pruebas-backend` · `VoteControllerTest` (CA-1 y CA-5 por HTTP;
  P-5 aprobada).
  - Se comprueba con `cd backend && mvn -B -pl app -am test -Dtest=VoteControllerTest -Dsurefire.failIfNoSpecifiedTests=false`.
  - Puede pasar ya: fija la conversión a segundos, que no cambia.
- [x] T5 · `orquestador` · Commit `test(voting): ...` con T3 y T4.
- [x] T6 · `dev-backend` · `CastVoteService`: paso 1, paso 7 y
  `Resultado.repetido` (D1 a D6).
  - Se comprueba con `mvn -B -f backend/pom.xml verify` en verde, incluida
    `ArquitecturaTest`.
  - Se anota la cobertura de `voting` medida por JaCoCo.
- [x] T7 · `orquestador` · Commit `feat(voting): ...`.
- [ ] T8 · `revisor` · Revisión del diff completo de la rama y mutación con
  `cd backend && mvn -B -Pmutacion -pl voting -am verify`.
  - Se clasifican aquí los mutantes supervivientes de `CastVoteService`.
  - Se comprueba CA-10 con `git diff --stat main`.
- [ ] T9 · `orquestador` · Cierre: «Evidencia», «Qué hubo que corregirle a la
  IA» y `pr.md` con la matriz y el valor de cobertura medido.
  - No hace falta verificación visual: no se toca `frontend/`.

## Evidencia

Salida real de los comandos, pegada al cerrar la spec.

| Qué | Comando | Resultado |
|---|---|---|
| Rojo | | |
| Verde | | |
| Cobertura `voting` | | |
| Mutación | | |
| Revisión | | |

## Qué hubo que corregirle a la IA

- **El primer borrador de la spec daba por hecho que el cambio llegaba al
  reintento por token** (`/api/v1/vote-tokens/{token}/vote`). El `analista`
  vio que el caso normal, con el token ya consumido, responde antes de llamar a
  `votar` (`VoteTokenController.java:69-74`) y devuelve `null` fijo. Se corrigió
  el «Problema», ese caso pasó a «Fuera de alcance» y queda la pregunta P-1.
- **El primer borrador no tenía en cuenta tres casos.** Los encontró el
  `analista` y se añadieron:
  - `creadoEn` en el futuro, por reloj desfasado (nuevo CA-9);
  - los estados `PENDING` y `REVOKED` (CA-5 ampliado);
  - el choque entre «ningún `pom.xml`» y el trinquete de cobertura de ADR-0032
    (CA-10 ajustado).

## Observaciones sobre el sistema de agentes

- El flujo del orquestador pide esperar la aprobación de la spec antes de
  llamar al `analista`. En esta sesión no interactiva se llamó sin esa
  aprobación, porque el encargo pedía los pasos 1 a 3 seguidos. Spec y plan se
  presentan juntos para aprobarlos.
- Ningún hook bloqueó nada en los pasos 1 a 3. Solo se usaron `git switch -c`
  y ediciones en `specs/`.
