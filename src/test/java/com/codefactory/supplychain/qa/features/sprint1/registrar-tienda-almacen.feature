# language: es
@inventario @sprint1
Característica: Registrar Tienda/Almacén
  Como administrador de la cadena de suministro
  Quiero registrar tiendas con su Almacén y Bodega
  Para poder operar inventario y ventas sobre ellas

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU03-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU03-001 — Registrar Tienda con información válida y ubicación
    Cuando registro una tienda con nombre único y ubicación "Medellín, Antioquia"
    Entonces la respuesta es 201
    Y el campo "nombre" es el nombre único de la tienda
    Y el campo "ubicacion" es "Medellín, Antioquia"
    Y el campo "estado" es "ACTIVA"

  @CP-HU03-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU03-002 — Rechazar registro de Tienda con información obligatoria incompleta
    Cuando registro una tienda con nombre vacío y ubicación "Sin nombre"
    Entonces la respuesta es 400

  @CP-HU03-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU03-003 — Rechazar registro de Tienda duplicada
    Dado que ya existe una tienda registrada con nombre único
    Cuando registro otra tienda con el mismo nombre
    Entonces la respuesta es 409
