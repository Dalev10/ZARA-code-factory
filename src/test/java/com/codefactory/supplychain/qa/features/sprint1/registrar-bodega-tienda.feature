# language: es
@inventario @sprint1
Característica: Registrar Bodega_Tienda
  Como usuario de la cadena de suministro
  Quiero que la Bodega_Tienda se cree junto con su Tienda
  Para operar traspasos e inventario sobre ella

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU07-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU07-001 — Registrar Bodega_Tienda asociada a Tienda existente
    Cuando registro una tienda llamada "HU07-001" ubicada en "Rionegro, Antioquia"
    Entonces la respuesta es 201
    Cuando consulto la bodega de la tienda recién registrada
    Entonces la respuesta es 200
    Y el campo "tiendaId" es el identificador de la tienda recién registrada

  @CP-HU07-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU07-002 — Rechazar Bodega_Tienda sin Tienda asociada
    Cuando consulto la bodega de una tienda inexistente
    Entonces la respuesta es 404

  @CP-HU07-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU07-003 — Rechazar Bodega_Tienda duplicada
    Cuando intento registrar una bodega de tienda para una tienda inexistente
    Entonces la respuesta es 405
