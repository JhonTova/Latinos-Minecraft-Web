#!/bin/sh
# Escribe una tabla Markdown con la cobertura de lineas de cada modulo, a partir
# de los jacoco.csv que deja 'mvn verify'. La CI la agrega al resumen del
# trabajo, para que la cifra se vea en el pull request sin bajar el artefacto.
#
# Uso (desde backend/): sh ../.github/scripts/resumen-cobertura.sh
set -eu

echo "### Cobertura de lineas (JaCoCo)"
echo
echo "| Modulo | Cubiertas | Total | Cobertura | Suelo |"
echo "|---|---|---|---|---|"

for csv in */target/site/jacoco/jacoco.csv; do
  [ -f "$csv" ] || continue
  modulo=${csv%%/*}
  suelo=$(sed -n 's:.*<cobertura.suelo>\(.*\)</cobertura.suelo>.*:\1:p' "$modulo/pom.xml")
  # Columnas 8 y 9 del CSV de JaCoCo: LINE_MISSED y LINE_COVERED.
  awk -F, -v m="$modulo" -v s="${suelo:-0.00}" '
    NR > 1 { perdidas += $8; cubiertas += $9 }
    END {
      total = perdidas + cubiertas
      if (total > 0) {
        printf "| `%s` | %d | %d | %.1f %% | %s |\n", m, cubiertas, total, 100 * cubiertas / total, (s + 0 > 0 ? s * 100 " %" : "solo informe")
      }
    }' "$csv"
done
