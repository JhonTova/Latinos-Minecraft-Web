import { describe, expect, it } from "vitest";

import { formatearEspera } from "./espera";

// Regla: todo valor positivo se redondea hacia arriba al minuto (ceil(x / 60))
// y luego se parte en horas y minutos. Es peor decirle al jugador que vuelva
// antes de tiempo que un minuto después.
describe("formatearEspera", () => {
  it.each([
    [3601, "1 h 1 min"],
    [24120, "6 h 42 min"],
    [24061, "6 h 42 min"], // un segundo de más sube al minuto siguiente
    [24060, "6 h 41 min"], // justo en el límite: minuto exacto, no sube
    [86340, "23 h 59 min"],
  ])("CA-1: %s s dan horas y minutos redondeando hacia arriba: %s", (segundos, esperado) => {
    expect(formatearEspera(segundos)).toBe(esperado);
  });

  it.each([
    [3600, "1 h"],
    [3541, "1 h"], // 59 min 1 s sube a 60 min
    [7200, "2 h"],
    [86399, "24 h"],
    [90000, "25 h"], // sin días
  ])("CA-2: %s s dan horas exactas sin «0 min»: %s", (segundos, esperado) => {
    const texto = formatearEspera(segundos);
    expect(texto).toBe(esperado);
    expect(texto).not.toContain("0 min");
  });

  it.each([
    [60, "1 min"],
    [61, "2 min"],
    [119, "2 min"],
    [120, "2 min"],
    [720, "12 min"],
    [3540, "59 min"],
  ])("CA-3: %s s dan solo minutos redondeando hacia arriba: %s", (segundos, esperado) => {
    expect(formatearEspera(segundos)).toBe(esperado);
  });

  it.each([0.5, 1, 59, 59.9])(
    "CA-4: %s s (positivo y menor de un minuto) da «1 min»",
    (segundos) => {
      expect(formatearEspera(segundos)).toBe("1 min");
    },
  );

  it.each([0, -0.5, -1, -3600])(
    "CA-5: %s s (cero o negativo) da «menos de un minuto» sin signo menos",
    (segundos) => {
      const texto = formatearEspera(segundos);
      expect(texto).toBe("menos de un minuto");
      expect(texto).not.toContain("-");
    },
  );

  it("CA-5: el cero negativo también da «menos de un minuto»", () => {
    expect(formatearEspera(-0)).toBe("menos de un minuto");
  });

  it.each([
    ["undefined", undefined],
    ["null", null],
    ["NaN", Number.NaN],
    ["Infinity", Number.POSITIVE_INFINITY],
    ["-Infinity", Number.NEGATIVE_INFINITY],
    ['"3600" (cadena)', "3600"],
    ["{} (objeto)", {}],
  ])("CA-6: %s no es un número finito y devuelve null", (_nombre, valor) => {
    expect(formatearEspera(valor)).toBeNull();
  });
});
