# API Usuario — Guía completa de pruebas en Postman

## 1. Objetivo de la guía

Esta guía permite probar manualmente, desde cero, toda la administración de Usuarios implementada en la Fase 07 de ORMAN-BACKEND: creación, consulta, listado paginado, actualización administrativa de estado, activación, desactivación y cambio de contraseña.

La API recibe `password` únicamente en las solicitudes autorizadas. El backend la transforma inmediatamente a BCrypt y la persiste en `usuarios.passwd` como hash. Ninguna respuesta de la API devuelve `password`, `passwd` ni el hash.

La autenticación JWT, las sesiones y la autorización por Roles ya están implementadas. Esta guía conserva las pruebas CRUD de Usuario y añade al final la matriz de Fase 11.2. Para obtener un `Authorization: Bearer` consulte [auth.md](auth.md); no incluya Roles dentro del JWT.

## 2. Requisitos previos

Antes de abrir la colección:

1. Inicie PostgreSQL.
2. Asegúrese de que las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` estén configuradas en la terminal que usará para iniciar el backend. No copie sus valores en Postman, esta guía ni capturas.
3. Ubíquese en el proyecto `C:\Users\Asus\Pictures\Orman\Backend\backend` o en la ruta equivalente de su instalación.
4. Compruebe que existe al menos una Persona sin Usuario. Puede crearla mediante la API de Persona o usar una Persona ficticia existente.
5. El backend debe arrancar en el puerto `9090`.
6. Flyway debe validar las migraciones V1, V2 y V3. La Fase 07 no crea V4.
7. Hibernate debe continuar configurado con `ddl-auto=validate`.

El esquema esperado contiene `flyway_schema_history`, `personas` y `usuarios`. No es necesario consultar ni modificar directamente la base de datos para probar esta guía.

## 3. Iniciar el backend

En una terminal con las variables `DB_*` disponibles, ejecute desde el proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

No muestre los valores de las variables en capturas. Espere a que la aplicación informe que inició correctamente y que PostgreSQL fue conectado.

La dirección base es:

```text
http://localhost:9090
```

Puede comprobar que el servidor escucha usando una petición válida de listado:

```text
GET http://localhost:9090/api/v1/usuarios?page=0&size=20
```

Si devuelve una respuesta HTTP, el servidor está atendiendo. Una respuesta `200` con `content` vacío también es válida si todavía no hay Usuarios.

## 4. Crear el entorno de Postman

En Postman:

1. Abra el menú de entornos y cree uno llamado `ORMAN Local`.
2. Agregue estas variables:

| Variable | Initial value | Current value |
|---|---|---|
| `baseUrl` | `http://localhost:9090` | `http://localhost:9090` |
| `personaId` | `1` | `1` |
| `personaIdDos` | `2` | `2` |
| `usuarioLogin` | `usuario.demo` | `usuario.demo` |
| `usuarioLoginDos` | `usuario.segundo` | `usuario.segundo` |

`Initial value` sirve como valor de referencia o compartible según la configuración de Postman. `Current value` es el valor local que realmente utiliza la ejecución. Para esta guía use valores ficticios en ambos y no guarde credenciales de PostgreSQL.

Seleccione `ORMAN Local` en el selector de entorno. Si no lo selecciona, `{{baseUrl}}` y las demás variables no se resolverán.

## 5. URL base

Todas las rutas de esta guía parten de:

```text
{{baseUrl}}/api/v1/usuarios
```

Por ejemplo:

```text
{{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}
```

## 6. Encabezados generales

Para todas las solicitudes que esperan JSON:

```text
Accept: application/json
```

Para `POST` y `PUT` con body:

```text
Content-Type: application/json
Accept: application/json
```

Las solicitudes `GET` no necesitan body. Los `PATCH` de activar y desactivar tampoco necesitan body ni `Content-Type`.

Los errores se devuelven como `application/problem+json`, aunque se mantiene `Accept: application/json` en las solicitudes.

## 7. Preparación de datos

Antes de crear un Usuario debe existir una Persona sin Usuario. Hay dos formas de prepararla:

1. Crear una Persona ficticia mediante la API existente. Consulte [docs/postman/persona.md](persona.md) y guarde su `codper` en `personaId`.
2. Usar una Persona ficticia ya existente que no esté vinculada a ningún Usuario. Coloque su `codper` en `personaId`.

La relación es uno a uno:

- `codper` debe corresponder a una Persona existente;
- una Persona puede tener cero o un Usuario;
- intentar agregar un segundo Usuario a la misma Persona produce `409 CONFLICT`;
- usar otro login es obligatorio para probar el conflicto de Persona y no el conflicto de login.

Para probar dos cuentas, prepare una segunda Persona sin Usuario y guarde su identificador en `personaIdDos`.

## Resumen completo de endpoints

| Método | Ruta | Operación | Auth/regla | Body | Éxito | Errores principales |
|---|---|---|---|---|---:|---|
| POST | `/api/v1/usuarios` | [Crear Usuario](#9-crear-usuario-correctamente) | PROPIETARIO o ADMINISTRADOR sobre Usuario común | Sí | 201 | 400, 401, 403, 404, 409 |
| GET | `/api/v1/usuarios/{login}` | [Consultar Usuario](#11-consultar-usuario-por-login) | PROPIETARIO o ADMINISTRADOR sobre Usuario común | No | 200 | 401, 403, 404 |
| GET | `/api/v1/usuarios` | [Listar Usuarios](#12-listar-usuarios-con-paginación) | PROPIETARIO o ADMINISTRADOR; ADMINISTRADOR ve solo comunes; `page,size,sort` | No | 200 | 401, 403 |
| PUT | `/api/v1/usuarios/{login}` | [Modificar estado](#13-actualización-administrativa) | PROPIETARIO o ADMINISTRADOR sobre Usuario común | Sí | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/usuarios/{login}/desactivar` | [Desactivar Usuario](#14-desactivar-usuario) | PROPIETARIO o ADMINISTRADOR sobre Usuario común; protege último propietario | No | 200 | 401, 403, 404, 409 |
| PATCH | `/api/v1/usuarios/{login}/activar` | [Activar Usuario](#15-activar-usuario) | PROPIETARIO o ADMINISTRADOR sobre Usuario común | No | 200 | 401, 403, 404 |
| PUT | `/api/v1/usuarios/{login}/password` | [Cambiar contraseña](#16-cambiar-o-restablecer-contraseña) | Usuario sobre sí mismo o PROPIETARIO sobre cualquier Usuario | Sí | 204 | 400, 401, 403, 404 |

Los endpoints de login, refresh y sesiones están documentados en [auth.md](auth.md); no existe `DELETE`, `/me` ni otro endpoint de Usuario fuera de esta tabla.

## 9. Crear Usuario correctamente

### Solicitud

```text
POST {{baseUrl}}/api/v1/usuarios
```

Headers:

```text
Accept: application/json
Content-Type: application/json
```

Body válido con estado omitido:

```json
{
  "login": "usuario.demo",
  "password": "Temporal-Prueba-2026",
  "codper": {{personaId}}
}
```

Body válido con estado explícito:

```json
{
  "login": "usuario.segundo",
  "password": "Temporal-Prueba-2026",
  "estado": 1,
  "codper": {{personaIdDos}}
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---|---:|---|
| `login` | string | Sí | No blanco, máximo 30 caracteres; se aplica `trim`. |
| `password` | string | Sí | No blanco, entre 8 y 72 caracteres; no se aplica `trim`. |
| `estado` | número | No | Solo 0 o 1; si se omite, PostgreSQL aplica 1. |
| `codper` | número | Sí | Positivo y correspondiente a una Persona existente. |

No envíe `passwd`, `hash`, `fechaCreacion`, `ultimoAcceso` ni una entidad Persona. El request recibe `password`; la columna de base de datos `passwd` contiene únicamente el hash BCrypt.

### Respuesta esperada

Status: `201 Created`.

Header esperado:

```text
Location: http://localhost:9090/api/v1/usuarios/usuario.demo
```

El login usado puede variar según el request. La respuesta tiene esta forma:

```json
{
  "login": "usuario.demo",
  "estado": 1,
  "codper": 1,
  "fechaCreacion": "2026-08-02T11:30:00",
  "ultimoAcceso": null
}
```

La fecha es generada por PostgreSQL y su valor exacto variará. No deben aparecer `password`, `passwd`, `hash` ni BCrypt.

## 10. Prueba automática de Postman para creación

En la pestaña `Tests` de la petición de creación, use:

```javascript
pm.test("status 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Content-Type JSON", function () {
    pm.expect(pm.response.headers.get("Content-Type")).to.include("application/json");
});

const body = pm.response.json();

pm.test("respuesta contiene campos de Usuario", function () {
    pm.expect(body).to.have.property("login");
    pm.expect(body).to.have.property("estado");
    pm.expect(body).to.have.property("codper");
    pm.expect(body).to.have.property("fechaCreacion");
});

pm.test("respuesta no contiene datos sensibles", function () {
    pm.expect(body).to.not.have.property("password");
    pm.expect(body).to.not.have.property("passwd");
    pm.expect(body).to.not.have.property("hash");
});

pm.test("Location existe", function () {
    pm.expect(pm.response.headers.get("Location")).to.be.ok;
});

pm.environment.set("usuarioLogin", body.login);
```

El script no imprime la contraseña ni ningún hash.

## 11. Consultar Usuario por login

### Solicitud existente

```text
GET {{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}
```

Respuesta: `200 OK`.

```json
{
  "login": "usuario.demo",
  "estado": 1,
  "codper": 1,
  "fechaCreacion": "2026-08-02T11:30:00",
  "ultimoAcceso": null
}
```

No aparece la Persona completa ni `passwd`.

### Test de consulta

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

pm.test("login correcto", function () {
    pm.expect(body.login).to.eql(pm.environment.get("usuarioLogin"));
});

pm.test("password y passwd ausentes", function () {
    pm.expect(body).to.not.have.property("password");
    pm.expect(body).to.not.have.property("passwd");
});
```

### Usuario inexistente

```text
GET {{baseUrl}}/api/v1/usuarios/usuario-no-existe-2026
```

Respuesta: `404`, `errorCode: RESOURCE_NOT_FOUND`, `detail: "Usuario no encontrado."`.

## 12. Listar Usuarios con paginación

### Primera página

```text
GET {{baseUrl}}/api/v1/usuarios?page=0&size=20
```

El controller establece:

- `page` predeterminado: `0`;
- `size` predeterminado: `20`;
- orden predeterminado: `login` ascendente;
- tamaño máximo aplicado por el controller: `100`.

La estructura real es:

```json
{
  "content": [
    {
      "login": "usuario.demo",
      "estado": 1,
      "codper": 1,
      "fechaCreacion": "2026-08-02T11:30:00",
      "ultimoAcceso": null
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

### Casos manuales

| Caso | Solicitud | Comprobación |
|---|---|---|
| Primera página | `?page=0&size=20` | `page=0`, `content` es arreglo. |
| Página vacía | `?page=999&size=20` | `200`, `content: []`; la metadata refleja la página devuelta por Spring Data. |
| Tamaño pequeño | `?page=0&size=1` | Como máximo un elemento en `content`. |
| Tamaño máximo | `?page=0&size=100` | `size=100`, salvo que la página tenga menos elementos. |
| Superior al máximo | `?page=0&size=150` | El controller limita el tamaño a `100`. |

El controller no define una validación propia para `page` negativo ni `size` cero/negativo. No los use como contratos estables de negocio: su tratamiento depende del resolver de `Pageable` de Spring. `sort` es aceptado por el resolver, pero la solicitud sin `sort` usa `login` ascendente; esta guía no depende de órdenes alternativos.

### Tests de paginación

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const page = pm.response.json();

pm.test("content es un arreglo", function () {
    pm.expect(page.content).to.be.an("array");
});

pm.test("metadata paginada existe", function () {
    ["page", "size", "totalElements", "totalPages", "first", "last"].forEach(function (field) {
        pm.expect(page).to.have.property(field);
    });
});

pm.test("ningún elemento expone passwd", function () {
    page.content.forEach(function (item) {
        pm.expect(item).to.not.have.property("password");
        pm.expect(item).to.not.have.property("passwd");
        pm.expect(item).to.not.have.property("hash");
    });
});
```

## 13. Actualización administrativa

### Solicitud

```text
PUT {{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}
```

Headers:

```text
Content-Type: application/json
Accept: application/json
```

Body válido:

```json
{
  "estado": 0
}
```

`UpdateUsuarioRequest` admite únicamente `estado`, que es obligatorio y solo puede ser 0 o 1. La respuesta es `200 OK` con `UsuarioResponse`.

No se modifican mediante PUT:

- `login`;
- `password`;
- `passwd`;
- `fechaCreacion`;
- `ultimoAcceso`;
- `codper`.

La contraseña tiene su endpoint específico. Activar y desactivar también tienen endpoints específicos idempotentes. No existe cambio de Persona asociada en esta fase, por lo que no existe un caso de `404` o `409` por Persona nueva en este PUT.

### Test de actualización

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

pm.test("estado actualizado y respuesta segura", function () {
    pm.expect(body.estado).to.be.oneOf([0, 1]);
    pm.expect(body).to.not.have.property("password");
    pm.expect(body).to.not.have.property("passwd");
});
```

Body vacío (`{}`) produce `400 VALIDATION_ERROR` porque `estado` es obligatorio. Un Usuario inexistente produce `404 RESOURCE_NOT_FOUND`.

## 14. Desactivar Usuario

### Primera ejecución

```text
PATCH {{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}/desactivar
```

No use body. No necesita `Content-Type`. Responde `200 OK` con `estado: 0`, conserva la fila `usuarios` y no modifica el estado de Persona.

### Segunda ejecución

Repita exactamente la solicitud. También responde `200 OK` con `estado: 0`; la operación es idempotente.

Test:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Usuario queda inactivo", function () {
    pm.expect(pm.response.json().estado).to.eql(0);
});
```

Un login inexistente responde `404` con `errorCode: RESOURCE_NOT_FOUND`.

## 15. Activar Usuario

### Primera ejecución

```text
PATCH {{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}/activar
```

No use body. Responde `200 OK` con `estado: 1`, conserva el registro y no activa la Persona.

### Segunda ejecución

Repita la solicitud sobre el Usuario ya activo. Responde nuevamente `200 OK` con `estado: 1`; la operación es idempotente.

Test:

```javascript
pm.test("status 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Usuario queda activo", function () {
    pm.expect(pm.response.json().estado).to.eql(1);
});
```

Un login inexistente responde `404` con `errorCode: RESOURCE_NOT_FOUND`.

## 16. Cambiar o restablecer contraseña

### Solicitud

```text
PUT {{baseUrl}}/api/v1/usuarios/{{usuarioLogin}}/password
```

Headers:

```text
Content-Type: application/json
Accept: application/json
```

Body real de `ChangePasswordRequest`:

```json
{
  "newPassword": "Temporal-Nueva-2026"
}
```

`newPassword` es el único campo admitido por el DTO. No envíe el hash, `passwd`, `login` ni `oldPassword`. El cambio requiere autenticación y solo permite el propio `login` o un actor con `ROLE_PROPIETARIO`; además revoca las sesiones del Usuario objetivo conforme a Fase 10.

Respuesta exitosa: `204 No Content`, sin body.

```javascript
pm.test("status 204", function () {
    pm.response.to.have.status(204);
});

pm.test("no existe body de respuesta", function () {
    pm.expect(pm.response.text()).to.eql("");
});
```

Un Usuario inexistente responde `404 RESOURCE_NOT_FOUND`. `newPassword` omitida, nula, vacía, menor a 8 o mayor a 72 caracteres responde `400 VALIDATION_ERROR`.

El backend aplica BCrypt. Postman no debe intentar obtener el hash. La integración del backend verifica que el valor persistido no coincide con el texto y que `PasswordEncoder.matches` funciona; esa comprobación no se realiza exponiendo la base de datos desde esta guía.

## 17. Login duplicado

1. Ejecute la creación correcta con `usuarioLogin` y `personaId`.
2. Prepare `personaIdDos` como una Persona diferente y sin Usuario.
3. Intente crear otro Usuario con el mismo login y otra Persona:

```json
{
  "login": "usuario.demo",
  "password": "Temporal-Otra-2026",
  "codper": {{personaIdDos}}
}
```

4. Espere `409 CONFLICT`.
5. Compruebe `errorCode: CONFLICT`.

ProblemDetail representativo:

```json
{
  "type": "about:blank",
  "title": "Conflicto",
  "status": 409,
  "detail": "El login ya está registrado.",
  "instance": "/api/v1/usuarios",
  "errorCode": "CONFLICT",
  "timestamp": "2026-08-02T11:30:00Z",
  "traceId": "valor-generado-por-el-backend"
}
```

El `traceId` y `timestamp` cambian en cada respuesta. La respuesta no expone SQL, nombres de constraints, `password`, `passwd` ni hash.

## 18. Persona inexistente

Use un identificador que no exista, por ejemplo `99999999`:

```text
POST {{baseUrl}}/api/v1/usuarios
```

```json
{
  "login": "usuario.sin-persona",
  "password": "Temporal-Sin-Persona-2026",
  "codper": 99999999
}
```

Resultado: `404`, `errorCode: RESOURCE_NOT_FOUND`, `detail: "Persona no encontrada."`. También aparecen `instance`, `timestamp` y `traceId`; no aparecen SQL ni datos sensibles.

## 19. Persona que ya tiene Usuario

1. Cree un Usuario para `personaId` con `usuarioLogin`.
2. Use el mismo `codper`, pero un login diferente:

```json
{
  "login": "usuario.duplicado-persona",
  "password": "Temporal-Segundo-2026",
  "codper": {{personaId}}
}
```

3. Espere `409 CONFLICT` y `errorCode: CONFLICT`.
4. Compruebe que el `detail` seguro es `La Persona ya tiene un Usuario.`.

No reutilice el mismo login en este caso; de lo contrario estaría probando duplicidad de login, no la restricción uno a uno de Persona–Usuario.

## 20. Usuario inexistente

Use `usuario-no-existe-2026` en cada solicitud:

| Operación | Solicitud | Resultado |
|---|---|---|
| GET | `/api/v1/usuarios/usuario-no-existe-2026` | 404 `RESOURCE_NOT_FOUND` |
| PUT | `/api/v1/usuarios/usuario-no-existe-2026` con `{"estado":0}` | 404 `RESOURCE_NOT_FOUND` |
| PATCH desactivar | `/api/v1/usuarios/usuario-no-existe-2026/desactivar` | 404 `RESOURCE_NOT_FOUND` |
| PATCH activar | `/api/v1/usuarios/usuario-no-existe-2026/activar` | 404 `RESOURCE_NOT_FOUND` |
| PUT password | `/api/v1/usuarios/usuario-no-existe-2026/password` | 404 `RESOURCE_NOT_FOUND` |

En todos los casos el body es ProblemDetail con `title: "Recurso no encontrado"`, `detail: "Usuario no encontrado."`, `instance`, `errorCode`, `timestamp` y `traceId`.

## 21. Validaciones de CreateUsuarioRequest

Use un login, password y `codper` ficticios nuevos para cada solicitud. Todos los casos siguientes esperan `400 VALIDATION_ERROR` y `fieldErrors` con el campo afectado.

| Caso | Body o cambio | Campo esperado |
|---|---|---|
| Login omitido | Elimine `login` | `login` |
| Login null | `"login": null` | `login` |
| Login vacío | `"login": ""` | `login` |
| Login solo espacios | `"login": "   "` | `login` |
| Login >30 | 31 caracteres | `login` |
| Password omitida | Elimine `password` | `password` |
| Password null | `"password": null` | `password` |
| Password vacía | `"password": ""` | `password` |
| Password <8 | `"password": "corta"` | `password` |
| Password >72 | 73 caracteres | `password` |
| Estado inválido | `"estado": 2` o `-1` | `estado` |
| `codper` omitido | Elimine `codper` | `codper` |
| `codper` null | `"codper": null` | `codper` |
| `codper` cero | `"codper": 0` | `codper` |
| `codper` negativo | `"codper": -1` | `codper` |

No existe una regla de complejidad adicional para contraseña: no se exige mayúscula, minúscula, número o símbolo mediante anotaciones del DTO.

## 22. Validaciones de UpdateUsuarioRequest

`UpdateUsuarioRequest` solo contiene `estado`:

| Caso | Body | Resultado |
|---|---|---|
| Válido activo | `{"estado":1}` | 200 si el Usuario existe |
| Válido inactivo | `{"estado":0}` | 200 si el Usuario existe |
| Body vacío | `{}` | 400 `VALIDATION_ERROR` |
| Estado omitido | Sin `estado` | 400 `VALIDATION_ERROR` |
| Estado null | `{"estado":null}` | 400 `VALIDATION_ERROR` |
| Estado >1 | `{"estado":2}` | 400 `VALIDATION_ERROR` |
| Estado negativo | `{"estado":-1}` | 400 `VALIDATION_ERROR` |

No envíe `login`, `password`, `passwd`, `codper`, `fechaCreacion` ni `ultimoAcceso`. Aunque un campo desconocido sea ignorado por la configuración automática de Jackson, no forma parte del contrato y no se actualiza.

## 23. Validaciones de ChangePasswordRequest

| Caso | Body | Resultado |
|---|---|---|
| Válido | `{"newPassword":"Temporal-Prueba-2026"}` | 204 si el Usuario existe |
| Omitida | `{}` | 400 `VALIDATION_ERROR` |
| Null | `{"newPassword":null}` | 400 `VALIDATION_ERROR` |
| Vacía | `{"newPassword":""}` | 400 `VALIDATION_ERROR` |
| Menor a 8 | `{"newPassword":"corta"}` | 400 `VALIDATION_ERROR` |
| Mayor a 72 | 73 caracteres | 400 `VALIDATION_ERROR` |
| Formato | No existe regla adicional | No se rechaza por complejidad |

Los textos son ficticios para pruebas y no son recomendaciones de contraseñas de producción.

## 24. JSON mal formado

Pruebe cada body con una petición que normalmente sería válida:

```json
{"login":"usuario.demo" "password":"Temporal-Prueba-2026","codper":1}
```

```json
{"login":"usuario.demo","password":"Temporal-Prueba-2026","codper":1
```

```json
{"login":"usuario.demo","password":"Temporal-Prueba-2026","codper":1,}
```

Resultado: `400`, `errorCode: INVALID_REQUEST`, `title: "Solicitud no válida"`, `detail: "El cuerpo de la solicitud no es válido."`, `instance`, `timestamp` y `traceId`. No se devuelve stack trace.

## 25. Campos desconocidos

No existe una configuración Jackson explícita en `application.yml`; la configuración automática de Spring Boot ignora propiedades desconocidas en estos DTO. Por tanto, un campo adicional como `passwd` no se toma del request ni reemplaza el campo `password`:

```json
{
  "login": "usuario.demo",
  "password": "Temporal-Prueba-2026",
  "passwd": "valor-ignorado",
  "codper": {{personaId}}
}
```

Con los campos obligatorios válidos, el campo desconocido se ignora y el flujo continúa. Si se elimina `password`, la validación sigue fallando: `passwd` nunca es un sustituto aceptado. No use campos desconocidos en colecciones reales.

## 26. Tipos de datos incorrectos

| Caso | Ejemplo | Resultado |
|---|---|---|
| `codper` como texto | `"codper":"abc"` | 400 `INVALID_REQUEST` |
| `estado` como texto | `"estado":"1"` | 400 `INVALID_REQUEST` |
| `estado` decimal | `"estado":1.5` | 400 `INVALID_REQUEST` |
| body como arreglo | `[]` | 400 `INVALID_REQUEST` |
| body como texto | `"texto"` | 400 `INVALID_REQUEST` |

El motivo es que Jackson no puede convertir el JSON al tipo real del DTO. El manejador global no expone la excepción interna.

## 27. Longitudes máximas

| Campo | Regla real | Ejemplo límite | Ejemplo inválido |
|---|---:|---|---|
| `login` | máximo 30 caracteres | 30 caracteres | 31 caracteres |
| `password` | máximo 72 caracteres | 72 caracteres | 73 caracteres |
| `newPassword` | máximo 72 caracteres | 72 caracteres | 73 caracteres |
| `codper` | positivo | `1` | `0` o `-1` |
| `estado` | solo 0 o 1 | `0`, `1` | `2`, `-1` |

Para evitar guardar ejemplos sensibles, genere cadenas ficticias de longitud controlada en Postman o use `"a"` repetida solo en una prueba de validación. No registre el body completo en evidencias.

## 28. ProblemDetail completo

El manejador global produce `application/problem+json` con estas propiedades:

- `type`: `about:blank`;
- `title`;
- `status`;
- `detail`;
- `instance`;
- `errorCode`;
- `timestamp`;
- `traceId`;
- `fieldErrors` solo cuando existen errores de validación; cada elemento contiene `field` y `message`.

Ejemplo seguro de validación:

```json
{
  "type": "about:blank",
  "title": "Solicitud no válida",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "instance": "/api/v1/usuarios",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-08-02T11:30:00Z",
  "traceId": "valor-generado-por-el-backend",
  "fieldErrors": [
    {
      "field": "login",
      "message": "El login es obligatorio."
    }
  ]
}
```

Resumen de errores:

| Error | Status | `errorCode` | `detail` representativo |
|---|---:|---|---|
| Validación | 400 | `VALIDATION_ERROR` | `Uno o más campos no son válidos.` |
| JSON inválido | 400 | `INVALID_REQUEST` | `El cuerpo de la solicitud no es válido.` |
| Recurso inexistente | 404 | `RESOURCE_NOT_FOUND` | `Usuario no encontrado.` o `Persona no encontrada.` |
| Login duplicado | 409 | `CONFLICT` | `El login ya está registrado.` |
| Persona duplicada | 409 | `CONFLICT` | `La Persona ya tiene un Usuario.` |
| Error inesperado | 500 | `INTERNAL_ERROR` | `Ocurrió un error interno.` |

No provoque intencionalmente un error interno en un entorno compartido. Si aparece, informe el `traceId` sin compartir datos sensibles.

## 29. Verificación de seguridad de la respuesta

Puede reutilizar este script en las respuestas JSON de creación, consulta, listado, PUT y PATCH. No lo use en el cambio de contraseña porque la respuesta es `204` y no tiene JSON.

```javascript
pm.test("no hay campos sensibles", function () {
    const body = pm.response.json();
    const forbidden = new Set(["password", "passwd", "hash"]);
    const found = [];

    function inspect(value, path) {
        if (!value || typeof value !== "object") return;
        Object.keys(value).forEach(function (key) {
            if (forbidden.has(key)) found.push(path + key);
            inspect(value[key], path + key + ".");
        });
    }

    inspect(body, "");
    pm.expect(found).to.be.empty;
    pm.expect(JSON.stringify(body).toLowerCase()).to.not.include("bcrypt");
});
```

La respuesta tampoco debe contener la entidad Persona completa ni datos personales como CI, nombre, correo o teléfono. `UsuarioResponse` solo incluye `codper`, no la entidad Persona.

## 30. Verificación de BCrypt

La API no devuelve el hash y Postman no debe solicitarlo. La comprobación observable desde Postman es:

- el request de creación acepta `password`;
- la respuesta de creación no contiene `password`, `passwd` ni `hash`;
- la consulta tampoco contiene esos campos;
- el listado no los contiene en ningún elemento;
- el cambio de contraseña responde `204` sin body;
- después del cambio, ninguna respuesta revela el nuevo hash.

La prueba de integración del backend comprueba localmente que el valor persistido no coincide con el texto, que `PasswordEncoder.matches` devuelve `true` para la contraseña correcta y `false` para otra. Esa comprobación no requiere documentar ni exponer consultas SQL con el hash.

## 31. Orden completo recomendado de pruebas

1. Crear Persona A mediante la guía de Persona y guardar su `codper` en `personaId`.
2. Crear Persona B y guardar su `codper` en `personaIdDos`.
3. Crear Usuario A omitiendo `estado`.
4. Ejecutar el script de creación y guardar `usuarioLogin`.
5. Consultar Usuario A.
6. Listar la primera página.
7. Listar con `size=1`, `size=100` y `size=150`.
8. Actualizar Usuario A con `{"estado":0}`.
9. Desactivar Usuario A.
10. Desactivarlo nuevamente.
11. Activar Usuario A.
12. Activarlo nuevamente.
13. Cambiar su contraseña.
14. Intentar login duplicado usando Persona B.
15. Intentar otro login para Persona A.
16. Intentar crear con Persona inexistente.
17. Probar GET, PUT, activar, desactivar y password con Usuario inexistente.
18. Ejecutar las validaciones de creación.
19. Ejecutar las validaciones de actualización.
20. Ejecutar las validaciones de contraseña.
21. Probar JSON mal formado y tipos incorrectos.
22. Confirmar que `passwd`, `password` y `hash` nunca aparecen.

## 32. Matriz completa de casos

| ID | Endpoint | Escenario | Datos principales | HTTP | `errorCode` | Comprobación |
|---|---|---|---|---:|---|---|
| USR-POST-001 | POST | Crear sin estado | login nuevo, password válida, Persona A | 201 | — | Location y respuesta segura |
| USR-POST-002 | POST | Crear con estado | estado 1, Persona B | 201 | — | estado 1 |
| USR-POST-003 | POST | Login duplicado | mismo login, Persona B | 409 | CONFLICT | no SQL ni secretos |
| USR-POST-004 | POST | Persona duplicada | login distinto, Persona A | 409 | CONFLICT | relación uno a uno |
| USR-POST-005 | POST | Persona inexistente | `codper=99999999` | 404 | RESOURCE_NOT_FOUND | detail seguro |
| USR-GET-001 | GET | Usuario existente | `usuarioLogin` | 200 | — | UsuarioResponse sin secretos |
| USR-GET-002 | GET | Usuario inexistente | login ficticio | 404 | RESOURCE_NOT_FOUND | ProblemDetail |
| USR-LIST-001 | GET | Primera página | page 0, size 20 | 200 | — | metadata y content |
| USR-LIST-002 | GET | Página vacía | page 999, size 20 | 200 | — | content vacío |
| USR-LIST-003 | GET | Tamaño pequeño | size 1 | 200 | — | máximo un elemento |
| USR-LIST-004 | GET | Tamaño máximo | size 100 | 200 | — | size 100 |
| USR-LIST-005 | GET | Tamaño superior | size 150 | 200 | — | controller limita a 100 |
| USR-PUT-001 | PUT | Cambiar estado | `estado=0` | 200 | — | solo estado cambia |
| USR-PUT-002 | PUT | Body vacío | `{}` | 400 | VALIDATION_ERROR | estado obligatorio |
| USR-PUT-003 | PUT | Usuario inexistente | login ficticio | 404 | RESOURCE_NOT_FOUND | detail seguro |
| USR-PATCH-DES-001 | PATCH | Desactivar activo | login activo | 200 | — | estado 0 |
| USR-PATCH-DES-002 | PATCH | Desactivar inactivo | repetir solicitud | 200 | — | idempotencia |
| USR-PATCH-ACT-001 | PATCH | Activar inactivo | login inactivo | 200 | — | estado 1 |
| USR-PATCH-ACT-002 | PATCH | Activar activo | repetir solicitud | 200 | — | idempotencia |
| USR-PASS-001 | PUT password | Cambiar contraseña | newPassword válida | 204 | — | body vacío |
| USR-PASS-002 | PUT password | Usuario inexistente | login ficticio | 404 | RESOURCE_NOT_FOUND | ProblemDetail |
| USR-VAL-001 | POST | Login omitido | sin login | 400 | VALIDATION_ERROR | fieldErrors.login |
| USR-VAL-002 | POST | Login >30 | 31 caracteres | 400 | VALIDATION_ERROR | fieldErrors.login |
| USR-VAL-003 | POST | Password <8 | `corta` | 400 | VALIDATION_ERROR | fieldErrors.password |
| USR-VAL-004 | POST | Password >72 | 73 caracteres | 400 | VALIDATION_ERROR | fieldErrors.password |
| USR-VAL-005 | POST | Estado inválido | 2 | 400 | VALIDATION_ERROR | fieldErrors.estado |
| USR-VAL-006 | POST | codper inválido | 0 o -1 | 400 | VALIDATION_ERROR | fieldErrors.codper |
| USR-VAL-007 | PUT | Estado omitido | `{}` | 400 | VALIDATION_ERROR | fieldErrors.estado |
| USR-VAL-008 | PUT password | newPassword omitida | `{}` | 400 | VALIDATION_ERROR | fieldErrors.newPassword |
| USR-JSON-001 | POST | JSON mal formado | coma o llave faltante | 400 | INVALID_REQUEST | no stack trace |
| USR-JSON-002 | POST | Tipo incorrecto | codper texto | 400 | INVALID_REQUEST | ProblemDetail |
| USR-SEC-001 | respuestas | Campos prohibidos | todas las respuestas JSON | — | — | password/passwd/hash ausentes |

## 33. Evidencia de pruebas manuales

Para cada caso registre en una hoja o reporte:

- fecha y hora;
- ID de la matriz;
- método y ruta;
- body ficticio, sin contraseñas reales;
- status HTTP;
- `errorCode` cuando exista;
- resultado esperado y observado;
- observación breve;
- captura opcional sin tokens, secretos, passwords, hashes ni datos personales.

No guarde el valor de ninguna contraseña ni un hash en la evidencia. Si necesita demostrar el cambio de contraseña, registre únicamente `204 No Content`.

## 34. Limitaciones actuales

- No existe alcance por propiedad, ciudad o sucursal.
- No existe un perfil propio de Persona para INQUILINO en esta fase.
- Activar Usuario no activa Persona.
- Desactivar Usuario no desactiva Persona.
- Reactivar Persona no activa Usuario automáticamente.
- La autenticación exige Persona activa, Usuario activo y contraseña válida.

## 35. Errores frecuentes al usar Postman

- Backend no iniciado: compruebe `http://localhost:9090`.
- Puerto incorrecto: use `9090`, no el puerto de otra aplicación.
- Variables `DB_*` ausentes: el problema ocurre al arrancar, no en Postman.
- PostgreSQL detenido: Flyway o Hibernate no podrán iniciar correctamente.
- `codper` inexistente: use una Persona real sin Usuario.
- Persona ya vinculada: seleccione `personaIdDos` para probar otra cuenta.
- Login reutilizado: cambie `usuarioLoginDos` cuando quiera probar creación exitosa.
- Usar `passwd` en lugar de `password`: `passwd` no es un campo de creación.
- Enviar password dentro del PUT general: use `/password`.
- Enviar body en activar/desactivar: esas rutas no necesitan body.
- Para login, refresh y sesiones use [auth.md](auth.md); no mezcle cookies WEB con refresh MOBILE.
- Compartir capturas con passwords o hashes: elimine esos datos antes de guardar evidencia.

## 36. Checklist final

- [ ] PostgreSQL está activo.
- [ ] Las variables `DB_*` están disponibles en la terminal del backend.
- [ ] El backend escucha en `http://localhost:9090`.
- [ ] Flyway validó V1, V2 y V3.
- [ ] Existe Persona A sin Usuario.
- [ ] Existe Persona B sin Usuario.
- [ ] La creación sin estado devolvió `201` y estado `1`.
- [ ] La respuesta de creación incluyó `Location`.
- [ ] La consulta por login devolvió `200`.
- [ ] El listado devolvió `PageResponse` correcto.
- [ ] Se probó `size=1`, `size=100` y `size=150`.
- [ ] La actualización administrativa modificó solo `estado`.
- [ ] La desactivación devolvió estado `0`.
- [ ] La desactivación repetida fue idempotente.
- [ ] La activación devolvió estado `1`.
- [ ] La activación repetida fue idempotente.
- [ ] El cambio de contraseña devolvió `204` sin body.
- [ ] Login duplicado devolvió `409 CONFLICT`.
- [ ] Persona duplicada devolvió `409 CONFLICT`.
- [ ] Persona inexistente devolvió `404 RESOURCE_NOT_FOUND`.
- [ ] Usuario inexistente devolvió `404` en GET, PUT, activar, desactivar y password.
- [ ] Se probaron validaciones de creación.
- [ ] Se probaron validaciones de actualización.
- [ ] Se probaron validaciones de contraseña.
- [ ] Se probó JSON mal formado.
- [ ] Se probaron tipos incorrectos.
- [ ] Se revisó ProblemDetail.
- [ ] Ninguna respuesta contiene `password`, `passwd`, `hash` o BCrypt.
- [ ] No se registraron credenciales ni hashes.
- [ ] PROPIETARIO listó y operó sobre Usuarios comunes y propietarios.
- [ ] ADMINISTRADOR listó solo Usuarios comunes y recibió 403 sobre un propietario.
- [ ] INQUILINO recibió 403 en el CRUD genérico.
- [ ] Cada Usuario pudo cambiar únicamente su propia contraseña; PROPIETARIO pudo cambiar una ajena.
- [ ] ADMINISTRADOR recibió 403 al cambiar una contraseña ajena.
- [ ] Desactivar al último propietario devolvió `409 LAST_OWNER_REQUIRED` sin cambios parciales.

## Autorización de Fase 11.2

Use `Authorization: Bearer` en todas las rutas. PROPIETARIO administra cualquier Usuario. ADMINISTRADOR crea y opera Usuarios comunes; su listado excluye Usuarios con asignación a un Rol PROPIETARIO activo. No puede consultar, activar, desactivar ni modificar esos objetivos. INQUILINO y Usuario sin Rol no acceden al CRUD.

`PUT /api/v1/usuarios/{login}/password` permite únicamente `login autenticado == login objetivo` o `ROLE_PROPIETARIO`. El cambio sigue revocando todas las sesiones del objetivo.
