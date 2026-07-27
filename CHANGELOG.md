# Registro de cambios

Este archivo registra cambios relevantes de ORMAN-BACKEND por fase, con una estructura inspirada en Keep a Changelog.

## Sin publicar

No hay cambios adicionales registrados fuera de la Fase 02 bloqueada.

## Fase 02 — 2026-07-27

### Agregado

- Configuración de datasource PostgreSQL mediante variables de entorno.
- Configuración inicial de Hibernate en modo `validate` y Flyway con migraciones en `classpath:db/migration`.
- `.env.example` sin credenciales reales y reglas para ignorar `.env`.

### Modificado

- README, plan, índices, teoría de la Etapa 1 y documentación de base de datos para registrar el bloqueo de la Fase 02.

### Documentación

- Registrado que PostgreSQL local responde, pero faltan credenciales válidas para validar conexión, Flyway y `contextLoads`.

### Verificación

- Compilación principal y de pruebas correcta.
- El árbol de dependencias confirma JPA, driver PostgreSQL, Flyway Core y `flyway-database-postgresql`.
- `contextLoads` llega al datasource y falla por autenticación ante credenciales de entorno ausentes.

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
