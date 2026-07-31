# Modelo inicial analizado

## Alcance

Este documento conserva el análisis preliminar del núcleo de personas, usuarios y roles. No representa una migración ni autoriza la creación de tablas.

## Tablas del núcleo

### `personas`

Representará los datos de una persona. Antes de implementarla se deben confirmar los atributos, restricciones, significado de `tipo_persona` y estrategia de estado.

### `usuarios`

Representa la identidad de acceso asociada opcionalmente a una persona. La Fase 06 confirmó `login VARCHAR(30)` como clave primaria y `passwd VARCHAR(255)` como columna reservada exclusivamente para un hash seguro futuro; BCrypt se adopta en una fase posterior autorizada.

### `roles`

Representará las agrupaciones de permisos asignables a usuarios. Su catálogo, restricciones y estados se definirán en la fase correspondiente.

### `rol_usuario`

Representará la relación de muchos a muchos entre usuarios y roles. Sus claves y restricciones se diseñarán antes de la primera migración relacionada.

## Relaciones confirmadas

- Una persona puede tener como máximo un usuario.
- Un usuario puede tener varios roles.
- Un rol puede pertenecer a varios usuarios.
- `rol_usuario` materializará la relación entre `usuarios` y `roles`.

La primera relación se implementó en V3: `usuarios.codper` es obligatorio, único y referencia `personas.codper` con `ON DELETE RESTRICT`. Por tanto, una Persona tiene cero o un Usuario y un Usuario pertenece a una Persona.

## Decisiones preliminares

- Usar nombres SQL en minúsculas y `snake_case`.
- Revisar el significado y la representación de `tipo_persona` antes de implementar `personas`.
- Definir una longitud uniforme para `login`.
- Usar `passwd` como columna persistida para el futuro hash; nunca almacenar texto plano.
- Evitar `ON DELETE CASCADE` entre `personas` y `usuarios`.
- Preferir tipos de fecha y hora con zona horaria cuando representen instantes.
- Revisar el significado, tipo y ciclo de `estado` antes de crear la primera migración.
- Definir restricciones, índices y nulabilidad a partir de reglas confirmadas, no de suposiciones.

## Estado de implementación

La Fase 00 no implementó tablas, migraciones, entidades ni repositorios. `personas` se completó en la Fase 04 con V1 y V2; `usuarios` se completó en la Fase 06 con V3. Sus modelos definitivos están documentados en `docs/fases/04-modelo-migracion-persona.md` y `docs/fases/06-modelo-migracion-usuario.md`.
