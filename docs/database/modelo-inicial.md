# Modelo inicial analizado

## Alcance

Este documento conserva el análisis preliminar del núcleo de personas, usuarios y roles. No representa una migración ni autoriza la creación de tablas.

## Tablas del núcleo

### `personas`

Representará los datos de una persona. Antes de implementarla se deben confirmar los atributos, restricciones, significado de `tipo_persona` y estrategia de estado.

### `usuarios`

Representará la identidad de acceso asociada opcionalmente a una persona. El identificador de acceso tendrá una longitud uniforme y la contraseña se almacenará únicamente como `password_hash` generado con BCrypt.

### `roles`

Representará las agrupaciones de permisos asignables a usuarios. Su catálogo, restricciones y estados se definirán en la fase correspondiente.

### `rol_usuario`

Representará la relación de muchos a muchos entre usuarios y roles. Sus claves y restricciones se diseñarán antes de la primera migración relacionada.

## Relaciones confirmadas

- Una persona puede tener como máximo un usuario.
- Un usuario puede tener varios roles.
- Un rol puede pertenecer a varios usuarios.
- `rol_usuario` materializará la relación entre `usuarios` y `roles`.

La primera relación requiere una restricción de unicidad apropiada en la futura referencia desde usuario a persona. Su opcionalidad y ciclo de vida se confirmarán antes de implementar.

## Decisiones preliminares

- Usar nombres SQL en minúsculas y `snake_case`.
- Revisar el significado y la representación de `tipo_persona` antes de implementar `personas`.
- Definir una longitud uniforme para `login`.
- Nombrar `password_hash` al valor de contraseña persistido; nunca almacenar texto plano.
- Evitar `ON DELETE CASCADE` entre `personas` y `usuarios`.
- Preferir tipos de fecha y hora con zona horaria cuando representen instantes.
- Revisar el significado, tipo y ciclo de `estado` antes de crear la primera migración.
- Definir restricciones, índices y nulabilidad a partir de reglas confirmadas, no de suposiciones.

## Estado de implementación

La Fase 00 no implementó tablas, migraciones, entidades ni repositorios. La definición de `personas` se completó y aplicó posteriormente en la Fase 04 con V1 y V2; su modelo definitivo está documentado en `docs/fases/04-modelo-migracion-persona.md`.
