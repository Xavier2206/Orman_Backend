# Informe de integración Backend ORMAN → Frontend Angular

> Fuente de verdad: código actual del backend ORMAN. Este documento es temporal y puede eliminarse cuando ya no sea necesario.

## 1. Resumen backend

ORMAN expone un backend Spring Boot para Personas, Usuarios, Roles, autenticación, sesiones y la administración de Menús/Procesos.

### ✅ Hecho confirmado por código

- API REST bajo `/api/v1`.
- CRUD Persona y administración de Usuarios con BCrypt.
- Roles, Usuario–Rol, JWT HS256, refresh rotatorio y sesiones por dispositivo.
- Seguridad HTTP stateless, CORS, CSRF para refresh WEB y OTP por correo.
- Autorización por `PROPIETARIO` y `ADMINISTRADOR`.
- Menús, Procesos, Rol–Menú y Menú–Proceso.
- Contexto post-login del Usuario autenticado (`GET /api/v1/auth/context`).
- Errores RFC 9457 con `ProblemDetail`.
- Flyway V1–V9.

### ⚠️ Inferencia / parcial

- `INQUILINO` puede existir como nombre de Rol, pero el código no le concede privilegios administrativos especiales.
- El cliente debe consumir el contexto autenticado para perfil y navegación; no debe inferirlo desde el JWT.

### ❌ Pendiente en backend

- Propiedades/inmuebles.
- Imágenes y upload de propiedades.
- Contacto, consultas, reservas o interesados.
- OpenAPI.

## 2. URL y configuración

```text
Host local: http://localhost:9090
Base API:   http://localhost:9090/api/v1
```

No existe `context-path`.

```text
Origen CORS por defecto: http://localhost:4200
Variable CORS: ORMAN_FRONTEND_URL
```

Variables externas relevantes: `DB_*`, `JWT_*`, `OTP_*`, `MAIL_*`, `REFRESH_*` y `ORMAN_FRONTEND_URL`. No exponer sus valores.

## 3. Autenticación

Endpoints públicos:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/otp/verify
POST /api/v1/auth/otp/resend
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "login": "usuario.demo",
  "password": "password-de-8-a-72-caracteres",
  "deviceId": "angular-browser",
  "deviceName": "Chrome Windows",
  "clientType": "WEB"
}
```

Validaciones: `login` obligatorio/máximo 30; `password` obligatorio, 8–72; `deviceId` y `deviceName` obligatorios/máximo 100; `clientType` obligatorio (`WEB` o `MOBILE`).

Flujo:

```text
WEB + ROLE_PROPIETARIO/ROLE_ADMINISTRADOR → OTP_REQUIRED
Todo otro caso                           → AUTHENTICATED
```

Respuesta WEB autenticada:

```json
{
  "status": "AUTHENTICATED",
  "login": "usuario.demo",
  "codper": 10,
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "sid": "uuid"
}
```

Respuesta MOBILE autenticada agrega `refreshToken`.

```json
{
  "status": "OTP_REQUIRED",
  "challengeId": "uuid",
  "expiresIn": 300
}
```

Errores: `400 VALIDATION_ERROR`, `401 INVALID_CREDENTIALS`, `503 OTP_DELIVERY_FAILED`.

## 4. JWT, refresh y OTP

### JWT

- Algoritmo `HS256`.
- Campo: `accessToken`.
- Duración por defecto: 900 segundos.
- Uso: `Authorization: Bearer <accessToken>`.
- Claims: `sub`, `sid`, `iss`, `iat`, `exp`.
- No contiene Roles, permisos, Persona, refresh token ni hashes.

Errores: JWT ausente/inválido `401 INVALID_TOKEN`; vencido `401 TOKEN_EXPIRED`; sesión revocada `401 SESSION_REVOKED`; sesión vencida `401 SESSION_EXPIRED`.

### Refresh WEB

```text
accessToken  → memoria Angular
refreshToken → cookie HttpOnly
```

Cookie por defecto:

```text
Nombre: orman_refresh
HttpOnly: true
Path: /api/v1/auth
Secure: false por defecto local
SameSite: Lax por defecto
```

Usar `withCredentials: true` en login y refresh.

```http
POST /api/v1/auth/refresh
X-XSRF-TOKEN: <csrf-token>
```

WEB no envía body; el navegador adjunta la cookie. La respuesta devuelve access token nuevo y rota la cookie refresh. No devuelve `refreshToken` en JSON.

MOBILE envía:

```json
{ "refreshToken": "sid.secreto" }
```

y recibe otro refresh token en JSON. Si llegan cookie y body con valores distintos: `401 INVALID_REFRESH_TOKEN`.

Ante `401 TOKEN_EXPIRED`: intentar refresh una sola vez, reintentar la petición original una vez; si falla, limpiar sesión y redirigir a login.

### CSRF

```text
Cookie: XSRF-TOKEN
Header: X-XSRF-TOKEN
```

Solo se exige en refresh WEB cuando hay cookie refresh.

### OTP

Verify:

```http
POST /api/v1/auth/otp/verify
```

```json
{
  "challengeId": "uuid",
  "code": "123456",
  "deviceId": "angular-browser",
  "deviceName": "Chrome Windows"
}
```

Resend:

```http
POST /api/v1/auth/otp/resend
Content-Type: application/json
```

```json
{ "challengeId": "uuid" }
```

Resend exitoso: `204 No Content`.

Defaults actuales: expiración 300 s, máximo 5 intentos, cooldown 60 s, máximo 3 reenvíos. El backend no expone contadores ni tiempo restante mediante API.

## 5. Roles y autorización

Roles especiales reales: `PROPIETARIO` y `ADMINISTRADOR`. No existen datos semilla de roles en migraciones.

| Grupo | PROPIETARIO | ADMINISTRADOR | INQUILINO/sin Rol |
|---|---:|---:|---:|
| Auth pública | Sí | Sí | Sí |
| Sesiones propias | Sí | Sí | Sí |
| Crear/listar Personas | Sí | Sí | No |
| Gestionar Persona | Sí | Solo objetivo común | No |
| Crear/listar Usuarios | Sí | Solo comunes | No |
| Gestionar Usuario | Sí | Solo objetivo común | No |
| Contraseña propia | Sí | Sí | Sí |
| Contraseña ajena | Sí | No | No |
| Roles, Menús y Procesos | Sí | No | No |

El JWT no trae roles. No existe `/auth/me`, `/auth/roles` ni endpoint de navegación del usuario actual.

## 6. API Personas

Servicio: `PersonaService`. Requiere `Authorization: Bearer`.

| Método | URL | Permiso | Body | Éxito |
|---|---|---|---|---:|
| POST | `/personas` | Propietario/Admin | `CreatePersonaRequest` | 201 |
| GET | `/personas/{codper}` | Propietario/Admin sobre común | — | 200 |
| GET | `/personas` | Propietario/Admin | — | 200 |
| PUT | `/personas/{codper}` | Propietario/Admin sobre común | `UpdatePersonaRequest` | 200 |
| PATCH | `/personas/{codper}/activar` | Propietario/Admin sobre común | — | 200 |
| PATCH | `/personas/{codper}/desactivar` | Propietario/Admin sobre común | — | 200 |
| DELETE | `/personas/{codper}` | Propietario/Admin sobre común | — | 204 |

Request:

```json
{
  "ci": "1234567",
  "nombre": "Ana",
  "ap": "Pérez",
  "am": "López",
  "genero": "F",
  "estado": "1",
  "correo": "ana@example.com",
  "telefono": "70000000",
  "tipoPersona": "I",
  "foto": "https://ejemplo.com/foto.jpg"
}
```

Response:

```json
{
  "codper": 10,
  "ci": "1234567",
  "nombre": "Ana",
  "ap": "Pérez",
  "am": "López",
  "genero": "F",
  "estado": 1,
  "correo": "ana@example.com",
  "telefono": "70000000",
  "tipoPersona": "I",
  "foto": "https://ejemplo.com/foto.jpg",
  "fechaRegistro": "2026-08-11T14:00:00"
}
```

Validaciones: `ci` obligatorio/max 20; `nombre` obligatorio/max 60; `ap`/`am` opcionales/max 40; `genero` M/F; `estado` 0/1; `correo` obligatorio/email/max 100; `telefono` obligatorio/max 20; `tipoPersona` A/I; `foto` opcional/max 255.

Posible comportamiento actual: eliminar una Persona con Usuario asociado puede responder `500 INTERNAL_ERROR` por la FK restrictiva.

## 7. API Usuarios

Servicio: `UsuarioService`.

| Método | URL | Permiso | Body | Éxito |
|---|---|---|---|---:|
| POST | `/usuarios` | Propietario/Admin | `CreateUsuarioRequest` | 201 |
| GET | `/usuarios/{login}` | Propietario/Admin sobre común | — | 200 |
| GET | `/usuarios` | Propietario/Admin | — | 200 |
| PUT | `/usuarios/{login}` | Propietario/Admin sobre común | `UpdateUsuarioRequest` | 200 |
| PATCH | `/usuarios/{login}/activar` | Propietario/Admin sobre común | — | 200 |
| PATCH | `/usuarios/{login}/desactivar` | Propietario/Admin sobre común | — | 200 |
| PUT | `/usuarios/{login}/password` | Titular o Propietario | `ChangePasswordRequest` | 204 |

Create:

```json
{
  "login": "ana.perez",
  "password": "password-de-8-a-72-caracteres",
  "estado": 1,
  "codper": 10
}
```

Response:

```json
{
  "login": "ana.perez",
  "estado": 1,
  "codper": 10,
  "fechaCreacion": "2026-08-11T14:00:00",
  "ultimoAcceso": null
}
```

Update: `{ "estado": 0 }`. Password: `{ "newPassword": "nueva-password" }`.

Validaciones: login máximo 30; contraseñas 8–72; estado 0/1; `codper` positivo. No se exponen hashes, `passwd`, password ni refresh token.

## 8. API Roles

Todos requieren `PROPIETARIO`.

| Método | URL | Body | Éxito |
|---|---|---|---:|
| POST | `/roles` | `CreateRolRequest` | 201 |
| GET | `/roles/{codr}` | — | 200 |
| GET | `/roles` | — | 200 |
| PUT | `/roles/{codr}` | `UpdateRolRequest` | 200 |
| PATCH | `/roles/{codr}/activar` | — | 200 |
| PATCH | `/roles/{codr}/desactivar` | — | 200 |
| POST | `/usuarios/{login}/roles/{codr}` | — | 201 |
| DELETE | `/usuarios/{login}/roles/{codr}` | — | 204 |
| GET | `/usuarios/{login}/roles` | — | 200 |
| GET | `/roles/{codr}/usuarios` | — | 200 |

```json
{ "nombre": "OPERADOR", "estado": 1 }
```

```json
{ "codr": 3, "nombre": "OPERADOR", "estado": 1 }
```

Asignación Usuario–Rol:

```json
{
  "login": "ana.perez",
  "codr": 3,
  "nombreRol": "OPERADOR",
  "fechaAsignacion": "2026-08-11T14:00:00"
}
```

## 9. API Menús

Todos requieren `PROPIETARIO`.

| Método | URL | Body | Éxito |
|---|---|---|---:|
| POST | `/menus` | `CreateMenuRequest` | 201 |
| GET | `/menus/{codm}` | — | 200 |
| GET | `/menus` | — | 200 |
| PUT | `/menus/{codm}` | `UpdateMenuRequest` | 200 |
| PATCH | `/menus/{codm}/activar` | — | 200 |
| PATCH | `/menus/{codm}/desactivar` | — | 200 |

```json
{ "nombre": "PERSONAS", "icono": "groups", "estado": 1 }
```

```json
{ "codm": 5, "nombre": "PERSONAS", "icono": "groups", "estado": 1 }
```

❌ **Pendiente en backend — navegación dinámica del usuario.** No existe endpoint que devuelva los menús autorizados del usuario autenticado.

## 10. API Procesos

Todos requieren `PROPIETARIO`.

| Método | URL | Body | Éxito |
|---|---|---|---:|
| POST | `/procesos` | `CreateProcesoRequest` | 201 |
| GET | `/procesos/{codp}` | — | 200 |
| GET | `/procesos` | — | 200 |
| PUT | `/procesos/{codp}` | `UpdateProcesoRequest` | 200 |
| PATCH | `/procesos/{codp}/activar` | — | 200 |
| PATCH | `/procesos/{codp}/desactivar` | — | 200 |

```json
{ "nombre": "LISTAR PERSONAS", "enlace": "personas/listar", "estado": 1 }
```

```json
{ "codp": 8, "nombre": "LISTAR PERSONAS", "enlace": "personas/listar", "estado": 1 }
```

## 11. Relaciones

Todos requieren `PROPIETARIO`.

```text
POST/DELETE /roles/{codr}/menus/{codm}
GET         /roles/{codr}/menus
GET         /menus/{codm}/roles

POST/DELETE /menus/{codm}/procesos/{codp}
GET         /menus/{codm}/procesos
GET         /procesos/{codp}/menus
```

Rol–Menú:

```json
{
  "codr": 3,
  "nombreRol": "OPERADOR",
  "estadoRol": 1,
  "codm": 5,
  "nombreMenu": "PERSONAS",
  "estadoMenu": 1
}
```

Menú–Proceso:

```json
{
  "codm": 5,
  "nombreMenu": "PERSONAS",
  "estadoMenu": 1,
  "codp": 8,
  "nombreProceso": "LISTAR PERSONAS",
  "enlaceProceso": "personas/listar",
  "estadoProceso": 1
}
```

## 12. Propiedades

❌ **Pendiente en backend — propiedades.** No existen entidad, migración, DTO, repository, service, controller ni endpoints de propiedad/inmueble/casa/departamento/publicación.

## 13. Imágenes

❌ **Pendiente en backend — imágenes de propiedad.** No existen upload, `MultipartFile`, `multipart/form-data`, storage, S3 ni Cloudinary. `Persona.foto` es solo un String de hasta 255 caracteres.

## 14. Contacto

❌ **Pendiente en backend — contacto/consultas.** No existen endpoints ni modelo para contacto, consulta, interesado, mensaje, WhatsApp, reserva o solicitud.

## 15. CORS

```text
Origen por defecto: http://localhost:4200
Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Headers: Authorization, Content-Type, X-XSRF-TOKEN
Exposed headers: X-XSRF-TOKEN
Credentials: true
```

Usar `withCredentials: true` en llamadas WEB que reciben o envían cookies.

## 16. Manejo de errores

```http
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Solicitud no válida",
  "status": 400,
  "detail": "Uno o más campos no son válidos.",
  "instance": "/api/v1/personas",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-08-11T18:00:00Z",
  "traceId": "uuid",
  "fieldErrors": [
    { "field": "correo", "message": "El correo no tiene un formato válido." }
  ]
}
```

Estados: `400 VALIDATION_ERROR/INVALID_REQUEST`, `401` de auth/token/sesión, `403 ACCESS_DENIED` o CSRF, `404 RESOURCE_NOT_FOUND`, `409 CONFLICT/LAST_OWNER_REQUIRED`, `422 BUSINESS_RULE_VIOLATION`, `500 INTERNAL_ERROR`, `503 OTP_DELIVERY_FAILED`.

## 17. Paginación

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

Parámetros: `page` base 0, `size`, `sort`. Tamaño máximo efectivo: 100.

| Endpoint | Default |
|---|---|
| `/personas` | `page=0&size=20&sort=codper,asc` |
| `/usuarios` | `page=0&size=20&sort=login,asc` |
| `/roles` | `page=0&size=20&sort=nombre,asc` |
| `/menus` | `page=0&size=20&sort=nombre,asc` |
| `/procesos` | `page=0&size=20&sort=nombre,asc` |

## 18. Orden recomendado para Angular

1. Environment, `HttpClient`, `ProblemDetail` y paginación.
2. Login WEB con `withCredentials`.
3. OTP, refresh, interceptor JWT y manejo único de 401.
4. Logout y sesiones.
5. Personas, Usuarios y Roles.
6. Menús, Procesos y relaciones administrativas.
7. Mantener Propiedades, Imágenes y Contacto como mocks claramente separados hasta que exista backend.

## 19. Mapa de entidades

```text
Persona
 └── Usuario
      ├── SesionUsuario
      ├── OtpChallenge (por login)
      └── RolUsu ── Rol
                    └── RolMe ── Menu
                                  └── MePro ── Proceso
```
