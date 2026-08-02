# Guía Postman — Roles y relación Usuario–Rol

## Preparación

Configure un entorno `ORMAN Local` con:

```text
baseUrl = http://localhost:9090
loginRol = usuario.rol.postman
codrUno =
codrDos =
```

Antes de asignar Roles, cree una Persona y un Usuario mediante la guía de Usuario. No use contraseñas reales en evidencias ni comparta hashes BCrypt.

## Flujo completo

### 1. Crear Rol

`POST {{baseUrl}}/api/v1/roles`

```json
{
  "nombre": " administrador "
}
```

Espere `201 Created`, `Location` y un nombre normalizado a `ADMINISTRADOR`. Guarde `$.codr` como `codrUno`.

### 2. Crear segundo Rol

`POST {{baseUrl}}/api/v1/roles`

```json
{
  "nombre": "OPERADOR",
  "estado": 1
}
```

Guarde `$.codr` como `codrDos`.

### 3. Consultar y listar

- `GET {{baseUrl}}/api/v1/roles/{{codrUno}}`
- `GET {{baseUrl}}/api/v1/roles?page=0&size=20`

Ambas responden `200`. El listado usa `PageResponse` y orden por nombre.

### 4. Actualizar nombre

`PUT {{baseUrl}}/api/v1/roles/{{codrUno}}`

```json
{
  "nombre": "SUPERVISOR"
}
```

Espere `200`; solo cambia el nombre.

### 5. Asignar Rol

`POST {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrUno}}`

No requiere body. Espere `201 Created`, `Location` y:

```json
{
  "login": "usuario.rol.postman",
  "codr": 1,
  "nombreRol": "SUPERVISOR",
  "fechaAsignacion": "..."
}
```

La respuesta no debe contener `password`, `passwd`, hash, Persona, CI, correo, teléfono ni foto.

### 6. Consultar asignaciones

- `GET {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles`
- `GET {{baseUrl}}/api/v1/roles/{{codrUno}}/usuarios`

Ambas responden `200`. Cuando no existan asignaciones, responden un arreglo vacío.

### 7. Duplicado

Repita la asignación del paso 5. Espere `409 CONFLICT` y `errorCode: CONFLICT`.

### 8. Desactivar y comprobar regla

`PATCH {{baseUrl}}/api/v1/roles/{{codrDos}}/desactivar`

Luego intente asignarlo. Espere `422 Unprocessable Content` y `errorCode: BUSINESS_RULE_VIOLATION`. Las asignaciones previas del Rol desactivado se conservan.

### 9. Reactivar y retirar

- `PATCH {{baseUrl}}/api/v1/roles/{{codrDos}}/activar`
- `DELETE {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrUno}}`

La reactivación responde `200`; el retiro responde `204 No Content` sin body. Repetir el retiro responde `404`.

## Validaciones y errores

Pruebe:

- nombre ausente, vacío o mayor a 50 caracteres: `400 VALIDATION_ERROR`;
- estado diferente de 0 o 1: `400 VALIDATION_ERROR`;
- nombre de Rol duplicado: `409 CONFLICT`;
- Usuario o Rol inexistente: `404 RESOURCE_NOT_FOUND`;
- asignación inexistente al retirar: `404 RESOURCE_NOT_FOUND`;
- Rol inactivo al asignar: `422 BUSINESS_RULE_VIOLATION`.

No existe `DELETE /api/v1/roles/{codr}`. No hay endpoints de login, JWT, sesiones, authorities, autorización, menús, procesos ni OTP.
