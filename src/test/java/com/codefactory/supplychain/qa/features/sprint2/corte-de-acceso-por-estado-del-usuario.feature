# language: es
@identidad @sprint2
Característica: Corte de acceso por estado del usuario
  Como administrador de la cadena de suministro
  Quiero que un usuario desactivado pierda el acceso de inmediato
  Para evitar operaciones de cuentas inhabilitadas

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU30-001 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU30-001 — Un usuario desactivado pierde el acceso con su token vigente (401 Bearer)
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Y el vendedor consulta las ventas
    Entonces la respuesta es 200
    Cuando el administrador desactiva al vendedor
    Entonces la respuesta es 200
    Y el campo "estado" es "INACTIVO"
    Cuando el vendedor consulta las ventas con su token vigente
    Entonces la respuesta es 401
    Y la cabecera "WWW-Authenticate" contiene "Bearer"

  @CP-HU30-002 @negativo @no-funcional @seguridad @identidad @sprint2
  Escenario: CP-HU30-002 — Un usuario desactivado no puede iniciar sesión
    Y que existe un vendedor autenticado con la contraseña "ClaveSegura-QA-2026!"
    Cuando el administrador desactiva al vendedor
    Entonces la respuesta es 200
    Cuando el vendedor intenta iniciar sesión con su contraseña "ClaveSegura-QA-2026!"
    Entonces la respuesta es 401
    Y el campo "mensaje" no es nulo
