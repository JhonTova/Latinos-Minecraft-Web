# Git y pull requests

Aplica siempre, en cualquier carpeta. `scripts/actualizar.sh` despliega lo que
hay en `main`, así que estas normas son las que protegen producción.

- **Nunca hagas commit en `main`.** Crea una rama corta desde `main` actualizada:
  `feat/...`, `fix/...`, `chore/...`, `docs/...`, `test/...` o `ci/...`.
- **Un pull request, un tema.** Si aparece un arreglo que no es de la solicitud,
  va en otra rama.
- **Conventional Commits en español y en minúscula, con ámbito**:
  `feat(voting): ...`, `test(frontend): ...`, `docs(arnes): ...`. El cuerpo
  explica el porqué.
- **La prueba va antes que el código.** En un cambio con TDD, el commit
  `test(...)` que deja la prueba en rojo se hace antes que el `feat(...)` o el
  `fix(...)` que la pone en verde. Ese orden es la evidencia del TDD.
- **No fusiones ni hagas push a `main`.** El ruleset `proteger-main` exige pull
  request y los checks de la CI; la fusión la hace siempre una persona.
- **No reescribas historia publicada**: nada de `--force` ni `--amend` sobre
  commits que ya están en GitHub.
- **No uses `--no-verify`** ni desactives un check para que algo pase. Si un
  check falla, se arregla la causa.
- La descripción del pull request incluye: qué cambia y por qué, la matriz
  criterio → prueba cuando hay criterios de aceptación y la evidencia
  (salida de `mvn`, `npm`, capturas). Si toca `/api/v1/plugin/*`, una migración,
  el ledger o agrega una dependencia, se dice en la primera línea.
