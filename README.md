# ORMAN Backend

Backend del sistema ORMAN desarrollado con Java y Spring Boot para la gestión de personas, usuarios, roles, propiedades, contratos, pagos, notificaciones y procesos relacionados con la administración inmobiliaria.

El proyecto expone una API REST utilizada por las aplicaciones cliente y centraliza la lógica de negocio, seguridad, persistencia y acceso a datos del sistema.

## Tecnologías

* Java 21
* Spring Boot 4.1.0
* Maven y Maven Wrapper
* PostgreSQL
* Spring Data JPA
* Hibernate
* Flyway
* Bean Validation
* Spring Security
* JWT
* BCrypt
* Lombok
* API REST
* Empaquetado JAR

## Arquitectura

El backend utiliza una arquitectura de monolito modular organizada por funcionalidades.

La aplicación separa las responsabilidades principales en:

* controladores REST;
* servicios de negocio;
* repositorios;
* entidades JPA;
* DTO de entrada y salida;
* mappers;
* validaciones;
* manejo centralizado de errores;
* seguridad y autenticación.

Las entidades de persistencia no se exponen directamente mediante la API.

## Funcionalidades principales

El sistema incluye funcionalidades para:

* gestión de personas;
* gestión de usuarios;
* administración de roles;
* asignación de roles a usuarios;
* autenticación de usuarios;
* sesiones por dispositivo;
* autenticación mediante JWT;
* renovación mediante refresh token;
* gestión de menús y procesos;
* administración de propiedades y unidades;
* registro de fotografías asociadas;
* gestión de contratos;
* administración de cuotas;
* registro de pagos;
* emisión y gestión de recibos;
* gestión de cuentas de pago;
* notificaciones;
* control de acceso mediante roles.

## API REST

Los endpoints utilizan el prefijo:

```text
/api/v1
```

El servidor se ejecuta por defecto en:

```text
http://localhost:9090
```

Las respuestas HTTP utilizan DTO específicos y los errores se representan utilizando `ProblemDetail`.

## Requisitos

Para ejecutar el proyecto se necesita:

* JDK 21;
* PostgreSQL;
* Maven Wrapper incluido en el proyecto.

No es necesario tener Maven instalado globalmente.

## Configuración

La aplicación utiliza variables de entorno para la configuración sensible.

Las principales variables son:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
OTP_HMAC_SECRET
MAIL_USERNAME
MAIL_PASSWORD
ORMAN_FRONTEND_URL
```

El archivo:

```text
.env.example
```

sirve como referencia para configurar el entorno local.

El archivo real:

```text
.env
```

no forma parte del repositorio y no debe contenerse en Git.

## Configuración de PostgreSQL

La conexión con PostgreSQL utiliza las siguientes variables:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Ejemplo para PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "orman"
$env:DB_USERNAME = "<usuario>"
$env:DB_PASSWORD = "<contraseña>"
```

Los cambios de estructura de la base de datos son administrados mediante Flyway.

Hibernate se utiliza para validar el esquema existente y no para generar automáticamente las tablas.

## Configuración de JWT

`JWT_SECRET` es necesario para la generación y validación de tokens JWT.

Debe configurarse mediante una variable de entorno o mediante un archivo `.env` local que no esté versionado.

Ejemplo en PowerShell:

```powershell
$env:JWT_SECRET = "<secreto-seguro>"
```

Para generar un valor aleatorio local puede utilizarse:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

El valor generado no debe publicarse ni incluirse en el repositorio.

## Seguridad

El backend implementa mecanismos de seguridad para proteger las operaciones y los datos del sistema.

Entre ellos:

* contraseñas almacenadas mediante BCrypt;
* autenticación mediante JWT;
* refresh tokens;
* sesiones diferenciadas por dispositivo;
* autorización basada en roles;
* protección de endpoints;
* configuración CORS;
* protección de información sensible;
* códigos OTP cuando corresponde.

Las contraseñas, tokens, secretos y credenciales no deben almacenarse directamente en el código fuente.

## Ejecución

### Windows

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Para compilar el proyecto:

```powershell
.\mvnw.cmd clean package
```

Para iniciar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux y macOS

Para ejecutar las pruebas:

```bash
./mvnw test
```

Para compilar:

```bash
./mvnw clean package
```

Para iniciar la aplicación:

```bash
./mvnw spring-boot:run
```

## Base de datos

PostgreSQL es la base de datos utilizada por el sistema.

Flyway administra las migraciones y mantiene el historial de cambios del esquema.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Al iniciar la aplicación, Flyway valida y aplica automáticamente las migraciones pendientes cuando corresponde.

## Pruebas

El proyecto incluye pruebas para validar las diferentes capas y reglas del backend.

Para ejecutar la suite:

```powershell
.\mvnw.cmd clean test
```

Las pruebas permiten validar, entre otros aspectos:

* lógica de negocio;
* persistencia;
* contratos HTTP;
* validaciones;
* seguridad;
* restricciones de base de datos;
* manejo de errores.

## Archivos excluidos del repositorio

Por seguridad y limpieza del proyecto no se incluyen en Git:

```text
.env
docs/
storage/
target/
.idea/
.vscode/
```

Tampoco se incluyen archivos temporales, logs, credenciales, claves privadas ni documentos almacenados durante la ejecución de la aplicación.

## Estructura general

```text
src/
├── main/
│   ├── java/
│   │   └── com/orman/backend/
│   └── resources/
│       ├── application.yml
│       └── db/
│           └── migration/
└── test/
    ├── java/
    └── resources/
```

## Compilación

Para generar el archivo ejecutable:

```powershell
.\mvnw.cmd clean package
```

El artefacto generado se almacena en:

```text
target/
```

La carpeta `target/` es generada automáticamente y no se incluye en el repositorio.
