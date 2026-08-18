import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Empaqueta solo lo que la aplicación usa de verdad, en lugar de arrastrar
  // node_modules entero. La imagen Docker pasa de cientos de MB a decenas.
  output: "standalone",

  // Nunca revelar la versión del framework: le dice a un atacante qué
  // vulnerabilidades conocidas probar primero.
  poweredByHeader: false,

  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          // Impide que la web se incruste en un iframe ajeno (clickjacking).
          { key: "X-Frame-Options", value: "DENY" },
          // Evita que el navegador adivine el tipo de un archivo servido.
          { key: "X-Content-Type-Options", value: "nosniff" },
          // No filtrar la URL completa al navegar a sitios externos.
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
