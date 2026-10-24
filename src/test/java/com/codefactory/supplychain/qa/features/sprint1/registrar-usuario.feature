# language: es
@identidad @sprint1
Característica: Registrar usuario
  Como administrador de la cadena de suministro
  Quiero registrar usuarios con su rol
  Para habilitar el acceso controlado al sistema

  Antecedentes:
    Dado que estoy autenticado como administrador

  @CP-HU01-001 @positivo @funcional @flujo-principal @identidad @sprint1
  Escenario: CP-HU01-001 — Registrar usuario con información válida, habilitado y con su rol
    Cuando registro un usuario válido con nombre "QA Usuario QA01-001" y contraseña "ClaveSegura2026!"
    Entonces la respuesta es 201
    Y el campo "estado" es "ACTIVO"
    Cuando creo un rol exclusivo y se lo asigno al usuario registrado
    Y consulto los roles del usuario registrado
    Entonces los roles del usuario incluyen el rol asignado
    Cuando consulto el usuario registrado
    Entonces la respuesta es 200
    Y el campo "estado" es "ACTIVO"
    Y el campo "email" es el correo del usuario registrado

  @CP-HU01-002 @negativo @funcional @ruta-negativa @identidad @sprint1
  Escenario: CP-HU01-002 — Rechazar registro de usuario con información obligatoria incompleta
    Cuando registro un usuario sin nombre completo con un correo único y contraseña "ClaveSegura2026!"
    Entonces la respuesta es 400

  @CP-HU01-003 @negativo @funcional @ruta-negativa @identidad @sprint1
  Escenario: CP-HU01-003 — Rechazar registro de usuario duplicado
    Dado que existe un usuario registrado con un correo único
    Cuando registro otro usuario con el mismo correo
    Entonces la respuesta es 409

  @CP-HU01-004 @negativo @funcional @ruta-negativa @identidad @sprint1
  Escenario: CP-HU01-004 — Rechazar registro de usuario con información inválida
    Cuando registro un usuario con nombre "QA Invalido" y contraseña "corta"
    Entonces la respuesta es 400
