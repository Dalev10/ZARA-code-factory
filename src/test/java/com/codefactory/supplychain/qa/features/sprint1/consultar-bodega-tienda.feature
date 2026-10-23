# language: es
@inventario @sprint1
Característica: Consultar Bodega_Tienda
  Como usuario de la cadena de suministro
  Quiero consultar la Bodega de una Tienda y su inventario
  Para conocer la información logística asociada

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU08-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU08-001 — Consultar Bodega_Tienda existente y su Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU08-001" ubicada en "Copacabana, Antioquia"
    Cuando consulto directamente la bodega de la tienda "HU08-001"
    Entonces la respuesta es 200
    Y el campo "tiendaId" es el identificador de la tienda "HU08-001"
    Y el campo "tiendaNombre" es el nombre de la tienda "HU08-001"

  @CP-HU08-002 @integracion @funcional @flujo-principal
  Escenario: CP-HU08-002 — Identificar el inventario asociado a la Bodega_Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU08-002" ubicada en "Copacabana, Antioquia"
    Cuando consulto directamente la bodega de la tienda "HU08-002"
    Entonces la respuesta es 200
    Y el campo "inventario" está vacío
