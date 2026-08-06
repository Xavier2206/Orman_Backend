# Postman — Administración de Procesos

Esta guía prueba `procesos` y prepara relaciones Menú–Proceso. Todas las rutas requieren `ROLE_PROPIETARIO`; no existe autorización por Proceso ni menú del Usuario autenticado.

## 1. Requisitos previos y variables

Inicie PostgreSQL y el backend en `http://localhost:9090`. Seleccione un entorno Postman con:

| Variable | Ejemplo seguro |
|---|---|
| `baseUrl` | `http://localhost:9090` |
| `accessTokenPropietario` | token obtenido por login, sin imprimirlo |
| `procesoId` / `procesoIdDos` | se guardan desde `codp` |
| `procesoNombre` | `LISTAR PERSONAS` |
| `procesoNombreDos` | `CREAR PERSONA` |
| `procesoEnlace` | `personas/listar` |
| `procesoEnlaceDos` | `personas/crear` |

Use `Authorization: Bearer {{accessTokenPropietario}}` y `Content-Type: application/json` solo cuando exista body.

## 2. Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/procesos` | [Crear Proceso](#4-crear-proceso) | Solo PROPIETARIO | Sí | 201 | 400, 401, 403, 409 |
| GET | `/api/v1/procesos` | [Listar Procesos](#5-listar-procesos) | Solo PROPIETARIO | No | 200 | 401, 403 |
| GET | `/api/v1/procesos/{codp}` | [Consultar Proceso](#6-consultar-proceso) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| PUT | `/api/v1/procesos/{codp}` | [Actualizar Proceso](#7-actualizar-proceso) | Solo PROPIETARIO | Sí | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/procesos/{codp}/activar` | [Activar Proceso](#8-activar-proceso) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| PATCH | `/api/v1/procesos/{codp}/desactivar` | [Desactivar Proceso](#9-desactivar-proceso) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |

Administre las relaciones en [mepro.md](mepro.md). No existe `DELETE /api/v1/procesos/{codp}`.

## 3. Modelo y normalización

`ProcesoResponse` devuelve `codp`, `nombre`, `enlace` y `estado`. `nombre` se recorta y se convierte a mayúsculas; `enlace` solo se recorta, conservando su contenido y mayúsculas/minúsculas. `nombre` y `enlace` son únicos. `estado` admite únicamente `0` o `1`; si se omite al crear, PostgreSQL aplica `1`.

`enlace` es un dato persistente del Proceso. No es una authority, permiso ni una ruta REST que el backend registre automáticamente.

## 4. Crear Proceso

### Request

`POST {{baseUrl}}/api/v1/procesos`

```json
{"nombre":"LISTAR PERSONAS","enlace":"personas/listar"}
```

También puede enviar estado explícito:

```json
{"nombre":"CREAR PERSONA","enlace":"personas/crear","estado":1}
```

| Campo | Tipo | Requerido | Regla |
|---|---|---:|---|
| `nombre` | string | Sí | 1–100; trim y mayúsculas |
| `enlace` | string | Sí | 1–60; trim; único |
| `estado` | entero | No | `0` o `1`; por defecto 1 |

Éxito `201 Created`, `Location: {{baseUrl}}/api/v1/procesos/{{procesoId}}` y:

```json
{"codp":21,"nombre":"LISTAR PERSONAS","enlace":"personas/listar","estado":1}
```

```javascript
pm.test("201 y Location", () => {
  pm.response.to.have.status(201);
  pm.expect(pm.response.headers.get("Location")).to.include("/api/v1/procesos/");
});
const b = pm.response.json();
pm.test("ProcesoResponse válido", () => {
  pm.expect(b.codp).to.be.a("number");
  pm.expect(b.nombre).to.be.a("string");
  pm.expect(b.enlace).to.be.a("string");
  pm.expect(b.estado).to.be.oneOf([0, 1]);
});
pm.environment.set("procesoId", b.codp);
```

Nombre o enlace repetidos → `409 CONFLICT`; body incompleto, JSON mal formado o tipo incorrecto → `400`.

## 5. Listar Procesos

`GET {{baseUrl}}/api/v1/procesos?page=0&size=20&sort=nombre,asc` no lleva body. Devuelve `200` con `PageResponse<ProcesoResponse>` y limita el tamaño a 100.

```javascript
pm.test("PageResponse de procesos", () => {
  pm.response.to.have.status(200);
  const b = pm.response.json();
  pm.expect(b.content).to.be.an("array");
  pm.expect(b.totalElements).to.be.a("number");
});
```

Una lista vacía sigue siendo `200` con `content: []`.

## 6. Consultar Proceso

`GET {{baseUrl}}/api/v1/procesos/{{procesoId}}` devuelve `200` y un `ProcesoResponse`. Un `codp` inexistente devuelve `404 RESOURCE_NOT_FOUND`.

## 7. Actualizar Proceso

`PUT {{baseUrl}}/api/v1/procesos/{{procesoId}}`:

```json
{"nombre":"LISTAR PERSONAS","enlace":"personas/listar"}
```

El `PUT` no cambia `estado`. Nombre o enlace duplicados → `409`; `codp` inexistente → `404`; body inválido → `400`. La respuesta `200` contiene los cuatro campos.

## 8. Activar Proceso

`PATCH {{baseUrl}}/api/v1/procesos/{{procesoId}}/activar` sin body devuelve `200` con `estado: 1`. Repetirlo es idempotente.

## 9. Desactivar Proceso

`PATCH {{baseUrl}}/api/v1/procesos/{{procesoId}}/desactivar` sin body devuelve `200` con `estado: 0` y no elimina filas `mepro`. Una nueva asignación Menú–Proceso hacia este extremo inactivo devuelve `422 BUSINESS_RULE_VIOLATION`; las consultas administrativas continúan mostrando relaciones existentes.

## 10. ProblemDetail y errores

Las respuestas de error son `application/problem+json` con los campos estándar y `errorCode`. Son aplicables `400 VALIDATION_ERROR` o `INVALID_REQUEST`, `401`, `403 ACCESS_DENIED`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT` y `422 BUSINESS_RULE_VIOLATION`. `500 INTERNAL_ERROR` solo representa un fallo inesperado.

## 11. Orden, matriz y checklist

| ID | Endpoint | Escenario | Datos | HTTP | errorCode | Comprobación |
|---|---|---|---|---:|---|---|
| PROC-01 | POST `/procesos` | Crear sin estado | nombre/enlace nuevos | 201 | — | `codp`, `Location`, estado 1 |
| PROC-02 | POST `/procesos` | Nombre duplicado | nombre existente | 409 | CONFLICT | no duplica |
| PROC-03 | POST `/procesos` | Enlace duplicado | enlace existente | 409 | CONFLICT | no duplica |
| PROC-04 | GET `/procesos` | Lista vacía o con datos | propietario | 200 | — | `content` arreglo |
| PROC-05 | PUT `/procesos/{codp}` | Actualizar | body válido | 200 | — | enlace persistido |
| PROC-06 | PATCH activar/desactivar | Idempotencia | `procesoId` | 200 | — | estado esperado |
| PROC-07 | DELETE `/procesos/{codp}` | DELETE físico | `procesoId` | 405 | — | no existe ruta |
| PROC-08 | GET `/procesos` | Sin autenticación | sin Bearer | 401 | contrato auth | no expone datos |
| PROC-09 | POST `/procesos` | Rol insuficiente | token no propietario | 403 | ACCESS_DENIED | ProblemDetail seguro |

- [ ] Backend en puerto 9090 y token de PROPIETARIO vigente.
- [ ] `procesoId` se obtuvo dinámicamente desde `codp`.
- [ ] Se probaron duplicidades de nombre y enlace.
- [ ] No se intentó eliminar físicamente un Proceso.
- [ ] Las relaciones se prueban en [mepro.md](mepro.md).

## 12. Limitaciones

La fase no crea endpoints `/me/menu`, authorities por Procesos, permisos dinámicos ni integración con Angular o Flutter.
