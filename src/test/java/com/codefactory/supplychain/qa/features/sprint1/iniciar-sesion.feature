# language: es
@identidad @sprint1
Característica: Iniciar sesión
  Como usuario de la cadena de suministro
  Quiero autenticarme con mis credenciales
  Para acceder únicamente a las funciones permitidas a mi rol

  @CP-HU02-001 @positivo @funcional @flujo-principal @identidad @sprint1
  Escenario: CP-HU02-001 — Iniciar sesión con credenciales válidas y acceder a funciones permitidas
    Dado que estoy autenticado como administrador
    Y que existe un usuario con el scope "cd:administrar" y la contraseña "ClaveSegura2026!"
    Cuando inicio sesión con el correo del usuario de prueba y la contraseña "ClaveSegura2026!"
    Entonces la respuesta es 200
    Y consulto los centros de distribución con la sesión iniciada
    Entonces la respuesta es 200

  @CP-HU02-002 @negativo @no-funcional @seguridad @identidad @sprint1
  Escenario: CP-HU02-002 — Rechazar inicio de sesión con contraseña incorrecta
    Dado que estoy autenticado como administrador
    Y que existe un usuario con el scope "cd:administrar" y la contraseña "ClaveSegura2026!"
    Cuando inicio sesión con el correo del usuario de prueba y la contraseña "ClaveIncorrecta999!"
    Entonces la respuesta es 401

  @CP-HU02-003 @negativo @no-funcional @seguridad @identidad @sprint1
  Escenario: CP-HU02-003 — Rechazar inicio de sesión de un usuario no registrado
    Cuando inicio sesión con un correo no registrado y la contraseña "ClaveCualquiera2026!"
    Entonces la respuesta es 401

  @CP-HU02-004 @negativo @no-funcional @seguridad @identidad @sprint1
  Escenario: CP-HU02-004 — Impedir el acceso a una función no permitida para el rol del usuario
    Dado que estoy autenticado como administrador
    Y que existe un usuario con el scope "cd:administrar" y la contraseña "ClaveSegura2026!"
    Cuando inicio sesión con el correo del usuario de prueba y la contraseña "ClaveSegura2026!"
    Entonces la respuesta es 200
    Y con ese usuario intento registrar una tienda
    Entonces la respuesta es 403
