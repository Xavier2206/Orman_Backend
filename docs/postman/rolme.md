# Postman — Asignaciones Rol–Menú (RolMe)

Esta guía administra únicamente la tabla intermedia `rolme`. Todas las rutas requieren `ROLE_PROPIETARIO` y Bearer. ADMINISTRADOR, INQUILINO y Usuario sin Rol reciben `403 ACCESS_DENIED`.

## 1. Requisitos y variables

Inicie el backend en `http://localhost:9090` y tenga un Rol y un Menú creados. Puede obtenerlos con `GET {{baseUrl}}/api/v1/roles` y `GET {{baseUrl}}/api/v1/menus`; use los `codr` y `codm` devueltos, nunca valores asumidos.

| Variable | Uso |
|---|---|
| `baseUrl` | `http://localhost:9090` |
| `accessTokenPropietario` | Bearer del propietario; no imprimirlo |
| `rolId`, `rolIdDos` | `codr` obtenidos desde Roles |
| `menuId`, `menuIdDos` | `codm` obtenidos desde Menús |

Las cuatro solicitudes de relación no llevan body. `Content-Type` no es necesario al no enviar JSON.

## 2. Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/roles/{codr}/menus/{codm}` | [Asignar Menu a Rol](#4-asignar-menu-a-rol) | Solo PROPIETARIO; Rol y Menú activos | No | 201 | 401, 403, 404, 409, 422 |
| DELETE | `/api/v1/roles/{codr}/menus/{codm}` | [Retirar Rol–Menu](#7-retirar-rol-menu) | Solo PROPIETARIO | No | 204 | 401, 403, 404 |
| GET | `/api/v1/roles/{codr}/menus` | [Consultar Menus de Rol](#5-consultar-menus-de-rol) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| GET | `/api/v1/menus/{codm}/roles` | [Consultar Roles de Menu](#6-consultar-roles-de-menu) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |

## 3. Respuesta y reglas persistentes

`RolMeResponse` contiene `codr`, `nombreRol`, `estadoRol`, `codm`, `nombreMenu` y `estadoMenu`. Una asignación requiere que ambos extremos existan y estén activos. Un duplicado devuelve `409 CONFLICT`; Rol o Menú inactivo devuelve `422 BUSINESS_RULE_VIOLATION`.

`rolme` no tiene columna `estado` ni borrado lógico. `DELETE` elimina físicamente únicamente la fila compuesta `(codr,codm)` y nunca elimina el Rol ni el Menú. Las consultas no filtran silenciosamente relaciones cuyos extremos fueron desactivados: las muestran con sus estados actuales.

## 4. Asignar Menu a Rol

### Request

`POST {{baseUrl}}/api/v1/roles/{{rolId}}/menus/{{menuId}}`

Headers: `Authorization: Bearer {{accessTokenPropietario}}`. Body: ninguno.

Éxito `201 Created`, `Location` igual a la URL de la relación y ejemplo de respuesta:

```json
{"codr":2,"nombreRol":"ADMINISTRADOR","estadoRol":1,"codm":12,"nombreMenu":"PERSONAS","estadoMenu":1}
```

```javascript
pm.test("RolMe creado", () => {
  pm.response.to.have.status(201);
  pm.expect(pm.response.headers.get("Location")).to.include("/roles/");
  const b = pm.response.json();
  pm.expect(b.codr).to.be.a("number");
  pm.expect(b.codm).to.be.a("number");
  pm.expect(b.estadoRol).to.be.oneOf([0, 1]);
  pm.expect(b.estadoMenu).to.be.oneOf([0, 1]);
});
```

Rol/Menú inexistente → `404 RESOURCE_NOT_FOUND`; asignación repetida → `409 CONFLICT`; extremo inactivo → `422 BUSINESS_RULE_VIOLATION`.

## 5. Consultar Menus de Rol

`GET {{baseUrl}}/api/v1/roles/{{rolId}}/menus` no lleva body y devuelve `200` con un arreglo de `RolMeResponse`. Un Rol existente sin asignaciones produce `[]`; un Rol inexistente produce `404`.

```javascript
pm.test("arreglo RolMe", () => {
  pm.response.to.have.status(200);
  pm.expect(pm.response.json()).to.be.an("array");
});
```

## 6. Consultar Roles de Menu

`GET {{baseUrl}}/api/v1/menus/{{menuId}}/roles` devuelve `200` y un arreglo. Un Menú sin relaciones produce `[]`; un Menú inexistente produce `404`. Los estados de Rol y Menú aparecen aunque una relación persistida tenga un extremo inactivo.

## 7. Retirar Rol Menu

`DELETE {{baseUrl}}/api/v1/roles/{{rolId}}/menus/{{menuId}}` no lleva body. La respuesta correcta es `204 No Content` sin cuerpo:

```javascript
pm.test("retiro sin body", () => {
  pm.response.to.have.status(204);
  pm.expect(pm.response.text()).to.eql("");
});
```

Repetir el retiro devuelve `404 RESOURCE_NOT_FOUND`. El Rol y el Menú continúan consultables y sus otras relaciones permanecen intactas.

## 8. ProblemDetail y autorización

Aplican `401` para autenticación inválida, `403 ACCESS_DENIED` para un autenticado sin `ROLE_PROPIETARIO`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT` y `422 BUSINESS_RULE_VIOLATION`. Validar un 403 sin imprimir el cuerpo completo:

```javascript
pm.test("403 seguro", () => {
  pm.response.to.have.status(403);
  const b = pm.response.json();
  pm.expect(b.errorCode).to.eql("ACCESS_DENIED");
});
```

El contrato de error es `application/problem+json`; no expone SQL, entidades internas, JWT ni secretos.

## 9. Matriz manual y orden recomendado

Primero obtenga `rolId` y `menuId`, confirme ambos activos, asigne, consulte desde ambos lados y finalmente retire.

| ID | Endpoint | Escenario | Datos | HTTP | errorCode | Comprobación |
|---|---|---|---|---:|---|---|
| ROLME-01 | POST relación | Asignación nueva | extremos activos | 201 | — | `Location` y response |
| ROLME-02 | POST relación | Duplicado | misma pareja | 409 | CONFLICT | una sola fila |
| ROLME-03 | POST relación | Rol inactivo | `rolId` inactivo | 422 | BUSINESS_RULE_VIOLATION | no asigna |
| ROLME-04 | POST relación | Menú inactivo | `menuId` inactivo | 422 | BUSINESS_RULE_VIOLATION | no asigna |
| ROLME-05 | GET por Rol | Sin relaciones | Rol válido | 200 | — | `[]` posible |
| ROLME-06 | GET por Menú | Sin relaciones | Menú válido | 200 | — | `[]` posible |
| ROLME-07 | DELETE relación | Retiro correcto | pareja existente | 204 | — | sin body |
| ROLME-08 | DELETE relación | Retiro repetido | misma pareja | 404 | RESOURCE_NOT_FOUND | no elimina extremos |
| ROLME-09 | GET relación | Objetivo inexistente | `codr`/`codm` desconocido | 404 | RESOURCE_NOT_FOUND | respuesta segura |
| ROLME-10 | GET relación | Sin autenticación | sin Bearer | 401 | contrato auth | no expone datos |
| ROLME-11 | POST relación | No propietario | token válido sin Rol | 403 | ACCESS_DENIED | autorización efectiva |

## 10. Checklist y limitaciones

- [ ] `rolId` y `menuId` se obtuvieron mediante sus APIs.
- [ ] Ambos extremos están activos antes de POST.
- [ ] Se verificaron arreglos desde ambos sentidos.
- [ ] Se comprobó `204` sin body y `404` al repetir DELETE.
- [ ] No se interpretó `rolme` como un recurso con estado.
- [ ] No se intentó borrar físicamente Rol o Menú.

No hay RolPro, permisos por Proceso ni endpoints de menú para el Usuario autenticado.
