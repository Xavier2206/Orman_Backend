# Contratos, archivos y cuotas

Todas las rutas requieren Bearer JWT de un Usuario con `ROLE_PROPIETARIO`. El
backend valida la titularidad de la Propiedad y que la Unidad sea operativa, la
Propiedad esté habilitada y la Persona inquilina esté activa.

## Registrar contrato

`POST {{baseUrl}}/api/v1/unidades/{{coduni}}/contratos`

```json
{
  "codperInquilino": 4,
  "fechaInicio": "2026-09-01",
  "fechaFin": "2027-09-01",
  "montoMensual": 2500.00,
  "garantia": 2500.00
}
```

Las fechas deben ser el primer día del mes y forman el intervalo
`[fechaInicio, fechaFin)`. El monto mensual debe ser mayor a cero y admitir
como máximo dos decimales. ORMAN usa exclusivamente `BOB`.

La respuesta es `201 Created`, incluye `Location` y crea las cuotas mensuales
en la misma transacción. Si `fechaInicio` es posterior a la fecha actual el
estado es `PROGRAMADO`; si ya llegó, es `VIGENTE`.

## Consultar y cambiar ciclo de vida

- `GET {{baseUrl}}/api/v1/unidades/{{coduni}}/contratos?page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos?coduni={{coduni}}&estado=PROGRAMADO&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}`
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/finalizar`
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/rescindir`

Estados permitidos: `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`.
Un scheduler activa de forma idempotente los contratos programados al llegar
su fecha de inicio.

No hay edición previa, confirmación ni endpoint especial de renovación. Una
continuación se registra como un contrato nuevo. Los contratos contiguos son
válidos; se rechaza cualquier solapamiento entre contratos `PROGRAMADO` o
`VIGENTE` de una misma Unidad.

## Finalizar

`PATCH .../finalizar` solo acepta un contrato `VIGENTE` cuando la fecha actual
es igual o posterior a `fechaFin`, no quedan cuotas `PENDIENTE` o `PARCIAL` y
no existe un pago `PENDIENTE_REVISION` asociado al contrato.

## Rescindir

```json
{
  "fechaRescision": "2026-09-01",
  "motivoRescision": "Terminación anticipada acordada."
}
```

La fecha representa un periodo mensual, debe ser el primer día del mes, estar
dentro del contrato y no ser futura. Todas las cuotas hasta ese periodo,
inclusive, deben estar `PAGADA`; tampoco puede quedar ningún pago
`PENDIENTE_REVISION`. Al aprobarse, todas las cuotas posteriores pasan a
`ANULADA`, permanecen en la base de datos y dejan de admitir pagos o
recordatorios.

## Archivos y cuotas

`POST {{baseUrl}}/api/v1/contratos/{{codcon}}/archivos`

```json
{
  "url": "https://example.test/contrato-firmado.pdf",
  "nombreArchivo": "contrato-firmado.pdf",
  "tipoContenido": "application/pdf",
  "orden": 0
}
```

- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}/archivos`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}/cuotas`

Cada cuota expone `monto`, `montoConfirmado`, `saldo`,
`montoPendienteRevision` y `estado`. No se cargan binarios: se persiste URL y
metadatos.

## Errores esperados

- `400 VALIDATION_ERROR`: importes, URL, orden, estado o datos requeridos inválidos.
- `403 ACCESS_DENIED`: el recurso no pertenece a la propietaria autenticada.
- `404 RESOURCE_NOT_FOUND`: Unidad o Contrato inexistente.
- `409 CONFLICT`: intervalo contractual solapado, orden o periodo duplicado.
- `422 BUSINESS_RULE_VIOLATION`: fechas no mensuales, transición inválida,
  Unidad no operativa, Propiedad no habilitada, inquilino inactivo o cuotas/pagos
  sin resolver.
