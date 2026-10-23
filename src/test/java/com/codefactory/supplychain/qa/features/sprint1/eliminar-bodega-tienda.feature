# language: es
@inventario @sprint1
Característica: Eliminar Bodega_Tienda
  Como usuario de la cadena de suministro
  Quiero validar que la Bodega_Tienda no se elimina por API
  Para conservar la información asociada a su Tienda

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU10-001 @positivo @funcional @ruta-negativa
  Escenario: CP-HU10-001 — Eliminar Bodega_Tienda elegible
    Dado que existe una tienda con almacén y bodega llamada "HU10-001" ubicada en "La Ceja, Antioquia"
    Cuando elimino la bodega de la tienda "HU10-001"
    Entonces la respuesta es 405
    Cuando consulto directamente la bodega de la tienda "HU10-001"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la bodega de la tienda "HU10-001"
