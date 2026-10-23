# language: es
@inventario @sprint1
Característica: Consultar Tienda/Almacén
  Como usuario de la cadena de suministro
  Quiero consultar una Tienda, su Bodega y su inventario
  Para conocer la información logística asociada

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU04-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU04-001 — Consultar Tienda existente, información básica y ubicación
    Dado que existe una tienda con almacén y bodega llamada "HU04-001" ubicada en "Envigado, Antioquia"
    Cuando consulto la tienda "HU04-001"
    Entonces la respuesta es 200
    Y el campo "nombre" es el nombre de la tienda "HU04-001"
    Y el campo "ubicacion" es "Envigado, Antioquia"

  @CP-HU04-002 @integracion @funcional @flujo-principal
  Escenario: CP-HU04-002 — Consultar Bodega_Tienda asociada a una Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU04-002" ubicada en "Envigado, Antioquia"
    Cuando consulto la bodega de la tienda "HU04-002"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la bodega de la tienda "HU04-002"
    Y el campo "tiendaNombre" es el nombre de la tienda "HU04-002"

  @CP-HU04-003 @integracion @funcional @flujo-principal
  Escenario: CP-HU04-003 — Consultar inventario correspondiente a una Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU04-003" ubicada en "Envigado, Antioquia"
    Cuando consulto directamente la bodega de la tienda "HU04-003"
    Entonces la respuesta es 200
    Y el campo "inventario" está vacío
