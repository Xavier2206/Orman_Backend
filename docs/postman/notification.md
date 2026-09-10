# Notificaciones

Todas las rutas requieren `Authorization: Bearer {{accessToken}}` de un Usuario
activo con `ROLE_PROPIETARIO`. Los recursos pertenecen exclusivamente al usuario
autenticado y no se aceptan cuerpos para crear notificaciones.

## Listar notificaciones

```http
GET {{baseUrl}}/api/v1/notificaciones?tipo=CUOTA_VENCIDA&leida=false&page=0&size=20
Authorization: Bearer {{accessToken}}
```

`tipo` es opcional y admite `CUOTA_PROXIMA_VENCER`, `CUOTA_VENCIDA`,
`COMPROBANTE_RECIBIDO`, `PAGO_CONFIRMADO` y `PAGO_RECHAZADO`. `leida` admite
`true` o `false`.

Respuesta `200 OK`:

```json
{
  "content": [
    {
      "codnot": 18,
      "tipo": "CUOTA_VENCIDA",
      "titulo": "Cuota vencida",
      "mensaje": "La cuota de alquiler del Departamento 2 se encuentra vencida.",
      "referenciaTipo": "CUOTA",
      "referenciaId": 31,
      "fechaCreacion": "2026-09-10T12:00:00",
      "leida": false,
      "fechaLectura": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## Consultar detalle

```http
GET {{baseUrl}}/api/v1/notificaciones/18
Authorization: Bearer {{accessToken}}
```

Responde `200 OK`; una notificación de otro usuario responde `404` con
`application/problem+json` y `RESOURCE_NOT_FOUND`.

## Resumen de no leídas

```http
GET {{baseUrl}}/api/v1/notificaciones/resumen
Authorization: Bearer {{accessToken}}
```

Respuesta `200 OK`:

```json
{
  "noLeidas": 3
}
```

## Marcar como leída

```http
PATCH {{baseUrl}}/api/v1/notificaciones/18/leer
Authorization: Bearer {{accessToken}}
```

Responde `200 OK` con la notificación. Repetir la operación conserva la primera
`fechaLectura` y responde el mismo recurso actualizado.

## Enviar recordatorio manual

```http
POST {{baseUrl}}/api/v1/cuotas/31/notificar
Authorization: Bearer {{accessToken}}
```

Solo admite cuotas propias `PENDIENTE` o `PARCIAL`, incluidas las vencidas.
Cuotas `PAGADA` o `ANULADA` responden `422 BUSINESS_RULE_VIOLATION`. La acción
es idempotente por destinatario, tipo y referencia; si ya existe el recordatorio
lógico devuelve la notificación persistida.
