# Guía Postman — Roles y relación Usuario–Rol

## 1. Objetivo de la guía

Esta guía explica cómo probar mediante Postman la administración de Roles y la relación entre Usuarios y Roles implementada en ORMAN-BACKEND.

Incluye solicitudes correctas, validaciones, conflictos, estados, asignación y retiro, respuestas `ProblemDetail`, paginación y comprobaciones de seguridad de las respuestas.

## 2. Alcance actual

La fase actual incluye:

- catálogo REST de Roles;
- creación, consulta, listado, actualización de nombre, activación y desactivación;
- asignación y retiro de Roles;
- listado de Roles por Usuario;
- listado de Usuarios por Rol;
- normalización de nombres a mayúsculas;
- validación de nombres y estados;
- relación persistente `rolusu`.

Las operaciones de este catálogo requieren `ROLE_PROPIETARIO`. La autenticación, JWT, sesiones y authorities se prueban en [auth.md](auth.md). Los Roles no están dentro del JWT y los cambios de asignación o estado se reflejan en la siguiente petición sin revocar la sesión.

### Gestión remota para la pantalla de Roles

`GET /api/v1/roles` conserva `page`, `size` y `sort`, y acepta los filtros opcionales `q` y `estado` en una única consulta paginada de PostgreSQL:

```http
GET {{baseUrl}}/api/v1/roles?q=admin&estado=1&page=0&size=10&sort=nombre,asc
```

- `q` busca parcialmente en `nombre`, sin distinguir mayúsculas/minúsculas; se aplica `trim` y un valor nulo, vacío o solo espacios no filtra.
- `estado` admite únicamente `1` (activo) o `0` (inactivo); si se omite devuelve ambos estados. Otro valor responde `400 VALIDATION_ERROR` en `application/problem+json`.
- Los filtros se combinan con `AND`; `totalElements`, `totalPages`, `first` y `last` describen solamente el conjunto filtrado.

El resumen es global e independiente de filtros y paginación:

```http
GET {{baseUrl}}/api/v1/roles/resumen
```

```json
{
  "totalRoles": 8,
  "activos": 6,
  "inactivos": 2
}
```

El Backend calcula los tres valores directamente en base de datos. No hay filtrado local requerido en Frontend.

## 3. Requisitos previos

Antes de comenzar:

- PostgreSQL debe estar disponible.
- Las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` deben estar configuradas para el proceso del backend.
- Flyway debe haber aplicado V1 a V5.
- Hibernate debe iniciar con `ddl-auto=validate`.
- Debe existir una Persona sin Usuario para la preparación del flujo.
- Debe usarse información ficticia en las pruebas.

Las migraciones V4 y V5 crean `roles` y `rolusu`. No se insertan Roles iniciales automáticamente.

## 4. Inicio del backend

En PowerShell, configure las variables externas y arranque la aplicación:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "orman"
$env:DB_USERNAME = "<usuario>"
$env:DB_PASSWORD = "<contraseña>"
.\mvnw.cmd spring-boot:run
```

No escriba credenciales reales en la colección, en capturas ni en archivos versionados.

Compruebe que el backend escucha en:

```text
http://localhost:9090
```

## 5. Configuración del entorno Postman

1. Abra Postman.
2. Cree o seleccione el entorno `ORMAN Local`.
3. Agregue las variables indicadas en la siguiente sección.
4. Guarde el entorno.
5. Selecciónelo antes de ejecutar las solicitudes.

Todas las solicitudes de este catálogo requieren `Authorization: Bearer {{accessTokenPropietario}}`. Obtenga el token mediante [auth.md](auth.md); no agregue `ROLE_PROPIETARIO` al JWT ni lo almacene en PostgreSQL.

## 6. Variables del entorno

Use estas variables:

| Variable | Initial value | Current value | Uso |
|---|---|---|---|
| `baseUrl` | `http://localhost:9090` | `http://localhost:9090` | URL del backend. |
| `loginRol` | `usuario.rol.postman` | `usuario.rol.postman` | Login de un Usuario de prueba. |
| `accessTokenPropietario` | vacío | vacío | Access token temporal obtenido con login WEB o MOBILE de un PROPIETARIO. |
| `codrUno` | vacío | vacío | Identificador del primer Rol. |
| `codrDos` | vacío | vacío | Identificador del segundo Rol. |

Las rutas de asignación deben utilizar siempre la variable `{{loginRol}}`.

## 7. URL base

Todas las solicitudes de esta guía utilizan:

```text
{{baseUrl}}
```

Ejemplo:

```text
{{baseUrl}}/api/v1/roles
```

## 8. Headers generales

Para solicitudes con JSON:

```text
Content-Type: application/json
Accept: application/json
```

Para `GET`, `PATCH` y `DELETE` sin body basta con:

```text
Accept: application/json
```

El retiro exitoso responde `204 No Content` y no incluye body.

## 9. Preparación de Persona y Usuario

La relación de Roles se asigna a un Usuario existente. Prepare primero una Persona mediante la guía de Persona.

Ejemplo de Persona ficticia:

```http
POST {{baseUrl}}/api/v1/personas
```

```json
{
  "ci": "POSTMAN-ROL-001",
  "nombre": "Persona Roles",
  "genero": "F",
  "telefono": "70000001",
  "tipoPersona": "A"
}
```

Guarde el `codper` de la respuesta. Luego cree el Usuario:

```http
POST {{baseUrl}}/api/v1/usuarios
```

```json
{
  "login": "{{loginRol}}",
  "password": "Temporal-Rol-2026",
  "codper": 1
}
```

Reemplace `1` por el `codper` real. La respuesta de Usuario no contiene `passwd` ni el hash BCrypt.

Si el CI o el login ya existen, use valores ficticios nuevos. La guía de Roles no crea Personas ni Usuarios automáticamente.

## Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/roles` | [Crear Rol](#11-crear-rol) | Solo PROPIETARIO | Sí | 201 | 400, 401, 403, 409 |
| GET | `/api/v1/roles/{codr}` | [Consultar Rol](#12-consultar-rol-por-codr) | Solo PROPIETARIO | No | 200 | 401, 403, 404 |
| GET | `/api/v1/roles/resumen` | Resumen global | Solo PROPIETARIO | No | 200 | 401, 403 |
| GET | `/api/v1/roles` | [Listar Roles](#13-listar-roles-con-paginación) | Solo PROPIETARIO; `q,estado,page,size,sort` | No | 200 | 400, 401, 403 |
| PUT | `/api/v1/roles/{codr}` | [Actualizar Rol](#14-actualizar-nombre-del-rol) | Solo PROPIETARIO; Rol `PROPIETARIO` protegido | Sí | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/roles/{codr}/activar` | [Activar Rol](#15-activar-rol) | Solo PROPIETARIO; Rol `PROPIETARIO` protegido | No | 200 | 401, 403, 404, 409 |
| PATCH | `/api/v1/roles/{codr}/desactivar` | [Desactivar Rol](#16-desactivar-rol) | Solo PROPIETARIO; Rol `PROPIETARIO` protegido | No | 200 | 401, 403, 404, 409 |

Las asignaciones Usuario–Rol tienen su tabla completa en [rolusu.md](rolusu.md). No existe eliminación física de Roles.

## 11. Crear Rol

### Solicitud

```http
POST {{baseUrl}}/api/v1/roles
```

Headers:

```text
Content-Type: application/json
Accept: application/json
```

Body válido sin estado explícito:

```json
{
  "nombre": " administrador "
}
```

Body válido con estado:

```json
{
  "nombre": "OPERADOR",
  "estado": 1
}
```

Reglas reales:

- `nombre` es obligatorio mediante `@NotBlank`.
- Se eliminan espacios externos con `trim`.
- Se convierte a mayúsculas con `Locale.ROOT`.
- El máximo es 50 caracteres.
- `estado` es opcional.
- Si `estado` se omite, PostgreSQL aplica `1`.
- `estado` solo admite `0` o `1`.
- Un nombre ya registrado produce `409 CONFLICT`.

### Respuesta exitosa

Status: `201 Created`.

Header:

```text
Location: http://localhost:9090/api/v1/roles/1
```

Body:

```json
{
  "codr": 1,
  "nombre": "ADMINISTRADOR",
  "estado": 1
}
```

Guarde el identificador con este script:

```javascript
pm.test("status 201", function () {
    pm.response.to.have.status(201);
});

pm.test("existe Location", function () {
    pm.expect(pm.response.headers.get("Location")).to.be.a("string").and.not.empty;
});

const body = pm.response.json();

pm.test("RolResponse contiene campos reales", function () {
    pm.expect(body).to.have.property("codr");
    pm.expect(body).to.have.property("nombre");
    pm.expect(body).to.have.property("estado");
});

pm.test("nombre normalizado", function () {
    pm.expect(body.nombre).to.eql("ADMINISTRADOR");
});

pm.test("estado activo", function () {
    pm.expect(body.estado).to.eql(1);
});

pm.test("no hay campos sensibles", function () {
    const serialized = JSON.stringify(body).toLowerCase();
    ["password", "passwd", "password_hash", "hash", "bcrypt"].forEach(function (field) {
        pm.expect(serialized).to.not.include(field);
    });
});

pm.environment.set("codrUno", body.codr);
```

Para el segundo Rol, ejecute la misma solicitud con `OPERADOR` y guarde el identificador en `codrDos`:

```javascript
pm.test("status 201", function () {
    pm.response.to.have.status(201);
});

const body = pm.response.json();
pm.expect(body.nombre).to.eql("OPERADOR");
pm.environment.set("codrDos", body.codr);
```

## 12. Consultar Rol por codr

### Solicitud

```http
GET {{baseUrl}}/api/v1/roles/{{codrUno}}
```

Header:

```text
Accept: application/json
```

### Rol existente

Responde `200 OK`:

```json
{
  "codr": 1,
  "nombre": "ADMINISTRADOR",
  "estado": 1
}
```

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
pm.expect(body.codr).to.eql(Number(pm.environment.get("codrUno")));
pm.expect(body).to.not.have.property("passwd");
pm.expect(body).to.not.have.property("password");
```

### Rol inexistente

```http
GET {{baseUrl}}/api/v1/roles/999999
```

Responde `404` con `errorCode: RESOURCE_NOT_FOUND` y `detail: "Rol no encontrado."`.

### `codr` no numérico

```http
GET {{baseUrl}}/api/v1/roles/no-numerico
```

`codr` se convierte a `Integer` en el controller. No existe una validación dedicada para texto no numérico; el manejador global actual lo trata como error inesperado (`500 INTERNAL_ERROR`). Use identificadores numéricos en las pruebas funcionales.

## 13. Listar Roles con paginación

### Solicitud

```http
GET {{baseUrl}}/api/v1/roles?page=0&size=20
```

Header:

```text
Accept: application/json
```

La respuesta usa `PageResponse<RolResponse>`:

```json
{
  "content": [
    {
      "codr": 1,
      "nombre": "ADMINISTRADOR",
      "estado": 1
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

Campos:

| Campo | Significado |
|---|---|
| `content` | Roles de la página. |
| `page` | Página actual, iniciando en 0. |
| `size` | Tamaño efectivo de página. |
| `totalElements` | Total de Roles. |
| `totalPages` | Total de páginas. |
| `first` | Indica si es la primera página. |
| `last` | Indica si es la última página. |

El controller limita `size` a 100 cuando se solicita un valor superior. Por ejemplo, `size=150` se procesa con tamaño efectivo 100. El repositorio ordena por `nombre` ascendente.

Filtros remotos opcionales:

- `q=admin`: encuentra Roles cuyo nombre contiene `admin`, sin distinguir mayúsculas/minúsculas.
- `estado=1`: devuelve solo Roles activos.
- `estado=0`: devuelve solo Roles inactivos.
- `q=admin&estado=1`: aplica ambos criterios con `AND`.
- `q=%20%20`: equivale a no enviar `q`.

`estado=2` y cualquier valor distinto de `0` o `1` devuelven `400 VALIDATION_ERROR` con `application/problem+json`.

Casos:

- `page=0&size=20`: primera página.
- `page=0&size=1`: tamaño pequeño.
- `page=0&size=100`: tamaño máximo efectivo.
- `page=0&size=150`: se limita a 100.
- una página posterior sin resultados: `200` con `content: []`.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
["content", "page", "size", "totalElements", "totalPages", "first", "last"].forEach(function (field) {
    pm.expect(body).to.have.property(field);
});
pm.expect(body.content).to.be.an("array");
```

## 14. Actualizar nombre del Rol

### Solicitud

```http
PUT {{baseUrl}}/api/v1/roles/{{codrUno}}
```

Headers:

```text
Content-Type: application/json
Accept: application/json
```

Body:

```json
{
  "nombre": " supervisor "
}
```

El resultado es `200 OK`:

```json
{
  "codr": 1,
  "nombre": "SUPERVISOR",
  "estado": 1
}
```

El PUT:

- modifica únicamente `nombre`;
- no modifica `codr`;
- no modifica `estado`;
- normaliza con trim y mayúsculas;
- no acepta un campo de cambio de estado en el DTO;
- responde `409 CONFLICT` si el nombre ya pertenece a otro Rol;
- responde `404 RESOURCE_NOT_FOUND` si el Rol no existe.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
pm.expect(body.nombre).to.eql("SUPERVISOR");
pm.expect(body.estado).to.eql(1);
pm.expect(body).to.not.have.property("password");
pm.expect(body).to.not.have.property("passwd");
```

## 15. Activar Rol

### Solicitud

```http
PATCH {{baseUrl}}/api/v1/roles/{{codrUno}}/activar
```

Header:

```text
Accept: application/json
```

No requiere body. Responde `200 OK` con `RolResponse` y `estado: 1`.

La operación es idempotente: activar un Rol que ya está activo vuelve a responder correctamente y no crea otra fila ni modifica `rolusu`.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});
pm.expect(pm.response.json().estado).to.eql(1);
```

Un `codr` inexistente responde `404 RESOURCE_NOT_FOUND`.

## 16. Desactivar Rol

### Solicitud

```http
PATCH {{baseUrl}}/api/v1/roles/{{codrUno}}/desactivar
```

Header:

```text
Accept: application/json
```

No requiere body. Responde `200 OK` con `RolResponse` y `estado: 0`.

La operación es idempotente: desactivar un Rol inactivo vuelve a responder correctamente. Las asignaciones existentes de `rolusu` se conservan; solo se impiden nuevas asignaciones a ese Rol.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});
pm.expect(pm.response.json().estado).to.eql(0);
```

Un `codr` inexistente responde `404 RESOURCE_NOT_FOUND`.

## 17. Asignar Rol a Usuario

### Solicitud

```http
POST {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrUno}}
```

Header:

```text
Accept: application/json
```

No requiere body.

El servicio valida, en este orden:

1. que exista el Usuario;
2. que exista el Rol;
3. que el Rol esté activo;
4. que no exista previamente la clave `(login, codr)`.

Respuesta exitosa: `201 Created`, `Location` y `RolUsuResponse`:

```json
{
  "login": "usuario.rol.postman",
  "codr": 1,
  "nombreRol": "SUPERVISOR",
  "fechaAsignacion": "2026-08-02T20:00:00"
}
```

La fecha es generada por PostgreSQL; el valor anterior es únicamente representativo.

Header representativo:

```text
Location: http://localhost:9090/api/v1/usuarios/usuario.rol.postman/roles/1
```

Script:

```javascript
pm.test("status 201", function () {
    pm.response.to.have.status(201);
});

pm.test("existe Location", function () {
    pm.expect(pm.response.headers.get("Location")).to.be.a("string").and.not.empty;
});

const body = pm.response.json();
pm.expect(body.login).to.eql(pm.environment.get("loginRol"));
pm.expect(body.codr).to.eql(Number(pm.environment.get("codrUno")));
pm.expect(body.nombreRol).to.be.a("string");
pm.expect(body.fechaAsignacion).to.be.a("string");
pm.expect(body).to.not.have.property("passwd");
pm.expect(body).to.not.have.property("password");
pm.expect(body).to.not.have.property("persona");
```

Un Usuario inactivo conserva sus Roles y puede recibir asignaciones administrativas. Un Rol inactivo no acepta nuevas asignaciones.

## 18. Retirar Rol de Usuario

### Solicitud

```http
DELETE {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrUno}}
```

Header:

```text
Accept: application/json
```

No requiere body.

La operación elimina únicamente la fila correspondiente de `rolusu`. No elimina el Usuario ni el Rol.

Respuesta exitosa: `204 No Content` sin body.

Script:

```javascript
pm.test("status 204", function () {
    pm.response.to.have.status(204);
});

pm.test("sin body", function () {
    pm.expect(pm.response.text()).to.eql("");
});
```

Casos de error:

- Usuario inexistente: `404 RESOURCE_NOT_FOUND`.
- Rol inexistente: `404 RESOURCE_NOT_FOUND`.
- Asignación inexistente: `404 RESOURCE_NOT_FOUND`.
- Repetir el retiro: `404 RESOURCE_NOT_FOUND`.

## 19. Listar Roles de Usuario

### Solicitud

```http
GET {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles
```

Header:

```text
Accept: application/json
```

Usuario con asignaciones:

```json
[
  {
    "login": "usuario.rol.postman",
    "codr": 1,
    "nombreRol": "SUPERVISOR",
    "fechaAsignacion": "2026-08-02T20:00:00"
  }
]
```

Usuario existente sin asignaciones: `200 OK` con `[]`.

Las filas se ordenan por `fechaAsignacion` ascendente. La respuesta no contiene `passwd`, hash, Persona, CI, correo, teléfono ni foto.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
pm.expect(body).to.be.an("array");
body.forEach(function (item) {
    pm.expect(item).to.have.all.keys("login", "codr", "nombreRol", "fechaAsignacion");
    pm.expect(item).to.not.have.property("passwd");
    pm.expect(item).to.not.have.property("persona");
});
```

Usuario inexistente: `404 RESOURCE_NOT_FOUND` con `detail: "Usuario no encontrado."`.

## 20. Listar Usuarios de Rol

### Solicitud

```http
GET {{baseUrl}}/api/v1/roles/{{codrUno}}/usuarios
```

Header:

```text
Accept: application/json
```

Rol con Usuarios:

```json
[
  {
    "login": "usuario.rol.postman",
    "codr": 1,
    "nombreRol": "SUPERVISOR",
    "fechaAsignacion": "2026-08-02T20:00:00"
  }
]
```

Rol existente sin Usuarios: `200 OK` con `[]`.

Las filas se ordenan por `fechaAsignacion` ascendente. Solo se devuelven `login`, `codr`, `nombreRol` y `fechaAsignacion`.

No se devuelven `password`, `passwd`, hashes, Persona, CI, correo, teléfono ni foto.

Script:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();
pm.expect(body).to.be.an("array");
body.forEach(function (item) {
    pm.expect(item).to.have.all.keys("login", "codr", "nombreRol", "fechaAsignacion");
});
```

Rol inexistente: `404 RESOURCE_NOT_FOUND` con `detail: "Rol no encontrado."`.

## 21. Nombre duplicado

Cree el primer Rol con `ADMINISTRADOR`. Luego repita:

```http
POST {{baseUrl}}/api/v1/roles
```

```json
{
  "nombre": " administrador "
}
```

El servicio normaliza el valor antes de consultar la unicidad. Resultado:

- HTTP `409 Conflict`;
- `errorCode: CONFLICT`;
- `title: "Conflicto"`;
- `detail: "El nombre del Rol ya está registrado."`.

No se expone `uk_roles_nombre` ni SQL.

Script:

```javascript
pm.test("nombre duplicado devuelve 409", function () {
    pm.response.to.have.status(409);
});
pm.expect(pm.response.json().errorCode).to.eql("CONFLICT");
```

## 22. Actualización a nombre duplicado

Cree dos Roles y guarde sus identificadores en `codrUno` y `codrDos`. Luego ejecute:

```http
PUT {{baseUrl}}/api/v1/roles/{{codrDos}}
```

```json
{
  "nombre": " administrador "
}
```

Resultado:

- HTTP `409 Conflict`;
- `errorCode: CONFLICT`;
- el nombre del segundo Rol no cambia;
- el estado del segundo Rol no cambia.

## 23. Rol inexistente

Use `999999` como identificador que no exista:

| Operación | Ruta | Resultado |
|---|---|---|
| GET | `/api/v1/roles/999999` | `404 RESOURCE_NOT_FOUND` |
| PUT | `/api/v1/roles/999999` | `404 RESOURCE_NOT_FOUND` |
| PATCH activar | `/api/v1/roles/999999/activar` | `404 RESOURCE_NOT_FOUND` |
| PATCH desactivar | `/api/v1/roles/999999/desactivar` | `404 RESOURCE_NOT_FOUND` |
| DELETE asignación | `/api/v1/usuarios/{{loginRol}}/roles/999999` | `404 RESOURCE_NOT_FOUND` |
| GET Usuarios | `/api/v1/roles/999999/usuarios` | `404 RESOURCE_NOT_FOUND` |

El `detail` de Rol inexistente es `Rol no encontrado.`.

## 24. Usuario inexistente

Use `usuario-no-existe-rol`:

| Operación | Ruta | Resultado |
|---|---|---|
| POST asignación | `/api/v1/usuarios/usuario-no-existe-rol/roles/{{codrUno}}` | `404 RESOURCE_NOT_FOUND` |
| DELETE asignación | `/api/v1/usuarios/usuario-no-existe-rol/roles/{{codrUno}}` | `404 RESOURCE_NOT_FOUND` |
| GET Roles | `/api/v1/usuarios/usuario-no-existe-rol/roles` | `404 RESOURCE_NOT_FOUND` |

El `detail` es `Usuario no encontrado.`. En una asignación, el servicio busca primero el Usuario y después el Rol.

## 25. Asignación duplicada

1. Ejecute correctamente:

   ```http
   POST {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrUno}}
   ```

2. Repita exactamente la misma solicitud.

Resultado de la segunda solicitud:

```json
{
  "type": "about:blank",
  "title": "Conflicto",
  "status": 409,
  "detail": "El Rol ya está asignado al Usuario.",
  "instance": "/api/v1/usuarios/usuario.rol.postman/roles/1",
  "errorCode": "CONFLICT",
  "timestamp": "2026-08-02T20:00:00Z",
  "traceId": "valor-generado-por-el-backend"
}
```

`timestamp` y `traceId` son variables. No se expone `pk_rolusu`.

## 26. Asignación inexistente

Ejecute:

```http
DELETE {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrDos}}
```

cuando esa combinación no esté asignada.

Resultado:

- HTTP `404 Not Found`;
- `errorCode: RESOURCE_NOT_FOUND`;
- `detail: "La asignación de Rol no existe."`.

El mismo resultado ocurre al repetir un retiro que ya respondió `204`.

## 27. Rol inactivo al asignar

1. Desactive el segundo Rol:

   ```http
   PATCH {{baseUrl}}/api/v1/roles/{{codrDos}}/desactivar
   ```

2. Intente asignarlo:

   ```http
   POST {{baseUrl}}/api/v1/usuarios/{{loginRol}}/roles/{{codrDos}}
   ```

Resultado real:

- HTTP `422 Unprocessable Content`;
- `errorCode: BUSINESS_RULE_VIOLATION`;
- no se crea la fila de `rolusu`.

ProblemDetail representativo:

```json
{
  "type": "about:blank",
  "title": "Regla de negocio no cumplida",
  "status": 422,
  "detail": "No se puede asignar un Rol inactivo.",
  "instance": "/api/v1/usuarios/usuario.rol.postman/roles/2",
  "errorCode": "BUSINESS_RULE_VIOLATION",
  "timestamp": "2026-08-02T20:00:00Z",
  "traceId": "valor-generado-por-el-backend"
}
```

Las asignaciones previas del Rol permanecen. Reactive el Rol para permitir nuevas asignaciones.

## 28. Validaciones de CreateRolRequest

Todas las solicitudes inválidas esperan HTTP `400`, `errorCode: VALIDATION_ERROR`, `detail: "Uno o más campos no son válidos."` y `fieldErrors`.

| Caso | Body o cambio | Resultado esperado |
|---|---|---|
| Nombre omitido | `{}` | `fieldErrors.nombre`; obligatorio. |
| Nombre null | `{"nombre":null}` | `fieldErrors.nombre`; obligatorio. |
| Nombre vacío | `{"nombre":""}` | `fieldErrors.nombre`; obligatorio. |
| Solo espacios | `{"nombre":"   "}` | `fieldErrors.nombre`; obligatorio. |
| 50 caracteres | nombre de 50 caracteres | Aceptado si no está duplicado. |
| 51 caracteres | nombre de 51 caracteres | `fieldErrors.nombre`; máximo 50. |
| Estado omitido | `{"nombre":"NUEVO"}` | Aceptado; PostgreSQL aplica 1. |
| Estado null | `{"nombre":"NUEVO","estado":null}` | Aceptado; PostgreSQL aplica 1. |
| Estado 0 | `{"nombre":"NUEVO","estado":0}` | Aceptado. |
| Estado 1 | `{"nombre":"NUEVO","estado":1}` | Aceptado. |
| Estado 2 | `{"nombre":"NUEVO","estado":2}` | `fieldErrors.estado`; debe ser 0 o 1. |
| Estado -1 | `{"nombre":"NUEVO","estado":-1}` | `fieldErrors.estado`; debe ser 0 o 1. |

La anotación `@NotBlank` se aplica a `nombre`; `@Size(max=50)` limita longitud; `@Min(0)` y `@Max(1)` validan `estado` cuando no es null.

## 29. Validaciones de UpdateRolRequest

`UpdateRolRequest` contiene únicamente `nombre`:

| Caso | Body o cambio | Resultado esperado |
|---|---|---|
| Nombre válido | `{"nombre":"SUPERVISOR"}` | `200` si el Rol existe y no duplica. |
| Nombre con espacios | `{"nombre":" supervisor "}` | `200`; se normaliza. |
| Nombre omitido | `{}` | `400 VALIDATION_ERROR`; `fieldErrors.nombre`. |
| Nombre null | `{"nombre":null}` | `400 VALIDATION_ERROR`; `fieldErrors.nombre`. |
| Nombre vacío | `{"nombre":""}` | `400 VALIDATION_ERROR`; `fieldErrors.nombre`. |
| Solo espacios | `{"nombre":"   "}` | `400 VALIDATION_ERROR`; `fieldErrors.nombre`. |
| Mayor a 50 | 51 caracteres | `400 VALIDATION_ERROR`; `fieldErrors.nombre`. |
| Nombre duplicado | nombre de otro Rol | `409 CONFLICT`. |

No existe `estado` en este DTO. El estado se modifica exclusivamente con PATCH activar/desactivar.

## 30. JSON mal formado

En una solicitud que requiera body, pruebe:

```json
{"nombre":"OPERADOR" "estado":1}
```

```json
{"nombre":"OPERADOR"
```

```json
{"nombre":"OPERADOR",}
```

El manejador global responde `400 INVALID_REQUEST`:

```json
{
  "type": "about:blank",
  "title": "Solicitud no válida",
  "status": 400,
  "detail": "El cuerpo de la solicitud no es válido.",
  "instance": "/api/v1/roles",
  "errorCode": "INVALID_REQUEST",
  "timestamp": "2026-08-02T20:00:00Z",
  "traceId": "valor-generado-por-el-backend"
}
```

No se expone la excepción de Jackson ni un stack trace.

## 31. Tipos de datos incorrectos

| Solicitud | Ejemplo | Resultado real esperado |
|---|---|---|
| Estado como texto | `{"nombre":"ROL","estado":"1"}` | `400 INVALID_REQUEST`. |
| Estado decimal | `{"nombre":"ROL","estado":1.5}` | `400 INVALID_REQUEST`. |
| Body como arreglo | `[]` | `400 INVALID_REQUEST`. |
| Body como texto | `"ROL"` | `400 INVALID_REQUEST`. |
| `codr` no numérico | `/api/v1/roles/abc` | Error de conversión tratado actualmente como `500 INTERNAL_ERROR`. |

Use valores numéricos para `codr` y `estado`. Un `codr` numérico inexistente sí permite probar el `404` específico.

## 32. ProblemDetail

El manejador global usa `application/problem+json` y produce estas propiedades:

- `type`: normalmente `about:blank`;
- `title`: título seguro;
- `status`: código HTTP;
- `detail`: detalle funcional;
- `instance`: ruta solicitada;
- `errorCode`: código estable;
- `timestamp`: instante generado;
- `traceId`: identificador de trazabilidad;
- `fieldErrors`: lista de `{field, message}` solo en validaciones.

Ejemplo de validación:

```json
{
  "type": "about:blank",
  "title": "Solicitud no válida",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "instance": "/api/v1/roles",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-08-02T20:00:00Z",
  "traceId": "valor-generado-por-el-backend",
  "fieldErrors": [
    {
      "field": "nombre",
      "message": "El nombre del Rol es obligatorio."
    }
  ]
}
```

Resumen:

| Situación | HTTP | `errorCode` |
|---|---:|---|
| DTO inválido | 400 | `VALIDATION_ERROR` |
| JSON o tipo ilegible | 400 | `INVALID_REQUEST` |
| Recurso inexistente | 404 | `RESOURCE_NOT_FOUND` |
| Nombre/asignación duplicada | 409 | `CONFLICT` |
| Rol inactivo al asignar | 422 | `BUSINESS_RULE_VIOLATION` |
| Método no permitido | 405 | `INVALID_REQUEST` |

No se exponen SQL, nombres de constraints ni stack traces.

## 33. Seguridad de las respuestas

Las respuestas de Rol no deben contener contraseñas ni datos de Usuario. Las respuestas de `RolUsu` solo contienen:

- `login`;
- `codr`;
- `nombreRol`;
- `fechaAsignacion`.

No deben aparecer:

- `password`;
- `passwd`;
- `password_hash`;
- `hash`;
- texto BCrypt;
- Persona completa;
- CI;
- correo;
- teléfono;
- foto.

Script reutilizable para respuestas JSON:

```javascript
pm.test("respuesta sin datos sensibles", function () {
    const serialized = JSON.stringify(pm.response.json()).toLowerCase();
    ["password", "passwd", "password_hash", "hash", "bcrypt", "persona", "ci", "correo", "telefono", "foto"]
        .forEach(function (field) {
            pm.expect(serialized).to.not.include(field);
        });
});
```

En `204 No Content`, compruebe únicamente que el body esté vacío.

## 34. Scripts de Postman

Scripts comunes para solicitudes de Roles:

```javascript
pm.test("Content-Type correcto cuando hay respuesta JSON", function () {
    if (pm.response.code !== 204) {
        pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
    }
});
```

Script para errores:

```javascript
pm.test("ProblemDetail contiene contrato base", function () {
    const body = pm.response.json();
    pm.expect(body).to.have.property("type");
    pm.expect(body).to.have.property("title");
    pm.expect(body).to.have.property("status");
    pm.expect(body).to.have.property("detail");
    pm.expect(body).to.have.property("instance");
    pm.expect(body).to.have.property("errorCode");
    pm.expect(body).to.have.property("timestamp");
    pm.expect(body).to.have.property("traceId");
});
```

Script para validar el arreglo de asignaciones:

```javascript
pm.test("asignaciones son un arreglo", function () {
    const body = pm.response.json();
    pm.expect(body).to.be.an("array");
    body.forEach(function (item) {
        pm.expect(item).to.have.all.keys("login", "codr", "nombreRol", "fechaAsignacion");
    });
});
```

Los scripts no deben registrar contraseñas, hashes, tokens ni bodies completos en la consola.

## 35. Orden recomendado de pruebas

1. Crear Persona ficticia.
2. Crear Usuario con login `{{loginRol}}`.
3. Crear Rol A y guardar `codrUno`.
4. Crear Rol B y guardar `codrDos`.
5. Consultar Rol A.
6. Listar Roles con paginación.
7. Actualizar el nombre del Rol A.
8. Asignar Rol A al Usuario.
9. Listar Roles de `{{loginRol}}`.
10. Listar Usuarios de `{{codrUno}}`.
11. Repetir la asignación y comprobar `409`.
12. Desactivar Rol B.
13. Intentar asignar Rol B inactivo y comprobar `422`.
14. Reactivar Rol B.
15. Asignar Rol B.
16. Desactivar el Usuario mediante la guía de Usuario y comprobar que conserva sus Roles.
17. Desactivar Rol A y comprobar que conserva sus asignaciones.
18. Retirar Rol A.
19. Repetir el retiro y comprobar `404`.
20. Ejecutar validaciones de `CreateRolRequest`.
21. Ejecutar validaciones de `UpdateRolRequest`.
22. Ejecutar errores de inexistencia, JSON y tipos.
23. Confirmar ausencia de datos sensibles.

## 36. Matriz completa de casos

| ID | Endpoint | Escenario | Datos | HTTP | `errorCode` | Comprobación |
|---|---|---|---|---:|---|---|
| ROL-POST-001 | POST `/roles` | Crear sin estado | nombre con espacios | 201 | — | trim, mayúsculas, Location, `codrUno` |
| ROL-POST-002 | POST `/roles` | Crear con estado | estado 1 | 201 | — | `RolResponse` |
| ROL-GET-001 | GET `/roles/{codr}` | Rol existente | `codrUno` | 200 | — | campos reales |
| ROL-GET-002 | GET `/roles/{codr}` | Rol inexistente | 999999 | 404 | RESOURCE_NOT_FOUND | ProblemDetail |
| ROL-LIST-001 | GET `/roles` | Primera página | page 0, size 20 | 200 | — | `PageResponse` |
| ROL-LIST-002 | GET `/roles` | Tamaño superior | size 150 | 200 | — | controller limita a 100 |
| ROL-PUT-001 | PUT `/roles/{codr}` | Actualizar nombre | nombre nuevo | 200 | — | estado y codr intactos |
| ROL-PUT-002 | PUT `/roles/{codr}` | Nombre duplicado | nombre existente | 409 | CONFLICT | no cambia Rol |
| ROL-PATCH-ACT-001 | PATCH `/roles/{codr}/activar` | Activar | `codrUno` | 200 | — | idempotencia |
| ROL-PATCH-DES-001 | PATCH `/roles/{codr}/desactivar` | Desactivar | `codrUno` | 200 | — | asignaciones conservadas |
| ROLUSU-POST-001 | POST `/usuarios/{login}/roles/{codr}` | Asignar | `loginRol`, `codrUno` | 201 | — | Location y fecha |
| ROLUSU-POST-002 | POST `/usuarios/{login}/roles/{codr}` | Usuario inexistente | login ficticio | 404 | RESOURCE_NOT_FOUND | usuario |
| ROLUSU-POST-003 | POST `/usuarios/{login}/roles/{codr}` | Rol inactivo | `codrDos` desactivado | 422 | BUSINESS_RULE_VIOLATION | no crea relación |
| ROLUSU-POST-004 | POST `/usuarios/{login}/roles/{codr}` | Duplicada | repetir POST | 409 | CONFLICT | no constraint interno |
| ROLUSU-DELETE-001 | DELETE `/usuarios/{login}/roles/{codr}` | Retiro correcto | relación existente | 204 | — | body vacío |
| ROLUSU-DELETE-002 | DELETE `/usuarios/{login}/roles/{codr}` | Retiro inexistente | relación ausente | 404 | RESOURCE_NOT_FOUND | asignación |
| ROLUSU-GET-001 | GET `/usuarios/{login}/roles` | Usuario con Roles | `loginRol` | 200 | — | arreglo y campos |
| ROLUSU-GET-002 | GET `/usuarios/{login}/roles` | Usuario sin Roles | usuario existente | 200 | — | `[]` |
| ROLUSU-GET-003 | GET `/roles/{codr}/usuarios` | Rol con Usuarios | `codrUno` | 200 | — | arreglo mínimo |
| ROLUSU-GET-004 | GET `/roles/{codr}/usuarios` | Rol sin Usuarios | Rol existente | 200 | — | `[]` |
| ROL-VAL-001 | POST `/roles` | Nombre omitido | `{}` | 400 | VALIDATION_ERROR | `fieldErrors.nombre` |
| ROL-VAL-002 | POST `/roles` | Estado inválido | estado 2 | 400 | VALIDATION_ERROR | `fieldErrors.estado` |
| ROL-VAL-003 | PUT `/roles/{codr}` | Nombre >50 | 51 caracteres | 400 | VALIDATION_ERROR | `fieldErrors.nombre` |
| ROL-ERR-001 | POST `/roles` | JSON mal formado | llave/coma inválida | 400 | INVALID_REQUEST | sin stack trace |
| ROL-ERR-002 | GET `/roles/abc` | `codr` no numérico | texto | 500 | INTERNAL_ERROR | conversión genérica actual |
| ROL-SEC-001 | respuestas | Campos sensibles | todas las respuestas JSON | — | — | no hay password/hash/Persona |

## 37. Evidencia de pruebas manuales

Para cada caso manual, registre:

- ID de la matriz;
- fecha y hora;
- método y URL;
- body ficticio, sin contraseñas reales;
- status observado;
- `errorCode` observado;
- resultado esperado;
- resultado observado;
- captura sin información sensible.

La creación de Roles y Usuarios deja datos persistentes si se ejecuta contra una base compartida. Use una base de pruebas controlada y no elimine manualmente tablas ni `flyway_schema_history`.

## 38. Errores frecuentes

- Backend detenido: compruebe `http://localhost:9090`.
- Variables `DB_*` ausentes: el problema ocurre al arrancar, no en Postman.
- PostgreSQL detenido: Flyway o Hibernate no podrán iniciar.
- No seleccionar el entorno `ORMAN Local`: las variables aparecen sin resolver.
- Usar otra variable para el login: este flujo requiere `{{loginRol}}`.
- Usar `codper` como `codr`: Persona, Usuario y Rol usan identificadores diferentes.
- Asignar un Rol inexistente: use un `codr` creado y guardado.
- Intentar asignar un Rol inactivo: reactive el Rol antes de una nueva asignación.
- Enviar body en POST de asignación: esa ruta no requiere body.
- Esperar `200` al retirar: el resultado correcto es `204` sin body.
- Esperar DELETE físico de Rol: esa operación no está expuesta.
- Repetir una asignación: la segunda solicitud produce `409`.
- Repetir un retiro: el segundo retiro produce `404`.
- Compartir capturas con passwords, hashes o datos personales.

## 39. Limitaciones actuales

- No existe alcance por propiedad, ciudad o sucursal.
- No existe eliminación física de Roles.
- Propiedades, permisos dinámicos, menús, procesos y OTP pertenecen a fases posteriores.

Un Rol inactivo conserva sus asignaciones, pero no concede authority en la siguiente petición. El Rol exacto `PROPIETARIO` está protegido por la regla del último propietario.

## 40. Checklist final

- [ ] PostgreSQL está activo.
- [ ] Las variables `DB_*` están disponibles para el backend.
- [ ] El backend escucha en `http://localhost:9090`.
- [ ] Flyway validó V1, V2, V3, V4 y V5.
- [ ] Se seleccionó el entorno `ORMAN Local`.
- [ ] `baseUrl` apunta a `http://localhost:9090`.
- [ ] `loginRol` contiene un Usuario real de pruebas.
- [ ] Persona y Usuario fueron preparados.
- [ ] Se creó Rol A y se guardó `codrUno`.
- [ ] Se creó Rol B y se guardó `codrDos`.
- [ ] Crear Rol devolvió `201` y `Location`.
- [ ] El nombre se normalizó a mayúsculas.
- [ ] El listado devolvió `PageResponse` correcto.
- [ ] `size=150` se limitó a 100.
- [ ] PUT modificó solo nombre.
- [ ] Activar y desactivar fueron idempotentes.
- [ ] Se asignó un Rol activo.
- [ ] La asignación devolvió `201` y `Location`.
- [ ] Se listaron Roles de Usuario.
- [ ] Se listaron Usuarios de Rol.
- [ ] La asignación duplicada devolvió `409 CONFLICT`.
- [ ] El Rol inactivo devolvió `422 BUSINESS_RULE_VIOLATION` al asignar.
- [ ] Las asignaciones se conservaron al desactivar Usuario o Rol.
- [ ] El retiro devolvió `204` sin body.
- [ ] El retiro repetido devolvió `404`.
- [ ] Se probaron nombres de 50 y 51 caracteres.
- [ ] Se probaron estados omitido, null, 0, 1, 2 y -1.
- [ ] Se probó JSON mal formado.
- [ ] Se probaron tipos incorrectos.
- [ ] Se revisaron respuestas `ProblemDetail`.
- [ ] Ninguna respuesta contiene `password`, `passwd`, `password_hash`, `hash` o BCrypt.
- [ ] Ninguna respuesta de asignación contiene Persona, CI, correo, teléfono o foto.
- [ ] PROPIETARIO administró un Rol común.
- [ ] ADMINISTRADOR e INQUILINO recibieron `403 ACCESS_DENIED`.
- [ ] Renombrar o desactivar PROPIETARIO devolvió `409 LAST_OWNER_REQUIRED`.

## Autorización de Fase 11.2

Todas las operaciones de Roles requieren `ROLE_PROPIETARIO`. ADMINISTRADOR, INQUILINO y Usuario sin Rol reciben 403. El Rol exacto `PROPIETARIO` está reservado: no puede renombrarse ni desactivarse. No existe endpoint de eliminación física y no se añadió.

Las asignaciones se documentan por separado en [Guía Postman Usuario–Rol](rolusu.md).
