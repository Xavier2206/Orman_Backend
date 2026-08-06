# Fase 12.1 — Modelo persistente de Menús y Procesos

`COMPLETADA` el 2026-08-06. Validación final: `./mvnw.cmd clean test` con 180 pruebas, 0 fallos, 0 errores y 0 omitidas.

## Objetivo y alcance

Esta subfase incorpora exclusivamente el modelo persistente necesario para recorrer:

```text
Usuarios -> RolUsu -> Roles -> RolMe -> Menús -> MePro -> Procesos
```

No agrega endpoints, CRUD, DTO, servicios de negocio, authorities, permisos ni autorización por Proceso.

## Relaciones

- `rolusu` representa Usuario–Rol.
- `rolme` representa Rol–Menú mediante la clave compuesta `(codr, codm)`.
- `mepro` representa Menú–Proceso mediante la clave compuesta `(codm, codp)`.

Las dos relaciones nuevas son entidades explícitas; no usan `@ManyToMany`. No existe ni se creó `rolpro`.

## Migración V7

`V7__create_menus_procesos_tables.sql` crea, sin insertar datos:

| Tabla | Columnas | Restricciones |
|---|---|---|
| `menus` | `codm`, `nombre`, `icono`, `estado` | PK, UNIQUE `nombre`, CHECK `estado IN (0,1)`, default 1 |
| `procesos` | `codp`, `nombre`, `enlace`, `estado` | PK, UNIQUE `nombre`, UNIQUE `enlace`, CHECK `estado IN (0,1)`, default 1 |
| `rolme` | `codr`, `codm` | PK compuesta y FKs restrictivas a `roles` y `menus` |
| `mepro` | `codm`, `codp` | PK compuesta y FKs restrictivas a `menus` y `procesos` |

No se añadieron índices secundarios: las claves primarias compuestas cubren los accesos iniciales por su primer campo. V1–V6 no se modifican; no existe V8.

## Entidades y repositorios

- `Menu` y `Proceso` representan sus tablas sin colecciones bidireccionales.
- `RolMe`/`RolMeId` y `MePro`/`MeProId` siguen el patrón `@EmbeddedId` + `@MapsId` de `RolUsu`.
- Los repositorios son `MenuRepository`, `ProcesoRepository`, `RolMeRepository` y `MeProRepository`.

`enlace` es un dato persistente de Proceso; no se interpreta como endpoint, permiso ni authority.

## Pruebas

Las pruebas PostgreSQL verifican V7, constraints, defaults, identidad, unicidad, FKs, claves compuestas, eliminación de relaciones sin cascadas y el flujo completo aprobado. Los fixtures son transaccionales y no dejan datos locales permanentes.

## Exclusiones y preparación para 12.2

No hay datos iniciales, `rolpro`, endpoints ni cambios en JWT, sesiones, CORS, CSRF, `SecurityFilterChain`, filtros ni matriz de Fase 11.2. La Fase 12.2 podrá consumir este modelo para la consulta funcional que sea autorizada después.

## Riesgos y decisiones

La integridad depende del orden correcto de altas y bajas por las FKs restrictivas. Esto es intencional: quitar una relación no elimina el Rol, Menú ni Proceso asociados.
