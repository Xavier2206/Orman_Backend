# Correccion tecnica: correo obligatorio de Persona

## Motivo y decision de dominio

Toda Persona de ORMAN debe contar con un correo antes de que, si corresponde,
se cree su Usuario. El correo sera el canal disponible para una futura fase de
OTP. La regla es general para toda Persona y no depende de `tipo_persona`.

`Persona.correo` debe ser no nulo, no vacio, no compuesto solo por espacios,
tener formato valido y medir como maximo 100 caracteres. No se agrega UNIQUE.

## Migracion V8

`V8__make_persona_correo_not_null.sql` comprueba primero si existen filas con
`correo IS NULL`. Si existen, aborta claramente y no altera datos; de lo
contrario ejecuta `ALTER COLUMN correo SET NOT NULL`. V1 a V7 permanecen
intactas y no existe V9.

La consulta local previa encontro 1 Persona con `correo IS NULL`. No se
mostraron datos personales, no se fabricaron correos y V8 no se aplico. El
registro debe corregirse manualmente antes de reintentar Flyway.

## Contrato y validaciones

La entidad usa `nullable = false` y longitud 100. Create y Update usan
`@NotBlank`, `@Email` y `@Size(max = 100)`. POST y PUT responden
`400 VALIDATION_ERROR` para omision, null, vacio, espacios, formato invalido o
longitud superior a 100. Se conserva la normalizacion existente con `trim`.

## Pruebas y regresion

Se actualizaron pruebas MVC, persistencia PostgreSQL, esquema, longitud maxima
y fixtures de Usuario, autenticacion, autorizacion, roles, menus y sesiones.
La ejecucion completa queda pendiente porque la precondicion de V8 detecta el
dato historico NULL local.

## Documentacion e impacto futuro

`docs/postman/persona.md` documenta ejemplos, validaciones, matriz
`PERSONA-CORREO-001` a `PERSONA-CORREO-007`, checklist y orden recomendado.
La obligatoriedad prepara el canal para OTP, pero OTP todavia no esta
implementado ni iniciado. No se modificaron seguridad, Fase 11, Fase 12,
Usuario ni Angular.
