/**
 * Minigrafico de tendencia. Sin librerias: son 7 puntos y una polilinea.
 *
 * Es decorativo respecto al dato principal (el numero de votos ya esta escrito
 * al lado), asi que se marca aria-hidden en vez de intentar describirlo a un
 * lector de pantalla.
 */
export function Sparkline({
  valores,
  className = "",
}: {
  valores: number[];
  className?: string;
}) {
  if (valores.length < 2) return null;

  const ancho = 64;
  const alto = 20;
  const min = Math.min(...valores);
  const max = Math.max(...valores);
  // Si todos los valores son iguales el rango es 0: se fuerza a 1 para no
  // dividir entre cero y que la linea salga plana en el centro.
  const rango = max - min || 1;

  const puntos = valores
    .map((v, i) => {
      const x = (i / (valores.length - 1)) * ancho;
      const y = alto - ((v - min) / rango) * alto;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  return (
    <svg
      viewBox={`0 0 ${ancho} ${alto}`}
      className={`h-5 w-16 overflow-visible ${className}`}
      aria-hidden
      preserveAspectRatio="none"
    >
      <polyline
        points={puntos}
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />
    </svg>
  );
}
