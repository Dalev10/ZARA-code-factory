# language: es
@catalogo @sprint1
Característica: Registrar Producto
  Como administrador del catálogo
  Quiero registrar productos con su información y sus ubicaciones
  Para incorporarlos a la operación de la cadena de suministro

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU15-001 @positivo @funcional @flujo-principal @omitido
  Escenario: CP-HU15-001 — Registrar Producto con información válida y situación definida
    Dado que no existe un producto con SKU "SKU-HU15-001"
    Cuando registro un producto con SKU "SKU-HU15-001", nombre "Camisa QA" y situación "ACTIVO"
    Entonces la respuesta es 201
    Y el campo "sku" es "SKU-HU15-001"
    Y el campo "situacion" es "ACTIVO"

  @CP-HU15-002 @negativo @funcional @ruta-negativa @omitido
  Escenario: CP-HU15-002 — Rechazar registro de Producto con información obligatoria incompleta
    Cuando registro un producto sin nombre ni SKU
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU15-003 @negativo @funcional @ruta-negativa @omitido
  Escenario: CP-HU15-003 — Rechazar registro de Producto duplicado
    Dado que existe un producto registrado con SKU "SKU-HU15-003"
    Cuando registro otro producto con el mismo SKU "SKU-HU15-003"
    Entonces la respuesta es 409
    Y el mensaje de error no está vacío

  @CP-HU15-004 @integracion @funcional @flujo-principal @omitido
  Escenario: CP-HU15-004 — Relacionar un Producto con las ubicaciones donde se gestiona
    Dado que existe una tienda llamada "Tienda HU15-004"
    Cuando registro un producto con SKU "SKU-HU15-004" y lo asocio a la tienda "Tienda HU15-004"
    Entonces la respuesta es 201
    Y el campo "ubicaciones" tiene 1 elementos
