# language: es
@identidad @sprint2
Característica: Asignar tiendas a usuario
  Como administrador de la cadena de suministro
  Quiero asignar y retirar tiendas a los usuarios
  Para delimitar dónde puede operar cada rol

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU32-001 @positivo @funcional @flujo-principal @identidad @sprint2
  Escenario: CP-HU32-001 — Asignar una tienda a un usuario y consultarla en sus tiendas
    Dado que existe una tienda llamada "QA32-001"
    Y que existe un vendedor con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador asigna la tienda al vendedor
    Y consulto las tiendas asignadas al vendedor
    Entonces la respuesta es 200
    Y las tiendas asignadas incluyen la tienda del escenario
    Y la tienda asignada está activa

  @CP-HU32-002 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU32-002 — Un vendedor sin la tienda asignada no registra ventas en ella (403)
    Dado que existe una tienda con una variante disponible
    Y que la tienda tiene 5 unidades disponibles de la variante
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el vendedor intenta registrar una venta de una unidad en la tienda
    Entonces la respuesta es 403
    Y la tienda conserva 5 unidades disponibles

  @CP-HU32-003 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU32-003 — La asignación aplica de inmediato sin pedir un token nuevo
    Dado que existe una tienda con una variante disponible
    Y que la tienda tiene 5 unidades disponibles de la variante
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador asigna la tienda al vendedor
    Y el vendedor registra una venta de una unidad en la tienda
    Entonces la respuesta es 201

  @CP-HU32-004 @positivo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU32-004 — Quitar la tienda corta la operación del usuario en ella
    Dado que existe una tienda con una variante disponible
    Y que la tienda tiene 5 unidades disponibles de la variante
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Y el administrador asigna la tienda al vendedor
    Cuando el administrador quita la tienda al vendedor
    Entonces la respuesta es 204
    Cuando el vendedor intenta registrar una venta de una unidad en la tienda
    Entonces la respuesta es 403

  @CP-HU32-005 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU32-005 — ADMIN opera en cualquier tienda sin tenerla asignada (tiendas:operar-todas)
    Dado que existe una tienda con una variante disponible
    Y que la tienda tiene 5 unidades disponibles de la variante
    Cuando el administrador registra una venta de una unidad en la tienda
    Entonces la respuesta es 201

  @CP-HU32-006 @negativo @funcional @ruta-negativa @identidad @sprint2
  Escenario: CP-HU32-006 — Rechazar asignar una tienda inexistente
    Dado que existe un vendedor con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador asigna una tienda inexistente al vendedor
    Entonces la respuesta es 404

  @CP-HU32-007 @positivo @funcional @caso-borde @identidad @sprint2
  Escenario: CP-HU32-007 — Se asigna una tienda inactiva, pero no se opera en ella hasta activarla
    Dado que existe una tienda con una variante disponible
    Y que la tienda tiene 5 unidades disponibles de la variante
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador desactiva la tienda del escenario
    Y el administrador asigna la tienda al vendedor
    Y consulto las tiendas asignadas al vendedor
    Entonces las tiendas asignadas incluyen la tienda del escenario
    Y la tienda asignada está inactiva
    Cuando el vendedor intenta registrar una venta de una unidad en la tienda
    Entonces la respuesta es un error de cliente
    Y la tienda conserva 5 unidades disponibles
    Cuando el administrador activa la tienda del escenario
    Y el vendedor registra una venta de una unidad en la tienda
    Entonces la respuesta es 201
    Y la tienda conserva 4 unidades disponibles
