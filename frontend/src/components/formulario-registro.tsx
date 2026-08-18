"use client";

import { Aviso, Boton, Campo, mensajeDeError } from "@/components/ui";
import { useRouter } from "next/navigation";
import { useState } from "react";

export function FormularioRegistro() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function enviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setError(null);
    setCargando(true);

    const datos = new FormData(evento.currentTarget);
    const respuesta = await fetch("/api/backend/auth/registro", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: datos.get("email"),
        nombre: datos.get("nombre"),
        password: datos.get("password"),
      }),
    });

    if (!respuesta.ok) {
      setError(await mensajeDeError(respuesta));
      setCargando(false);
      return;
    }

    // Directo a verificar: el código ya está en su correo y es el siguiente paso
    // natural. Mandarlo al panel primero le obligaría a buscar dónde verificar.
    router.push("/panel/verificar");
    router.refresh();
  }

  return (
    <form onSubmit={enviar} className="space-y-4">
      {error && <Aviso tono="error">{error}</Aviso>}

      <Campo
        etiqueta="Tu nombre"
        name="nombre"
        autoComplete="name"
        required
        maxLength={80}
        placeholder="Jhon"
      />
      <Campo
        etiqueta="Correo"
        name="email"
        type="email"
        autoComplete="email"
        required
        placeholder="tu@correo.com"
        ayuda="Te enviaremos un código para confirmarlo."
      />
      <Campo
        etiqueta="Contraseña"
        name="password"
        type="password"
        autoComplete="new-password"
        required
        minLength={10}
        ayuda="Mínimo 10 caracteres. Una frase larga es más segura y más fácil de recordar."
      />

      <Boton type="submit" cargando={cargando}>
        Crear cuenta
      </Boton>
    </form>
  );
}
