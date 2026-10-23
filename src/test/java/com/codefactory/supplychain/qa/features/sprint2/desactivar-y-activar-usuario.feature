# language: es
@identidad @sprint2
Característica: Desactivar y activar usuario
  Como administrador de la cadena de suministro
  Quiero controlar el estado de las cuentas de usuario
  Para suspender y restablecer su acceso de forma segura

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU31-001 @positivo @funcional @flujo-principal @identidad @sprint2
  Escenario: CP-HU31-001 — Reactivar un usuario le devuelve el acceso con un inicio de sesión nuevo
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador desactiva al vendedor
    Entonces la respuesta es 200
    Y el administrador activa al vendedor
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVO"
    Cuando el vendedor inicia sesión de nuevo con su contraseña "ClaveSegura-QA-2026!"
    Entonces la respuesta es 200
    Y consulto las ventas con la nueva sesión
    Entonces la respuesta es 200

  @CP-HU31-002 @negativo @funcional @ruta-negativa @identidad @sprint2
  Escenario: CP-HU31-002 — Rechazar que un usuario se desactive a sí mismo
    Y que existe un gestor de usuarios autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el gestor intenta desactivarse a sí mismo
    Entonces la operación responde 400 o 409
    Y el campo "mensaje" no es nulo
    Cuando el administrador consulta al gestor
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVO"

  @CP-HU31-003 @negativo @funcional @ruta-negativa @identidad @sprint2
  Escenario: CP-HU31-003 — Rechazar la desactivación del último ADMIN activo
    Y que existe un gestor de usuarios autenticado con la contraseña "ClaveSegura-QA-2026!"
    Y que conozco el identificador del único administrador activo
    Cuando el gestor intenta desactivar al administrador
    Entonces la operación responde 400 o 409
    Y el campo "mensaje" no es nulo
    Cuando el gestor consulta al administrador
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVO"
    Y el gestor asegura que el administrador quede activo

  @CP-HU31-004 @negativo @funcional @ruta-negativa @identidad @sprint2
  Escenario: CP-HU31-004 — Desactivar un usuario inexistente responde 404
    Cuando intento desactivar a un usuario inexistente
    Entonces la respuesta es 404
    Y el campo "mensaje" no es nulo
