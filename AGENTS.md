# Reglas permanentes de trabajo — ORMAN-BACKEND

Este archivo define las reglas obligatorias para cualquier ejecución futura de Codex en este repositorio.

## Metodología

- Trabajar exclusivamente por fases y dentro del alcance autorizado.
- No comenzar ni adelantar otra fase sin autorización expresa del usuario.
- Antes de actuar, leer este archivo, `docs/PLAN_GENERAL.md` y el documento de la fase actual.
- No ampliar el alcance ni asumir requisitos no documentados.
- Evitar la sobreingeniería y mantener cambios pequeños, claros y revisables.
- No crear archivos, carpetas, dependencias o abstracciones sin un uso real dentro de la fase.
- Inspeccionar solo el contexto necesario y preservar los cambios existentes del usuario.
- Informar con claridad errores, limitaciones, riesgos, decisiones y validaciones.

## Arquitectura

- Mantener un monolito modular.
- Organizar el código por funcionalidad o dominio, no mediante paquetes globales por capa.
- Usar `com.orman.backend` como paquete base.
- No crear microservicios.
- No mezclar responsabilidades entre capas sin una justificación documentada.
- No devolver entidades JPA directamente desde futuras APIs; usar contratos de entrada y salida apropiados.
- Evitar dependencias circulares entre módulos.

## Java y Spring Boot

- Usar Java 21, Spring Boot, Maven y empaquetado JAR.
- Configurar el backend en el puerto `9090`.
- Usar `application.yml` como archivo principal de configuración.
- Preferir inyección por constructor y evitar la inyección en campos.
- No usar `System.out.println`; usar logging apropiado sin exponer información sensible.
- No agregar dependencias sin necesidad y justificación documentada.
- No usar `spring.jpa.hibernate.ddl-auto=create` ni `update`.
- Flyway controlará la creación y evolución del esquema.
- Cuando exista un esquema administrado por Flyway, Hibernate deberá validarlo con `ddl-auto=validate`, si corresponde a la fase.

## Lombok

- Lombok está permitido, pero debe usarse de forma controlada.
- No usar `@Data` indiscriminadamente en entidades JPA.
- Preferir anotaciones específicas como `@Getter`, `@Setter`, `@NoArgsConstructor` y `@Builder` cuando correspondan.
- Evitar que `toString`, `equals` o `hashCode` incluyan relaciones JPA o datos sensibles.
- No ocultar reglas o lógica de negocio mediante código generado por Lombok.

## Base de datos

- Usar PostgreSQL.
- Usar nombres SQL en minúsculas y `snake_case`.
- Crear y modificar el esquema exclusivamente mediante migraciones Flyway.
- No modificar una migración ya aplicada; crear una migración posterior.
- No usar borrados en cascada sin una decisión explícita y documentada.
- Preferir borrado lógico cuando el modelo disponga de estado y el dominio lo requiera.
- No crear tablas fuera de la fase que las autorice.

## Seguridad

- Nunca guardar contraseñas en texto plano.
- Usar BCrypt cuando la fase de usuarios implemente almacenamiento de contraseñas.
- No registrar contraseñas, hashes, tokens ni códigos OTP en logs.
- No exponer `password_hash` mediante APIs, DTO, trazas ni documentación de ejemplos.
- No incluir secretos reales en el repositorio.
- No implementar JWT, OTP ni Spring Security antes de sus fases autorizadas.

## Pruebas

- Toda fase que incorpore lógica debe incluir pruebas relevantes.
- No desactivar, omitir o debilitar pruebas para conseguir una compilación exitosa.
- No crear pruebas sin valor que solo validen getters o setters.
- Registrar los comandos de validación ejecutados y sus resultados reales.
- No declarar una fase completada si fallan las validaciones requeridas, salvo que el criterio de la fase permita documentar expresamente un impedimento externo.

## Documentación

- Actualizar el documento individual de la fase durante el trabajo.
- Actualizar `docs/PLAN_GENERAL.md` al cambiar el estado de una fase.
- Actualizar `CHANGELOG.md` al cerrar cada fase.
- Actualizar `README.md` solo cuando cambie la forma de configurar, ejecutar o comprender el proyecto.
- Actualizar la teoría de la etapa cuando aparezcan conceptos importantes; no crear teoría separada por cada fase.
- Registrar archivos creados, modificados y eliminados.
- Registrar decisiones, comandos, resultados, errores, correcciones, riesgos, limitaciones y pendientes.

## Git

- Git será ejecutado manualmente por el usuario.
- Codex no debe cambiar ramas, crear commits, publicar cambios, revertir cambios ni eliminar cambios del usuario.
- No ejecutar: `git add`, `git commit`, `git push`, `git pull`, `git merge`, `git rebase`, `git reset`, `git restore`, `git checkout`, `git switch`, `git clean`, `git tag`, `git stash` ni `git cherry-pick`.
- Evitar comandos Git de lectura cuando la inspección directa de archivos sea suficiente.
- Al finalizar, solo sugerir al usuario los comandos Git manuales apropiados, sin ejecutarlos.

## Cierre de fase

Una fase solo puede marcarse como `COMPLETADA` cuando:

- cumple su objetivo y alcance;
- no contiene errores conocidos bloqueantes;
- pasan las validaciones correspondientes o existe un impedimento externo admitido y documentado;
- el documento de fase y el plan general están actualizados;
- `CHANGELOG.md` está actualizado;
- se registraron decisiones, archivos y resultados reales;
- queda explícito que el usuario debe revisar los cambios y ejecutar Git manualmente.
