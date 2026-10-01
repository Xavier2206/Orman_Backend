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

## 4. Instalación local

1. Clona el repositorio y entra en su carpeta:

```bash
git clone <url-del-repositorio-backend>
cd <carpeta-del-backend>
```

2. Crea una base de datos PostgreSQL llamada `orman` y prepara las credenciales de conexión.
3. Copia `.env.example` como `.env` y completa los valores locales descritos en la sección 5. El archivo `.env` es opcional si configuras las mismas variables en el entorno del sistema.
4. Desde la raíz del proyecto, inicia la aplicación:

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Flyway valida y aplica las migraciones pendientes al iniciar. El servidor queda disponible en `http://localhost:9090`; el prefijo de la API es `/api/v1`.

Para generar el JAR:

```text
.\mvnw.cmd clean package
./mvnw clean package
```

## 5. Variables de entorno

`application.yml` importa opcionalmente el archivo local `.env`. Las variables del entorno del sistema tienen prioridad. Las variables requeridas para iniciar el backend son las credenciales de PostgreSQL y `JWT_SECRET`.

| Variable | Requerida | Valor predeterminado o uso |
|---|---|---|
| `DB_HOST` | No | `localhost` |
| `DB_PORT` | No | `5432` |
| `DB_NAME` | No | `orman` |
| `DB_USERNAME` | Sí | Usuario de PostgreSQL |
| `DB_PASSWORD` | Sí | Contraseña de PostgreSQL |
| `JWT_SECRET` | Sí | Secreto local para firmar JWT; usar al menos 32 bytes |
| `JWT_ISSUER` | No | `orman-backend` |
| `JWT_ACCESS_EXPIRATION_MINUTES` | No | `15` |
| `JWT_REFRESH_EXPIRATION_DAYS` | No | `30` |
| `REFRESH_COOKIE_NAME` | No | `orman_refresh` |
| `REFRESH_COOKIE_SECURE` | No | `false` en configuración local |
| `REFRESH_COOKIE_SAME_SITE` | No | `Lax` |
| `ORMAN_FRONTEND_URL` | No | `http://localhost:4200`; origen WEB permitido por CORS |
| `ORMAN_FIREBASE_ENABLED` | No | `false`; habilita el envío push |
| `ORMAN_FIREBASE_PROJECT_ID` | Al habilitar Firebase | ID del proyecto; usa `GOOGLE_CLOUD_PROJECT` como alternativa |
| `GOOGLE_CLOUD_PROJECT` | No | Alternativa para el ID de proyecto Firebase |
| `GOOGLE_APPLICATION_CREDENTIALS` | Al habilitar Firebase* | Ruta a credenciales de Google para Application Default Credentials |

Además, `application.yml` define valores predeterminados para límites de carga y almacenamiento (`ORMAN_MULTIPART_MAX_*`, `PERSONA_PHOTO_*`, `PROPERTY_PHOTO_*`, `UNIT_PHOTO_*`, `CONTRACT_DOCUMENT_*`, `PAYMENT_IMAGE_*`) y para los programadores `NOTIFICATION_SCHEDULER_CRON` y `CONTRACT_SCHEDULER_CRON`.

* Si se habilita Firebase, configura `GOOGLE_APPLICATION_CREDENTIALS` con un JSON de cuenta de servicio o proporciona otra fuente válida de Application Default Credentials. El JSON es privado: mantenlo fuera del repositorio, no lo copies a `src/main/resources` y no lo subas a GitHub. Por ejemplo, en PowerShell:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "C:\ORMAN_SECRETS\firebase-service-account.json"
```

Flyway administra el esquema y Hibernate usa `ddl-auto: validate`. La migración `V22__fijar_catalogo_roles.sql` fija el catálogo funcional en `PROPIETARIO` e `INQUILINO`; la migración `V23__retirar_otp_login.sql` retira la persistencia del mecanismo de segundo factor.

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
