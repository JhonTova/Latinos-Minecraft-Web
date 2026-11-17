# Spec 001 — Fijar el comportamiento del motor antifraude

- **Estado:** Implementada, pendiente de fusión (aprobada el 13/11/2026; P-1 a P-8 quedan abiertas como decisiones de producto)
- **Rama:** `feat/001-fijar-motor-antifraude`
- **Parte:** backend
- **Riesgo:** 🔴 crítico (módulo `trust`, ver `backend/AGENTS.md`)

## Problema

La primera medición de mutación (`docs/calidad/README.md`, línea base del
13/11/2026) dejó 10 mutantes vivos dentro de `RuleBasedTrustEngine.evaluar`:
la fuerza de las pruebas de `trust` es del 52 %, frente al 97–100 % de los
otros módulos críticos.

Caso concreto: si alguien cambia `identidades > MAX_IDENTIDADES_POR_IP` por
`>=`, una casa con tres hermanos empieza a sumar 15 puntos de riesgo; si cambia
`riesgo >= UMBRAL_RECHAZO` por `>`, un voto con exactamente 60 puntos deja de
rechazarse; si el tope de 45 de la regla de IP pasa a 450, una granja de cuentas
no cambia de veredicto en ninguna prueba actual. Las pruebas de hoy comprueban
que «aparece la señal» o que «el riesgo es menor que», nunca el límite exacto
ni cuánto suma cada regla. Nadie se enteraría hasta ver el ranking falseado o a
jugadores legítimos bloqueados. El antifraude es lo que protege la
credibilidad del ranking.

## Qué se construye

Pruebas, no código de producción. Se especifica **el comportamiento actual** del
motor —cada regla con su límite exacto y su aportación al puntaje, el recorte a
0–100 y el umbral de rechazo— para que cualquier cambio de esas cifras haga
fallar una prueba. El comportamiento en producción no cambia.

## Criterios de aceptación

Notación: «base» es una petición con identidad verificable (Mojang), 1
identidad en la IP en 24 h, 1 voto de la IP en 1 h e historial de 5 votos; en
ella el motor acepta con riesgo 0 y sin señales. Cada criterio cambia solo lo
que nombra.

| ID | Dado / cuando / entonces |
|---|---|
| CA-1 | Dada la petición base, cuando se evalúa, entonces se acepta con riesgo 0 y sin ninguna señal. |
| CA-2 | Dada una identidad no verificable (offline-mode), cuando se evalúa, entonces el riesgo sube exactamente 25 y aparece una señal `IDENTITY_UNVERIFIABLE` con aportación 25; con identidad verificable esa señal no aparece. |
| CA-3 | Dadas N identidades distintas en la IP en 24 h, cuando se evalúa, entonces: con N = 3 no aparece `IP_MULTI_IDENTITY` ni sube el riesgo; con N = 4, 5 y 6 la señal aporta exactamente 15, 30 y 45; con N = 7 y con N = 30 aporta 45 (tope). El detalle de la señal incluye N. |
| CA-4 | Dados V votos de la IP en 1 h, cuando se evalúa, entonces: con V = 6 no aparece `IP_BURST` ni sube el riesgo; con V = 7 y V = 10 la señal aporta exactamente 10 y 40; con V = 11 y V = 50 aporta 40 (tope). El detalle de la señal incluye V. |
| CA-5 | Dado un historial de H votos de la identidad, cuando se evalúa, entonces: con H = 0 aparece `IDENTITY_NEW` con aportación +10; con H = 1 y H = 9 no aparece ninguna señal de historial; con H = 10 y H = 50 aparece `IDENTITY_TRUSTED` con aportación −15 y el detalle incluye H. |
| CA-6 | Dada una combinación de reglas, cuando se evalúa, entonces el riesgo es la suma exacta de las aportaciones de sus señales (p. ej. offline + 4 identidades + 7 votos + historial 0 = 25 + 15 + 10 + 10 = 60). |
| CA-7 | Dada una suma negativa (identidad verificable con historial 10: −15), cuando se evalúa, entonces el riesgo es 0; dada una suma mayor de 100 (offline + 30 identidades + 50 votos + historial 0 = 120), entonces el riesgo es 100. |
| CA-8 | Dado un riesgo de 55 (offline + 5 identidades, historial 5), cuando se evalúa, entonces se acepta sin motivo de rechazo; dado un riesgo de exactamente 60 (el caso de CA-6), entonces se rechaza con el motivo `fraud.risk_too_high` y conserva sus señales. |
| CA-9 | Dada una petición, cuando se evalúa, entonces el motor consulta las identidades por IP desde exactamente `momento − 24 h`, los votos por IP desde exactamente `momento − 1 h`, ambas con el hash de la IP de la petición, y el historial con el hash de identidad de la petición. |
| CA-10 | Dadas las pruebas nuevas, cuando se ejecuta `mvn -B -Pmutacion -pl trust -am verify`, entonces no sobrevive ningún mutante en `RuleBasedTrustEngine` (el informe anterior a las pruebas, con sus supervivientes, queda como evidencia del «rojo»). |
| CA-11 | Dadas las pruebas nuevas, cuando se ejecuta `mvn -B verify` en `backend/`, entonces pasa; si la cobertura de líneas de `trust` sube, su `cobertura.suelo` sube con ella (ADR-0032), y nunca baja. |
| CA-12 | Dado el diff de la rama, entonces no modifica nada bajo `backend/*/src/main/`: el comportamiento en producción es el mismo. |

## Fuera de alcance

- **Arreglar el motor.** Si al especificar aparece algo que parece un error,
  se deja como pregunta en esta spec y, si se decide cambiarlo, va en otra.
- Los mutantes de `TrustQueries` (consultas JDBC, sin cobertura): necesitan
  PostgreSQL real, que es la deuda de ADR-0021.
- Cambiar los umbrales, los pesos o agregar reglas nuevas (ADR-0005).
- Los mutantes de los otros módulos críticos.
- Hacer obligatoria la mutación en la CI con un umbral (eso es ADR-0032).

## Paradas

- [ ] Cambia el contrato `/api/v1/plugin/*`
- [ ] Necesita una migración Flyway
- [ ] Escribe en el ledger
- [ ] Agrega una dependencia

Ninguna aplica: solo se agregan pruebas en `backend/trust/src/test` y, si
procede, se sube el suelo de cobertura.

## Preguntas abiertas (posibles errores del motor, no se arreglan aquí)

Al fijar las cifras salen combinaciones que conviene confirmar que son
intencionadas. Las pruebas fijan el comportamiento actual en todos los casos.

- **P-1. Granja de cuentas premium.** Una identidad Mojang nueva desde una IP
  con 30 identidades suma 45 + 10 = 55 y **se acepta**. ¿Es intencionado
  (una cuenta premium cuesta dinero) o una granja de premium debería rechazarse?
- **P-2. Historial en offline-mode.** Una identidad offline con 10 votos previos
  desde una IP con 30 identidades suma 25 + 45 − 15 = 55 y **se acepta**. El
  historial «es caro de fabricar», pero un nombre offline es gratis y su
  historial se puede ir acumulando. ¿Debe compensar igual que en una identidad
  verificable?
- **P-3. El recorte se come la compensación.** Las señales se recortan al final:
  con −15 de historial y 0 de lo demás, la suma de señales es −15 pero el
  riesgo es 0. Si en el futuro se explica un veredicto sumando las señales, no
  cuadra con el puntaje. ¿Se acepta así?
- **P-4. Los puntajes van de 5 en 5.** Todas las aportaciones son múltiplos de
  5, así que el riesgo nunca vale entre 56 y 59: el umbral de 60 se comporta
  igual que uno de 56. Solo es una observación; no cambia nada.

Las siguientes salieron del mapa del `analista` (ver [plan.md](plan.md)):

- **P-5. IP nula o en blanco.** `hashIp` devuelve `null` y `TrustQueries`
  devuelve 0, así que el voto se evalúa sin las reglas de IP y sin ninguna
  señal que lo diga. ¿Se acepta, o merece una spec propia (por explicabilidad,
  ADR-0005)? Esta spec no lo fija.
- **P-6. Qué cuentan N y V.** Las consultas cuentan votos previos de cualquier
  estado (también los rechazados), y N incluye al propio votante si ya votó
  desde esa IP en 24 h. «Más de 3 identidades» no equivale a «más de 3
  jugadores en la casa». ¿Es lo que se quiere?
- **P-7. Dos reglas de IP distintas.** `CastVoteService` limita jugadores por IP
  con `reglas.ventana()` y `maxJugadoresPorIp()` excluyendo al votante; el motor
  usa 24 h y 3 fijos sin excluirlo. ¿Es intencionado?
- **P-8. ADR-0005 dice «configurable en caliente».** Las cifras del motor están
  fijas en el código. ¿Se registra como deuda?
