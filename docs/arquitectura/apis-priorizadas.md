# APIs priorizadas

Entregables «APIs priorizadas» y «al menos tres APIs REST». Este documento explica **qué API
importa más y por qué**; el contrato de cada endpoint (parámetros, cuerpos, respuestas y
errores) está en Swagger y no se repite aquí.

- **Contrato:** OpenAPI 3.1 generado desde el código por springdoc. Navegable en
  [Swagger UI](https://supplychain-api-dzmc.onrender.com/swagger-ui.html) y descargable en
  [`/v3/api-docs`](https://supplychain-api-dzmc.onrender.com/v3/api-docs).
- **Versión:** todas las rutas bajo `/api/v1`.
- **Autenticación:** `Authorization: Bearer <access token>` (JWT de 15 minutos) en todo
  endpoint salvo los públicos de `auth`. El refresh token viaja en una cookie `HttpOnly`.
- **Autorización:** cada endpoint exige un scope con `@PreAuthorize`. Los scopes se
  resuelven contra la base en cada petición, así que quitar un scope aplica de inmediato.
  Las operaciones sobre una tienda exigen además poder operar en ella (asignación
  usuario-tienda, HU-32, o el scope `tiendas:operar-todas`).
- **Errores:** cuerpo uniforme `ErrorResponse` desde `GlobalExceptionHandler`.
- **Estado:** 1 de noviembre de 2026, sobre `develop` (igual a `main`): **89 endpoints en
  21 controladores y 5 módulos** (`logistica` todavía no expone endpoints).

## Criterio de prioridad

La prioridad se asigna por **criticidad operativa**: el impacto que tendría que la API
fallara durante un día normal de operación de las tiendas. El orden de construcción ya está
fijado por el camino crítico del informe de arquitectura v2
(HU-28 → HU-30 → HU-32 → HU-33 → HU-36 → HU-40 → HU-45), así que aquí no se repite.

| Prioridad | Significado | Si falla… |
|---|---|---|
| **P1 — Habilitante** | Sin ella ninguna otra operación protegida es posible | Nadie opera: todo token sale de aquí |
| **P2 — Operación diaria** | Captura lo que ocurre en la tienda: ventas, demanda perdida y existencias | Se pierden ventas o demanda sin registrar y el stock deja de ser confiable; es el objetivo del Sprint 2 |
| **P3 — Control y reposición** | Corrige y repone el inventario; corre por evento o por decisión de un supervisor | La operación sigue; el sistema se corrige con el siguiente movimiento o con una reconciliación |
| **P4 — Configuración y administración** | Datos maestros y control de acceso que cambian poco | Solo se bloquean altas y cambios; lo ya configurado sigue funcionando |

| Prioridad | Endpoints | Módulos |
|---|---:|---|
| P1 | 6 | `identity` |
| P2 | 14 | `ventas`, `inventario` |
| P3 | 13 | `inventario`, `ventas`, `reposicion` |
| P4 | 56 | `catalogo`, `inventario`, `identity` |
| **Total** | **89** | |

## Las tres APIs REST principales

Una por cada nivel crítico y de tres módulos distintos. Son las que se documentan con más
detalle y ejemplos en Swagger.

| API | Prioridad | Módulo | Por qué |
|---|---|---|---|
| **Autenticación** (`/api/v1/auth`, `/api/v1/mfa`) | P1 | `identity` | Puerta de todas las demás. Login con bloqueo progresivo, segundo factor TOTP, refresh con rotación y detección de reuso, logout idempotente |
| **Ventas y demanda** (`/api/v1/ventas`, `/api/v1/solicitudes-no-satisfechas`, `/api/v1/ventanas-quiebre`) | P2 | `ventas` | Proceso principal del negocio. Idempotente por `transaccionId`, descuenta stock en la misma transacción, restringida por tienda y con efectos encadenados por eventos (ventana de quiebre, traspaso automático) |
| **Inventario** (`/api/v1/nodos/{nodoId}/inventario/...`, `/api/v1/variantes/{varianteId}/inventario`) | P2 / P3 | `inventario` | Fuente de verdad del stock por nodo. Kardex con saldo resultante, carga inicial todo o nada y ajustes que nunca dejan el stock negativo |

## Inventario por prioridad

«Por tienda» indica que, además del scope, el caso de uso verifica que el usuario pueda
operar en la tienda del recurso; si no puede, el recurso se trata como inexistente (404).

### P1 — Habilitante · `identity`

| Método y ruta | Qué hace | Acceso | HU | Pruebas |
|---|---|---|---|---|
| `POST /api/v1/auth/login` | Valida credenciales; emite access token y cookie de refresh, o un token de desafío si la cuenta tiene MFA. Bloqueo progresivo tras intentos fallidos | Público | HU-03, HU-04 | `AuthControllerTest`, `IniciarSesionIT` |
| `POST /api/v1/auth/login/mfa` | Segundo paso: cambia el token de desafío y un código TOTP o de respaldo por los tokens de sesión | Público (credencial: token de desafío) | HU-08 | `AuthControllerTest` |
| `POST /api/v1/auth/refresh` | Renueva el access token rotando el refresh; si detecta reuso revoca toda la familia | Público (credencial: cookie) | HU-05 | `AuthControllerTest` |
| `POST /api/v1/auth/logout` | Revoca el refresh y borra la cookie; siempre 204, no revela si había sesión | Público (credencial: cookie) | HU-06 | `AuthControllerTest` |
| `POST /api/v1/mfa/activar` | Genera el secreto TOTP del usuario, cifrado en reposo, para registrarlo en su app autenticadora | Autenticado | HU-07 | `MfaControllerTest` |
| `POST /api/v1/mfa/confirmar` | Confirma el primer código TOTP, habilita MFA y genera los códigos de respaldo | Autenticado | HU-07 | `MfaControllerTest` |

### P2 — Operación diaria · `ventas`, `inventario`

| Método y ruta | Qué hace | Acceso | HU | Pruebas |
|---|---|---|---|---|
| `POST /api/v1/ventas` | Registra una venta por lote y descuenta el stock del Almacén. Sin stock responde 409 salvo venta forzada; un reintento con el mismo `transaccionId` devuelve 200 con la venta original | `ventas:registrar` · por tienda | HU-40 | `VentaControllerTest` |
| `GET /api/v1/ventas` | Lista paginada, filtrable por tienda, variante, transacción, estado y fechas | `ventas:consultar` · por tienda | HU-42 | `VentaControllerTest` |
| `GET /api/v1/ventas/{id}` | Una venta; la de otra tienda responde 404 | `ventas:consultar` · por tienda | HU-42 | `VentaControllerTest` |
| `POST /api/v1/ventas/{id}/anular` | Anula una línea con motivo y repone el stock; no se anula dos veces | `ventas:anular` · por tienda | HU-41 | `VentaControllerTest` |
| `POST /api/v1/ventas/transacciones/{transaccionId}/anular` | Anula todas las líneas de una transacción | `ventas:anular` · por tienda | HU-41 | `VentaControllerTest` |
| `POST /api/v1/solicitudes-no-satisfechas` | Registra demanda perdida con el stock del Almacén en ese momento; idempotente por clave | `ventas:registrar` · por tienda | HU-43 | `SolicitudNoSatisfechaControllerTest` |
| `GET /api/v1/solicitudes-no-satisfechas` | Lista paginada y filtrable | `ventas:consultar` · por tienda | HU-44 | `SolicitudNoSatisfechaControllerTest` |
| `GET /api/v1/solicitudes-no-satisfechas/{id}` | Una solicitud | `ventas:consultar` · por tienda | HU-44 | `SolicitudNoSatisfechaControllerTest` |
| `POST /api/v1/solicitudes-no-satisfechas/{id}/anular` | Anula con motivo obligatorio; idempotente | `ventas:anular` · por tienda | HU-44 | `SolicitudNoSatisfechaControllerTest` |
| `GET /api/v1/ventanas-quiebre` | Ventanas de quiebre de stock, abiertas o cerradas, filtrables | `ventas:consultar` · por tienda | HU-45, HU-46 | `VentaControllerTest` |
| `GET /api/v1/ventanas-quiebre/{id}` | Una ventana de quiebre | `ventas:consultar` · por tienda | HU-46 | `VentaControllerTest` |
| `GET /api/v1/nodos/{nodoId}/inventario` | Stock de un nodo (Almacén, Bodega o CD), paginado | `inventario:consultar` · por tienda | HU-39 | `ConsultaInventarioControllerTest` |
| `GET /api/v1/variantes/{varianteId}/inventario` | Stock de una variante en los nodos visibles para el usuario | `inventario:consultar` · por tienda | HU-39 | `ConsultaInventarioControllerTest` |
| `GET /api/v1/nodos/{nodoId}/inventario/movimientos` | Kardex del nodo con saldo resultante, filtrable por tipo y fechas | `inventario:consultar` · por tienda | HU-39 | `ConsultaInventarioControllerTest` |

### P3 — Control y reposición · `inventario`, `ventas`, `reposicion`

| Método y ruta | Qué hace | Acceso | HU | Pruebas |
|---|---|---|---|---|
| `POST /api/v1/nodos/{nodoId}/inventario/carga-inicial` | Carga inicial por SKU, hasta 500 ítems, todo o nada; 409 si la fila ya existe | `inventario:administrar` o `inventario-cd:administrar` · por tienda | HU-37 | `CargaInicialControllerTest` |
| `POST /api/v1/nodos/{nodoId}/inventario/ajustes` | Ajuste con motivo de una lista cerrada; nunca deja el stock negativo | `inventario:administrar` o `inventario-cd:administrar` · por tienda | HU-38 | `AjusteInventarioControllerTest` |
| `POST /api/v1/ventanas-quiebre/reconciliar` | Reconstruye las ventanas de quiebre de una tienda desde el kardex; idempotente | `inventario:administrar` · por tienda | HU-46 | `VentaControllerTest` |
| `POST /api/v1/umbrales-reposicion` | Crea un umbral mínimo y objetivo por variante o por categoría | `reposicion:administrar` | HU-47 | `UmbralReposicionControllerTest` |
| `GET /api/v1/umbrales-reposicion` | Lista los umbrales | `reposicion:administrar` | HU-47 | `UmbralReposicionControllerTest` |
| `GET /api/v1/umbrales-reposicion/{id}` | Un umbral | `reposicion:administrar` | HU-47 | `UmbralReposicionControllerTest` |
| `PUT /api/v1/umbrales-reposicion/{id}` | Modifica un umbral | `reposicion:administrar` | HU-47 | `UmbralReposicionControllerTest` |
| `DELETE /api/v1/umbrales-reposicion/{id}` | Elimina un umbral | `reposicion:administrar` | HU-47 | `UmbralReposicionControllerTest` |
| `POST /api/v1/tiendas/{tiendaId}/traspasos-internos` | Traspaso manual Bodega → Almacén; nunca deja la Bodega en negativo | `reposicion:administrar` · por tienda | HU-49 | `TraspasoInternoControllerTest` |
| `GET /api/v1/tiendas/{tiendaId}/traspasos-internos` | Traspasos de una tienda, automáticos y manuales | `reposicion:administrar` · por tienda | HU-49 | `TraspasoInternoControllerTest` |
| `GET /api/v1/traspasos-internos/{id}` | Un traspaso | `reposicion:administrar` · por tienda | HU-49 | `TraspasoInternoControllerTest` |
| `POST /api/v1/reposicion-interna/evaluar` | Evalúa la reposición de todas las tiendas activas | `reposicion:administrar` | HU-50 | `EvaluacionMasivaControllerTest` |
| `POST /api/v1/reposicion-interna/tiendas/{tiendaId}/evaluar` | Evalúa la reposición de una tienda | `reposicion:administrar` | HU-50 | `EvaluacionMasivaControllerTest` |

El traspaso automático Bodega → Almacén (HU-48) no tiene endpoint: lo dispara el evento
`ExistenciasActualizadas` después de cada venta o ajuste.

### P4 — Configuración y administración · `catalogo`, `inventario`, `identity`

**Catálogo** (FEAT-05) · `catalogo:administrar` · pruebas: `CategoriaControllerTest`,
`TemplateControllerTest`, `VarianteControllerTest` y las `*ProductoIT` de QA.

| Método y ruta | Qué hace |
|---|---|
| `POST`, `GET /api/v1/categorias` | Crea y lista categorías (`SIN_CATEGORIA` es reservada, HU-35) |
| `GET`, `PUT`, `DELETE /api/v1/categorias/{id}` | Obtiene, modifica y elimina una categoría; con templates asociados responde 409 |
| `POST`, `GET /api/v1/templates` | Crea y lista templates de producto; la categoría es obligatoria (HU-35) |
| `GET`, `PUT`, `DELETE /api/v1/templates/{id}` | Obtiene, modifica y elimina un template |
| `POST`, `GET /api/v1/variantes` | Crea y lista variantes (SKU) |
| `GET /api/v1/variantes/{id}`, `GET /api/v1/variantes/sku/{sku}` | Obtiene una variante por id o por SKU |
| `PUT`, `DELETE /api/v1/variantes/{id}` | Modifica y elimina una variante |

**Red de nodos** (FEAT-02 a FEAT-04)

| Método y ruta | Qué hace | Acceso | HU | Pruebas |
|---|---|---|---|---|
| `POST`, `GET /api/v1/tiendas` | Crea una tienda con su Almacén y su Bodega; lista tiendas | `tiendas:administrar` | FEAT-02, HU-34 | `TiendaControllerTest`, `*TiendaIT` |
| `GET`, `PUT /api/v1/tiendas/{id}` | Obtiene (con los ids de sus nodos) y modifica una tienda | `tiendas:administrar` | FEAT-02 | `TiendaControllerTest` |
| `DELETE /api/v1/tiendas/{id}` | Desactiva la tienda (borrado lógico) | `tiendas:administrar` | FEAT-02, HU-45 | `TiendaControllerTest` |
| `PUT /api/v1/tiendas/{id}/activar` | Reactiva una tienda | `tiendas:administrar` | HU-24 | `TiendaControllerTest` |
| `GET /api/v1/bodegas-tienda`, `/{id}`, `/tienda/{tiendaId}` | Consulta de Bodegas; solo lectura porque se crean con la tienda | `tiendas:administrar` | FEAT-03, HU-34 | `BodegaTiendaControllerTest`, `*BodegaTiendaIT` |
| `POST`, `GET /api/v1/centros-distribucion` | Crea y lista centros de distribución | `cd:administrar` | FEAT-04 | `CentroDistribucionControllerTest`, `*CentroDistribucionIT` |
| `GET`, `PUT`, `DELETE /api/v1/centros-distribucion/{id}` | Obtiene (con su nodo), modifica y elimina un CD | `cd:administrar` | FEAT-04, HU-22 | `CentroDistribucionControllerTest` |
| `PUT`, `GET /api/v1/tiendas/{tiendaId}/prioridad-cd` | Reemplaza completa y consulta la lista ordenada de CD que abastecen la tienda | `inventario:administrar` | HU-51 | `PrioridadTiendaCdControllerTest` |

**Control de acceso** (FEAT-01) · pruebas: `UsuarioControllerTest`, `RolControllerTest`,
`ScopeControllerTest`, `RegistrarUsuarioIT`.

| Método y ruta | Qué hace | Acceso | HU |
|---|---|---|---|
| `POST /api/v1/usuarios` | Registra un usuario con política de contraseña y verificación contra filtraciones (HIBP) | `usuarios:administrar` | HU-02 |
| `GET /api/v1/usuarios`, `/{id}` | Lista y obtiene usuarios | `usuarios:administrar` | HU-02 |
| `GET`, `POST /api/v1/usuarios/{id}/roles`, `DELETE …/roles/{rolId}` | Consulta, asigna y quita roles | `usuarios:administrar` | HU-10 |
| `PUT /api/v1/usuarios/{id}/desactivar`, `/activar` | Desactiva y reactiva; nadie puede desactivarse a sí mismo | `usuarios:administrar` | HU-31 |
| `GET`, `POST /api/v1/usuarios/{id}/tiendas`, `DELETE …/tiendas/{tiendaId}` | Consulta, asigna y quita tiendas en las que el usuario opera | `usuarios:administrar` | HU-32 |
| `POST`, `GET /api/v1/roles`, `GET`, `PUT`, `DELETE /api/v1/roles/{id}` | CRUD de roles | `roles:administrar` | HU-09 |
| `GET`, `POST /api/v1/roles/{id}/scopes`, `DELETE …/scopes/{scopeId}` | Consulta, asigna y quita scopes de un rol | `roles:administrar` | HU-09, HU-11 |
| `POST`, `GET /api/v1/scopes`, `GET`, `PUT`, `DELETE /api/v1/scopes/{id}` | CRUD de scopes | `scopes:administrar` | HU-09 |

Fuera de `/api/v1`, `GET /actuator/health` es público: lo usa Render para saber si el
servicio está vivo.

## Cumplimiento de los requisitos

| Requisito | Cumplimiento |
|---|---|
| Al menos tres APIs REST | Tres principales (Autenticación, Ventas y demanda, Inventario) y 89 endpoints en total |
| Endpoints de distintos módulos | Cinco módulos: `identity`, `catalogo`, `inventario`, `ventas`, `reposicion` |
| Diseño orientado a recursos | Sustantivos en plural; filtros como parámetros sobre la colección; transiciones de estado como sub-recursos de acción (`/anular`, `/activar`, `/desactivar`, `/evaluar`) |
| Autorización por endpoint | Scope en cada endpoint con `@PreAuthorize`; restricción por tienda en el caso de uso |
| Idempotencia | Registrar venta y solicitud, anular solicitud, logout y reconciliación |
| Paginación | Los listados de catálogo, tiendas, CD, bodegas, umbrales, inventario, kardex, ventas, solicitudes y ventanas son paginados (HU-26). **Ocho no lo están**: usuarios, roles, scopes, scopes de un rol, roles y tiendas de un usuario, traspasos de una tienda y prioridad de CD. Los seis de `identity` y la prioridad de CD son listas acotadas; los traspasos de una tienda crecen sin límite y quedan registrados como hallazgo en el análisis OWASP |
| Contrato publicado | OpenAPI 3.1 en `/v3/api-docs` y Swagger UI en producción |
