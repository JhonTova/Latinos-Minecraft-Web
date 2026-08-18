"use client";

import { Boton } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

/** El mismo alfabeto que genera el backend, sin I, L, O ni U. */
const ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

/** Ocho caracteres, dos grupos de cuatro (ADR-0026). */
export const LARGO_DEL_CODIGO = 8;

/**
 * Campo donde el jugador escribe el código que le dio el servidor.
 *
 * <p>Está en un solo sitio y se usa en dos —la ficha de cada servidor y la
 * página de votar— porque las reglas de este campo no son cosmética: el alfabeto
 * y la tolerancia a erratas tienen que coincidir con el backend. Con dos copias,
 * el día que cambie una el jugador vería "código incorrecto" sin haberse
 * equivocado.
 *
 * <p>**Aquí no se pide el nick.** El código ya lleva dentro quién eres
 * (ADR-0016): si la web preguntara el nombre, cualquiera podría votar en nombre
 * de otro sin haber pisado nunca ese servidor.
 */
export function CampoDeCodigo({
  etiqueta = "Tu código",
  ayuda,
  autoFocus,
}: {
  etiqueta?: string;
  ayuda?: string;
  autoFocus?: boolean;
}) {
  const router = useRouter();
  const [escrito, setEscrito] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const limpio = normalizar(escrito);
  const completo = limpio.length === LARGO_DEL_CODIGO;

  function continuar() {
    if (!completo) {
      setError(`El código tiene ${LARGO_DEL_CODIGO} caracteres. Revísalo.`);
      return;
    }
    setEnviando(true);
    // Se navega a la página del código en lugar de votar desde aquí: allí se
    // resuelve a quién pertenece y se le enseña antes de confirmar. Votar sin
    // que el jugador vea su propio nombre invita a pensar que se ha votado por
    // otro.
    router.push(`/votar/${limpio}`);
  }

  const textoDeAyuda =
    error ??
    ayuda ??
    `${limpio.length} de ${LARGO_DEL_CODIGO} caracteres. Los guiones los ponemos nosotros.`;

  return (
    <div>
      <label htmlFor="codigo" className="mb-1.5 block text-sm font-medium">
        {etiqueta}
      </label>
      <input
        id="codigo"
        value={escrito}
        onChange={(e) => {
          setEscrito(agrupar(normalizar(e.target.value)));
          setError(null);
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter") continuar();
        }}
        placeholder="K7M2-9QX4"
        autoFocus={autoFocus}
        // Sin corrector ni mayúsculas automáticas: en el móvil convertirían el
        // código en otra cosa mientras se escribe.
        autoComplete="off"
        autoCorrect="off"
        autoCapitalize="characters"
        spellCheck={false}
        // 9 = 8 caracteres + el guion que se inserta solo.
        maxLength={LARGO_DEL_CODIGO + 1}
        aria-describedby="codigo-ayuda"
        aria-invalid={error ? true : undefined}
        className="w-full rounded-md border border-border bg-surface px-3 py-2.5 text-center font-mono text-lg tracking-widest uppercase outline-none transition-colors placeholder:tracking-normal placeholder:text-subtle focus:border-primary"
      />
      <p
        id="codigo-ayuda"
        className={`mt-1.5 text-xs ${error ? "text-danger" : "text-subtle"}`}
      >
        {textoDeAyuda}
      </p>

      <div className="mt-3">
        <Boton onClick={continuar} cargando={enviando} disabled={!completo}>
          Continuar
        </Boton>
      </div>
    </div>
  );
}

/**
 * Deja solo caracteres del alfabeto y corrige las confusiones de lectura.
 *
 * Es la misma tolerancia que aplica el backend, repetida aquí para que el campo
 * reaccione bien mientras se escribe. Nadie distingue una I de un 1 leyendo el
 * chat de Minecraft, y rechazar por eso haría pensar que el sitio está roto.
 */
function normalizar(escrito: string): string {
  let salida = "";

  for (const caracter of escrito.trim().toUpperCase()) {
    const equivalente =
      caracter === "I" || caracter === "L"
        ? "1"
        : caracter === "O"
          ? "0"
          : caracter === "U"
            ? "V"
            : caracter;

    if (ALFABETO.includes(equivalente)) salida += equivalente;
    if (salida.length === LARGO_DEL_CODIGO) break;
  }
  return salida;
}

/** Mete el guion mientras se escribe, para que se lea igual que en el chat. */
function agrupar(limpio: string): string {
  return limpio.replace(/(.{4})(?=.)/g, "$1-");
}
