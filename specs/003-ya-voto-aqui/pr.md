⚠️ **Cambia el comportamiento del contrato `/api/v1/plugin/*` (no su forma):** `POST /api/v1/plugin/vote-tokens` ahora puede responder `motivo: "YA_VOTO_AQUI"`. La release publicada del plugin, `v0.2.0`, ya maneja ese valor, así que **no hace falta publicar una versión nueva del plugin**. No hay migración, ni ledger, ni dependencias nuevas.

## Qué cambia y por qué

`CastVoteService.puedeVotar` es la consulta que hace el plugin antes de darle un código de voto al jugador. Con la regla «un voto al día» desactivada, respondía siempre que sí. Un jugador que ya había votado a ese servidor recibía su código, abría el enlace y solo entonces `votar()` lo rechazaba con `vote.window.open`. El comentario del código lo justificaba diciendo que cada servidor tenía su propia ventana, y eso dejó de ser cierto con ADR-0023: la ventana es la misma para todos y la fija la plataforma.

Ahora, con la regla global desactivada, `puedeVotar` consulta el último voto **aceptado** de esa identidad a **ese** servidor. Si sigue dentro de la ventana, responde `YA_VOTO_AQUI` con el tiempo restante. Usa el mismo borde semiabierto que `votar()` (ADR-0011): en el instante exacto en que se cierra la ventana, ya puede votar.

Sigue siendo una consulta de cortesía. No escribe nada, y quien decide es `votar()`, que no cambia. Con la regla global activada, el comportamiento es el mismo de antes (`YA_VOTO_HOY`).

**Plugin.** El desarrollador lo comprobó en `JhonTova/LatinosMC`: en `v0.2.0`, `ComandoVotar.avisarQueYaVoto` usa la clave `ya-voto-aqui` («Ya votaste a este servidor. Vuelve en {restante}.») y lee `segundosRestantes`. La forma de `RespuestaToken` no cambia.

- Spec: [spec.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/003-ya-voto-aqui/spec.md)
- Plan: [plan.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/003-ya-voto-aqui/plan.md)
- Tareas y evidencia: [tareas.md](https://github.com/JhonTova/Latinos-Minecraft-Web/blob/main/specs/003-ya-voto-aqui/tareas.md)

## Archivos

| Archivo | Cambio |
|---|---|
| `backend/voting/src/main/java/com/latinosmc/voting/CastVoteService.java` | Solo cambian `puedeVotar`, su Javadoc y el comentario, que ahora cita ADR-0023 |
| `backend/voting/src/test/java/com/latinosmc/voting/CastVoteServiceTest.java` | 11 pruebas nuevas de `puedeVotar`; antes no tenía ninguna |
| `backend/app/src/test/java/com/latinosmc/app/web/PluginControllerTest.java` | Nueva: fija la forma de la respuesta al plugin |
| `specs/003-ya-voto-aqui/` | Spec, plan, tareas y esta descripción |

## Matriz criterio → prueba

| Criterio | Prueba |
|---|---|
| CA-1, quedan 18 h | `CastVoteServiceTest#puedeVotar_sinReglaGlobal_votoAquiHaceSeisHoras_respondeYaVotoAquiConDieciochoHoras` y `#puedeVotar_sinReglaGlobal_ventanaDeDoceHoras_votoHaceSeisHoras_respondeSeisHoras` |
| CA-2, borde semiabierto | `#puedeVotar_sinReglaGlobal_finDeVentanaJustoAhora_puede` y `#puedeVotar_sinReglaGlobal_unSegundoAntesDelFin_respondeYaVotoAquiConUnSegundo` |
| CA-3, fuera de la ventana | `#puedeVotar_sinReglaGlobal_votoAquiFueraDeVentana_puede` y `#puedeVotar_sinReglaGlobal_ventanaDeDoceHoras_votoHaceTrece_puede` |
| CA-4, votar a otro servidor no bloquea | `#puedeVotar_sinReglaGlobal_votoRecienteEnOtroServidor_puede` |
| CA-5, servidor e identidad correctos | `#puedeVotar_sinReglaGlobal_consultaSoloElServidorYLaIdentidadCorrectos` y `#puedeVotar_sinReglaGlobal_usaElServidorIdRecibido` |
| CA-6, regla global sin cambios | `#puedeVotar_conReglaGlobal_votoHoyEnCualquiera_respondeYaVotoHoy` y `#puedeVotar_conReglaGlobal_sinVotoHoy_puede` |
| CA-7, no escribe nada | `verificarQueNoHuboEfectos()` en las 11 pruebas de `puedeVotar` |
| CA-8, respuesta al plugin | `PluginControllerTest#emitirToken_yaVotoAqui_devuelveMotivoYSegundosSinCodigo` |
| CA-9, contrato, ADR y suite | Revisión del diff por el `revisor` y `mvn -B verify` en verde |

## Evidencia

- **Orden TDD:** `docs(specs)` → `test(voting)` (rojo) → `feat(voting)` (verde) → dos `test(voting)` con las correcciones de la revisión.
- **Rojo:** `cd backend && mvn -B -pl voting -am test` da `Tests run: 46, Failures: 3, Errors: 0`. Las tres fallan por aserción: CA-1 esperaba `[false, YA_VOTO_AQUI, PT18H]` y obtuvo `[true, null, PT0S]`; CA-2, un segundo antes, esperaba `[false, YA_VOTO_AQUI, PT1S]`; CA-5 esperaba `puede = false`.
- **Verde:** `mvn -B -f backend/pom.xml verify` da BUILD SUCCESS, sin fallos ni errores en ningún módulo: `shared` 50, `identity` 27, `servers` 79, `voting` 46, `trust` 35, `billing` 22 y `app` 107 pruebas. `ArquitecturaTest` pasa 4 de 4 y `jacoco:check` dice «All coverage checks have been met». Después de las correcciones, `voting` tiene 48 pruebas y `CastVoteServiceTest` 31, todas en verde.
- **Mutación:** `mvn -B -Pmutacion -pl voting -am verify`. En `puedeVotar` hay **9 mutantes y los 9 mueren**, así que no queda ningún superviviente. En todo `voting` hay 106 mutantes, con una fuerza de las pruebas del 98 %. El único superviviente de `CastVoteService` está en la línea 317 (`CupoDeIpAgotadoException`). Ya existía antes de esta rama y queda fuera del diff.
- **Cobertura de `voting`:** **0,408** de líneas (137 de 336), con el suelo en 0,36. ADR-0032 pide subir `cobertura.suelo` (`backend/voting/pom.xml`) en este mismo PR. Ningún agente tiene permiso para editar `pom.xml`, así que el suelo se sube **en un commit propio de esta rama, fuera del sistema de agentes**, después del cierre de la spec.
- **Revisión:** el `revisor` hizo tres vueltas, la última de confirmación con `verify` en verde, y no encontró ningún hallazgo alto. Lo corregido en esta rama:
  - M-1: una ventana fija no la detectaba ninguna prueba.
  - M-2: la evidencia estaba incompleta.
  - B-1 y B-2: pruebas que no podían fallar.
  - B-3: una etiqueta de criterio equivocada.
  - B-4 y B-5: referencias de los documentos.

  Queda M-4, el suelo de cobertura, que se resuelve con el commit del punto anterior. El detalle está en `tareas.md`.

## Fuera de alcance

- `segundosRestantes` se trunca con `Duration.toSeconds()`. Si queda menos de un segundo, sale `0` con `puedeVotar: false`. Ya pasaba con `YA_VOTO_HOY`. Es una posible mejora para otra spec.
- Con la regla global activada sigue saliendo `YA_VOTO_HOY` aunque el voto fuera a este mismo servidor.
- El estado de ADR-0016 («Propuesto») se actualiza en otra rama `docs/`.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
