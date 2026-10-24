# language: es
@inventario @sprint1
Característica: Eliminar Tienda/Almacén
  Como usuario de la cadena de suministro
  Quiero desactivar una Tienda conservando su información
  Para mantener el historial de la operación

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU06-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU06-001 — Eliminar Tienda sin dependencias bloqueantes
    Dado que existe una tienda con almacén y bodega llamada "QA06-001" ubicada en "Envigado, Antioquia"
    Cuando elimino la tienda "QA06-001"
    Entonces la respuesta es 204
    Cuando consulto la tienda "QA06-001"
    Entonces la respuesta es 200
    Y el campo "estado" es "INACTIVA"

  @CP-HU06-002 @negativo @funcional @caso-borde
  Escenario: CP-HU06-002 — Bloquear eliminación de Tienda con Bodega_Tienda asociada
    Dado que existe una tienda con almacén y bodega llamada "QA06-002" ubicada en "Bello, Antioquia"
    Y que la bodega de la tienda "QA06-002" existe
    Cuando elimino la tienda "QA06-002"
    Entonces la respuesta es 409
    Cuando consulto la tienda "QA06-002"
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVA"

  @CP-HU06-003 @negativo @funcional @caso-borde
  Escenario: CP-HU06-003 — Bloquear eliminación de Tienda con operaciones o información asociada
    Dado que existe una tienda con almacén y bodega llamada "QA06-003" ubicada en "La Estrella, Antioquia"
    Y que la bodega de la tienda "QA06-003" existe
    Cuando elimino la tienda "QA06-003"
    Entonces la respuesta es 409
    Cuando consulto directamente la bodega de la tienda "QA06-003"
    Entonces la respuesta es 200
    Y el campo "tiendaId" es el identificador de la tienda "QA06-003"
    Cuando consulto la tienda "QA06-003"
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVA"

  @CP-HU06-004 @integridad-de-datos @funcional @caso-borde
  Escenario: CP-HU06-004 — Conservar información histórica después de eliminar una Tienda
    Dado que existe una tienda con almacén y bodega llamada "QA06-004" ubicada en "Caldas, Antioquia"
    Cuando elimino la tienda "QA06-004"
    Entonces la respuesta es 204
    Cuando consulto la tienda "QA06-004"
    Entonces la respuesta es 200
    Y el campo "nombre" es el nombre de la tienda "QA06-004"
    Y el campo "estado" es "INACTIVA"
