# Guía Postman — Autenticación inicial

## Alcance

Esta guía prueba exclusivamente `POST {{baseUrl}}/api/v1/auth/login`. No genera JWT, refresh token, cookies, sesiones, `sid`, logout, roles de autorización ni datos de dispositivo.

Variables recomendadas:

```text
baseUrl = http://localhost:9090
loginAuth = usuario.auth.2026
passwordAuth = Temporal-Prueba-2026
```

No guarde contraseñas reales en colecciones compartidas, capturas ni consola. Los valores de esta guía son ficticios.

## Preparación

1. Cree una Persona activa mediante `POST /api/v1/personas`.
2. Cree un Usuario activo asociado a esa Persona mediante `POST /api/v1/usuarios`, usando `{{loginAuth}}` y `{{passwordAuth}}`.
3. No es necesario asignar Roles: un Usuario sin Roles puede autenticarse.

## Login correcto

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "login": "{{loginAuth}}",
  "password": "{{passwordAuth}}"
}
```

Respuesta esperada: `200 OK`.

```json
{
  "login": "usuario.auth.2026",
  "codper": 1
}
```

Script de prueba:

```javascript
pm.test("login correcto", () => pm.response.to.have.status(200));
const body = pm.response.json();
pm.expect(body).to.have.property("login");
pm.expect(body).to.have.property("codper");
["authenticated", "password", "passwd", "password_hash", "hash", "persona", "roles", "token", "accessToken", "refreshToken", "sid"]
  .forEach(field => pm.expect(body).to.not.have.property(field));
```

Puede consultar después `GET /api/v1/usuarios/{{loginAuth}}` para observar que `ultimoAcceso` deja de ser `null`; no consulte ni exponga el hash almacenado.

## Credenciales inválidas

Los siguientes casos responden exactamente `401 INVALID_CREDENTIALS` con detalle `Las credenciales no son válidas.`:

| Caso | Preparación |
|---|---|
| Contraseña incorrecta | Envíe una contraseña ficticia diferente. |
| Usuario inexistente | Envíe un login ficticio no creado. |
| Usuario inactivo | Desactive el Usuario y use la contraseña correcta. |
| Persona inactiva | Reactive el Usuario si hace falta, desactive la Persona y use la contraseña correcta. |

Script común:

```javascript
pm.test("credenciales protegidas", () => pm.response.to.have.status(401));
const body = pm.response.json();
pm.expect(body.errorCode).to.eql("INVALID_CREDENTIALS");
pm.expect(body.title).to.eql("Credenciales inválidas");
pm.expect(body.detail).to.eql("Las credenciales no son válidas.");
pm.expect(body.traceId).to.be.a("string").and.not.empty;
pm.expect(JSON.stringify(body)).to.not.include("passwd");
pm.expect(JSON.stringify(body)).to.not.include("bcrypt");
```

Los fallos no actualizan `ultimoAcceso`.

## Roles

Pruebe un Usuario sin Roles: el login debe responder `200`. Luego asigne uno o varios Roles, incluso desactive uno asignado: el login sigue respondiendo `200` si Usuario, Persona y contraseña son válidos. La Fase 09 ignora por completo Roles y `rolusu`.

## Validación y JSON inválido

Todos los siguientes deben devolver `400`:

| Caso | Resultado |
|---|---|
| `login` omitido, vacío o solo espacios | `VALIDATION_ERROR` |
| `login` de más de 30 caracteres | `VALIDATION_ERROR` |
| `password` omitida, vacía o solo espacios | `VALIDATION_ERROR` |
| `password` con menos de 8 o más de 72 caracteres | `VALIDATION_ERROR` |
| JSON mal formado | `INVALID_REQUEST` |

Ejemplo de JSON inválido:

```json
{"login":"usuario.demo","password":"Temporal-Prueba-2026"
```

## Matriz y checklist

| Id | Caso | HTTP | Código |
|---|---|---:|---|
| AUTH-001 | Login correcto | 200 | — |
| AUTH-002 | Password incorrecta | 401 | `INVALID_CREDENTIALS` |
| AUTH-003 | Usuario inexistente | 401 | `INVALID_CREDENTIALS` |
| AUTH-004 | Usuario inactivo | 401 | `INVALID_CREDENTIALS` |
| AUTH-005 | Persona inactiva | 401 | `INVALID_CREDENTIALS` |
| AUTH-006 | Usuario sin Roles | 200 | — |
| AUTH-007 | Request inválido | 400 | `VALIDATION_ERROR` |
| AUTH-008 | JSON inválido | 400 | `INVALID_REQUEST` |

- [ ] El login correcto devuelve solo `login` y `codper`.
- [ ] La respuesta no contiene password, hash, Persona, Roles, token o sesión.
- [ ] Todos los fallos de autenticación devuelven el mismo contrato externo.
- [ ] `ultimoAcceso` cambia solo tras login correcto.
- [ ] No se registraron contraseñas ni hashes en consola o evidencia.
- [ ] No se recibieron tokens, cookies ni datos de sesión.
