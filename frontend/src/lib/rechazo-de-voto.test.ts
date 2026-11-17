import { describe, expect, it } from "vitest";

import { clasificarRechazo } from "./rechazo-de-voto";

const RESPALDO = "No se pudo registrar tu voto. Inténtalo otra vez.";
const DETAIL_DIARIO = "Ya has votado hoy. Solo se puede votar a un servidor al dia.";

describe("clasificarRechazo", () => {
  it("CA-7: voto diario con tiempo da tipo voto-diario y la espera formateada, sin mensaje", () => {
    const resultado = clasificarRechazo({
      title: "vote.daily.used",
      detail: DETAIL_DIARIO,
      segundosRestantes: 24120,
    });
    // toEqual del objeto entero: no debe aparecer el detail del backend como mensaje.
    expect(resultado).toEqual({ tipo: "voto-diario", espera: "6 h 42 min" });
  });

  it.each([
    ["ausente", { title: "vote.daily.used", detail: DETAIL_DIARIO }],
    ["null", { title: "vote.daily.used", segundosRestantes: null }],
    ['"abc"', { title: "vote.daily.used", segundosRestantes: "abc" }],
  ])("CA-9: voto diario con segundosRestantes %s da espera null", (_caso, problema) => {
    expect(clasificarRechazo(problema)).toEqual({ tipo: "voto-diario", espera: null });
  });

  it("CA-10: ventana por servidor con 3600 s da «1 h»", () => {
    expect(
      clasificarRechazo({ title: "vote.window.open", segundosRestantes: 3600 }),
    ).toEqual({ tipo: "ventana-servidor", espera: "1 h" });
  });

  it("CA-10: ventana por servidor sin el dato da espera null", () => {
    expect(clasificarRechazo({ title: "vote.window.open" })).toEqual({
      tipo: "ventana-servidor",
      espera: null,
    });
  });

  it("CA-10: ventana por servidor con un valor no válido da espera null", () => {
    expect(
      clasificarRechazo({ title: "vote.window.open", segundosRestantes: "3600" }),
    ).toEqual({ tipo: "ventana-servidor", espera: null });
  });

  it("CA-11: otro title con detail cadena da tipo otro con ese detail", () => {
    expect(
      clasificarRechazo({ title: "vote.ip.quota", detail: "Demasiados votos desde tu red." }),
    ).toEqual({ tipo: "otro", mensaje: "Demasiados votos desde tu red." });
  });

  it("CA-11: otro title con segundosRestantes no se trata como espera", () => {
    expect(
      clasificarRechazo({ title: "vote.otro", detail: "Algo", segundosRestantes: 3600 }),
    ).toEqual({ tipo: "otro", mensaje: "Algo" });
  });

  it.each([
    ["null", null],
    ["undefined", undefined],
    ["un array", []],
    ["una cadena", "vote.daily.used"],
    ["un número", 409],
    ["un objeto vacío", {}],
    ["detail que es un objeto", { title: "vote.otro", detail: {} }],
    ["detail que es un número", { title: "vote.otro", detail: 42 }],
    ["detail vacío", { title: "vote.otro", detail: "" }],
    ["detail ausente", { title: "vote.otro" }],
  ])("CA-11: %s da tipo otro con el mensaje de respaldo", (_caso, problema) => {
    expect(clasificarRechazo(problema)).toEqual({ tipo: "otro", mensaje: RESPALDO });
  });

  it("CA-11: la comparación del title es exacta (mayúsculas caen en otro)", () => {
    expect(
      clasificarRechazo({
        title: "VOTE.DAILY.USED",
        detail: "Detalle",
        segundosRestantes: 24120,
      }),
    ).toEqual({ tipo: "otro", mensaje: "Detalle" });
  });

  it("CA-11: se compara title y no type", () => {
    expect(
      clasificarRechazo({
        type: "vote.daily.used",
        detail: "Detalle",
        segundosRestantes: 24120,
      }),
    ).toEqual({ tipo: "otro", mensaje: "Detalle" });
  });
});
