# language: es
@identidad @sprint2
Característica: Roles y scopes sembrados
  Como administrador de la cadena de suministro
  Quiero contar con roles y scopes iniciales coherentes
  Para que los permisos del sistema arranquen con el catálogo esperado

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU33-001 @positivo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU33-001 — El rol VENDEDOR viene sembrado con sus cuatro scopes
    Cuando consulto los scopes del rol "VENDEDOR"
    Entonces los scopes del rol son exactamente:
      | ventas:registrar        |
      | ventas:consultar        |
      | inventario:consultar    |
      | ventas:forzar-sin-stock |

  @CP-HU33-002 @positivo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU33-002 — El rol SUPERVISOR_TIENDA tiene lo del vendedor más anular y administrar inventario
    Cuando consulto los scopes del rol "SUPERVISOR_TIENDA"
    Entonces los scopes del rol son exactamente:
      | ventas:registrar        |
      | ventas:consultar        |
      | inventario:consultar    |
      | ventas:forzar-sin-stock |
      | ventas:anular           |
      | inventario:administrar  |

  @CP-HU33-003 @positivo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU33-003 — El rol OPERADOR_CD solo administra y consulta inventario de CD
    Cuando consulto los scopes del rol "OPERADOR_CD"
    Entonces los scopes del rol son exactamente:
      | inventario-cd:administrar |
      | inventario:consultar      |

  @CP-HU33-004 @positivo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU33-004 — ADMIN incluye tiendas:operar-todas y reposicion:administrar
    Cuando consulto los scopes del rol "ADMIN"
    Entonces los scopes del rol incluyen:
      | tiendas:operar-todas     |
      | reposicion:administrar   |
      | inventario:administrar   |
      | inventario-cd:administrar |
      | ventas:anular            |

  @CP-HU33-005 @negativo @funcional @ruta-negativa @identidad @sprint2
  Escenario: CP-HU33-005 — Rechazar crear un rol con un nombre reservado (409)
    Cuando creo un rol con nombre "VENDEDOR" y descripción "Duplicado"
    Entonces la respuesta es 409

  @CP-HU33-006 @negativo @funcional @caso-borde @identidad @sprint2
  Escenario: CP-HU33-006 — Crear un scope sin el campo sensible lo deja en false
    Cuando creo un scope con un código único y descripción "Scope QA sin sensible"
    Entonces la respuesta es 201
    Y el scope queda con sensible en falso

  @CP-HU33-007 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU33-007 — Un VENDEDOR no puede anular ventas (403, falta ventas:anular)
    Dado que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el vendedor intenta anular una venta inexistente con motivo "Intento sin permiso"
    Entonces la respuesta es 403
    Y el campo "mensaje" es "No tiene permisos para realizar esta acción"
