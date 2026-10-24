# language: es
@ventas @sprint2
Característica: Consultar y anular solicitud no satisfecha
  Como usuario autorizado de la cadena
  Quiero consultar y anular solicitudes no satisfechas
  Para gestionar la demanda pendiente respetando el alcance por tienda

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU44-001 @negativo @no-funcional @seguridad
  Escenario: CP-HU44-001 — Consultar una solicitud por id; la de otra tienda responde 404
    Dado que existe una tienda de ventas llamada "propia"
    Y que existe una tienda de ventas llamada "ajena"
    Y que existe una variante de ventas llamada "QA44-001"
    Cuando registro una solicitud no satisfecha en la tienda "ajena" para la variante "QA44-001" con cantidad 1 y clave "solicitudAjena"
    Entonces la respuesta es 201
    Y que existe un vendedor llamado "vendedorQA44" asignado a la tienda "propia"
    Cuando consulto la solicitud no satisfecha "solicitudAjena"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador de la solicitud no satisfecha "solicitudAjena"
    Cuando consulto la solicitud no satisfecha "solicitudAjena" como "vendedorQA44"
    Entonces la respuesta es 404

  @CP-HU44-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU44-002 — Anular una solicitud exige motivo y la deja ANULADA
    Dado que existe una tienda de ventas llamada "QA44-002"
    Y que existe una variante de ventas llamada "QA44-002"
    Cuando registro una solicitud no satisfecha en la tienda "QA44-002" para la variante "QA44-002" con cantidad 1 y clave "claveQA44"
    Entonces la respuesta es 201
    Cuando anulo la solicitud no satisfecha "claveQA44" sin motivo
    Entonces la respuesta es 400
    Y el campo "mensaje" no es nulo
    Cuando anulo la solicitud no satisfecha "claveQA44" con el motivo "Registro duplicado"
    Entonces la respuesta es 200
    Y el campo "estado" es "ANULADA"
    Y el campo "motivoAnulacion" es "Registro duplicado"
    Y el campo "anuladaEn" no es nulo

  @CP-HU44-003 @positivo @funcional @caso-borde
  Escenario: CP-HU44-003 — Anular una solicitud dos veces responde 200 con la anulación original
    Dado que existe una tienda de ventas llamada "QA44-003"
    Y que existe una variante de ventas llamada "QA44-003"
    Cuando registro una solicitud no satisfecha en la tienda "QA44-003" para la variante "QA44-003" con cantidad 1 y clave "claveQA44"
    Entonces la respuesta es 201
    Cuando anulo la solicitud no satisfecha "claveQA44" con el motivo "Primera"
    Entonces la respuesta es 200
    Cuando anulo la solicitud no satisfecha "claveQA44" con el motivo "Segunda"
    Entonces la respuesta es 200
    Y el campo "motivoAnulacion" es "Primera"

  @CP-HU44-004 @negativo @no-funcional @seguridad
  Escenario: CP-HU44-004 — Un vendedor no puede anular solicitudes (403)
    Dado que existe una tienda de ventas llamada "QA44-004"
    Y que existe una variante de ventas llamada "QA44-004"
    Y que existe un vendedor llamado "vendedorQA44" asignado a la tienda "QA44-004"
    Cuando registro una solicitud no satisfecha como "vendedorQA44" en la tienda "QA44-004" para la variante "QA44-004" con cantidad 1 y clave "claveQA44"
    Entonces la respuesta es 201
    Cuando anulo la solicitud no satisfecha "claveQA44" como "vendedorQA44" con el motivo "Sin permiso"
    Entonces la respuesta es 403

  @CP-HU44-005 @positivo @funcional @flujo-principal
  Escenario: CP-HU44-005 — Filtrar solicitudes por estado
    Dado que existe una tienda de ventas llamada "QA44-005"
    Y que existe una variante de ventas llamada "QA44-005"
    Cuando registro una solicitud no satisfecha en la tienda "QA44-005" para la variante "QA44-005" con cantidad 1 y clave "anulada"
    Entonces la respuesta es 201
    Cuando registro una solicitud no satisfecha en la tienda "QA44-005" para la variante "QA44-005" con cantidad 2 y clave "activa"
    Entonces la respuesta es 201
    Cuando anulo la solicitud no satisfecha "anulada" con el motivo "Filtro"
    Entonces la respuesta es 200
    Cuando consulto las solicitudes no satisfechas de la tienda "QA44-005" con estado "ACTIVA"
    Entonces la respuesta es 200
    Y el campo "content" tiene 1 elementos
    Y el campo "content[0].cantidad" es 2
