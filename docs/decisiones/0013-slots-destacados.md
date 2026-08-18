# ADR-0013 — Venta de slots destacados a precio fijo (sustituye a las subastas)

- **Estado:** ✅ Vigente
- **Fecha:** 2026-08-03
- **Decide:** el propietario del producto, a propuesta propia
- **Reemplaza:** el sistema de subastas de los documentos 02, 12, 15 y 17

---

## Contexto

Los requisitos originales planteaban un motor de subastas para vender
posiciones destacadas: pujas en tiempo real, incrementos mínimos, calendario,
empates, sanciones por impago y reservas.

Dos problemas independientes:

1. **Complejidad técnica desproporcionada.** Un motor de subastas exige
   bloqueo pesimista, resolución de condiciones de carrera, protección contra
   sniping, detección de colusión, gestión de pujas perdedoras y reembolsos.
   Es un producto en sí mismo.
2. **Tensión con el activo principal.** El producto real es la credibilidad
   del ranking. Vender posiciones dentro del ranking orgánico lo destruye:
   deja de correlacionar con la calidad, los jugadores dejan de confiar, cae
   el tráfico y las posiciones pierden su valor. El ingreso se consume a sí
   mismo.

## Decisión

### 1. Venta a precio fijo, no subasta

Se venden **5 slots destacados** con precio configurable por slot y por
periodo. Sin pujas.

| | Subasta (descartada) | **Precio fijo (adoptada)** |
|---|---|---|
| Motor de pujas y bloqueos | Necesario | ❌ No existe |
| Condiciones de carrera | Críticas | ❌ No existen |
| Sniping y colusión | A mitigar | ❌ No aplican |
| Reembolso de perdedores | Necesario | ❌ No hay perdedores |
| Comprensión del cliente | Requiere explicación | ✅ Inmediata |

### 2. Separación estricta de lo orgánico

Los 5 slots **no son las posiciones 1 a 5 del ranking**. Son un bloque
independiente, situado por encima del ranking, con tratamiento visual distinto
y etiquetado explícito como patrocinado.

```
┌─────────────────────────────────────┐
│  ⭐ DESTACADOS  (patrocinado)        │   ← 5 slots vendidos
│  slot 1 · slot 2 · slot 3 · 4 · 5   │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│  RANKING                            │   ← orgánico, intacto
│  #1  #2  #3  #4  #5  …              │      empieza en #1
└─────────────────────────────────────┘
```

El ranking orgánico atrae al jugador; el bloque patrocinado se vende al dueño
del servidor. **Ambos conservan su valor solo si están separados.**

### 3. Rotación semanal con cola FIFO

- Cada slot se ocupa por **periodos de 1 semana**.
- Si el slot está libre, la compra se activa en la siguiente rotación.
- Si está ocupado, el comprador entra en **cola por orden de compra (FIFO)**
  para ese slot.
- En el momento de la compra se muestran **fechas exactas**:
  *"Tu servidor estará en el Slot 3 del 10 al 17 de agosto."*
  Nunca un "próximamente" sin fecha.

### 4. Límites de la cola

- **Máximo 4 periodos vendidos por adelantado** por slot.
- Alcanzado el tope, el slot aparece como *agotado hasta [fecha]*.

Motivo: vender 20 semanas por adelantado equivale a retener dinero por un
servicio lejano, con el mismo riesgo de reembolsos y pasivo diferido que llevó
a descartar el wallet.

### 5. Cobro

El cobro se realiza **en el momento de la compra**, contra una fecha de
entrega comprometida y visible. No se acumula saldo en la plataforma
(coherente con ADR-0004 y con el descarte del wallet).

Si por causa de la plataforma un periodo comprado no se sirve, se emite un
**asiento compensatorio** en el ledger; nunca se modifica el asiento original.

## Consecuencias

**Positivas**

- Desaparece por completo el módulo de subastas del alcance
- El ranking conserva su credibilidad
- El cliente entiende qué compra y cuándo lo recibe
- Ingreso predecible y fácil de proyectar

**Negativas aceptadas**

- Se renuncia al ingreso extra que generaría una puja competitiva
- Fijar el precio correcto es responsabilidad de la plataforma; con subasta lo
  fijaba el mercado. Mitigación: precio configurable por slot, revisable según
  la longitud de la cola (si siempre está llena, el precio es bajo)

**Requisitos derivados**

- La cola de cada slot es estado persistente y auditable
- El calendario de ocupación debe ser consultable por el cliente
- Toda compra genera asiento en el ledger inmutable
- El bloque de destacados y el ranking son consultas separadas: ninguna
  posición patrocinada puede filtrarse al ranking orgánico

## Alternativas descartadas

| Alternativa | Motivo |
|---|---|
| Subasta en tiempo real | Complejidad de producto completo; ver contexto |
| Posiciones compradas dentro del ranking orgánico | Destruye el activo principal |
| Puja por sobre cerrado | Sigue exigiendo motor de pujas y reembolsos |
| Modelo por impresiones/CPC | Requiere volumen de tráfico que no existe aún |
