#!/usr/bin/env bash
#
# Actualizar el despliegue.
#
#   ./scripts/actualizar.sh
#
# Sustituye al `git pull && docker compose up -d --build` a pelo. Hace lo mismo,
# mas la parte que se olvida siempre: dejar data/ con el dueño correcto.
#
# El fallo que motiva este script: todo lo que sobrevive a una actualizacion
# vive en ./data (base de datos, copias e imagenes subidas). Esa carpeta esta en
# .gitignore, asi que el pull nunca la toca —bien—, pero si falta, Docker la
# crea como root al montar el volumen, y la aplicacion corre como usuario sin
# privilegios. Resultado: todo arranca aparentemente bien y la primera subida de
# imagen falla con "El servidor no puede escribir en su carpeta de archivos".

set -euo pipefail

cd "$(dirname "$0")/.."

# Debe coincidir con el UID/GID de backend/Dockerfile.
UID_APP=10001
GID_APP=10001

VERDE=$'\e[32m'; GRIS=$'\e[90m'; FIN=$'\e[0m'
paso() { printf '\n%s==>%s %s\n' "$VERDE" "$FIN" "$1"; }
nota() { printf '    %s%s%s\n' "$GRIS" "$1" "$FIN"; }

# chown necesita root, salvo que ya se ejecute como root.
if [ "$(id -u)" -eq 0 ]; then
  SUDO=""
else
  SUDO="sudo"
fi

paso "Bajando los cambios"
git pull

paso "Preparando ./data"
# Las tres carpetas se crean ANTES de levantar los contenedores. Si no existen,
# las crea Docker como root y el problema vuelve.
mkdir -p data/postgres data/copias data/archivos

# Solo la de imagenes necesita ser del usuario de la aplicacion. La de PostgreSQL
# la ajusta su propia imagen al arrancar, y tocarla a mano puede romperla.
$SUDO chown -R "$UID_APP:$GID_APP" data/archivos
nota "data/archivos -> $UID_APP:$GID_APP"

paso "Reconstruyendo y levantando"
docker compose up -d --build

paso "Comprobando"
# Las migraciones de Flyway se aplican al arrancar; darle margen antes de mirar.
sleep 20
./scripts/comprobar.sh "$@"
