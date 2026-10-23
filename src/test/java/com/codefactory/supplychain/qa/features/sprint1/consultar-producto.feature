# language: es
@catalogo @sprint1
Característica: Consultar Producto
  Como administrador del catálogo
  Quiero consultar y buscar productos
  Para conocer su información, su situación y su disponibilidad

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU16-001 @positivo @funcional @flujo-principal @omitido
  Escenario: CP-HU16-001 — Consultar Producto existente, información de identificación y situación
    Dado que existe un producto registrado con SKU "SKU-HU16-001"
    Cuando consulto el producto con SKU "SKU-HU16-001"
    Entonces la respuesta es 200
    Y el campo "sku" es "SKU-HU16-001"
    Y el campo "situacion" no es nulo

  @CP-HU16-002 @positivo @funcional @flujo-principal @omitido
  Escenario: CP-HU16-002 — Buscar Productos con los criterios definidos para la operación
    Dado que existe un producto registrado con SKU "SKU-HU16-002"
    Cuando busco productos por el criterio "camisa"
    Entonces la respuesta es 200
    Y el campo "contenido" no es nulo
    Y el campo "contenido" tiene 1 elementos

  @CP-HU16-003 @seguridad @no-funcional @seguridad @omitido
  Escenario: CP-HU16-003 — Restringir la consulta de Productos según las responsabilidades del usuario
    Dado que existe una tienda llamada "Tienda HU16-003"
    Y que existe un producto registrado con SKU "SKU-HU16-003"
    Cuando consulto los productos de la tienda "Tienda HU16-003" según mis responsabilidades
    Entonces la respuesta es 200
    Y el campo "contenido" tiene 1 elementos

  @CP-HU16-004 @integracion @funcional @flujo-principal @omitido
  Escenario: CP-HU16-004 — Consultar información logística asociada a un Producto
    Dado que existe un producto registrado con SKU "SKU-HU16-004"
    Cuando consulto la información logística del producto con SKU "SKU-HU16-004"
    Entonces la respuesta es 200
    Y el campo "stock" no es nulo
    Y el campo "ubicaciones" tiene 1 elementos
