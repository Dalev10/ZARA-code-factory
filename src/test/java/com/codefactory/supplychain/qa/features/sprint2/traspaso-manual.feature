# language: es
@reposicion @sprint2
Característica: Traspaso manual
  Como usuario de la cadena de suministro
  Quiero mover stock de la Bodega al Almacén de una tienda
  Para reponer manualmente cuando la operación lo requiera

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU49-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU49-001 — Registrar un traspaso manual mueve stock de la Bodega al Almacén
    Dado que existe una tienda llamada "t-49-001"
    Y que existe una categoría llamada "cat-49-001"
    Y que existe un template llamado "tmpl-49-001" en la categoría "cat-49-001"
    Y que existe una variante para el template "tmpl-49-001"
    Y que la tienda "t-49-001" tiene 2 unidades de la variante "tmpl-49-001" en el almacén
    Y que la tienda "t-49-001" tiene 10 unidades de la variante "tmpl-49-001" en la bodega
    Cuando registro un traspaso manual de 4 unidades de la variante "tmpl-49-001" en la tienda "t-49-001"
    Entonces la respuesta es 201
    Y el campo "origen" es "MANUAL"
    Y el campo "cantidad" es 4
    Y el traspaso pertenece a la tienda "t-49-001"
    Y el traspaso fue registrado por el administrador
    Y el campo "timestampTraspaso" termina en Z
    Y el almacén de la tienda "t-49-001" tiene 6 unidades de la variante "tmpl-49-001"
    Y la bodega de la tienda "t-49-001" tiene 6 unidades de la variante "tmpl-49-001"

  @CP-HU49-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU49-002 — Un traspaso mayor que la Bodega se rechaza (409) sin mover stock
    Dado que existe una tienda llamada "t-49-002"
    Y que existe una categoría llamada "cat-49-002"
    Y que existe un template llamado "tmpl-49-002" en la categoría "cat-49-002"
    Y que existe una variante para el template "tmpl-49-002"
    Y que la tienda "t-49-002" tiene 3 unidades de la variante "tmpl-49-002" en la bodega
    Cuando registro un traspaso manual de 4 unidades de la variante "tmpl-49-002" en la tienda "t-49-002"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío
    Y la bodega de la tienda "t-49-002" tiene 3 unidades de la variante "tmpl-49-002"
    Y el almacén de la tienda "t-49-002" tiene 0 unidades de la variante "tmpl-49-002"

  @CP-HU49-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU49-003 — Rechazar cantidad 0 o sin variante (400)
    Dado que existe una tienda llamada "t-49-003"
    Y que existe una categoría llamada "cat-49-003"
    Y que existe un template llamado "tmpl-49-003" en la categoría "cat-49-003"
    Y que existe una variante para el template "tmpl-49-003"
    Cuando registro un traspaso manual de 0 unidades de la variante "tmpl-49-003" en la tienda "t-49-003"
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Cuando registro un traspaso manual sin variante con cantidad 1 en la tienda "t-49-003"
    Entonces la respuesta es 400

  @CP-HU49-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU49-004 — Una variante inexistente responde 404
    Dado que existe una tienda llamada "t-49-004"
    Y que existe una categoría llamada "cat-49-004"
    Y que existe un template llamado "tmpl-49-004" en la categoría "cat-49-004"
    Y que existe una variante para el template "tmpl-49-004"
    Cuando registro un traspaso manual de 1 unidad de una variante inexistente en la tienda "t-49-004"
    Entonces la respuesta es 404

  @CP-HU49-005 @negativo @no-funcional @seguridad
  Escenario: CP-HU49-005 — Un usuario con reposicion:administrar sin la tienda asignada recibe 403
    Dado que existe una tienda llamada "t-49-005"
    Y que existe una categoría llamada "cat-49-005"
    Y que existe un template llamado "tmpl-49-005" en la categoría "cat-49-005"
    Y que existe una variante para el template "tmpl-49-005"
    Y que la tienda "t-49-005" tiene 5 unidades de la variante "tmpl-49-005" en la bodega
    Y que existe un usuario de reposición con el scope "reposicion:administrar" llamado "usuario-49-005"
    Cuando el usuario "usuario-49-005" registra un traspaso manual de 1 unidad de la variante "tmpl-49-005" en la tienda "t-49-005"
    Entonces la respuesta es 403
    Cuando el usuario "usuario-49-005" lista los traspasos de la tienda "t-49-005"
    Entonces la respuesta es 403
    Y la bodega de la tienda "t-49-005" tiene 5 unidades de la variante "tmpl-49-005"

  @CP-HU49-006 @positivo @funcional @flujo-principal
  Escenario: CP-HU49-006 — Listar los traspasos de una tienda y consultar uno por id
    Dado que existe una tienda llamada "t-49-006"
    Y que existe una categoría llamada "cat-49-006"
    Y que existe un template llamado "tmpl-49-006" en la categoría "cat-49-006"
    Y que existe una variante para el template "tmpl-49-006"
    Y que la tienda "t-49-006" tiene 10 unidades de la variante "tmpl-49-006" en la bodega
    Cuando registro un traspaso manual de 2 unidades de la variante "tmpl-49-006" en la tienda "t-49-006"
    Y registro un traspaso manual de 3 unidades de la variante "tmpl-49-006" en la tienda "t-49-006"
    Cuando listo los traspasos de la tienda "t-49-006"
    Entonces la respuesta es 200
    Y la tienda "t-49-006" tiene 2 traspasos registrados
    Cuando consulto el traspaso número 1 de la tienda "t-49-006"
    Entonces la respuesta es 200
    Y el traspaso consultado es el número 1 de la tienda "t-49-006"
    Y el campo "cantidad" es 2

  @CP-HU49-007 @negativo @funcional @ruta-negativa
  Escenario: CP-HU49-007 — Consultar un traspaso inexistente responde 404
    Cuando consulto un traspaso inexistente
    Entonces la respuesta es 404

  @CP-HU49-008 @positivo @funcional @flujo-principal
  Escenario: CP-HU49-008 — El traspaso queda en el kardex de la Bodega y del Almacén
    Dado que existe una tienda llamada "t-49-008"
    Y que existe una categoría llamada "cat-49-008"
    Y que existe un template llamado "tmpl-49-008" en la categoría "cat-49-008"
    Y que existe una variante para el template "tmpl-49-008"
    Y que la tienda "t-49-008" tiene 10 unidades de la variante "tmpl-49-008" en la bodega
    Cuando registro un traspaso manual de 3 unidades de la variante "tmpl-49-008" en la tienda "t-49-008"
    Entonces la respuesta es 201
    Cuando consulto el kardex de la bodega de la tienda "t-49-008" con tipo "TRASPASO_SALIDA"
    Entonces la respuesta es 200
    Y el kardex tiene 1 movimiento
    Cuando consulto el kardex del almacén de la tienda "t-49-008" con tipo "TRASPASO_ENTRADA"
    Entonces la respuesta es 200
    Y el kardex tiene 1 movimiento
