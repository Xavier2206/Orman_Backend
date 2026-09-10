# Contratos, archivos y cuotas

Todas las rutas requieren Bearer JWT de un Usuario con `ROLE_PROPIETARIO`. El
backend confirma además que la Persona asociada al Usuario sea propietaria de
la Propiedad que contiene la Unidad del Contrato.

## Crear borrador

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

Las fechas deben ser el primer día de mes. Devuelve `201 Created`, estado
`BORRADOR` y `Location` hacia `/api/v1/contratos/{codcon}`.

## Contratos

- `GET {{baseUrl}}/api/v1/unidades/{{coduni}}/contratos?page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos?coduni={{coduni}}&estado=BORRADOR&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}`
- `PUT {{baseUrl}}/api/v1/contratos/{{codcon}}` con el mismo contrato JSON, solo mientras sea `BORRADOR`.
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/confirmar`
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/finalizar`

Confirmar cambia `BORRADOR` a `VIGENTE` y genera una cuota `PENDIENTE` por cada
mes del intervalo. Un Contrato del 01/09/2026 al 01/09/2027 genera 12 cuotas,
desde septiembre de 2026 hasta agosto de 2027.

## Renovación y rescisión

`POST {{baseUrl}}/api/v1/contratos/{{codcon}}/renovaciones`

```json
{
  "fechaInicio": "2027-09-01",
  "fechaFin": "2028-09-01",
  "montoMensual": 2700.00,
  "garantia": 2500.00
}
```

La renovación crea un nuevo `BORRADOR` asociado al Contrato origen.

`PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/rescindir`

```json
{
  "fechaRescision": "2027-01-01",
  "motivoRescision": "Finalización anticipada acordada."
}
```

La rescisión solo cambia el estado del Contrato y conserva las cuotas; no
procesa pagos ni comprobantes.

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

No se cargan archivos binarios: se persiste únicamente URL y metadatos.

## Errores esperados

- `400 VALIDATION_ERROR`: importes, URL, orden, estado o datos requeridos inválidos.
- `403 ACCESS_DENIED`: el recurso no pertenece al propietario autenticado.
- `404 RESOURCE_NOT_FOUND`: Unidad o Contrato inexistente.
- `409 CONFLICT`: Unidad con otro Contrato `VIGENTE`, orden de archivo duplicado o cuota duplicada.
- `422 BUSINESS_RULE_VIOLATION`: fechas no mensuales, estado no transicionable, Unidad inactiva o inquilino inactivo.
