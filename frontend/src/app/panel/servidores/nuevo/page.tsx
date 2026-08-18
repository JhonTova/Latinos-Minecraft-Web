import { FormularioServidor } from "@/components/formulario-servidor";
import { Tarjeta } from "@/components/ui";
import { perfilActual } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Añadir servidor" };

export default async function PaginaNuevoServidor() {
  const perfil = await perfilActual();

  // La API lo rechazaría igualmente, pero enseñar el formulario para que falle
  // al enviarlo sería hacerle perder el tiempo al usuario.
  if (!perfil?.emailVerificado) {
    redirect("/panel/verificar");
  }

  return (
    <main className="mx-auto w-full max-w-lg flex-1 px-4 py-8 sm:px-6">
      <Link
        href="/panel"
        className="mb-4 inline-block text-sm text-muted transition-colors hover:text-text"
      >
        ← Volver
      </Link>

      <Tarjeta
        titulo="Añadir servidor"
        descripcion="Al terminar te daremos la clave para conectar el plugin."
      >
        <FormularioServidor />
      </Tarjeta>
    </main>
  );
}
