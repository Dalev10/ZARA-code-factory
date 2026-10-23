# language: es
@inventario @sprint1
Característica: Modificar Centro de Distribución
  Como usuario de la cadena de suministro
  Quiero modificar los datos permitidos de un Centro de Distribución
  Para mantener actualizada la red logística

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU13-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU13-001 — Modificar campos permitidos de Centro de Distribución
    Dado que existe un centro de distribución de inventario llamado "HU13-001" ubicado en "Caldas, Antioquia"
    Cuando modifico el centro de distribución "HU13-001" con un nombre único y ubicación "Envigado, Antioquia"
    Entonces la respuesta es 200
    Y el campo "nombre" es el nombre nuevo del centro de distribución "HU13-001"
    Y el campo "ubicacion" es "Envigado, Antioquia"

  @CP-HU13-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU13-002 — Rechazar modificación inválida de Centro de Distribución
    Dado que existe un centro de distribución de inventario llamado "HU13-002" ubicado en "Caldas, Antioquia"
    Cuando modifico el centro de distribución "HU13-002" con nombre vacío y ubicación "No debería guardarse"
    Entonces la respuesta es 400
