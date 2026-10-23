# language: es
@catalogo @sprint1
Característica: Eliminar Producto
  Como administrador del catálogo
  Quiero eliminar productos
  Para mantener actualizado el catálogo sin perder la trazabilidad

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU18-001 @positivo @funcional @flujo-principal @omitido
  Escenario: CP-HU18-001 — Eliminar Producto sin dependencias bloqueantes
    Dado que existe un producto registrado con SKU "SKU-HU18-001"
    Cuando elimino el producto con SKU "SKU-HU18-001"
    Entonces la respuesta es 204

  @CP-HU18-002 @negativo @funcional @ruta-negativa @omitido
  Escenario: CP-HU18-002 — Bloquear eliminación de Producto con inventario u operaciones asociadas
    Dado que existe un producto registrado con SKU "SKU-HU18-002"
    Y que el producto tiene inventario y operaciones asociadas
    Cuando intento eliminar el producto con SKU "SKU-HU18-002"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío

  @CP-HU18-003 @integridad-de-datos @funcional @caso-borde @omitido
  Escenario: CP-HU18-003 — Conservar información histórica después de eliminar un Producto
    Dado que existe un producto registrado con SKU "SKU-HU18-003"
    Cuando elimino el producto con SKU "SKU-HU18-003"
    Y consulto el historial del producto con SKU "SKU-HU18-003"
    Entonces la respuesta es 200
    Y el campo "situacion" es "INACTIVO"
