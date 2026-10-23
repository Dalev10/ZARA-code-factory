# language: es
@inventario @sprint2
Característica: Ajuste de inventario
  Como usuario de la cadena de suministro
  Quiero ajustar las existencias de una variante en un nodo
  Para corregir el stock por conteo físico, merma u otra causa

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU38-001 @positivo @funcional @flujo-principal
  Escenario: CP-HU38-001 — Ajuste positivo por conteo físico suma al stock y devuelve el saldo
    Dado que existe una tienda con almacén y bodega llamada "HU38-001" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-001"
    Y que la variante "HU38-001" tiene 10 unidades cargadas en el almacén de la tienda "HU38-001"
    Cuando ajusto la variante "HU38-001" con 3 unidades por motivo "CONTEO_FISICO" en el almacén de la tienda "HU38-001"
    Entonces la respuesta es 201
    Y el campo "delta" es 3
    Y el campo "motivo" es "CONTEO_FISICO"
    Y el campo "aLaMano" es 13
    Y el stock de la variante "HU38-001" en el almacén de la tienda "HU38-001" es 13

  @CP-HU38-002 @positivo @funcional @flujo-principal
  Escenario: CP-HU38-002 — Ajuste negativo por merma resta del stock
    Dado que existe una tienda con almacén y bodega llamada "HU38-002" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-002"
    Y que la variante "HU38-002" tiene 10 unidades cargadas en el almacén de la tienda "HU38-002"
    Cuando ajusto la variante "HU38-002" con -4 unidades por motivo "MERMA" y comentario "Prendas dañadas" en el almacén de la tienda "HU38-002"
    Entonces la respuesta es 201
    Y el campo "aLaMano" es 6

  @CP-HU38-003 @negativo @funcional @ruta-negativa
  Escenario: CP-HU38-003 — CORRECCION sin comentario se rechaza (400)
    Dado que existe una tienda con almacén y bodega llamada "HU38-003" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-003"
    Y que la variante "HU38-003" tiene 10 unidades cargadas en el almacén de la tienda "HU38-003"
    Cuando ajusto la variante "HU38-003" con -1 unidades por motivo "CORRECCION" en el almacén de la tienda "HU38-003"
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Cuando ajusto la variante "HU38-003" con -1 unidades por motivo "CORRECCION" con un comentario en blanco en el almacén de la tienda "HU38-003"
    Entonces la respuesta es 400
    Y el stock de la variante "HU38-003" en el almacén de la tienda "HU38-003" es 10
    Cuando ajusto la variante "HU38-003" con -1 unidades por motivo "CORRECCION" y comentario "Error de digitación" en el almacén de la tienda "HU38-003"
    Entonces la respuesta es 201

  @CP-HU38-004 @negativo @funcional @ruta-negativa
  Escenario: CP-HU38-004 — Un ajuste que dejaría el stock negativo se rechaza (400) sin cambiar el saldo
    Dado que existe una tienda con almacén y bodega llamada "HU38-004" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-004"
    Y que la variante "HU38-004" tiene 2 unidades cargadas en el almacén de la tienda "HU38-004"
    Cuando ajusto la variante "HU38-004" con -3 unidades por motivo "MERMA" en el almacén de la tienda "HU38-004"
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío
    Y el stock de la variante "HU38-004" en el almacén de la tienda "HU38-004" es 2

  @CP-HU38-005 @negativo @funcional @caso-borde
  Escenario: CP-HU38-005 — Un ajuste positivo sobre una variante sin fila crea la fila
    Dado que existe una tienda con almacén y bodega llamada "HU38-005" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-005"
    Cuando ajusto la variante "HU38-005" con 7 unidades por motivo "INGRESO_MANUAL" en el almacén de la tienda "HU38-005"
    Entonces la respuesta es 201
    Y el campo "aLaMano" es 7
    Y el stock de la variante "HU38-005" en el almacén de la tienda "HU38-005" es 7

  @CP-HU38-006 @negativo @funcional @ruta-negativa
  Escenario: CP-HU38-006 — Un ajuste negativo sobre una variante sin fila se rechaza (400) y no crea fila
    Dado que existe una tienda con almacén y bodega llamada "HU38-006" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-006"
    Cuando ajusto la variante "HU38-006" con -1 unidades por motivo "MERMA" en el almacén de la tienda "HU38-006"
    Entonces la respuesta es 400
    Y la variante "HU38-006" no tiene fila de inventario en el almacén de la tienda "HU38-006"

  @CP-HU38-007 @negativo @funcional @ruta-negativa
  Escenario: CP-HU38-007 — Rechazar un motivo fuera de la lista (400)
    Dado que existe una tienda con almacén y bodega llamada "HU38-007" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-007"
    Y que la variante "HU38-007" tiene 5 unidades cargadas en el almacén de la tienda "HU38-007"
    Cuando ajusto la variante "HU38-007" con 1 unidades por motivo "REGALO" en el almacén de la tienda "HU38-007"
    Entonces la respuesta es 400
    Y el mensaje de error no está vacío

  @CP-HU38-008 @negativo @no-funcional @seguridad
  Escenario: CP-HU38-008 — Un VENDEDOR no puede ajustar inventario (403)
    Dado que existe una tienda con almacén y bodega llamada "HU38-008" ubicada en "Medellín, Antioquia"
    Y que existe una variante de inventario llamada "HU38-008"
    Y que la variante "HU38-008" tiene 5 unidades cargadas en el almacén de la tienda "HU38-008"
    Y que existe un vendedor asignado a la tienda "HU38-008"
    Cuando el vendedor ajusta la variante "HU38-008" con 1 unidades por motivo "CONTEO_FISICO" en el almacén de la tienda "HU38-008"
    Entonces la respuesta es 403
    Y el stock de la variante "HU38-008" en el almacén de la tienda "HU38-008" es 5

  @CP-HU38-009 @negativo @funcional @ruta-negativa
  Escenario: CP-HU38-009 — Ajustar un SKU inexistente se rechaza
    Dado que existe una tienda con almacén y bodega llamada "HU38-009" ubicada en "Medellín, Antioquia"
    Cuando ajusto un SKU inexistente con 1 unidades por motivo "INGRESO_MANUAL" en el almacén de la tienda "HU38-009"
    Entonces la respuesta es 400 o 404
    Y el mensaje de error no está vacío
