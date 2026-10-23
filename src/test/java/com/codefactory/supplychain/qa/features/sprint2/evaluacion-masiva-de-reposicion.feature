# language: es
@reposicion @sprint2
Característica: Evaluación masiva de reposición
  Como usuario de la cadena de suministro
  Quiero evaluar una tienda o todas las tiendas activas
  Para ejecutar los traspasos de reposición que correspondan

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU50-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU50-001 — Evaluar una tienda tras subir el mínimo ejecuta el traspaso
    Dado que existe una tienda llamada "t-50-001"
    Y que existe una categoría llamada "cat-50-001"
    Y que existe un template llamado "tmpl-50-001" en la categoría "cat-50-001"
    Y que existe una variante para el template "tmpl-50-001"
    Y que la tienda "t-50-001" tiene 4 unidades de la variante "tmpl-50-001" en el almacén
    Y que la tienda "t-50-001" tiene 20 unidades de la variante "tmpl-50-001" en la bodega
    Y que existe un umbral para la variante "tmpl-50-001" con mínimo 2 y objetivo 5
    Cuando modifico el umbral de la variante "tmpl-50-001" a mínimo 6 y objetivo 10
    Entonces la respuesta es 200
    Y el almacén de la tienda "t-50-001" tiene 4 unidades de la variante "tmpl-50-001"
    Cuando evalúo la reposición de la tienda "t-50-001"
    Entonces la respuesta es 200
    Y el campo "traspasoEjecutados" es 1
    Y el campo "traspasos[0].origen" es "AUTOMATICO"
    Y el campo "traspasos[0].cantidad" es 6
    Y el almacén de la tienda "t-50-001" tiene 10 unidades de la variante "tmpl-50-001"
    Y la bodega de la tienda "t-50-001" tiene 14 unidades de la variante "tmpl-50-001"

  @CP-HU50-002 @negativo @funcional @caso-borde
  Escenario: CP-HU50-002 — Evaluar dos veces seguidas no ejecuta traspasos la segunda vez
    Dado que existe una tienda llamada "t-50-002"
    Y que existe una categoría llamada "cat-50-002"
    Y que existe un template llamado "tmpl-50-002" en la categoría "cat-50-002"
    Y que existe una variante para el template "tmpl-50-002"
    Y que la tienda "t-50-002" tiene 1 unidad de la variante "tmpl-50-002" en el almacén
    Y que la tienda "t-50-002" tiene 20 unidades de la variante "tmpl-50-002" en la bodega
    Y que existe un umbral para la variante "tmpl-50-002" con mínimo 3 y objetivo 6
    Cuando evalúo la reposición de la tienda "t-50-002"
    Entonces la respuesta es 200
    Y el campo "traspasoEjecutados" es 1
    Cuando evalúo la reposición de la tienda "t-50-002"
    Entonces la respuesta es 200
    Y el campo "traspasoEjecutados" es 0
    Y el almacén de la tienda "t-50-002" tiene 6 unidades de la variante "tmpl-50-002"

  @CP-HU50-003 @positivo @funcional @flujo-principal
  Escenario: CP-HU50-003 — Evaluar todas las tiendas activas repone cada una
    Dado que existe una tienda llamada "t-50-003A"
    Y que existe una tienda llamada "t-50-003B"
    Y que existe una categoría llamada "cat-50-003"
    Y que existe un template llamado "tmpl-50-003" en la categoría "cat-50-003"
    Y que existe una variante para el template "tmpl-50-003"
    Y que la tienda "t-50-003A" tiene 1 unidad de la variante "tmpl-50-003" en el almacén
    Y que la tienda "t-50-003A" tiene 10 unidades de la variante "tmpl-50-003" en la bodega
    Y que la tienda "t-50-003B" tiene 1 unidad de la variante "tmpl-50-003" en el almacén
    Y que la tienda "t-50-003B" tiene 10 unidades de la variante "tmpl-50-003" en la bodega
    Y que existe un umbral para la variante "tmpl-50-003" con mínimo 3 y objetivo 5
    Cuando evalúo la reposición de todas las tiendas activas
    Entonces la respuesta es 200
    Y se ejecutaron al menos 2 traspasos
    Y el almacén de la tienda "t-50-003A" tiene 5 unidades de la variante "tmpl-50-003"
    Y el almacén de la tienda "t-50-003B" tiene 5 unidades de la variante "tmpl-50-003"

  @CP-HU50-004 @negativo @funcional @caso-borde
  Escenario: CP-HU50-004 — La evaluación masiva omite las tiendas inactivas
    Dado que existe una tienda llamada "t-50-004"
    Y que existe una categoría llamada "cat-50-004"
    Y que existe un template llamado "tmpl-50-004" en la categoría "cat-50-004"
    Y que existe una variante para el template "tmpl-50-004"
    Y que la tienda "t-50-004" tiene 1 unidad de la variante "tmpl-50-004" en el almacén
    Y que la tienda "t-50-004" tiene 10 unidades de la variante "tmpl-50-004" en la bodega
    Y que existe un umbral para la variante "tmpl-50-004" con mínimo 3 y objetivo 5
    Cuando desactivo la tienda "t-50-004"
    Entonces la respuesta es 204
    Cuando evalúo la reposición de todas las tiendas activas
    Entonces la respuesta es 200
    Y el almacén de la tienda "t-50-004" tiene 1 unidad de la variante "tmpl-50-004"
    Y la bodega de la tienda "t-50-004" tiene 10 unidades de la variante "tmpl-50-004"

  @CP-HU50-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU50-005 — Evaluar una tienda inexistente responde 404
    Cuando evalúo la reposición de una tienda inexistente
    Entonces la respuesta es 404

  @CP-HU50-006 @negativo @no-funcional @seguridad
  Escenario: CP-HU50-006 — La evaluación masiva exige reposicion:administrar (403 para un supervisor)
    Dado que existe un supervisor de reposición llamado "supervisor-50-006"
    Cuando el supervisor "supervisor-50-006" evalúa la reposición de todas las tiendas activas
    Entonces la respuesta es 403
