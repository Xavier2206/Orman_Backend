# Pagos, comprobantes, recibos y cuentas de pago

ORMAN no procesa dinero: registra pagos ocurridos externamente. El origen se
deduce del Usuario autenticado y se devuelve como `origenRegistro`:
`PROPIETARIA` o `INQUILINO`.

## Cuentas de pago

Las cuentas siguen siendo administradas exclusivamente por
`ROLE_PROPIETARIO`:

- `POST/GET {{baseUrl}}/api/v1/cuentas-pago`
- `GET/PUT {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}`
- `PATCH {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}/activar`
- `PATCH {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}/desactivar`

No existe `DELETE`; una cuenta puede quedar referenciada por pagos históricos.

## Registro por la propietaria

`POST {{baseUrl}}/api/v1/cuotas/{{codcuo}}/pagos`

Efectivo recibido y verificado:

```json
{
  "monto": 500.00,
  "metodo": "EFECTIVO",
  "referenciaExterna": "Pago parcial de septiembre",
  "fechaPago": "2026-09-10T10:00:00",
  "idempotencyKey": "11111111-1111-1111-1111-111111111111"
}
```

QR o transferencia verificada por la propietaria:

```json
{
  "monto": 1000.00,
  "metodo": "QR",
  "codcta": 1,
  "referenciaExterna": "TRX-1001",
  "fechaPago": "2026-09-10T10:30:00",
  "idempotencyKey": "22222222-2222-2222-2222-222222222222"
}
```

Estos pagos se crean directamente `CONFIRMADO`. Se bloquea y recalcula la
cuota, se impide el sobrepago y se genera exactamente un recibo. En QR y
transferencia la cuenta activa es obligatoria; el comprobante es opcional.

## Presentación por el inquilino

La misma ruta `POST` admite `ROLE_INQUILINO` únicamente sobre cuotas de su
propio contrato. Solo permite `QR` o `TRANSFERENCIA` y exige comprobante en el
mismo request:

```json
{
  "monto": 1000.00,
  "metodo": "TRANSFERENCIA",
  "codcta": 1,
  "referenciaExterna": "TRX-2002",
  "fechaPago": "2026-09-10T11:00:00",
  "idempotencyKey": "33333333-3333-3333-3333-333333333333",
  "comprobante": {
    "url": "https://example.test/comprobante.pdf",
    "nombreArchivo": "comprobante.pdf",
    "tipoContenido": "application/pdf",
    "orden": 0
  }
}
```

El resultado siempre es `PENDIENTE_REVISION`. Un inquilino no puede presentar
`EFECTIVO` ni confirmar, rechazar o anular pagos.

## Revisión por la propietaria

- `GET {{baseUrl}}/api/v1/cuotas/{{codcuo}}/pagos`
- `GET {{baseUrl}}/api/v1/pagos?estado=PENDIENTE_REVISION&metodo=QR&page=0&size=20`
- `GET {{baseUrl}}/api/v1/pagos/{{codpag}}`
- `PATCH {{baseUrl}}/api/v1/pagos/{{codpag}}/confirmar`
- `PATCH {{baseUrl}}/api/v1/pagos/{{codpag}}/rechazar`
- `PATCH {{baseUrl}}/api/v1/pagos/{{codpag}}/anular`

Para rechazar o anular:

```json
{ "motivo": "Descripción de la revisión." }
```

Confirmar actualiza pago y cuota y crea un recibo dentro de una sola
transacción. Rechazar y anular no modifican el saldo ni generan recibo.
`ANULADO` solo aplica a registros pendientes; no revierte confirmados.

La respuesta identifica `registradoPor` y, una vez resuelto, `revisadoPor`.
Los pagos parciales permanecen soportados y cada pago confirmado tiene su
propio recibo interno, no fiscal.

## Comprobantes y recibos

- `POST/GET {{baseUrl}}/api/v1/pagos/{{codpag}}/comprobantes`
- `DELETE {{baseUrl}}/api/v1/pagos/{{codpag}}/comprobantes/{{id}}`
- `GET {{baseUrl}}/api/v1/pagos/{{codpag}}/recibo`
- `GET {{baseUrl}}/api/v1/recibos/{{codrec}}`

Un recibo expone el pago, la cuota, el periodo, monto, moneda `BOB`, método y
fechas. Solo existe para un pago `CONFIRMADO`.

## Errores esperados

- `400 VALIDATION_ERROR`: monto, método, UUID o comprobante inválido.
- `403 ACCESS_DENIED`: cuota, pago o cuenta fuera del alcance del actor.
- `404 RESOURCE_NOT_FOUND`: recurso inexistente.
- `409 CONFLICT`: idempotencia u orden duplicado.
- `422 BUSINESS_RULE_VIOLATION`: sobrepago, cuota pagada/anulada, cuenta
  inactiva, transición inválida, comprobante ausente o efectivo presentado por
  inquilino.
