import { describe, expect, it } from "vitest";

import { ipDelCliente, rutaPermitida } from "./puente";

const UUID = "018f3c2a-7b1e-7c4d-9a55-0123456789ab";

describe("rutaPermitida", () => {
  it.each([
    "auth/login",
    "auth/registro",
    "auth/yo",
    "auth/oauth/google",
    "auth/oauth/discord/callback",
    "servers",
    "servers/mios",
    "servers/modalidades",
    `servers/${UUID}`,
    `servers/${UUID}/imagenes/icono`,
    `servers/${UUID}/imagenes/banner/origen`,
    `servers/${UUID}/imagenes/automaticas/actualizar`,
    "pagos/slots",
    "pagos/confirmar",
    "servers/mi-servidor-1/votes",
    "admin/soy",
    "admin/sitio/logo",
    "admin/slots/1",
    "admin/slots/6",
    `admin/patrocinadores/${UUID}/aprobar`,
    "admin/servidores",
    "admin/servidores/imagenes-automaticas",
    `admin/servidores/${UUID}/imagenes-automaticas/bloquear`,
    `admin/servidores/${UUID}/suspender`,
  ])("deja pasar %s", (camino) => {
    expect(rutaPermitida(camino)).toBe(true);
  });

  it.each([
    // Rutas del backend que existen pero no son para el navegador.
    "plugin/heartbeat",
    "plugin/rewards/pending",
    "pagos/webhook",
    "actuator/health",
    // Rutas inventadas dentro de un prefijo permitido.
    "auth/otra",
    "admin/usuarios",
    "pagos/reembolsar",
    // Un prefijo válido no abre lo que cuelga de él.
    "auth/login/extra",
    "servers/mios/extra",
    `servers/${UUID}/borrar`,
    `servers/${UUID}/imagenes/fondo`,
    `admin/patrocinadores/${UUID}/borrar`,
    // Solo hay seis espacios destacados.
    "admin/slots/0",
    "admin/slots/7",
    "admin/slots/10",
    // El identificador tiene que parecer un UUID.
    "servers/123/imagenes/icono",
    `admin/servidores/${UUID}x/suspender`,
    // Intentos de salir del camino.
    "servers/../admin/soy",
    "auth/login/../../plugin/heartbeat",
    "servers/mi%2Fservidor/votes",
    "servers/Mi_Servidor/votes",
    // Bordes.
    "",
    "/auth/login",
    "auth/login/",
    " auth/login",
    "AUTH/LOGIN",
  ])("responde que no a %j", (camino) => {
    expect(rutaPermitida(camino)).toBe(false);
  });

  it("no acepta un slug de más de 64 caracteres en la ruta de voto", () => {
    expect(rutaPermitida(`servers/${"a".repeat(64)}/votes`)).toBe(true);
    expect(rutaPermitida(`servers/${"a".repeat(65)}/votes`)).toBe(false);
  });
});

describe("ipDelCliente", () => {
  it("devuelve la IP cuando la cabecera trae una sola", () => {
    expect(ipDelCliente("203.0.113.7")).toBe("203.0.113.7");
  });

  it("se queda con la primera cuando la cabecera trae una lista", () => {
    expect(ipDelCliente("203.0.113.7, 10.0.0.1, 10.0.0.2")).toBe("203.0.113.7");
  });

  it("quita los espacios de alrededor", () => {
    expect(ipDelCliente("  203.0.113.7  ,10.0.0.1")).toBe("203.0.113.7");
  });

  it("devuelve 0.0.0.0 cuando no hay cabecera", () => {
    expect(ipDelCliente(null)).toBe("0.0.0.0");
  });

  it("devuelve 0.0.0.0 cuando la cabecera está vacía o en blanco", () => {
    expect(ipDelCliente("")).toBe("0.0.0.0");
    expect(ipDelCliente("   ")).toBe("0.0.0.0");
  });

  it("devuelve 0.0.0.0 cuando el primer elemento de la lista está vacío", () => {
    expect(ipDelCliente(", 203.0.113.7")).toBe("0.0.0.0");
  });
});
