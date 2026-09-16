# Plan general de ORMAN-BACKEND

## Objetivo general

Construir de forma incremental un backend mantenible para ORMAN, comenzando por la gestión de personas y usuarios, incorporando roles, autenticación y autorización, y continuando con la gestión inmobiliaria, la calidad y la preparación para producción. El plan organiza exclusivamente modelo de datos, entidades, migraciones, repositories, services, controllers, DTOs, validaciones, seguridad, pruebas backend y documentación API. La solución parte como un monolito modular con Java 21, Spring Boot, Maven y PostgreSQL.

## Forma de trabajo

El desarrollo se divide en etapas temáticas, subetapas funcionales y fases acotadas exclusivamente de backend. Solo una fase autorizada puede estar activa; cada fase debe documentar su alcance, cambios, validaciones y pendientes. Cada bloque funcional de la Gestión Inmobiliaria ORMAN tendrá análisis del módulo, diseño técnico, implementación backend, pruebas backend y documentación propios. La teoría transversal se mantiene en el documento de su etapa. Al cerrar una fase se actualizan este plan, su documento individual y `CHANGELOG.md`.

No se adelantan código, tablas, migraciones, dependencias o funcionalidades de una fase futura. Las Fases 10, 11, 12 y 13 están completadas; la Fase 12.3 se cerró mediante autorización explícita.

## Estados permitidos

| Estado | Significado |
|---|---|
| `PENDIENTE` | Aún no iniciada. |
| `EN ANÁLISIS` | Se están confirmando alcance, requisitos o decisiones. |
| `EN DESARROLLO` | La fase está autorizada y en ejecución. |
| `BLOQUEADA` | Existe un impedimento que evita continuar o cerrar. |
| `COMPLETADA` | Cumplió su objetivo, validaciones y documentación. |
| `CANCELADA` | Se decidió no continuar con la fase. |

## Estado actual

- Correccion tecnica vigente: `Persona.correo` obligatorio mediante V8; no es
  una fase nueva ni inicia la Fase 13.
- Corrección contractual vigente: Personas preparado para integración mediante API
  (filtros, Usuario vinculado, capacidades, fotografía local y resumen global), sin migración.
- Mejora puntual vigente: Roles preparado para gestión remota con filtros `q` y
  `estado`, paginación/ordenamiento conservados y resumen global, sin migración
  ni cambios en seguridad, relaciones o CRUD existente.
- Mejora puntual vigente: Menús preparado para gestión remota con filtros `q` y
  `estado`, paginación/ordenamiento conservados y resumen global, sin migración
  ni cambios en seguridad, relaciones o CRUD existente.

- Etapas 1, 2 y 3: **COMPLETADAS**.
- Etapa actual de planificación: **ETAPA 4 — Gestión inmobiliaria ORMAN**.
- Última subetapa completada: **ETAPA 4.4 — Notificaciones internas**.
- Fase 12.3: **COMPLETADA**; consulta post-login actual de Usuario, Persona, Roles, Menús y Procesos mediante `/api/v1/auth/context`.
- Estado de la Fase 09: **COMPLETADA**.
- Estado de la Fase 10 global: **COMPLETADA**; 10.1 y 10.2 están cerradas.
- Resultado de Fase 10: sesiones por dispositivo, JWT HS256, refresh rotatorio, autenticación HTTP stateless, logout y administración de sesiones sobre Flyway V6.
- Estado de la Fase 11 global: **COMPLETADA**.
- Resultado de Fase 11.1: Roles activos consultados en PostgreSQL en cada petición protegida, convertidos a authorities de Spring Security, sin incluirlos en el JWT ni revocar sesiones por sus cambios.
- Resultado de Fase 11.2: matriz aplicada a módulos actuales, objetivos propietarios protegidos, Rol PROPIETARIO reservado y mínimo concurrente de un propietario activo.
- Estado de la Fase 13 global: **COMPLETADA**; OTP WEB administrativo por correo, verify y resend cerrados.
- Resultado de ETAPA 4.1: módulo backend `property`, Flyway V10 con Propiedades, Unidades y UnidadFotos, acceso exclusivo de la Persona propietaria autenticada con `ROLE_PROPIETARIO`, y 262 pruebas totales sin fallos.
- Resultado vigente de ETAPA 4.2: V11 más correcciones V16 y V18; Contratos `PROGRAMADO`/`VIGENTE` sin borradores ni renovación especial, intervalos no solapados, cuotas mensuales y terminación condicionada por cuotas y pagos. V18 añade carga/descarga/eliminación privada de documentos PDF de Contrato.
- Resultado vigente de ETAPA 4.3: V12 más corrección V17; Pagos diferenciados por actor `PROPIETARIA`/`INQUILINO`, confirmación financiera única, trazabilidad de revisión, pagos parciales y un recibo interno por pago confirmado.
- Resultado de ETAPA 4.4: módulo backend `notification`, Flyway V13 con notificaciones internas por Usuario, consulta propia, lectura idempotente, recordatorios de cuotas y eventos internos de pagos; 290 pruebas totales sin fallos.
- Corrección controlada de Contratos/Pagos/Notificaciones: **COMPLETADA** el
  2026-09-15 mediante V16 y V17; eventos financieros consumidos `AFTER_COMMIT`
  y 367 pruebas totales sin fallos.
- Ampliación posterior de Propiedades: Flyway V14 añade `portada_ref` y los
  endpoints autenticados de portada interna, sin alterar `portada_url` ni las
  fotografías de UnidadFoto.
- Próxima fase autorizable: **Fase 18 — Documentación OpenAPI**; requiere autorización explícita independiente.
- Fecha de actualización: **2026-09-16**.

## Etapas y fases previstas

### ETAPA 1 — Fundación técnica

Teoría: [Etapa 1 — Fundación técnica](etapas/etapa-01-fundacion-tecnica.md)

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 00 — Planificación general y estructura documental | `COMPLETADA` | Ninguna | [Documento de Fase 00](fases/00-planificacion-general.md) |
| 01 — Revisión y normalización del proyecto Spring Boot | `COMPLETADA` | Fase 00 | [Documento de Fase 01](fases/01-revision-normalizacion-spring-boot.md) |
| 02 — Configuración de PostgreSQL y Flyway | `COMPLETADA` | Fase 01 | [Documento de Fase 02](fases/02-configuracion-postgresql-flyway.md) |
| 03 — Infraestructura común y manejo de errores | `COMPLETADA` | Fases 01 y 02 | [Documento de Fase 03](fases/03-infraestructura-comun-manejo-errores.md) |

### ETAPA 2 — Personas, usuarios y roles

Teoría: [Etapa 2 — Personas, usuarios y roles](etapas/etapa-02-personas-usuarios-roles.md)

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 04 — Modelo y migración de Persona | `COMPLETADA` | Fases 02 y 03 | [Documento de Fase 04](fases/04-modelo-migracion-persona.md) |
| 05 — API CRUD de Persona | `COMPLETADA` | Fase 04 | [Documento de Fase 05](fases/05-crud-persona.md) |
| 06 — Modelo y migración de Usuario | `COMPLETADA` | Fases 04 y 05 | [Documento de Fase 06](fases/06-modelo-migracion-usuario.md) |
| 07 — Administración de Usuarios y Contraseñas | `COMPLETADA` | Fase 06 | [Documento de Fase 07](fases/07-administracion-usuarios-contrasenas.md) |
| 08 — Roles y relación Usuario–Rol | `COMPLETADA` | Fases 06 y 07 | [Documento de Fase 08](fases/08-roles-relacion-usuario-rol.md) |

### ETAPA 3 — Autenticación, sesiones y autorización

Teoría: pendiente de creación cuando corresponda.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 09 — Autenticación y validación de credenciales | `COMPLETADA` | Fases 07 y 08 | [Documento de Fase 09](fases/09-autenticacion-validacion-credenciales.md) |
| 10 — JWT y control de sesiones | `COMPLETADA` | Fase 09 | [Subfase 10.1](fases/10-1-sesiones-jwt-refresh.md); [Subfase 10.2](fases/10-2-seguridad-sesiones.md) |
| 11 — Autorización por roles | `COMPLETADA` | Fases 08 y 10 | [Subfase 11.1](fases/11-1-base-autorizacion-roles.md); [Subfase 11.2](fases/11-2-matriz-autorizacion-propietario.md) |
| 12 — Menús y procesos dinámicos | `COMPLETADA` | Fase 11 | [Subfase 12.1](fases/12-1-modelo-menus-procesos.md); [Subfase 12.2](fases/12-2-administracion-rest-menus-procesos.md) |
| 12.3 — Contexto del usuario autenticado | `COMPLETADA` | Fases 10, 11 y 12.2 | [Documento de Fase 12.3](fases/12-3-contexto-usuario-autenticado.md) |
| 13 — OTP y desafíos de autenticación | `COMPLETADA` | Fases 09 y 10 | [Documento de Fase 13](fases/13-otp-autenticacion-doble-factor.md) |

### ETAPA 4 — Gestión inmobiliaria ORMAN

Esta etapa es el agrupador principal del módulo de negocio inmobiliario. Cada subetapa es un bloque funcional backend independiente, con análisis del módulo, diseño técnico, implementación backend, pruebas backend y documentación propios.

Teoría: pendiente de creación cuando corresponda.

#### ETAPA 4.1 — Propiedades, unidades y fotografías

| Fase interna | Estado | Dependencia | Documento |
|---|---|---|---|
| 4.1.1 — Análisis | `COMPLETADA` | Etapas 1, 2 y 3 completadas | [Documento de ETAPA 4.1](fases/14-propiedades-unidades-fotografias.md) |
| 4.1.2 — Diseño | `COMPLETADA` | Fase 4.1.1 | [Documento de ETAPA 4.1](fases/14-propiedades-unidades-fotografias.md) |
| 4.1.3 — Implementación backend | `COMPLETADA` | Fase 4.1.2 | [Documento de ETAPA 4.1](fases/14-propiedades-unidades-fotografias.md) |
| 4.1.4 — Pruebas backend | `COMPLETADA` | Fase 4.1.3 | [Documento de ETAPA 4.1](fases/14-propiedades-unidades-fotografias.md) |
| 4.1.5 — Documentación | `COMPLETADA` | Fase 4.1.4 | [Documento de ETAPA 4.1](fases/14-propiedades-unidades-fotografias.md) |

**Alcance de las fases internas:**

- **4.1.1 — Análisis:** requisitos, reglas de negocio, relaciones y permisos.
- **4.1.2 — Diseño:** modelo entidad-relación, DTOs, endpoints y migraciones.
- **4.1.3 — Implementación backend:** entidades, repositories, services, controllers, validaciones y Flyway.
- **4.1.4 — Pruebas backend:** pruebas unitarias, integración, persistencia y autorización.
- **4.1.5 — Documentación:** OpenAPI, decisiones técnicas y cierre de fase.

**Objetivo:** implementar la administración backend de inmuebles.

**Incluye:** Propiedades; Unidades; Fotografías; propietario asociado mediante Persona; estados; disponibilidad.

**Dependencias:** Personas, Usuarios, Roles y Autorización.

**Resultado esperado:** el sistema permite registrar propiedades como edificios o casas y administrar sus unidades.

#### ETAPA 4.2 — Contratos y cuotas

| Fase interna | Estado | Dependencia | Documento |
|---|---|---|---|
| 4.2.1 — Análisis | `COMPLETADA` | Etapa 4.1 completada | [Documento de ETAPA 4.2](fases/15-contratos-cuotas.md) |
| 4.2.2 — Diseño | `COMPLETADA` | Fase 4.2.1 | [Documento de ETAPA 4.2](fases/15-contratos-cuotas.md) |
| 4.2.3 — Implementación backend | `COMPLETADA` | Fase 4.2.2 | [Documento de ETAPA 4.2](fases/15-contratos-cuotas.md) |
| 4.2.4 — Pruebas backend | `COMPLETADA` | Fase 4.2.3 | [Documento de ETAPA 4.2](fases/15-contratos-cuotas.md) |
| 4.2.5 — Documentación | `COMPLETADA` | Fase 4.2.4 | [Documento de ETAPA 4.2](fases/15-contratos-cuotas.md) |

**Objetivo:** implementar la gestión backend del alquiler.

**Incluye:** Contratos; historial contractual; `ContratoArchivo`; inquilino responsable; relación Unidad–Contrato; generación automática de cuotas mensuales; estados de cuotas; contratos futuros; finalización y rescisión.

**Dependencia:** Etapa 4.1.

**Resultado esperado:** administrar alquileres desde la firma del contrato hasta el seguimiento mensual.

**Resultado de implementación vigente:** V11 crea la base y V16 corrige el
ciclo a `PROGRAMADO`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`. El registro
genera cuotas, el scheduler activa contratos futuros, la Unidad serializa la
validación de intervalos `[inicio, fin)`, y finalización/rescisión comprueban
cuotas y pagos pendientes. Una continuación contractual es un contrato nuevo.

**Autorización implementada:** todas las rutas requieren `ROLE_PROPIETARIO` y verifican Persona propietaria, Propiedad, Unidad y Contrato en la capa transaccional. No se agregaron roles ni permisos.

#### ETAPA 4.3 — Pagos y recibos

| Fase interna | Estado | Dependencia | Documento |
|---|---|---|---|
| 4.3.1 — Análisis | `COMPLETADA` | Etapa 4.2 completada | [Documento de ETAPA 4.3](fases/16-pagos-recibos-cuentas-pago.md) |
| 4.3.2 — Diseño | `COMPLETADA` | Fase 4.3.1 | [Documento de ETAPA 4.3](fases/16-pagos-recibos-cuentas-pago.md) |
| 4.3.3 — Implementación backend | `COMPLETADA` | Fase 4.3.2 | [Documento de ETAPA 4.3](fases/16-pagos-recibos-cuentas-pago.md) |
| 4.3.4 — Pruebas backend | `COMPLETADA` | Fase 4.3.3 | [Documento de ETAPA 4.3](fases/16-pagos-recibos-cuentas-pago.md) |
| 4.3.5 — Documentación | `COMPLETADA` | Fase 4.3.4 | [Documento de ETAPA 4.3](fases/16-pagos-recibos-cuentas-pago.md) |

**Objetivo:** implementar la gestión backend financiera del alquiler.

**Incluye:** Pagos; pagos parciales; `PagoComprobantes`; validación manual; `CuentasPago`; QR; recibos.

**Reglas:** una cuota puede tener múltiples pagos; los pagos pueden ser parciales; los comprobantes deben validarse; los recibos se generan después de confirmar pagos.

**Excluye:** pasarela bancaria; integración bancaria; conciliación automática.

**Dependencia:** Etapa 4.2.

**Resultado esperado:** registrar pagos, aplicar pagos parciales y generar recibos después de la confirmación de los pagos.

**Resultado de implementación vigente:** V12 crea la base y V17 incorpora
origen de registro y actores. La propietaria registra pagos verificados como
`CONFIRMADO`; el inquilino presenta QR/transferencia con comprobante como
`PENDIENTE_REVISION`. Una lógica financiera común bloquea la cuota, evita
sobrepago y genera un recibo por pago confirmado.

**Autorización implementada:** las consultas y revisiones requieren
`ROLE_PROPIETARIO`. El registro de pago también admite `ROLE_INQUILINO`, pero
solo sobre cuotas de su propia Persona inquilina. Las cuentas se limitan a la
propietaria de la cuota.

#### ETAPA 4.4 — Notificaciones

| Fase interna | Estado | Dependencia | Documento |
|---|---|---|---|
| 4.4.1 — Análisis | `COMPLETADA` | Etapas 4.2 y 4.3 completadas | [Documento de ETAPA 4.4](fases/17-notificaciones.md) |
| 4.4.2 — Implementación backend | `COMPLETADA` | Fase 4.4.1 | [Documento de ETAPA 4.4](fases/17-notificaciones.md) |
| 4.4.3 — Pruebas backend | `COMPLETADA` | Fase 4.4.2 | [Documento de ETAPA 4.4](fases/17-notificaciones.md) |
| 4.4.4 — Documentación | `COMPLETADA` | Fase 4.4.3 | [Documento de ETAPA 4.4](fases/17-notificaciones.md) |

**Objetivo:** implementar notificaciones internas backend.

**Incluye:** recordatorios de cuotas; pagos pendientes de revisión; pagos confirmados; pagos rechazados.

**Usuarios implementados:** Propietarios con `ROLE_PROPIETARIO`.

**Considerar:** eventos; destinatarios; relación con Usuarios; persistencia de notificaciones.

**Dependencias:** Etapas 4.2 y 4.3.

**Resultado de implementación:** V13 persiste notificaciones internas por Usuario destinatario, con referencias a Cuota o Pago, lectura idempotente, recordatorio manual, scheduler diario `America/La_Paz` y eventos de comprobante, pago confirmado y pago rechazado. No se agregaron canales externos, usuarios inquilinos ni administradores.

### ETAPA 5 — Calidad y producción

Esta etapa se ejecutará después de finalizar las cuatro subetapas de la Gestión Inmobiliaria ORMAN y concentra el cierre transversal de calidad y producción.

Teoría: pendiente de creación cuando corresponda.

| Fase | Estado | Dependencia | Documento |
|---|---|---|---|
| 18 — Documentación OpenAPI | `PENDIENTE` | APIs principales estables | Documento pendiente de creación |
| 19 — Pruebas de integración ampliadas | `PENDIENTE` | Etapas 4.1 a 4.4 | Documento pendiente de creación |
| 20 — Auditoría técnica | `PENDIENTE` | Modelo inmobiliario y seguridad estables | Documento pendiente de creación |
| 21 — Preparación para producción | `PENDIENTE` | Fases 18, 19 y 20 | Documento pendiente de creación |

## Alcance aprobado de las fases futuras y bloques funcionales

### Fase 07 — Administración de Usuarios y Contraseñas

**Objetivo:** implementar la administración REST segura de Usuarios vinculados a Personas existentes, almacenando las contraseñas únicamente como hash BCrypt.

**Incluye:** creación de Usuario; consulta por login; listado paginado; actualización de datos administrativos permitidos; activación; desactivación; validación de Persona existente; un único Usuario por Persona; login duplicado; DTO, mapper, service, service.impl, controller, Bean Validation, `PasswordEncoder` con BCrypt; endpoint específico de cambio o restablecimiento de contraseña; pruebas unitarias, MVC e integración; documentación Postman; `ProblemDetail`.

**Reglas:** `login` es clave primaria y no se modifica mediante PUT; `passwd` no aparece en respuestas, logs ni PUT general; el hash tampoco se expone; crear y cambiar contraseña siempre aplica BCrypt; activar Usuario no activa Persona; desactivar Usuario no desactiva Persona.

**Excluye:** login/autenticación, JWT, refresh token, sesiones, roles, autorización, OTP y Spring Security HTTP completo.

**Dependencia:** Fase 06. **Resultado esperado:** Usuarios administrables por API y contraseñas almacenadas únicamente como BCrypt, sin autenticación todavía.

**Resultado:** completada el 2026-08-02 sin modificar el esquema. La actualización administrativa quedó limitada a `estado`; `login` y `codper` son inmutables. No existe eliminación física de Usuario.

BCrypt se introduce aquí mediante `PasswordEncoder`. Puede incorporarse `spring-security-crypto` sin activar `spring-boot-starter-security` ni el modelo HTTP completo.

### Fase 08 — Roles y relación Usuario–Rol

**Objetivo:** modelar y administrar roles y su asociación con Usuarios.

**Incluye:** tabla `roles`; tabla `rolusu`; migraciones V4 y V5; entidades; repositorios; administración REST de Roles; asignación y retiro de roles; validaciones y pruebas.

**Excluye:** login, JWT, sesiones, filtros de seguridad y autorización de endpoints.

**Dependencias:** Fases 06 y 07. `tipo_persona` sigue siendo clasificación de negocio y no sustituye roles.

**Resultado:** completada el 2026-08-02. `roles.codr` es la PK; `rolusu(login, codr)` materializa la relación N:M con `fecha_asignacion`, `ON DELETE CASCADE` desde Usuario y `ON DELETE RESTRICT` desde Rol. No se insertaron roles iniciales ni se implementó autorización.

### Fase 09 — Autenticación y validación de credenciales

**Objetivo:** validar el inicio de sesión mediante Usuario, Persona y BCrypt.

**Incluye:** endpoint de login; búsqueda de Usuario; validación BCrypt; validación de `usuarios.estado = 1` y `personas.estado = 1`; errores seguros de credenciales; actualización controlada de `ultimo_acceso` si se aprueba; pruebas.

**Excluye:** access token, refresh token, `sesiones_usuario`, logout, autorización por roles y OTP.

**Dependencias:** Fases 07 y 08. El Usuario solo podrá autenticarse cuando `personas.estado = 1`, `usuarios.estado = 1` y la contraseña sea válida.

### Fase 10 — JWT y control de sesiones

**Objetivo:** implementar autenticación basada en tokens y sesiones persistentes por dispositivo.

**Subfase 10.1 completada:** access token JWT; refresh token opaco; expiración; rotación y renovación; tabla `sesiones_usuario`; `sid`; hash SHA-256; login WEB/MOBILE; una sesión activa por `(login, device_id)`; reemplazo de la sesión del mismo dispositivo; pruebas.

**Subfase 10.2 completada el 2026-08-04:** filtro JWT, `SecurityFilterChain`, protección de endpoints, logout, logout-all, administración/revocación de sesiones, CORS/CSRF y revocación ante cambios de contraseña o desactivaciones.

**Excluye:** autorización por rol, OTP, menús y procesos. **Dependencia:** Fase 09.

Cada Usuario puede mantener varias sesiones activas, con una sola por combinación `(login, device_id)`. El JWT incluye `sid`; un nuevo login reemplaza únicamente la sesión activa del mismo dispositivo. `ultimo_acceso` no reemplaza `sesiones_usuario` y PostgreSQL almacena solo el hash del refresh token. Las revocaciones administrativas usan motivos específicos; reactivar Usuario o Persona no recupera sesiones.

### Fase 11 — Autorización por roles

**Objetivo:** proteger endpoints usando identidad autenticada y roles.

**Incluye:** reglas por rol; protección de endpoints; integración con roles; respuestas 401 y 403; pruebas de autorización.

**Excluye:** creación de roles, OTP, menús y procesos. **Dependencias:** Fases 08 y 10.

**Subfase 11.1 completada el 2026-08-06:** consulta escalar de nombres de Roles activos por login, conversión normalizada a `ROLE_<NOMBRE>`, authorities inmutables en `Authentication`, `@EnableMethodSecurity` y respuesta estable `403 ACCESS_DENIED`. Los Roles se consultan después de validar JWT, sesión, Usuario y Persona, y nunca forman parte del JWT. Asignar, retirar, desactivar o reactivar un Rol se refleja en la siguiente petición con la misma sesión y el mismo access token.

**Subfase 11.2 implementada el 2026-08-06 (validación manual pendiente):** matriz concreta de autenticación/sesiones, Personas, Usuarios, Roles y asignaciones; operaciones propias para todo Usuario autenticado; alcance común del ADMINISTRADOR; protección de objetivos propietarios; contraseña propia o PROPIETARIO; Rol reservado; mínimo de un propietario activo con bloqueo pesimista; `403 ACCESS_DENIED` y `409 LAST_OWNER_REQUIRED`.

El alcance Administrador–Propiedad permanece pendiente porque todavía no existe el módulo ni su relación de asignación. No se simula mediante `tipo_persona`, dispositivo, ciudad u otro dato no autorizado.

### Fase 12 — Menús y procesos dinámicos

**Subfase 12.1 completada el 2026-08-06:** modelo V7 con `menus`, `procesos`, `rolme` y `mepro`; relaciones explícitas por claves compuestas para el flujo Usuario–Rol–Menú–Proceso.

**Subfase 12.2 completada el 2026-08-06:** administración REST exclusiva de PROPIETARIO para Menús, Procesos, RolMe y MePro; estados administrativos, validaciones, relaciones explícitas y guías Postman. No se añadieron `rolpro`, V8, datos iniciales, menú de Usuario ni autorización por Proceso.

**Objetivo:** modelar menús, procesos y relaciones de acceso según roles.

**Incluye:** tablas, migraciones, entidades, repositorios, administración, asociación con roles, consultas de navegación autorizada y pruebas.

**Excluye:** OTP, cambios de autenticación y nuevos mecanismos de sesión. **Dependencia:** Fase 11.

### Fase 13 — OTP y desafíos de autenticación

**Estado:** `COMPLETADA` — V9 aporta `otp_challenges`; las subfases 13.1 a
13.4 completan persistencia, ciclo de vida, integración WEB administrativa,
correo SMTP síncrono y reenvío seguro.

**Objetivo:** implementar desafíos temporales de autenticación.

**Incluye:** persistencia de challenges OTP, generación posterior, expiración,
consumo único, límites, intentos y pruebas. El documento único de la fase es
`docs/fases/13-otp-autenticacion-doble-factor.md`.

**Excluye:** sustituir BCrypt, eliminar JWT o cambiar la sesión única sin decisión explícita. **Dependencias:** Fases 09 y 10.

#### Detalle funcional de ETAPA 4.1 — Propiedades, unidades y fotografías

Este bloque conserva el alcance previamente definido como Fase 14 y se desarrollará mediante las fases internas 4.1.1 a 4.1.5.

**Objetivo:** implementar la administración backend de inmuebles.

**Incluye:** Propiedades; Unidades; Fotografías; propietario asociado mediante Persona; estados; disponibilidad.

**Excluye:** contratos, cuotas, pagos, recibos y notificaciones, que corresponden a fases posteriores.

**Dependencias:** Personas, Usuarios, Roles y Autorización.

**Resultado esperado:** el sistema permite registrar propiedades como edificios o casas y administrar sus unidades disponibles.

**Resultado de implementación:** Flyway V10 creó `propiedades`, `unidades` y `unidad_fotos`; el módulo `property` incorpora entidades, DTOs, mappers, repositories, servicios, controladores y guía API. Las relaciones JPA son unidireccionales y LAZY desde Propiedad a Persona, Unidad a Propiedad y UnidadFoto a Unidad, sin colecciones ni cascadas.

**Autorización implementada:** todas las rutas requieren `ROLE_PROPIETARIO`; el servicio verifica además que la Persona asociada al Usuario autenticado coincida con `codper_propietaria`. No existe alcance de ADMINISTRADOR, ni se modificaron JWT, `SecurityConfig`, Roles, Menús o Procesos.

**Validaciones y pruebas:** campos, estados, importes, área, conteos, coordenadas, URLs, pertenencia de rutas anidadas, orden único y única portada por Unidad; pruebas unitarias, MVC, persistencia e integración contra PostgreSQL real. `./mvnw.cmd clean test`: 262 pruebas, 0 fallos, 0 errores y 0 omitidas.

**Ampliación posterior:** Flyway V14 incorpora `propiedades.portada_ref` y el
módulo de Propiedades expone carga, descarga y eliminación autenticadas de una
portada interna, manteniendo `portada_url` como URL externa de compatibilidad.

#### Detalle funcional de ETAPA 4.2 — Contratos y cuotas

Este bloque conserva el alcance previamente definido como Fase 15 y se desarrollará mediante las fases internas 4.2.1 a 4.2.5.

**Objetivo:** implementar la gestión backend del alquiler.

**Incluye:** Contratos; historial contractual; `ContratoArchivo`; inquilino responsable; relación Unidad–Contrato; generación automática de cuotas mensuales; estados de cuotas; programación, finalización y rescisión.

**Reglas:** un contrato pertenece a una Unidad; un contrato tiene un inquilino responsable; una Unidad puede tener múltiples contratos históricos; un contrato confirmado genera cuotas automáticamente.

**Excluye:** pagos, recibos y notificaciones, que corresponden a fases posteriores.

**Dependencia:** Etapa 4.1.

**Resultado esperado:** administrar alquileres desde la firma del contrato hasta el seguimiento mensual.

**Validaciones:** cardinalidad Unidad–Contrato e inquilino–Contrato; intervalos
no solapados; generación idempotente de cuotas; estados, finalización y
rescisión; autorización; pruebas unitarias, persistencia, MVC e integración.

#### Detalle funcional de ETAPA 4.3 — Pagos y recibos

Este bloque conserva el alcance previamente definido como Fase 16 y se desarrollará mediante las fases internas 4.3.1 a 4.3.5.

**Objetivo:** implementar la gestión backend financiera del alquiler.

**Incluye:** Pagos; pagos parciales; `PagoComprobantes`; validación manual; `CuentasPago`; QR; recibos.

**Reglas:** una cuota puede tener múltiples pagos; un pago puede requerir comprobante; el propietario valida pagos; el recibo se genera después de confirmar el pago.

**Excluye:** pasarela bancaria, API bancaria y conciliación automática.

**Dependencia:** Etapa 4.2.

**Resultado esperado:** registrar pagos y generar respaldo documental.

**Validaciones:** aplicación de pagos parciales; estados y transiciones; comprobantes; validación manual; generación posterior del recibo; idempotencia; autorización; pruebas unitarias, MVC, persistencia e integración.

#### Detalle funcional de ETAPA 4.4 — Notificaciones

Este bloque conserva el alcance previamente definido como Fase 17 y se desarrolló mediante las fases internas 4.4.1 a 4.4.4.

**Objetivo:** implementar notificaciones internas backend.

**Incluye:** recordatorios de cuotas; pagos pendientes de revisión; pagos confirmados; pagos rechazados.

**Usuarios implementados:** Propietarios con `ROLE_PROPIETARIO`.

**Considerar:** eventos; destinatarios; relación con Usuarios; persistencia de notificaciones.

**Resultado de implementación:** V13 incorpora `notificaciones` por Usuario destinatario, con referencia a Cuota o Pago, listado paginado propio, resumen de no leídas y marcado idempotente. El scheduler diario en `America/La_Paz` avisa cuotas próximas o vencidas; `payment` publica eventos internos para comprobantes, pagos confirmados y pagos rechazados. No se implementan destinatarios inquilinos ni canales externos.

**Excluye:** canales externos o funcionalidades de mensajería no definidas en esta fase.

**Dependencias:** Etapas 4.2 y 4.3.

**Resultado esperado:** los usuarios reciben avisos relacionados con el flujo de alquileres.

**Validaciones:** destinatario y alcance por contrato o pago; estados de lectura y entrega definidos para la implementación; no exposición de datos de otros usuarios; pruebas unitarias, de servicio, persistencia, MVC e integración.

### Fase 18 — Documentación OpenAPI (ETAPA 5)

**Objetivo:** documentar los contratos HTTP implementados del backend, incluidos los módulos inmobiliarios.

**Incluye:** especificación OpenAPI; modelos de solicitud y respuesta; códigos HTTP; errores `ProblemDetail`; autenticación y autorización; ejemplos de uso.

**Excluye:** cambios funcionales o de contrato no aprobados.

**Dependencia:** APIs principales estables.

**Resultado esperado:** documentación OpenAPI coherente, versionada y revisable para las APIs del sistema.

**Validaciones:** correspondencia con controladores y DTO; revisión de seguridad; verificación de respuestas exitosas y de error.

### Fase 19 — Pruebas de integración ampliadas (ETAPA 5)

**Objetivo:** validar el sistema completo sobre PostgreSQL real.

**Incluye:** flujos de persistencia y API; autenticación; JWT; sesiones; roles; autorización; OTP; Propiedades; Unidades; Contratos; Cuotas; Pagos; Recibos y Notificaciones.

**Excluye:** sustituir las pruebas unitarias y de integración creadas en las fases anteriores.

**Dependencia:** Etapas 4.1 a 4.4.

**Resultado esperado:** cobertura de los flujos críticos y de sus restricciones de datos con resultados reproducibles.

**Validaciones:** pruebas contra PostgreSQL aislado; concurrencia en operaciones críticas; constraints, estados, idempotencia, autorización y no exposición de datos sensibles.

### Fase 20 — Auditoría técnica (ETAPA 5)

**Objetivo:** revisar la seguridad, arquitectura, trazabilidad y protección de datos del backend completo.

**Incluye:** seguridad; arquitectura; trazabilidad; datos sensibles; sesiones; operaciones críticas; revisión de dependencias y configuración relevante.

**Excluye:** implementar correcciones de fases posteriores sin autorización específica.

**Dependencia:** modelo inmobiliario y seguridad estables.

**Resultado esperado:** informe de auditoría con hallazgos clasificados, riesgos, decisiones pendientes y correcciones priorizadas.

**Validaciones:** revisión documental y de código; pruebas de seguridad; verificación de logs, errores, permisos, secretos y contratos.

### Fase 21 — Preparación para producción (ETAPA 5)

**Objetivo:** preparar el backend para un despliegue operativo controlado.

**Incluye:** configuración final; despliegue; observabilidad; controles operativos; gestión externa de secretos; perfiles; empaquetado y procedimientos de respaldo y restauración.

**Excluye:** cambios de dominio o funcionalidades nuevas no contempladas en las etapas 4.1 a 4.4 y las fases 18 a 20.

**Dependencias:** Fases 18, 19 y 20.

**Resultado esperado:** aplicación empaquetada y documentada para operar en un entorno de producción con controles verificables.

**Validaciones:** configuración por ambiente; migraciones Flyway; health checks; logs y métricas; manejo de secretos; despliegue reproducible; respaldo y restauración; ejecución final de pruebas.

## Decisiones pendientes

`tipo_persona` es una clasificación de negocio de Persona; no representa roles, permisos, authorities ni autorización.

## Dependencias generales

- Fase 07 depende de Fase 06.
- Fase 08 depende de Fases 06 y 07.
- Fase 09 depende de Fases 07 y 08.
- Fase 10 depende de Fase 09.
- Fase 11 depende de Fases 08 y 10.
- Fase 12 depende de Fase 11.
- Fase 13 depende de Fases 09 y 10.
- Etapa 4.1 depende de Personas, Usuarios, Roles y Autorización.
- Etapa 4.2 depende de Etapa 4.1.
- Etapa 4.3 depende de Etapa 4.2.
- Etapa 4.4 depende de Etapas 4.2 y 4.3.
- Fase 18 depende de APIs principales estables.
- Fase 19 depende de Etapas 4.1 a 4.4.
- Fase 20 depende del modelo inmobiliario y la seguridad estables.
- Fase 21 depende de Fases 18, 19 y 20.

Una dependencia expresa orden técnico, pero cada fase requiere además autorización explícita del usuario.
