# Modelo inicial analizado

## Alcance

Este documento conserva el análisis del núcleo de personas, usuarios y roles. No representa una migración ni autoriza la creación de tablas fuera de la fase correspondiente.

## Tablas del núcleo

### `personas`

Representa los datos de una persona. `tipo_persona` es una clasificación de negocio y no sustituye roles, permisos ni autorización.

### `usuarios`

Representa la identidad de acceso asociada a una persona. La Fase 06 confirmó `login VARCHAR(30)` como clave primaria y `passwd VARCHAR(255)` como columna reservada para un hash seguro. La Fase 07 introdujo BCrypt mediante `PasswordEncoder`; nunca se almacena texto plano ni se expone o registra el hash.

### `roles`

Representa agrupaciones asignables a usuarios. La Fase 08 implementó `codr` como identidad, `nombre` único y `estado` como `SMALLINT` (`1` activo, `0` inactivo).

### Relación Usuario–Rol

La tabla `rolusu` materializa la relación de muchos a muchos entre usuarios y roles. Usa la clave primaria compuesta `(login, codr)`, conserva `fecha_asignacion`, elimina asignaciones al eliminar físicamente un Usuario y restringe la eliminación de un Rol asignado.

### `sesiones_usuario`

V6 incorpora sesiones persistentes. `sid UUID` es PK y `login VARCHAR(30)` referencia `usuarios(login)` con `ON DELETE CASCADE`. Solo existe una fila activa por `(login, device_id)` mediante índice único parcial. La fila conserva cliente WEB/MOBILE, fechas UTC, versión, revocación y exclusivamente el hash SHA-256 del refresh token; nunca el token original.

### `otp_challenges`

V9 incorpora challenges OTP con `id UUID` generado en Java y `login VARCHAR(30)`
referenciado por una FK normal a `usuarios(login)`, sin cascada ni cláusula de
borrado. El registro conserva únicamente `otp_digest VARCHAR(64)`, nunca el OTP
en texto plano. Persiste cliente WEB/MOBILE, propósito LOGIN, estado, contadores,
fechas UTC, IP y user-agent. Un índice único parcial limita a un PENDING por
`(login, client_type, purpose)` sin impedir el historial no PENDING.

La ampliación V14 agrega `propiedades.portada_ref` como referencia interna
nullable de la portada gestionada por filesystem; `portada_url` conserva la
referencia externa HTTP/HTTPS.

## Relaciones confirmadas

- Una persona puede tener como máximo un usuario.
- Un usuario puede tener varios roles.
- Un rol puede pertenecer a varios usuarios.
- Un usuario puede tener varias sesiones, con máximo una activa por dispositivo.
- Una propiedad tiene una Persona propietaria y puede tener varias unidades, sin colección JPA inversa.
- Una unidad puede tener varias fotografías, administradas mediante consultas del repositorio.
- Una unidad puede tener contratos históricos, uno actual y contratos futuros;
  los intervalos `PROGRAMADO`/`VIGENTE` no pueden solaparse.
- Un contrato puede tener archivos por URL y cuotas mensuales, sin relaciones JPA inversas.
- Una cuota puede tener varios pagos; cada pago puede tener comprobantes y como máximo un recibo.
- Una Persona propietaria puede tener varias cuentas de pago; una cuenta puede ser referenciada por pagos históricos.
- Un Usuario puede recibir varias notificaciones internas, sin colección JPA inversa.
- La tabla relacional que se elija en la Fase 08 materializará la relación entre `usuarios` y `roles`.

La relación Persona–Usuario se implementó en V3: `usuarios.codper` es obligatorio, único y referencia `personas.codper` con `ON DELETE RESTRICT`.

## Decisiones preliminares

- Usar nombres SQL en minúsculas y `snake_case`.
- Mantener `tipo_persona` separada del sistema de roles.
- Usar `passwd` como columna persistida para el futuro hash; nunca almacenar texto plano.
- Introducir BCrypt en la Fase 07 sin activar todavía Spring Security HTTP completo.
- Evitar `ON DELETE CASCADE` entre `personas` y `usuarios`.
- Definir restricciones, índices y nulabilidad a partir de reglas confirmadas.
- Diseñar `sesiones_usuario` únicamente en la Fase 10.

## Estado de implementación

La Fase 04 completó `personas` con V1 y V2; la Fase 06 completó `usuarios` con V3; la Fase 08 completó `roles` y `rolusu` con V4 y V5; la subfase 10.1 completó `sesiones_usuario` con V6; la Fase 13 incorporó `otp_challenges` con V9; la Etapa 4.1 incorporó `propiedades`, `unidades` y `unidad_fotos` con V10; la Etapa 4.2 incorporó `contratos`, `contrato_archivos` y `cuotas` con V11; la Etapa 4.3 incorporó `cuentas_pago`, `pagos`, `pago_comprobantes` y `recibos` con V12; la Etapa 4.4 incorporó `notificaciones` con V13; y la corrección controlada incorporó V16 y V17.

## ETAPA 4.4 — Notificaciones

V13 implementa `notificaciones` con identidad `BIGINT`, FK restrictiva hacia
`usuarios.login`, tipo, título, mensaje y referencia polimórfica controlada por
`referencia_tipo` y `referencia_id`. La fecha de lectura representa el estado
de la notificación. Una clave única por destinatario, tipo y referencia evita
duplicados; los índices cubren listado cronológico y no leídas.

## ETAPA 4.1 — Propiedades, Unidades y Fotografías

V10 implementa `propiedades` con una FK a `personas` para la propietaria y estados binarios; `unidades` con FK a `propiedades`, importes `NUMERIC` y unicidad de nombre por propiedad; y `unidad_fotos` con FK a `unidades`, orden único por unidad y una portada como máximo mediante índice único parcial. Las relaciones JPA son unidireccionales desde el hijo hacia el padre, sin cascadas ni colecciones inversas.

## ETAPA 4.2 — Contratos y Cuotas

V11 implementa las tablas base. V16 deja `codcon_origen` solo como dato legado,
incorpora `moneda = BOB`, exige renta positiva y define estados
`PROGRAMADO`, `VIGENTE`, `FINALIZADO` y `RESCINDIDO`. `contrato_archivos`
conserva URL y metadatos; `cuotas` conserva cada periodo y puede quedar
`ANULADA` sin eliminación física.

## ETAPA 4.3 — Pagos, recibos y cuentas de pago

V12 implementa las tablas base. V17 redefine `origen_registro` como
`PROPIETARIA`/`INQUILINO` y agrega `registrado_por` y `revisado_por` con FKs a
Usuario. `pago_comprobantes` conserva URL y metadatos. `recibos` garantiza una
constancia interna única por pago confirmado. Los saldos no se almacenan: se
calculan con pagos `CONFIRMADO`; también se expone la suma pendiente de revisión.
