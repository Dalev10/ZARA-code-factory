# language: es
@reposicion @sprint2
Característica: Prioridad de CDs por tienda
  Como usuario de la cadena de suministro
  Quiero definir el orden de los centros de distribución de una tienda
  Para que la reposición consulte primero los CDs con mayor prioridad

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU51-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU51-001 — Definir la prioridad de CDs respeta el orden de la lista
    Dado que existe una tienda llamada "t-51-001"
    Y que existe un centro de distribución llamado "cd-51-001A"
    Y que existe un centro de distribución llamado "cd-51-001B"
    Y que existe un centro de distribución llamado "cd-51-001C"
    Cuando defino la prioridad de CDs de la tienda "t-51-001" como "cd-51-001B", "cd-51-001C", "cd-51-001A"
    Entonces la respuesta es 200
    Cuando consulto la prioridad de CDs de la tienda "t-51-001"
    Entonces la respuesta es 200
    Y la prioridad de CDs de la tienda "t-51-001" es "cd-51-001B", "cd-51-001C", "cd-51-001A"

  @CP-HU51-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU51-002 — Un PUT nuevo reemplaza la lista completa
    Dado que existe una tienda llamada "t-51-002"
    Y que existe un centro de distribución llamado "cd-51-002A"
    Y que existe un centro de distribución llamado "cd-51-002B"
    Cuando defino la prioridad de CDs de la tienda "t-51-002" como "cd-51-002A", "cd-51-002B"
    Entonces la respuesta es 200
    Cuando defino la prioridad de CDs de la tienda "t-51-002" como "cd-51-002B"
    Entonces la respuesta es 200
    Cuando consulto la prioridad de CDs de la tienda "t-51-002"
    Entonces la respuesta es 200
    Y la tienda "t-51-002" tiene 1 CD prioritario: "cd-51-002B"

  @CP-HU51-003 @negativo @funcional @caso-borde
  Escenario: CP-HU51-003 — Una lista vacía deja la tienda sin prioridades y libera el CD
    Dado que existe una tienda llamada "t-51-003"
    Y que existe un centro de distribución llamado "cd-51-003"
    Cuando defino la prioridad de CDs de la tienda "t-51-003" como "cd-51-003"
    Entonces la respuesta es 200
    Cuando defino la prioridad de CDs de la tienda "t-51-003" con una lista vacía
    Entonces la respuesta es 200
    Cuando consulto la prioridad de CDs de la tienda "t-51-003"
    Entonces la respuesta es 200
    Y la tienda "t-51-003" no tiene CDs prioritarios
    Cuando elimino el centro de distribución "cd-51-003"
    Entonces la respuesta es 204

  @CP-HU51-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU51-004 — Un CD con prioridades no se elimina (409 con mensaje)
    Dado que existe una tienda llamada "t-51-004"
    Y que existe un centro de distribución llamado "cd-51-004"
    Cuando defino la prioridad de CDs de la tienda "t-51-004" como "cd-51-004"
    Entonces la respuesta es 200
    Cuando elimino el centro de distribución "cd-51-004"
    Entonces la respuesta es 409
    Y el mensaje de error menciona "prioridades"

  @CP-HU51-005 @negativo @funcional @ruta-negativa
  Escenario: CP-HU51-005 — Rechazar un CD repetido o inexistente en la lista
    Dado que existe una tienda llamada "t-51-005"
    Y que existe un centro de distribución llamado "cd-51-005"
    Cuando defino la prioridad de CDs de la tienda "t-51-005" como "cd-51-005", "cd-51-005"
    Entonces el CD repetido es rechazado con 400 o 409
    Cuando defino la prioridad de CDs de la tienda "t-51-005" con un CD inexistente
    Entonces la respuesta es 404
    Cuando consulto la prioridad de CDs de la tienda "t-51-005"
    Entonces la respuesta es 200
    Y la tienda "t-51-005" no tiene CDs prioritarios

  @CP-HU51-006 @negativo @funcional @ruta-negativa
  Escenario: CP-HU51-006 — Rechazar el cuerpo sin cdIds (400) y una tienda inexistente (404)
    Dado que existe una tienda llamada "t-51-006"
    Y que existe un centro de distribución llamado "cd-51-006"
    Cuando defino la prioridad de CDs de la tienda "t-51-006" sin la lista de CDs
    Entonces la respuesta es 400
    Cuando defino la prioridad de CDs de una tienda inexistente como "cd-51-006"
    Entonces la respuesta es 404

  @CP-HU51-007 @negativo @no-funcional @seguridad
  Escenario: CP-HU51-007 — Definir prioridades exige inventario:administrar (403 para un vendedor)
    Dado que existe una tienda llamada "t-51-007"
    Y que existe un centro de distribución llamado "cd-51-007"
    Y que existe un vendedor asignado a la tienda "t-51-007"
    Cuando el vendedor asignado define la prioridad de CDs de la tienda "t-51-007" como "cd-51-007"
    Entonces la respuesta es 403
