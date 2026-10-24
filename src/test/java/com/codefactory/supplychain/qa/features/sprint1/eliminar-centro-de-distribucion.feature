# language: es
@inventario @sprint1
Característica: Eliminar Centro de Distribución
  Como usuario de la cadena de suministro
  Quiero eliminar un Centro de Distribución sin dependencias
  Para mantener actualizada la red logística

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU14-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU14-001 — Eliminar CD sin dependencias bloqueantes
    Dado que existe un centro de distribución de inventario llamado "QA14-001" ubicado en "La Estrella, Antioquia"
    Cuando elimino el centro de distribución "QA14-001"
    Entonces la respuesta es 204
    Cuando consulto el centro de distribución "QA14-001"
    Entonces la respuesta es 404

  @CP-HU14-002 @negativo @funcional @ruta-negativa
  Escenario: CP-HU14-002 — Bloquear eliminación de CD con operaciones o información asociada
    Dado que existe un centro de distribución de inventario llamado "QA14-002" ubicado en "Guarne, Antioquia"
    Y que existe una tienda con almacén y bodega llamada "QA14-002" ubicada en "Rionegro, Antioquia"
    Y que la tienda "QA14-002" tiene como prioridad el centro de distribución "QA14-002"
    Cuando elimino el centro de distribución "QA14-002"
    Entonces la respuesta es 409
    Y el mensaje de error menciona "prioridades asignadas"

  @CP-HU14-003 @integridad-de-datos @funcional @ruta-negativa
  Escenario: CP-HU14-003 — Conservar historial después de eliminar CD
    Dado que existe un centro de distribución de inventario llamado "QA14-003" ubicado en "Guarne, Antioquia"
    Y que existe una tienda con almacén y bodega llamada "QA14-003" ubicada en "Rionegro, Antioquia"
    Y que la tienda "QA14-003" tiene como prioridad el centro de distribución "QA14-003"
    Cuando elimino el centro de distribución "QA14-003"
    Entonces la respuesta es 409
    Cuando consulto el centro de distribución "QA14-003"
    Entonces la respuesta es 200
    Y el campo "id" es el identificador del centro de distribución "QA14-003"
