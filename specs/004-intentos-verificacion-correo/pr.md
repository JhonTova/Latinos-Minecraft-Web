**Corrige un fallo de seguridad de `identity` que está en producción: el límite de 5 intentos al verificar el correo no se aplicaba.** No toca `/api/v1/plugin/*`, ni migraciones, ni el ledger, ni agrega dependencias.

# Spec 004: decir cuántos intentos quedan al verificar el correo

Spec: [spec.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/004-intentos-verificacion-correo/spec.md) · Plan: [plan.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/004-intentos-verificacion-correo/plan.md) · Tareas y evidencia: [tareas.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/004-intentos-verificacion-correo/tareas.md)

## Qué cambia y por qué

**El fallo de seguridad.**
- `EmailVerificationService.verificar` era `@Transactional`, sumaba el intento
  fallido y justo después lanzaba `DomainException.Conflict`, que es una
  `RuntimeException`. Spring revertía la transacción, y con ella el
  `UPDATE attempts = attempts + 1`.
- En producción el contador nunca subía: durante los 15 minutos de vigencia se
  podían probar códigos sin límite, y solo frenaba el control de tráfico por IP.
- La prueba de CA-3 lo demostró en rojo: `rollback(...)` en lugar de
  `commit(...)`.

**El arreglo.**
- La excepción nueva `EmailVerificationService.IntentoRechazadoException`
  lleva `intentosRestantes`.
- `@Transactional(noRollbackFor = IntentoRechazadoException.class)` se acota
  solo a esa clase. Un código caducado o cualquier otro error se siguen
  revirtiendo como antes.
- Los tres límites no cambian: 15 min de vigencia, 5 intentos y 60 s entre
  envíos.

**Lo que ve el usuario.**
- Un código incorrecto responde `409` `verification.invalid_code` con la
  propiedad entera `intentosRestantes` (4, 3, 2, 1 y 0).
- Con los intentos agotados responde `verification.too_many_attempts` con
  `intentosRestantes: 0`.
- La pantalla `/panel/verificar` muestra «El código no es correcto. Te quedan 2
  intentos.», «… Te queda 1 intento.» o «… No te quedan intentos. Pide un
  código nuevo con «No me ha llegado, enviar otro».».
- El texto se arma en `frontend/src/lib/intentos-de-verificacion.ts`, probado
  con Vitest.

**Pruebas.** `EmailVerificationService` no tenía ninguna. Ahora sus tres
límites quedan fijados con pruebas de comportamiento y con un `Clock` fijo.

**Efecto visible al desplegar:** desde este cambio, quien falle 5 veces
tendrá que pedir otro código, cosa que hasta ahora nunca pasaba. Por eso el
aviso de intentos va en el mismo PR.

## Matriz criterio → prueba

| Criterio | Prueba |
|---|---|
| CA-1 intentos restantes 4..0 | `EmailVerificationServiceTest › CodigoIncorrecto › restaN`, `› nuloCuentaComoIncorrecto`; `ManejadorDeErroresTest › intentoRechazadoLlevaIntentosRestantesEntero`, `› springEnrutaElIntentoRechazadoAlManejadorEspecifico` |
| CA-2 agotados, 0 y nada se escribe | `› IntentosAgotados › conCincoFallosNiSiquieraElCorrectoVale`, `› conCincoFallosElIncorrectoNoSuma`; `ManejadorDeErroresTest › intentosAgotadosLlevaCero` |
| CA-3 el intento persiste | `EmailVerificationServiceTransaccionTest › codigoIncorrectoHaceCommitYNoRollback` y el control `› caducadoHaceRollback` |
| CA-4 otros errores sin la propiedad | `ManejadorDeErroresTest › conflictoGenericoSinIntentosRestantes`, `› ventanaAbiertaMantieneSegundosRestantes`, `› votoDiarioMantieneSegundosRestantes`, `› cupoDeIpMantieneLimite`, `› springEnrutaUnConflictoNormalAlManejadorGenerico` |
| CA-5 vigencia de 15 min | `› Vigencia › guardaConCaducidadDeQuinceMinutos`, `› buscaVigenteConElRelojInyectado`, `› sinVigenteRespondeExpiradoSinEscribir` |
| CA-6 cinco intentos | `› restaN(4)`, `› IntentosAgotados`, `› ConCuatroFallos › elCorrectoTodaviaVerifica` |
| CA-7 60 s entre envíos | `› EsperaEntreEnvios › a59sRechaza`, `› a0sRechaza`, `› a60sExactosEnvia`, `› aMasDe60sEnvia`, `› sinEnvioPrevioEnvia` |
| CA-8 camino feliz y normalización | `› CaminoFeliz › conEspacios`, `› conGuion`, `› marcaConEmailDelCodigoYRelojInyectado`, `› codigoDeSeisDigitosEnElCorreoYSoloSuHashGuardado` |
| CA-9 a CA-11 textos | `intentos-de-verificacion.test.ts › textoDeIntentos` (plural y singular; cero y negativos; valores no enteros) |
| CA-12 y CA-13 aviso en la pantalla | `intentos-de-verificacion.test.ts › avisoDeVerificacion`. **Sin verificación visual** (ver abajo). |
| CA-14 sin plugin, migraciones, `pom.xml` ni `package.json`; límites intactos | Revisión del diff y los comandos de abajo |

## Evidencia

- **Rojo.**
  - `cd backend && mvn -B -pl identity -am test` → `Tests run: 43, Failures: 1`.
    Falla `codigoIncorrectoHaceCommitYNoRollback` con `Wanted but not invoked:
    platformTransactionManager.commit(...)`: hubo un `rollback(...)` desde
    `TransactionAspectSupport.completeTransactionAfterThrowing`.
  - Las pruebas que usan la excepción nueva no compilaban: 7 ×
    `cannot find symbol: class IntentoRechazadoException`.
  - `cd frontend && npm run test` → `Cannot find module
    './intentos-de-verificacion'`.
- **Verde backend** (ejecución limpia del `revisor`): `cd backend && mvn -B
  verify` da `BUILD SUCCESS`.
  - Pruebas por módulo: shared 50, identity 52, servers 79, voting 37, trust
    35, billing 22 y app 112. Después de la última prueba añadida, app tiene
    114.
  - `ArquitecturaTest` 4/4 y `All coverage checks have been met`.
- **Verde frontend:** `npm run lint` sin errores, `npm run test` con 155/155 y
  `npm run build` compilado, con 15 de 15 páginas. El build lo ejecutó
  `dev-frontend`; el `revisor` repitió lint y test, porque el hook no le deja
  ejecutar el build.
- **Mutación:** `cd backend && mvn -B -Pmutacion -pl identity -am verify`.
  - `EmailVerificationService`: 17/17 mutantes muertos.
    `IntentoRechazadoException`: 1/1. Ningún superviviente en las clases
    tocadas.
  - El único superviviente del módulo está en `AuthService.registrar:55`, que
    esta rama no toca.
- **Cobertura de líneas de `identity`** (JaCoCo, LINE): **106/419 = 0,253**,
  frente a unos 0,157 antes. `cobertura.suelo` sigue en 0,15 en
  `backend/identity/pom.xml`. **Hay que subirlo (propuesto: 0,25) en un commit
  de este mismo PR (ADR-0032).** Ningún rol del arnés puede editar `pom.xml`.

## Lo que este PR no demuestra

- **CA-3 no demuestra que el `UPDATE` llegue a PostgreSQL.** La prueba
  demuestra que el proxy transaccional de Spring hace `commit` y no `rollback`,
  con un gestor de transacciones simulado. Que la fila quede escrita depende de
  la base de datos, y las pruebas del backend no usan ninguna (ADR-0021).
- **Falta la verificación visual de CA-12 y CA-13.** `verificador-ui` no tiene
  navegador en este entorno y el hook le bloquea las peticiones HTTP. **Hay que
  revisar `/panel/verificar` en el navegador antes de fusionar:** móvil y
  escritorio, tema claro y oscuro, con un código incorrecto (2, 1 y 0
  intentos), `too_many_attempts` y un error genérico.

## Riesgos conocidos (fuera de alcance)

- **Carrera de peticiones simultáneas.** El `UPDATE attempts = attempts + 1` es
  atómico, pero la comprobación `intentos >= 5` se lee antes y por separado.
  - Si llegan K peticiones a la vez con 4 fallos, todas se evalúan, y un
    atacante obtiene K intentos en vez de 5.
  - Lo frena el control de tráfico por IP (`ESCRITURA`, 20 peticiones por minuto
    y por IP).
  - **Solución propuesta**, para un `fix/` posterior: `SELECT … FOR UPDATE` en
    `VerificationRepository.codigoVigente`.
  - **Ninguna prueba sin PostgreSQL podría demostrar ni el fallo ni el
    arreglo.**
- **Fallo de red en la pantalla de verificación**, que ya pasaba en `main`.
  - El `fetch` de `formulario-verificar.tsx` no tiene `catch`, así que si cae la
    red del navegador, el botón se queda en «cargando».
  - Si el backend está caído, sale «No hay conexión con el servidor.».
  - Va en un `fix/` aparte.
- Si en el futuro alguien llama a `verificar` dentro de otra transacción, la
  transacción exterior revertiría el intento. Hoy `AuthController.verificar` no
  abre ninguna.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
