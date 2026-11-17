#!/bin/sh
# Falla si la rama actual modifica, renombra o borra una migracion Flyway que
# ya existe en la rama base. Agregar migraciones nuevas esta permitido.
#
# Uso: .github/scripts/migraciones-intactas.sh [rama-base]      (por defecto origin/main)
#
# Lo ejecuta la CI en cada pull request. Tambien sirve en local antes de abrirlo.
set -eu

BASE="${1:-origin/main}"
CARPETA="backend/app/src/main/resources/db/migration"

# Tres puntos: se compara contra el punto donde la rama se separo de la base,
# no contra la punta de la base. Si no, una migracion que otro agrego a main
# despues apareceria aqui como "borrada" por esta rama.
#
# --no-renames: un renombrado se ve como un borrado mas un alta, y el borrado
# es justo lo que hay que detectar.
TOCADAS="$(git diff --no-renames --name-status --diff-filter=MD "$BASE...HEAD" -- "$CARPETA")"

if [ -n "$TOCADAS" ]; then
  echo "Este cambio toca migraciones que ya estan en $BASE:"
  echo
  echo "$TOCADAS"
  echo
  echo "Una migracion aplicada no se edita ni se borra: Flyway compara su"
  echo "checksum al arrancar y la aplicacion no levantaria en produccion."
  echo "Para corregir algo, crea una migracion nueva (skill migracion-flyway)."
  exit 1
fi

echo "Las migraciones de $BASE siguen intactas."
