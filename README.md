# Supply Chain MVP

Sistema de planificación de cadena de suministro retail (MVP). Monolito modular
con arquitectura hexagonal por módulo, expuesto vía API REST.

## Stack

- Java 21 (LTS)
- Spring Boot 4.1.1 (Spring Framework 7)
- PostgreSQL 16
- Maven (via Maven Wrapper — ver "Primeros pasos")
- Docker / Docker Compose

## Módulos

Cada módulo bajo `src/main/java/com/codefactory/supplychain/` sigue el patrón
de Puertos y Adaptadores (arquitectura hexagonal):

```
<modulo>/
├── api/               # Lo único que otros módulos pueden usar (solo si tiene consumidores)
├── domain/            # Reglas de negocio puras, sin dependencias de framework
│   ├── model/
│   └── exception/
├── application/       # Casos de uso
│   ├── port/in/       # Qué expone el módulo
│   ├── port/out/      # Qué necesita el módulo de la infraestructura
│   └── service/       # Implementación de los casos de uso
└── infrastructure/    # Detalles técnicos
    └── adapter/
        ├── in/web/            # Controladores REST + DTOs
        └── out/persistence/   # Entidades JPA, repositorios, mappers
```

Módulos actuales: `identity`, `catalogo`, `inventario`, `ventas`,
`reposicion`, `logistica`. Componentes transversales (config, seguridad,
excepciones globales, DTOs comunes) viven en `shared/`.

**Regla no negociable:** ningún módulo accede directamente a las tablas o
entidades JPA de otro módulo. Un módulo solo puede usar de otro lo que este
publica en su paquete `api/` (marcado con `@NamedInterface("api")`), y lo hace a
través de un puerto propio y un adaptador. La guía completa, con ejemplos y el
checklist para cada HU, está en [ARQUITECTURA.md](ARQUITECTURA.md).
Spring Modulith la verifica en cada build (`ModularidadTest`): si un módulo usa un
tipo interno de otro, o aparece un ciclo entre módulos, `./mvnw verify` falla.
`shared` es un módulo abierto.

**Eventos entre módulos.** Si un módulo necesita una respuesta de otro, usa un
puerto síncrono; si solo informa un hecho, publica un evento. Todo listener que
reacciona a un hecho de otro módulo sigue esta forma:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void alOcurrirAlgo(AlgoOcurrio evento) { ... }
```

Corre en el mismo hilo, después del commit y antes de responder al cliente; si
falla, Spring lo registra en el log y la operación de origen queda confirmada.
Spring no arranca si el listener declara `@Transactional` sin `REQUIRES_NEW`.
Comportamiento fijado en `EventosReintentosZonaHorariaTest`.

## Primeros pasos

1. Generar el Maven Wrapper (requiere Maven instalado localmente una única vez):
   ```
   mvn -N io.takari:maven:wrapper -Dmaven=3.9.9
   ```
2. Crear tu archivo de variables de entorno local a partir de la plantilla:
   ```
   cp .env.example .env
   ```
   Luego completa los valores reales en `.env` (nunca se commitea).
3. Levantar el stack completo:
   ```
   docker compose up --build
   ```
4. La API queda disponible en `http://localhost:8080`.

## Esquema de base de datos

El esquema se gestiona con **Flyway**, no con `ddl-auto` de Hibernate.
Las migraciones viven en `src/main/resources/db/migration/`, con la
convención de nombre `V<numero>__descripcion.sql`. Una migración ya
aplicada nunca se edita — cualquier corrección se hace con una migración
nueva. `spring.jpa.hibernate.ddl-auto` queda en `validate`: Hibernate
verifica que las entidades coincidan con lo que Flyway ya aplicó, pero
nunca modifica el esquema por su cuenta.

La migración inicial (`V1__crear_esquema_inicial.sql`) cubre todas las
entidades del ERD de negocio, con UUID como identificador en todo el
dominio de negocio. El esquema del módulo `identity` (usuarios/autenticación)
se agrega en `V2` a `V8`; las migraciones posteriores (`V9` en adelante)
extienden el esquema de negocio a medida que se desarrollan las features.

## Pruebas

- `./mvnw verify` corre todas las pruebas del equipo de desarrollo (sufijo `Test`),
  con Postgres real vía Testcontainers, la verificación de módulos y el umbral de
  cobertura de JaCoCo (85 %). Es lo mismo que corre la CI.
- `./mvnw -P casos-qa-sprint1 verify` corre las pruebas funcionales de QA
  (sufijo `IT`, en `src/test/.../qa/`) y genera su reporte.
- Para probar autorización con usuarios reales, usar `soporte/UsuariosDePrueba`
  (`@Import(UsuariosDePrueba.class)`): `usuarios.conRol("VENDEDOR", tiendaId)` o
  `usuarios.conScopes(Set.of("ventas:anular"))` devuelven el usuario y su token
  (`bearer()`), sin copiar en cada test el armado de usuario, rol y login.
- Los roles y scopes sembrados por las migraciones (`ADMIN`, `VENDEDOR`,
  `SUPERVISOR_TIENDA`, `OPERADOR_CD` y sus scopes) son nombres reservados: no
  usarlos como datos de prueba nuevos.
