# Spec 004 — Decir cuántos intentos quedan al verificar el correo

- **Estado:** Aprobada (17/11/2026, con las respuestas P-1 a P-6)
- **Rama:** `feat/004-intentos-verificacion-correo`
- **Parte:** ambas
- **Riesgo:** 🔴 crítico (`identity`: verificación de correo, ver `backend/AGENTS.md`)

## Problema

Al registrarse, el usuario recibe un código de 6 dígitos y lo teclea en la
pantalla de verificación (`frontend/src/components/formulario-verificar.tsx`).
`EmailVerificationService.verificar` (en
`backend/identity/src/main/java/com/latinosmc/identity/`) cuenta cada fallo y,
con 5 fallos, el código deja de valer (`verification.too_many_attempts`).

Cuando el código no coincide, el backend solo responde `409` con
`title: "verification.invalid_code"` y `detail: "El codigo no es correcto."`.
No dice cuántos intentos quedan.

Caso concreto: Alex se registra en el ordenador y copia el código desde el
móvil. Se equivoca de dígito cuatro veces seguidas y cada vez lee lo mismo,
«El código no es correcto». Al quinto fallo el código ya no vale. En el sexto
intento lee «Demasiados intentos fallidos» sin haber recibido ningún aviso.
Para él, el sitio «le ha bloqueado».

Además, `EmailVerificationService` no tiene ninguna prueba, aunque sostiene la
seguridad de la verificación con tres límites: 15 minutos de vigencia,
5 intentos y 60 segundos entre envíos. El propio Javadoc dice que «lo que no
puede faltar es ninguno de los tres límites». Hoy nada impide que un cambio
quite uno de ellos sin que nadie se entere.

**Fallo confirmado por el mapa del analista (ver «Paradas»):** `verificar` está anotado con
`@Transactional` y, después de `repositorio.sumarIntento(...)`, lanza
`DomainException.Conflict`, que es una `RuntimeException`. Con la configuración
por defecto de Spring eso revierte la transacción, y con ella el `UPDATE` que
suma el intento. En `backend/` no aparece ningún `noRollbackFor`. Por eso el
contador de intentos nunca sube en producción: el límite de
5 intentos no protege nada, y el número de intentos restantes que pide esta spec
sería falso.

## Qué se construye

Cuando el código es incorrecto, la respuesta lleva la propiedad
`intentosRestantes`, igual que los votos llevan `segundosRestantes`. La
pantalla de verificación muestra cuántos intentos quedan y, cuando no queda
ninguno, dice que hay que pedir un código nuevo. Los tres límites del servicio
quedan fijados con pruebas que controlan el tiempo con un `Clock` inyectado.
Los valores de los límites no cambian.

## Criterios de aceptación

Notación: `N` es el número de intentos fallidos que tenía el código vigente
antes de la petición. El límite es 5 (`MAX_INTENTOS`).

### Backend: intentos restantes

| ID | Dado / cuando / entonces |
|---|---|
| CA-1 | Dado un código vigente con `N` fallos (`N` de 0 a 4), cuando se envía un código incorrecto, entonces se suma un intento a ese código y la respuesta es `409` con `title: "verification.invalid_code"` y la propiedad `intentosRestantes = 5 − (N + 1)`: 4, 3, 2, 1 y 0. Debe ser un entero, no un texto. |
| CA-2 | Dado un código vigente con 5 fallos, cuando se envía cualquier código, incluido el correcto, entonces la respuesta es `409` con `title: "verification.too_many_attempts"` y `intentosRestantes = 0`. No se suma ningún intento, el correo no se marca como verificado y el código no se consume. |
| CA-3 | Dado un código incorrecto, cuando el servicio responde, entonces el intento sumado **persiste** aunque la petición termine en error. Una prueba que ejecuta el servicio con su configuración transaccional real (no solo con Mockito) demuestra que el error de código incorrecto no revierte el `sumarIntento`. |
| CA-4 | Dada cualquier otra respuesta de error del backend (por ejemplo `vote.window.open` o `verification.expired`), entonces no lleva `intentosRestantes`. Las respuestas que ya llevaban `segundosRestantes` o `limitePorConexion` no cambian. |

### Backend: los tres límites fijados con pruebas (sin cambiar sus valores)

| ID | Dado / cuando / entonces |
|---|---|
| CA-5 | **Vigencia de 15 min.** Dado un reloj fijo, cuando se envía un código, entonces se guarda con caducidad igual a `ahora + 15 min` exactos. Además, `verificar` busca el código vigente con el instante del reloj inyectado y, si no hay ninguno, responde `409` `verification.expired` sin sumar intentos ni marcar nada. |
| CA-6 | **5 intentos.** Con 4 fallos previos, un código incorrecto todavía se evalúa y suma el quinto (CA-1, `intentosRestantes = 0`). Con 5 fallos previos ya no se evalúa (CA-2). Con 4 fallos previos, el código correcto verifica el correo, que es el borde de «el quinto intento todavía vale». |
| CA-7 | **60 s entre envíos.** Dado un último envío hace 59 s o hace 0 s, cuando se pide otro código, entonces responde `409` `verification.too_soon` y no invalida códigos, no guarda ninguno ni envía correo. Dado un último envío hace exactamente 60 s, hace más de 60 s o ningún envío previo, entonces invalida los anteriores, guarda uno nuevo y envía el correo. |
| CA-8 | **Camino feliz y normalización.** Con el código correcto, incluso con espacios o guiones (`"123 456"`, `"123-456"`), el código se consume y el correo se marca verificado con el `email` del código y el instante del reloj. El código generado tiene siempre 6 dígitos, también si empieza por cero, y el correo enviado lo contiene. Solo se guarda su hash, nunca el código en claro. |

### Frontend

Notación: `t(x)` es la función pura que, en `frontend/src/lib/`, convierte
`intentosRestantes` en el texto que se muestra.

| ID | Dado / cuando / entonces |
|---|---|
| CA-9 | `t(4)` = «Te quedan 4 intentos.», `t(3)` = «Te quedan 3 intentos.», `t(2)` = «Te quedan 2 intentos.» y `t(1)` = «Te queda 1 intento.» (en singular). |
| CA-10 | `t(0)` y cualquier entero negativo devuelven un texto que dice que no quedan intentos y que hay que pedir un código nuevo con el botón «No me ha llegado, enviar otro». Nunca sale «Te quedan 0 intentos» ni un número negativo. |
| CA-11 | Si `x` no es un entero finito (`undefined`, `null`, `NaN`, `Infinity`, `2.5`, `"2"`, `{}`), la función indica que no hay texto que mostrar y no se inventa ningún número. |
| CA-12 | Dada una respuesta `409` `verification.invalid_code` con `intentosRestantes: 2`, cuando el usuario pulsa «Verificar», entonces ve en el aviso de error «El código no es correcto.» y «Te quedan 2 intentos.». Con `intentosRestantes: 0` ve el texto de CA-10. Sin `intentosRestantes` ve solo el mensaje, como hoy. |
| CA-13 | Dada una respuesta `409` `verification.too_many_attempts`, cuando el usuario pulsa «Verificar», entonces ve el texto de CA-10. Cualquier otro error (otro `title`, un cuerpo que no es JSON o un fallo de red) se muestra como hoy, con `mensajeDeError`. |

### Global

| ID | Dado / cuando / entonces |
|---|---|
| CA-14 | Dado el diff de la rama, entonces no cambia `/api/v1/plugin/*`, ninguna migración, ningún `pom.xml` ni `package.json`, y los valores de `VIGENCIA`, `MAX_INTENTOS` y `ESPERA_ENTRE_ENVIOS` siguen siendo 15 min, 5 y 60 s. `mvn -B verify`, `npm run lint`, `npm run test` y `npm run build` pasan. |

## Fuera de alcance

- Cambiar los límites (15 min, 5 intentos, 60 s) o el formato del código.
- Decir en `verification.expired` o `verification.too_soon` cuánto tiempo
  queda. Es una mejora posible para otra spec.
- **Riesgo conocido: la carrera entre peticiones simultáneas** (decisión
  P-3). El `UPDATE attempts = attempts + 1` es atómico, pero la comprobación
  `intentos >= 5` lee el valor antes y por separado. Si llegan K peticiones a la
  vez con 4 fallos, todas leen 4 y todas se evalúan: un atacante obtiene K
  intentos en lugar de 5, y solo lo frena el control de tráfico por IP.
  - **Solución propuesta**, para un `fix/` posterior: `SELECT … FOR UPDATE` en
    `VerificationRepository.codigoVigente`, que serializa los intentos sobre el
    mismo código.
  - **Ninguna prueba sin PostgreSQL** podría demostrar ni este fallo ni su
    arreglo (ADR-0021).
- El control de tráfico por IP del que habla el Javadoc.
- Pruebas contra PostgreSQL real (deuda de ADR-0021) y pruebas de componentes
  React (ADR-0031). La lógica del texto va en `src/lib/` con Vitest.
- `marcarVerificadoPorProveedor` (Google y Discord) y la purga de códigos.
- Comparar `type` en lugar de `title` en el frontend. Se sigue comparando
  `title`, como en la spec 002.

## Paradas

- [ ] Cambia el contrato `/api/v1/plugin/*`
- [ ] Necesita una migración Flyway
- [ ] Escribe en el ledger
- [ ] Agrega una dependencia

Ninguna de las cuatro aplica, según el mapa del analista: la tabla ya tiene
`attempts` (`V10__verificacion_de_email.sql:31`), el endpoint `/auth/verificar`
no es del plugin y la prueba transaccional no necesita dependencias nuevas.
**Pero hay una parada de seguridad**: esta spec también corrige un fallo de seguridad en `identity`
que hoy está en producción (CA-3). El desarrollador debe decidir si va en esta
rama o en un `fix/` aparte antes (pregunta P-1).

## Decisiones del desarrollador

El desarrollador respondió el 17/11/2026 las preguntas P-1 a P-6 de
[plan.md](plan.md). El mapa del `analista` había confirmado la sospecha del
rollback (`EmailVerificationService.java:94-110`).

- **P-1.** El arreglo de la transacción va en esta rama.
- **P-2.** Se aprueba `noRollbackFor`, acotado a la excepción nueva.
- **P-3.** La carrera de peticiones simultáneas queda fuera de alcance. Es un
  riesgo conocido: ver «Fuera de alcance».
- **P-4.** La prueba de CA-3 con un gestor de transacciones simulado basta.
- **P-5.** Se aprueban los textos de D8. Con 0 intentos, el aviso también
  empieza por «El código no es correcto.».
- **P-6.** La 003 ya está en `main` (PR #17), así que el número 004 se queda.

## Cambios durante la implementación

Vacío mientras lo implementado coincida con lo especificado.
