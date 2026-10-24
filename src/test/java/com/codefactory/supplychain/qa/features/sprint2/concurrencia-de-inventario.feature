# language: es
@inventario @sprint2
Característica: Concurrencia de inventario
  Como usuario de la cadena de suministro
  Quiero que las ventas simultáneas no pierdan ni dupliquen unidades
  Para mantener la consistencia del inventario

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU36-001 @negativo @no-funcional @fiabilidad
  Escenario: CP-HU36-001 — Ventas simultáneas del mismo SKU no pierden ni duplican unidades
    Dado que existe una tienda con almacén y bodega llamada "QA36-001" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA36-001"
    Y que la variante "QA36-001" tiene 20 unidades cargadas en el almacén de la tienda "QA36-001"
    Cuando se registran 8 ventas simultáneas de una unidad de la variante "QA36-001" en la tienda "QA36-001"
    Entonces todas las ventas simultáneas finalizan con código 201 o 409
    Y el stock de la variante "QA36-001" en el almacén de la tienda "QA36-001" es 20 menos las ventas confirmadas

  @CP-HU36-002 @positivo @no-funcional @fiabilidad
  Escenario: CP-HU36-002 — Reintentar la misma transacción en paralelo registra la venta una sola vez
    Dado que existe una tienda con almacén y bodega llamada "QA36-002" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA36-002"
    Y que la variante "QA36-002" tiene 10 unidades cargadas en el almacén de la tienda "QA36-002"
    Cuando se reintenta en paralelo 4 veces la misma venta de 2 unidades de la variante "QA36-002" en la tienda "QA36-002"
    Entonces todos los reintentos en paralelo finalizan con código 200, 201 o 409
    Y el reintento final confirma la venta
    Y el stock de la variante "QA36-002" en el almacén de la tienda "QA36-002" es 8
