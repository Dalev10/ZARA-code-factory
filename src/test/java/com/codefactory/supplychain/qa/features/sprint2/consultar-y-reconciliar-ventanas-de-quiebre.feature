# language: es
@ventas @sprint2
Característica: Consultar y reconciliar ventanas de quiebre
  Como usuario autorizado de la cadena
  Quiero consultar y reconciliar ventanas de quiebre
  Para mantener consistente el historial de faltantes del Almacén

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU46-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU46-001 — Consultar una ventana por id y filtrar por abiertas
    Dado que existe una tienda de ventas llamada "HU46-001"
    Y que existe una variante de ventas llamada "HU46-001"
    Y que la variante "HU46-001" tiene 1 unidades en el almacén de la tienda "HU46-001"
    Cuando registro una venta en la tienda "HU46-001" con la transacción "v1" y la variante "HU46-001" y cantidad 1
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU46-001" para la variante "HU46-001"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Cuando consulto la ventana de quiebre por su identificador
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la ventana de quiebre
    Y el campo "tiendaId" es el identificador de la tienda "HU46-001"
    Y el campo "varianteId" es el identificador de la variante "HU46-001"
    Cuando consulto las ventanas de quiebre abiertas de la tienda "HU46-001"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos

  @CP-HU46-002 @negativo @no-funcional @seguridad
  Escenario: CP-HU46-002 — Una ventana de otra tienda responde 404 para un vendedor sin acceso
    Dado que existe una tienda de ventas llamada "HU46-002"
    Y que existe una variante de ventas llamada "HU46-002"
    Y que la variante "HU46-002" tiene 1 unidades en el almacén de la tienda "HU46-002"
    Cuando registro una venta en la tienda "HU46-002" con la transacción "v1" y la variante "HU46-002" y cantidad 1
    Entonces la respuesta es 201
    Cuando consulto las ventanas de quiebre de la tienda "HU46-002" para la variante "HU46-002"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y que existe un vendedor llamado "vendedorHU46" sin tiendas asignadas
    Cuando consulto la última ventana de quiebre como "vendedorHU46"
    Entonces la respuesta es 404

  @CP-HU46-003 @negativo @funcional @caso-borde
  Escenario: CP-HU46-003 — Reconciliar dos veces: la segunda ejecución no cambia nada
    Dado que existe una tienda de ventas llamada "HU46-003"
    Y que existe una variante de ventas llamada "HU46-003"
    Y que la variante "HU46-003" tiene 1 unidades en el almacén de la tienda "HU46-003"
    Cuando registro una venta en la tienda "HU46-003" con la transacción "v1" y la variante "HU46-003" y cantidad 1
    Entonces la respuesta es 201
    Cuando repongo el almacén de la tienda "HU46-003" con 2 unidades de la variante "HU46-003"
    Entonces la respuesta es 201
    Cuando registro una venta en la tienda "HU46-003" con la transacción "v2" y la variante "HU46-003" y cantidad 2
    Entonces la respuesta es 201
    Cuando reconcilio las ventanas de quiebre de la tienda "HU46-003"
    Entonces la respuesta es 200
    Y el campo "paresProcesados" no es nulo
    Cuando recuerdo las ventanas de quiebre de la tienda "HU46-003" para la variante "HU46-003"
    Cuando reconcilio las ventanas de quiebre de la tienda "HU46-003"
    Entonces la respuesta es 200
    Y el campo "creadas" es 0
    Y el campo "cerradas" es 0
    Y el campo "corregidas" es 0
    Y las ventanas de quiebre de la tienda "HU46-003" para la variante "HU46-003" no cambiaron
    Y hay 2 ventanas de quiebre de la tienda "HU46-003" para la variante "HU46-003"

  @CP-HU46-004 @negativo @no-funcional @seguridad
  Escenario: CP-HU46-004 — Reconciliar exige inventario:administrar (403 para un vendedor)
    Dado que existe una tienda de ventas llamada "HU46-004"
    Y que existe un vendedor llamado "vendedorHU46" asignado a la tienda "HU46-004"
    Cuando reconcilio las ventanas de quiebre de la tienda "HU46-004" como "vendedorHU46"
    Entonces la respuesta es 403

  @CP-HU46-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU46-005 — Reconciliar sin tiendaId se rechaza (400)
    Cuando reconcilio las ventanas de quiebre sin tienda
    Entonces la respuesta es 400
    Y el campo "mensaje" no es nulo
