import { FormularioVerificar } from "@/components/formulario-verificar";
import { Tarjeta } from "@/components/ui";
import { perfilActual } from "@/lib/sesion";
import type { Metadata } from "next";
import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Verificar correo" };

export default async function PaginaVerificar() {
  const perfil = await perfilActual();

  // Ya verificado: no tiene sentido enseñar el formulario otra vez.
  if (perfil?.emailVerificado) {
    redirect("/panel");
  }

  return (
    <main className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center px-4 py-12">
      <Tarjeta
        titulo="Confirma tu correo"
        descripcion="Es lo que evita que se creen cuentas falsas para inflar el ranking."
      >
        <FormularioVerificar correo={perfil?.email ?? ""} />
      </Tarjeta>
    </main>
  );
}
