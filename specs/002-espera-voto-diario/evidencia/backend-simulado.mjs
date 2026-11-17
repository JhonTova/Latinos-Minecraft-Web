// Backend simulado SOLO para la verificación visual de la spec 002.
// Cada token devuelve la respuesta de un criterio de aceptación.
import http from "node:http";
const prob = (title, extra = {}) => ({ type: "about:blank", title, status: 409, detail: "detalle del backend que NO debe mostrarse", ...extra });
const votos = {
  "ca7-diario": [409, prob("vote.daily.used", { segundosRestantes: 24120 })],
  "ca9-diario-sin-tiempo": [409, prob("vote.daily.used")],
  "ca9-diario-invalido": [409, prob("vote.daily.used", { segundosRestantes: "abc" })],
  "ca10-ventana": [409, prob("vote.window.open", { segundosRestantes: 3600 })],
  "ca10-ventana-sin-tiempo": [409, prob("vote.window.open")],
  "ca11-otro": [409, prob("vote.otra.cosa", { detail: "Otro error del backend" })],
  "ca11-no-json": [500, "esto no es json"],
};
http.createServer((req, res) => {
  const m = req.url.match(/^\/api\/v1\/vote-tokens\/([^/]+)(\/vote)?$/);
  console.log(req.method, req.url);
  if (!m) { res.writeHead(404, { "content-type": "application/problem+json" }); return res.end('{"title":"not.found","status":404}'); }
  const token = decodeURIComponent(m[1]);
  if (!m[2]) { res.writeHead(200, { "content-type": "application/json" }); return res.end(JSON.stringify({ servidorSlug: "servidor-demo", servidorNombre: "Servidor Demo", jugador: "JugadorPrueba" })); }
  const [st, cuerpo] = votos[token] ?? [500, "token desconocido"];
  res.writeHead(st, { "content-type": typeof cuerpo === "string" ? "text/plain" : "application/problem+json" });
  res.end(typeof cuerpo === "string" ? cuerpo : JSON.stringify(cuerpo));
}).listen(8099, () => console.log("simulador listo en 8099"));
