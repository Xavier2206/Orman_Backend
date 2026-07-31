# Registro de cambios

Este archivo registra cambios relevantes de ORMAN-BACKEND por fase, con una estructura inspirada en Keep a Changelog.

## Sin publicar

No hay cambios adicionales registrados fuera de las fases cerradas.

## Fase 06 — 2026-07-31

### Agregado

- Migración Flyway V3 para `usuarios`, con `login` como PK, `codper` único y FK a `personas` con `ON DELETE RESTRICT`.
- Entidad `Usuario`, relación JPA uno a uno unidireccional y `UsuarioRepository` en el módulo `user`.
- Pruebas de integración de esquema, defaults, restricciones, relación, borrado restringido y ausencia de cascada desde Usuario hacia Persona.

### Modificado

- Pruebas de integración existentes actualizadas mínimamente para reconocer V3 y la tabla `usuarios`.
- Plan, documentación de Fase 06, índices, teoría de Etapa 2, documentación de base de datos y README.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 65 pruebas, 0 fallos y 0 errores.
- `contextLoads` pasó; PostgreSQL 17.6 conectó; Flyway validó V1–V3 y aplicó V3; Hibernate validó el esquema con `ddl-auto=validate`.
- Confirmado: no se modificaron V1, V2 ni `personas`; no se añadieron dependencias, CRUD, BCrypt, autenticación, JWT, roles ni sesiones.

## Fase 05 — 2026-07-31

### Agregado

- CRUD HTTP de Persona, paginación, validaciones, manejo de CI duplicado y operaciones idempotentes de activación y desactivación.
- Pruebas específicas de mapper, servicio con mocks, MVC y flujo de integración con PostgreSQL.
- Teoría de la Etapa 2 y guía Postman de Persona actualizada.

### Corregido

- La creación ahora refresca la entidad después de `saveAndFlush` para que `PersonaResponse` incluya `estado` y `fechaRegistro` generados por PostgreSQL cuando se omiten en la solicitud.

### Verificación

- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 56 pruebas, 0 fallos y 0 errores.
- `contextLoads` pasó; PostgreSQL conectó; Flyway validó V1 y V2 sin aplicar migraciones; Hibernate validó el esquema.
- Confirmado que no existe V3, no se alteró `personas`, no se agregaron tablas, dependencias ni código en `security`.

## Fase 04 — 2026-07-29

### Agregado

- Entidad `Persona` y `PersonaRepository` en el módulo `person`.
- Pruebas de persistencia y restricciones contra PostgreSQL real.
- Migración V2 para hacer obligatorio `personas.fecha_registro`.

### Corregido

- V1 había creado `fecha_registro` nullable; tras confirmar que no existían valores nulos, V2 aplicó la nulabilidad aprobada sin modificar la migración ya aplicada.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**, 17 pruebas, 0 fallos y 0 errores.
- Flyway registró V1 y V2; Hibernate validó el esquema sin DDL y `contextLoads` pasó.
- El esquema público final contiene únicamente `flyway_schema_history` y `personas`.

## Fase 03 — 2026-07-27

### Agregado

- Contrato de errores RFC 9457 basado en `ProblemDetail`, con código interno, instante, ruta y trazabilidad local.
- Excepciones reutilizables para recurso inexistente, conflicto y regla de negocio.
- Manejo global de validación, JSON inválido y errores inesperados sin exponer detalles internos.
- Pruebas MVC aisladas para los casos principales del contrato de error.

### Modificado

- Habilitada la propiedad oficial `spring.mvc.problemdetails.enabled`.
- Actualizados README, plan, índices, arquitectura y teoría de la Etapa 1.

### Verificación

- `.\mvnw.cmd clean test`: **BUILD SUCCESS**; 7 pruebas, 0 fallos y 0 errores.
- `contextLoads` sigue validando PostgreSQL, Hikari, Flyway e Hibernate.
- Confirmado que no se agregaron migraciones SQL, tablas de negocio ni dependencias.

### Corregido

- El primer intento de pruebas reveló que el manejador integrado de Spring tenía prioridad para validación y JSON inválido. Se aplicó `@Order(Ordered.HIGHEST_PRECEDENCE)` al consejo global y la validación final pasó.

## Fase 02 — 2026-07-27

### Agregado

- Configuración de datasource PostgreSQL mediante variables de entorno.
- Configuración inicial de Hibernate en modo `validate` y Flyway con migraciones en `classpath:db/migration`.
- `.env.example` sin credenciales reales y reglas para ignorar `.env`.

### Modificado

- `.env.example` ahora usa un marcador de contraseña y no contiene una credencial concreta.
- README, plan e índice de fases actualizados para registrar el cierre de la Fase 02.

### Documentación

- Registrada la causa del placeholder literal y su corrección mediante importación de variables de usuario al proceso que ejecuta Maven.
- Confirmado que las credenciales siguen siendo externas y no se almacenan en el repositorio.

### Verificación

- Compilación principal y de pruebas correcta.
- El árbol de dependencias confirma JPA, driver PostgreSQL, Flyway Core y `flyway-database-postgresql`.
- `.\mvnw.cmd clean`: **BUILD SUCCESS**.
- `.\mvnw.cmd test`: **BUILD SUCCESS**; `contextLoads` ejecutó 1 prueba, 0 fallos y 0 errores.
- PostgreSQL aceptó la conexión; HikariPool inició; Flyway validó 0 migraciones y creó `flyway_schema_history`.
- La consulta JDBC de solo lectura confirmó que `flyway_schema_history` es la única tabla pública y que no hay tablas de negocio.

## Fase 00 — 2026-07-27

### Agregado

- Reglas permanentes de trabajo en `AGENTS.md`.
- Plan general inicial de etapas y fases, corregido posteriormente para conservar únicamente las fases 00 a 17 confirmadas.
- Documento teórico de la Etapa 1 y registro de la Fase 00.
- Cinco ADR para arquitectura, esquema, Git, configuración y Lombok.
- Documentación inicial de arquitectura y base de datos.
- Índices documentales y README principal.
- Configuración YAML del nombre de aplicación y puerto `9090`.

### Modificado

- Formato principal de configuración migrado de properties a YAML.

### Eliminado

- `src/main/resources/application.properties`, después de trasladar su única propiedad.

### Corregido

- No se registran correcciones de código funcional.

### Documentación

- Registrados alcance, decisiones, modelo inicial, tablas postergadas, reglas de seguridad y secuencia de trabajo.
- Documentadas las restricciones de Git y los criterios de cierre por fase.
- Eliminadas temporalmente del plan confirmado las fases de propiedades, unidades y disponibilidad pública por falta de información funcional proporcionada.
- Renumeradas las fases de autenticación, autorización, calidad y producción desde la Fase 09 hasta la Fase 17.
- Incorporada una etapa futura de módulos adicionales con estado `PENDIENTE DE ANÁLISIS`, sin fases numeradas.
- Corregidas las referencias internas del objetivo y las dependencias generales del plan.

### Verificación

- Verificados 18 documentos Markdown, incluido `HELP.md`: ninguno vacío y ningún enlace relativo roto.
- Confirmado que solo existen la clase principal y la prueba Java iniciales; no se crearon clases de negocio.
- Confirmado `application.yml` como único recurso de configuración, con puerto `9090`.
- Maven compiló las clases principal y de prueba; `contextLoads` terminó con 1 error por ausencia de datasource, configuración reservada para la Fase 02.
- Maven Wrapper no pudo iniciar Maven debido a un error de su script PowerShell; se verificó adicionalmente con Maven 3.9.16 instalado.

## Fase 01 — 2026-07-27

### Modificado

- Normalizados los metadatos Maven, el empaquetado JAR y la codificación UTF-8.
- Renombradas la clase principal y la prueba inicial como `OrmanBackendApplication` y `OrmanBackendApplicationTests`.
- Actualizado el nombre de aplicación a `ORMAN-BACKEND` en `application.yml`.
- Corregido `mvnw.cmd` para manejar correctamente un directorio Maven local que no sea enlace simbólico.

### Eliminado

- Configuración personalizada e innecesaria de `maven-compiler-plugin`; se conserva la configuración administrada por Spring Boot.
- Archivos `BackendApplication.java` y `BackendApplicationTests.java`, reemplazados por sus nombres normalizados.

### Documentación

- Registrado el diagnóstico y la corrección del Maven Wrapper.
- Creado el documento de la Fase 01 y actualizados el plan, índices, README y teoría de la Etapa 1.

### Verificación

- Confirmados Java 21, Maven 3.9.16, Spring Boot 4.1.0, paquete base y puerto 9090.
- El Wrapper funciona desde PowerShell y mediante `cmd /c`.
- `clean test-compile` compila correctamente clases principales y de prueba.
- `contextLoads` sigue fallando únicamente por falta de datasource, reservada para la Fase 02.
