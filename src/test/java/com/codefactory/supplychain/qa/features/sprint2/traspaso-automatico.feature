# language: es
@reposicion @sprint2
Característica: Traspaso automático
  Como usuario de la cadena de suministro
  Quiero que la Bodega reponga el Almacén cuando quede bajo el mínimo
  Para mantener el stock disponible sin intervención manual

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU48-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU48-001 — Una venta deja el Almacén bajo el mínimo y se repone en la misma respuesta
    Dado que existe una tienda llamada "t-48-001"
    Y que existe una categoría llamada "cat-48-001"
    Y que existe un template llamado "tmpl-48-001" en la categoría "cat-48-001"
    Y que existe una variante para el template "tmpl-48-001"
    Y que la tienda "t-48-001" tiene 3 unidades de la variante "tmpl-48-001" en el almacén
    Y que la tienda "t-48-001" tiene 10 unidades de la variante "tmpl-48-001" en la bodega
    Y que existe un umbral para la variante "tmpl-48-001" con mínimo 3 y objetivo 6
    Y que existe un vendedor asignado a la tienda "t-48-001"
    Cuando el vendedor asignado registra una venta de 1 unidad de la variante "tmpl-48-001" en la tienda "t-48-001"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-001" tiene 6 unidades de la variante "tmpl-48-001"
    Y la bodega de la tienda "t-48-001" tiene 6 unidades de la variante "tmpl-48-001"
    Y la tienda "t-48-001" tiene 1 traspaso registrado
    Y el traspaso número 1 de la tienda "t-48-001" es automático con cantidad 4 de la variante "tmpl-48-001"

  @CP-HU48-002 @negativo @funcional @caso-borde
  Escenario: CP-HU48-002 — Quedar exactamente en el mínimo no dispara traspaso (límite)
    Dado que existe una tienda llamada "t-48-002"
    Y que existe una categoría llamada "cat-48-002"
    Y que existe un template llamado "tmpl-48-002" en la categoría "cat-48-002"
    Y que existe una variante para el template "tmpl-48-002"
    Y que la tienda "t-48-002" tiene 4 unidades de la variante "tmpl-48-002" en el almacén
    Y que la tienda "t-48-002" tiene 10 unidades de la variante "tmpl-48-002" en la bodega
    Y que existe un umbral para la variante "tmpl-48-002" con mínimo 3 y objetivo 6
    Cuando se registra una venta de 1 unidad de la variante "tmpl-48-002" en la tienda "t-48-002"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-002" tiene 3 unidades de la variante "tmpl-48-002"
    Y la bodega de la tienda "t-48-002" tiene 10 unidades de la variante "tmpl-48-002"
    Y la tienda "t-48-002" no tiene traspasos registrados

  @CP-HU48-003 @negativo @funcional @caso-borde
  Escenario: CP-HU48-003 — Si la Bodega no alcanza, el traspaso es parcial y la deja en 0
    Dado que existe una tienda llamada "t-48-003"
    Y que existe una categoría llamada "cat-48-003"
    Y que existe un template llamado "tmpl-48-003" en la categoría "cat-48-003"
    Y que existe una variante para el template "tmpl-48-003"
    Y que la tienda "t-48-003" tiene 3 unidades de la variante "tmpl-48-003" en el almacén
    Y que la tienda "t-48-003" tiene 2 unidades de la variante "tmpl-48-003" en la bodega
    Y que existe un umbral para la variante "tmpl-48-003" con mínimo 3 y objetivo 6
    Cuando se registra una venta de 1 unidad de la variante "tmpl-48-003" en la tienda "t-48-003"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-003" tiene 4 unidades de la variante "tmpl-48-003"
    Y la bodega de la tienda "t-48-003" tiene 0 unidades de la variante "tmpl-48-003"
    Y el traspaso número 1 de la tienda "t-48-003" tiene cantidad 2

  @CP-HU48-004 @positivo @funcional @caso-borde
  Escenario: CP-HU48-004 — Con Almacén negativo la reposición usa MAX(almacén, 0)
    Dado que existe una tienda llamada "t-48-004"
    Y que existe una categoría llamada "cat-48-004"
    Y que existe un template llamado "tmpl-48-004" en la categoría "cat-48-004"
    Y que existe una variante para el template "tmpl-48-004"
    Y que la tienda "t-48-004" tiene 1 unidad de la variante "tmpl-48-004" en el almacén
    Y que la tienda "t-48-004" tiene 20 unidades de la variante "tmpl-48-004" en la bodega
    Y que existe un umbral para la variante "tmpl-48-004" con mínimo 3 y objetivo 6
    Cuando se registra una venta forzada de 3 unidades de la variante "tmpl-48-004" en la tienda "t-48-004"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-004" tiene 4 unidades de la variante "tmpl-48-004"
    Y la bodega de la tienda "t-48-004" tiene 14 unidades de la variante "tmpl-48-004"
    Y el traspaso número 1 de la tienda "t-48-004" tiene cantidad 6

  @CP-HU48-005 @positivo @funcional @caso-borde
  Escenario: CP-HU48-005 — El umbral de la variante gana sobre el de su categoría
    Dado que existe una tienda llamada "t-48-005"
    Y que existe una categoría llamada "cat-48-005"
    Y que existe un template llamado "tmpl-48-005" en la categoría "cat-48-005"
    Y que existe una variante para el template "tmpl-48-005"
    Y que la tienda "t-48-005" tiene 3 unidades de la variante "tmpl-48-005" en el almacén
    Y que la tienda "t-48-005" tiene 30 unidades de la variante "tmpl-48-005" en la bodega
    Y que existe un umbral para la categoría "cat-48-005" con mínimo 3 y objetivo 12
    Y que existe un umbral para la variante "tmpl-48-005" con mínimo 3 y objetivo 6
    Cuando se registra una venta de 1 unidad de la variante "tmpl-48-005" en la tienda "t-48-005"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-005" tiene 6 unidades de la variante "tmpl-48-005"

  @CP-HU48-006 @negativo @funcional @flujo-principal
  Escenario: CP-HU48-006 — Sin umbral de variante aplica el de su categoría
    Dado que existe una tienda llamada "t-48-006"
    Y que existe una categoría llamada "cat-48-006"
    Y que existe un template llamado "tmpl-48-006" en la categoría "cat-48-006"
    Y que existe una variante para el template "tmpl-48-006"
    Y que la tienda "t-48-006" tiene 3 unidades de la variante "tmpl-48-006" en el almacén
    Y que la tienda "t-48-006" tiene 30 unidades de la variante "tmpl-48-006" en la bodega
    Y que existe un umbral para la categoría "cat-48-006" con mínimo 3 y objetivo 10
    Cuando se registra una venta de 1 unidad de la variante "tmpl-48-006" en la tienda "t-48-006"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-006" tiene 10 unidades de la variante "tmpl-48-006"
    Y la bodega de la tienda "t-48-006" tiene 22 unidades de la variante "tmpl-48-006"

  @CP-HU48-007 @positivo @funcional @flujo-principal
  Escenario: CP-HU48-007 — Un aumento de la Bodega con el Almacén bajo el mínimo dispara el traspaso
    Dado que existe una tienda llamada "t-48-007"
    Y que existe una categoría llamada "cat-48-007"
    Y que existe un template llamado "tmpl-48-007" en la categoría "cat-48-007"
    Y que existe una variante para el template "tmpl-48-007"
    Y que la tienda "t-48-007" tiene 1 unidad de la variante "tmpl-48-007" en el almacén
    Y que existe un umbral para la variante "tmpl-48-007" con mínimo 3 y objetivo 6
    Cuando se cargan 10 unidades de la variante "tmpl-48-007" en la bodega de la tienda "t-48-007"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-007" tiene 6 unidades de la variante "tmpl-48-007"
    Y la bodega de la tienda "t-48-007" tiene 5 unidades de la variante "tmpl-48-007"
    Y la tienda "t-48-007" tiene 1 traspaso automático

  @CP-HU48-008 @positivo @funcional @flujo-principal
  Escenario: CP-HU48-008 — Un ajuste positivo de la Bodega también dispara el traspaso
    Dado que existe una tienda llamada "t-48-008"
    Y que existe una categoría llamada "cat-48-008"
    Y que existe un template llamado "tmpl-48-008" en la categoría "cat-48-008"
    Y que existe una variante para el template "tmpl-48-008"
    Y que la tienda "t-48-008" tiene 0 unidades de la variante "tmpl-48-008" en el almacén
    Y que la tienda "t-48-008" tiene 0 unidades de la variante "tmpl-48-008" en la bodega
    Y que existe un umbral para la variante "tmpl-48-008" con mínimo 2 y objetivo 5
    Cuando se registra un ajuste positivo de 8 unidades de la variante "tmpl-48-008" en la bodega de la tienda "t-48-008" con motivo "INGRESO_MANUAL"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-008" tiene 5 unidades de la variante "tmpl-48-008"
    Y la bodega de la tienda "t-48-008" tiene 3 unidades de la variante "tmpl-48-008"

  @CP-HU48-009 @negativo @funcional @caso-borde
  Escenario: CP-HU48-009 — Sin umbral configurado una venta no dispara traspaso
    Dado que existe una tienda llamada "t-48-009"
    Y que existe una categoría llamada "cat-48-009"
    Y que existe un template llamado "tmpl-48-009" en la categoría "cat-48-009"
    Y que existe una variante para el template "tmpl-48-009"
    Y que la tienda "t-48-009" tiene 2 unidades de la variante "tmpl-48-009" en el almacén
    Y que la tienda "t-48-009" tiene 10 unidades de la variante "tmpl-48-009" en la bodega
    Cuando se registra una venta de 2 unidades de la variante "tmpl-48-009" en la tienda "t-48-009"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-009" tiene 0 unidades de la variante "tmpl-48-009"
    Y la bodega de la tienda "t-48-009" tiene 10 unidades de la variante "tmpl-48-009"
    Y la tienda "t-48-009" no tiene traspasos registrados

  @CP-HU48-010 @positivo @funcional @flujo-principal
  Escenario: CP-HU48-010 — El traspaso automático que repone el Almacén cierra la ventana de quiebre
    Dado que existe una tienda llamada "t-48-010"
    Y que existe una categoría llamada "cat-48-010"
    Y que existe un template llamado "tmpl-48-010" en la categoría "cat-48-010"
    Y que existe una variante para el template "tmpl-48-010"
    Y que la tienda "t-48-010" tiene 1 unidad de la variante "tmpl-48-010" en el almacén
    Y que existe un umbral para la variante "tmpl-48-010" con mínimo 1 y objetivo 4
    Cuando se registra una venta de 1 unidad de la variante "tmpl-48-010" en la tienda "t-48-010"
    Entonces la respuesta es 201
    Y la ventana de quiebre de la variante "tmpl-48-010" en la tienda "t-48-010" está abierta
    Cuando se cargan 10 unidades de la variante "tmpl-48-010" en la bodega de la tienda "t-48-010"
    Entonces la respuesta es 201
    Y el almacén de la tienda "t-48-010" tiene 4 unidades de la variante "tmpl-48-010"
    Y la ventana de quiebre de la variante "tmpl-48-010" en la tienda "t-48-010" está cerrada con motivo "REPOSICION"
