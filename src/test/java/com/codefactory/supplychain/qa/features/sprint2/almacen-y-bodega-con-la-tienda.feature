# language: es
@inventario @catalogo @sprint2
Característica: Almacén y Bodega con la tienda
  Como usuario de la cadena de suministro
  Quiero que cada tienda nazca con su Almacén y su Bodega
  Para operar inventario y ventas sobre sus nodos

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU34-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU34-001 — Crear una tienda devuelve los ids de su Almacén y su Bodega
    Cuando registro una tienda llamada "HU34-001" ubicada en "Medellín, Antioquia"
    Entonces la respuesta es 201
    Y el almacén y la bodega de la tienda "HU34-001" son nodos distintos

  @CP-HU34-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU34-002 — Consultar la tienda devuelve los mismos ids de nodo que la creación
    Cuando registro una tienda llamada "HU34-002" ubicada en "Medellín, Antioquia"
    Entonces la respuesta es 201
    Cuando consulto la tienda "HU34-002"
    Entonces la respuesta es 200
    Y el campo "almacenId" es el identificador del almacén de la tienda "HU34-002"
    Y el campo "bodegaId" es el identificador de la bodega de la tienda "HU34-002"

  @CP-HU34-003 @positivo @funcional @flujo-principal
  Escenario: CP-HU34-003 — La Bodega consultada por tienda coincide con el bodegaId de la tienda
    Cuando registro una tienda llamada "HU34-003" ubicada en "Medellín, Antioquia"
    Entonces la respuesta es 201
    Cuando consulto la bodega de la tienda "HU34-003"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la bodega de la tienda "HU34-003"

  @CP-HU34-004 @negativo @funcional @flujo-principal
  Escenario: CP-HU34-004 — El listado de tiendas no trae los ids de nodo
    Cuando registro una tienda llamada "HU34-004" ubicada en "Medellín, Antioquia"
    Entonces la respuesta es 201
    Cuando consulto el listado de tiendas
    Entonces la respuesta es 200
    Y el listado de tiendas no expone el almacén de la primera tienda

  @CP-HU34-005 @positivo @funcional @flujo-principal
  Escenario: CP-HU34-005 — El Centro de Distribución expone el id de su nodo en nodo.id
    Dado que existe un centro de distribución de inventario llamado "HU34-005" ubicado en "Girardota, Antioquia"
    Cuando consulto el centro de distribución "HU34-005"
    Entonces la respuesta es 200
    Y el campo "nodo.id" no es nulo
    Y el campo "nodo.tipo" es "CD"
    Y el campo "nodo.cdId" es el identificador del centro de distribución "HU34-005"

  @CP-HU34-006 @positivo @funcional @caso-borde
  Escenario: CP-HU34-006 — Una tienda desactivada conserva su Bodega (baja lógica, D-50)
    Cuando registro una tienda llamada "HU34-006" ubicada en "Medellín, Antioquia"
    Entonces la respuesta es 201
    Cuando elimino la tienda "HU34-006"
    Entonces la respuesta es 204
    Cuando consulto la tienda "HU34-006"
    Entonces la respuesta es 200
    Y el campo "estado" es "INACTIVA"
    Y el campo "bodegaId" es el identificador de la bodega de la tienda "HU34-006"
