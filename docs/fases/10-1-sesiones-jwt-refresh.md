# Fase 10.1 — Sesiones por dispositivo, JWT y refresh token

## Estado

`COMPLETADA` el 2026-08-03. La Fase 10 global permanece `EN DESARROLLO` hasta ejecutar expresamente la Fase 10.2.

## Objetivo y alcance

Esta subfase incorpora la base técnica de autenticación por tokens para Angular WEB, Flutter MOBILE y Postman: sesiones persistentes por dispositivo, access token JWT de 15 minutos y refresh token opaco de 30 días con rotación. Incluye login y `POST /api/v1/auth/refresh`.

No incluye filtro JWT, `SecurityFilterChain`, protección de endpoints, logout, administración de sesiones, revocación por cambios administrativos, autorización, Roles en el token, CORS o CSRF definitivos. Esos puntos no se adelantaron.

## Implementación

- V6 crea `sesiones_usuario` con PK UUID, FK exacta `VARCHAR(30)` a `usuarios.login`, checks explícitos, índice de login e índice único parcial para `(login, device_id)` activo.
- `SesionUsuario` usa relación `LAZY` unidireccional hacia `Usuario`, sin cascadas ni colección inversa. Su igualdad depende de `sid`; no genera `toString` con relaciones o hash.
- `ClientType` admite `WEB` y `MOBILE`; `RevocationReason` solo contiene `REPLACED_BY_NEW_LOGIN`, `REFRESH_REUSE` y `EXPIRED`.
- Nimbus JOSE + JWT 10.8 firma exclusivamente HS256. La configuración exige un secreto externo de 32 bytes como mínimo y valida issuer, expiraciones y cookie al arrancar.
- El JWT contiene únicamente `sub`, `sid`, `iss`, `iat` y `exp`; no contiene Persona, Roles, authorities ni datos personales.
- El refresh tiene forma `sid.secreto`, usa 32 bytes de `SecureRandom`, Base64 URL sin padding y SHA-256 sobre el token completo. PostgreSQL guarda solo el hash Base64 URL; la comparación usa tiempo constante.
- Login bloquea pesimistamente el Usuario, revoca y hace flush de la sesión previa del mismo dispositivo, crea una nueva y preserva las de otros dispositivos.
- Refresh bloquea pesimistamente la sesión. Una rotación cambia hash, incrementa versión y actualiza `ultimo_uso`. Un hash no coincidente revoca con `REFRESH_REUSE`; una sesión vencida se marca `EXPIRED`.
- `InvalidRefreshTokenException` no provoca rollback en refresh para conservar la revocación que origina el 401 seguro.

## Contratos

`POST /api/v1/auth/login` recibe login, password, `deviceId`, `deviceName` y `clientType`. MOBILE devuelve ambos tokens en JSON. WEB omite el refresh del JSON y lo entrega en cookie `HttpOnly`, con `Path=/api/v1/auth`, `Max-Age` coherente y `Secure`/`SameSite` configurables.

`POST /api/v1/auth/refresh` recibe el token en body MOBILE o cookie WEB. Si ambos valores existen y difieren, responde `401 INVALID_REFRESH_TOKEN`. Todos los fallos de refresh comparten el detalle `La sesión no es válida o ha expirado.`

## Archivos principales

- Migración: `src/main/resources/db/migration/V6__create_sesiones_usuario_table.sql`.
- Sesión: `auth/entity`, `auth/model` y `auth/repository`.
- Tokens/configuración: `auth/config`, `JwtService`, `RefreshTokenService` y sus implementaciones.
- HTTP: DTO, `AuthController`, `AuthService` y `AuthServiceImpl`.
- Pruebas: configuración, servicios criptográficos, servicio de auth, MVC e integración PostgreSQL.

## Validación

- PostgreSQL 17.6 conectado; Flyway validó seis migraciones y dejó el esquema en V6.
- Hibernate inició con `ddl-auto=validate`.
- Se verificaron login WEB/MOBILE, coexistencia, reemplazo por `deviceId`, JWT, cookie, refresh, rotación, reutilización, errores seguros, constraints e índices.
- `.\mvnw.cmd clean test`: `BUILD SUCCESS`; 133 pruebas, 0 fallos, 0 errores y 0 omitidas.

## Riesgos y pendientes

- La clave HS256 debe generarse y gestionarse fuera del repositorio; en producción la cookie requiere HTTPS y `Secure=true`.
- El access token aún no autentica requests; el filtro y la cadena HTTP pertenecen a 10.2.
- CORS/CSRF deben diseñarse en 10.2 considerando credenciales WEB y el frontend real.
- No se ejecutaron pruebas manuales con Angular o Flutter; la compatibilidad queda definida por contrato y validada por MVC/integración.
