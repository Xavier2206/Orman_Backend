# ETAPA 4.2 — Contratos y Cuotas

## Estado

`COMPLETADA` el 2026-09-10. La etapa implementa exclusivamente Contratos,
ContratoArchivos y Cuotas. No incorpora Pagos, PagoComprobantes, Recibos,
CuentasPago ni Notificaciones.

## Diseño e implementación

- El módulo reside en `com.orman.backend.contract`, con `controller`,
  `dto.request`, `dto.response`, `entity`, `mapper`, `repository`, `service` y
  `service.impl`.
- `ContratoEntity` referencia una `UnidadEntity`, una Persona inquilina y,
  opcionalmente, su contrato origen. `ContratoArchivoEntity` referencia al
  contrato y `CuotaEntity` referencia al contrato. Todas las relaciones son
  `@ManyToOne(fetch = FetchType.LAZY)`, sin colecciones inversas ni cascadas.
- Flyway V11 crea `contratos`, `contrato_archivos` y `cuotas` con FKs
  restrictivas, importes `NUMERIC(14,2)`, checks de fechas y estados, una sola
  cuota por `(codcon, periodo)` y un único contrato `VIGENTE` por Unidad.
- Los contratos trabajan en intervalos mensuales `[fecha_inicio, fecha_fin)`;
  ambas fechas deben ser el primer día del mes. Al confirmar un borrador se
  generan las cuotas `PENDIENTE` dentro de la misma transacción.

## Ciclo de vida

`BORRADOR -> VIGENTE -> FINALIZADO` y `VIGENTE -> RESCINDIDO`.

Una renovación crea un nuevo borrador con `codcon_origen`; no altera el
contrato histórico. La rescisión guarda fecha y motivo, pero no modifica cuotas
ni intenta validar pagos: esa lógica pertenece exclusivamente a ETAPA 4.3.

Las cuotas se crean solo como `PENDIENTE`. Los estados `PARCIAL` y `PAGADA` no
tienen endpoints ni transiciones en este módulo.

## Seguridad

Todos los controladores requieren `ROLE_PROPIETARIO`. La capa de servicio
reutiliza la comprobación de propiedad existente y valida la cadena:

`Usuario autenticado -> Persona asociada -> Propiedad propia -> Unidad -> Contrato`.

No se agregaron `ROLE_INQUILINO`, administrador inmobiliario, permisos nuevos,
ni cambios a JWT, `SecurityConfig`, Roles, Menús o Procesos.

## API implementada

| Recurso | Rutas |
|---|---|
| Contratos | `POST/GET /api/v1/unidades/{coduni}/contratos`; `GET /api/v1/contratos`; `GET/PUT /api/v1/contratos/{codcon}`; `PATCH .../confirmar`; `PATCH .../finalizar`; `POST .../renovaciones`; `PATCH .../rescindir` |
| Archivos | `POST/GET /api/v1/contratos/{codcon}/archivos` |
| Cuotas | `GET /api/v1/contratos/{codcon}/cuotas` |

Las creaciones devuelven `201 Created` y `Location`. Las respuestas de error
usan `ProblemDetail` con los códigos existentes.

## Validación

- Compilaciones parciales tras V11/entidades, DTOs/repositorios y
  servicios/controladores: `BUILD SUCCESS`.
- Pruebas de mapper, MVC, persistencia, integración PostgreSQL y autorización
  de propiedad.
- `./mvnw.cmd clean test`: **BUILD SUCCESS**; 272 pruebas, 0 fallos, 0 errores
  y 0 omitidas. Flyway validó V1–V11 e Hibernate validó `ddl-auto=validate`.

## Riesgos y pendientes

- Pagos, aplicación de pagos parciales, cambios a `PARCIAL`/`PAGADA`, recibos,
  comprobantes y conciliación no fueron implementados.
- La rescisión no comprueba cuotas regularizadas ni anula cuotas futuras; ambas
  reglas dependen de la ETAPA 4.3 y requieren autorización independiente.
- La ETAPA 4.3.1 requiere autorización explícita antes de comenzar.
