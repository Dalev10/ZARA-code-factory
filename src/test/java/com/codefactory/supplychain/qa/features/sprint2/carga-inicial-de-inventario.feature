# language: es
@inventario @sprint2
Característica: Carga inicial de inventario
  Como usuario de la cadena de suministro
  Quiero cargar el inventario inicial de un nodo por lote
  Para iniciar la operación con existencias conocidas

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU37-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU37-001 — Cargar inventario inicial válido en el Almacén de una tienda
    Dado que existe una tienda con almacén y bodega llamada "HU37-001" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-001A"
    Y que existe una variante de inventario llamada "HU37-001B"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-001" los ítems:
      | variante   | cantidad |
      | HU37-001A  | 12       |
      | HU37-001B  | 8        |
    Entonces la respuesta es 201
    Y el campo "nodoId" es el identificador del almacén de la tienda "HU37-001"
    Y el campo "filasCreadas" es 2
    Y el campo "unidadesTotales" es 20
    Y el stock de la variante "HU37-001A" en el almacén de la tienda "HU37-001" es 12
    Y el stock de la variante "HU37-001B" en el almacén de la tienda "HU37-001" es 8

  @CP-HU37-002 @positivo @funcional @caso-borde
  Escenario: CP-HU37-002 — Una cantidad en 0 cuenta como cargada y crea la fila
    Dado que existe una tienda con almacén y bodega llamada "HU37-002" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-002"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-002" los ítems:
      | variante  | cantidad |
      | HU37-002  | 0        |
    Entonces la respuesta es 201
    Y el campo "filasCreadas" es 1
    Y la variante "HU37-002" tiene fila de inventario en el almacén de la tienda "HU37-002"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-002" los ítems:
      | variante  | cantidad |
      | HU37-002  | 5        |
    Entonces la respuesta es 409

  @CP-HU37-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-003 — Un SKU inexistente rechaza todo el lote (400) y no carga nada
    Dado que existe una tienda con almacén y bodega llamada "HU37-003" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-003"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-003" los ítems:
      | variante                  | cantidad |
      | HU37-003                  | 10       |
      | SKU-NO-EXISTE-HU37        | 3        |
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Y la variante "HU37-003" no tiene fila de inventario en el almacén de la tienda "HU37-003"

  @CP-HU37-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-004 — Un SKU repetido en el lote lo rechaza (400)
    Dado que existe una tienda con almacén y bodega llamada "HU37-004" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-004"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-004" los ítems:
      | variante  | cantidad |
      | HU37-004  | 2        |
      | HU37-004  | 3        |
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Y la variante "HU37-004" no tiene fila de inventario en el almacén de la tienda "HU37-004"

  @CP-HU37-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-005 — Una variante que ya tiene fila en el nodo responde 409
    Dado que existe una tienda con almacén y bodega llamada "HU37-005" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-005"
    Y que la variante "HU37-005" tiene 4 unidades cargadas en el almacén de la tienda "HU37-005"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-005" los ítems:
      | variante  | cantidad |
      | HU37-005  | 9        |
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío
    Y el stock de la variante "HU37-005" en el almacén de la tienda "HU37-005" es 4

  @CP-HU37-006 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-006 — Rechazar una cantidad negativa (400)
    Dado que existe una tienda con almacén y bodega llamada "HU37-006" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-006"
    Cuando cargo inicialmente en el almacén de la tienda "HU37-006" los ítems:
      | variante  | cantidad |
      | HU37-006  | -1       |
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU37-007 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-007 — Rechazar un lote vacío y uno de más de 500 ítems (400)
    Dado que existe una tienda con almacén y bodega llamada "HU37-007" ubicada en "Medellín, Antioquia"
    Cuando cargo inicialmente un lote vacío en el almacén de la tienda "HU37-007"
    Entonces la respuesta es 400
    Cuando cargo inicialmente un lote de más de 500 ítems en el almacén de la tienda "HU37-007"
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU37-008 @positivo @funcional @flujo-principal
  Escenario: CP-HU37-008 — Cargar en la Bodega_Tienda y en un Centro de Distribución
    Dado que existe una tienda con almacén y bodega llamada "HU37-008" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-008"
    Y que existe un centro de distribución de inventario llamado "HU37-008" ubicado en "Copacabana, Antioquia"
    Cuando cargo inicialmente en la bodega de la tienda "HU37-008" los ítems:
      | variante  | cantidad |
      | HU37-008  | 15       |
    Entonces la respuesta es 201
    Cuando cargo inicialmente en el nodo del centro de distribución "HU37-008" los ítems:
      | variante  | cantidad |
      | HU37-008  | 100      |
    Entonces la respuesta es 201
    Y el stock de la variante "HU37-008" en la bodega de la tienda "HU37-008" es 15
    Y el stock de la variante "HU37-008" en el nodo del centro de distribución "HU37-008" es 100

  @CP-HU37-009 @negativo @no-funcional @seguridad
  Escenario: CP-HU37-009 — Un OPERADOR_CD no ve el nodo de una tienda (404) y un SUPERVISOR no carga en un CD (403)
    Dado que existe una tienda con almacén y bodega llamada "HU37-009" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-009"
    Y que existe un centro de distribución de inventario llamado "HU37-009" ubicado en "Barbosa, Antioquia"
    Y que existe un operador de centro de distribución
    Y que existe un supervisor de tienda asignado a la tienda "HU37-009"
    Cuando el operador de centro de distribución intenta cargar inicialmente en el almacén de la tienda "HU37-009" la variante "HU37-009" con 3 unidades
    Entonces la respuesta es 404
    Cuando el supervisor intenta cargar inicialmente en el nodo del centro de distribución "HU37-009" la variante "HU37-009" con 3 unidades
    Entonces la respuesta es 403
    Cuando el operador de centro de distribución carga inicialmente en el nodo del centro de distribución "HU37-009" la variante "HU37-009" con 3 unidades
    Entonces la respuesta es 201
    Cuando el supervisor carga inicialmente en el almacén de la tienda "HU37-009" la variante "HU37-009" con 3 unidades
    Entonces la respuesta es 201

  @CP-HU37-010 @negativo @no-funcional @seguridad
  Escenario: CP-HU37-010 — Un SUPERVISOR no carga en el nodo de una tienda que no tiene asignada (404)
    Dado que existe una tienda con almacén y bodega llamada "HU37-010" ubicada en "Medellín, Antioquia"
    Y que existe una tienda con almacén y bodega llamada "HU37-010B" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU37-010"
    Y que existe un supervisor de tienda asignado a la tienda "HU37-010"
    Cuando el supervisor intenta cargar inicialmente en el almacén de la tienda "HU37-010B" la variante "HU37-010" con 3 unidades
    Entonces la respuesta es 404
    Y la variante "HU37-010" no tiene fila de inventario en el almacén de la tienda "HU37-010B"

  @CP-HU37-011 @negativo @funcional @ruta-negativa
  Escenario: CP-HU37-011 — Cargar en un nodo inexistente responde 404
    Dado que existe una variante de inventario llamada "HU37-011"
    Cuando cargo inicialmente en un nodo inexistente la variante "HU37-011" con 3 unidades
    Entonces la respuesta es 404
