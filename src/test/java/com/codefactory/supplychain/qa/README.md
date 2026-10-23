# Casos de prueba de QA

Casos `*IT` de caja negra contra la API real (RestAssured + PostgreSQL en Testcontainers).
Cada clase agrupa los casos de una HU; si la HU tiene casos de varios tipos, la clase existe
con el mismo nombre en cada carpeta que le corresponde. El id del caso (`cpHuNNXXX`) no cambia,
así que la trazabilidad con la HU y el reporte se mantiene.

```
qa/
├── soporte/                    Clases base: datos de prueba, usuarios, tiendas, inventario
├── funcionales/
│   ├── happypath/              Flujo principal con datos válidos: la operación tiene éxito
│   ├── negativepath/           Entradas inválidas o reglas de negocio que deben rechazarse
│   │                           (400, 404 de recurso inexistente, 405, 409)
│   └── exceptionalpath/        Caminos alternos y casos borde válidos: límites, idempotencia,
│                               reintentos, ventas forzadas, inventario fantasma, baja lógica
└── nofuncionales/
    ├── seguridad/              Autenticación, autorización por scope, alcance por tienda,
    │                           corte de acceso de usuarios desactivados
    ├── fiabilidad/             Concurrencia y tolerancia a errores (JSON mal formado, código
    │                           real en vez de 401, cuerpo de error con mensaje)
    └── interoperabilidad/      Formato del contrato (fechas en UTC, ISO-8601 con Z)
```

## Ejecutar

```
./mvnw clean -P casos-qa-sprint1 verify                         # todos + reporte HTML
./mvnw clean -P casos-qa-sprint1 verify -Dgroups=happy-path     # solo una categoría
./mvnw clean -P casos-qa-sprint1 verify -Dgroups=no-funcional   # varias: "seguridad,fiabilidad"
```

Etiquetas (`@Tag`) disponibles: `funcional`, `no-funcional`, `happy-path`, `negative-path`,
`exceptional-path`, `seguridad`, `fiabilidad`, `interoperabilidad`.

Usar siempre `clean`: sin él, el reporte mezcla resultados viejos de `target/failsafe-reports`.
