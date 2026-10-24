# language: es
@ventas @sprint2
Característica: Registrar solicitud no satisfecha
  Como vendedor de una tienda
  Quiero registrar solicitudes no satisfechas
  Para dejar evidencia de la demanda que no pudo atenderse

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU43-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU43-001 — Registrar una solicitud guarda el stock del sistema aunque sea positivo
    Dado que existe una tienda de ventas llamada "QA43-001"
    Y que existe una variante de ventas llamada "QA43-001"
    Y que la variante "QA43-001" tiene 4 unidades en el almacén de la tienda "QA43-001"
    Y que existe un vendedor llamado "vendedorQA43" asignado a la tienda "QA43-001"
    Cuando registro una solicitud no satisfecha como "vendedorQA43" en la tienda "QA43-001" para la variante "QA43-001" con cantidad 2 y clave "claveQA43"
    Entonces la respuesta es 201
    Y el campo "stockSistema" es 4
    Y el campo "cantidad" es 2
    Y el campo "estado" es "ACTIVA"
    Y el campo "registradoPor" es el identificador del usuario "vendedorQA43"
    Y el stock de la variante "QA43-001" en el almacén de la tienda "QA43-001" es 4

  @CP-HU43-002 @negativo @funcional @caso-borde
  Escenario: CP-HU43-002 — La cantidad es opcional y sin fila el stock del sistema es 0
    Dado que existe una tienda de ventas llamada "QA43-002"
    Y que existe una variante de ventas llamada "QA43-002"
    Cuando registro una solicitud no satisfecha en la tienda "QA43-002" para la variante "QA43-002" sin cantidad y clave "claveQA43"
    Entonces la respuesta es 201
    Y el campo "cantidad" es 1
    Y el campo "stockSistema" es 0

  @CP-HU43-003 @negativo @funcional @caso-borde
  Escenario: CP-HU43-003 — La misma clave con los mismos datos responde 200 sin duplicar
    Dado que existe una tienda de ventas llamada "QA43-003"
    Y que existe una variante de ventas llamada "QA43-003"
    Cuando registro una solicitud no satisfecha en la tienda "QA43-003" para la variante "QA43-003" con cantidad 1 y clave "claveQA43"
    Entonces la respuesta es 201
    Cuando registro una solicitud no satisfecha en la tienda "QA43-003" para la variante "QA43-003" con cantidad 1 y clave "claveQA43"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la solicitud no satisfecha "claveQA43"
    Cuando consulto las solicitudes no satisfechas de la tienda "QA43-003"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos

  @CP-HU43-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU43-004 — La misma clave con otros datos responde 409
    Dado que existe una tienda de ventas llamada "QA43-004"
    Y que existe una variante de ventas llamada "QA43-004"
    Cuando registro una solicitud no satisfecha en la tienda "QA43-004" para la variante "QA43-004" con cantidad 1 y clave "claveQA43"
    Entonces la respuesta es 201
    Cuando registro una solicitud no satisfecha en la tienda "QA43-004" para la variante "QA43-004" con cantidad 3 y clave "claveQA43"
    Entonces la respuesta es 409
    Y el campo "mensaje" no es nulo

  @CP-HU43-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU43-005 — Una variante fuera de catálogo responde 404
    Dado que existe una tienda de ventas llamada "QA43-005"
    Cuando registro una solicitud no satisfecha en la tienda "QA43-005" para una variante inexistente con cantidad 1 y clave "claveQA43"
    Entonces la respuesta es 404
    Y el campo "mensaje" no es nulo

  @CP-HU43-006 @negativo @no-funcional @seguridad
  Escenario: CP-HU43-006 — Registrar en una tienda no asignada responde 403
    Dado que existe una tienda de ventas llamada "QA43-006"
    Y que existe una variante de ventas llamada "QA43-006"
    Y que existe un vendedor llamado "vendedorQA43" sin tiendas asignadas
    Cuando registro una solicitud no satisfecha como "vendedorQA43" en la tienda "QA43-006" para la variante "QA43-006" con cantidad 1 y clave "claveQA43"
    Entonces la respuesta es 403

  @CP-HU43-007 @negativo @funcional @ruta-negativa
  Escenario: CP-HU43-007 — Rechazar sin clave de idempotencia o con cantidad 0 (400)
    Dado que existe una tienda de ventas llamada "QA43-007"
    Y que existe una variante de ventas llamada "QA43-007"
    Cuando registro una solicitud no satisfecha en la tienda "QA43-007" para la variante "QA43-007" con cantidad 1 sin clave
    Entonces la respuesta es 400
    Cuando registro una solicitud no satisfecha en la tienda "QA43-007" para la variante "QA43-007" con cantidad 0 y clave "claveQA43"
    Entonces la respuesta es 400
