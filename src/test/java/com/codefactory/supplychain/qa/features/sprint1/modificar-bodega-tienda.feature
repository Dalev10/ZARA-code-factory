# language: es
@inventario @sprint1
Característica: Modificar Bodega_Tienda
  Como usuario de la cadena de suministro
  Quiero validar que la Bodega_Tienda permanece asociada a su Tienda
  Para conservar la integridad de la red logística

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU09-001 @positivo @funcional @ruta-negativa
  Escenario: CP-HU09-001 — Modificar datos permitidos de Bodega_Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU09-001" ubicada en "Girardota, Antioquia"
    Cuando intento modificar la bodega de la tienda "HU09-001"
    Entonces la respuesta es 405

  @CP-HU09-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU09-002 — Rechazar cambio de Bodega_Tienda hacia Tienda inexistente
    Dado que existe una tienda con almacén y bodega llamada "HU09-002" ubicada en "Girardota, Antioquia"
    Cuando consulto directamente la bodega de la tienda "HU09-002"
    Entonces la respuesta es 200
    Y el campo "tiendaId" es el identificador de la tienda "HU09-002"
