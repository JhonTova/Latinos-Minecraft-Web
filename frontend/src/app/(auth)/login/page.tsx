import { BotonesOAuth } from "@/components/botones-oauth";
import { FormularioLogin } from "@/components/formulario-login";
import { Tarjeta } from "@/components/ui";
import { perfilActual } from "@/lib/sesion";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense } from "react";

export const metadata: Metadata = { title: "Entrar" };

export default async function PaginaLogin() {
  if (await perfilActual()) {
    redirect("/panel");
  }

  return (
    <>
      <Tarjeta titulo="Entrar" descripcion="Accede a tu panel de servidores.">
        <div className="space-y-5">
          <BotonesOAuth />
          <Suspense>
            <FormularioLogin />
          </Suspense>
        </div>
      </Tarjeta>

      <p className="mt-4 text-center text-sm text-muted">
        ¿No tienes cuenta?{" "}
        <Link href="/registro" className="text-primary hover:underline">
          Crear una
        </Link>
      </p>
    </>
  );
}
