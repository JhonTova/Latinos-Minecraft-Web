#!/usr/bin/env bash
#
# Comprobación de humo del despliegue.
#
#   ./scripts/comprobar.sh
#   ./scripts/comprobar.sh https://latinosmc.com https://api.latinosmc.com
#
# Recorre la cadena completa —contenedores, base de datos, API, web, subidas y
# metadatos— y dice qué falla y por qué. Existe porque los últimos fallos que
# aparecieron en producción eran todos invisibles desde fuera: un límite de
# Nginx, unos permisos de carpeta, una caché de cinco minutos. Ninguno daba un
# error legible y los tres costaron una ronda cada uno.
#
# No sustituye a probar el flujo de voto a mano: eso necesita un servidor de
# Minecraft de verdad y está en la tabla final de LANZAMIENTO.md.

set -uo pipefail

WEB="${1:-https://latinosmc.com}"
API="${2:-https://api.latinosmc.com}"

VERDE=$'\e[32m'; ROJO=$'\e[31m'; AMBAR=$'\e[33m'; GRIS=$'\e[90m'; FIN=$'\e[0m'
fallos=0
avisos=0

ok()    { printf '  %s✓%s %s\n' "$VERDE" "$FIN" "$1"; }
mal()   { printf '  %s✗%s %s\n     %s%s%s\n' "$ROJO" "$FIN" "$1" "$GRIS" "$2" "$FIN"; fallos=$((fallos + 1)); }
aviso() { printf '  %s!%s %s\n     %s%s%s\n' "$AMBAR" "$FIN" "$1" "$GRIS" "$2" "$FIN"; avisos=$((avisos + 1)); }
titulo(){ printf '\n%s\n' "$1"; }

codigo() { curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$1"; }
cuerpo() { curl -s --max-time 15 "$1"; }

printf '\nComprobando %s\n' "$WEB"

# --- Contenedores ------------------------------------------------------------
titulo "Contenedores"

for servicio in db api web; do
  estado=$(docker compose ps --format '{{.Service}} {{.State}}' 2>/dev/null | awk -v s="$servicio" '$1 == s {print $2}')
  if [ "$estado" = "running" ]; then
    ok "$servicio en marcha"
  else
    mal "$servicio no está en marcha" "estado: ${estado:-no existe}. Mira: docker compose logs $servicio"
  fi
done

# --- Base de datos -----------------------------------------------------------
titulo "Base de datos"

migraciones=$(docker compose exec -T db psql -U latinosmc -d latinosmc -tAc \
  "SELECT count(*) FROM flyway_schema_history WHERE success" 2>/dev/null | tr -d '[:space:]')

if [ -n "$migraciones" ] && [ "$migraciones" -gt 0 ] 2>/dev/null; then
  ok "$migraciones migraciones aplicadas"
else
  mal "no se pueden leer las migraciones" "¿arrancó el contenedor db? docker compose logs db"
fi

fallidas=$(docker compose exec -T db psql -U latinosmc -d latinosmc -tAc \
  "SELECT count(*) FROM flyway_schema_history WHERE NOT success" 2>/dev/null | tr -d '[:space:]')

if [ "${fallidas:-0}" != "0" ]; then
  mal "hay migraciones fallidas" "la base quedó a medias; revisa flyway_schema_history"
fi

admins=$(docker compose exec -T db psql -U latinosmc -d latinosmc -tAc \
  "SELECT count(*) FROM platform_admins WHERE revoked_at IS NULL" 2>/dev/null | tr -d '[:space:]')

if [ "${admins:-0}" -gt 0 ] 2>/dev/null; then
  ok "$admins administrador(es) de plataforma"
else
  aviso "no hay ningún administrador" "no podrás entrar a /panel/admin. Ver LANZAMIENTO.md paso 1b"
fi

# --- Permisos de disco -------------------------------------------------------
titulo "Almacenamiento de imágenes"

if docker compose exec -T api sh -c 'touch /datos/archivos/.prueba && rm /datos/archivos/.prueba' 2>/dev/null; then
  ok "la carpeta de archivos se puede escribir"
else
  mal "la aplicación NO puede escribir en /datos/archivos" \
      "si borraste data/archivos con sudo, Docker la recreó como root. Arréglalo con:
     sudo chown -R \$(docker compose exec -T api id -u):\$(docker compose exec -T api id -g) data/archivos"
fi

# --- API ---------------------------------------------------------------------
titulo "API"

for ruta in /api/v1/ranking /api/v1/estadisticas /api/v1/destacados /api/v1/patrocinadores /api/v1/sitio; do
  estado=$(codigo "$API$ruta")
  if [ "$estado" = "200" ]; then
    ok "$ruta"
  else
    mal "$ruta devuelve $estado" "docker compose logs api --tail 50"
  fi
done

# --- Web ---------------------------------------------------------------------
titulo "Web"

for ruta in / /servidores /plugin /destacar /registro /login /robots.txt /sitemap.xml; do
  estado=$(codigo "$WEB$ruta")
  if [ "$estado" = "200" ]; then
    ok "$ruta"
  else
    mal "$ruta devuelve $estado" "docker compose logs web --tail 50"
  fi
done

# --- Límite de subida --------------------------------------------------------
titulo "Límite de subida"

# 3 MB de ceros contra el proxy. Un 413 significa que Nginx corta antes de que
# la petición llegue a la aplicación, y ese fue el fallo que dejó las subidas
# rotas sin dejar rastro en ningún log del backend.
grande=$(head -c 3000000 /dev/zero | tr '\0' 'x')
estado=$(curl -s -o /dev/null -w '%{http_code}' --max-time 30 \
  -X POST "$WEB/api/backend/admin/sitio/logo" \
  -F "archivo=@-;filename=prueba.png;type=image/png" <<< "$grande" 2>/dev/null)

case "$estado" in
  413) mal "Nginx rechaza cuerpos de 3 MB" \
           "falta 'client_max_body_size 8m;' en el bloque server de la WEB, no solo en el de api." ;;
  401|403|404) ok "acepta 3 MB (rechazado por permisos, que es lo correcto sin sesión)" ;;
  *)   ok "acepta 3 MB (respuesta $estado)" ;;
esac

# --- Metadatos ---------------------------------------------------------------
titulo "Metadatos y marca"

html=$(cuerpo "$WEB")

if grep -q 'property="og:title"' <<< "$html"; then
  ok "etiquetas Open Graph presentes"
else
  mal "faltan las etiquetas Open Graph" "al compartir el enlace no saldrá tarjeta"
fi

if grep -q 'og:image' <<< "$html"; then
  ok "imagen para compartir configurada"
else
  aviso "sin imagen para compartir" "súbela en /panel/admin"
fi

logo=$(cuerpo "$API/api/v1/sitio" | grep -o '"logoUrl":"[^"]*"' | cut -d'"' -f4)
if [ -n "$logo" ]; then
  if [ "$(codigo "$logo")" = "200" ]; then
    ok "el logotipo se sirve correctamente"
  else
    mal "el logotipo está configurado pero no se puede descargar" "URL: $logo"
  fi
else
  aviso "sin logotipo" "se pintan las iniciales. Súbelo en /panel/admin"
fi

plugin=$(cuerpo "$API/api/v1/sitio" | grep -o '"urlPlugin":"[^"]*"' | cut -d'"' -f4)
if [ -n "$plugin" ]; then
  ok "enlace de descarga del plugin configurado"
else
  aviso "sin enlace del plugin" "el botón de descarga no aparecerá"
fi

# --- Pasarela y correo -------------------------------------------------------
titulo "Cobros y correo"

if docker compose logs api 2>/dev/null | grep -q "PayPal activo en modo REAL"; then
  ok "PayPal en modo real"
elif docker compose logs api 2>/dev/null | grep -q "PayPal activo en modo pruebas"; then
  aviso "PayPal en SANDBOX" "cobra dinero falso. Cambia PAYPAL_MODO=live antes de abrir"
else
  aviso "PayPal no está activo" "los pedidos quedarán pendientes de cobro manual"
fi

if docker compose logs api 2>/dev/null | grep -q "CORREO (modo desarrollo"; then
  mal "los correos salen por consola" "nadie recibirá su código de verificación. Configura SMTP y PERFIL=prod"
else
  ok "el correo no está en modo consola"
fi

perfil=$(docker compose exec -T api sh -c 'echo $SPRING_PROFILES_ACTIVE' 2>/dev/null | tr -d '[:space:]')
if [ "$perfil" = "prod" ]; then
  ok "perfil de producción"
else
  aviso "perfil '$perfil'" "en producción usa PERFIL=prod: se niega a arrancar sin correo ni pepper"
fi

# --- Resumen -----------------------------------------------------------------
printf '\n'
if [ "$fallos" -eq 0 ] && [ "$avisos" -eq 0 ]; then
  printf '%sTodo correcto.%s\n\n' "$VERDE" "$FIN"
elif [ "$fallos" -eq 0 ]; then
  printf '%sSin fallos, %d aviso(s).%s Se puede abrir, pero léelos.\n\n' "$AMBAR" "$avisos" "$FIN"
else
  printf '%s%d fallo(s)%s y %d aviso(s). NO abras hasta arreglar los fallos.\n\n' "$ROJO" "$fallos" "$FIN" "$avisos"
fi

exit "$fallos"
