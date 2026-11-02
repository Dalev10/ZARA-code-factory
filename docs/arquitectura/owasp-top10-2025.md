# Análisis de vulnerabilidades: OWASP Top 10:2025

Entregable «Identificación de vulnerabilidades con OWASP Top Ten». Revisa el backend
`supply-chain-mvp` (Java 21, Spring Boot 4.1.1) y su despliegue en Render y Supabase contra
las diez categorías de la edición 2025.

| | |
|---|---|
| Alcance | Código de `src/main`, migraciones Flyway, `Dockerfile`, pipeline (`.github/workflows/ci.yml`) y configuración de GitHub, Render y SonarCloud. Fuera de alcance: el frontend en Vercel y la infraestructura interna de Render y Supabase |
| Método | Revisión manual del código, resultados de SonarCloud y CodeQL, y verificación de cada control con las pruebas existentes |
| Fecha | Noviembre de 2026, sobre `develop` después del PR #43 |

## Resumen

| Categoría | Controles | Hallazgos | Estado |
|---|---|---|---|
| A01 Broken Access Control | Scope por operación, alcance por tienda, 404 fuera de alcance | H-02, H-04 | 🟢 H-02 corregido · 🟡 H-04 pendiente |
| A02 Security Misconfiguration | Actuator mínimo, CORS por lista, sesión sin estado, contenedor sin root | H-07 | ⚪ Riesgo aceptado |
| A03 Software Supply Chain Failures | Dependabot, CodeQL, SonarCloud, Actions por SHA | H-06 | 🟢 Corregido |
| A04 Cryptographic Failures | BCrypt, AES-GCM, JWT con clave validada, tokens con hash | H-11 | ⚪ Falso positivo |
| A05 Injection | JPA parametrizado, Bean Validation, entradas acotadas | H-12 | ⚪ Informativo |
| A06 Insecure Design | Bloqueo progresivo, idempotencia, guardas de negocio | H-08 | 🟡 Pendiente |
| A07 Authentication Failures | Política de contraseña, MFA, rotación de refresh, respuesta uniforme | H-01, H-03, H-10 | 🟢 H-01 corregido · 🟡 H-03 pendiente · ⚪ H-10 aceptado |
| A08 Software or Data Integrity Failures | Checksums de Flyway, despliegue solo desde el pipeline | — | 🟢 Sin hallazgos |
| A09 Security Logging and Alerting Failures | Logger `seguridad` para autenticación | H-05 | 🟡 Parcial |
| A10 Mishandling of Exceptional Conditions | Manejador global con cuerpo uniforme | H-09, H-13 | 🟡 Pendiente |

**Severidad y plazo de corrección**

| Severidad | Criterio | Plazo |
|---|---|---|
| Crítica | Explotable sin autenticación; compromete toda la aplicación | 24 horas |
| Alta | Explotable por un usuario autenticado; afecta datos de otros | 7 días |
| Media | Requiere condiciones adicionales o tiene impacto acotado | 30 días |
| Baja | Impacto menor o defensa en profundidad | 90 días |
| Informativa | Sin impacto explotable; se documenta la decisión | — |

---

## A01 · Broken Access Control

**Controles existentes**

- Toda operación, salvo las cuatro de `/api/v1/auth` y las dos de `/api/v1/mfa`, exige un
  scope con `@PreAuthorize`. Las rutas no declaradas exigen autenticación
  (`SecurityConfig.java:72`, `anyRequest().authenticated()`).
- Alcance por tienda: `UsuarioActual.puedeOperarEn` (`UsuarioActual.java:35`) limita ventas,
  inventario y reposición a las tiendas asignadas al usuario, salvo con
  `tiendas:operar-todas`. Un recurso de otra tienda responde 404, no 403, para no revelar
  que existe.
- Los permisos se resuelven en cada petición (`JwtAuthenticationFilter.java:67`): quitar un
  rol o desactivar un usuario rige desde su siguiente petición, sin esperar a que venza el
  token.
- El usuario de MFA y del registro de ventas sale del token, nunca del cuerpo.
- Pruebas: `ScopeAuthorizationGuardTest` y las pruebas de controlador de cada módulo
  verifican 401, 403 y 404.

**H-02 · La evaluación de reposición no respetaba las tiendas del usuario** · Alta · 🟢 Corregido

`POST /api/v1/reposicion-interna/tiendas/{tiendaId}/evaluar` no verificaba que el usuario
operara en la tienda, y `POST /api/v1/reposicion-interna/evaluar` recorría todas las tiendas
activas. Un supervisor con `reposicion:administrar` podía disparar traspasos de inventario en
tiendas ajenas.

*Corrección* (commit `8e5d1b9`): la evaluación de una tienda ajena responde 403 y la masiva
solo recorre las tiendas del usuario. Pruebas: `EvaluacionMasivaServiceTest` y
`EvaluacionMasivaControllerTest.evaluarUnaTiendaAjenaEsProhibido`.

**H-04 · CSRF desactivado con la cookie de refresh en `SameSite=None`** · Baja · 🟡 90 días

CSRF está desactivado (`SecurityConfig.java:56`). Es correcto para las operaciones con JWT
en la cabecera `Authorization`, que un sitio ajeno no puede adjuntar. Sin embargo,
`/auth/refresh` y `/auth/logout` usan la cookie `refresh_token`, que va con
`SameSite=None` porque el frontend está en otro dominio (Vercel). Un sitio ajeno puede
forzar desde el navegador de la víctima:

- un **logout**: se cierra su sesión, sin pérdida de datos;
- un **refresh**: se rota su cookie, pero CORS impide leer el nuevo access token.

*Remediación propuesta*: exigir en esos dos endpoints una cabecera propia (por ejemplo,
`X-Requested-With`), que un formulario de otro sitio no puede enviar y que CORS solo permite
a los orígenes autorizados. Requiere un cambio coordinado con el frontend.

---

## A02 · Security Misconfiguration

**Controles existentes**

- Actuator solo expone `health`, sin detalles (`application.yml:14`).
- CORS con lista explícita de orígenes desde `CORS_ALLOWED_ORIGINS`
  (`CorsConfig.java:36`); no hay comodín, que sería incompatible con
  `allowCredentials` (`CorsConfig.java:40`).
- Sesión sin estado (`SecurityConfig.java:58`): no hay sesión de servidor que secuestrar.
- Cabeceras por defecto de Spring Security: `X-Content-Type-Options`, `X-Frame-Options:
  DENY`, `Cache-Control` y HSTS bajo HTTPS.
- Los secretos (`JWT_SECRET`, `MFA_ENCRYPTION_KEY`, credenciales de base) solo existen como
  variables de entorno de Render, sin valor por defecto en el código: si faltan, la
  aplicación no arranca.
- El contenedor corre con un usuario sin privilegios (`Dockerfile:20`, `USER app`).

**H-07 · Swagger UI y `/v3/api-docs` públicos en producción** · Informativa · ⚪ Riesgo aceptado

El contrato completo de la API se publica sin autenticación. **Se acepta** porque el
entregable pide compartir la documentación desplegada, y además:

- el documento no contiene datos ni secretos;
- toda operación sigue exigiendo JWT y scope;
- la API no depende de que sus rutas sean secretas.

Revisión: al cerrar el curso, restringirlo con `springdoc.api-docs.enabled=false` en el
perfil de producción.

---

## A03 · Software Supply Chain Failures

**Controles existentes**

- **Dependabot security updates** abre PR ante dependencias con vulnerabilidades conocidas.
- **CodeQL** (configuración por defecto: Java/Kotlin, Actions y Python) es un check
  obligatorio en `develop` y `main`.
- El Quality Gate de **SonarCloud** es obligatorio en cada PR.
- **Secret scanning** con push protection: bloquea un push que contenga credenciales.
- Las versiones de Spring Boot, Spring Modulith y Testcontainers las fijan sus BOM; el
  resto de las versiones está fijado en el `pom.xml`.

**H-06 · jjwt desactualizado y Actions fijadas por tag** · Baja · 🟢 Corregido

jjwt estaba en 0.12.3 (2023), y las Actions se referenciaban por tag (`@v7`), que su autor
puede mover a otro código.

*Corrección* (commit `3c97c51`): jjwt 0.13.0, la última versión publicada, y cada Action
fijada por SHA con su versión en un comentario.

*Pendiente sin plazo*: generar un SBOM (CycloneDX) en el pipeline.

---

## A04 · Cryptographic Failures

**Controles existentes**

| Dato | Protección | Evidencia |
|---|---|---|
| Contraseñas | BCrypt | `BCryptPasswordHasherAdapter` |
| Secreto TOTP | AES-256-GCM con IV aleatorio, clave `MFA_ENCRYPTION_KEY` | `AesSecretEncryptorAdapter` |
| Códigos de respaldo y refresh tokens | Solo se guarda su hash SHA-256; el valor es aleatorio (`SecureRandom`) | `HashingSupport` |
| Access token | JWT HS256. `Keys.hmacShaKeyFor` rechaza una clave de menos de 256 bits al arrancar | `JjwtAccessTokenAdapter.java:29` |
| Cookie de refresh | `HttpOnly`, `Secure`, `path=/api/v1/auth` | `AuthController` |
| Tránsito | HTTPS terminado por Render | — |

**H-11 · SonarCloud S4790: uso de SHA-1** · Informativa · ⚪ Falso positivo

`HaveIBeenPwnedAdapter` calcula SHA-1 de la contraseña. No se usa para almacenarla: la API
de Have I Been Pwned exige SHA-1 y solo recibe los primeros 5 caracteres del hash
(k-anonimato). Se marca como *Safe* en SonarCloud.

---

## A05 · Injection

**Controles existentes**

- Todo el acceso a datos usa JPA con parámetros (`:param`) o *Specifications*. Las dos
  consultas nativas (`VentaJpaRepository`, `VentanaQuiebreJpaRepository`) usan parámetros
  enlazados; no se concatena SQL.
- Bean Validation en cada cuerpo (`@Valid`), con longitudes máximas acordes a las columnas.
- Enumeraciones cerradas para motivos y tipos: un valor fuera de la lista responde 400.

**H-12 · SonarCloud S8786: expresión regular del email con backtracking superlineal** · Informativa · ⚪ Sin acción

`Email.java` valida con `^[^\s@]+@[^\s@]+\.[^\s@]+$`. El *backtracking* es como mucho
cuadrático y la longitud se valida **antes** de aplicar la expresión: 255 caracteres en
`Email.java:32`, antes de `:35`, y además `@Size(max = 255)` en el cuerpo. El peor caso
cuesta del orden de 65 000 pasos, sin riesgo de denegación de servicio.

---

## A06 · Insecure Design

**Controles existentes**

- Bloqueo progresivo por cuenta: 3, 5, 7 y 10 intentos fallidos bloquean 1, 5, 15 y 60
  minutos (`Usuario.java:33`). El segundo factor suma intentos al mismo contador.
- Ventas y solicitudes idempotentes por clave: un reintento no duplica ni descuenta dos veces.
- Concurrencia: bloqueo optimista con reintento, bloqueos pesimistas en la anulación y
  bloqueo consultivo por transacción.
- Reglas que evitan perder el control del sistema: nadie se desactiva a sí mismo, y no se
  puede quitar el rol ADMIN ni desactivar al último administrador activo
  (`UsuarioService.java:74`).
- El inventario nunca queda negativo; los ajustes exigen un motivo de una lista cerrada.

**H-08 · El listado de traspasos de una tienda no está paginado** · Baja · 🟡 90 días

`GET /api/v1/tiendas/{tiendaId}/traspasos-internos` (`TraspasoInternoController.java:74`)
devuelve todos los traspasos de la tienda. Crecen con cada reposición automática, así que
la respuesta y la memoria crecen sin límite. Los otros siete listados sin paginar (usuarios,
roles, scopes y sus asignaciones, y prioridad de CD) son listas acotadas por naturaleza.

*Remediación*: paginar con `Pageable`, como el resto de los listados.

---

## A07 · Authentication Failures

**Controles existentes**

- Contraseñas de 12 a 128 caracteres (`Password.java:21`), rechazadas si figuran en
  filtraciones conocidas (`RegistrarUsuarioService.java:36`).
- MFA con TOTP y 8 códigos de respaldo de un solo uso. El token de desafío vence en 5
  minutos.
- Respuesta uniforme: credenciales inválidas, usuario inactivo y cuenta bloqueada responden
  el mismo 401. Un hash señuelo (`LoginService.java:32`) iguala el tiempo de respuesta
  cuando el email no existe.
- Access token de 15 minutos. Refresh de 7 días con rotación: presentar uno ya usado revoca
  toda su familia (`RefrescarTokenService.java:56`).
- Desactivar un usuario revoca sus refresh tokens, y sus access tokens dejan de aceptarse en
  la siguiente petición.

**H-01 · La contraseña inicial del administrador semilla era válida en producción** · Crítica · 🟢 Corregido

La migración `V4__seed_admin_inicial.sql` crea `admin@supplychain.local` con una contraseña
conocida por el equipo, y esa contraseña permitía iniciar sesión en producción.

*Corrección*: se rotó la contraseña con un `UPDATE` directo en Supabase, con un hash BCrypt
generado localmente, y se revocaron los refresh tokens del administrador. V4 no se modificó:
cambiar una migración aplicada rompe el checksum de Flyway. Verificado en producción.

**H-03 · Sin límite de intentos por IP en los endpoints públicos** · Media · 🟡 30 días

El bloqueo progresivo es por cuenta. No hay límite por dirección IP, y eso permite dos
abusos:

- probar una contraseña común contra muchas cuentas sin activar ningún bloqueo;
- bloquear a propósito la cuenta de otra persona.

*Remediación propuesta*: limitar `/auth/login` y `/auth/login/mfa` por IP (por ejemplo, con
Bucket4j). En Render, la IP real llega en `X-Forwarded-For`.

**H-10 · La verificación contra filtraciones es fail-open** · Baja · ⚪ Aceptado

Si Have I Been Pwned no responde a tiempo, el registro se acepta sin verificar
(`HaveIBeenPwnedAdapter`, decisión documentada en el código). Se prioriza que el alta de
usuarios no dependa de un servicio externo. La política de longitud mínima sigue aplicando.

---

## A08 · Software or Data Integrity Failures

**Controles existentes**

- Flyway valida el checksum de cada migración aplicada; una migración modificada impide
  arrancar.
- Producción solo cambia mediante el job `desplegar`. Ese job corre en push a `main`
  después de la verificación y del Quality Gate; el Auto-Deploy de Render está apagado.
- `main` y `develop` no admiten *force push* ni integración sin los checks obligatorios,
  tampoco para administradores.
- El secreto del deploy hook vive en el entorno de GitHub `produccion`, que solo acepta la
  rama `main`.
- Jackson sin tipado polimórfico por defecto: el JSON recibido no puede instanciar clases
  arbitrarias.

Sin hallazgos.

---

## A09 · Security Logging and Alerting Failures

**Controles existentes**

- Logger `seguridad` (`RegistroSeguridad`) para autenticación. Registra cada intento fallido
  de contraseña o de segundo factor, cada bloqueo de cuenta, cada rechazo por bloqueo y los
  intentos con un email no registrado.
- Identifica la cuenta por su id. Nunca registra contraseñas, códigos ni el email intentado;
  `LoginServiceTest` lo verifica.

**H-05 · Registro de eventos de seguridad incompleto** · Media · 🟡 Parcial, 30 días

Desde el commit `be5c3c2` se registra la autenticación. Falta:

- registrar los cambios de permisos: asignar o quitar roles, scopes y tiendas, y activar o
  desactivar usuarios, con quién lo hizo;
- generar alertas: hoy los eventos solo quedan en el log de Render, que en el plan gratuito
  se conserva poco tiempo;
- enviar los logs a un servicio externo con retención y alertas (por ejemplo, Better Stack o
  Grafana Cloud).

---

## A10 · Mishandling of Exceptional Conditions

**Controles existentes**

- `GlobalExceptionHandler` traduce las excepciones de negocio a 400, 401, 403, 404 y 409
  con el cuerpo `ErrorResponse` y un mensaje en español. No incluye trazas.
- Las violaciones de restricciones de la base responden 409, sin filtrar el SQL.
- En la evaluación masiva, la falla de una variante se registra y no detiene al resto.

**H-09 · Los errores no previstos no usan el cuerpo de error de la API** · Baja · 🟡 90 días

Una excepción sin manejador cae en la respuesta por defecto de Spring Boot: 500 con
`timestamp`, `status`, `error` y `path`. No filtra la traza, pero no sigue el contrato
`ErrorResponse`, y el error no queda registrado con el contexto de la petición.

*Remediación*: un `@ExceptionHandler(Exception.class)` que registre el error y responda 500
con un `ErrorResponse` genérico.

**H-13 · SonarCloud S2259: posible `NullPointerException` en `VarianteWebMapper`** · Baja · 🟡 90 días

`VarianteWebMapper.java:17` desreferencia la variante sin comprobar `null`. Hoy siempre
llega una variante válida, pero conviene validarla o anotar el contrato.

---

## Hallazgos consolidados

| ID | Hallazgo | Categoría | Severidad | Estado | Plazo |
|---|---|---|---|---|---|
| H-01 | Contraseña inicial del admin semilla válida en producción | A07 | Crítica | 🟢 Corregido | — |
| H-02 | Evaluación de reposición sin control de tienda | A01 | Alta | 🟢 Corregido | — |
| H-03 | Sin límite de intentos por IP | A07 | Media | 🟡 Pendiente | 30 días |
| H-05 | Registro de eventos de seguridad incompleto | A09 | Media | 🟡 Parcial | 30 días |
| H-04 | CSRF en refresh y logout por cookie `SameSite=None` | A01 | Baja | 🟡 Pendiente | 90 días |
| H-08 | Traspasos de una tienda sin paginación | A06 | Baja | 🟡 Pendiente | 90 días |
| H-09 | Errores 500 fuera del contrato `ErrorResponse` | A10 | Baja | 🟡 Pendiente | 90 días |
| H-13 | Posible NPE en `VarianteWebMapper` (Sonar S2259) | A10 | Baja | 🟡 Pendiente | 90 días |
| H-06 | jjwt desactualizado y Actions por tag | A03 | Baja | 🟢 Corregido | — |
| H-10 | Verificación contra filtraciones fail-open | A07 | Baja | ⚪ Aceptado | — |
| H-07 | Swagger público en producción | A02 | Informativa | ⚪ Riesgo aceptado | — |
| H-11 | SHA-1 en Have I Been Pwned (Sonar S4790) | A04 | Informativa | ⚪ Falso positivo | — |
| H-12 | Expresión regular del email (Sonar S8786) | A05 | Informativa | ⚪ Sin acción | — |

De 13 hallazgos, 3 están corregidos (entre ellos el único crítico y el único alto), 1 está
corregido en parte y 5 tienen remediación propuesta con plazo. Los otros 4 son decisiones
documentadas: un riesgo aceptado, un falso positivo, uno informativo sin acción y un
comportamiento aceptado (H-10).
