# language: es
@transversal @sprint2
Característica: Contrato de errores
  Como consumidor de la API
  Quiero que todos los errores sigan un contrato uniforme
  Para interpretar los fallos sin ambigüedad

  @CP-HU29-001 @negativo @no-funcional @fiabilidad @transversal @sprint2
  Escenario: CP-HU29-001 — Un JSON mal formado responde 400 con mensaje, no con cuerpo vacío
    Dado que estoy autenticado como administrador
    Cuando envío una tienda con un JSON mal formado
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU29-002 @negativo @no-funcional @seguridad @transversal @sprint2
  Escenario: CP-HU29-002 — Sin token responde 401 con WWW-Authenticate: Bearer
    Cuando consulto las ventas sin iniciar sesión
    Entonces la respuesta es 401
    Y la cabecera "WWW-Authenticate" contiene "Bearer"

  @CP-HU29-003 @negativo @no-funcional @fiabilidad @transversal @sprint2
  Escenario: CP-HU29-003 — Un recurso inexistente con usuario autenticado responde 404, no 401
    Dado que estoy autenticado como administrador
    Cuando consulto una tienda con un identificador inexistente
    Entonces la respuesta es 404
    Y el campo "mensaje" no es nulo

  @CP-HU29-004 @negativo @no-funcional @fiabilidad @transversal @sprint2
  Escenario: CP-HU29-004 — Un método no permitido con usuario autenticado responde 405, no 401
    Dado que estoy autenticado como administrador
    Cuando intento registrar una bodega de tienda con datos vacíos
    Entonces la respuesta es 405
    Y el campo "mensaje" no es nulo

  @CP-HU29-005 @negativo @no-funcional @seguridad @transversal @sprint2
  Escenario: CP-HU29-005 — Falta de scope responde 403 con mensaje en español neutro
    Dado que estoy autenticado como administrador
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el vendedor consulta los umbrales de reposición
    Entonces la respuesta es 403
    Y el campo "mensaje" es "No tiene permisos para realizar esta acción"

  @CP-HU29-006 @positivo @no-funcional @interoperabilidad @transversal @sprint2
  Escenario: CP-HU29-006 — Las fechas se responden en UTC (ISO-8601 con Z)
    Dado que estoy autenticado como administrador
    Y que existe una tienda llamada "QA29-006"
    Cuando consulto la última tienda creada
    Entonces la respuesta es 200
    Y el campo "creadoEn" termina en Z

  @CP-HU29-007 @negativo @no-funcional @fiabilidad @transversal @sprint2
  Escenario: CP-HU29-007 — Un id con formato inválido en la ruta responde 400 con mensaje
    Dado que estoy autenticado como administrador
    Cuando consulto una tienda con un identificador con formato inválido
    Entonces la respuesta es 400
    Y el campo "mensaje" no es nulo
