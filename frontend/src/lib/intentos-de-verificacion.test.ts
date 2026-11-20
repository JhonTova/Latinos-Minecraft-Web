import { describe, expect, it } from "vitest";

import { avisoDeVerificacion, textoDeIntentos } from "./intentos-de-verificacion";

const CODIGO_NUEVO =
  "No te quedan intentos. Pide un código nuevo con «No me ha llegado, enviar otro».";
const INCORRECTO = "El código no es correcto.";

describe("textoDeIntentos", () => {
  describe("CA-9: plural y singular", () => {
    it.each([
      [4, "Te quedan 4 intentos."],
      [3, "Te quedan 3 intentos."],
      [2, "Te quedan 2 intentos."],
      [10, "Te quedan 10 intentos."],
    ])("%i da el plural", (n, esperado) => {
      expect(textoDeIntentos(n)).toBe(esperado);
    });

    it("1 da el singular", () => {
      expect(textoDeIntentos(1)).toBe("Te queda 1 intento.");
    });
  });

  describe("CA-10: cero y negativos piden código nuevo", () => {
    it.each([0, -1, -5, -100])("%i pide un código nuevo", (n) => {
      expect(textoDeIntentos(n)).toBe(CODIGO_NUEVO);
    });

    it("-0 se trata como cero", () => {
      expect(textoDeIntentos(-0)).toBe(CODIGO_NUEVO);
    });

    it("nunca muestra «0 intentos», un signo menos ni NaN", () => {
      for (const n of [0, -1, -5]) {
        const texto = textoDeIntentos(n) as string;
        expect(texto).not.toContain("0 intentos");
        expect(texto).not.toMatch(/-\d/);
        expect(texto).not.toContain("NaN");
      }
    });
  });

  describe("CA-11: valores no enteros devuelven null", () => {
    it.each([
      ["undefined", undefined],
      ["null", null],
      ["NaN", Number.NaN],
      ["Infinity", Number.POSITIVE_INFINITY],
      ["-Infinity", Number.NEGATIVE_INFINITY],
      ["2.5", 2.5],
      ["-0.5", -0.5],
      ['la cadena "2"', "2"],
      ["un objeto vacío", {}],
      ["un array", [2]],
      ["un booleano", true],
    ])("%s da null", (_caso, valor) => {
      expect(textoDeIntentos(valor)).toBeNull();
    });
  });
});

describe("avisoDeVerificacion", () => {
  describe("CA-12: invalid_code con 2, con 0 y sin propiedad", () => {
    it("con 2 intentos suma el texto de intentos", () => {
      expect(
        avisoDeVerificacion({ title: "verification.invalid_code", intentosRestantes: 2 }),
      ).toBe("El código no es correcto. Te quedan 2 intentos.");
    });

    it("con 1 intento usa el singular", () => {
      expect(
        avisoDeVerificacion({ title: "verification.invalid_code", intentosRestantes: 1 }),
      ).toBe("El código no es correcto. Te queda 1 intento.");
    });

    it("con 0 intentos pide un código nuevo, empezando por «El código no es correcto.»", () => {
      expect(
        avisoDeVerificacion({ title: "verification.invalid_code", intentosRestantes: 0 }),
      ).toBe(`${INCORRECTO} ${CODIGO_NUEVO}`);
    });

    it("sin la propiedad da solo «El código no es correcto.»", () => {
      expect(avisoDeVerificacion({ title: "verification.invalid_code" })).toBe(INCORRECTO);
    });

    it.each([
      ['la cadena "2"', "2"],
      ["2.5", 2.5],
      ["null", null],
      ["NaN", Number.NaN],
    ])("con intentosRestantes %s da solo «El código no es correcto.»", (_caso, valor) => {
      expect(
        avisoDeVerificacion({ title: "verification.invalid_code", intentosRestantes: valor }),
      ).toBe(INCORRECTO);
    });

    it("se compara title y no type", () => {
      expect(
        avisoDeVerificacion({ type: "verification.invalid_code", intentosRestantes: 2 }),
      ).toBeNull();
    });

    it("nunca contiene «0 intentos», un signo menos delante de un número ni NaN", () => {
      for (const valor of [0, -3, Number.NaN, "x", 2.5, undefined]) {
        const texto = avisoDeVerificacion({
          title: "verification.invalid_code",
          intentosRestantes: valor,
        }) as string;
        expect(texto).not.toContain("0 intentos");
        expect(texto).not.toMatch(/-\d/);
        expect(texto).not.toContain("NaN");
      }
    });
  });

  describe("CA-13: too_many_attempts", () => {
    it("sin intentosRestantes pide un código nuevo", () => {
      expect(avisoDeVerificacion({ title: "verification.too_many_attempts" })).toBe(
        CODIGO_NUEVO,
      );
    });

    it("con intentosRestantes 0 pide un código nuevo, sin el prefijo de código incorrecto", () => {
      expect(
        avisoDeVerificacion({ title: "verification.too_many_attempts", intentosRestantes: 0 }),
      ).toBe(CODIGO_NUEVO);
    });
  });

  describe("CA-13: otros títulos, no objeto y null devuelven null", () => {
    it.each([
      ["vote.window.open", { title: "vote.window.open", segundosRestantes: 3600 }],
      ["verification.expired", { title: "verification.expired" }],
      ["mayúsculas en el title", { title: "VERIFICATION.INVALID_CODE", intentosRestantes: 2 }],
      ["objeto vacío", {}],
      ["title que no es cadena", { title: 42, intentosRestantes: 2 }],
      ["null", null],
      ["undefined", undefined],
      ["una cadena", "verification.invalid_code"],
      ["un número", 409],
      ["un array", [{ title: "verification.invalid_code" }]],
      ["un array vacío", []],
    ])("%s da null", (_caso, problema) => {
      expect(avisoDeVerificacion(problema)).toBeNull();
    });
  });
});
