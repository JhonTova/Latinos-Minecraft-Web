"use client";

import { Aviso, Boton, Campo, mensajeDeError } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

export function FormularioVerificar({ correo }: { correo: string }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function verificar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setAviso(null);
    setCargando(true);

    const datos = new FormData(evento.currentTarget);
    const respuesta = await fetch("/api/backend/auth/verificar", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ codigo: datos.get("codigo") }),
    });

    if (!respuesta.ok) {
      setError(await mensajeDeError(respuesta));
      setCargando(false);
      return;
    }

    router.push("/panel");
    router.refresh();
  }

  async function reenviar() {
    setError(null);
    setAviso(null);

    const respuesta = await fetch("/api/backend/auth/reenviar-codigo", {
      method: "POST",
    });

    if (respuesta.ok) {
      setAviso("Te hemos enviado un código nuevo.");
    } else {
      setError(await mensajeDeError(respuesta));
    }
  }

  return (
    <div className="space-y-4">
      <form onSubmit={verificar} className="space-y-4">
        {error && <Aviso tono="error">{error}</Aviso>}
        {aviso && <Aviso tono="exito">{aviso}</Aviso>}

        <Campo
          etiqueta="Código de 6 dígitos"
          name="codigo"
          // inputMode numérico para que en el móvil salga el teclado de números
          // directamente, y autoComplete para que el sistema lo ofrezca al
          // llegar el correo.
          inputMode="numeric"
          autoComplete="one-time-code"
          required
          maxLength={12}
          placeholder="000000"
          className="w-full rounded-md border border-border bg-surface px-3 py-2 text-center font-mono text-2xl tracking-[0.4em] outline-none transition-colors focus:border-primary"
          ayuda={`Lo enviamos a ${correo}. Caduca en 15 minutos.`}
        />

        <Boton type="submit" cargando={cargando}>
          Verificar
        </Boton>
      </form>

      <button
        type="button"
        onClick={reenviar}
        className="w-full text-sm text-muted transition-colors hover:text-text"
      >
        No me ha llegado, enviar otro
      </button>

      <p className="text-center text-xs text-subtle text-pretty">
        Revisa también la carpeta de spam.
      </p>
    </div>
  );
}
