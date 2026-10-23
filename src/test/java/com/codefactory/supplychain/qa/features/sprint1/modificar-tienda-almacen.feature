# language: es
@inventario @sprint1
Característica: Modificar Tienda/Almacén
  Como usuario de la cadena de suministro
  Quiero modificar los datos permitidos de una Tienda
  Para mantener actualizada la información de la operación

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU05-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU05-001 — Modificar campos permitidos de una Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU05-001" ubicada en "Itagüí, Antioquia"
    Cuando modifico la tienda "HU05-001" con un nombre único y ubicación "Sabaneta, Antioquia"
    Entonces la respuesta es 200
    Y el campo "nombre" es el nombre nuevo de la tienda "HU05-001"
    Y el campo "ubicacion" es "Sabaneta, Antioquia"

  @CP-HU05-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU05-002 — Rechazar modificación inválida de una Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU05-002" ubicada en "Itagüí, Antioquia"
    Cuando modifico la tienda "HU05-002" con nombre vacío y ubicación "No debería guardarse"
    Entonces la respuesta es 400

  @CP-HU05-003 @integridad-de-datos @funcional @caso-borde
  Escenario: CP-HU05-003 — Conservar información histórica al modificar una Tienda
    Dado que existe una tienda con almacén y bodega llamada "HU05-003" ubicada en "Itagüí, Antioquia"
    Y que la bodega de la tienda "HU05-003" existe
    Cuando modifico la tienda "HU05-003" con un nombre único y ubicación "Envigado, Antioquia"
    Entonces la respuesta es 200
    Cuando consulto directamente la bodega de la tienda "HU05-003"
    Entonces la respuesta es 200
    Y el campo "tiendaId" es el identificador de la tienda "HU05-003"
