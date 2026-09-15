# ETAPA 4.3 — Pagos, comprobantes, recibos y cuentas de pago

## Estado

`COMPLETADA`, corregida el 2026-09-15 conforme a los dos actores de pago de
ORMAN.

## Diseño vigente

- `origenRegistro` distingue `PROPIETARIA` de `INQUILINO`; no representa el
  dispositivo usado.
- Todo pago conserva el Usuario que lo registró o presentó y, al resolverse,
  el Usuario que lo confirmó, rechazó o anuló.
- La propietaria registra efectivo, QR o transferencia ya verificados. El
  pago entra directamente `CONFIRMADO`, actualiza la cuota y crea un recibo.
- El inquilino solo presenta QR o transferencia de una cuota propia, con
  cuenta activa y comprobante obligatorio. Entra `PENDIENTE_REVISION`.
- Confirmar un pago presentado bloquea la cuota, recalcula únicamente pagos
  confirmados, impide sobrepago, actualiza `PARCIAL`/`PAGADA` y crea un recibo.
- Rechazar exige motivo y no modifica la cuota. Anular solo cancela un registro
  pendiente; no revierte confirmados.
- Los pagos pendientes no reservan saldo. Una cuota expone monto total,
  confirmado, saldo y monto pendiente de revisión.
- Una cuota `ANULADA` no acepta pagos.

## Recibos

El recibo es una constancia interna, no fiscal. Existe exactamente uno por
cada pago confirmado, incluidos pagos parciales, y expone de forma explícita
el pago, cuota, periodo, monto, moneda, método y fechas.

## API vigente

| Recurso | Rutas |
|---|---|
| Pagos | `POST /api/v1/cuotas/{codcuo}/pagos` para propietaria o inquilino; consultas y `PATCH .../confirmar`, `.../rechazar`, `.../anular` solo para propietaria |
| Comprobantes | `POST/GET /api/v1/pagos/{codpag}/comprobantes`; `DELETE .../{id}` |
| Recibos | `GET /api/v1/pagos/{codpag}/recibo`; `GET /api/v1/recibos/{codrec}` |
| CuentasPago | `POST/GET /api/v1/cuentas-pago`; `GET/PUT .../{codcta}`; `PATCH .../activar`, `.../desactivar` |

## Persistencia y validación

V17 renombra el origen, transforma pagos históricos a `PROPIETARIA`, incorpora
actores con FKs a `usuarios` y agrega checks e índices. La lógica financiera se
centraliza en una sola operación transaccional para registro directo y
confirmación posterior. La suite global ejecutó 367 pruebas, sin fallos,
errores ni omisiones, contra PostgreSQL real.
