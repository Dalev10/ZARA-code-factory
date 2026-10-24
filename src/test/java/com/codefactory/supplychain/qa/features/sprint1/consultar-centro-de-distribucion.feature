# language: es
@inventario @sprint1
Característica: Consultar Centro de Distribución
  Como usuario de la cadena de suministro
  Quiero consultar un Centro de Distribución y su inventario
  Para conocer la información logística asociada

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU12-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU12-001 — Consultar Centro de Distribución y ubicación
    Dado que existe un centro de distribución de inventario llamado "QA12-001" ubicado en "Sabaneta, Antioquia"
    Cuando consulto el centro de distribución "QA12-001"
    Entonces la respuesta es 200
    Y el campo "nombre" es el nombre del centro de distribución "QA12-001"
    Y el campo "nodo.tipo" es "CD"

  @CP-HU12-002 @integracion @funcional @flujo-principal
  Escenario: CP-HU12-002 — Consultar inventario asociado al CD
    Dado que existe un centro de distribución de inventario llamado "QA12-002" ubicado en "Itagüí, Antioquia"
    Y que existe una variante de inventario llamada "QA12-002"
    Y que la variante "QA12-002" tiene 40 unidades cargadas en el nodo del centro de distribución "QA12-002"
    Cuando consulto el inventario del nodo del centro de distribución "QA12-002"
    Entonces la respuesta es 200
    Y el inventario del nodo contiene la variante "QA12-002" con 40 unidades a la mano
