# language: es
@inventario @sprint1
Característica: Registrar Centro de Distribución
  Como usuario de la cadena de suministro
  Quiero registrar centros de distribución
  Para organizar la red logística

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU11-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU11-001 — Registrar Centro de Distribución válido
    Cuando registro un centro de distribución llamado "QA11-001" ubicado en "Itagüí, Antioquia"
    Entonces la respuesta es 201
    Y el campo "nombre" es el nombre del último centro de distribución registrado
    Y el campo "ubicacion" es "Itagüí, Antioquia"

  @CP-HU11-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU11-002 — Rechazar registro de CD con campo obligatorio faltante
    Cuando registro un centro de distribución con nombre vacío ubicado en "Sin nombre"
    Entonces la respuesta es 400

  @CP-HU11-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU11-003 — Rechazar registro de CD duplicado
    Cuando registro un centro de distribución llamado "QA11-003" ubicado en "Bogotá"
    Entonces la respuesta es 201
    Cuando registro de nuevo un centro de distribución con el nombre del último centro registrado
    Entonces la respuesta es 409
