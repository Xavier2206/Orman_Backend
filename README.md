# ORMAN

> Trabajo Final · Diplomado en Desarrollo Web y Aplicaciones Móviles · UAJMS 2026<br>
> Autor: `<Xavier Ortega Mancilla>` · Tutor: `<Nombre del tutor>`

## 1. Descripción

ORMAN es un sistema de gestión inmobiliaria para administrar propietarios, inquilinos, propiedades, unidades, contratos, cuotas, pagos, comprobantes y notificaciones.

Este repositorio contiene únicamente el Backend. Expone la API REST que utilizan el cliente Angular WEB y la aplicación Flutter para Android.

**Sistema desplegado:** `<pendiente / URL pública del Backend>`

## 2. Stack tecnológico

| Componente | Versión | Función |
|---|---:|---|
| Java | 21 | Plataforma de ejecución |
| Spring Boot | 4.1.0 | Aplicación y dependencias base |
| Maven / Maven Wrapper | Maven 3.9.16 · Wrapper 3.3.4 | Compilación y ejecución |
| PostgreSQL | — | Base de datos relacional |
| Spring Data JPA | — | Acceso a datos |
| Hibernate | — | ORM y validación del esquema |
| Flyway | — | Migraciones de base de datos |
| Spring Security | — | Autenticación y autorización |
| JWT (Nimbus JOSE JWT) | 10.8 | Tokens de acceso |
| BCrypt | — | Verificación de contraseñas |
| Bean Validation | — | Validación de solicitudes |
| Lombok | — | Reducción de código repetitivo |
| API REST | — | Comunicación HTTP bajo `/api/v1` |
| WebSocket / STOMP | — | Notificaciones en tiempo real para WEB |
| Firebase Admin SDK / Firebase Cloud Messaging | 9.11.0 | Envío de notificaciones push a Android |

### Seguridad y sesiones

El inicio de sesión es directo: usuario y contraseña se verifican con BCrypt, se crea una sesión por dispositivo y se emiten un access token JWT y un refresh token. El refresh token rota al renovarse la sesión.

En WEB, la API entrega el access token y administra el refresh token mediante una cookie `HttpOnly`. CORS permite solicitudes con credenciales; el cliente web debe enviarlas con `withCredentials`. El refresh con cookie cuenta con protección XSRF y utiliza el encabezado `X-XSRF-TOKEN`.

En Android, el refresh token se devuelve en el cuerpo de la respuesta para que la aplicación gestione su sesión. Las sesiones autenticadas pueden cerrarse individualmente o en conjunto y revocarse por dispositivo. La API expone `POST /api/v1/auth/logout`, `POST /api/v1/auth/logout-all`, `GET /api/v1/auth/sessions`, `DELETE /api/v1/auth/sessions/{sid}` y `GET /api/v1/auth/context`.

### Notificaciones y tiempo real

Las notificaciones se persisten y pueden consultarse mediante REST:

```text
GET   /api/v1/notificaciones
GET   /api/v1/notificaciones/{codnot}
GET   /api/v1/notificaciones/resumen
PATCH /api/v1/notificaciones/{codnot}/leer
```

Para WEB, el endpoint STOMP es `/ws` y la cola privada es `/user/queue/notificaciones`. Para Android, el backend integra Firebase Cloud Messaging y permite registrar o desactivar una instalación push autenticada:

```text
PUT    /api/v1/mobile/push-installation
DELETE /api/v1/mobile/push-installation
```

## 3. Requisitos previos

- JDK 21.
- PostgreSQL.
- Git para clonar el repositorio.
- No se requiere una instalación global de Maven; el proyecto incluye Maven Wrapper.

## 4. DESARROLLO LOCAL

El perfil `local` conecta por defecto con PostgreSQL en `localhost:5432/orman`, sin SSL. El backend escucha en `http://localhost:9090` y permite el origen Angular `http://localhost:4200`.

1. Instala JDK 21 y PostgreSQL. Crea la base local `orman` y configura un usuario local con permiso para que Flyway administre su esquema.
2. Desde la raíz del repositorio, configura las variables del proceso en PowerShell. Sustituye los marcadores por los valores locales propios; no guardes contraseñas ni secretos en Git:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "orman"
$env:DB_SSL_MODE = "disable"
$env:DB_USERNAME = "<usuario-postgresql-local>"
$env:DB_PASSWORD = "<contraseña-postgresql-local>"
$env:JWT_SECRET = "<secreto-local-estable-de-al-menos-32-bytes>"
$env:ORMAN_FRONTEND_URL = "http://localhost:4200"
$env:REFRESH_COOKIE_SECURE = "false"
.\mvnw.cmd spring-boot:run
```

`JWT_SECRET` local debe ser estable para ese entorno y distinto del secreto de producción. `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` son obligatorios; el perfil local aporta defaults seguros para host, puerto, base, SSL, puerto HTTP, cookie y CORS.

Spring Boot no carga `.env` automáticamente. Antes, `application.yml` importaba explícitamente `./.env`; esa importación se retiró para evitar que el archivo local con destino Neon sobreescriba el perfil. `.env.example` sirve solo como plantilla de referencia; define los valores en el entorno del proceso.

Flyway valida y aplica migraciones al iniciar. Hibernate conserva `ddl-auto: validate`. Para compilar sin ejecutar pruebas ni iniciar una conexión a base de datos:

```powershell
.\mvnw.cmd -DskipTests compile
```

## 5. PRODUCCIÓN Y VARIABLES DE ENTORNO

Producción debe iniciar con `SPRING_PROFILES_ACTIVE=prod`. La plataforma debe proporcionar las variables requeridas desde su gestor de secretos; no se guardan valores de producción en YAML, `.env.example` ni en el repositorio. El perfil requiere host, puerto, base, usuario, contraseña, `DB_SSL_MODE`, secreto JWT, origen frontend, `REFRESH_COOKIE_SECURE` y proveedor de almacenamiento; si faltan, Spring falla al resolver la configuración en vez de usar defaults locales. Configura `DB_SSL_MODE=require` y `REFRESH_COOKIE_SECURE=true`.

La plataforma debe exponer el frontend y el backend por HTTPS (normalmente mediante TLS en el ingress o proxy del despliegue); Spring escucha en el puerto `PORT` que le proporcione la plataforma.

Ejemplo de nombres de variables para el entorno de despliegue (sin valores reales):

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<host-postgresql-remoto>
DB_PORT=<puerto-postgresql-remoto>
DB_NAME=<base-postgresql-remota>
DB_USERNAME=<usuario-de-produccion>
DB_PASSWORD=<secreto-del-gestor-de-secretos>
DB_SSL_MODE=require
JWT_SECRET=<secreto-de-produccion-de-al-menos-32-bytes>
ORMAN_FRONTEND_URL=https://<origen-frontend-produccion>
REFRESH_COOKIE_SECURE=true
REFRESH_COOKIE_SAME_SITE=<Strict-Lax-o-None-segun-el-dominio>
ORMAN_STORAGE_PROVIDER=<r2-o-local-segun-el-despliegue>
PORT=<puerto-proporcionado-por-la-plataforma>
```

`ORMAN_FRONTEND_URL` configura el único origen de CORS del perfil. No uses `*`; la configuración existente también rechaza ese valor. El origen de producción debe ser HTTPS. `SameSite` sigue siendo configurable (`Strict`, `Lax` o `None`); `None` requiere `Secure`.

| Variable | Perfil local | Perfil prod |
|---|---|---|
| `DB_HOST` | `localhost` por defecto | Obligatoria, sin default local |
| `DB_PORT` | `5432` por defecto | Obligatoria, sin default local |
| `DB_NAME` | `orman` por defecto | Obligatoria, sin default local |
| `DB_SSL_MODE` | `disable` por defecto | Obligatoria; debe ser `require` |
| `DB_USERNAME`, `DB_PASSWORD` | Obligatoria desde el entorno local | Obligatoria desde la plataforma |
| `JWT_SECRET` | Obligatoria, mínimo 32 bytes | Obligatoria, mínimo 32 bytes y distinta de local |
| `ORMAN_FRONTEND_URL` | `http://localhost:4200` por defecto | Obligatoria; un origen HTTPS |
| `REFRESH_COOKIE_SECURE` | `false` por defecto | Obligatoria; debe ser `true` |
| `REFRESH_COOKIE_SAME_SITE` | `Lax` por defecto | Configurable; `Lax` por defecto |
| `ORMAN_STORAGE_PROVIDER` | `local` por defecto | Obligatoria; configura `r2` o `local` según el despliegue |
| `PORT` | `9090` por defecto | Opcional; `9090` si la plataforma no lo define |

Si se utiliza Cloudflare R2, configura externamente `ORMAN_STORAGE_PROVIDER=r2`, `ORMAN_R2_ENDPOINT`, `ORMAN_R2_BUCKET`, `ORMAN_R2_ACCESS_KEY_ID` y `ORMAN_R2_SECRET_ACCESS_KEY`. Si se habilita Firebase, configura `ORMAN_FIREBASE_ENABLED=true`, `ORMAN_FIREBASE_PROJECT_ID` (o `GOOGLE_CLOUD_PROJECT`) y credenciales externas mediante `ORMAN_FIREBASE_SERVICE_ACCOUNT_JSON` o Application Default Credentials con `GOOGLE_APPLICATION_CREDENTIALS`. No incluyas los JSON ni sus secretos en el repositorio.

`JWT_ISSUER`, `JWT_ACCESS_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`, `REFRESH_COOKIE_NAME`, límites de carga/almacenamiento y cron de tareas conservan sus defaults comunes, todos sobrescribibles mediante variables de entorno. Flyway sigue siendo la única fuente de evolución del esquema.

La configuración sensible se obtiene del entorno del proceso o de la plataforma. Las variables `MAIL_*` y `OTP_*` que puedan existir en un `.env` local no se cargan desde ese archivo; no aparecen referenciadas en la configuración activa del backend.

Las reglas funcionales de fechas y horas utilizan la zona `America/La_Paz`, correspondiente a la hora de Bolivia.

## 6. Estructura del repositorio

```text
.
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/orman/backend/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
├── .env.example
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

El backend es un monolito modular organizado por funcionalidades. Los módulos separan controladores, servicios, repositorios, entidades, DTO, mappers y validadores, con manejo centralizado de errores y configuración de seguridad. Las entidades JPA no se exponen directamente por la API.

Se conservan los módulos de roles, asignación de roles, menús, procesos y sus asignaciones. Las rutas de almacenamiento de fotos y documentos son configurables y usan `./storage` como base predeterminada.

## 7. Roles y credenciales de prueba

| Rol | Usuario | Contraseña |
|---|---|---|
| PROPIETARIO | `<usuario de prueba>` | `<ver documento entregado a la coordinación>` |
| INQUILINO | `<usuario de prueba>` | `<ver documento entregado a la coordinación>` |

Las credenciales de prueba se entregan por un canal privado. No se incluyen contraseñas ni secretos en este repositorio.

## 8. Pruebas

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean test
```

Linux/macOS:

```bash
./mvnw test
./mvnw clean test
```

La suite cubre lógica de negocio, persistencia y restricciones de base de datos, contratos HTTP, validaciones, manejo de errores, autenticación y autorización, JWT, sesiones y renovación, roles, propiedades, contratos, cuotas, pagos y comprobantes. También incluye pruebas de notificaciones REST, WebSocket, FCM, instalaciones push y reglas de zona horaria.

## 9. Despliegue

Spring Boot puede empaquetarse como un JAR con Maven Wrapper. En el entorno de destino deben configurarse PostgreSQL y las variables necesarias; los secretos y las credenciales de Firebase se administran fuera del JAR.

Las direcciones públicas se completarán cuando se realice el despliegue:

- Backend público: `<pendiente / URL>`
- Frontend web: `<pendiente / URL>`
- Aplicación móvil: `<pendiente / enlace APK>`

## 10. Licencia

Uso académico. Todos los derechos reservados por el autor.
