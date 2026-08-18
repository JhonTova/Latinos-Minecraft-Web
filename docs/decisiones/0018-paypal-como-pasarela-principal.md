# ADR-0018 — PayPal como pasarela principal, Stripe como alternativa

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-11
- **Decide:** el propietario del producto
- **Depende de:** [ADR-0004 — Pagos sin tocar tarjetas](0004-pagos-sin-tarjetas.md)

---

## Contexto

La primera integración fue Stripe. Funciona —10 tests de intento de falsificación
de webhook, todos rechazados— pero para cobrar de verdad Stripe pide activar la
cuenta: identidad, datos fiscales, cuenta bancaria y, según el país, datos de la
empresa. Con la plataforma todavía sin un solo cliente, eso es un muro antes del
primer euro.

PayPal, para empezar, pide una cuenta y dos claves.

Hay además un argumento de producto: en el mercado hispanohablante mucha gente ya
tiene cuenta de PayPal y no le apetece meter la tarjeta en un sitio que acaba de
descubrir. Una pasarela conocida convierte mejor que una que el comprador ve por
primera vez.

## Decisión

### 1. PayPal es la pasarela por defecto

`PasarelaPayPal` implementa el mismo puerto `PaymentGateway` que ya existía. No
cambia nada del dominio: ni el ledger, ni la cola FIFO de slots, ni la
idempotencia. Es lo que ADR-0004 pretendía al declarar la pasarela como puerto
desde el primer día, y aquí se cobra ese seguro.

### 2. Stripe se queda

No se borra nada. Cada pasarela se activa por configuración:

| Configurado | Pasarela activa |
|---|---|
| `PAYPAL_CLIENT_ID` | PayPal |
| solo `STRIPE_SECRET_KEY` | Stripe |
| ninguno | cobro manual |

Si están las dos, gana PayPal (`@Primary`). Que sea determinista importa más que
cuál gane: la alternativa es que la aplicación no arranque por ambigüedad de
beans, en producción, un domingo.

### 3. "Aprobado" no es "pagado"

Es la diferencia de fondo entre las dos pasarelas, y la fuente del error de
dinero más fácil de cometer con PayPal.

Stripe cobra al completarse el pago y avisa. PayPal **aprueba** primero —el
comprador dice que sí— y el dinero no se mueve hasta que la plataforma llama a
`/capture`. Si no se llama, PayPal anula la orden a los tres días.

Por eso:

- El evento `CHECKOUT.ORDER.APPROVED` **se ignora**. Solo
  `PAYMENT.CAPTURE.COMPLETED` cuenta como cobro.
- Existe `PaymentGateway.capturarAlVolver(...)`, que por defecto no hace nada
  —correcto para las pasarelas que cobran solas— y PayPal sobrescribe.

Tratar "aprobado" como "pagado" regalaría el espacio destacado a cualquiera que
aprobase el pago y cerrase el navegador. Hay un test que lo fija:
`aprobadoNoEsPagado`.

### 4. La verificación de firma es una llamada a PayPal

PayPal no firma con HMAC como Stripe: hay que preguntarle a PayPal si la firma es
suya, enviándole las cinco cabeceras que acompañan al webhook.

Es más lento —una llamada de red por webhook— y menos elegante. Se acepta: aquí
la corrección vale más que la elegancia, y sin `webhook-id` configurado no se
acepta ningún webhook. Perder una confirmación se arregla; aceptar un cobro falso
no.

### 5. La captura se puede pedir sin sesión

`POST /api/v1/pagos/confirmar` no exige sesión. El comprador puede volver desde
la app móvil de PayPal sin cookie, y perder el cobro por eso sería peor que el
riesgo de dejarlo abierto.

Lo que hace seguro dejarlo abierto no es la autenticación:

- La referencia es un identificador opaco que emite PayPal.
- El pedido y el importe salen de **la respuesta de PayPal**, nunca de lo que
  manda el cliente.
- Capturar dos veces la misma orden no duplica nada: el registro de idempotencia
  de webhooks cubre los dos caminos.
- La referencia se valida contra `^[A-Za-z0-9-]{5,64}$` antes de concatenarla en
  la URL de la API de PayPal.

Con una referencia inventada, lo único que se consigue es un 200 con
`"pendiente"`.

## Consecuencias

**A favor**

- Se puede cobrar de verdad sin verificación de empresa por adelantado.
- Mejor conversión esperada en el mercado objetivo.
- El puerto queda probado con dos implementaciones reales, que es la única
  manera de saber que una abstracción sirve.

**En contra**

- Un paso más en el flujo (la captura) y, por tanto, un estado más en el que las
  cosas pueden quedarse a medias. Lo cubre el webhook.
- Verificar cada webhook cuesta una llamada de red a PayPal.
- Las comisiones de PayPal son algo más altas que las de Stripe. A este volumen,
  irrelevante frente a poder empezar.

## Cómo se sabe que funciona

- `PasarelaPayPalTest` — 9 casos, ninguno toca la red. Que no la toquen es parte
  de lo que comprueban: si la verificación de firma se saltara, intentarían salir
  a internet.
- `PasarelaStripeTest` — sigue verde; el cambio de puerto no rompió nada.
- 91 tests de backend en verde tras el cambio.
