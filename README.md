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
entidades JPA de otro módulo. La comunicación entre módulos ocurre siempre a
través de los puertos de aplicación (`application/port/*`).

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
entidades del ERD de negocio. El esquema del módulo `identity`
(usuarios/autenticación) se agregará en una migración posterior, cuando
ese módulo se diseñe en detalle.
