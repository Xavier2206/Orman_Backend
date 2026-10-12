# ORMAN

> Trabajo Final · Diplomado en Desarrollo Web y Aplicaciones Móviles · UAJMS 2026<br>
> Autor: Xavier Ortega Mancilla

## 1. Descripción

ORMAN es un sistema de gestión inmobiliaria para administrar propietarios, inquilinos, propiedades, unidades, contratos, cuotas, pagos, comprobantes y notificaciones.

Este repositorio contiene únicamente el Backend. Expone la API REST que utilizan el cliente Angular WEB y la aplicación Flutter para Android.

**Direcciones públicas proporcionadas por el autor:**

- [Backend](https://orman-backend-e09w.onrender.com).
- [Salud pública](https://orman-backend-e09w.onrender.com/api/v1/salud).
- [Aplicación web para el Propietario](https://orman-frontend.ortegaxavier75.workers.dev/).
- [APK Android para el Inquilino](https://github.com/Xavier2206/Orman_Mobile/raw/refs/heads/main/build/entrega/ORMAN-1.0.0.apk), publicado en el repositorio móvil según confirmación del autor; no es una Release de GitHub.

Estas direcciones se documentan sin afirmar una verificación de disponibilidad en esta actualización.

**Repositorios relacionados:** [Backend](https://github.com/Xavier2206/Orman_Backend), [Frontend](https://github.com/Xavier2206/Orman_Frontend) y [Móvil](https://github.com/Xavier2206/Orman_Mobile).

## 2. Stack tecnológico

| Componente | Versión | Función |
|---|---:|---|
| Java | 21 | Plataforma de ejecución |
| Spring Boot | 4.1.0 | Aplicación y dependencias base |
| Maven / Maven Wrapper | Maven 3.9.16 · Wrapper 3.3.4 | Compilación y ejecución |
| PostgreSQL | 18 en Neon (producción, dato proporcionado por el autor) | Base de datos relacional; el POM no fija la versión del servidor |
| Spring Data JPA | Gestionada por Spring Boot | Acceso a datos |
| Hibernate | Gestionada por Spring Boot | ORM y validación del esquema |
| Flyway | Gestionada por Spring Boot | Migraciones de base de datos |
| Spring Security | Gestionada por Spring Boot | Autenticación y autorización |
| JWT (Nimbus JOSE JWT) | 10.8 | Tokens de acceso |
| BCrypt / spring-security-crypto | Gestionada por Spring Boot | Verificación de contraseñas |
| Bean Validation | Gestionada por Spring Boot | Validación de solicitudes |
| Lombok | Gestionada por Spring Boot | Reducción de código repetitivo |
| Driver JDBC PostgreSQL | Gestionada por Spring Boot | Conexión a PostgreSQL; su versión no es la del servidor |
| AWS SDK S3 / url-connection-client | 2.55.8 | Integración con Cloudflare R2 |
| Apache PDFBox | 3.0.8 | Procesamiento de documentos PDF |
| API REST | — | Comunicación HTTP bajo `/api/v1` |
| WebSocket / STOMP | — | Notificaciones en tiempo real para WEB |
| Firebase Admin SDK / Firebase Cloud Messaging | 9.11.0 | Envío de notificaciones push a Android |

Java, Spring Boot, Nimbus JOSE JWT, AWS SDK, PDFBox y Firebase Admin tienen versiones declaradas en `pom.xml`. Maven 3.9.16 y Wrapper 3.3.4 se declaran en `.mvn/wrapper/maven-wrapper.properties` (tipo `only-script`). Las dependencias gestionadas no tienen una versión propia fijada en el POM del proyecto; no se les atribuye aquí una versión exacta sin resolverlas. El empaquetado es JAR; el Dockerfile utiliza Eclipse Temurin 21, con JDK para construir y JRE para ejecutar.

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

1. Instala JDK 21, PostgreSQL y Git. Crea la base local `orman` y configura un usuario local con permiso para que Flyway administre su esquema.
2. Clona el backend y entra en su directorio:

```powershell
git clone https://github.com/Xavier2206/Orman_Backend.git
cd Orman_Backend
```

3. Desde la raíz del repositorio, configura las variables del proceso en PowerShell. Sustituye los marcadores por los valores locales propios; no guardes contraseñas ni secretos en Git:

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

La configuración actual no importa `.env`; Spring Boot no lo carga automáticamente. `.env.example` sirve solo como plantilla de referencia; define los valores en el entorno del proceso. Si ese proceso ya contiene variables de producción, sustitúyelas por los valores locales antes de iniciar.

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
ORMAN_FRONTEND_URL=https://orman-frontend.ortegaxavier75.workers.dev
REFRESH_COOKIE_SECURE=true
REFRESH_COOKIE_SAME_SITE=None
ORMAN_STORAGE_PROVIDER=r2
PORT=10000
```

`ORMAN_FRONTEND_URL` configura el único origen de CORS del perfil: debe coincidir exactamente, sin barra final, con `https://orman-frontend.ortegaxavier75.workers.dev`. No uses `*`; el código lo rechaza. CORS permite credenciales y el encabezado `X-XSRF-TOKEN`, que también expone al cliente.

El frontend en `workers.dev` y el backend en `onrender.com` están en sitios distintos. Para enviar la cookie de refresh en solicitudes entre estos sitios, configura `REFRESH_COOKIE_SAME_SITE=None` y `REFRESH_COOKIE_SECURE=true`, además de credenciales en el cliente. El default común del código es `Lax`, no `None`; `local` aporta `secure=false`, mientras que `prod` exige un valor explícito. El código rechaza `None` sin `Secure`. Estos valores son requisitos para los dominios publicados, no una confirmación de las variables guardadas en Render. Las políticas del navegador sobre cookies de terceros pueden seguir bloqueándolas; debe comprobarse login, renovación y XSRF desde el frontend público. Véase la [documentación de cookies de MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie).

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
| `REFRESH_COOKIE_SAME_SITE` | `Lax` por defecto | Default común `Lax`; requiere `None` para los sitios públicos indicados |
| `ORMAN_STORAGE_PROVIDER` | `local` por defecto | Obligatoria; utiliza `r2` para persistir archivos en Render Free |
| `PORT` | `9090` por defecto | Opcional; `9090` si la plataforma no lo define |

Si se utiliza Cloudflare R2, configura externamente `ORMAN_STORAGE_PROVIDER=r2`, `ORMAN_R2_ENDPOINT`, `ORMAN_R2_BUCKET`, `ORMAN_R2_ACCESS_KEY_ID` y `ORMAN_R2_SECRET_ACCESS_KEY`. Si se habilita Firebase, configura `ORMAN_FIREBASE_ENABLED=true`, `ORMAN_FIREBASE_PROJECT_ID` (o `GOOGLE_CLOUD_PROJECT`) y credenciales externas mediante `ORMAN_FIREBASE_SERVICE_ACCOUNT_JSON` o Application Default Credentials con `GOOGLE_APPLICATION_CREDENTIALS`. No incluyas los JSON ni sus secretos en el repositorio.

El endpoint R2 debe ser HTTPS y el bucket debe estar configurado. Ambas integraciones están disponibles en el código, pero su activación y funcionamiento en Render quedan pendientes de confirmar. Firebase está deshabilitado por defecto (`ORMAN_FIREBASE_ENABLED=false`); si se usa ADC, no definas `ORMAN_FIREBASE_SERVICE_ACCOUNT_JSON` vacío, pues el código lo considera una configuración inválida. Firebase se utiliza para notificaciones push Android; ORMAN autentica a sus usuarios mediante su propio backend, no mediante Firebase Authentication.

### Neon PostgreSQL 18

Para producción se utiliza Neon PostgreSQL 18, según la información proporcionada por el autor. Obtén del panel privado de Neon los datos de conexión del proyecto y rama correspondientes y configúralos únicamente en el gestor de secretos de Render: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`. El backend construye la URL JDBC con esas variables y `DB_SSL_MODE`; no requiere publicar una cadena real de conexión. La cuenta debe permitir a Flyway aplicar las migraciones en el esquema de destino.

Configura `DB_SSL_MODE=require`: exige cifrado de la conexión, pero no verifica la identidad ni el nombre del servidor. No equivale a `verify-full`, según la [documentación oficial de pgJDBC](https://jdbc.postgresql.org/documentation/ssl/). No publiques host privado, usuarios, contraseñas ni cadenas de conexión. Confirma privadamente la versión del servidor y el destino configurado en Neon/Render; estos datos no se deducen del POM.

`JWT_ISSUER`, `JWT_ACCESS_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`, `REFRESH_COOKIE_NAME`, límites de carga/almacenamiento y cron de tareas conservan sus defaults comunes, todos sobrescribibles mediante variables de entorno. Flyway sigue siendo la única fuente de evolución del esquema.

La configuración sensible se obtiene del entorno del proceso o de la plataforma.

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
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
│       ├── java/
│       └── resources/application.yml
├── .env.example
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

El backend es un monolito modular organizado por funcionalidades. Los módulos separan controladores, servicios, repositorios, entidades, DTO, mappers y validadores, con manejo centralizado de errores y configuración de seguridad. Las entidades JPA no se exponen directamente por la API.

Se conservan los módulos de roles, asignación de roles, menús, procesos y sus asignaciones. Las rutas de almacenamiento de fotos y documentos son configurables y usan `./storage` como base predeterminada.

## 7. Roles y credenciales de prueba

Las cuentas de prueba para la evaluación académica se proporcionan en el apartado A4.4 del documento entregado al tribunal. La aplicación web corresponde al Propietario y la aplicación móvil al Inquilino.

## 8. Pruebas

Requieren JDK 21, Maven Wrapper (y acceso a sus descargas si no están en caché) y PostgreSQL real accesible. `src/test/resources/application.yml` toma `DB_HOST`, `DB_PORT` y `DB_NAME` con defaults `localhost`, `5432` y `orman`; `DB_USERNAME` y `DB_PASSWORD` son obligatorias. Esa URL de pruebas no incluye `DB_SSL_MODE`; se recomienda una base local dedicada a pruebas, con permiso para aplicar migraciones Flyway, nunca la base de producción. La suite escribe datos de prueba y archivos bajo `target/test-*`.

Configura esas variables de proceso apuntando a la base de pruebas antes de ejecutar los comandos. El YAML de pruebas aporta configuración JWT y cookies propia; evita heredar `SPRING_PROFILES_ACTIVE=prod` o variables de producción. No se usan H2 ni Testcontainers y no se requiere Docker para ejecutar la suite con PostgreSQL instalado. Docker solo es necesario si decides construir la imagen localmente. Las pruebas de Firebase y R2 utilizan dobles de prueba; no requieren credenciales reales de esos servicios.

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

### Render con el Dockerfile existente

Los siguientes pasos son instrucciones para el operador; no se ejecutaron durante esta actualización del README.

1. En Render, crea un **Web Service**, conecta la cuenta de GitHub y selecciona `Xavier2206/Orman_Backend`, rama `main`. Si el servicio ya existe, revisa su repositorio y rama en Settings.
2. Selecciona **Docker** como runtime. Mantén la raíz del repositorio como contexto (Root Directory sin subdirectorio), Dockerfile Path `./Dockerfile`. No se necesita un comando Maven adicional de construcción ni sustituir el arranque del contenedor. Véase [Docker en Render](https://render.com/docs/docker).
3. En Environment, configura `SPRING_PROFILES_ACTIVE=prod` y todas las variables de la sección 5: conexión privada a Neon, SSL, secreto JWT, origen web, cookies y R2 con sus cuatro variables. Si se activa Firebase, añade sus variables y credenciales externas; no guardes archivos privados en Git.
4. Configura `PORT=10000` para alinear el puerto de escucha con `EXPOSE 10000`. Render proporciona `PORT` (10000 por defecto); los perfiles usan `${PORT:9090}`. `EXPOSE` declara el puerto de la imagen, pero no cambia el de Spring. Localmente el default sigue siendo 9090. El proxy de Render proporciona el acceso público HTTPS. Véase [puertos de Web Services](https://render.com/docs/web-services#port-binding).
5. Define Health Check Path `/api/v1/salud`. Inicia el despliegue y revisa el arranque y las migraciones en los logs sin divulgar información sensible. Flyway aplica las migraciones al iniciar; Hibernate valida el esquema.
6. Espera el estado **Live** y comprueba la ruta pública de salud con el comando documentado abajo. Si el servicio gratuito estaba inactivo, espera su arranque y repite la consulta. El estado Live por sí solo no verifica login, cookies, archivos ni notificaciones.
7. Para futuras actualizaciones, **Auto-Deploy: On Commit** despliega los cambios publicados en `main` si está habilitado con el proveedor Git conectado. **After CI Checks Pass** depende de checks configurados; no demuestra que este Dockerfile ejecute pruebas. Para actualizar manualmente, usa **Manual Deploy → Deploy latest commit**. Confirma la opción vigente en el panel; no se afirma conocerla. Véase [despliegues de Render](https://render.com/docs/deploys).

El Dockerfile construye con `./mvnw -B -ntp clean package -DskipTests`, copia `backend-0.0.1-SNAPSHOT.jar` a `/app/app.jar` y arranca `java -jar /app/app.jar` como usuario sin privilegios. **La construcción omite la ejecución de las pruebas**; no se afirma que se hayan ejecutado por separado ni se aportan resultados nuevos.

### Salud pública y alcance de la comprobación

`GET` y `HEAD /api/v1/salud` son públicos y no requieren autenticación; pueden utilizarse con monitores HTTP como UptimeRobot. Ambos comprueban el backend y el acceso a PostgreSQL mediante una consulta mínima, y responden `200` si está disponible o `503` si falla. `HEAD` no devuelve cuerpo; `GET` devuelve el siguiente JSON. Esta documentación describe el código y no confirma que la corrección esté desplegada:

| Resultado de la comprobación | HTTP | Respuesta |
|---|---|---|
| Base de datos disponible | `200 OK` | `{"estado":"UP","baseDatos":"UP"}` |
| Comprobación fallida o error de acceso a datos | `503 Service Unavailable` | `{"estado":"DOWN","baseDatos":"DOWN"}` |

Comprobación puntual desde PowerShell (comando documentado, no ejecutado aquí):

```powershell
curl.exe -i https://orman-backend-e09w.onrender.com/api/v1/salud
curl.exe -I https://orman-backend-e09w.onrender.com/api/v1/salud
```

Una respuesta de salud describe ese instante y el acceso básico a PostgreSQL; no verifica R2, Firebase ni todos los flujos funcionales. No constituye monitoreo continuo ni demuestra RNF-05. Para evaluar disponibilidad se necesitan observaciones sostenidas y evidencia del periodo medido; no se confirma aquí un monitor activo.

### Limitaciones de Render Free y persistencia

Render Free suspende el servicio tras 15 minutos sin tráfico y el siguiente acceso puede tardar aproximadamente un minuto en reactivarlo. Comparte 750 horas gratuitas por workspace al mes; agotar esa cuota suspende los servicios gratuitos hasta el mes siguiente. También existen límites de ancho de banda y minutos de construcción. Estas condiciones afectan las respuestas y la ejecución de tareas programadas durante la suspensión. Véase [Render Free](https://render.com/docs/free).

El sistema de archivos local es efímero: los archivos escritos se pierden al desplegar, reiniciar o suspender el servicio; Free no admite discos persistentes. Aunque el Dockerfile crea `/app/storage` y el código admite almacenamiento `local`, no es una solución persistente de producción en ese plan. Las fotos, documentos y comprobantes que deban conservarse requieren almacenamiento externo, mediante la integración R2 disponible. Los datos relacionales se almacenan en Neon, fuera del contenedor.

### Datos pendientes de confirmar en el entorno

Revisar privadamente en Render la rama y commit desplegados, perfil `prod`, `PORT`, origen CORS, `REFRESH_COOKIE_SECURE=true`, `REFRESH_COOKIE_SAME_SITE=None`, conexión a Neon y versión PostgreSQL 18, configuración de R2, activación y credenciales de Firebase, modalidad Auto-Deploy y ruta de salud. Confirmar desde el navegador público login, renovación y XSRF, y comprobar persistencia de archivos y entrega push si se habilitó Firebase. El código y estas instrucciones no acreditan los valores actualmente guardados en el panel ni la disponibilidad continua.

## 10. Licencia

Uso académico. Todos los derechos reservados por el autor.
