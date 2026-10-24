# language: es
@ventas @sprint2
Característica: Anular venta
  Como supervisor de una tienda
  Quiero anular líneas o transacciones de venta
  Para devolver el stock al Almacén dejando el motivo registrado

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU41-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU41-001 — Anular una línea de venta devuelve el stock al Almacén y registra el motivo
    Dado que existe una tienda de ventas llamada "QA41-001"
    Y que existe una variante de ventas llamada "QA41-001"
    Y que la variante "QA41-001" tiene 10 unidades en el almacén de la tienda "QA41-001"
    Y que existe un supervisor llamado "supervisorQA41" asignado a la tienda "QA41-001"
    Cuando registro una venta como "supervisorQA41" en la tienda "QA41-001" con la transacción "base" y la variante "QA41-001" y cantidad 4
    Entonces la respuesta es 201
    Cuando anulo la línea de venta "base" como "supervisorQA41" con el motivo "Cliente desistió"
    Entonces la respuesta es 200
    Y el campo "lineas[0].estado" es "ANULADA"
    Y el campo "lineas[0].motivoAnulacion" es "Cliente desistió"
    Y el campo "lineas[0].anuladaPor" es el identificador del usuario "supervisorQA41"
    Y el campo "lineas[0].anuladaEn" no es nulo
    Y el stock de la variante "QA41-001" en el almacén de la tienda "QA41-001" es 10

  @CP-HU41-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU41-002 — Anular sin motivo se rechaza (400) y la venta sigue registrada
    Dado que existe una tienda de ventas llamada "QA41-002"
    Y que existe una variante de ventas llamada "QA41-002"
    Y que la variante "QA41-002" tiene 10 unidades en el almacén de la tienda "QA41-002"
    Cuando registro una venta en la tienda "QA41-002" con la transacción "base" y la variante "QA41-002" y cantidad 2
    Entonces la respuesta es 201
    Cuando anulo la línea de venta "base" sin motivo
    Entonces la respuesta es 400
    Y el campo "mensaje" no es nulo
    Cuando anulo la línea de venta "base" con motivo en blanco
    Entonces la respuesta es 400
    Cuando consulto la línea de venta "base"
    Entonces la respuesta es 200
    Y el campo "lineas[0].estado" es "REGISTRADA"
    Y el stock de la variante "QA41-002" en el almacén de la tienda "QA41-002" es 8

  @CP-HU41-003 @negativo @funcional @caso-borde
  Escenario: CP-HU41-003 — Anular dos veces responde 200 con la anulación original y no devuelve stock doble
    Dado que existe una tienda de ventas llamada "QA41-003"
    Y que existe una variante de ventas llamada "QA41-003"
    Y que la variante "QA41-003" tiene 10 unidades en el almacén de la tienda "QA41-003"
    Cuando registro una venta en la tienda "QA41-003" con la transacción "base" y la variante "QA41-003" y cantidad 3
    Entonces la respuesta es 201
    Cuando anulo la línea de venta "base" con el motivo "Primera"
    Entonces la respuesta es 200
    Cuando anulo la línea de venta "base" con el motivo "Segunda"
    Entonces la respuesta es 200
    Y el campo "lineas[0].motivoAnulacion" es "Primera"
    Y el stock de la variante "QA41-003" en el almacén de la tienda "QA41-003" es 10

  @CP-HU41-004 @positivo @funcional @flujo-principal
  Escenario: CP-HU41-004 — Anular por transacción anula todas sus líneas y devuelve todo el stock
    Dado que existe una tienda de ventas llamada "QA41-004"
    Y que existe una variante de ventas llamada "varianteA"
    Y que existe una variante de ventas llamada "varianteB"
    Y que la variante "varianteA" tiene 10 unidades en el almacén de la tienda "QA41-004"
    Y que la variante "varianteB" tiene 10 unidades en el almacén de la tienda "QA41-004"
    Cuando registro una venta en la tienda "QA41-004" con la transacción "total" y las líneas:
      | variante  | cantidad |
      | varianteA | 2        |
      | varianteB | 5        |
    Entonces la respuesta es 201
    Cuando anulo la transacción "total" con el motivo "Devolución total"
    Entonces la respuesta es 200
    Y cada elemento del campo "lineas.estado" es "ANULADA"
    Y el stock de la variante "varianteA" en el almacén de la tienda "QA41-004" es 10
    Y el stock de la variante "varianteB" en el almacén de la tienda "QA41-004" es 10

  @CP-HU41-005 @negativo @funcional @caso-borde
  Escenario: CP-HU41-005 — Anular por transacción dos veces no devuelve stock otra vez
    Dado que existe una tienda de ventas llamada "QA41-005"
    Y que existe una variante de ventas llamada "QA41-005"
    Y que la variante "QA41-005" tiene 6 unidades en el almacén de la tienda "QA41-005"
    Cuando registro una venta en la tienda "QA41-005" con la transacción "repetida" y la variante "QA41-005" y cantidad 2
    Entonces la respuesta es 201
    Cuando anulo la transacción "repetida" con el motivo "Error de caja"
    Entonces la respuesta es 200
    Cuando anulo la transacción "repetida" con el motivo "Error de caja"
    Entonces la respuesta es 200
    Y el stock de la variante "QA41-005" en el almacén de la tienda "QA41-005" es 6

  @CP-HU41-006 @negativo @funcional @ruta-negativa
  Escenario: CP-HU41-006 — Anular una venta inexistente responde 404
    Cuando anulo una línea de venta inexistente con el motivo "No existe"
    Entonces la respuesta es 404
    Cuando anulo una transacción de venta inexistente con el motivo "No existe"
    Entonces la respuesta es 404

  @CP-HU41-007 @negativo @no-funcional @seguridad
  Escenario: CP-HU41-007 — Un supervisor no anula ventas de una tienda que no tiene asignada
    Dado que existe una tienda de ventas llamada "QA41-007"
    Y que existe una variante de ventas llamada "QA41-007"
    Y que la variante "QA41-007" tiene 5 unidades en el almacén de la tienda "QA41-007"
    Cuando registro una venta en la tienda "QA41-007" con la transacción "base" y la variante "QA41-007" y cantidad 1
    Entonces la respuesta es 201
    Y que existe un supervisor llamado "supervisorAjeno" sin tiendas asignadas
    Cuando anulo la línea de venta "base" como "supervisorAjeno" con el motivo "Ajena"
    Entonces la respuesta es 403 o 404
    Y el stock de la variante "QA41-007" en el almacén de la tienda "QA41-007" es 4
