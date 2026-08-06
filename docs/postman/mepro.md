# Postman — Asignaciones Menú–Proceso (MePro)

Esta guía administra únicamente la tabla intermedia `mepro`. Todas las rutas requieren `ROLE_PROPIETARIO` y Bearer.

## 1. Requisitos y variables

Inicie el backend en `http://localhost:9090`. Obtenga `menuId` con `GET {{baseUrl}}/api/v1/menus` y `procesoId` con `GET {{baseUrl}}/api/v1/procesos`; no asuma identificadores numéricos.

| Variable | Uso |
|---|---|
| `baseUrl` | `http://localhost:9090` |
| `accessTokenPropietario` | Bearer del propietario, sin imprimirlo |
| `menuId`, `menuIdDos` | `codm` dinámicos |
| `procesoId`, `procesoIdDos` | `codp` dinámicos |

## 2. Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/menus/{codm}/procesos/{codp}` | [Asignar Proceso a Menu](#4-asignar-proceso-a-menu) | Solo PROPIETARIO; Menú y Proceso activos | No | 201 | 401, 403, 404, 409, 422 |
| DELETE | `/api/v1/menus/{codm}/procesos/{codp}` | [Retirar Menu–Proceso](#7-retirar-menu-proceso) | Solo PROPIETARIO | No | 204 | 401, 403, 404 |
| GET | `/api/v1/menus/{codm}/procesos` | [Consultar Procesos de Menu](#5-consultar-procesos-de-menu) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| GET | `/api/v1/procesos/{codp}/menus` | [Consultar Menus de Proceso](#6-consultar-menus-de-proceso) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |

## 3. Respuesta y reglas persistentes

`MeProResponse` devuelve `codm`, `nombreMenu`, `estadoMenu`, `codp`, `nombreProceso`, `enlaceProceso` y `estadoProceso`. Ambos extremos deben existir y estar activos para una nueva asignación. Un extremo inactivo produce `422 BUSINESS_RULE_VIOLATION`; un duplicado produce `409 CONFLICT`.

`mepro` no tiene columna `estado`, no existe borrado lógico y `DELETE` elimina físicamente solo la fila `(codm,codp)`. No elimina Menú ni Proceso. Las relaciones existentes con extremos desactivados permanecen visibles en las consultas administrativas junto con sus estados.

## 4. Asignar Proceso a Menu

`POST {{baseUrl}}/api/v1/menus/{{menuId}}/procesos/{{procesoId}}` sin body y con `Authorization: Bearer {{accessTokenPropietario}}`.

Devuelve `201 Created`, `Location` de la relación y:

```json
{"codm":12,"nombreMenu":"PERSONAS","estadoMenu":1,"codp":21,"nombreProceso":"LISTAR PERSONAS","enlaceProceso":"personas/listar","estadoProceso":1}
```

```javascript
pm.test("MePro creado", () => {
  pm.response.to.have.status(201);
  pm.expect(pm.response.headers.get("Location")).to.include("/menus/");
  const b = pm.response.json();
  pm.expect(b.codm).to.be.a("number");
  pm.expect(b.codp).to.be.a("number");
  pm.expect(b.enlaceProceso).to.be.a("string");
  pm.expect(b.estadoMenu).to.be.oneOf([0, 1]);
  pm.expect(b.estadoProceso).to.be.oneOf([0, 1]);
});
```

Menú/Proceso inexistente → `404`; duplicado → `409`; Menú inactivo o Proceso inactivo → `422`.

## 5. Consultar Procesos de Menu

`GET {{baseUrl}}/api/v1/menus/{{menuId}}/procesos` no lleva body y devuelve `200` con un arreglo de `MeProResponse`. Un Menú válido sin relaciones da `[]`; un Menú inexistente da `404`.

```javascript
pm.test("arreglo de procesos", () => {
  pm.response.to.have.status(200);
  pm.expect(pm.response.json()).to.be.an("array");
});
```

## 6. Consultar Menus de Proceso

`GET {{baseUrl}}/api/v1/procesos/{{procesoId}}/menus` devuelve `200` con un arreglo. Los estados de ambos extremos se incluyen aun cuando una relación persistida tenga un extremo inactivo.

## 7. Retirar Menu Proceso

`DELETE {{baseUrl}}/api/v1/menus/{{menuId}}/procesos/{{procesoId}}` sin body devuelve `204 No Content` sin cuerpo y elimina únicamente la fila de `mepro`.

```javascript
pm.test("204 sin body", () => {
  pm.response.to.have.status(204);
  pm.expect(pm.response.text()).to.eql("");
});
```

Repetir el retiro devuelve `404 RESOURCE_NOT_FOUND`. Menú y Proceso continúan existiendo y pueden volver a relacionarse si están activos.

## 8. ProblemDetail, autorización y errores

Aplican `401`, `403 ACCESS_DENIED`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT` y `422 BUSINESS_RULE_VIOLATION`; errores de JSON/body son `400 VALIDATION_ERROR` o `INVALID_REQUEST`. No provoque `500 INTERNAL_ERROR`.

```javascript
pm.test("403 ACCESS_DENIED", () => {
  pm.response.to.have.status(403);
  pm.expect(pm.response.json().errorCode).to.eql("ACCESS_DENIED");
});
```

## 9. Matriz manual y orden recomendado

Cree primero Menú y Proceso, consulte ambos, asígnelos, consulte desde ambos sentidos, desactive un extremo para comprobar que la relación persiste visible y finalmente retírela.

| ID | Endpoint | Escenario | Datos | HTTP | errorCode | Comprobación |
|---|---|---|---|---:|---|---|
| MEPRO-01 | POST relación | Asignación nueva | extremos activos | 201 | — | `Location`, nombres, enlace y estados |
| MEPRO-02 | POST relación | Duplicado | misma pareja | 409 | CONFLICT | no duplica |
| MEPRO-03 | POST relación | Menú inactivo | `menuId` inactivo | 422 | BUSINESS_RULE_VIOLATION | no asigna |
| MEPRO-04 | POST relación | Proceso inactivo | `procesoId` inactivo | 422 | BUSINESS_RULE_VIOLATION | no asigna |
| MEPRO-05 | GET por Menú | Lista vacía o con datos | `menuId` válido | 200 | — | arreglo |
| MEPRO-06 | GET por Proceso | Lista vacía o con datos | `procesoId` válido | 200 | — | arreglo |
| MEPRO-07 | DELETE relación | Retiro correcto | pareja existente | 204 | — | sin body |
| MEPRO-08 | DELETE relación | Retiro repetido | misma pareja | 404 | RESOURCE_NOT_FOUND | extremos existen |
| MEPRO-09 | GET relación | Objetivo inexistente | id desconocido | 404 | RESOURCE_NOT_FOUND | no filtra error |
| MEPRO-10 | GET relación | Sin autenticación | sin Bearer | 401 | contrato auth | no expone datos |
| MEPRO-11 | POST relación | No propietario | token insuficiente | 403 | ACCESS_DENIED | autorización efectiva |

## 10. Checklist y limitaciones

- [ ] `menuId` y `procesoId` se obtuvieron de sus APIs.
- [ ] Ambos estaban activos al asignar.
- [ ] Se revisaron `enlaceProceso` y ambos estados.
- [ ] Se comprobó que DELETE no elimina Menú ni Proceso.
- [ ] Se confirmó que `mepro` no tiene estado propio.
- [ ] No se probaron funcionalidades de Fase 12.3.
