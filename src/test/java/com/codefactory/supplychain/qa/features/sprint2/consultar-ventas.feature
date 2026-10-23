# language: es
@ventas @sprint2
Característica: Consultar ventas
  Como usuario autorizado de la cadena
  Quiero consultar el historial de ventas con filtros
  Para auditar las transacciones y su alcance por tienda

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU42-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU42-001 — Consultar ventas filtradas por tienda y por variante
    Dado que existe una tienda de ventas llamada "HU42-001"
    Y que existe una variante de ventas llamada "varianteA"
    Y que existe una variante de ventas llamada "varianteB"
    Y que la variante "varianteA" tiene 10 unidades en el almacén de la tienda "HU42-001"
    Y que la variante "varianteB" tiene 10 unidades en el almacén de la tienda "HU42-001"
    Cuando registro una venta en la tienda "HU42-001" con la transacción "ventaA" y la variante "varianteA" y cantidad 1
    Entonces la respuesta es 201
    Cuando registro una venta en la tienda "HU42-001" con la transacción "ventaB" y la variante "varianteB" y cantidad 1
    Entonces la respuesta es 201
    Cuando consulto las ventas de la tienda "HU42-001"
    Entonces la respuesta es 200
    Y el campo "content" tiene 2 elementos
    Y todas las ventas son de la tienda "HU42-001"
    Cuando consulto las ventas de la tienda "HU42-001" de la variante "varianteA"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].lineas[0].varianteId" es el identificador de la variante "varianteA"

  @CP-HU42-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU42-002 — Consultar una venta por id
    Dado que existe una tienda de ventas llamada "HU42-002"
    Y que existe una variante de ventas llamada "HU42-002"
    Y que la variante "HU42-002" tiene 10 unidades en el almacén de la tienda "HU42-002"
    Cuando registro una venta en la tienda "HU42-002" con la transacción "consulta" y la variante "HU42-002" y cantidad 2
    Entonces la respuesta es 201
    Cuando consulto la línea de venta "consulta"
    Entonces la respuesta es 200
    Y el campo "lineas[0].id" es el identificador de la línea de venta "consulta"
    Y el campo "lineas[0].cantidad" es 2

  @CP-HU42-003 @negativo @no-funcional @seguridad
  Escenario: CP-HU42-003 — Una venta de otra tienda responde 404 para un vendedor sin acceso
    Dado que existe una tienda de ventas llamada "propia"
    Y que existe una tienda de ventas llamada "ajena"
    Y que existe una variante de ventas llamada "HU42-003"
    Y que la variante "HU42-003" tiene 5 unidades en el almacén de la tienda "ajena"
    Cuando registro una venta en la tienda "ajena" con la transacción "ventaAjena" y la variante "HU42-003" y cantidad 1
    Entonces la respuesta es 201
    Y que existe un vendedor llamado "vendedorHU42" asignado a la tienda "propia"
    Cuando consulto la línea de venta "ventaAjena" como "vendedorHU42"
    Entonces la respuesta es 404

  @CP-HU42-004 @positivo @no-funcional @seguridad
  Escenario: CP-HU42-004 — Filtrar por una tienda ajena devuelve una página vacía
    Dado que existe una tienda de ventas llamada "propia"
    Y que existe una tienda de ventas llamada "ajena"
    Y que existe una variante de ventas llamada "HU42-004"
    Y que la variante "HU42-004" tiene 5 unidades en el almacén de la tienda "ajena"
    Cuando registro una venta en la tienda "ajena" con la transacción "ventaAjena" y la variante "HU42-004" y cantidad 1
    Entonces la respuesta es 201
    Y que existe un vendedor llamado "vendedorHU42" asignado a la tienda "propia"
    Cuando consulto las ventas de la tienda "ajena" como "vendedorHU42"
    Entonces la respuesta es 200
    Y el campo "content" está vacío

  @CP-HU42-005 @negativo @no-funcional @seguridad
  Escenario: CP-HU42-005 — Sin filtro de tienda, un vendedor solo ve las ventas de sus tiendas
    Dado que existe una tienda de ventas llamada "propia"
    Y que existe una tienda de ventas llamada "ajena"
    Y que existe una variante de ventas llamada "HU42-005"
    Y que la variante "HU42-005" tiene 5 unidades en el almacén de la tienda "propia"
    Y que la variante "HU42-005" tiene 5 unidades en el almacén de la tienda "ajena"
    Cuando registro una venta en la tienda "propia" con la transacción "ventaPropia" y la variante "HU42-005" y cantidad 1
    Entonces la respuesta es 201
    Cuando registro una venta en la tienda "ajena" con la transacción "ventaAjena" y la variante "HU42-005" y cantidad 1
    Entonces la respuesta es 201
    Y que existe un vendedor llamado "vendedorHU42" asignado a la tienda "propia"
    Cuando consulto las ventas de la variante "HU42-005" como "vendedorHU42"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].tiendaId" es el identificador de la tienda "propia"

  @CP-HU42-006 @positivo @funcional @flujo-principal
  Escenario: CP-HU42-006 — Filtrar por estado ANULADA y por rango de fechas UTC
    Dado que existe una tienda de ventas llamada "HU42-006"
    Y que existe una variante de ventas llamada "HU42-006"
    Y que la variante "HU42-006" tiene 10 unidades en el almacén de la tienda "HU42-006"
    Cuando registro una venta en la tienda "HU42-006" con la transacción "anulada" y la variante "HU42-006" y cantidad 1
    Entonces la respuesta es 201
    Cuando registro una venta en la tienda "HU42-006" con la transacción "vigente" y la variante "HU42-006" y cantidad 1
    Entonces la respuesta es 201
    Cuando anulo la línea de venta "anulada" con el motivo "Prueba de filtro"
    Entonces la respuesta es 200
    Cuando consulto las ventas de la tienda "HU42-006" con estado "ANULADA"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].lineas[0].id" es el identificador de la línea de venta "anulada"
    Cuando consulto las ventas de la tienda "HU42-006" desde una fecha futura
    Entonces la respuesta es 200
    Y el campo "content" está vacío

  @CP-HU42-007 @positivo @funcional @flujo-principal
  Escenario: CP-HU42-007 — Consultar por transaccionId devuelve la transacción completa
    Dado que existe una tienda de ventas llamada "HU42-007"
    Y que existe una variante de ventas llamada "varianteA"
    Y que existe una variante de ventas llamada "varianteB"
    Y que la variante "varianteA" tiene 5 unidades en el almacén de la tienda "HU42-007"
    Y que la variante "varianteB" tiene 5 unidades en el almacén de la tienda "HU42-007"
    Cuando registro una venta en la tienda "HU42-007" con la transacción "completa" y las líneas:
      | variante  | cantidad |
      | varianteA | 1        |
      | varianteB | 1        |
    Entonces la respuesta es 201
    Cuando consulto las ventas con la transacción "completa"
    Entonces la respuesta es 200
    Y todas las ventas tienen la transacción "completa"
    Y el campo "content.lineas.flatten()" tiene 2 elementos
