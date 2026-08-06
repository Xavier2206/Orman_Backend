# Guía Postman — Asignaciones Usuario–Rol

## Autorización

Todas las rutas de esta guía requieren `ROLE_PROPIETARIO` y:

```http
Authorization: Bearer {{accessTokenPropietario}}
```

ADMINISTRADOR, INQUILINO y Usuarios sin Rol reciben `403 ACCESS_DENIED`. Nunca coloque tokens reales en documentación o capturas.

## Variables

- `baseUrl`: `http://localhost:9090`
- `loginObjetivo`: login ficticio existente
- `codrAdministrador`, `codrInquilino`, `codrPropietario`: códigos obtenidos por la API de Roles
- `accessTokenPropietario`, `accessTokenAdministrador`: valores temporales del entorno local de Postman

Antes de ejecutar la matriz debe existir un Rol activo `PROPIETARIO`, un Usuario activo con Persona activa y la asignación correspondiente. No se insertan estos datos por Flyway ni se hardcodean logins, `codper` o `codr`.

## Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| GET | `/api/v1/usuarios/{login}/roles` | [Consultar Roles de Usuario](#consultar-roles-de-usuario) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| GET | `/api/v1/roles/{codr}/usuarios` | [Consultar Usuarios de Rol](#consultar-usuarios-de-rol) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| POST | `/api/v1/usuarios/{login}/roles/{codr}` | [Asignar Rol](#asignar-rol) | Solo PROPIETARIO | No | 201 | 401, 403, 404, 409, 422 |
| DELETE | `/api/v1/usuarios/{login}/roles/{codr}` | [Retirar Rol](#retirar-rol) | Solo PROPIETARIO; protege al último PROPIETARIO | No | 204 | 401, 403, 404, 409 |

### Consultar Roles de Usuario

```http
GET {{baseUrl}}/api/v1/usuarios/{{loginObjetivo}}/roles
Authorization: Bearer {{accessTokenPropietario}}
```

### Consultar Usuarios de Rol

```http
GET {{baseUrl}}/api/v1/roles/{{codrAdministrador}}/usuarios
Authorization: Bearer {{accessTokenPropietario}}
```

### Asignar Rol

```http
POST {{baseUrl}}/api/v1/usuarios/{{loginObjetivo}}/roles/{{codrAdministrador}}
Authorization: Bearer {{accessTokenPropietario}}
```

Responde `201 Created`. La asignación devuelve el login, código/nombre del Rol y fecha de asignación.

### Retirar Rol

```http
DELETE {{baseUrl}}/api/v1/usuarios/{{loginObjetivo}}/roles/{{codrAdministrador}}
Authorization: Bearer {{accessTokenPropietario}}
```

Responde `204 No Content`. La misma mecánica permite asignar o retirar `ADMINISTRADOR`, `INQUILINO` o `PROPIETARIO`. Un Rol inactivo no puede asignarse; una asignación duplicada responde `409 CONFLICT` y un retiro inexistente `404 RESOURCE_NOT_FOUND`.

## Protección y cambios inmediatos

Retirar PROPIETARIO al último propietario activo responde `409 LAST_OWNER_REQUIRED`. Con dos propietarios activos puede retirarse uno. Un ADMINISTRADOR que intente asignarse PROPIETARIO recibe `403 ACCESS_DENIED` antes de llegar al servicio.

Conserve el JWT del Usuario objetivo, asigne ADMINISTRADOR, repita una petición autorizada, retire la asignación y repita nuevamente. La authority aparece y desaparece con el mismo JWT y sesión; el cambio de Rol no revoca sesiones.

## Evidencias seguras

Las respuestas de asignación contienen únicamente login, código/nombre de Rol y fecha de asignación. Oculte Bearer, cookies, refresh y variables antes de compartir evidencias.
