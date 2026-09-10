# API Persona — Guía de pruebas en Postman

Esta guía se basa exclusivamente en la API implementada. Use datos ficticios para las pruebas.

## Actualización contractual Angular (2026-08-14)

`GET /api/v1/personas` admite filtros de PostgreSQL: `q`, `tipoPersona` (`A`/`I`), `estado` (`1`/`0`), `page`, `size` y `sort` (por ejemplo, `sort=ap,asc`). `q` busca parcialmente y sin distinción de mayúsculas en nombre, ap, am y CI; vacío equivale a ausencia. Los filtros inválidos devuelven `400 VALIDATION_ERROR`.

Cada Persona incluye `usuario` (`null` o `{ "login", "estado" }`) y `acciones` con `puedeEditar`, `puedeDesactivar`, `puedeActivar`, `puedeEliminar`, `puedeCrearUsuario` y `puedeCambiarPassword`. Son ayudas visuales: Backend sigue autorizando. No se devuelven password, hashes, roles, tokens ni sesiones.

Foto: `PUT /api/v1/personas/{codper}/foto` (multipart, parte `foto`), `GET` y `DELETE` en la misma ruta. Solo JPEG/PNG reales hasta 2 MiB; ImageIO los decodifica y normaliza a JPEG optimizado (calidad 0.85), con un máximo de 320 px por lado y sin ampliar imágenes pequeñas. Se conserva la proporción; PNG transparente se compone sobre blanco para permitir JPEG. La referencia es `personas/{codper}/{uuid}.jpg`, bajo `PERSONA_PHOTO_STORAGE_ROOT` (por defecto `./storage`), sin Base64/BLOB. Solo existe una foto vigente: se guarda y confirma la nueva antes de eliminar la anterior. Referencias inválidas o traversal se rechazan. Sin foto: `404 RESOURCE_NOT_FOUND`.

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

## Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/personas` | [Crear Persona](#5-crear-persona) | PROPIETARIO o ADMINISTRADOR | Sí | 201 | 400, 401, 403, 409 |
| GET | `/api/v1/personas` | [Listar Personas](#7-listar-paginado) | PROPIETARIO o ADMINISTRADOR; `page,size,sort` | No | 200 | 401, 403 |
| GET | `/api/v1/personas/resumen` | [Resumen global de Personas](#71-resumen-global-de-personas) | PROPIETARIO o ADMINISTRADOR | No | 200 | 401, 403 |
| GET | `/api/v1/personas/{codper}` | [Consultar Persona](#6-consultar-por-identificador) | PROPIETARIO o ADMINISTRADOR sobre Persona común | No | 200 | 401, 403, 404 |
| PUT | `/api/v1/personas/{codper}` | [Actualizar Persona](#8-actualizar-persona) | PROPIETARIO o ADMINISTRADOR sobre Persona común | Sí | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/personas/{codper}/desactivar` | [Desactivar Persona](#9-desactivar-persona) | PROPIETARIO o ADMINISTRADOR sobre Persona común; protege último propietario | No | 200 | 401, 403, 404, 409 |
| PATCH | `/api/v1/personas/{codper}/activar` | [Activar Persona](#10-activar-persona) | PROPIETARIO o ADMINISTRADOR sobre Persona común | No | 200 | 401, 403, 404 |
| DELETE | `/api/v1/personas/{codper}` | [Eliminar Persona](#11-eliminación-física) | PROPIETARIO o ADMINISTRADOR sobre Persona común; protege último propietario | No | 204 | 401, 403, 404, 409 |

## 5. Crear Persona

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

## 6. Consultar por identificador

`GET /api/v1/personas/{codper}`

Ejemplo: `GET {{baseUrl}}/api/v1/personas/{{personaId}}`.

- `200 OK`: devuelve `PersonaResponse`.
- `404 Not Found`: `errorCode` es `RESOURCE_NOT_FOUND`.

## 7. Listar paginado

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

## 7.1 Resumen global de Personas

`GET /api/v1/personas/resumen`

Devuelve los conteos globales de toda la base de Personas. No utiliza ni
depende de `q`, `tipoPersona`, `estado`, `page`, `size` o `sort`.

```json
{
  "totalPersonas": 37,
  "activas": 31,
  "inactivas": 6,
  "conUsuario": 24
}
```

`conUsuario` significa Persona con Usuario vinculado, independientemente de
que el Usuario esté activo o inactivo.

- `200 OK`: devuelve `PersonaResumenResponse`.
- `401 Unauthorized`: autenticación ausente o inválida.
- `403 Forbidden`: el Usuario autenticado no tiene Rol `PROPIETARIO` ni
  `ADMINISTRADOR`.

## 8. Actualizar Persona

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

## 9. Desactivar Persona

`PATCH /api/v1/personas/{codper}/desactivar`

Ejemplo: `PATCH {{baseUrl}}/api/v1/personas/{{personaId}}/desactivar`.

No necesita body. Conserva el registro, cambia `estado` a `0`, responde `200 OK` y devuelve `PersonaResponse` con `estado: 0`. Si no existe, responde `404 RESOURCE_NOT_FOUND`. Es idempotente: una Persona ya inactiva sigue respondiendo exitosamente con estado `0`.

## 10. Activar Persona

`PATCH /api/v1/personas/{codper}/activar`

Ejemplo: `PATCH {{baseUrl}}/api/v1/personas/{{personaId}}/activar`.

No necesita body. Conserva el registro, cambia `estado` a `1`, responde `200 OK` y devuelve `PersonaResponse` con `estado: 1`. Si no existe, responde `404 RESOURCE_NOT_FOUND`. Es idempotente: una Persona ya activa sigue respondiendo exitosamente con estado `1`.

## 11. Eliminación física

`DELETE /api/v1/personas/{codper}`

Elimina definitivamente el registro; no cambia `estado` a `0`. Responde `204 No Content` sin body y `404 RESOURCE_NOT_FOUND` si la Persona no existe. Una Persona eliminada físicamente no puede reactivarse mediante la API.

## 12. Diferencias entre operaciones

| Operación | Efecto | Conserva registro | Estado final | Respuesta |
|---|---|---|---|---|
| Desactivar | Actualiza estado | Sí | 0 | 200 |
| Activar | Actualiza estado | Sí | 1 | 200 |
| Eliminar | Borra físicamente | No | No aplica | 204 |

## 13. Validaciones

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

## 14. CI duplicado

La creación y la actualización responden `409 Conflict` con `errorCode=CONFLICT` si el CI ya pertenece a otra Persona. Para comprobarlo, cree dos Personas ficticias con CI distintos e intente crear una repetida o actualizar la segunda con el CI de la primera.

## 15. ProblemDetail

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

## 16. Orden recomendado de pruebas

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

## 17. Variables de Postman

```text
baseUrl = http://localhost:9090
personaId = 1
```

No incluya variables de PostgreSQL.

## 18.1 Regla vigente para correo

`correo` es obligatorio para toda Persona en `POST` y `PUT`. El backend exige
un valor no nulo, no vacio, no compuesto solo por espacios, con formato de
correo valido y un maximo de 100 caracteres. PostgreSQL tambien lo garantiza
con `NOT NULL`. No es un campo `UNIQUE`.

Casos de validacion:

| Identificador | Caso | POST | PUT |
|---|---|---:|---:|
| PERSONA-CORREO-001 | correo omitido | 400 | 400 |
| PERSONA-CORREO-002 | correo null | 400 | 400 |
| PERSONA-CORREO-003 | correo vacio | 400 | 400 |
| PERSONA-CORREO-004 | correo solo espacios | 400 | 400 |
| PERSONA-CORREO-005 | correo invalido | 400 | 400 |
| PERSONA-CORREO-006 | correo mayor a 100 caracteres | 400 | 400 |
| PERSONA-CORREO-007 | correo valido | 201 | 200 |

Ejemplo valido: `"correo": "persona@example.test"`.

Un `PUT` completo que omite `correo` responde `400 VALIDATION_ERROR`. El
ejemplo POST y el ejemplo PUT de esta guia ya incluyen correo. El orden
recomendado es crear, consultar, listar, actualizar, probar los casos
`PERSONA-CORREO-001` a `PERSONA-CORREO-007`, cambiar estados y eliminar.

Checklist:

- [ ] POST incluye correo valido.
- [ ] PUT completo incluye correo valido.
- [ ] Se probaron null, vacio, espacios, formato invalido y mas de 100 caracteres.
- [ ] No se confundio obligatoriedad con unicidad.
- [ ] OTP no forma parte de esta guia.

## 18. Autorización de Fase 11.2

Todas las rutas requieren Bearer. PROPIETARIO realiza todas las operaciones. ADMINISTRADOR puede listar, crear y operar sobre Personas comunes, pero recibe `403 ACCESS_DENIED` al consultar individualmente, modificar, activar, desactivar o eliminar una Persona asociada a un Usuario con Rol PROPIETARIO activo. INQUILINO y Usuario sin Rol reciben 403 en el CRUD genérico.

Desactivar mediante `PUT` o `PATCH`, o eliminar la Persona del último propietario activo, devuelve `409 LAST_OWNER_REQUIRED`. Con dos propietarios puede desactivarse uno. `tipo_persona` no identifica propietarios.

La carga física de fotos está disponible mediante los tres endpoints documentados. Perfil propio, restauración de registros eliminados y alcance por propiedad no forman parte de esta API. La eliminación física se mantiene; si la Persona tiene un Usuario vinculado, la FK actual puede rechazarla por integridad.
