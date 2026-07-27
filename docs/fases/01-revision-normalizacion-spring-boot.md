# Fase 01 — Revisión y normalización del proyecto Spring Boot

## Identificación

- **Estado:** `COMPLETADA`
- **Fecha de inicio:** 2026-07-27
- **Fecha de finalización:** 2026-07-27

## Objetivo

Revisar y normalizar la estructura técnica inicial de Spring Boot para dejarla preparada para la configuración de PostgreSQL y Flyway en la Fase 02, sin implementar funcionalidades de negocio ni infraestructura de persistencia.

## Alcance

- Revisar Maven, Java 21, Spring Boot, dependencias, metadatos y empaquetado.
- Normalizar el nombre lógico del proyecto, la configuración mínima y los nombres de la aplicación y su prueba.
- Diagnosticar y corregir el Maven Wrapper de Windows.
- Verificar compilación de código principal y pruebas.
- Mantener documentado el fallo de `contextLoads` causado por la ausencia intencional de datasource.

## Fuera de alcance

No se configuraron PostgreSQL, datasource, JDBC, credenciales, JPA, Hibernate, Flyway, perfiles, seguridad, JWT, CORS, Docker, migraciones, módulos de negocio ni paquetes por dominio.

## Estado inicial

- Paquete base: `com.orman.backend`.
- Clase principal: `BackendApplication`.
- Prueba inicial: `BackendApplicationTests` con `contextLoads`.
- `application.yml` configuraba `spring.application.name: backend` y puerto `9090`.
- `pom.xml` usaba Java 21 y Spring Boot 4.1.0, pero tenía nombre y descripción genéricos, sin empaquetado explícito y con configuración personalizada del compilador.
- Maven Wrapper 3.3.4 con Maven 3.9.16 fallaba desde PowerShell y desde `cmd /c` al indexar `Target[0]` sobre un valor nulo.
- No existían migraciones ni clases de negocio.

## Versiones confirmadas

| Componente | Versión o valor |
|---|---|
| Java | Temurin 21 LTS (21+35) |
| Maven global | 3.9.16 |
| Maven Wrapper | 3.3.4 |
| Distribución del Wrapper | Maven 3.9.16 |
| Spring Boot | 4.1.0 |
| Empaquetado | JAR |

## Metadatos Maven finales

| Campo | Valor |
|---|---|
| `groupId` | `com.orman` |
| `artifactId` | `backend` |
| `version` | `0.0.1-SNAPSHOT` |
| `name` | `ORMAN-BACKEND` |
| `description` | `Backend de ORMAN.` |
| `packaging` | `jar` |
| `java.version` | `21` |
| Codificación | UTF-8 para fuentes y reportes |

## Dependencias revisadas

- Spring Data JPA.
- Flyway Migration y extensión PostgreSQL.
- Bean Validation.
- Spring Web MVC.
- Driver PostgreSQL con alcance `runtime`.
- Spring Boot DevTools con alcance `runtime` y opcional.
- Lombok opcional.
- Starters de prueba especializados de JPA, Flyway, Validation y Web MVC con alcance `test`.

No se agregaron, eliminaron ni cambiaron versiones de dependencias. No hay versiones manuales innecesarias, duplicados directos ni scopes incorrectos detectados. La configuración personalizada de `maven-compiler-plugin` se eliminó porque Spring Boot ya administra la compilación y no existe todavía una necesidad concreta de procesadores configurados de forma explícita.

## Diagnóstico del Maven Wrapper

El Wrapper estaba completo: `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties` existían y declaraban Maven 3.9.16. Los scripts y propiedades usaban UTF-8 sin BOM; `mvnw.cmd` tenía finales LF, aunque Git declara CRLF para archivos `.cmd`.

El error no era de descarga ni de Maven global. En `mvnw.cmd`, el script PowerShell intentaba evaluar `(Get-Item $MAVEN_M2_PATH).Target[0]` cuando el directorio `.m2` no era un enlace simbólico y `Target` era nulo. Esto impedía iniciar Maven desde PowerShell y desde `cmd /c`, ya que el batch delega en PowerShell.

Se corrigió de forma mínima conservando Wrapper 3.3.4 y distribución Maven 3.9.16: primero se obtiene `Target` y se comprueba si es nulo antes de indexarlo. Tras la corrección, ambos modos de invocación funcionan.

## Archivos creados

- `docs/fases/01-revision-normalizacion-spring-boot.md`
- `src/main/java/com/orman/backend/OrmanBackendApplication.java`
- `src/test/java/com/orman/backend/OrmanBackendApplicationTests.java`

## Archivos modificados

- `pom.xml`
- `src/main/resources/application.yml`
- `mvnw.cmd`
- `README.md`
- `CHANGELOG.md`
- `docs/README.md`
- `docs/PLAN_GENERAL.md`
- `docs/fases/README.md`
- `docs/etapas/etapa-01-fundacion-tecnica.md`

## Archivos eliminados

- `src/main/java/com/orman/backend/BackendApplication.java`, reemplazado por `OrmanBackendApplication.java`.
- `src/test/java/com/orman/backend/BackendApplicationTests.java`, reemplazado por `OrmanBackendApplicationTests.java`.

No se eliminaron migraciones, recursos de configuración ni dependencias.

## Implementación realizada

- Se normalizaron metadatos Maven, empaquetado JAR y codificación UTF-8.
- Se eliminaron bloques vacíos de metadatos y configuración personalizada no necesaria del compilador.
- Se conservó el paquete base `com.orman.backend`.
- Se actualizó el nombre de aplicación a `ORMAN-BACKEND` y se conservó el puerto `9090`.
- Se renombraron coherentemente la aplicación y la prueba de carga de contexto.
- Se corrigió el manejo de directorio no enlazado del Wrapper de Windows.

## Comandos ejecutados

- Inspección de reglas, archivos, scripts, propiedades del Wrapper, codificación, finales de línea y estructura mediante PowerShell y `rg`.
- `java -version` — correcto, Java 21.
- `mvn -version` — correcto, Maven global 3.9.16.
- `.\mvnw.cmd -v` — falló inicialmente por acceso a `Target[0]` nulo; correcto después de la corrección.
- `cmd /c mvnw.cmd -v` — falló inicialmente por la misma causa; correcto después de la corrección.
- `.\mvnw.cmd clean test-compile` — primer intento bloqueado por acceso restringido a Maven Central; segundo intento autorizado y correcto.
- `.\mvnw.cmd test` — primer intento bloqueado por acceso restringido a Maven Central; segundo intento autorizado, compiló y ejecutó la prueba.

No se ejecutaron comandos Git.

## Pruebas y validaciones

- `clean test-compile`: **BUILD SUCCESS**; compiladas una clase principal y una de prueba con Java 21.
- `test`: una prueba ejecutada, cero fallos de aserción, un error de carga de contexto y cero omitidas.
- `OrmanBackendApplicationTests.contextLoads` encuentra correctamente `OrmanBackendApplication`.
- El único error de prueba es `Failed to determine a suitable driver class`, causado por la ausencia de datasource y base embebida.
- Verificados paquete base, nombre lógico, puerto 9090, ausencia de migraciones y ausencia de clases de negocio.

## Errores encontrados

1. `mvnw.cmd` y `cmd /c mvnw.cmd` fallaban por indexar un `Target` nulo.
2. Los primeros intentos de compilación y prueba no pudieron acceder a Maven Central dentro del entorno restringido.
3. `contextLoads` continúa fallando por ausencia de datasource, condición preexistente y fuera del alcance de esta fase.

## Correcciones aplicadas

- Se comprobó `Target` antes de indexarlo en `mvnw.cmd`.
- Se normalizaron metadatos Maven, codificación y empaquetado.
- Se retiró configuración explícita innecesaria de compilación.
- Se alinearon los nombres de aplicación, prueba y configuración YAML con ORMAN-BACKEND.
- Se reintentaron Maven Wrapper, compilación y prueba con acceso autorizado a Maven Central.

## Riesgos y limitaciones

- `contextLoads` no queda verde hasta que exista datasource PostgreSQL en la Fase 02.
- `mvnw.cmd` continúa con finales LF en el árbol de trabajo, aunque `.gitattributes` declara CRLF; el script funciona en ambos modos verificados.
- El warning de `javac` sobre el descubrimiento futuro de procesadores de anotaciones no afecta la compilación actual y deberá revisarse si se incorporan procesadores con uso real.

## Pendientes

- Configurar PostgreSQL, datasource, Flyway y validación de esquema exclusivamente en la Fase 02.
- Ejecutar nuevamente `contextLoads` una vez que exista el entorno de persistencia correspondiente.
- Revisar el warning de procesadores de anotaciones cuando se introduzcan usos reales de Lombok o `@ConfigurationProperties`.
- Revisión manual del usuario y operaciones Git manuales.

## Criterios de aceptación

- [x] `pom.xml` coherente con Java 21, JAR y nombre lógico ORMAN-BACKEND.
- [x] Paquete base `com.orman.backend` conservado.
- [x] Dependencias revisadas sin adiciones ni eliminaciones.
- [x] Clase principal y prueba renombradas de forma coherente.
- [x] Puerto 9090 y configuración YAML mínima conservados.
- [x] Maven Wrapper diagnosticado y corregido; funciona en PowerShell y `cmd /c`.
- [x] Código principal y de prueba compilan.
- [x] `contextLoads` falla solo por datasource, sin ocultar el error.
- [x] Documentación, plan, índices y changelog actualizados.
- [x] PostgreSQL y Flyway no fueron configurados.

## Resultado final

Fase 01 completada. El proyecto queda normalizado y preparado para que la Fase 02 configure PostgreSQL y Flyway. No se implementó negocio ni se introdujo configuración artificial para hacer verde la prueba de contexto.

## Siguiente fase recomendada

**FASE 02 — CONFIGURACIÓN DE POSTGRESQL Y FLYWAY**

No debe iniciarse sin autorización expresa del usuario.
