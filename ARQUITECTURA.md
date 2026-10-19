# Comunicación entre módulos: API pública por módulo

Esta guía define **cómo un módulo usa datos u operaciones de otro**. Es obligatoria para
toda HU que cruce módulos (decisión **D-71**, adoptada en la HU-32). Si una HU no cruza
módulos, no necesita nada de esto.

## 1. El problema

El sistema es un monolito modular: `identity`, `catalogo`, `inventario`, `ventas`,
`reposicion` y `logistica`. Cada módulo es dueño de sus datos y sus reglas. Pero los
módulos se necesitan entre sí. Solo en el Sprint 2:

- `identity` necesita saber si una tienda existe (las tiendas son de `inventario`).
- `ventas` necesita saber si una tienda está activa y descontar stock (`inventario`).
- `inventario` necesita convertir un SKU en una variante (`catalogo`).
- `reposicion` necesita la categoría de una variante (`catalogo`).

Si un módulo usa directamente las clases internas de otro (su modelo de dominio, sus
servicios, sus repositorios), queda atado a su implementación: cualquier cambio interno
rompe al vecino, y con el tiempo todo depende de todo.

La dependencia hacia el dato no se puede evitar: si `identity` necesita saber si una tienda
existe, depende de quien lo sabe. Lo que sí se decide es **hacia qué** se depende: hacia
el interior del otro módulo (mal) o hacia un **contrato publicado** y estable (bien).

## 2. Los conceptos (DDD)

| Concepto | Qué significa aquí |
|---|---|
| **Bounded Context** | Cada módulo. Tiene su propio modelo y su propio lenguaje: la "Tienda" de `inventario` (con nodos, estado, ubicación) no es lo mismo que la "tienda asignable" de `identity` (un id, un nombre y si está activa). |
| **Open Host Service** | El módulo que provee un dato publica una **API** para que otros la usen, en lugar de dejar que lean su interior. |
| **Published Language** | Esa API habla con tipos propios y estables (interfaces y `record`), **nunca** con las entidades del dominio. Así el dominio puede cambiar sin romper a nadie. |
| **Anti-Corruption Layer (ACL)** | El módulo que consume traduce la API ajena a su propio lenguaje en un adaptador. Su capa de aplicación nunca conoce al otro módulo. |
| **Shared Kernel** | Lo poco que todos comparten deliberadamente. Aquí es `shared/`: excepciones base, seguridad y `UsuarioActual` (el usuario del request). Nada de negocio de un módulo concreto va en `shared/`. |

## 3. Por qué este patrón y no otro

| Alternativa | Por qué no |
|---|---|
| Usar directamente las clases internas del otro módulo | Acopla al dominio ajeno y expone operaciones que no corresponden (por ejemplo, `TiendaUseCase` incluye crear y desactivar). Spring Modulith hace fallar el build. |
| Un módulo central de contratos (`contratos/`) que todos comparten | Nadie es su dueño, crece hasta saber de todo, y separa cada contrato de su implementación: cada cambio toca tres lugares en vez de dos. |
| Poner los contratos de negocio en `shared/` | Mezcla infraestructura técnica con negocio; `shared/` dejaría de ser transversal. |
| Copiar los datos de otros módulos con eventos | Consistencia eventual e infraestructura extra; desproporcionado para consultas simples del MVP. |

La API por módulo deja **el contrato junto a quien lo implementa** (el dueño es claro),
oculta el dominio, y Spring Modulith hace cumplir la regla automáticamente.

## 4. Las reglas

### 4.1 Quien provee: `<módulo>/api/`

```
<modulo>/
├── api/               # LO ÚNICO que otros módulos pueden usar
├── domain/
├── application/
└── infrastructure/
```

- El paquete `api/` lleva un `package-info.java` con `@NamedInterface("api")`. Todo lo que
  está fuera de `api/` es interno: si otro módulo lo usa, **el build falla**.
- En `api/` va **solo** lo que algún módulo consume hoy:
  - interfaces de consulta u operación (por ejemplo `ConsultaTiendas`);
  - `record` con los datos que se comparten (por ejemplo `TiendaResumen`);
  - los eventos que el módulo publica (por ejemplo `ExistenciasActualizadas`).
- En `api/` **nunca** van entidades de dominio, entidades JPA, repositorios, servicios,
  DTOs web ni nada "por si acaso".
- El `api/` de un módulo se crea cuando aparece su primer consumidor, no antes.
- La interfaz pública la implementa un servicio de aplicación del propio módulo, que
  traduce su dominio al `record` publicado.

### 4.2 Quien consume: puerto propio + adaptador

```
<consumidor>/
├── application/port/out/   # Puerto en SU lenguaje: no menciona al otro módulo
└── infrastructure/adapter/out/<proveedor>/
                            # Adaptador: el ÚNICO archivo que importa <proveedor>.api
```

- La capa de aplicación del consumidor depende de **su propio puerto**, con sus propios
  tipos. Así se prueba con un mock de ese puerto y no se entera si el proveedor cambia.
- El adaptador solo traduce y delega: sin reglas de negocio.

### 4.3 Síncrono o evento

Se aplica la regla **D-15**: *si necesito una respuesta, puerto síncrono; si solo informo
un hecho, evento*.

- **Puerto síncrono:** el proveedor publica una interfaz en `api/` (sección 4.1) y el
  consumidor la usa mediante su adaptador (sección 4.2).
- **Evento:** el `record` del evento vive en el `api/` de quien lo publica. Quien
  reacciona tiene el listener en `infrastructure/adapter/in/eventos/`, con la forma
  `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation =
  REQUIRES_NEW)` (ver README), y el listener solo invoca un caso de uso del propio módulo.
  Es síncrono (D-16): corre en el mismo hilo, así que sus efectos ya se ven cuando el
  cliente recibe la respuesta (D-18). No se usa `@Async`.

### 4.4 Lo que nunca se hace

- Leer o escribir tablas de otro módulo (la regla original del README).
- Importar algo de otro módulo que no esté en su `api/`.
- Crear ciclos: si `identity` depende de `inventario`, `inventario` no puede depender de
  `identity`. Spring Modulith también lo detecta.
- Poner contratos de negocio en `shared/`.

## 5. Ejemplo real: HU-32

`identity` necesita validar que una tienda existe para asignarla a un usuario.

**Provee `inventario`** (`inventario/api/`):

```java
// package-info.java
@NamedInterface("api")
package com.codefactory.supplychain.inventario.api;

public interface ConsultaTiendas {
    Optional<TiendaResumen> buscar(UUID tiendaId);
    List<TiendaResumen> buscarTodas(Collection<UUID> tiendaIds);
}

public record TiendaResumen(UUID id, String nombre, boolean activa) {}
```

La implementa `TiendaService` (`inventario/application/service`), que convierte su
`Tienda` de dominio en `TiendaResumen`.

**Consume `identity`**:

```java
// identity/application/port/out — su puerto, en su lenguaje
public interface TiendaConsultaPort {
    Optional<TiendaAsignable> buscar(UUID tiendaId);
    List<TiendaAsignable> buscarTodas(Collection<UUID> tiendaIds);
}

// identity/infrastructure/adapter/out/inventario — la capa anticorrupción
@Component
@RequiredArgsConstructor
public class TiendaConsultaAdapter implements TiendaConsultaPort {
    private final ConsultaTiendas consultaTiendas;   // único import de inventario.api
    public Optional<TiendaAsignable> buscar(UUID tiendaId) {
        return consultaTiendas.buscar(tiendaId).map(TiendaConsultaAdapter::traducir);
    }
    // ...
}
```

`UsuarioTiendaService` usa `TiendaConsultaPort` y nunca sabe que existe `inventario`.

**Verificación:** si `TiendaConsultaAdapter` usara `TiendaUseCase` (interno de
`inventario`), `./mvnw verify` falla con:

```
Module 'identity' depends on non-exposed type
com.codefactory.supplychain.inventario.application.port.in.TiendaUseCase within module 'inventario'!
```

## 6. Cómo aplica en el resto del Sprint 2

| HU | Contrato | Vive en | Lo consume | Tipo |
|---|---|---|---|---|
| HU-32 | `ConsultaTiendas`, `TiendaResumen` | `inventario/api` | `identity` | Síncrono (creado) |
| HU-36 | `ConsultaNodos`: Almacén y Bodega de una tienda | `inventario/api` | `ventas` (HU-40/41/43/46), `reposicion` (HU-48/49) | Síncrono |
| HU-36 | Movimientos que otros disparan: descontar por venta, reponer por anulación, trasladar | `inventario/api` | `ventas` (HU-40/41), `reposicion` (HU-48) | Síncrono |
| HU-36 | Consulta de stock de un nodo y una variante | `inventario/api` | `ventas` (HU-43, `stock_sistema`), `reposicion` (HU-48) | Síncrono |
| HU-36 | Evento `ExistenciasActualizadas` | `inventario/api` | `ventas` (HU-45), `reposicion` (HU-48) | Evento |
| HU-37 | Consulta de variantes: por SKU, existencia y categoría | `catalogo/api` | `inventario` (HU-37), `ventas` (HU-40/43), `reposicion` (HU-47) | Síncrono |
| HU-40/43 | Reutilizan `ConsultaTiendas` (tienda activa) | `inventario/api` | `ventas` | Síncrono (no se crea nada nuevo) |
| HU-45 | Evento `TiendaDesactivada` | `inventario/api` | `ventas` | Evento |
| HU-46 | `ConsultaKardex`: movimientos con saldo resultante | `inventario/api` | `ventas` (reconciliación) | Síncrono |
| HU-50 | `ConsultaTiendas.listarActivas` | `inventario/api` | `reposicion` (evaluación masiva) | Síncrono |

Las operaciones que solo usa el propio módulo (por ejemplo, la carga inicial y los ajustes
de inventario) **no** van en `api/`. Los nombres exactos se definen en cada HU; la tabla
fija dónde viven y quién los consume.

## 7. Checklist para cada HU que cruza módulos

1. ¿Necesito una respuesta (puerto) o solo informo un hecho (evento)?
2. ¿El contrato ya existe en el `api/` del proveedor? Si existe, se reutiliza.
3. Si no existe: interfaz o evento + `record` en `<proveedor>/api/`, sin tipos de dominio;
   el `package-info.java` con `@NamedInterface("api")` se crea si es el primer contrato.
4. El proveedor lo implementa en su capa de aplicación.
5. El consumidor declara su propio puerto en `application/port/out` con sus propios tipos.
6. El consumidor implementa el adaptador en `infrastructure/adapter/out/<proveedor>/`.
7. `./mvnw verify` en verde: `ModularidadTest` confirma que no hay accesos internos ni
   ciclos.

## 8. Preguntas frecuentes

**¿Puedo usar un caso de uso existente del otro módulo, que ya hace lo que necesito?**
No, salvo que esté en su `api/`. Si hace falta, se publica en `api/` una interfaz mínima
con solo lo necesario.

**Dos módulos necesitan el mismo dato. ¿Dos contratos?**
No. Un solo contrato en el `api/` del dueño del dato, que ambos consumen con sus propios
adaptadores (ver `ConsultaTiendas`).

**¿Y si el tipo publicado tiene más campos de los que necesito?**
El adaptador traduce y descarta lo que sobra: tu puerto expone solo lo que tu módulo usa.

**¿Por qué no basta con depender de la interfaz del otro módulo desde mi servicio?**
Porque entonces tu capa de aplicación queda atada a otro módulo. Con el puerto propio, el
cambio de la API ajena se absorbe en un solo adaptador.

**VS Code marca "Invalid reference to non-exposed type of module 'shared'".**
Es un falso positivo de la extensión de Spring: no reconoce que `shared` es un módulo
abierto. La verificación válida es `ModularidadTest`.
