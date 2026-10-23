# language: es
@catalogo @sprint2
Característica: Categoría obligatoria en template
  Como administrador del catálogo
  Quiero exigir una categoría válida en cada template
  Para garantizar la integridad del catálogo y reservar el nombre SIN_CATEGORIA

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU35-001 @negativo @funcional @ruta-negativa
  Escenario: CP-HU35-001 — Rechazar un template sin categoría (400 con mensaje)
    Cuando registro un template llamado "Template HU35-001" sin categoría
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU35-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU35-002 — Crear un template con categoría la conserva
    Dado que existe una categoría llamada "Cat HU35-002"
    Cuando registro un template llamado "Template HU35-002" en la categoría "Cat HU35-002"
    Entonces la respuesta es 201
    Y el campo "categoriaId" es el identificador de la categoría "Cat HU35-002"

  @CP-HU35-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU35-003 — Rechazar crear la categoría reservada SIN_CATEGORIA, sin distinguir mayúsculas (409)
    Cuando registro la categoría "SIN_CATEGORIA"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío
    Cuando registro la categoría "sin_categoria"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío
    Cuando registro la categoría "Sin_Categoria"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío

  @CP-HU35-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU35-004 — Rechazar renombrar una categoría hacia SIN_CATEGORIA (409)
    Dado que existe una categoría llamada "Cat HU35-004"
    Cuando renombro la categoría "Cat HU35-004" a "sin_categoria"
    Entonces la respuesta es 409

  @CP-HU35-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU35-005 — Rechazar un template con una categoría inexistente
    Cuando registro un template llamado "Template HU35-005" con una categoría inexistente
    Entonces la respuesta es 404
