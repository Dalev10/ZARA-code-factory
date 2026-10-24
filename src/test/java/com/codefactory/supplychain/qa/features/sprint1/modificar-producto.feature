# language: es
@catalogo @sprint1
Característica: Modificar Producto
  Como administrador del catálogo
  Quiero modificar los campos permitidos de un producto
  Para mantener su información vigente en el inventario y en las operaciones

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU17-001 @positivo @funcional @flujo-principal @omitido
  Escenario: CP-HU17-001 — Modificar campos permitidos de un Producto
    Dado que existe un producto registrado con SKU "SKU-QA17-001"
    Cuando modifico el producto con SKU "SKU-QA17-001" con el nombre "Camisa actualizada"
    Entonces la respuesta es 200
    Y el campo "nombre" es "Camisa actualizada"

  @CP-HU17-002 @negativo @funcional @ruta-negativa @omitido
  Escenario: CP-HU17-002 — Rechazar modificación inválida de un Producto
    Dado que existe un producto registrado con SKU "SKU-QA17-002"
    Cuando modifico el producto con SKU "SKU-QA17-002" con un precio inválido
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU17-003 @integracion @funcional @caso-borde @omitido
  Escenario: CP-HU17-003 — Reflejar los cambios del Producto en inventario y operaciones asociadas
    Dado que existe una tienda llamada "Tienda QA17-003"
    Y que existe un producto registrado con SKU "SKU-QA17-003"
    Cuando modifico el producto con SKU "SKU-QA17-003" y consulto el inventario de la tienda "Tienda QA17-003"
    Entonces la respuesta es 200
    Y el campo "nombre" es "Camisa actualizada"
    Y el campo "inventario" tiene 1 elementos
