// Backend simulado SOLO para la verificación visual de la spec 004.
// /auth/yo devuelve un perfil sin verificar; /auth/verificar responde según el código.
import http from "node:http";
const prob = (status, title, extra = {}) => [status, { type: "about:blank", title, status, detail: "detalle del backend", ...extra }];
const porCodigo = {
  "222222": prob(409, "verification.invalid_code", { intentosRestantes: 2 }),
  "111111": prob(409, "verification.invalid_code", { intentosRestantes: 1 }),
  "000000": prob(409, "verification.invalid_code", { intentosRestantes: 0 }),
  "333333": prob(409, "verification.invalid_code"),
  "999999": prob(409, "verification.too_many_attempts", { intentosRestantes: 0 }),
  "555555": prob(500, "error.interno", { detail: "Algo falló en el servidor" }),
};
http.createServer((req, res) => {
  let cuerpo = ""; req.on("data", (c) => (cuerpo += c));
  req.on("end", () => {
    console.log(req.method, req.url, cuerpo);
    if (req.url === "/api/v1/auth/yo") {
      res.writeHead(200, { "content-type": "application/json" });
      return res.end(JSON.stringify({ id: "u1", email: "jugador@ejemplo.com", nombre: "Jugador", emailVerificado: false }));
    }
    if (req.url === "/api/v1/auth/verificar") {
      const codigo = (JSON.parse(cuerpo || "{}").codigo ?? "").toString();
      if (codigo === "777777") return req.socket.destroy(); // fallo de red
      const [st, p] = porCodigo[codigo] ?? prob(409, "verification.invalid_code", { intentosRestantes: 4 });
      res.writeHead(st, { "content-type": "application/problem+json" });
      return res.end(JSON.stringify(p));
    }
    res.writeHead(404, { "content-type": "application/problem+json" }); res.end('{"title":"not.found","status":404}');
  });
}).listen(8098, () => console.log("simulador listo en 8098"));
