"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

export function BotonSalir() {
  const router = useRouter();
  const [saliendo, setSaliendo] = useState(false);

  async function salir() {
    setSaliendo(true);
    // La sesión se revoca también en el servidor, no solo se borra la cookie:
    // una cookie borrada en un navegador no invalida el token si alguien lo
    // copió antes.
    await fetch("/api/backend/auth/logout", { method: "POST" });
    router.push("/");
    router.refresh();
  }

  return (
    <button
      type="button"
      onClick={salir}
      disabled={saliendo}
      className="rounded-md border border-border px-3 py-1.5 text-sm text-muted transition-colors hover:bg-surface-sunken hover:text-text disabled:opacity-60"
    >
      {saliendo ? "Saliendo…" : "Salir"}
    </button>
  );
}
