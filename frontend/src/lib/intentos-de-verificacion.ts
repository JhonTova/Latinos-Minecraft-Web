const INCORRECTO = "El código no es correcto.";
const SIN_INTENTOS =
  "No te quedan intentos. Pide un código nuevo con «No me ha llegado, enviar otro».";

/**
 * Convierte los intentos que le quedan al usuario en una frase.
 *
 * Solo un entero es un dato de fiar: una cadena, un decimal o NaN no se
 * adivinan, devuelven `null` para que el aviso no invente un número. Cero y
 * los negativos (incluido -0) piden un código nuevo, para no mostrar nunca
 * «0 intentos» ni un signo menos.
 */
export function textoDeIntentos(intentos: unknown): string | null {
  if (typeof intentos !== "number" || !Number.isInteger(intentos)) {
    return null;
  }
  if (intentos <= 0) {
    return SIN_INTENTOS;
  }
  if (intentos === 1) {
    return "Te queda 1 intento.";
  }
  return `Te quedan ${intentos} intentos.`;
}

/**
 * Decide el aviso al fallar la verificación del correo, o `null` si el error
 * no es de intentos y toca usar el mensaje genérico de siempre.
 *
 * Se compara `title` (no `type`) con igualdad exacta, como en
 * `rechazo-de-voto.ts`. El texto lo pone el frontend y no el `detail` del
 * backend, porque el cuerpo no es de fiar.
 */
export function avisoDeVerificacion(problema: unknown): string | null {
  if (typeof problema !== "object" || problema === null || Array.isArray(problema)) {
    return null;
  }

  const cuerpo = problema as Record<string, unknown>;

  if (cuerpo.title === "verification.invalid_code") {
    const intentos = textoDeIntentos(cuerpo.intentosRestantes);
    return intentos === null ? INCORRECTO : `${INCORRECTO} ${intentos}`;
  }
  if (cuerpo.title === "verification.too_many_attempts") {
    // Ya no hay intentos, venga o no la propiedad: sin prefijo de «incorrecto».
    return SIN_INTENTOS;
  }

  return null;
}
