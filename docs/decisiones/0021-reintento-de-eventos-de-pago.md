# ADR-0021 — Un evento de pago solo se da por hecho cuando lo está

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-15
- **Decide:** ingeniería
- **Afecta a:** [ADR-0004 — Pagos sin tarjetas](0004-pagos-sin-tarjetas.md)
  (el manejo de eventos, no la decisión de pasarela)

---

## Contexto

El manejador de eventos de la pasarela hacía esto, en este orden:

1. Registrar el evento en `payment_webhooks`. Si ya estaba, responder 200 y salir.
2. Aplicarlo (anotar el asiento, reservar el espacio).
3. Marcarlo como procesado.
4. Si algo fallaba: marcarlo como procesado **con el error escrito** y responder
   500 para que la pasarela reintentase.

El paso 4 anulaba el paso 4. Al reenviar, la pasarela se topaba con el paso 1 —el
evento ya constaba— y recibía un 200 sin que nadie volviera a intentar aplicarlo.

**El resultado:** una caída momentánea de la base de datos en el instante
equivocado dejaba un cobro real cobrado en PayPal, sin asiento en el ledger y sin
espacio reservado. El cliente había pagado y no salía destacado. Y en silencio:
la pasarela tenía su 200, así que dejaba de insistir, y el único rastro era una
fila con un `error` que nadie mira.

No hacía falta que fuera un desastre. Un despliegue en el segundo justo, un
`deadlock`, un tiempo de espera agotado.

## Decisión

**Un evento solo deja de reintentarse cuando termina, no cuando se recibe.**

La condición de "ya visto" pasa a ser "ya terminado":

```sql
INSERT INTO payment_webhooks (...)
VALUES (...)
ON CONFLICT (gateway, event_id) DO UPDATE
   SET received_at = now(), error = NULL
 WHERE payment_webhooks.processed_at IS NULL
```

- Devuelve una fila —y con ella el turno— si el evento nunca llegó a terminarse.
- No devuelve nada si ya se procesó con éxito, que es lo que impide contar un
  cobro dos veces cuando la pasarela reenvía.

Y el fallo deja de cerrar el evento:

| Qué pasó | Qué se escribe | Qué se responde |
|---|---|---|
| Se aplicó bien | `processed_at` + sin error | 200 |
| Falló algo pasajero | solo `error`, sigue pendiente | 500 — que reintente |
| Falló de forma definitiva (`DomainException.NotFound`) | `processed_at` + error | 200 — no insistas |

La tercera fila importa tanto como la segunda: un pedido que no existe no va a
existir por reintentarlo, y hacer que la pasarela insista tres días contra un
evento condenado solo entierra los fallos que sí se pueden arreglar.

### Reintentar exige poder repetir sin duplicar

Reintentar es seguro solo si aplicar dos veces da lo mismo que aplicar una:

- **Cobros:** ya lo era. `marcarPagado` no afecta a ninguna fila si el pedido ya
  estaba pagado, y el manejador se sale antes de anotar nada.
- **Reembolsos:** no lo era. Un evento repetido añadía un segundo asiento
  negativo, y la organización aparecía debiendo un dinero que nunca cobró. Ahora
  se comprueba antes de escribir.

Y como comprobar antes de escribir depende de que alguien se acuerde de hacerlo,
la garantía baja a la base de datos (`V18`):

```sql
CREATE UNIQUE INDEX ledger_entries_un_asiento_por_pedido
    ON ledger_entries (order_id, entry_type)
 WHERE entry_type IN ('PAYMENT_RECEIVED', 'REFUND');
```

Un pedido no puede cobrarse dos veces ni reembolsarse dos veces, lo escriba quien
lo escriba. El ledger ya era inmutable; inmutable no es lo mismo que correcto.

## Consecuencias

**A favor**

- Un cobro no se pierde por un tropiezo pasajero: la pasarela reintenta y la
  segunda vez se aplica.
- Duplicar dinero deja de depender de que el código lo recuerde.
- Los fallos definitivos se distinguen de los pasajeros, así que la tabla de
  eventos vuelve a servir para conciliar.

**En contra**

- Dos entregas simultáneas del mismo evento pueden coger el turno a la vez
  (antes la segunda quedaba fuera por el `INSERT`). Es lo que obliga a que
  aplicar sea repetible, y por eso el índice único de V18 no es opcional: es la
  pieza que sostiene esta decisión.
- Un evento que falla siempre se reintentará hasta que la pasarela se rinda. Es
  ruido en el log a cambio de no perder dinero en silencio; el intercambio es
  claro.

## Cómo se sabe que funciona

`PagosWebhookTest`, 8 casos, cubre el contrato del manejador: firma inválida,
cobro, reembolso, evento ya terminado, fallo pasajero (queda pendiente y pide
reintento), **la secuencia completa falla → reenvío → se aplica**, fallo
definitivo y el guardado del cuerpo crudo.

`CompraDeSlotServiceTest`, 22 casos, cubre lo que el manejador invoca: que no se
reserve nada sin cobrar, que el importe salga del pedido y no del mensaje, que un
webhook repetido no anote el cobro otra vez y que un reembolso repetido no vuelva
a restar.

### Lo que estos tests NO cubren

**La consulta del `ON CONFLICT`.** Es SQL, y el proyecto no tiene todavía ningún
test contra una base de datos real —ni Testcontainers ni CI—, así que la
condición `WHERE processed_at IS NULL` está razonada y revisada, no ejecutada.

Es deuda deliberada y se registra como tal:

- **Responsable:** ingeniería
- **Se paga:** al montar CI, con un test de integración sobre PostgreSQL que
  recorra los tres estados (pendiente, terminado, fallido)
- **Mientras tanto**, se comprueba a mano contra la base de datos de pruebas:

```sql
-- 1. Un evento pendiente se puede volver a coger
INSERT INTO payment_webhooks (id, gateway, event_id, event_type, payload)
VALUES (gen_random_uuid(), 'test', 'evt-x', 'payment.succeeded', '{}');

INSERT INTO payment_webhooks (id, gateway, event_id, event_type, payload)
VALUES (gen_random_uuid(), 'test', 'evt-x', 'payment.succeeded', '{}')
ON CONFLICT (gateway, event_id) DO UPDATE SET received_at = now(), error = NULL
WHERE payment_webhooks.processed_at IS NULL;
-- Tiene que decir: UPDATE 1

-- 2. Uno terminado, no
UPDATE payment_webhooks SET processed_at = now() WHERE event_id = 'evt-x';

INSERT INTO payment_webhooks (id, gateway, event_id, event_type, payload)
VALUES (gen_random_uuid(), 'test', 'evt-x', 'payment.succeeded', '{}')
ON CONFLICT (gateway, event_id) DO UPDATE SET received_at = now(), error = NULL
WHERE payment_webhooks.processed_at IS NULL;
-- Tiene que decir: UPDATE 0

DELETE FROM payment_webhooks WHERE gateway = 'test';
```
