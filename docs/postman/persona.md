# API Persona — Guía de pruebas en Postman

Esta guía se basa exclusivamente en la API implementada. Use datos ficticios para las pruebas.

## 1. Requisitos previos

- PostgreSQL activo.
- Variables `DB_*` configuradas.
- Backend ejecutándose en el puerto `9090`.
- `baseUrl`: `http://localhost:9090`.

No incluya credenciales en Postman ni en esta guía.

## 2. URL base

`http://localhost:9090/api/v1/personas`

## 3. Encabezados

Para solicitudes con JSON (`POST` y `PUT`):

```text
Content-Type: application/json
Accept: application/json
```

Activar, desactivar, consultar y eliminar no requieren body.

## 4. Crear Persona

`POST /api/v1/personas`

```json
{
  "ci": "TEST-CI-POST-001",
  "nombre": "Persona Ficticia",
  "ap": "Apellido Uno",
  "am": "Apellido Dos",
  "genero": "F",
  "estado": "1",
  "correo": "persona.ficticia@example.test",
  "telefono": "70000001",
  "tipoPersona": "I",
  "foto": "referencia-foto-test"
}
```

Responde `201 Created` con `PersonaResponse` y el encabezado `Location`, por ejemplo:

```text
Location: http://localhost:9090/api/v1/personas/1
```

`estado` es opcional al crear: si se omite, PostgreSQL aplica su valor predeterminado `1`.

## 5. Consultar por identificador

`GET /api/v1/personas/{codper}`

Ejemplo: `GET {{baseUrl}}/api/v1/personas/{{personaId}}`.

- `200 OK`: devuelve `PersonaResponse`.
- `404 Not Found`: `errorCode` es `RESOURCE_NOT_FOUND`.

## 6. Listar paginado

`GET /api/v1/personas?page=0&size=20`

El valor por defecto es `page=0`, `size=20`, ordenado por `codper` ascendente. Un tamaño solicitado mayor a 100 se limita a 100. La estructura real de `PageResponse` es:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Cada elemento de `content` es un `PersonaResponse`.

## 7. Actualizar Persona

`PUT /api/v1/personas/{codper}`

```json
{
  "ci": "TEST-CI-PUT-001",
  "nombre": "Persona Ficticia Actualizada",
  "ap": "Apellido Nuevo",
  "am": null,
  "genero": "F",
  "estado": "1",
  "correo": "actualizada@example.test",
  "telefono": "70000003",
  "tipoPersona": "I",
  "foto": null
}
```

- `200 OK`: devuelve la Persona actualizada.
- `404 Not Found`: no existe el identificador.
- `409 Conflict`: el CI ya pertenece a otra Persona.

El `PUT` puede modificar `estado` como parte de una actualización completa. Para cambiar únicamente el estado, las operaciones recomendadas son activar y desactivar.

## 8. Desactivar Persona

`PATCH /api/v1/personas/{codper}/desactivar`

Ejemplo: `PATCH {{baseUrl}}/api/v1/personas/{{personaId}}/desactivar`.

No necesita body. Conserva el registro, cambia `estado` a `0`, responde `200 OK` y devuelve `PersonaResponse` con `estado: 0`. Si no existe, responde `404 RESOURCE_NOT_FOUND`. Es idempotente: una Persona ya inactiva sigue respondiendo exitosamente con estado `0`.

## 9. Activar Persona

`PATCH /api/v1/personas/{codper}/activar`

Ejemplo: `PATCH {{baseUrl}}/api/v1/personas/{{personaId}}/activar`.

No necesita body. Conserva el registro, cambia `estado` a `1`, responde `200 OK` y devuelve `PersonaResponse` con `estado: 1`. Si no existe, responde `404 RESOURCE_NOT_FOUND`. Es idempotente: una Persona ya activa sigue respondiendo exitosamente con estado `1`.

## 10. Eliminación física

`DELETE /api/v1/personas/{codper}`

Elimina definitivamente el registro; no cambia `estado` a `0`. Responde `204 No Content` sin body y `404 RESOURCE_NOT_FOUND` si la Persona no existe. Una Persona eliminada físicamente no puede reactivarse mediante la API.

## 11. Diferencias entre operaciones

| Operación | Efecto | Conserva registro | Estado final | Respuesta |
|---|---|---|---|---|
| Desactivar | Actualiza estado | Sí | 0 | 200 |
| Activar | Actualiza estado | Sí | 1 | 200 |
| Eliminar | Borra físicamente | No | No aplica | 204 |

## 12. Validaciones

Use un `POST` o `PUT` válido como base y cambie un campo por solicitud.

| Caso | Ejemplo | Resultado |
|---|---|---|
| CI vacío | `"ci": ""` | 400 `VALIDATION_ERROR` |
| Nombre vacío | `"nombre": ""` | 400 `VALIDATION_ERROR` |
| Género inválido | `"genero": "X"` | 400 `VALIDATION_ERROR` |
| Estado inválido | `"estado": "2"` | 400 `VALIDATION_ERROR` |
| Correo inválido | `"correo": "no-es-correo"` | 400 `VALIDATION_ERROR` |
| Teléfono vacío | `"telefono": ""` | 400 `VALIDATION_ERROR` |
| TipoPersona inválido | `"tipoPersona": "X"` | 400 `VALIDATION_ERROR` |
| JSON mal formado | body `{` | 400 `INVALID_REQUEST` |

Los únicos valores de estado aceptados en los contratos JSON son `"0"` y `"1"`; género admite `"M"` o `"F"`, y tipo de persona `"A"` o `"I"`.

## 13. CI duplicado

La creación y la actualización responden `409 Conflict` con `errorCode=CONFLICT` si el CI ya pertenece a otra Persona. Para comprobarlo, cree dos Personas ficticias con CI distintos e intente crear una repetida o actualizar la segunda con el CI de la primera.

## 14. ProblemDetail

Los errores usan `application/problem+json` y contienen:

- `status`
- `title`
- `detail`
- `instance`
- `errorCode`
- `timestamp`
- `traceId`
- `fieldErrors` cuando corresponde a validaciones; cada elemento contiene `field` y `message`.

Persona inexistente devuelve `404 RESOURCE_NOT_FOUND`. Los errores inesperados devuelven `500 INTERNAL_ERROR` sin exponer el detalle interno.

## 15. Orden recomendado de pruebas

1. Crear Persona.
2. Consultarla.
3. Listar.
4. Actualizar.
5. Desactivar.
6. Consultar y comprobar estado 0.
7. Activar.
8. Consultar y comprobar estado 1.
9. Probar validaciones y CI duplicado.
10. Eliminar físicamente.
11. Consultar nuevamente y comprobar 404.

## 16. Variables de Postman

```text
baseUrl = http://localhost:9090
personaId = 1
```

No incluya variables de PostgreSQL.

## 17. Resumen de endpoints

- `POST /api/v1/personas`
- `GET /api/v1/personas`
- `GET /api/v1/personas/{codper}`
- `PUT /api/v1/personas/{codper}`
- `PATCH /api/v1/personas/{codper}/desactivar`
- `PATCH /api/v1/personas/{codper}/activar`
- `DELETE /api/v1/personas/{codper}`

## 18. Funcionalidades no implementadas

Todavía no existe autenticación, JWT, autorización, carga física de fotos, restauración de registros eliminados físicamente ni filtros avanzados.
