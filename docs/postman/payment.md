# Pagos, comprobantes, recibos y cuentas de pago

Todas las rutas requieren Bearer JWT de un Usuario con `ROLE_PROPIETARIO`. El
backend verifica además la propiedad de la cadena inmobiliaria; las cuentas
solo pueden pertenecer a la Persona propietaria autenticada.

## Cuentas de pago

`POST {{baseUrl}}/api/v1/cuentas-pago`

```json
{
  "banco": "Banco de prueba",
  "numeroCuenta": "1001001",
  "titular": "Propietaria ORMAN",
  "qrUrl": "https://example.test/qr.png",
  "instrucciones": "Indique el número de unidad en la referencia.",
  "orden": 0,
  "estado": "1"
}
```

- `GET {{baseUrl}}/api/v1/cuentas-pago?estado=1&page=0&size=20`
- `GET {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}`
- `PUT {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}` con el mismo JSON.
- `PATCH {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}/activar`
- `PATCH {{baseUrl}}/api/v1/cuentas-pago/{{codcta}}/desactivar`

No existe `DELETE`: una cuenta puede tener pagos históricos.

## Registrar y confirmar pagos

`POST {{baseUrl}}/api/v1/cuotas/{{codcuo}}/pagos`

Pago en efectivo:

```json
{
  "monto": 1000.00,
  "metodo": "EFECTIVO",
  "fechaPago": "2026-09-10T10:00:00",
  "idempotencyKey": "11111111-1111-1111-1111-111111111111"
}
```

Pago por transferencia o QR:

```json
{
  "monto": 500.00,
  "metodo": "TRANSFERENCIA",
  "codcta": 1,
  "referenciaExterna": "TRX-1001",
  "fechaPago": "2026-09-10T10:30:00",
  "idempotencyKey": "22222222-2222-2222-2222-222222222222"
}
```

El registro crea `PENDIENTE_REVISION`; no se envían estado, origen ni fecha de
registro. Transferencia y QR requieren comprobante antes de confirmar.

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

La confirmación no admite sobrepagos: actualiza automáticamente el estado de
la cuota a `PARCIAL` o `PAGADA` según la suma de los pagos confirmados y genera
su recibo.

## Comprobantes y recibos

`POST {{baseUrl}}/api/v1/pagos/{{codpag}}/comprobantes`

```json
{
  "url": "https://example.test/comprobante.pdf",
  "nombreArchivo": "comprobante.pdf",
  "tipoContenido": "application/pdf",
  "orden": 0
}
```

- `GET {{baseUrl}}/api/v1/pagos/{{codpag}}/comprobantes`
- `DELETE {{baseUrl}}/api/v1/pagos/{{codpag}}/comprobantes/{{id}}`
- `GET {{baseUrl}}/api/v1/pagos/{{codpag}}/recibo`
- `GET {{baseUrl}}/api/v1/recibos/{{codrec}}`

No se cargan binarios: se guarda URL y metadatos. No existe endpoint para crear
recibos porque se emiten solamente al confirmar el pago.

## Errores esperados

- `400 VALIDATION_ERROR`: importe, estado, método, URL, UUID o campos
  obligatorios inválidos.
- `403 ACCESS_DENIED`: el recurso o cuenta pertenece a otra propietaria.
- `404 RESOURCE_NOT_FOUND`: Cuota, Pago, CuentaPago, comprobante o Recibo
  inexistente.
- `409 CONFLICT`: clave de idempotencia, cuenta u orden de comprobante
  duplicados.
- `422 BUSINESS_RULE_VIOLATION`: sobrepago, pago fuera de estado pendiente,
  cuenta inactiva o falta de comprobante para transferencia/QR.
