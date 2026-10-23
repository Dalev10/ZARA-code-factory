# language: es
@ventas @sprint2
Característica: Ventanas de quiebre
  Como usuario autorizado de la cadena
  Quiero registrar ventanas de quiebre del Almacén
  Para medir los periodos en que la tienda no tuvo stock disponible

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU45-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU45-001 — Una venta que deja el Almacén en 0 abre una ventana de quiebre en la misma respuesta
    Dado que existe una tienda de ventas llamada "HU45-001"
    Y que existe una variante de ventas llamada "HU45-001"
    Y que la variante "HU45-001" tiene 2 unidades en el almacén de la tienda "HU45-001"
    Cuando registro una venta en la tienda "HU45-001" con la transacción "v1" y la variante "HU45-001" y cantidad 2
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-001" para la variante "HU45-001"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "ABIERTA"
    Y el campo "content[0].fechaInicio" termina en Z

  @CP-HU45-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU45-002 — Reponer el Almacén por encima de 0 cierra la ventana con REPOSICION
    Dado que existe una tienda de ventas llamada "HU45-002"
    Y que existe una variante de ventas llamada "HU45-002"
    Y que la variante "HU45-002" tiene 1 unidades en el almacén de la tienda "HU45-002"
    Cuando registro una venta en la tienda "HU45-002" con la transacción "v1" y la variante "HU45-002" y cantidad 1
    Entonces la respuesta es 201
    Cuando repongo el almacén de la tienda "HU45-002" con 5 unidades de la variante "HU45-002"
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-002" para la variante "HU45-002"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "CERRADA"
    Y el campo "content[0].motivoCierre" es "REPOSICION"

  @CP-HU45-003 @positivo @funcional @caso-borde
  Escenario: CP-HU45-003 — Una venta forzada cierra la ventana abierta con VENTA_FORZADA
    Dado que existe una tienda de ventas llamada "HU45-003"
    Y que existe una variante de ventas llamada "HU45-003"
    Y que la variante "HU45-003" tiene 1 unidades en el almacén de la tienda "HU45-003"
    Cuando registro una venta en la tienda "HU45-003" con la transacción "v1" y la variante "HU45-003" y cantidad 1
    Entonces la respuesta es 201
    Cuando registro una venta forzada en la tienda "HU45-003" con la transacción "v2" y la variante "HU45-003" y cantidad 1
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-003" para la variante "HU45-003"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "CERRADA"
    Y el campo "content[0].motivoCierre" es "VENTA_FORZADA"

  @CP-HU45-004 @negativo @funcional @caso-borde
  Escenario: CP-HU45-004 — Una venta forzada que deja el Almacén en negativo no abre ventana
    Dado que existe una tienda de ventas llamada "HU45-004"
    Y que existe una variante de ventas llamada "HU45-004"
    Y que la variante "HU45-004" tiene 1 unidades en el almacén de la tienda "HU45-004"
    Cuando registro una venta forzada en la tienda "HU45-004" con la transacción "v1" y la variante "HU45-004" y cantidad 3
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-004" para la variante "HU45-004"
    Entonces la respuesta es 200
    Y el campo "content" está vacío

  @CP-HU45-005 @positivo @funcional @caso-borde
  Escenario: CP-HU45-005 — Desactivar la tienda cierra sus ventanas con TIENDA_DESACTIVADA
    Dado que existe una tienda de ventas llamada "HU45-005"
    Y que existe una variante de ventas llamada "HU45-005"
    Y que la variante "HU45-005" tiene 1 unidades en el almacén de la tienda "HU45-005"
    Cuando registro una venta en la tienda "HU45-005" con la transacción "v1" y la variante "HU45-005" y cantidad 1
    Entonces la respuesta es 201
    Cuando desactivo la tienda "HU45-005"
    Entonces la respuesta es 204
    Cuando consulto las ventanas de quiebre de la tienda "HU45-005" para la variante "HU45-005"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "CERRADA"
    Y el campo "content[0].motivoCierre" es "TIENDA_DESACTIVADA"

  @CP-HU45-006 @negativo @funcional @caso-borde
  Escenario: CP-HU45-006 — Una carga inicial en 0 no abre ventana
    Dado que existe una tienda de ventas llamada "HU45-006"
    Y que existe una variante de ventas llamada "HU45-006"
    Cuando cargo 0 unidades de la variante "HU45-006" en el almacén de la tienda "HU45-006"
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-006" para la variante "HU45-006"
    Entonces la respuesta es 200
    Y el campo "content" está vacío

  @CP-HU45-007 @positivo @funcional @caso-borde
  Escenario: CP-HU45-007 — Anular la venta que abrió la ventana devuelve stock y la cierra con REPOSICION
    Dado que existe una tienda de ventas llamada "HU45-007"
    Y que existe una variante de ventas llamada "HU45-007"
    Y que la variante "HU45-007" tiene 1 unidades en el almacén de la tienda "HU45-007"
    Cuando registro una venta en la tienda "HU45-007" con la transacción "base" y la variante "HU45-007" y cantidad 1
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU45-007" para la variante "HU45-007"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "ABIERTA"
    Cuando anulo la línea de venta "base" con el motivo "Devolución"
    Entonces la respuesta es 200
    Cuando consulto las ventanas de quiebre de la tienda "HU45-007" para la variante "HU45-007"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].estado" es "CERRADA"
    Y el campo "content[0].motivoCierre" es "REPOSICION"
