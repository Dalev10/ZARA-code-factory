# language: es
@ventas @sprint2
Característica: Registrar venta
  Como vendedor de una tienda
  Quiero registrar ventas por lote
  Para descontar el stock del Almacén de forma atómica y auditable

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU40-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU40-001 — Registrar una venta válida descuenta el Almacén y la firma el servidor
    Dado que existe una tienda de ventas llamada "HU40-001"
    Y que existe una variante de ventas llamada "HU40-001"
    Y que la variante "HU40-001" tiene 10 unidades en el almacén de la tienda "HU40-001"
    Y que existe un vendedor llamado "vendedorHU40" asignado a la tienda "HU40-001"
    Cuando registro una venta como "vendedorHU40" en la tienda "HU40-001" con la transacción "ventaValida" y la variante "HU40-001" y cantidad 3
    Entonces la respuesta es 201
    Y el campo "transaccionId" es el identificador de la transacción "ventaValida"
    Y el campo "vendedorId" es el identificador del usuario "vendedorHU40"
    Y el campo "fecha" termina en Z
    Y el campo "total" es el número "269700.0"
    Y el campo "lineas" tiene 1 elementos
    Y el campo "lineas[0].estado" es "REGISTRADA"
    Y el campo "lineas[0].forzada" es falso
    Y el stock de la variante "HU40-001" en el almacén de la tienda "HU40-001" es 7

  @CP-HU40-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-002 — Una venta sin stock suficiente responde 409 y no descuenta
    Dado que existe una tienda de ventas llamada "HU40-002"
    Y que existe una variante de ventas llamada "HU40-002"
    Y que la variante "HU40-002" tiene 2 unidades en el almacén de la tienda "HU40-002"
    Cuando registro una venta en la tienda "HU40-002" con la transacción "sinStock" y la variante "HU40-002" y cantidad 3
    Entonces la respuesta es 409
    Y el campo "mensaje" no es nulo
    Y el stock de la variante "HU40-002" en el almacén de la tienda "HU40-002" es 2

  @CP-HU40-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-003 — Un lote con una línea sin stock no registra ninguna línea (todo o nada)
    Dado que existe una tienda de ventas llamada "HU40-003"
    Y que existe una variante de ventas llamada "conStock"
    Y que existe una variante de ventas llamada "sinStock"
    Y que la variante "conStock" tiene 10 unidades en el almacén de la tienda "HU40-003"
    Y que la variante "sinStock" tiene 1 unidades en el almacén de la tienda "HU40-003"
    Cuando registro una venta en la tienda "HU40-003" con la transacción "lote" y las líneas:
      | variante  | cantidad |
      | conStock  | 2        |
      | sinStock  | 5        |
    Entonces la respuesta es 409
    Y el stock de la variante "conStock" en el almacén de la tienda "HU40-003" es 10
    Y el stock de la variante "sinStock" en el almacén de la tienda "HU40-003" es 1
    Cuando consulto las ventas con la transacción "lote"
    Entonces la respuesta es 200
    Y el campo "content" está vacío

  @CP-HU40-004 @negativo @funcional @caso-borde
  Escenario: CP-HU40-004 — Una venta forzada sin stock deja el Almacén en negativo y queda marcada
    Dado que existe una tienda de ventas llamada "HU40-004"
    Y que existe una variante de ventas llamada "HU40-004"
    Y que la variante "HU40-004" tiene 1 unidades en el almacén de la tienda "HU40-004"
    Y que existe un vendedor llamado "vendedorHU40Forzada" asignado a la tienda "HU40-004"
    Cuando registro una venta forzada como "vendedorHU40Forzada" en la tienda "HU40-004" con la transacción "forzada" y la variante "HU40-004" y cantidad 3
    Entonces la respuesta es 201
    Y el campo "lineas[0].forzada" es verdadero
    Y el stock de la variante "HU40-004" en el almacén de la tienda "HU40-004" es -2

  @CP-HU40-005 @negativo @funcional @caso-borde
  Escenario: CP-HU40-005 — Reintentar con el mismo transaccionId y los mismos datos devuelve 200 sin duplicar
    Dado que existe una tienda de ventas llamada "HU40-005"
    Y que existe una variante de ventas llamada "HU40-005"
    Y que la variante "HU40-005" tiene 10 unidades en el almacén de la tienda "HU40-005"
    Cuando registro una venta en la tienda "HU40-005" con la transacción "idempotente" y la variante "HU40-005" y cantidad 2
    Entonces la respuesta es 201
    Cuando repito la última venta registrada
    Entonces la respuesta es 200
    Y el campo "lineas[0].id" es el identificador de la línea de venta "idempotente"
    Y el stock de la variante "HU40-005" en el almacén de la tienda "HU40-005" es 8

  @CP-HU40-006 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-006 — Reutilizar un transaccionId con otros datos responde 409
    Dado que existe una tienda de ventas llamada "HU40-006"
    Y que existe una variante de ventas llamada "HU40-006"
    Y que la variante "HU40-006" tiene 10 unidades en el almacén de la tienda "HU40-006"
    Cuando registro una venta en la tienda "HU40-006" con la transacción "reutilizada" y la variante "HU40-006" y cantidad 2
    Entonces la respuesta es 201
    Cuando registro otra venta en la tienda "HU40-006" con la transacción "reutilizada" y la variante "HU40-006" y cantidad 5
    Entonces la respuesta es 409
    Y el campo "mensaje" no es nulo
    Y el stock de la variante "HU40-006" en el almacén de la tienda "HU40-006" es 8

  @CP-HU40-007 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-007 — Un SKU inexistente responde 404
    Dado que existe una tienda de ventas llamada "HU40-007"
    Cuando registro una venta en la tienda "HU40-007" con la transacción "skuInexistente" y el SKU "SKU-NO-EXISTE-HU40" y cantidad 1
    Entonces la respuesta es 404
    Y el campo "mensaje" no es nulo

  @CP-HU40-008 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-008 — Rechazar datos inválidos: cantidad 0, precio negativo o sin ítems (400)
    Dado que existe una tienda de ventas llamada "HU40-008"
    Y que existe una variante de ventas llamada "HU40-008"
    Y que la variante "HU40-008" tiene 10 unidades en el almacén de la tienda "HU40-008"
    Cuando registro una venta en la tienda "HU40-008" con la transacción "cantidadCero" y la variante "HU40-008" y cantidad 0
    Entonces la respuesta es 400
    Y el campo "mensaje" no es nulo
    Cuando registro una venta en la tienda "HU40-008" con la transacción "precioNegativo" y la variante "HU40-008" con precio unitario -1.0
    Entonces la respuesta es 400
    Cuando registro una venta en la tienda "HU40-008" con la transacción "sinItems" sin líneas
    Entonces la respuesta es 400
    Y el stock de la variante "HU40-008" en el almacén de la tienda "HU40-008" es 10

  @CP-HU40-009 @positivo @funcional @caso-borde
  Escenario: CP-HU40-009 — canal y forzarSinStock son opcionales
    Dado que existe una tienda de ventas llamada "HU40-009"
    Y que existe una variante de ventas llamada "HU40-009"
    Y que la variante "HU40-009" tiene 5 unidades en el almacén de la tienda "HU40-009"
    Cuando registro una venta sin canal ni forzarSinStock en la tienda "HU40-009" con la transacción "opcional" y la variante "HU40-009" y cantidad 1
    Entonces la respuesta es 201
    Y el campo "lineas[0].forzada" es falso

  @CP-HU40-010 @negativo @funcional @caso-borde
  Escenario: CP-HU40-010 — Sin fila la variante tiene stock 0: venta normal 409, forzada crea la fila en negativo
    Dado que existe una tienda de ventas llamada "HU40-010"
    Y que existe una variante de ventas llamada "HU40-010"
    Cuando registro una venta en la tienda "HU40-010" con la transacción "sinFila" y la variante "HU40-010" y cantidad 1
    Entonces la respuesta es 409
    Y la variante "HU40-010" no tiene fila de inventario en el almacén de la tienda "HU40-010"
    Cuando registro una venta forzada en la tienda "HU40-010" con la transacción "forzadaSinFila" y la variante "HU40-010" y cantidad 2
    Entonces la respuesta es 201
    Y el stock de la variante "HU40-010" en el almacén de la tienda "HU40-010" es -2

  @CP-HU40-011 @negativo @funcional @caso-borde
  Escenario: CP-HU40-011 — Si una venta forzada ya creó la fila, la carga inicial responde 409 y el ajuste sí aplica
    Dado que existe una tienda de ventas llamada "HU40-011"
    Y que existe una variante de ventas llamada "HU40-011"
    Cuando registro una venta forzada en la tienda "HU40-011" con la transacción "forzadaPrevia" y la variante "HU40-011" y cantidad 1
    Entonces la respuesta es 201
    Cuando intento cargar 10 unidades de la variante "HU40-011" en el almacén de la tienda "HU40-011"
    Entonces la respuesta es 409
    Cuando ajusto el inventario de la variante "HU40-011" en la tienda "HU40-011" en 10 unidades con el motivo "INGRESO_MANUAL"
    Entonces la respuesta es 201
    Y el campo "aLaMano" es 9

  @CP-HU40-012 @negativo @no-funcional @seguridad
  Escenario: CP-HU40-012 — Forzar sin stock exige el scope ventas:forzar-sin-stock (403)
    Dado que existe una tienda de ventas llamada "HU40-012"
    Y que existe una variante de ventas llamada "HU40-012"
    Y que existe un usuario de ventas llamado "usuarioLimitado" con el permiso "ventas:registrar"
    Y que la tienda "HU40-012" está asignada al usuario "usuarioLimitado"
    Cuando registro una venta forzada como "usuarioLimitado" en la tienda "HU40-012" con la transacción "forzadaLimitada" y la variante "HU40-012" y cantidad 1
    Entonces la respuesta es 403
    Y la variante "HU40-012" no tiene fila de inventario en el almacén de la tienda "HU40-012"

  @CP-HU40-013 @negativo @funcional @ruta-negativa
  Escenario: CP-HU40-013 — No se registran ventas en una tienda desactivada
    Dado que existe una tienda de ventas llamada "HU40-013"
    Y que existe una variante de ventas llamada "HU40-013"
    Y que la variante "HU40-013" tiene 5 unidades en el almacén de la tienda "HU40-013"
    Cuando desactivo la tienda "HU40-013"
    Entonces la respuesta es 204
    Cuando registro una venta en la tienda "HU40-013" con la transacción "tiendaInactiva" y la variante "HU40-013" y cantidad 1
    Entonces la respuesta es un error de cliente
    Y el stock de la variante "HU40-013" en el almacén de la tienda "HU40-013" es 5
