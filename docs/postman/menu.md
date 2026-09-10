# Postman — Administración de Menús

Guía para probar el catálogo `menus` y preparar sus relaciones Rol–Menú. Las rutas requieren un Usuario autenticado con `ROLE_PROPIETARIO`; ADMINISTRADOR, INQUILINO y Usuario sin Rol reciben `403 ACCESS_DENIED`.

## 1. Requisitos previos

- PostgreSQL disponible y backend iniciado en `http://localhost:9090`.
- Un Rol `PROPIETARIO` activo asignado a un Usuario y Persona activos.
- Postman con un entorno seleccionado.

Variables sugeridas:

| Variable | Ejemplo seguro |
|---|---|
| `baseUrl` | `http://localhost:9090` |
| `accessTokenPropietario` | Se guarda desde login; no escribirlo en documentación |
| `menuId` | Se guarda desde la respuesta de creación |
| `menuIdDos` | Se guarda desde otra creación |
| `menuNombre` | `PERSONAS` |
| `menuNombreDos` | `USUARIOS` |

En cada request protegido use `Authorization: Bearer {{accessTokenPropietario}}`. No imprima el token ni guarde cookies de refresh en esta guía.

## 2. Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/menus` | [Crear Menu](#4-crear-menu) | Solo PROPIETARIO | Sí | 201 | 400, 401, 403, 409 |
| GET | `/api/v1/menus` | [Listar Menus](#5-listar-menus) | Solo PROPIETARIO | No | 200 | 401, 403 |
| GET | `/api/v1/menus/resumen` | [Resumen global](#51-resumen-global) | Solo PROPIETARIO | No | 200 | 401, 403 |
| GET | `/api/v1/menus/{codm}` | [Consultar Menu](#6-consultar-menu) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| PUT | `/api/v1/menus/{codm}` | [Actualizar Menu](#7-actualizar-menu) | Solo PROPIETARIO | Sí | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/menus/{codm}/activar` | [Activar Menu](#8-activar-menu) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| PATCH | `/api/v1/menus/{codm}/desactivar` | [Desactivar Menu](#9-desactivar-menu) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |

Las asignaciones no se duplican aquí: consulte [rolme.md](rolme.md). Las relaciones Menú–Proceso se administran en [mepro.md](mepro.md).

## 3. Modelo y normalización

`MenuResponse` devuelve `codm`, `nombre`, `icono` y `estado`. `nombre` se recorta y se normaliza a mayúsculas; `icono` es opcional, se recorta y una cadena vacía se trata como `null`. `estado` solo admite `0` o `1`; si se omite al crear, PostgreSQL aplica `1`.

El nombre es único. Desactivar un Menú no elimina sus filas `rolme` ni `mepro`; las consultas administrativas de relaciones continúan mostrando extremos inactivos con su estado.

## 4. Crear Menu

### Request

`POST {{baseUrl}}/api/v1/menus`

Headers: `Authorization: Bearer {{accessTokenPropietario}}`, `Content-Type: application/json`.

Body con estado omitido:

```json
{"nombre":"PERSONAS","icono":"users"}
```

Body con estado explícito:

```json
{"nombre":"USUARIOS","icono":"users","estado":1}
```

| Campo | Tipo | Requerido | Regla |
|---|---|---:|---|
| `nombre` | string | Sí | 1–100 caracteres; se recorta y pasa a mayúsculas |
| `icono` | string | No | máximo 50; vacío se convierte en `null` |
| `estado` | entero | No | `0` o `1`; por defecto 1 |

### Response

Éxito `201 Created`, `Location: {{baseUrl}}/api/v1/menus/{{menuId}}` y:

```json
{"codm":12,"nombre":"PERSONAS","icono":"users","estado":1}
```

Script Tests seguro:

```javascript
pm.test("201 y JSON", () => {
  pm.response.to.have.status(201);
  pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
});
const body = pm.response.json();
pm.test("MenuResponse válido", () => {
  pm.expect(body.codm).to.be.a("number");
  pm.expect(body.nombre).to.eql("PERSONAS");
  pm.expect(body.estado).to.be.oneOf([0, 1]);
});
pm.environment.set("menuId", body.codm);
```

Casos negativos: nombre vacío/tipo incorrecto → `400 VALIDATION_ERROR`; nombre repetido → `409 CONFLICT`; sin Bearer → `401`; Rol insuficiente → `403 ACCESS_DENIED`.

## 5. Listar Menus

### Request

`GET {{baseUrl}}/api/v1/menus?page=0&size=20&sort=nombre,asc`

No lleva body. El backend limita `size` a 100. Los filtros opcionales son `q` y `estado`:

```text
GET {{baseUrl}}/api/v1/menus?q=control&estado=1&page=0&size=10&sort=nombre,asc
```

- `q`: busca `nombre` por coincidencia parcial, sin distinguir mayúsculas y minúsculas; los espacios exteriores se ignoran. Si es vacío, no filtra.
- `estado`: admite solamente `0` o `1`. Si se omite, se devuelven ambos estados. Un valor distinto devuelve `400 VALIDATION_ERROR`.
- Ambos filtros se combinan con `AND` y la paginación se calcula sobre el resultado filtrado en PostgreSQL.

El éxito es `200` con `PageResponse<MenuResponse>`:

```json
{"content":[{"codm":12,"nombre":"PERSONAS","icono":"users","estado":1}],"page":0,"size":20,"totalElements":1,"totalPages":1,"first":true,"last":true}
```

```javascript
pm.test("PageResponse de menus", () => {
  pm.response.to.have.status(200);
  const b = pm.response.json();
  pm.expect(b.content).to.be.an("array");
  pm.expect(b.totalElements).to.be.a("number");
});
```

Una lista vacía es un `200` válido con `content: []`, no un `404`.

## 5.1 Resumen global

`GET {{baseUrl}}/api/v1/menus/resumen` no lleva filtros ni body. Devuelve el conteo global del catálogo, independiente de la paginación:

```json
{"totalMenus":8,"activos":6,"inactivos":2}
```

`totalMenus` es igual a `activos + inactivos` porque el estado persistente solo admite `0` y `1`.

## 6. Consultar Menu

`GET {{baseUrl}}/api/v1/menus/{{menuId}}` no lleva body y devuelve `200` con `MenuResponse`. Un `codm` inexistente devuelve `404 RESOURCE_NOT_FOUND`.

```javascript
pm.test("consulta correcta", () => {
  pm.response.to.have.status(200);
  pm.expect(pm.response.json().codm).to.eql(Number(pm.environment.get("menuId")));
});
```

## 7. Actualizar Menu

### Request

`PUT {{baseUrl}}/api/v1/menus/{{menuId}}` con `Content-Type: application/json`:

```json
{"nombre":"PERSONAS","icono":"people"}
```

El `PUT` no cambia `estado`; use los endpoints de estado para ello. Devuelve `200` con el recurso actualizado. Nombre repetido → `409`; recurso inexistente → `404`; body inválido → `400`.

## 8. Activar Menu

`PATCH {{baseUrl}}/api/v1/menus/{{menuId}}/activar` sin body. Devuelve `200` y `estado: 1`. Repetir la operación es idempotente.

## 9. Desactivar Menu

`PATCH {{baseUrl}}/api/v1/menus/{{menuId}}/desactivar` sin body. Devuelve `200` y `estado: 0`; repetirla también es idempotente. No elimina relaciones RolMe o MePro. La asignación de una relación nueva a este Menú inactivo devuelve `422 BUSINESS_RULE_VIOLATION`.

## 10. ProblemDetail y errores

Los errores usan `application/problem+json` y pueden incluir `type`, `title`, `status`, `detail`, `instance`, `errorCode`, `timestamp` y `traceId`. En esta guía son aplicables `400 VALIDATION_ERROR` para `estado` distinto de `0` o `1`, `400 INVALID_REQUEST` para requests mal formados, `401`, `403 ACCESS_DENIED`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT` y `422 BUSINESS_RULE_VIOLATION`. Un `500 INTERNAL_ERROR` es inesperado y no debe provocarse como prueba.

## 11. Orden recomendado y matriz manual

| ID | Endpoint | Escenario | Datos | HTTP | errorCode | Comprobación |
|---|---|---|---|---:|---|---|
| MENU-01 | POST `/menus` | Crear sin estado | nombre nuevo | 201 | — | `Location`, `codm`, estado 1 |
| MENU-02 | POST `/menus` | Crear duplicado | mismo nombre | 409 | CONFLICT | no crea otra fila |
| MENU-03 | GET `/menus` | Lista vacía o con datos | propietario | 200 | — | `content` es arreglo |
| MENU-04 | PUT `/menus/{codm}` | Actualizar | nombre/icono válidos | 200 | — | estado se conserva |
| MENU-05 | PATCH activar/desactivar | Cambio repetido | `menuId` | 200 | — | idempotencia |
| MENU-06 | DELETE `/menus/{codm}` | DELETE físico | `menuId` | 405 | — | ruta inexistente |
| MENU-07 | POST `/menus` | Sin autenticación | sin Bearer | 401 | contrato auth | no expone datos |
| MENU-08 | GET `/menus` | Rol no propietario | token válido | 403 | ACCESS_DENIED | ProblemDetail seguro |
| MENU-09 | GET `/menus` | Filtros combinados | `q=control`, `estado=1` | 200 | — | total paginado filtrado |
| MENU-10 | GET `/menus` | Estado inválido | `estado=2` | 400 | VALIDATION_ERROR | ProblemDetail seguro |
| MENU-11 | GET `/menus/resumen` | Resumen global | propietario | 200 | — | total, activos e inactivos |

## 12. Checklist y limitaciones

- [ ] Backend en puerto 9090 y entorno Postman seleccionado.
- [ ] Token de PROPIETARIO vigente en `accessTokenPropietario`.
- [ ] Se guardó `menuId` desde `codm`, nunca desde un valor asumido.
- [ ] Se verificó `Location` sin imprimir el token.
- [ ] Se probaron `q`, `estado`, filtros combinados y paginación filtrada.
- [ ] Se verificó el resumen global sin enviar filtros.
- [ ] No se intentó DELETE físico de Menú.
- [ ] Las relaciones se prueban en [rolme.md](rolme.md) y [mepro.md](mepro.md).

La navegación del Usuario autenticado se documenta en [auth.md](auth.md); esta guía cubre exclusivamente la administración de Menús.
