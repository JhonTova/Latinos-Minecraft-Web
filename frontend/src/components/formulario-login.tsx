"use client";

import { Aviso, Boton, Campo, mensajeDeError } from "@/components/ui";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

export function FormularioLogin() {
  const router = useRouter();
  const parametros = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setCargando(true);

    const datos = new FormData(evento.currentTarget);
    const respuesta = await fetch("/api/backend/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: datos.get("email"),
        password: datos.get("password"),
      }),
    });

    if (!respuesta.ok) {
      // El mensaje viene de la API y no distingue si falló el correo o la
      // contraseña. Es intencionado: distinguirlo permitiría averiguar qué
      // cuentas existen.
      setError(await mensajeDeError(respuesta));
      setCargando(false);
      return;
    }

    // Vuelve a donde intentaba ir. Solo se aceptan rutas internas: sin esa
    // comprobacion, un enlace del tipo ?volver=https://sitio-falso.com
    // convertiria el login en un trampolin hacia una web de phishing con
    // nuestro dominio como aval.
    const volver = parametros.get("volver");
    const destino = volver?.startsWith("/") && !volver.startsWith("//")
      ? volver
      : "/panel";

    router.push(destino);
    router.refresh();
  }

  return (
    <form onSubmit={enviar} className="space-y-4">
      {error && <Aviso tono="error">{error}</Aviso>}

      <Campo
        etiqueta="Correo"
        name="email"
        type="email"
        autoComplete="email"
        required
        placeholder="tu@correo.com"
      />
      <Campo
        etiqueta="Contraseña"
        name="password"
        type="password"
        autoComplete="current-password"
        required
      />

      <Boton type="submit" cargando={cargando}>
        Entrar
      </Boton>
    </form>
  );
}
