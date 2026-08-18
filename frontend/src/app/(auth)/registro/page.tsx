import { BotonesOAuth } from "@/components/botones-oauth";
import { FormularioRegistro } from "@/components/formulario-registro";
import { Tarjeta } from "@/components/ui";
import { perfilActual } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Crear cuenta" };

export default async function PaginaRegistro() {
  // Quien ya tiene sesión no debería ver este formulario: se comprueba en el
  // servidor para que no llegue a pintarse ni un instante.
  if (await perfilActual()) {
    redirect("/panel");
  }

  return (
    <>
      <Tarjeta
        titulo="Crea tu cuenta"
        descripcion="Registra tus servidores y empieza a recibir votos."
      >
        <div className="space-y-5">
          <BotonesOAuth />
          <FormularioRegistro />
        </div>
      </Tarjeta>

      <p className="mt-4 text-center text-sm text-muted">
        ¿Ya tienes cuenta?{" "}
        <Link href="/login" className="text-primary hover:underline">
          Entrar
        </Link>
      </p>
    </>
  );
}
