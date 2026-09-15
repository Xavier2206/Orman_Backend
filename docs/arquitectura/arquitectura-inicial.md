# Arquitectura inicial

## Visión

ORMAN-BACKEND comenzará como un monolito modular: una sola aplicación Spring Boot y un único artefacto desplegable, dividido internamente por funcionalidades del dominio. Esta decisión mantiene simple la operación inicial y exige límites claros entre módulos.

## Convenciones base

- Paquete raíz: `com.orman.backend`.
- Organización: paquetes por funcionalidad o dominio.
- Plataforma: Java 21, Spring Boot y Maven.
- Configuración principal: `application.yml`.
- Persistencia: PostgreSQL, Spring Data JPA e Hibernate.
- Evolución del esquema: Flyway.
- Puerto HTTP: `9090`.

## Límites y dependencias

- Cada funcionalidad debe concentrar sus reglas y exponer interacciones deliberadas.
- Las capas internas no deben mezclarse ni saltarse sin justificación.
- Los módulos no deben formar dependencias circulares.
- Las entidades de persistencia no serán contratos directos de las APIs.
- Los componentes comunes solo se crearán cuando resuelvan una necesidad compartida real.
- La infraestructura transversal de errores reside en `common.error` y `common.exception`.

## Datos y esquema

La correccion tecnica de Persona establece que `personas.correo` es
`VARCHAR(100) NOT NULL` desde V8. La validacion de entrada usa DTO y Bean
Validation; Flyway mantiene la garantia de PostgreSQL. No se agrega unicidad ni
se inicia OTP.

PostgreSQL será la fuente persistente. Flyway creará y modificará el esquema mediante migraciones versionadas; Hibernate validará la correspondencia. No se usará generación automática `create` o `update`.

El núcleo contiene `personas`, `usuarios`, `roles`, `rolusu`, `sesiones_usuario`,
`otp_challenges`, `propiedades`, `unidades`, `unidad_fotos`, `contratos`,
`contrato_archivos`, `cuotas`, `cuentas_pago`, `pagos`, `pago_comprobantes`,
`recibos` y `notificaciones`. `rolusu` materializa Usuario–Rol; `sesiones_usuario`
pertenece al módulo `auth`, referencia Usuario de forma unidireccional y limita
a una sesión activa por `(login, device_id)`. `otp_challenges` conserva `login`
como identificador simple respaldado por FK de base de datos, sin relación JPA ni
cascada, y limita a un challenge PENDING por cliente y propósito.

La lógica OTP usa `SecureRandom`, HMAC-SHA-256 con secreto externo y `Clock`
UTC. WEB con Roles administrativos activos recibe un challenge y correo SMTP
síncrono antes de la sesión; MOBILE y WEB INQUILINO conservan autenticación
directa. Verify vuelve a validar Usuario, Persona y Roles antes de crear la
sesión/JWT existentes. El reenvío prepara el código nuevo y solo lo confirma
después del éxito SMTP, para preservar el anterior ante fallos de entrega.

## Seguridad

La Fase 07 introdujo BCrypt, la Fase 09 validó credenciales y la Fase 10 incorporó JWT HS256, refresh opaco, sesiones por dispositivo y seguridad HTTP stateless. Nimbus JOSE + JWT es la única biblioteca JWT; la clave se obtiene del entorno y debe tener al menos 32 bytes. Ningún secreto, contraseña, hash, token u OTP se expone o registra.

`tipo_persona` es clasificación de negocio y no sustituye roles, permisos ni autorización. Los JWT contienen exclusivamente `sub`, `sid`, `iss`, `iat` y `exp`; no contienen Roles. El filtro valida JWT, sesión, Usuario y Persona, consulta los nombres de Roles activos en PostgreSQL y crea un `Authentication` con authorities inmutables `ROLE_<NOMBRE>`. Esta consulta ocurre en cada petición protegida, de modo que los cambios confirmados de asignación o estado se reflejan sin renovar el token ni revocar la sesión.

El módulo `auth` valida Usuario, Persona y BCrypt, actualiza `ultimo_acceso` UTC, crea sesiones y emite tokens mediante login. `POST /api/v1/auth/refresh` rota el refresh bajo bloqueo pesimista. WEB usa cookie HttpOnly y MOBILE JSON.

La Fase 11.2 aplica `@PreAuthorize` a los controladores actuales. `AuthorizationService` resuelve alcance propio, Usuario común y Persona común; las consultas complejas no viven en SpEL. `OwnerProtectionService` mantiene las invariantes en la capa transaccional. Todas las operaciones que pueden reducir propietarios activos bloquean primero, con `PESSIMISTIC_WRITE`, la fila del Rol exacto `PROPIETARIO`; después cuentan asignación, Rol, Usuario y Persona activos. Esta fila actúa como mutex y evita que operaciones concurrentes dejen cero propietarios.

Las Fases 12.1 y 12.2 incorporan el flujo persistente `Usuario -> RolUsu -> Rol -> RolMe -> Menu -> MePro -> Proceso` y su administración REST exclusiva de PROPIETARIO. `RolMe` y `MePro` son entidades explícitas con claves compuestas; no existe `rolpro`, `@ManyToMany` automático, authorities por Proceso ni menú del Usuario autenticado.

Una autenticación insuficientemente autorizada devuelve `403 ACCESS_DENIED`; romper el mínimo devuelve `409 LAST_OWNER_REQUIRED`; la autenticación inválida conserva `401`. CORS, CSRF y los contratos WEB/MOBILE no se modificaron. Los módulos `property` y `contract` restringen sus controladores a `ROLE_PROPIETARIO` y, en su capa transaccional, resuelven `Usuario autenticado -> Persona asociada -> Propiedad.codper_propietaria`; `contract` continúa la comprobación hacia Unidad y Contrato. No crean un administrador inmobiliario, no derivan autorización de `tipo_persona` y no modifican la seguridad transversal.

## Evolución

La Etapa 4.3 agrega el módulo `payment`, dependiente de `contract`, `property`,
`person` y `user`, sin dependencias inversas. La corrección V16/V17 distingue
contratos programados y vigentes, serializa por Unidad la validación de
intervalos y diferencia pagos registrados por propietaria de los presentados
por inquilino. Una única operación financiera bloquea la cuota, recalcula su
estado y genera el recibo dentro de la transacción. Solo el alta de pago admite
`ROLE_INQUILINO`, verificando la Persona inquilina de la cuota; la revisión
permanece exclusiva de `ROLE_PROPIETARIO`.

La Etapa 4.4 agrega `notification`, con una FK de destinatario a `Usuario` y
referencias escalares a Cuota o Pago, sin relaciones JPA polimórficas. El
módulo consulta solo notificaciones propias y marca su lectura de forma
idempotente. Consume eventos internos publicados por `payment` después del
commit financiero y contiene sus fallos; el scheduler diario de cuotas usa
`America/La_Paz` y excluye cuotas pagadas o anuladas. No incorpora mensajería externa ni
modifica JWT, seguridad, roles, menús o procesos.

La modularidad facilitará crecer dentro del mismo despliegue. Una separación en microservicios solo podría considerarse ante necesidades técnicas y operativas demostrables; no es parte del plan actual.

## Decisiones relacionadas

- [ADR-001 — Monolito modular](../decisiones/ADR-001-monolito-modular.md)
- [ADR-002 — Flyway controla el esquema](../decisiones/ADR-002-flyway-controla-esquema.md)
- [ADR-004 — Configuración YAML](../decisiones/ADR-004-configuracion-yaml.md)
- [ADR-005 — Uso controlado de Lombok](../decisiones/ADR-005-uso-controlado-lombok.md)
