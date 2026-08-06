# Fase 12.2 — Administración REST de Menús, Procesos y relaciones

## Alcance

Se administra el modelo persistente de la Fase 12.1 sin alterar autenticación ni autorización existente:

`Usuarios → RolUsu → Roles → RolMe → Menús → MePro → Procesos`.

Todas las rutas descritas requieren `ROLE_PROPIETARIO`. ADMINISTRADOR, INQUILINO y un Usuario autenticado sin Roles reciben `403 ACCESS_DENIED`; una identidad inválida conserva el `401` de Fase 10.

## Endpoints

| Módulo | Rutas |
|---|---|
| Menús | `POST/GET /api/v1/menus`, `GET/PUT /api/v1/menus/{codm}`, `PATCH /activar` y `/desactivar` |
| Procesos | `POST/GET /api/v1/procesos`, `GET/PUT /api/v1/procesos/{codp}`, `PATCH /activar` y `/desactivar` |
| RolMe | `POST/DELETE /api/v1/roles/{codr}/menus/{codm}`, `GET /api/v1/roles/{codr}/menus`, `GET /api/v1/menus/{codm}/roles` |
| MePro | `POST/DELETE /api/v1/menus/{codm}/procesos/{codp}`, `GET /api/v1/menus/{codm}/procesos`, `GET /api/v1/procesos/{codp}/menus` |

Crear devuelve `201` y `Location`; consultar, listar, actualizar y cambiar estado devuelve `200`; retirar una relación devuelve `204`.

## Reglas

- Menú: `nombre` único, normalizado con `trim` y mayúsculas; `icono` opcional; estado `0` o `1`.
- Proceso: `nombre` único normalizado; `enlace` obligatorio, recortado y único; el enlace es dato persistente, no endpoint ni authority.
- No existe eliminación física de Menús ni Procesos. Activar y desactivar son idempotentes.
- RolMe y MePro son entidades explícitas de clave compuesta. Solo sus filas se eliminan físicamente; retirar una no elimina sus extremos.
- Una asignación exige Rol y Menú activos, o Menú y Proceso activos, según corresponda. Un extremo inactivo produce `422`.
- Las consultas de relaciones no ocultan filas cuyo Rol, Menú o Proceso haya sido desactivado; por eso sus respuestas incluyen los estados administrativos de ambos extremos.
- Una relación duplicada devuelve `409`; retirar una inexistente devuelve `404`.

## Contratos

`POST /menus`: `{ "nombre": "PERSONAS", "icono": "users", "estado": 1 }`.

`PUT /menus/{codm}`: `{ "nombre": "PERSONAS", "icono": "users" }`.

`POST /procesos`: `{ "nombre": "LISTAR PERSONAS", "enlace": "personas/listar", "estado": 1 }`.

`PUT /procesos/{codp}`: `{ "nombre": "LISTAR PERSONAS", "enlace": "personas/listar" }`.

Las rutas de RolMe y MePro no reciben body. Sus respuestas incluyen identificadores, nombres y estados de sus dos extremos.

## Errores

Se mantiene `ProblemDetail`: `400 VALIDATION_ERROR`, `401` de autenticación, `403 ACCESS_DENIED`, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT` y `422 BUSINESS_RULE_VIOLATION` para asignar un extremo inactivo. No se exponen entidades, SQL, tokens ni secretos.

## Exclusiones

No se creó V8, `rolpro`, datos iniciales, menú del Usuario autenticado, authorities por Proceso, endpoints `/me/menu`, integración de clientes ni Fase 12.3. JWT, sesiones, CORS, CSRF, filtros y reglas de Fase 11.2 no se modifican.

## Validación

La fase cubre DTO/mappers, servicios transaccionales, contratos MVC y persistencia PostgreSQL. Flyway conserva V7 como última migración y Hibernate sigue validando el esquema.
