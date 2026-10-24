# language: es
@inventario @sprint2
Característica: Consultar inventario y kardex
  Como usuario de la cadena de suministro
  Quiero consultar el stock por nodo y variante y el kardex de movimientos
  Para auditar las existencias y su historial

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU39-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU39-001 — Consultar el stock de un nodo
    Dado que existe una tienda con almacén y bodega llamada "QA39-001" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA39-001"
    Y que la variante "QA39-001" tiene 9 unidades cargadas en el almacén de la tienda "QA39-001"
    Cuando consulto el inventario del almacén de la tienda "QA39-001"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el inventario del nodo contiene la variante "QA39-001" con 9 unidades a la mano

  @CP-HU39-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU39-002 — Consultar el stock de una variante en todos sus nodos
    Dado que existe una tienda con almacén y bodega llamada "QA39-002" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA39-002"
    Y que la variante "QA39-002" tiene 4 unidades cargadas en el almacén de la tienda "QA39-002"
    Y que la variante "QA39-002" tiene 11 unidades cargadas en la bodega de la tienda "QA39-002"
    Cuando consulto el inventario de la variante "QA39-002"
    Entonces la respuesta es 200
    Y el inventario de la variante "QA39-002" tiene 2 filas
    Y el stock de la variante "QA39-002" en el almacén de la tienda "QA39-002" es 4
    Y el stock de la variante "QA39-002" en la bodega de la tienda "QA39-002" es 11

  @CP-HU39-003 @positivo @funcional @flujo-principal
  Escenario: CP-HU39-003 — El kardex muestra cada movimiento en orden, con fechas en UTC
    Dado que existe una tienda con almacén y bodega llamada "QA39-003" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA39-003"
    Y que la variante "QA39-003" tiene 10 unidades cargadas en el almacén de la tienda "QA39-003"
    Cuando ajusto la variante "QA39-003" con -2 unidades por motivo "MERMA" en el almacén de la tienda "QA39-003"
    Entonces la respuesta es 201
    Cuando registro una venta de 1 unidades de la variante "QA39-003" en la tienda "QA39-003"
    Entonces la respuesta es 201
    Cuando consulto el kardex del almacén de la tienda "QA39-003"
    Entonces la respuesta es 200
    Y todos los movimientos del kardex tienen fecha en UTC
    Y el kardex registra los movimientos "CARGA_INICIAL, AJUSTE, VENTA"

  @CP-HU39-004 @positivo @funcional @flujo-principal
  Escenario: CP-HU39-004 — Filtrar el kardex por tipo y por rango de fechas UTC
    Dado que existe una tienda con almacén y bodega llamada "QA39-004" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA39-004"
    Y que la variante "QA39-004" tiene 10 unidades cargadas en el almacén de la tienda "QA39-004"
    Cuando ajusto la variante "QA39-004" con 1 unidades por motivo "CONTEO_FISICO" en el almacén de la tienda "QA39-004"
    Entonces la respuesta es 201
    Cuando consulto el kardex del almacén de la tienda "QA39-004" filtrando por tipo "AJUSTE"
    Entonces la respuesta es 200
    Y el kardex tiene 1 movimientos
    Y el kardex contiene un único movimiento de tipo "AJUSTE" con motivo "CONTEO_FISICO"
    Cuando consulto el kardex del almacén de la tienda "QA39-004" desde una fecha futura
    Entonces la respuesta es 200
    Y el kardex está vacío
    Cuando consulto el kardex del almacén de la tienda "QA39-004" entre ayer y mañana
    Entonces la respuesta es 200
    Y el kardex tiene 2 movimientos

  @CP-HU39-005 @negativo @no-funcional @seguridad
  Escenario: CP-HU39-005 — Un vendedor no ve el inventario de una tienda que no tiene asignada (404)
    Dado que existe una tienda con almacén y bodega llamada "QA39-005A" ubicada en "Medellín, Antioquia"
    Y que existe una tienda con almacén y bodega llamada "QA39-005B" ubicada en "Medellín, Antioquia"
    Y que existe un vendedor asignado a la tienda "QA39-005A"
    Cuando el vendedor consulta el inventario del almacén de la tienda "QA39-005A"
    Entonces la respuesta es 200
    Cuando el vendedor consulta el inventario del almacén de la tienda "QA39-005B"
    Entonces la respuesta es 404
    Cuando el vendedor consulta el kardex del almacén de la tienda "QA39-005B"
    Entonces la respuesta es 404

  @CP-HU39-006 @positivo @no-funcional @seguridad
  Escenario: CP-HU39-006 — La consulta por variante solo muestra nodos de las tiendas asignadas
    Dado que existe una tienda con almacén y bodega llamada "QA39-006A" ubicada en "Medellín, Antioquia"
    Y que existe una tienda con almacén y bodega llamada "QA39-006B" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "QA39-006"
    Y que la variante "QA39-006" tiene 3 unidades cargadas en el almacén de la tienda "QA39-006A"
    Y que la variante "QA39-006" tiene 8 unidades cargadas en el almacén de la tienda "QA39-006B"
    Y que existe un vendedor asignado a la tienda "QA39-006A"
    Cuando el vendedor consulta el inventario de la variante "QA39-006"
    Entonces la respuesta es 200
    Y el inventario de la variante "QA39-006" solo muestra el almacén de la tienda "QA39-006A"

  @CP-HU39-007 @negativo @no-funcional @seguridad
  Escenario: CP-HU39-007 — Un CD lo ven ADMIN y OPERADOR_CD; un VENDEDOR recibe 404
    Dado que existe una tienda con almacén y bodega llamada "QA39-007T" ubicada en "Bello, Antioquia"
    Y que existe un centro de distribución de inventario llamado "QA39-007" ubicado en "Bello, Antioquia"
    Y que existe un operador de centro de distribución
    Y que existe un vendedor asignado a la tienda "QA39-007T"
    Cuando consulto el inventario del nodo del centro de distribución "QA39-007"
    Entonces la respuesta es 200
    Cuando el operador de centro de distribución consulta el inventario del nodo del centro de distribución "QA39-007"
    Entonces la respuesta es 200
    Cuando el vendedor consulta el inventario del nodo del centro de distribución "QA39-007"
    Entonces la respuesta es 404

  @CP-HU39-008 @negativo @no-funcional @seguridad
  Escenario: CP-HU39-008 — Consultar inventario sin el scope inventario:consultar responde 403
    Dado que existe una tienda con almacén y bodega llamada "QA39-008" ubicada en "Medellín, Antioquia"
    Y que existe un usuario con solo el permiso "ventas:registrar" asignado a la tienda "QA39-008"
    Cuando el usuario sin permiso de consulta consulta el inventario del almacén de la tienda "QA39-008"
    Entonces la respuesta es 403
