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
- `GET {{baseUrl}}/api/v1/contratos?codprop={{codprop}}&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos?codprop={{codprop}}&coduni={{coduni}}&estado=VIGENTE&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos?q=Juan&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos?q=Juan&codprop={{codprop}}&estado=VIGENTE&page=0&size=20`
- `GET {{baseUrl}}/api/v1/contratos/resumen`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}`
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/finalizar`
- `PATCH {{baseUrl}}/api/v1/contratos/{{codcon}}/rescindir`

Estados permitidos: `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`.
Un scheduler activa de forma idempotente los contratos programados al llegar
su fecha de inicio.

En `GET /api/v1/contratos`, `codprop` es opcional y filtra los contratos de
las Unidades pertenecientes a esa Propiedad. Puede combinarse con `coduni` y
`estado`; todos los filtros se aplican conjuntamente y la propietaria
autenticada solo puede consultar sus propias Propiedades.

`GET /api/v1/contratos/resumen` devuelve los conteos de `VIGENTE`,
`PROGRAMADO`, `FINALIZADO` y `RESCINDIDO` de las Propiedades de la propietaria
autenticada. Se utiliza para las tarjetas resumen del frontend y no recibe
filtros de paginación.

El parámetro opcional `q` busca por nombre, apellido paterno o apellido
materno del inquilino. La coincidencia es parcial y no distingue mayúsculas de
minúsculas; por ejemplo, `q=Juan`, `q=Pérez`, `q=Gómez` o `q=Pe`.

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

Enviar como `multipart/form-data`, con las partes:

| Clave | Tipo | Valor |
|---|---|---|
| `archivo` | File | Documento `.pdf` |
| `orden` | Text | Entero único no negativo dentro del contrato |

El backend acepta únicamente PDF válido: extensión `.pdf`, MIME compatible
con `application/pdf`, firma PDF y contenido parseable. El máximo recibido es
20 MiB por defecto. La configuración `storage.contratos.max-file-size-bytes`
se define mediante `CONTRACT_DOCUMENT_MAX_FILE_SIZE_BYTES`; el límite multipart
global también debe permitir ese tamaño (`ORMAN_MULTIPART_MAX_FILE_SIZE` y
`ORMAN_MULTIPART_MAX_REQUEST_SIZE`). El objetivo de optimización, configurable
con `CONTRACT_DOCUMENT_TARGET_SIZE_BYTES`, es 10 MiB por defecto y no supone
rechazar documentos que no puedan reducirse hasta ese tamaño.

La respuesta incluye metadatos, por ejemplo:

```json
{
  "codarc": 17,
  "codcon": 42,
  "nombreArchivo": "contrato-firmado.pdf",
  "tipoContenido": "application/pdf",
  "tamanoOriginal": 2450000,
  "tamanoFinal": 2380000,
  "fechaSubida": "2026-09-16T12:00:00",
  "subidoPor": "propietaria",
  "orden": 0,
  "almacenadoInternamente": true
}
```

- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}/archivos`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}/archivos/{{codarc}}/download`
- `DELETE {{baseUrl}}/api/v1/contratos/{{codcon}}/archivos/{{codarc}}`
- `GET {{baseUrl}}/api/v1/contratos/{{codcon}}/cuotas`

La descarga responde `application/pdf` como adjunto y es privada; requiere que
el archivo pertenezca al contrato indicado y que la propietaria autenticada
sea dueña del contrato. La eliminación responde `204 No Content` y elimina
metadato y archivo físico después de confirmar la transacción. El listado solo
devuelve metadatos; no devuelve bytes, rutas internas ni URL pública. Los
registros históricos solo con URL pueden aparecer con
`almacenadoInternamente: false` y no tienen descarga interna.

Los PDFs se guardan en `storage/contratos/{codcon}/` por defecto, en el
almacenamiento local privado configurado por `CONTRACT_DOCUMENT_STORAGE_ROOT`.
Los PDFs que contienen diccionarios de firma no se reescriben; para los demás,
la optimización sin pérdida es de mejor esfuerzo y conserva el texto extraído.

Cada cuota expone `monto`, `montoConfirmado`, `saldo`,
`montoPendienteRevision` y `estado`. El endpoint paginado `GET /api/v1/contratos`
no incluye ni carga documentos.

## Errores esperados

- `400 VALIDATION_ERROR`: importes, PDF inválido, tamaño o datos requeridos inválidos.
- `403 ACCESS_DENIED`: el recurso no pertenece a la propietaria autenticada.
- `404 RESOURCE_NOT_FOUND`: Unidad o Contrato inexistente.
- `409 CONFLICT`: intervalo contractual solapado, orden o periodo duplicado.
- `422 BUSINESS_RULE_VIOLATION`: fechas no mensuales, transición inválida,
  Unidad no operativa, Propiedad no habilitada, inquilino inactivo o cuotas/pagos
  sin resolver.
