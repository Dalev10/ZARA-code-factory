# language: es
@reposicion @sprint2
Característica: Umbrales de reposición
  Como usuario de la cadena de suministro
  Quiero configurar los umbrales de reposición por variante o por categoría
  Para que la reposición respete el mínimo y el objetivo definidos

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU47-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU47-001 — Crear un umbral por variante y consultarlo
    Dado que existe una categoría llamada "cat-47-001"
    Y que existe un template llamado "tmpl-47-001" en la categoría "cat-47-001"
    Y que existe una variante para el template "tmpl-47-001"
    Cuando registro un umbral para la variante "tmpl-47-001" con mínimo 3 y objetivo 8
    Entonces la respuesta es 201
    Y el umbral pertenece a la variante "tmpl-47-001"
    Cuando consulto el umbral creado
    Entonces la respuesta es 200
    Y el campo "cantidadMinima" es 3
    Y el campo "cantidadObjetivo" es 8

  @CP-HU47-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU47-002 — Crear un umbral por categoría
    Dado que existe una categoría llamada "cat-47-002"
    Cuando registro un umbral para la categoría "cat-47-002" con mínimo 2 y objetivo 5
    Entonces la respuesta es 201
    Y el umbral pertenece a la categoría "cat-47-002"

  @CP-HU47-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU47-003 — Rechazar un umbral con variante y categoría a la vez, o sin ninguna (400)
    Dado que existe una categoría llamada "cat-47-003"
    Y que existe un template llamado "tmpl-47-003" en la categoría "cat-47-003"
    Y que existe una variante para el template "tmpl-47-003"
    Cuando registro un umbral para la variante "tmpl-47-003" y la categoría "cat-47-003" con mínimo 2 y objetivo 5
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Cuando registro un umbral sin variante ni categoría con mínimo 2 y objetivo 5
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU47-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU47-004 — Rechazar mínimo ≤ 0 u objetivo ≤ mínimo (400)
    Dado que existe una categoría llamada "cat-47-004"
    Y que existe un template llamado "tmpl-47-004" en la categoría "cat-47-004"
    Y que existe una variante para el template "tmpl-47-004"
    Cuando registro un umbral para la variante "tmpl-47-004" con mínimo 0 y objetivo 5
    Entonces la respuesta es 400
    Cuando registro un umbral para la variante "tmpl-47-004" con mínimo 5 y objetivo 5
    Entonces la respuesta es 400
    Cuando registro un umbral para la variante "tmpl-47-004" con mínimo 6 y objetivo 5
    Entonces la respuesta es 400

  @CP-HU47-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU47-005 — Rechazar un segundo umbral para el mismo alcance (409)
    Dado que existe una categoría llamada "cat-47-005"
    Y que existe un template llamado "tmpl-47-005" en la categoría "cat-47-005"
    Y que existe una variante para el template "tmpl-47-005"
    Y que existe un umbral para la variante "tmpl-47-005" con mínimo 2 y objetivo 5
    Cuando registro un umbral para la variante "tmpl-47-005" con mínimo 3 y objetivo 9
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío

  @CP-HU47-006 @positivo @funcional @flujo-principal
  Escenario: CP-HU47-006 — Modificar las cantidades de un umbral y eliminarlo
    Dado que existe una categoría llamada "cat-47-006"
    Y que existe un template llamado "tmpl-47-006" en la categoría "cat-47-006"
    Y que existe una variante para el template "tmpl-47-006"
    Y que existe un umbral para la variante "tmpl-47-006" con mínimo 2 y objetivo 5
    Cuando modifico el umbral de la variante "tmpl-47-006" a mínimo 4 y objetivo 10
    Entonces la respuesta es 200
    Y el campo "cantidadMinima" es 4
    Y el campo "cantidadObjetivo" es 10
    Cuando modifico el umbral de la variante "tmpl-47-006" a mínimo 10 y objetivo 4
    Entonces la respuesta es 400
    Cuando elimino el umbral de la variante "tmpl-47-006"
    Entonces la respuesta es 204
    Cuando consulto el umbral de la variante "tmpl-47-006"
    Entonces la respuesta es 404

  @CP-HU47-007 @negativo @funcional @caso-borde
  Escenario: CP-HU47-007 — Crear un umbral no repone por sí solo
    Dado que existe una tienda llamada "t-47-007"
    Y que existe una categoría llamada "cat-47-007"
    Y que existe un template llamado "tmpl-47-007" en la categoría "cat-47-007"
    Y que existe una variante para el template "tmpl-47-007"
    Y que la tienda "t-47-007" tiene 1 unidad de la variante "tmpl-47-007" en el almacén
    Y que la tienda "t-47-007" tiene 20 unidades de la variante "tmpl-47-007" en la bodega
    Cuando registro un umbral para la variante "tmpl-47-007" con mínimo 3 y objetivo 6
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-47-007" tiene 1 unidad de la variante "tmpl-47-007"
    Y la bodega de la tienda "t-47-007" tiene 20 unidades de la variante "tmpl-47-007"
    Y la tienda "t-47-007" no tiene traspasos registrados

  @CP-HU47-008 @negativo @funcional @ruta-negativa
  Escenario: CP-HU47-008 — Un umbral sobre una variante inexistente responde 404
    Cuando registro un umbral para una variante inexistente con mínimo 2 y objetivo 5
    Entonces la respuesta es 404

  @CP-HU47-009 @negativo @no-funcional @seguridad
  Escenario: CP-HU47-009 — Administrar umbrales exige reposicion:administrar (403 para un supervisor)
    Dado que existe una categoría llamada "cat-47-009"
    Y que existe un template llamado "tmpl-47-009" en la categoría "cat-47-009"
    Y que existe una variante para el template "tmpl-47-009"
    Y que existe un supervisor de reposición llamado "supervisor-47-009"
    Cuando el supervisor "supervisor-47-009" intenta registrar un umbral para la variante "tmpl-47-009" con mínimo 2 y objetivo 5
    Entonces la respuesta es 403
