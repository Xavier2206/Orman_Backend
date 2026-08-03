# Guía Postman — Login, JWT y refresh

## 1. Variables de entorno

```text
baseUrl = http://localhost:9090
loginAuth = usuario.auth.2026
passwordAuth = Temporal-Prueba-2026
deviceMobile = postman-mobile-001
deviceWeb = postman-web-001
accessToken =
refreshToken =
previousRefreshToken =
sid =
```

Use datos ficticios. No sincronice contraseñas, JWT, refresh tokens, cookies ni capturas con valores vigentes.

## 2. Configuración JWT local

Defina `JWT_SECRET` con un valor aleatorio de al menos 32 bytes; no copie el ejemplo como secreto real. Los valores predeterminados son issuer `orman-backend`, access de 15 minutos, refresh de 30 días, cookie `orman_refresh`, `Secure=false` y `SameSite=Lax`. En producción use HTTPS y `REFRESH_COOKIE_SECURE=true`.

## 3. Login MOBILE

```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "login": "{{loginAuth}}",
  "password": "{{passwordAuth}}",
  "deviceId": "{{deviceMobile}}",
  "deviceName": "Postman Mobile",
  "clientType": "MOBILE"
}
```

Devuelve `login`, `codper`, `accessToken`, `refreshToken`, `tokenType: Bearer`, `expiresIn: 900` y `sid`.

```javascript
pm.test("login MOBILE", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.environment.set("accessToken", json.accessToken);
pm.environment.set("refreshToken", json.refreshToken);
pm.environment.set("sid", json.sid);
pm.expect(json.tokenType).to.eql("Bearer");
pm.expect(json.expiresIn).to.eql(900);
["password", "passwd", "hash", "persona", "roles"].forEach(k => pm.expect(json).not.to.have.property(k));
```

## 4–5. Login WEB y cookie HttpOnly

Use el mismo endpoint con `deviceId={{deviceWeb}}`, nombre de navegador y `clientType=WEB`. El JSON contiene access token pero no `refreshToken`. Abra **Cookies** en Postman para `localhost`: debe existir `orman_refresh`, `HttpOnly`, path `/api/v1/auth`, `Max-Age=2592000`, `SameSite=Lax` y `Secure=false` solo en local. Postman administra la cookie; no la copie a variables ni scripts.

## 6–7. Access token y claims

El JWT firmado tiene tres segmentos. Su payload mínimo contiene `sub={{loginAuth}}`, `sid={{sid}}`, `iss=orman-backend`, `iat` y `exp`; `exp-iat` es 900 segundos. No debe contener Roles, authorities, Persona, password ni datos personales. Decodificar el payload sirve para inspección, no valida la firma.

## 8–10. Sesiones por dispositivo

- Login WEB y MOBILE con distintos `deviceId`: ambas sesiones permanecen activas.
- Otro dispositivo conserva su sesión.
- Repetir login con el mismo login y `deviceId` revoca la anterior con `REPLACED_BY_NEW_LOGIN` y crea un `sid` nuevo.
- Cambiar solo WEB/MOBILE sin cambiar `deviceId` también reemplaza la sesión activa de esa combinación login/dispositivo.

## 11. Refresh MOBILE

Antes de renovar, copie temporalmente el valor actual a `previousRefreshToken`.

```http
POST {{baseUrl}}/api/v1/auth/refresh
Content-Type: application/json

{"refreshToken":"{{refreshToken}}"}
```

```javascript
pm.test("refresh MOBILE", () => pm.response.to.have.status(200));
const json = pm.response.json();
pm.expect(json.refreshToken).not.to.eql(pm.environment.get("refreshToken"));
pm.environment.set("accessToken", json.accessToken);
pm.environment.set("refreshToken", json.refreshToken);
```

## 12. Refresh WEB

Envíe `POST {{baseUrl}}/api/v1/auth/refresh` sin body. Postman adjunta la cookie. Debe llegar un access token nuevo, ninguna propiedad `refreshToken` y una nueva cookie `orman_refresh`.

## 13–14. Rotación y reutilización

Cada refresh reemplaza el hash, incrementa la versión y actualiza `ultimo_uso`. Envíe `previousRefreshToken`: debe responder 401, `INVALID_REFRESH_TOKEN`, revocar la sesión con `REFRESH_REUSE` y no emitir tokens. El refresh más reciente tampoco funcionará después de esa revocación.

## 15–18. Casos negativos

| Caso | Ejecución | Resultado |
|---|---|---|
| Token manipulado | Cambie un carácter de la parte secreta conservando el sid | 401 y revocación por reutilización |
| Token mal formado/sid inexistente | Envíe texto o UUID ficticio con 32 bytes Base64 URL | 401 genérico |
| Sesión expirada | Pruebe una sesión con `fecha_expiracion <= ahora UTC` | 401; puede quedar `EXPIRED` |
| Usuario inactivo | Desactive Usuario después del login | 401, sin revocación administrativa en 10.1 |
| Persona inactiva | Desactive Persona después del login | 401, sin revocación administrativa en 10.1 |
| Sesión ya revocada | Use cualquiera de sus refresh | 401 genérico |
| Cookie/body ausentes | Refresh sin ninguno | 401 genérico |
| Cookie/body distintos | Envíe ambos con valores diferentes | 401 genérico |

## 19. ProblemDetail

Todo fallo de refresh devuelve `application/problem+json`, HTTP 401, `errorCode=INVALID_REFRESH_TOKEN`, título `Sesión no válida`, detalle `La sesión no es válida o ha expirado.`, timestamp y `traceId`. Nunca distingue la causa ni devuelve token, hash, SQL, constraint o stack trace.

## 20. Scripts Postman

Script de seguridad reutilizable:

```javascript
const raw = pm.response.text().toLowerCase();
["password", "passwd", "refresh_token_hash", "jwt_secret", "roles", "authorities"]
  .forEach(value => pm.expect(raw).not.to.include(value));
```

Para WEB compruebe `pm.response.json()` sin `refreshToken`; inspeccione la cookie solo desde el administrador de Postman. No registre su valor en consola.

## 21. Matriz completa

| Id | Caso | HTTP |
|---|---|---:|
| AUTH-101 | Login MOBILE | 200 |
| AUTH-102 | Login WEB + cookie | 200 |
| AUTH-103 | WEB/MOBILE simultáneos | 200/200 |
| AUTH-104 | Mismo device reemplaza | 200 |
| AUTH-105 | Refresh MOBILE | 200 |
| AUTH-106 | Refresh WEB | 200 |
| AUTH-107 | Reutilización/manipulación | 401 |
| AUTH-108 | Expirada/revocada/inexistente | 401 |
| AUTH-109 | Usuario/Persona inactivos | 401 |
| AUTH-110 | Cookie/body ausentes o ambiguos | 401 |
| AUTH-111 | Credenciales inválidas | 401 `INVALID_CREDENTIALS` |
| AUTH-112 | Validación/JSON inválido | 400 |

## 22. Orden recomendado

Prepare Persona/Usuario activos; ejecute login MOBILE, login WEB, inspección JWT/cookie, coexistencia, reemplazo, refresh MOBILE, refresh WEB, rotación, reutilización y casos negativos. Use dispositivos nuevos al reiniciar la matriz.

## 23. Checklist

- [ ] Access contiene solo claims aprobados y dura 900 segundos.
- [ ] MOBILE recibe refresh en JSON; WEB no.
- [ ] WEB recibe cookie HttpOnly con atributos correctos.
- [ ] Dos dispositivos coexisten y el mismo dispositivo se reemplaza.
- [ ] Cada refresh rota token/hash/versión.
- [ ] Reutilización revoca y devuelve el mismo 401 seguro.
- [ ] Ninguna respuesta contiene password, hash, Persona completa o Roles.

## 24. Seguridad de evidencias y clientes

Antes de compartir capturas, oculte Authorization, JWT, refresh, cookie, contraseña, variables y consola. Flutter debe generar un `deviceId` persistente, enviar MOBILE, guardar refresh en almacenamiento seguro y mantener access preferentemente en memoria. Angular debe generar un `deviceId` persistente del navegador, enviar WEB, mantener access en memoria, no leer la cookie HttpOnly y usar credenciales al renovar. No se implementa código cliente en esta fase.
